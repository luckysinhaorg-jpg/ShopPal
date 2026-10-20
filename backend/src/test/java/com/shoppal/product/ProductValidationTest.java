package com.shoppal.product;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsMissingNameAndNegativeStock() {
        Product product = new Product("SKU", " ", BusinessProfile.KIRANA, BigDecimal.ONE,
                BigDecimal.ONE, -1, 1, 0, 1, null, null, null);
        var violations = validator.validate(product);
        assertFalse(violations.isEmpty());
    }

    @Test
    void acceptsValidProduct() {
        Product product = new Product("SKU", "Rice 1 kg", BusinessProfile.KIRANA, BigDecimal.TEN,
                BigDecimal.ONE, 4, 2, 1, 1, null, null, null);
        assertTrue(validator.validate(product).isEmpty());
    }
}
