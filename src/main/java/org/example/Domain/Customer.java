package org.example.Domain;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class Customer implements Runnable{

    private final String customerId;
    private final int orderId;
    private final MenuItems Item;
    private final int quantity;
    private final Priority priority;
    private final BlockingQueue<Order> queue;
    private final CountDownLatch ordersPlaced;

    private boolean delivered = false;


    public Customer(String customerId, int orderId, Priority priority, int quantity, MenuItems item,
                    BlockingQueue<Order> queue, CountDownLatch ordersPlaced) {
        this.customerId = customerId;
        this.orderId = orderId;
        this.priority = priority;
        this.quantity = quantity;
        this.Item = item;
        this.queue = queue;
        this.ordersPlaced = ordersPlaced;
    }

    public String getCustomerId() {
        return customerId;
    }
    @Override
    public void run() {
        Order order = new Order(orderId, Item, this, quantity, priority);
        try {
            queue.put(order);
            System.out.println(customerId + " placed " + order.details());
            ordersPlaced.countDown();
            waitForDelivery();

            if (order.getStatus() == OrderStatus.OUT_OF_STOCK) {
                System.out.println(customerId + " notified: " + order + " is OUT_OF_STOCK");
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
