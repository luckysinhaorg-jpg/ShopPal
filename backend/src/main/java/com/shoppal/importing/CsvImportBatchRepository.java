package com.shoppal.importing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CsvImportBatchRepository extends JpaRepository<CsvImportBatch, String> {}
