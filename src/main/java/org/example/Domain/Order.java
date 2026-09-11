package org.example.Domain;

public class Order{
    private final int orderId;
    private final MenuItems Item;
    private final int quantity;
    private final Priority priority;
    private final Customer customer;
    private volatile OrderStatus status = OrderStatus.PLACED;

    public boolean shutdown() {
        return false;
    }
    public Order(int orderId, MenuItems item, Customer customer, int quantity, Priority priority) {
        this.orderId = orderId;
        this.Item = item;
        this.customer = customer;
        this.quantity = quantity;
        this.priority = priority;
    }

    @Override
    public String toString() {
         return "Order-" + orderId + ": " + Item.getLabel() + " x" + quantity
                + (priority == Priority.VIP ? " [VIP]" : "");
    }

    public int getOrderId() {return orderId;}
    public MenuItems getItem() {return Item;}
    public Customer getCustomer() {return customer;}
    public Priority getPriority() {return priority;}
    public int getQuantity() {return quantity;}
    public OrderStatus getStatus() {return status;}
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

}
