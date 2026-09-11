package org.example.Domain;

import java.util.HashMap;
import java.util.Map;

public class Inventory {
    private final Map<MenuItems, Integer> stock = new HashMap<>();

    public Inventory() {
        stock.put(MenuItems.PIZZA, 8);
        stock.put(MenuItems.BURGER, 10);
        stock.put(MenuItems.PASTA, 6);
        stock.put(MenuItems.SANDWICH, 7);
    }

    public synchronized boolean reserve(MenuItems Item, int quantity){
        int available = stock.get(Item);
        if(available>quantity) {
            stock.put(Item, available - 1);
            return true;
        }
        return false;
    }


    public int getStock(MenuItems Item) {
        return stock.get(Item);
    }
}
