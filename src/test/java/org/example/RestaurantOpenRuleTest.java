package org.example;

import jakarta.persistence.EntityManagerFactory;
import org.example.Domain.Inventory;
import org.example.Domain.MenuItems;
import org.example.Domain.Priority;
import org.example.Repository.OrderRepo;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/** The rule: the restaurant opens only when at least one chef AND one waiter are available. */
class RestaurantOpenRuleTest {

    private static EntityManagerFactory emf;
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
    void freshRestaurant() {
        restaurant = new Restaurant(new Inventory(), new OrderRepo(emf));
    }

    @AfterEach
    void closeRestaurant() throws InterruptedException {
        restaurant.close();
    }

    @Test
    void startsClosed() {
        assertEquals(Restaurant.Status.CLOSED, restaurant.status());
        assertFalse(restaurant.isOpen());
    }

    @Test
    void cannotOpenWithNoStaff() {
        assertFalse(restaurant.canOpen());
        assertEquals("no chefs and no waiters", restaurant.openBlockedReason());
    }

    @Test
    void cannotOpenWithOnlyChefs() {
        restaurant.hireChef("Ravi");
        assertFalse(restaurant.canOpen());
        assertEquals("no waiters (chefs: 1)", restaurant.openBlockedReason());
    }

    @Test
    void cannotOpenWithOnlyWaiters() {
        restaurant.hireWaiter("Sam");
        restaurant.hireWaiter("Alia");
        assertFalse(restaurant.canOpen());
        assertEquals("no chefs (waiters: 2)", restaurant.openBlockedReason());
    }

    @Test
    void opensWithOneChefAndOneWaiter() {
        restaurant.hireChef("Ravi");
        restaurant.hireWaiter("Sam");

        assertTrue(restaurant.canOpen());
        assertEquals("", restaurant.openBlockedReason());

        restaurant.open();

        assertTrue(restaurant.isOpen());
        assertEquals(Restaurant.Status.OPEN, restaurant.status());
    }

    @Test
    void openingWithoutStaffThrows() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> restaurant.open());
        assertTrue(error.getMessage().contains("no chefs and no waiters"), error.getMessage());
    }

    @Test
    void openingTwiceThrows() {
        restaurant.hireChef("Ravi");
        restaurant.hireWaiter("Sam");
        restaurant.open();

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> restaurant.open());
        assertEquals("Restaurant is already open", error.getMessage());
    }

    @Test
    void ordersAreRejectedWhileClosed() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> restaurant.placeOrder("Alice", MenuItems.PIZZA, 1, Priority.NORMAL));
        assertTrue(error.getMessage().contains("CLOSED"), error.getMessage());
    }

    @Test
    void duplicateStaffNameIsRejected() {
        restaurant.hireChef("Ravi");
        assertThrows(IllegalArgumentException.class, () -> restaurant.hireChef("Ravi"));
        assertThrows(IllegalArgumentException.class, () -> restaurant.hireWaiter("Ravi"));
        assertEquals(1, restaurant.chefCount());
    }

    @Test
    void blankStaffNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> restaurant.hireChef("   "));
        assertThrows(IllegalArgumentException.class, () -> restaurant.hireWaiter(null));
        assertEquals(0, restaurant.chefCount());
    }

    @Test
    void firingUnknownStaffIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> restaurant.fireChef("Nobody"));
    }

    @Test
    void firingTheLastChefClosesTheRestaurant() throws InterruptedException {
        restaurant.hireChef("Ravi");
        restaurant.hireWaiter("Sam");
        restaurant.open();

        restaurant.fireChef("Ravi");

        assertEquals(Restaurant.Status.CLOSED, restaurant.status());
        assertEquals(0, restaurant.chefCount());
        assertEquals(1, restaurant.waiterCount());
        assertEquals("no chefs (waiters: 1)", restaurant.openBlockedReason());
    }

    @Test
    void firingTheLastWaiterClosesTheRestaurant() throws InterruptedException {
        restaurant.hireChef("Ravi");
        restaurant.hireWaiter("Sam");
        restaurant.open();

        restaurant.fireWaiter("Sam");

        assertEquals(Restaurant.Status.CLOSED, restaurant.status());
        assertFalse(restaurant.canOpen());
    }

    @Test
    void staffCanBeHiredWhileOpen() {
        restaurant.hireChef("Ravi");
        restaurant.hireWaiter("Sam");
        restaurant.open();

        restaurant.hireChef("Alia");

        assertTrue(restaurant.isOpen());
        assertEquals(2, restaurant.chefCount());
        assertTrue(restaurant.chefs().contains("Alia"));
    }

    @Test
    void closingTwiceIsSafe() throws InterruptedException {
        restaurant.hireChef("Ravi");
        restaurant.hireWaiter("Sam");
        restaurant.open();

        restaurant.close();
        restaurant.close();

        assertEquals(Restaurant.Status.CLOSED, restaurant.status());
    }

    @Test
    void canReopenAfterClosing() throws InterruptedException {
        restaurant.hireChef("Ravi");
        restaurant.hireWaiter("Sam");
        restaurant.open();
        restaurant.close();

        restaurant.open();

        assertTrue(restaurant.isOpen());
    }
}
