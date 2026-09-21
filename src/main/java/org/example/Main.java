package org.example;

import jakarta.persistence.EntityManagerFactory;
import org.example.Domain.*;
import org.example.Entity.OrderRecord;
import org.example.Repository.Database;
import org.example.Repository.OrderRepo;
import org.example.Repository.StaffRepo;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {

        EntityManagerFactory emf = Database.open();
        try {
            OrderRepo orderRepo = new OrderRepo(emf);
            StaffRepo staffRepo = new StaffRepo(emf);
            Restaurant restaurant = new Restaurant(new Inventory(), orderRepo, staffRepo);
            restaurant.loadStaff();     // staff hired in earlier runs

            if (args.length > 0 && args[0].equals("--demo")) {
                runDemo(restaurant);
            } else {
                menuLoop(restaurant);
            }
        } finally {
            emf.close();
        }
    }

    // ---------------- interactive menu ----------------

    private static void menuLoop(Restaurant restaurant) throws Exception {
        Scanner in = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("""

                    === RESTAURANT ===
                    1) Hire chef          2) Hire waiter
                    3) Fire chef          4) Fire waiter
                    5) Add customer order
                    6) Open restaurant    7) Status
                    8) Close restaurant   9) Run 10-customer demo
                    0) Exit""");
            System.out.print("Choice: ");

            String choice;
            try {
                choice = in.nextLine().trim();
            } catch (NoSuchElementException e) {   // no console input (piped or closed)
                System.out.println("\nNo input available, exiting.");
                restaurant.close();
                return;
            }

            try {
                switch (choice) {
                    case "1" -> restaurant.hireChef(ask(in, "Chef name: "));
                    case "2" -> restaurant.hireWaiter(ask(in, "Waiter name: "));
                    case "3" -> restaurant.fireChef(ask(in, "Chef name: "));
                    case "4" -> restaurant.fireWaiter(ask(in, "Waiter name: "));
                    case "5" -> addOrder(in, restaurant);
                    case "6" -> tryOpen(restaurant);
                    case "7" -> restaurant.printStatus();
                    case "8" -> closeAndReport(restaurant);
                    case "9" -> runDemo(restaurant);
                    case "0" -> {
                        closeAndReport(restaurant);
                        running = false;
                    }
                    default -> System.out.println("Unknown option: " + choice);
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("!! " + e.getMessage());     // friendly, keeps the menu alive
            }
        }
        System.out.println("Goodbye.");
    }

    private static void tryOpen(Restaurant restaurant) {
        if (!restaurant.canOpen()) {
            System.out.println("!! Cannot open: " + restaurant.openBlockedReason());
            System.out.println("   You need at least 1 chef AND 1 waiter.");
            return;
        }
        restaurant.open();
    }

    private static void addOrder(Scanner in, Restaurant restaurant) {
        if (!restaurant.isOpen()) {
            System.out.println("!! Restaurant is " + restaurant.status()
                    + ", so no orders can be taken."
                    + (restaurant.canOpen() ? " Choose 6 to open it." : " " + restaurant.openBlockedReason() + "."));
            return;
        }
        String customer = ask(in, "Customer name: ");

        System.out.println("  Menu:");
        MenuItems[] items = MenuItems.values();
        for (int i = 0; i < items.length; i++) {
            System.out.printf("    %d) %-9s (%.1fs each, %d in stock)%n",
                    i + 1, items[i].getLabel(), items[i].getCookTimeS() / 1000.0,
                    restaurant.inventory().getStock(items[i]));
        }
        MenuItems item = items[askInt(in, "  Item number: ", 1, items.length) - 1];
        int quantity = askInt(in, "  Quantity: ", 1, 99);
        boolean vip = ask(in, "  VIP? (y/N): ").equalsIgnoreCase("y");

        int orderId = restaurant.placeOrder(customer, item, quantity, vip ? Priority.VIP : Priority.NORMAL);
        System.out.println("Order-" + orderId + " accepted for " + customer + ".");
    }

    private static void closeAndReport(Restaurant restaurant) throws InterruptedException {
        if (restaurant.status() == Restaurant.Status.CLOSED) {
            System.out.println("Restaurant is already closed.");
            return;
        }
        restaurant.close();
        printReport(restaurant);
    }

    // ---------------- demo (the original 10-customer scenario) ----------------

    private static void runDemo(Restaurant restaurant) throws InterruptedException {
        if (restaurant.chefCount() == 0) {
            for (int i = 1; i <= 3; i++) {
                restaurant.hireChef("Chef-" + i);
            }
        }
        if (restaurant.waiterCount() == 0) {
            for (int i = 1; i <= 2; i++) {
                restaurant.hireWaiter("Waiter-" + i);
            }
        }
        if (!restaurant.isOpen()) {
            restaurant.open();
        }

        System.out.println();
        restaurant.placeOrder("Customer-1",  MenuItems.PIZZA,    2, Priority.NORMAL);
        restaurant.placeOrder("Customer-2",  MenuItems.BURGER,   1, Priority.VIP);
        restaurant.placeOrder("Customer-3",  MenuItems.PASTA,    2, Priority.NORMAL);
        restaurant.placeOrder("Customer-4",  MenuItems.PIZZA,    1, Priority.VIP);
        restaurant.placeOrder("Customer-5",  MenuItems.SANDWICH, 2, Priority.NORMAL);
        restaurant.placeOrder("Customer-6",  MenuItems.BURGER,   3, Priority.NORMAL);
        restaurant.placeOrder("Customer-7",  MenuItems.PASTA,    2, Priority.VIP);
        restaurant.placeOrder("Customer-8",  MenuItems.PIZZA,    2, Priority.NORMAL);
        restaurant.placeOrder("Customer-9",  MenuItems.SANDWICH, 1, Priority.NORMAL);
        restaurant.placeOrder("Customer-10", MenuItems.BURGER,   2, Priority.VIP);

        restaurant.awaitAllServed();
        restaurant.close();
        printReport(restaurant);
    }

    // ---------------- report ----------------

    private static void printReport(Restaurant restaurant) {
        System.out.println("\nRemaining Inventory");
        for (MenuItems item : MenuItems.values()) {
            System.out.println("  " + item.getLabel() + ": " + restaurant.inventory().getStock(item));
        }

        System.out.println("\nOrders (from database)");
        System.out.printf("  %-4s %-12s %-9s %-7s %-13s %-7s %-9s%n",
                "ID", "Customer", "Item", "Prio", "Status", "Chef", "Waiter");
        for (OrderRecord r : restaurant.orders().findAll()) {
            System.out.printf("  %-4d %-12s %-9s %-7s %-13s %-7s %-9s%n",
                    r.getOrderId(), r.getCustomerId(), r.getItem().getLabel(),
                    r.getPriority(), r.getStatus(),
                    r.getChef() == null ? "-" : r.getChef(),
                    r.getWaiter() == null ? "-" : r.getWaiter());
        }
        System.out.println("Restaurant closed successfully.");
    }

    // ---------------- input helpers ----------------

    private static String ask(Scanner in, String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = in.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("  Please type something.");
        }
    }

    private static int askInt(Scanner in, String prompt, int min, int max) {
        while (true) {
            try {
                int value = Integer.parseInt(ask(in, prompt));
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("  Enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  That is not a number.");
            }
        }
    }
}
