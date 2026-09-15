package org.example.Domain;

import java.util.HashMap;
import java.util.Map;

public class UnsafeInventory {
    private final Map<MenuItems, Integer> stock = new HashMap<>();

    public UnsafeInventory() {
        stock.put(MenuItems.PIZZA, 8);
        stock.put(MenuItems.BURGER, 10);
        stock.put(MenuItems.PASTA, 6);
        stock.put(MenuItems.SANDWICH, 7);
    }

    public boolean reserve(MenuItems Item, int quantity){
        int available = stock.get(Item);
        if(available>=quantity) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            stock.put(Item, available - quantity);
            return true;
        }
        return false;
    }

    public int getStock(MenuItems Item) {
        return stock.get(Item);
    }
}
