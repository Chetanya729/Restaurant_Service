package org.example.Service;

import org.example.Domain.Inventory;
import org.example.Domain.Order;
import org.example.Domain.OrderStatus;
import java.util.concurrent.BlockingQueue;

public class Chef implements Runnable {
    private final String name;
    private final BlockingQueue<Order> orderQueue;
    private final BlockingQueue<Order> completedQueue;
    private final Inventory inventory;

    public Chef(String name, BlockingQueue<Order> orderQueue, BlockingQueue<Order> completedQueue, Inventory inventory) {
        this.name = name;
        this.orderQueue = orderQueue;
        this.completedQueue = completedQueue;
        this.inventory = inventory;
    }
    @Override
    public void run() {

        try{
            while (true){
                Order order = orderQueue.take();
                if (order.shutdown()){
                    break;
                }
                prepare(order);
            }
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    private void prepare(Order order) throws InterruptedException {
        order.setStatus(OrderStatus.PREPARING);
        boolean reserved = inventory.reserve(order.getItem(), order.getQuantity());

        String header = name + " picked " + order + order.vipTag() + System.lineSeparator()
                + name + " checking inventory for " + order.getItem().getLabel() + " x" + order.getQuantity()
                + System.lineSeparator();

        if(!reserved){
            order.setStatus(OrderStatus.OUT_OF_STOCK);
            System.out.println(header + order + ": OUT_OF_STOCK");
            completedQueue.put(order);
            return;
        }
        System.out.println(header + name + " preparing " + order);
        Thread.sleep(order.getItem().getCookTimeS()*order.getQuantity());

        order.setStatus(OrderStatus.READY);
        System.out.println(name + " completed " + order);
        completedQueue.put(order);
    }
}
