package org.example.Service;

public class PriceCalculator {

    private double priceCalculated(Double price , Double discount)
    {
        return price - ((price*discount)/100);
    }

    public double calculatePrice(Double price , Double discount)
    {
        if (price == null || discount == null) {
            throw new IllegalArgumentException("Price and discount are required");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (discount < 0 || discount > 100) {
            throw new IllegalArgumentException("Discount cannot be negative or greater than 100");
        }
        return priceCalculated(price,discount);
    }

}
