package com.shoppal.importing;

import com.shoppal.product.Product;
import com.shoppal.product.ProductRepository;
import com.shoppal.sales.SalesRecord;
import com.shoppal.sales.SalesRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class SalesCsvService {
    public static final String HEADER = "sku,date,unitsSold,unitPrice";
    private static final int MAX_BYTES = 1_048_576;
    private final ProductRepository products;
    private final SalesRecordRepository sales;
    private final CsvImportBatchRepository imports;

    public SalesCsvService(ProductRepository products, SalesRecordRepository sales, CsvImportBatchRepository imports) {
        this.products = products; this.sales = sales; this.imports = imports;
    }

    public Preview preview(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return new Preview(List.of(), List.of("CSV file is empty."));
        if (bytes.length > MAX_BYTES) return new Preview(List.of(), List.of("CSV file exceeds the 1 MB limit."));
        String content = new String(bytes, StandardCharsets.UTF_8);
        String[] lines = content.replace("\uFEFF", "").split("\\R", -1);
        if (lines.length == 0 || !HEADER.equals(lines[0].trim()))
            return new Preview(List.of(), List.of("Header must be exactly: " + HEADER));
        List<SalesCsvRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            List<String> fields = splitCsv(line);
            if (fields == null || fields.size() != 4) { errors.add("Row " + (i + 1) + ": expected 4 CSV fields."); continue; }
            try {
                String sku = fields.get(0).trim();
                if (sku.isBlank()) throw new IllegalArgumentException("SKU is required");
                LocalDate date = LocalDate.parse(fields.get(1).trim());
                int units = Integer.parseInt(fields.get(2).trim());
                BigDecimal unitPrice = new BigDecimal(fields.get(3).trim());
                if (units < 1) throw new IllegalArgumentException("unitsSold must be a positive whole number");
                if (unitPrice.signum() < 0) throw new IllegalArgumentException("unitPrice cannot be negative");
                rows.add(new SalesCsvRow(sku, date, units, unitPrice));
            } catch (RuntimeException ex) { errors.add("Row " + (i + 1) + ": invalid value (" + ex.getMessage() + ")."); }
        }
        if (rows.isEmpty() && errors.isEmpty()) errors.add("CSV has no data rows.");
        return new Preview(rows, errors);
    }

    @Transactional
    public ImportResult importCsv(byte[] bytes) {
        return importCsv(null, bytes);
    }

    @Transactional
    public ImportResult importCsv(Long accountId, byte[] bytes) {
        Preview preview = preview(bytes);
        if (!preview.errors().isEmpty()) throw new CsvValidationException(preview.errors());
        String hashKey = sha256(bytes) + (accountId != null ? ("#" + accountId) : "");
        var previous = imports.findById(hashKey);
        if (previous.isPresent()) return new ImportResult(0, true, preview.rows().size(), List.of());
        List<String> errors = new ArrayList<>();
        List<SalesRecord> records = new ArrayList<>();
        List<Product> availableProducts = accountId != null ? products.findByAccountId(accountId) : products.findAll();
        if (availableProducts.isEmpty() && accountId != null) {
            availableProducts = products.findAll();
        }
        var bySku = availableProducts.stream().collect(java.util.stream.Collectors.toMap(
                p -> p.getSku().toLowerCase(java.util.Locale.ROOT), p -> p, (a, b) -> a));
        for (int i = 0; i < preview.rows().size(); i++) {
            SalesCsvRow row = preview.rows().get(i);
            Product product = bySku.get(row.sku().toLowerCase(java.util.Locale.ROOT));
            if (product == null) errors.add("Data row " + (i + 2) + ": unknown SKU " + row.sku() + ".");
            else records.add(new SalesRecord(product, row.date(), row.unitsSold(), row.unitPrice()));
        }
        if (!errors.isEmpty()) throw new CsvValidationException(errors);
        sales.saveAll(records);
        imports.save(new CsvImportBatch(hashKey, Instant.now(), records.size()));
        return new ImportResult(records.size(), false, records.size(), List.of());
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception ex) { throw new IllegalStateException("SHA-256 unavailable", ex); }
    }

    private static List<String> splitCsv(String line) {
        List<String> values = new ArrayList<>(); StringBuilder field = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { field.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) { values.add(field.toString()); field.setLength(0); }
            else field.append(c);
        }
        if (quoted) return null;
        values.add(field.toString()); return values;
    }

    public record SalesCsvRow(String sku, LocalDate date, int unitsSold, BigDecimal unitPrice) {}
    public record Preview(List<SalesCsvRow> rows, List<String> errors) {}
    public record ImportResult(int importedRows, boolean duplicate, int validatedRows, List<String> errors) {}
    public static class CsvValidationException extends RuntimeException {
        private final List<String> errors;
        public CsvValidationException(List<String> errors) { super(String.join("; ", errors)); this.errors = List.copyOf(errors); }
        public List<String> getErrors() { return errors; }
    }
}
