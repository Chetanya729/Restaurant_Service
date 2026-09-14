package org.example;

import org.example.Domain.*;
import org.example.Service.Chef;
import org.example.Service.Waiter;

import java.util.Comparator;
import java.util.concurrent.*;

public class Main {

        private static final int Chefs = 3;
        private static final int Waiters = 2;
        private static final int Customers = 10;

    static void main() throws InterruptedException {

        Inventory inventory = new Inventory();

        BlockingQueue<Order> orderQueue = new PriorityBlockingQueue<>(20,
                Comparator.comparing(Order::getPriority).thenComparing(Order::getOrderId));

        BlockingQueue<Order> completedOrderQueue = new LinkedBlockingQueue<>();

        CountDownLatch ordersPlaced = new CountDownLatch(Customers);

        Customer[] customers = {
                new Customer("Customer-1",  101, Priority.NORMAL, 2, MenuItems.PIZZA,    orderQueue, ordersPlaced),
                new Customer("Customer-2",  102, Priority.VIP,    1, MenuItems.BURGER,   orderQueue, ordersPlaced),
                new Customer("Customer-3",  103, Priority.NORMAL, 2, MenuItems.PASTA,    orderQueue, ordersPlaced),
                new Customer("Customer-4",  104, Priority.VIP,    1, MenuItems.PIZZA,    orderQueue, ordersPlaced),
                new Customer("Customer-5",  105, Priority.NORMAL, 2, MenuItems.SANDWICH, orderQueue, ordersPlaced),
                new Customer("Customer-6",  106, Priority.NORMAL, 3, MenuItems.BURGER,   orderQueue, ordersPlaced),
                new Customer("Customer-7",  107, Priority.VIP,    2, MenuItems.PASTA,    orderQueue, ordersPlaced),
                new Customer("Customer-8",  108, Priority.NORMAL, 2, MenuItems.PIZZA,    orderQueue, ordersPlaced),
                new Customer("Customer-9",  109, Priority.NORMAL, 1, MenuItems.SANDWICH, orderQueue, ordersPlaced),
                new Customer("Customer-10", 110, Priority.VIP,    2, MenuItems.BURGER,   orderQueue, ordersPlaced)
        };

        Thread[] customerThreads = new Thread[customers.length];
        for (int i = 0; i < customers.length; i++) {
            customerThreads[i] = new Thread(customers[i], "Customer-Thread-" + (i + 1));
            customerThreads[i].start();
        }

        ordersPlaced.await();
        System.out.println("\nAll orders placed. Kitchen is open.\n");

        ExecutorService kitchen = Executors.newFixedThreadPool(Chefs);
        ExecutorService service = Executors.newFixedThreadPool(Waiters);

        for (int i = 1; i <= Chefs; i++)
        {
            kitchen.execute(new Chef("Chef-" + i, orderQueue, completedOrderQueue, inventory));
        }
        for (int i = 1; i <= Waiters; i++)
        {
            service.execute(new Waiter("Waiter-" + i, completedOrderQueue));
        }

        for (Thread t : customerThreads)
        {
            t.join();
        }
        System.out.println("\nRestaurant is closing...");
        System.out.println("All orders processed.");

        for (int i = 0; i < Chefs; i++){
            orderQueue.put(new ShutDown());
        }
        kitchen.shutdown();
        if (!kitchen.awaitTermination(10, TimeUnit.SECONDS)) {
            kitchen.shutdownNow();
        }
        System.out.println("All chefs stopped.");

        for (int i = 0; i < Waiters; i++){
            completedOrderQueue.put(new ShutDown());
        }
        service.shutdown();
        if (!service.awaitTermination(10, TimeUnit.SECONDS)) {
            service.shutdownNow();
        }
        System.out.println("All waiters stopped.");

        System.out.println("Remaining Inventory");
        for(MenuItems item:MenuItems.values())
        {
            System.out.println("  " + item.getLabel() + ": " + inventory.getStock(item));
        }
        System.out.println("Restaurant closed successfully.");
    }
}
