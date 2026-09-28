package org.example.Domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Order{
    private final int orderId;
    /** item -> quantity. Unmodifiable and never changed after construction, so every thread sees the same lines. */
    private final Map<MenuItems, Integer> lines;
    private final Priority priority;
    private final Customer customer;
    @Setter
    private volatile OrderStatus status = OrderStatus.PLACED;

    /** Why the status is what it is, e.g. which lines were out of stock. Set by the chef, read by the waiter. */
    @Setter
    @Getter
    private volatile String statusNote = "";

    public boolean shutdown() {
        return false;
    }

    public Order(int orderId, Map<MenuItems, Integer> lines, Customer customer, Priority priority) {
        this.orderId = orderId;
        // defensive copy: the caller's cart may be cleared or reused afterwards
        this.lines = lines.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(lines));
        this.customer = customer;
        this.priority = priority;
    }

    @Override
    public String toString() {
         return "Order-" + orderId;
    }

    public String vipTag() {
        return priority == Priority.VIP ? " [VIP]" : "";
    }

    /** "Pizza x2, Sandwich x3" */
    public String itemSummary() {
        return lines.entrySet().stream()
                .map(e -> e.getKey().getLabel() + " x" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    public String details() {
        return this + ": " + itemSummary() + vipTag();
    }

    /** Total cooking time for every line of this order. */
    public long cookTimeMs() {
        return lines.entrySet().stream()
                .mapToLong(e -> e.getKey().getCookTimeS() * e.getValue())
                .sum();
    }

    public int getOrderId() {return orderId;}
    public Map<MenuItems, Integer> getLines() {return lines;}
    public Customer getCustomer() {return customer;}
    public Priority getPriority() {return priority;}
    public OrderStatus getStatus() {return status;}

}
