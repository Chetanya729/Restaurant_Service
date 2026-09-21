package org.example;

import org.example.Service.PriceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriceCalculatorTest {

    private PriceCalculator priceCalculator;

    @BeforeEach
    void setup() {
        priceCalculator = new PriceCalculator();
    }

    @Test
    void shouldApplyDiscount() {
        assertEquals(90, priceCalculator.calculatePrice(100.0, 10.0));
    }

    @Test
    void shouldApplyNoDiscountForZeroPercent() {
        assertEquals(100, priceCalculator.calculatePrice(100.0, 0.0));
    }

    @Test
    void shouldRejectDiscountAboveHundred() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> priceCalculator.calculatePrice(100.0, 110.0));
        assertEquals("Discount cannot be negative or greater than 100", error.getMessage());
    }

    @Test
    void shouldRejectNegativeDiscount() {
        assertThrows(IllegalArgumentException.class, () -> priceCalculator.calculatePrice(100.0, -5.0));
    }

    @Test
    void shouldRejectNegativePrice() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> priceCalculator.calculatePrice(-1.0, 10.0));
        assertEquals("Price cannot be negative", error.getMessage());
    }
}
