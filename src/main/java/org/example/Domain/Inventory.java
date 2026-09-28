package org.example.Domain;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Inventory {
    private final Map<MenuItems, Integer> stock = new HashMap<>();

    public Inventory() {
        stock.put(MenuItems.PIZZA, 8);
        stock.put(MenuItems.BURGER, 3);
        stock.put(MenuItems.PASTA, 6);
        stock.put(MenuItems.SANDWICH, 2);
    }


    public synchronized List<String> shortages(Map<MenuItems, Integer> lines){
        List<String> shortages = new ArrayList<>();
        for (Map.Entry<MenuItems, Integer> line : lines.entrySet()) {
            int available = stock.get(line.getKey());
            if (available < line.getValue()) {
                shortages.add(line.getKey().getLabel() + " x" + line.getValue()
                        + " requested, only " + available + " left");
            }
        }
        return shortages;
    }

    public synchronized boolean reserveAll(Map<MenuItems, Integer> lines){
        for (Map.Entry<MenuItems, Integer> line : lines.entrySet()) {
            if (stock.get(line.getKey()) < line.getValue()) {
                return false;
            }
        }
        for (Map.Entry<MenuItems, Integer> line : lines.entrySet()) {
            stock.put(line.getKey(), stock.get(line.getKey()) - line.getValue());
        }
        return true;
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
