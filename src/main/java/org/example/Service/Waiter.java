package org.example.Service;

import org.example.Domain.Order;
import org.example.Domain.OrderStatus;

import java.util.concurrent.BlockingQueue;

public class Waiter implements Runnable{

    private final String name;
    private final BlockingQueue<Order> completedQueue;

    public Waiter(String name, BlockingQueue<Order> completedQueue) {
        this.name = name;
        this.completedQueue = completedQueue;
    }

    @Override
    public void run() {
        try {
            while(true){
                Order order = completedQueue.take();
                if (order.shutdown()){
                    break;
                }
                deliver(order);
            }
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    private void deliver(Order order) {
        String customerId = order.getCustomer().getCustomerId();
        System.out.println(name + " picked " + order);

        if (order.getStatus() == OrderStatus.READY) {
            order.setStatus(OrderStatus.DELIVERED);
            System.out.println(name + " delivered " + order + " to " + customerId);
        }else {
            System.out.println(name + " informed " + customerId + " that " + order + " is OUT_OF_STOCK");
        }
        order.getCustomer().receivedOrder(order);
    }
}
