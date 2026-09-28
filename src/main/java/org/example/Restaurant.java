package org.example;

import org.example.Domain.*;
import org.example.Service.OrderService;
import org.example.Service.Chef;
import org.example.Service.Waiter;
import org.example.Entity.OrderRecord;
import org.example.io.CsvLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class Restaurant {

    @Autowired
    public Restaurant(Inventory inventory, OrderService orderService) {
        this(inventory, orderService,
                new PriorityBlockingQueue<>(20,
                        Comparator.comparing(Order::getPriority).thenComparingInt(Order::getOrderId)),
                new LinkedBlockingQueue<>());
    }

    public Restaurant(Inventory inventory, OrderService orderService, BlockingQueue<Order> orderQueue, BlockingQueue<Order> completedOrderQueue) {
        this.inventory = inventory;
        this.orderService = orderService;
        this.orderQueue = orderQueue;
        this.completedOrderQueue = completedOrderQueue;
    }

    public enum Status {
        CLOSED,
        OPEN
    }
    private final List<String> chefNames = new ArrayList<>();
    private final List<String> waiterNames = new ArrayList<>();
    private Status status = Status.CLOSED;

    private final Inventory inventory;
    private final OrderService orderService;
    private final BlockingQueue<Order> orderQueue;
    private final BlockingQueue<Order> completedOrderQueue ;

    private ExecutorService kitchen;
    private ExecutorService service;

    private final AtomicInteger nextOrderId = new AtomicInteger(101);
    private final List<Thread> customerThreads = Collections.synchronizedList(new ArrayList<>());


    public synchronized boolean chefNamesAdd(String name) {
        if(status == Status.OPEN) {
            System.out.println("Chefs can't be added as the restaurant is already open");
            return false;
        }
        String clean = name.trim();
        if (clean.isEmpty() || isTaken(clean)){
            return false;
        }
        chefNames.add(clean);
        System.out.println("Hired chef: " + name + " (chefs: " + chefNames.size() + ")");
        return true;
    }
    public synchronized boolean waiterNamesAdd(String name) {
        String clean = name.trim();
        if (clean.isEmpty() || isTaken(clean)){
            return false;
        }
        waiterNames.add(name);
        System.out.println("Hired waiter: " + name + " (waiters: " + waiterNames.size() + ")");
        return true;
    }

    public synchronized int chefCount() {
        return chefNames.size();
    }

    public synchronized int waiterCount() {
        return waiterNames.size();
    }

    public synchronized boolean isOpen() {
        return status == Status.OPEN;
    }

    public synchronized boolean canOpen(){
        return !chefNames.isEmpty() && !waiterNames.isEmpty();
    }

    public synchronized String openBlockedReason(){
        if(chefNames.isEmpty() &&  waiterNames.isEmpty()){
            return "No chef or waiter";
        }
        if (chefNames.isEmpty()){
            return "No chef present (waiters " + waiterNames.size() + ")";
        }
        if (waiterNames.isEmpty()){
            return "No waiter present (chef " + chefNames.size() + ")";
        }
        return "";
    }
    public synchronized void open(){
        if (status == Status.OPEN) throw new IllegalStateException("Restaurant Already open");
        if(!canOpen()){throw new IllegalStateException("Restaurant not open: " + openBlockedReason());}

        kitchen = Executors.newFixedThreadPool(chefNames.size());
        service = Executors.newFixedThreadPool(waiterNames.size());

        for (String name : chefNames) {
            kitchen.execute(new Chef(name, orderQueue, completedOrderQueue, inventory, orderService ));
        }
        for(String name : waiterNames){
            service.execute(new Waiter(name,completedOrderQueue, orderService ));
        }
        status = Status.OPEN;
        System.out.println("Restaurant is OPEN with " + chefNames.size() + " chefs and " + waiterNames.size() + " waiters.");
    }

    public static void runMenu(Scanner sc, Restaurant restaurant) throws InterruptedException {
        {
            boolean running = true;
            while(running){
                System.out.println("""

                === RESTAURANT ===
                1) Hire chef        2) Hire waiter
                3) Add customer order
                4) Open restaurant  5) Status
                6) Close restaurant 7) Load customers from CSV
                0) Exit""");
                switch (sc.nextLine().trim()) {
                    case "1" -> hire(sc, restaurant, true);
                    case "2" -> hire(sc, restaurant, false);
                    case "3" -> addOrder(sc, restaurant);
                    case "4" -> tryOpen(restaurant);
                    case "5" -> restaurant.printStatus();
                    case "6" -> restaurant.close();
                    case "7" -> loadCustomersFromCsv(restaurant);
                    case "0" -> {restaurant.close() ; restaurant.abandonWaitingCustomers(); running = false;}
                    default -> System.out.println("Invalid input");

                }
            }
        }
    }
    public synchronized boolean isTaken(String name){
        return chefNames.stream().anyMatch(chef->chef.equalsIgnoreCase(name))||waiterNames.stream().anyMatch(waiter->waiter.equalsIgnoreCase(name));
    }

    public static void hire(Scanner sc, Restaurant restaurant, boolean chef) {
        String name = ask(sc, chef ? "Chef name: " : "Waiter name: ");
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty");
            return;
        }
        boolean added = chef ? restaurant.chefNamesAdd(name) : restaurant.waiterNamesAdd(name);
        if (!added) {
            System.out.println("'" + name + "' could not be hired (name already used, or the restaurant is open)");
        }
    }

    public static void loadCustomersFromCsv(Restaurant restaurant) {
        List<CsvLoader.CustomerRow> rows;
        try {
            rows = CsvLoader.loadCustomers(CsvLoader.CUSTOMERS_FILE);
        } catch (RuntimeException e) {
            System.out.println("Could not load customers: " + e.getMessage());
            return;
        }

        for (CsvLoader.CustomerRow row : rows) {
            OrderTicket ticket = restaurant.placeOrder(
                    row.customer(), Map.of(row.item(), row.quantity()), row.priority(), true);
            System.out.printf("Queued Order-%d for %-12s %s x%d%s%n",
                    ticket.orderId(), row.customer(), row.item().getLabel(), row.quantity(),
                    row.priority() == Priority.VIP ? " [VIP]" : "");
        }

        System.out.println(restaurant.isOpen()
                ? "Loaded " + rows.size() + " orders from CSV; the kitchen is already working on them."
                : "Loaded " + rows.size() + " orders from CSV. Choose 4) Open restaurant to start cooking "
                  + "(loading before opening is what keeps VIP orders first).");
    }

    public synchronized void abandonWaitingCustomers() {
        int abandoned = 0;
        for (Thread t : List.copyOf(customerThreads)) {
            if (t.isAlive()) {
                System.out.println(t.getName() + " left without being served.");
                t.interrupt();
                abandoned++;
            }
        }
        for (Thread t : List.copyOf(customerThreads)) {
            try {
                t.join(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        customerThreads.clear();
        if (abandoned > 0) {
            System.out.println(abandoned + " order(s) were never cooked; they stay PLACED in the database.");
        }
    }

    public void printStatus() {
        String state;
        int chefs, waiters, queued;
        synchronized (this) {
            state = status.toString();
            chefs = chefNames.size();
            waiters = waiterNames.size();
            queued = orderQueue.size();
        }
        System.out.printf("%nStatus: %s | Chefs: %d | Waiters: %d | Waiting in kitchen queue: %d%n",
                state, chefs, waiters, queued);
        if (!isOpen() && !canOpen()) {
            System.out.println("Cannot open: " + openBlockedReason());
        }

        List<OrderRecord> orders;
        try {
            orders = orderService.findAll();
        } catch (RuntimeException e) {
            System.out.println("Could not read orders: " + e.getMessage());
            return;
        }
        if (orders.isEmpty()) {
            System.out.println("No orders yet.");
            return;
        }

        StringBuilder counts = new StringBuilder();
        for (OrderStatus s : OrderStatus.values()) {
            long n = orders.stream().filter(o -> o.getStatus() == s).count();
            if (n > 0) counts.append(counts.isEmpty() ? "" : ", ").append(s).append(" ").append(n);
        }
        System.out.println("Orders: " + orders.size() + " total  (" + counts + ")");

        System.out.printf("  %-5s %-12s %-28s %-7s %-13s %-8s %-8s %-6s%n",
                "ID", "Customer", "Items", "Prio", "Status", "Chef", "Waiter", "Took");
        for (OrderRecord o : orders) {
            System.out.printf("  %-5d %-12s %-28s %-7s %-13s %-8s %-8s %-6s%n",
                    o.getOrderId(),
                    o.getCustomerId(),
                    o.getItems(),
                    o.getPriority(),
                    o.getStatus(),
                    o.getChef() == null ? "-" : o.getChef(),
                    o.getWaiter() == null ? "-" : o.getWaiter(),
                    elapsed(o));
        }
    }

    private static String elapsed(OrderRecord order) {
        Instant end = order.getDeliveredAt() != null ? order.getDeliveredAt() : order.getPreparedAt();
        if (order.getPlacedAt() == null || end == null) {
            return "-";
        }
        return String.format("%.1fs", Duration.between(order.getPlacedAt(), end).toMillis() / 1000.0);
    }

    public static void tryOpen(Restaurant restaurant){
        if (!restaurant.canOpen()){
            System.out.println("Restaurant not open: " +  restaurant.openBlockedReason());
            System.out.println("You need least 1 chef and 1 waiter to open restaurant. ");
            return;
        }
        restaurant.open();
    }

    private synchronized void close() throws InterruptedException {
        if(status == Status.CLOSED){
            return;
        }
        for (Thread t : List.copyOf(customerThreads)) {
            t.join(30_000);
            if (t.isAlive()) {
                System.out.println("Warning: " + t.getName() + " was never served; giving up on it.");
                t.interrupt();
            }
        }
        customerThreads.clear();

        for(int i = 0; i < chefNames.size(); i++){
            orderQueue.put(new ShutDown());
        }
        kitchen.shutdown();
        if(!kitchen.awaitTermination(10, TimeUnit.SECONDS)){
            kitchen.shutdownNow();
        }
        for(int i = 0; i < waiterNames.size(); i++){
            completedOrderQueue.put(new ShutDown());
        }
        service.shutdown();
        if(!service.awaitTermination(10, TimeUnit.SECONDS)){
            service.shutdownNow();
        }
        status = Status.CLOSED;
        System.out.println("Restaurant is CLOSED.");
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        close();
        abandonWaitingCustomers();
    }

    public static String ask(Scanner sc, String prompt) {
        System.out.println(prompt);
        return sc.nextLine().trim();
    }

    public static void addOrder(Scanner sc, Restaurant restaurant) throws InterruptedException {
        if (!restaurant.isOpen()){
            System.out.println("Restaurant is closed. Open it first (needs at least 1 chef and 1 waiter)." );
            return;
        }
        String customerName =  ask(sc, "Customer Names: ");
        if(customerName.isEmpty()){
            System.out.println("Customer Names cannot be empty");
            return;
        }
        MenuItems[] menuItems = MenuItems.values();
        for(int i = 0; i < menuItems.length; i++){
            System.out.printf("  %d) %-9s (%.1fs each)%n", i + 1, menuItems[i].getLabel(), menuItems[i].getCookTimeS() / 1000.0);
        }

        System.out.println("Enter items as <item number>x<quantity>, e.g. 1x2");
        System.out.println("Several at once with commas (1x2, 4x3), or one per line. Blank line finishes.");

        Map<MenuItems, Integer> cart = new EnumMap<>(MenuItems.class);
        while (true) {
            String line = ask(sc, "Items: ");
            if (line.isEmpty()) break;
            addToCart(line, cart, menuItems, restaurant.inventory);
            if (!cart.isEmpty()) System.out.println("  Cart: " + cartSummary(cart));
        }
        if (cart.isEmpty()) {
            System.out.println("No items chosen - order cancelled.");
            return;
        }

        Priority priority = ask(sc, "VIP? y/n" ).equalsIgnoreCase("y") ? Priority.VIP : Priority.NORMAL;
        OrderTicket orderTicket = restaurant.placeOrder(customerName, cart, priority, false);
        System.out.println("Order -  " + orderTicket.orderId() + "is sent to Kitchen");
        orderTicket.thread.join(120000);
        if (orderTicket.thread.isAlive()) {
            System.out.println("Still in progress - back to menu . Process will be finishing in background");
        }else {
            System.out.println("Order - " + orderTicket.orderId() + " completed going back to main menu\n");
        }
        orderTicket.thread.sleep(150);
    }

    private static void addToCart(String line, Map<MenuItems, Integer> cart, MenuItems[] menuItems,
                                  Inventory inventory) {
        for (String token : line.split(",")) {
            String entry = token.trim();
            if (entry.isEmpty()) continue;

            String[] parts = entry.split("[xX*]");
            Integer choice = parts.length > 0 ? parsePositive(parts[0].trim(), menuItems.length) : null;
            Integer quantity = parts.length == 1 ? 1
                    : (parts.length == 2 ? parsePositive(parts[1].trim(), 20) : null);

            if (choice == null || quantity == null) {
                System.out.println("  Skipped '" + entry + "' - use <item number>x<quantity>, e.g. 1x2"
                        + " (item 1-" + menuItems.length + ", quantity 1-20)");
                continue;
            }

            MenuItems item = menuItems[choice - 1];
            int available = inventory.getStock(item);
            int inCart = cart.getOrDefault(item, 0);
            if (inCart + quantity > available) {
                System.out.printf("  Only %d %s left%s - adjust the quantity or pick another item%n",
                        available, item.getLabel(),
                        inCart > 0 ? " (you already have " + inCart + " in the cart)" : "");
                continue;
            }
            cart.merge(item, quantity, Integer::sum);
        }
    }

    private static String cartSummary(Map<MenuItems, Integer> cart) {
        return cart.entrySet().stream()
                .map(e -> e.getKey().getLabel() + " x" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    private static Integer parsePositive(String task, int max) {
        try{
            int value = Integer.parseInt(task);
            return (value >= 1 && value <= max)? value : null;
        }catch (NumberFormatException e){
            return  null;
        }
    }

    public record OrderTicket(int orderId, Thread thread){
    }

    private synchronized OrderTicket placeOrder(String customerName, Map<MenuItems, Integer> lines, Priority priority ,boolean allowWhileClosed) {
        synchronized (this){
            if (status == Status.CLOSED && !allowWhileClosed){
                throw new IllegalStateException("Restaurant is closed");
            }
            if (lines.isEmpty()){
                throw new IllegalArgumentException("An order needs at least one item");
            }
            int orderId = nextOrderId.getAndIncrement();
            Customer customer = new Customer(customerName, orderId, priority, lines, orderQueue);

            orderService.save(customer.createOrder());

            Thread t =  new Thread(customer, "Customer-" + orderId);
            customerThreads.add(t);
            t.start();
            return new OrderTicket(orderId, t);
        }
    }
}
