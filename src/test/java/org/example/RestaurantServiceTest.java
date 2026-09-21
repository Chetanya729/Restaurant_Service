package org.example;

import jakarta.persistence.EntityManagerFactory;
import org.example.Domain.Inventory;
import org.example.Domain.MenuItems;
import org.example.Domain.OrderStatus;
import org.example.Domain.Priority;
import org.example.Entity.OrderRecord;
import org.example.Repository.OrderRepo;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end: customers order, chefs cook, waiters deliver, everything lands in the database. */
class RestaurantServiceTest {

    private static EntityManagerFactory emf;
    private OrderRepo orderRepo;
    private Inventory inventory;
    private Restaurant restaurant;

    @BeforeAll
    static void startDatabase() {
        emf = TestDatabase.inMemory();
    }

    @AfterAll
    static void stopDatabase() {
        emf.close();
    }

    @BeforeEach
    void openRestaurant() {
        orderRepo = new OrderRepo(emf);
        inventory = new Inventory();
        restaurant = new Restaurant(inventory, orderRepo);
        restaurant.hireChef("Chef-A");
        restaurant.hireWaiter("Waiter-B");
        restaurant.open();
    }

    @AfterEach
    void closeRestaurant() throws InterruptedException {
        restaurant.close();
    }

    @Test
    void servesOrdersAndRecordsThem() throws InterruptedException {
        int first = restaurant.placeOrder("Alice", MenuItems.SANDWICH, 1, Priority.NORMAL);
        int second = restaurant.placeOrder("Bob", MenuItems.SANDWICH, 1, Priority.VIP);

        restaurant.awaitAllServed();

        assertEquals(0, inventory.getStock(MenuItems.SANDWICH), "2 in stock, 2 sold");

        List<OrderRecord> records = orderRepo.findAll().stream()
                .filter(r -> r.getOrderId() == first || r.getOrderId() == second)
                .toList();
        assertEquals(2, records.size());
        for (OrderRecord record : records) {
            assertEquals(OrderStatus.DELIVERED, record.getStatus());
            assertEquals("Chef-A", record.getChef());
            assertEquals("Waiter-B", record.getWaiter());
            assertNotNull(record.getPlacedAt());
            assertNotNull(record.getDeliveredAt());
        }
    }

    @Test
    void marksOrderOutOfStockWhenInventoryIsTooLow() throws InterruptedException {
        int orderId = restaurant.placeOrder("Greedy", MenuItems.SANDWICH, 5, Priority.NORMAL);

        restaurant.awaitAllServed();

        OrderRecord record = orderRepo.findAll().stream()
                .filter(r -> r.getOrderId() == orderId)
                .findFirst()
                .orElseThrow();
        assertEquals(OrderStatus.OUT_OF_STOCK, record.getStatus());
        assertEquals(2, inventory.getStock(MenuItems.SANDWICH), "stock untouched when the order fails");
        assertEquals("Waiter-B", record.getWaiter(), "the customer is still told");
    }

    @Test
    void ordersGetUniqueIncreasingIds() {
        int first = restaurant.placeOrder("Alice", MenuItems.PIZZA, 1, Priority.NORMAL);
        int second = restaurant.placeOrder("Bob", MenuItems.PIZZA, 1, Priority.NORMAL);

        assertTrue(second > first, second + " should be greater than " + first);
    }

    @Test
    void quantityMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> restaurant.placeOrder("Alice", MenuItems.PIZZA, 0, Priority.NORMAL));
    }
}
