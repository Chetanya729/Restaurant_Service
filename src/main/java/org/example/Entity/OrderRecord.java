package org.example.Entity;

import jakarta.persistence.*;
import org.example.Domain.MenuItems;
import org.example.Domain.Order;
import org.example.Domain.OrderStatus;
import org.example.Domain.Priority;

import java.time.Instant;

@Entity
@Table(name = "order_records")
public class OrderRecord {

    @Id
    private int orderId;
    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MenuItems item;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String Chef;
    private String Waiter;

    private Instant placedAt;
    private Instant preparedAt;
    private Instant deliveredAt;

    protected OrderRecord() {
    }

    public OrderRecord(Order order) {
        this.orderId = order.getOrderId();
        this.customerId = order.getCustomer().getCustomerId();
        this.item = order.getItem();
        this.priority = order.getPriority();
        this.status = OrderStatus.PLACED;
        this.placedAt = Instant.now();
    }

    public void kitchen(OrderStatus orderStatus, String chef) {
        this.status = orderStatus;
        this.Chef = chef;
        if (orderStatus != OrderStatus.PREPARING){
            this.preparedAt = Instant.now();
        }
    }

    public void service(OrderStatus orderStatus, String waiter) {
        this.status = orderStatus;
        this.Waiter = waiter;
        this.deliveredAt = Instant.now();
    }

    public int getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public MenuItems getItem() {
        return item;
    }

    public Priority getPriority() {
        return priority;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getChef() {
        return Chef;
    }

    public String getWaiter() {
        return Waiter;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public Instant getPreparedAt() {
        return preparedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }
}
