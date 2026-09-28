package org.example.Domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Inventory {
    private final Map<MenuItems, Integer> stock = new EnumMap<>(MenuItems.class);
    /** what the kitchen opened with, so reports can show start vs remaining */
    private final Map<MenuItems, Integer> initialStock = new EnumMap<>(MenuItems.class);
    /** units actually taken by chefs; an OUT_OF_STOCK order takes nothing */
    private final Map<MenuItems, Integer> sold = new EnumMap<>(MenuItems.class);

    public Inventory() {
        stock.put(MenuItems.PIZZA, 8);
        stock.put(MenuItems.BURGER, 3);
        stock.put(MenuItems.PASTA, 6);
        stock.put(MenuItems.SANDWICH, 2);

        initialStock.putAll(stock);
        for (MenuItems item : MenuItems.values()) {
            sold.put(item, 0);
        }
    }

    /**
     * Lines of this order that cannot be filled right now, e.g. "Sandwich x3 requested, only 2 left".
     * Empty list means everything is available.
     */
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

    /**
     * All-or-nothing reservation for a multi-item order: checks every line first,
     * then deducts. One lock guards the whole operation, so no deadlock is possible.
     */
    public synchronized boolean reserveAll(Map<MenuItems, Integer> lines){
        for (Map.Entry<MenuItems, Integer> line : lines.entrySet()) {
            if (stock.get(line.getKey()) < line.getValue()) {
                return false;
            }
        }
        for (Map.Entry<MenuItems, Integer> line : lines.entrySet()) {
            take(line.getKey(), line.getValue());
        }
        return true;
    }

    public synchronized boolean reserve(MenuItems Item, int quantity){
        int available = stock.get(Item);
        if(available>=quantity) {
            take(Item, quantity);
            return true;
        }
        return false;
    }

    /** caller must hold the lock */
    private void take(MenuItems item, int quantity) {
        stock.put(item, stock.get(item) - quantity);
        sold.merge(item, quantity, Integer::sum);
    }

    public synchronized int getStock(MenuItems Item) {
        return stock.get(Item);
    }

    public synchronized int getInitialStock(MenuItems item) {
        return initialStock.get(item);
    }

    public synchronized int getSold(MenuItems item) {
        return sold.get(item);
    }

    /** Consistent snapshot of everything sold so far, safe to read outside the lock. */
    public synchronized Map<MenuItems, Integer> soldSnapshot() {
        return Collections.unmodifiableMap(new EnumMap<>(sold));
    }

    /** Consistent snapshot of remaining stock, safe to read outside the lock. */
    public synchronized Map<MenuItems, Integer> stockSnapshot() {
        return Collections.unmodifiableMap(new EnumMap<>(stock));
    }
}
