package com.shoppal.sales;

import com.shoppal.product.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "return_records")
public class ReturnRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Product product;
    @NotNull private LocalDate returnDate;
    @NotNull @Min(1) private Integer unitsReturned;
    @NotBlank private String reason;

    protected ReturnRecord() {}
    public ReturnRecord(Product product, LocalDate returnDate, int unitsReturned, String reason) {
        this.product = product; this.returnDate = returnDate; this.unitsReturned = unitsReturned; this.reason = reason;
    }
    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public LocalDate getReturnDate() { return returnDate; }
    public Integer getUnitsReturned() { return unitsReturned; }
    public String getReason() { return reason; }
}
