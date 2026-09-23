package org.example;

import jakarta.persistence.EntityManagerFactory;
import org.example.Domain.*;
import org.example.Repository.Database;
import org.example.Repository.OrderRepo;
import org.example.Service.Chef;
import org.example.Service.Waiter;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Restaurant {

    public Restaurant(Inventory inventory, OrderRepo orderRepo) {
        this(inventory, orderRepo,
                new PriorityBlockingQueue<>(20,
                        Comparator.comparing(Order::getPriority).thenComparingInt(Order::getOrderId)),
                new LinkedBlockingQueue<>());
    }

    public Restaurant(Inventory inventory, OrderRepo orderRepo, BlockingQueue<Order> orderQueue, BlockingQueue<Order> completedOrderQueue) {
        this.inventory = inventory;
        this.orderRepo = orderRepo;
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
    private final OrderRepo orderRepo;
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
            kitchen.execute(new Chef(name, orderQueue, completedOrderQueue, inventory, orderRepo ));
        }
        for(String name : waiterNames){
            service.execute(new Waiter(name,completedOrderQueue, orderRepo ));
        }
        status = Status.OPEN;
        System.out.println("Restaurant is OPEN with " + chefNames.size() + " chefs and " + waiterNames.size() + " waiters.");
    }

    public static void main(String[] args) throws InterruptedException {
        EntityManagerFactory emf = Database.open();
        Scanner sc  = new Scanner(System.in);
        Restaurant restaurant = new Restaurant(new Inventory(), new OrderRepo(emf));
        try {
            boolean running = true;
            while(running){
                System.out.println("""

                === RESTAURANT ===
                1) Hire chef        2) Hire waiter
                3) Add customer order
                4) Open restaurant  5) Status
                6) Close restaurant 0) Exit""");
                switch (sc.nextLine().trim()) {
                    case "1" -> hire(sc, restaurant, true);
                    case "2" -> hire(sc, restaurant, false);
                    case "3" -> addOrder(sc, restaurant);
                    case "4" -> tryOpen(restaurant);
                    case "5" -> restaurant.printStatus();
                    case "6" -> restaurant.close();
                    case "0" -> {restaurant.close() ; running = false;}
                    default -> System.out.println("Invalid input");

                }
            }
        }finally {
            emf.close();
        }
    }
    public synchronized boolean isTaken(String name){
        return chefNames.stream().anyMatch(chef->chef.equalsIgnoreCase(name))||waiterNames.stream().anyMatch(waiter->waiter.equalsIgnoreCase(name));
    }

    public static void hire(Scanner sc, Restaurant restaurant, boolean chef) {
        String name = ask(sc, chef ? "Chef name: " : "Waiter name: ");
        boolean added = chef ? restaurant.chefNamesAdd(name) : restaurant.waiterNamesAdd(name);
        if (!added) System.out.println( name + "' is already working here");
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty");
            return;
        }
        if (chef) {
            restaurant.chefNamesAdd(name);
        } else {
            restaurant.waiterNamesAdd(name);
        }
    }

    public synchronized void printStatus() {
        System.out.printf("Status: %s | Chefs: %d | Waiters: %d | Orders waiting: %d%n",
                status, chefNames.size(), waiterNames.size(), orderQueue.size());
        if (status == Status.CLOSED && !canOpen()) System.out.println("Cannot open: " + openBlockedReason());
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

        Integer choice = parsePositive(ask(sc, "Item number: "), menuItems.length);
        if (choice == null) {
            System.out.println("Pick a number between 1 and " + menuItems.length + ".");
            return;
        }
        MenuItems item = menuItems[choice - 1];

        Integer quantity = parsePositive(ask(sc, "Quantity: "), 20);
        if (quantity == null) {
            System.out.println("Quantity must be a number between 1 and 20.");
            return;
        }
        Priority priority = ask(sc, "VIP? y/n" ).equalsIgnoreCase("y") ? Priority.VIP : Priority.NORMAL;
        OrderTicket orderTicket = restaurant.placeOrder(customerName, item, quantity, priority, false);
        System.out.println("Order -  " + orderTicket.orderId() + "is sent to Kitchen");
        orderTicket.thread.join(120000);
        if (orderTicket.thread.isAlive()) {
            System.out.println("Still in progress - back to menu . Process will be finishing in background");
        }else {
            System.out.println("Order - " + orderTicket.orderId() + " completed going back to main menu\n");
        }
        orderTicket.thread.sleep(150);
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

    private synchronized OrderTicket placeOrder(String customerName, MenuItems item, Integer quantity, Priority priority ,boolean allowWhileClosed) {
        synchronized (this){
            if (status == Status.CLOSED){
                throw new IllegalStateException("Restaurant is closed");
            }
            int orderId = nextOrderId.getAndIncrement();
            Customer customer = new Customer(customerName, orderId, priority, quantity, item, orderQueue, orderRepo);
            Thread t =  new Thread(customer, "Customer-" + orderId);
            customerThreads.add(t);
            t.start();
            return new OrderTicket(orderId, t);
        }
    }
}
