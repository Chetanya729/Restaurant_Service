package org.example.Service;

import org.example.Domain.Inventory;
import org.example.Domain.MenuItems;
import org.example.Domain.UnsafeInventory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class RaceDemo {
    private static final int Chefs = 100;

    public static void main(String[] args) throws InterruptedException {
        UnsafeInventory unsafe = new UnsafeInventory();
        race("WITHOUT synchronization", () -> unsafe.reserve(MenuItems.PIZZA, 1), () -> unsafe.getStock(MenuItems.PIZZA));

        Inventory safe = new Inventory();
        race("WITH synchronization", () -> safe.reserve(MenuItems.PIZZA, 1), () -> safe.getStock(MenuItems.PIZZA));
    }

    private static void race(String label, BooleanSupplier reserve, IntSupplier stock) throws InterruptedException {
        int stockAtStart = stock.getAsInt();

        CountDownLatch startsignal= new CountDownLatch(1);
        CountDownLatch endsignal= new CountDownLatch(Chefs);
        AtomicInteger success = new AtomicInteger();

        for(int i = 0; i < Chefs; i++){
            new Thread(() -> {
                try {
                    startsignal.await();
                    if (reserve.getAsBoolean()){
                        success.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                   Thread.currentThread().interrupt();
                }finally {
                    endsignal.countDown();
                }
            }).start();
        }
        startsignal.countDown();
        endsignal.await();

        int left = stock.getAsInt();
        System.out.println("== " + label + " ==");
        System.out.println("Stock at start   : " + stockAtStart);
        System.out.println("Reservations ok  : " + success.get());
        System.out.println("Stock at end     : " + left);
        System.out.println("Sold + left      : " + (success.get() + left)
                + (success.get() + left == stockAtStart ? "  (consistent)" : "  (RACE: should be " + stockAtStart + ")"));
    }
}
