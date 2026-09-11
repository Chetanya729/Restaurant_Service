package org.example.Domain;

public enum MenuItems {
    PIZZA("Pizza" , 3000),
    BURGER("Burger" ,2000),
    PASTA("Pasta", 2500),
    SANDWICH("Sandwich", 1500);

    private final String label;
    private final long cookTimeS;

    MenuItems(String label, int cookTimeS)
    {
        this.label = label;
        this.cookTimeS = cookTimeS;
    }

    public String getLabel() {
        return label;
    }

    public long getCookTimeS() {
        return cookTimeS;
    }

}
