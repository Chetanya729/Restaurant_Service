package org.example.io;

import org.example.Domain.MenuItems;
import org.example.Domain.Priority;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CsvLoader {

    public static final Path MENU_FILE = Path.of("csv", "menu_items.csv");
    public static final Path CUSTOMERS_FILE = Path.of("csv", "customers.csv");

    private CsvLoader() {
    }


    public record MenuRow(MenuItems item, String label, long cookTimeMs, int price) {}

    public record CustomerRow(String customer, MenuItems item, int quantity, Priority priority) {}

    public static Map<MenuItems, MenuRow> loadMenu(Path file) {
        Map<MenuItems, MenuRow> menu = new EnumMap<>(MenuItems.class);

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file.toAbsolutePath(), e);
        }

        boolean headerSeen = false;
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i).trim();
            int lineNumber = i + 1;

            if (raw.isEmpty() || raw.startsWith("#")) continue;
            if (!headerSeen) {
                headerSeen = true;
                continue;
            }

            String[] cells = raw.split(",", -1);
            if (cells.length != 4) {
                throw new IllegalArgumentException(file + " line " + lineNumber
                        + ": expected 4 columns but found " + cells.length + " -> " + raw);
            }

            try {
                MenuItems item = MenuItems.valueOf(cells[0].trim().toUpperCase());
                String label = cells[1].trim();
                long cookTimeMs = Long.parseLong(cells[2].trim());
                int price = Integer.parseInt(cells[3].trim());

                if (cookTimeMs <= 0 || price < 0) {
                    throw new IllegalArgumentException("cookTimeMs must be > 0 and price >= 0");
                }
                menu.put(item, new MenuRow(item, label, cookTimeMs, price));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(file + " line " + lineNumber
                        + ": " + e.getMessage() + " -> " + raw, e);
            }
        }

        if (menu.isEmpty()) {
            throw new IllegalArgumentException(file + " contains no menu rows");
        }
        return menu;
    }
    public static List<CustomerRow> loadCustomers(Path file) {
        List<CustomerRow> rows = new ArrayList<>();

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file.toAbsolutePath(), e);
        }

        boolean headerSeen = false;
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i).trim();
            int lineNumber = i + 1;

            if (raw.isEmpty() || raw.startsWith("#")) continue;
            if (!headerSeen) {
                headerSeen = true;
                continue;
            }

            String[] cells = raw.split(",", -1);
            if (cells.length != 4) {
                throw new IllegalArgumentException(file + " line " + lineNumber
                        + ": expected 4 columns but found " + cells.length + " -> " + raw);
            }

            try {
                String customer = cells[0].trim();
                if (customer.isEmpty()) {
                    throw new IllegalArgumentException("customer name cannot be empty");
                }
                MenuItems item = MenuItems.valueOf(cells[1].trim().toUpperCase());
                int quantity = Integer.parseInt(cells[2].trim());
                if (quantity < 1 || quantity > 20) {
                    throw new IllegalArgumentException("quantity must be between 1 and 20 but was " + quantity);
                }
                Priority priority = Priority.valueOf(cells[3].trim().toUpperCase());

                rows.add(new CustomerRow(customer, item, quantity, priority));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(file + " line " + lineNumber
                        + ": " + e.getMessage() + " -> " + raw, e);
            }
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(file + " contains no customer rows");
        }
        return rows;
    }
}
