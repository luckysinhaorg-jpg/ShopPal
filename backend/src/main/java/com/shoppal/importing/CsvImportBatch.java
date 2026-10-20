package com.shoppal.importing;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.time.Instant;

@Entity
public class CsvImportBatch {
    @Id @Column(length = 64) private String sha256;
    private Instant importedAt;
    private int rowCount;
    protected CsvImportBatch() {}
    public CsvImportBatch(String sha256, Instant importedAt, int rowCount) {
        this.sha256 = sha256; this.importedAt = importedAt; this.rowCount = rowCount;
    }
    public String getSha256() { return sha256; }
    public Instant getImportedAt() { return importedAt; }
    public int getRowCount() { return rowCount; }
}
