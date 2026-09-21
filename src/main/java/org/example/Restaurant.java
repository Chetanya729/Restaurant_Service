package org.example;

import org.example.Domain.*;
import org.example.Repository.OrderRepo;
import org.example.Repository.StaffRepo;
import org.example.Service.Chef;
import org.example.Service.Waiter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owns the staff, the queues and the worker threads.
 * The restaurant can only open when at least one chef AND one waiter are available.
 */
public class Restaurant {

    public enum Status {
        CLOSED,
        CLOSING,
        OPEN
    }

    private final Inventory inventory;
    private final OrderRepo orderRepo;
    private final StaffRepo staffRepo;          // may be null (tests without staff persistence)

    private final BlockingQueue<Order> orderQueue = new PriorityBlockingQueue<>(20,
            Comparator.comparing(Order::getPriority).thenComparingInt(Order::getOrderId));
    private final BlockingQueue<Order> completedOrderQueue = new LinkedBlockingQueue<>();

    // all mutable state below is guarded by `this`
    private final List<String> chefNames = new ArrayList<>();
    private final List<String> waiterNames = new ArrayList<>();
    private final Map<String, Future<?>> chefTasks = new LinkedHashMap<>();
    private final Map<String, Future<?>> waiterTasks = new LinkedHashMap<>();
    private final List<Thread> customerThreads = new ArrayList<>();
    private Status status = Status.CLOSED;
    private ExecutorService kitchen;
    private ExecutorService service;

    private final AtomicInteger nextOrderId;

    public Restaurant(Inventory inventory, OrderRepo orderRepo) {
        this(inventory, orderRepo, null);
    }

    public Restaurant(Inventory inventory, OrderRepo orderRepo, StaffRepo staffRepo) {
        this.inventory = inventory;
        this.orderRepo = orderRepo;
        this.staffRepo = staffRepo;
        // continue after the highest order id already in the database
        this.nextOrderId = new AtomicInteger(orderRepo.nextOrderId());
    }

    // ---------------- staff ----------------

    /** Loads previously hired staff from the database, if staff persistence is enabled. */
    public synchronized void loadStaff() {
        if (staffRepo == null) {
            return;
        }
        chefNames.addAll(staffRepo.findActiveNames(Role.CHEF));
        waiterNames.addAll(staffRepo.findActiveNames(Role.WAITER));
    }

    public void hireChef(String name) {
        hire(name, Role.CHEF);
    }

    public void hireWaiter(String name) {
        hire(name, Role.WAITER);
    }

    private synchronized void hire(String rawName, Role role) {
        String name = validateName(rawName);
        if (chefNames.contains(name) || waiterNames.contains(name)) {
            throw new IllegalArgumentException(name + " already works here");
        }
        List<String> names = role == Role.CHEF ? chefNames : waiterNames;
        names.add(name);
        if (staffRepo != null) {
            staffRepo.hire(name, role);
        }
        // hiring while open: the new worker starts serving immediately
        if (status == Status.OPEN) {
            startWorker(name, role);
        }
        System.out.println("Hired " + role.name().toLowerCase() + " " + name
                + " (chefs: " + chefNames.size() + ", waiters: " + waiterNames.size() + ")");
    }

    public void fireChef(String name) throws InterruptedException {
        fire(name, Role.CHEF);
    }

    public void fireWaiter(String name) throws InterruptedException {
        fire(name, Role.WAITER);
    }

    /** Removes a staff member. If that leaves no chefs or no waiters, the restaurant closes. */
    private void fire(String rawName, Role role) throws InterruptedException {
        boolean mustClose;
        synchronized (this) {
            String name = validateName(rawName);
            List<String> names = role == Role.CHEF ? chefNames : waiterNames;
            if (!names.remove(name)) {
                throw new IllegalArgumentException("No " + role.name().toLowerCase() + " named " + name);
            }
            if (staffRepo != null) {
                staffRepo.fire(name, role);
            }
            Map<String, Future<?>> tasks = role == Role.CHEF ? chefTasks : waiterTasks;
            Future<?> task = tasks.remove(name);
            if (task != null) {
                task.cancel(true);   // interrupts take(), the worker's catch block exits cleanly
            }
            System.out.println(name + " left (chefs: " + chefNames.size() + ", waiters: " + waiterNames.size() + ")");
            mustClose = status == Status.OPEN && !canOpen();
        }
        if (mustClose) {
            System.out.println("Restaurant must close: " + openBlockedReason());
            close();
        }
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        return name.trim();
    }

    public synchronized int chefCount() {
        return chefNames.size();
    }

    public synchronized int waiterCount() {
        return waiterNames.size();
    }

    public synchronized List<String> chefs() {
        return List.copyOf(chefNames);
    }

    public synchronized List<String> waiters() {
        return List.copyOf(waiterNames);
    }

    // ---------------- open / closed ----------------

    public synchronized boolean canOpen() {
        return !chefNames.isEmpty() && !waiterNames.isEmpty();
    }

    public synchronized String openBlockedReason() {
        if (chefNames.isEmpty() && waiterNames.isEmpty()) {
            return "no chefs and no waiters";
        }
        if (chefNames.isEmpty()) {
            return "no chefs (waiters: " + waiterNames.size() + ")";
        }
        if (waiterNames.isEmpty()) {
            return "no waiters (chefs: " + chefNames.size() + ")";
        }
        return "";
    }

    public synchronized Status status() {
        return status;
    }

    public synchronized boolean isOpen() {
        return status == Status.OPEN;
    }

    public synchronized void open() {
        if (status == Status.OPEN) {
            throw new IllegalStateException("Restaurant is already open");
        }
        if (!canOpen()) {
            throw new IllegalStateException("Cannot open: " + openBlockedReason());
        }
        // cached pools so staff can be hired while the restaurant is open
        kitchen = Executors.newCachedThreadPool();
        service = Executors.newCachedThreadPool();

        for (String name : chefNames) {
            startWorker(name, Role.CHEF);
        }
        for (String name : waiterNames) {
            startWorker(name, Role.WAITER);
        }
        status = Status.OPEN;
        System.out.println("Restaurant is OPEN with " + chefNames.size() + " chefs and "
                + waiterNames.size() + " waiters.");
    }

    /** Caller must hold the lock. */
    private void startWorker(String name, Role role) {
        if (role == Role.CHEF) {
            chefTasks.put(name, kitchen.submit(
                    new Chef(name, orderQueue, completedOrderQueue, inventory, orderRepo)));
        } else {
            waiterTasks.put(name, service.submit(
                    new Waiter(name, completedOrderQueue, orderRepo)));
        }
    }

    /** Waits for orders in progress, stops every worker, then marks the restaurant closed. */
    public void close() throws InterruptedException {
        List<Thread> waitingCustomers;
        int chefWorkers;
        int waiterWorkers;
        ExecutorService kitchenPool;
        ExecutorService servicePool;

        synchronized (this) {
            if (status != Status.OPEN) {
                return;
            }
            status = Status.CLOSING;          // no new orders from here on
            waitingCustomers = new ArrayList<>(customerThreads);
            chefWorkers = chefTasks.size();
            waiterWorkers = waiterTasks.size();
            kitchenPool = kitchen;
            servicePool = service;
        }

        System.out.println("\nRestaurant is closing...");
        for (Thread t : waitingCustomers) {
            t.join();                          // every customer has been served
        }
        System.out.println("All orders processed.");

        // one poison pill per running worker
        for (int i = 0; i < chefWorkers; i++) {
            orderQueue.put(new ShutDown());
        }
        kitchenPool.shutdown();
        if (!kitchenPool.awaitTermination(10, TimeUnit.SECONDS)) {
            kitchenPool.shutdownNow();
        }
        System.out.println("All chefs stopped.");

        for (int i = 0; i < waiterWorkers; i++) {
            completedOrderQueue.put(new ShutDown());
        }
        servicePool.shutdown();
        if (!servicePool.awaitTermination(10, TimeUnit.SECONDS)) {
            servicePool.shutdownNow();
        }
        System.out.println("All waiters stopped.");

        synchronized (this) {
            chefTasks.clear();
            waiterTasks.clear();
            customerThreads.clear();
            kitchen = null;
            service = null;
            status = Status.CLOSED;
        }
    }

    // ---------------- orders ----------------

    /** Starts one customer thread for this order. Only allowed while the restaurant is open. */
    public synchronized int placeOrder(String customerName, MenuItems item, int quantity, Priority priority) {
        if (status != Status.OPEN) {
            throw new IllegalStateException("Restaurant is " + status + ": "
                    + (canOpen() ? "open it first" : openBlockedReason()));
        }
        String name = validateName(customerName);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        int orderId = nextOrderId.getAndIncrement();
        Customer customer = new Customer(name, orderId, priority, quantity, item, orderQueue, orderRepo);
        Thread thread = new Thread(customer, "Customer-Thread-" + orderId);
        customerThreads.add(thread);
        thread.start();
        return orderId;
    }

    /** Waits until every order placed so far has been delivered or reported out of stock. */
    public void awaitAllServed() throws InterruptedException {
        List<Thread> threads;
        synchronized (this) {
            threads = new ArrayList<>(customerThreads);
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    public synchronized int ordersWaiting() {
        return orderQueue.size();
    }

    public synchronized int ordersAwaitingService() {
        return completedOrderQueue.size();
    }

    public Inventory inventory() {
        return inventory;
    }

    public OrderRepo orders() {
        return orderRepo;
    }

    public void printStatus() {
        String state;
        String chefList;
        String waiterList;
        String blocked;
        synchronized (this) {
            state = status.name();
            chefList = chefNames.isEmpty() ? "-" : String.join(", ", chefNames);
            waiterList = waiterNames.isEmpty() ? "-" : String.join(", ", waiterNames);
            blocked = openBlockedReason();
        }
        System.out.println("""

                === STATUS ===""");
        System.out.printf("  State            : %s%n", state);
        System.out.printf("  Chefs (%d)        : %s%n", chefCount(), chefList);
        System.out.printf("  Waiters (%d)      : %s%n", waiterCount(), waiterList);
        System.out.printf("  Orders waiting   : %d%n", ordersWaiting());
        System.out.printf("  Ready to serve   : %d%n", ordersAwaitingService());
        if (!blocked.isEmpty()) {
            System.out.println("  Cannot open: " + blocked + " (need at least 1 chef AND 1 waiter)");
        }
    }
}
