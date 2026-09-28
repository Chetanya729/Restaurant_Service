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
    private final OrderService orderService;

    public Chef(String name, BlockingQueue<Order> orderQueue, BlockingQueue<Order> completedQueue, Inventory inventory, OrderService orderService) {
        this.name = name;
        this.orderQueue = orderQueue;
        this.completedQueue = completedQueue;
        this.inventory = inventory;
        this.orderService = orderService;
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
        boolean reserved = inventory.reserveAll(order.getLines());

        String header = name + " picked " + order + order.vipTag() + System.lineSeparator()
                + name + " checking inventory for " + order.itemSummary()
                + System.lineSeparator();

        if(!reserved){
            String missing = String.join("; ", inventory.shortages(order.getLines()));
            order.setStatus(OrderStatus.OUT_OF_STOCK);
            order.setStatusNote(missing);
            orderService.recordKitchen(order.getOrderId(), OrderStatus.OUT_OF_STOCK,name);
            System.out.println(header + order + ": OUT_OF_STOCK - " + missing);
            completedQueue.put(order);
            return;
        }
        orderService.recordKitchen(order.getOrderId(), OrderStatus.PREPARING,name);
        System.out.println(header + name + " preparing " + order);
        Thread.sleep(order.cookTimeMs());


        order.setStatus(OrderStatus.READY);
        orderService.recordKitchen(order.getOrderId(), OrderStatus.READY,name);
        System.out.println(name + " completed " + order);
        completedQueue.put(order);
    }
}
