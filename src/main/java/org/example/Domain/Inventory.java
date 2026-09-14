package org.example.Domain;

import java.util.HashMap;
import java.util.Map;

public class Inventory {
    private final Map<MenuItems, Integer> stock = new HashMap<>();

    public Inventory() {
        stock.put(MenuItems.PIZZA, 8);
        stock.put(MenuItems.BURGER, 3);
        stock.put(MenuItems.PASTA, 6);
        stock.put(MenuItems.SANDWICH, 2);
    }

    public synchronized boolean reserve(MenuItems Item, int quantity){
        int available = stock.get(Item);
        if(available>=quantity) {
            stock.put(Item, available - quantity);
            return true;
        }
        return false;
    }


    public synchronized int getStock(MenuItems Item) {
        return stock.get(Item);
    }
}
