package org.example.Domain;
import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class Customer implements Runnable{

    @Getter
    private final String customerId;
    private final int orderId;
    private final Map<MenuItems, Integer> lines;
    private final Priority priority;
    private final BlockingQueue<Order> queue;

    // built (and saved to the database) by the caller before this thread starts
    private volatile Order order;

    private boolean delivered = false;


    public Customer(String customerId, int orderId, Priority priority, Map<MenuItems, Integer> lines,
                    BlockingQueue<Order> queue) {
        this.customerId = customerId;
        this.orderId = orderId;
        this.priority = priority;
        this.lines = lines.isEmpty() ? Map.of() : new EnumMap<>(lines);
        this.queue = queue;
    }

    /** Creates this customer's order. Call it (and persist the order) before starting the thread. */
    public Order createOrder() {
        this.order = new Order(orderId, lines, this, priority);
        return order;
    }

    @Override
    public void run() {
        Order order = this.order != null ? this.order : createOrder();
        try {
            queue.put(order);
            System.out.println(customerId + " placed " + order.details());

            waitForDelivery();

            if (order.getStatus() == OrderStatus.OUT_OF_STOCK) {
                String why = order.getStatusNote().isEmpty() ? "" : " - " + order.getStatusNote();
                System.out.println(customerId + " notified: " + order + " is OUT_OF_STOCK" + why);
            } else {
                System.out.println(customerId + " received " + order);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void receivedOrder(Order order){
        delivered = true;
        notifyAll();
    }

    public synchronized void waitForDelivery() throws InterruptedException {
        while(!delivered){
            wait();
        }
    }
}
