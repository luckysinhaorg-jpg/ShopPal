package com.shoppal.supplier;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Supplier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank private String name;
    private String phone;

    protected Supplier() {}
    public Supplier(String name, String phone) { this.name = name; this.phone = phone; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
}
