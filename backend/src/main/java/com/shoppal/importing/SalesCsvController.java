package com.shoppal.importing;

import com.shoppal.account.AuthService;
import com.shoppal.account.ShopAccount;
import com.shoppal.product.BusinessProfile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/import")
public class SalesCsvController {
    private final SalesCsvService csv;
    private final AuthService authService;

    public SalesCsvController(SalesCsvService csv, AuthService authService) {
        this.csv = csv;
        this.authService = authService;
    }

    @GetMapping(value = "/sample.csv", produces = "text/csv")
    public ResponseEntity<byte[]> sample(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) BusinessProfile profile) {

        Optional<ShopAccount> authAccount = authService.resolveAccount(authHeader);
        BusinessProfile effectiveProfile = profile;
        if (effectiveProfile == null && authAccount.isPresent()) {
            effectiveProfile = authAccount.get().getBusinessProfile();
        }

        String template;
        if (effectiveProfile == BusinessProfile.PHARMACY) {
            template = SalesCsvService.HEADER + "\nMED-001,2026-10-18,5,45.00\nMED-002,2026-10-18,3,120.00\n";
        } else if (effectiveProfile == BusinessProfile.FOOTWEAR) {
            template = SalesCsvService.HEADER + "\nFTW-001,2026-10-18,1,1499.00\nFTW-002,2026-10-18,1,2199.00\n";
        } else {
            template = SalesCsvService.HEADER + "\nKIR-001,2026-10-18,3,145.00\nKIR-002,2026-10-18,1,220.00\n";
        }

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shoppal-sales-sample.csv")
                .contentType(MediaType.parseMediaType("text/csv")).body(template.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SalesCsvService.Preview preview(@RequestPart("file") MultipartFile file) throws IOException {
        return csv.preview(file.getBytes());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SalesCsvService.ImportResult importCsv(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestPart("file") MultipartFile file) throws IOException {
        Long accountId = authService.resolveAccount(authHeader).map(ShopAccount::getId).orElse(null);
        return csv.importCsv(accountId, file.getBytes());
    }

    @ExceptionHandler(SalesCsvService.CsvValidationException.class)
    public ResponseEntity<Map<String, Object>> invalidCsv(SalesCsvService.CsvValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "CSV validation failed", "errors", ex.getErrors()));
    }
}
