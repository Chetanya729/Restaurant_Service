package org.example;

import org.example.Domain.Inventory;
import org.example.Domain.MenuItems;
import org.example.Domain.UnsafeInventory;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {

    @Test
    void reserveDeductsTheRequestedQuantity() {
        Inventory inventory = new Inventory();

        assertTrue(inventory.reserve(MenuItems.PIZZA, 2));
        assertEquals(6, inventory.getStock(MenuItems.PIZZA));
    }

    @Test
    void reserveFailsWhenStockIsTooLow() {
        Inventory inventory = new Inventory();

        assertFalse(inventory.reserve(MenuItems.SANDWICH, 3), "only 2 sandwiches in stock");
        assertEquals(2, inventory.getStock(MenuItems.SANDWICH), "a failed reserve changes nothing");
    }

    @Test
    void concurrentReservesNeverOversell() throws InterruptedException {
        Inventory inventory = new Inventory();
        int stockAtStart = inventory.getStock(MenuItems.PIZZA);
        int threads = 100;

        AtomicInteger sold = concurrentReserve(threads, () -> inventory.reserve(MenuItems.PIZZA, 1));

        assertEquals(stockAtStart, sold.get(), "exactly the available stock is sold");
        assertEquals(0, inventory.getStock(MenuItems.PIZZA));
        assertEquals(stockAtStart, sold.get() + inventory.getStock(MenuItems.PIZZA));
    }

    @Test
    void unsafeInventoryShowsTheRaceCondition() throws InterruptedException {
        UnsafeInventory unsafe = new UnsafeInventory();
        int stockAtStart = unsafe.getStock(MenuItems.PIZZA);

        AtomicInteger sold = concurrentReserve(100, () -> unsafe.reserve(MenuItems.PIZZA, 1));

        assertTrue(sold.get() > stockAtStart,
                "without synchronization more is sold than exists: sold=" + sold.get() + ", stock=" + stockAtStart);
    }

    private static AtomicInteger concurrentReserve(int threads, java.util.function.BooleanSupplier reserve)
            throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger sold = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    if (reserve.getAsBoolean()) {
                        sold.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        done.await();
        return sold;
    }
}
