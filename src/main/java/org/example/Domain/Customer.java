package org.example.Domain;

import java.util.concurrent.BlockingQueue;

public class Customer implements Runnable{

    private final int customerId;
    private final int orderId;
    private final MenuItems Item;
    private final int quantity;
    private final Priority priority;
    private final BlockingQueue<Order> queue;

    private boolean delivered = false;


    public Customer(int customerId, int orderId, Priority priority, int quantity, MenuItems item, BlockingQueue<Order> queue) {
        this.customerId = customerId;
        this.orderId = orderId;
        this.priority = priority;
        this.quantity = quantity;
        this.Item = item;
        this.queue = queue;
    }

    public int getCustomerId() {
        return customerId;
    }
    @Override
    public void run() {
    Order order1  = new Order(orderId,Item, this, quantity, priority);
            try{
                queue.put(order1);
                System.out.println("Order placed by customer " + customerId);
                waitForDelivery();

            }catch(Exception e ) {
                throw new RuntimeException(e);
            }
    }

    public synchronized void receivedOrder(){
        delivered = true;
        System.out.println("Order" + orderId + " received" + " by customer " + customerId);
        notifyAll();
    }

    public synchronized void waitForDelivery() throws InterruptedException {
        while(!delivered){
            wait();
        }
    }
}
