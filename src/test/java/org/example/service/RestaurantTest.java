package org.example.service;

import org.example.Domain.Inventory;
import org.example.Domain.MenuItems;
import org.example.Restaurant;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RestaurantTest {

    private Restaurant Newrestaurant(){
        return new Restaurant(new Inventory(), null);
    }

    @Test
    void cannoyOpenwithNoStaff(){
        Restaurant r = new Restaurant(new Inventory(), null);
        assertFalse(r.canOpen());
        assertEquals("No chef or waiter", r.openBlockedReason());
    }

    @Test
    void cannotOpenWithOnlyWaiters() {
        Restaurant r = Newrestaurant();
        r.waiterNamesAdd("Asha");
        assertFalse(r.canOpen());
        assertTrue(r.openBlockedReason().contains("No chef"));
    }

    @Test
    void cannotOpenWithOnlyChefs() {
        Restaurant r = Newrestaurant();
        r.chefNamesAdd("Ravi");
        assertFalse(r.canOpen());
        assertTrue(r.openBlockedReason().contains("No waiter"));
    }

    @Test
    void canOpenWithOneWaitersAndOneChefs() {
        Restaurant r = Newrestaurant();
        r.waiterNamesAdd("Asha");
        r.chefNamesAdd("Ravi");
        assertTrue(r.canOpen());        // allowed to open...
        assertFalse(r.isOpen());        // ...but open() has not been called yet
    }

    @Test
    void inventoryDeductsExactQuantity(){
        Inventory inv = new Inventory();
        int before = inv.getStock(MenuItems.PIZZA);
        assertTrue(inv.reserve(MenuItems.PIZZA, 2));
        assertEquals(before - 2, inv.getStock(MenuItems.PIZZA));
    }

    @Test
    void reserveFailsWhenStockIsTooLow(){
        Inventory inv = new Inventory();
        int before = inv.getStock(MenuItems.BURGER);
        assertFalse(inv.reserve(MenuItems.BURGER, before + 1));
        assertEquals(before, inv.getStock(MenuItems.BURGER));   // nothing deducted
    }

    @Test
    void multiItemReservationIsAllOrNothing(){
        Inventory inv = new Inventory();
        int pizzaBefore = inv.getStock(MenuItems.PIZZA);
        int sandwichBefore = inv.getStock(MenuItems.SANDWICH);

        Map<MenuItems, Integer> lines = new EnumMap<>(MenuItems.class);
        lines.put(MenuItems.PIZZA, 1);
        lines.put(MenuItems.SANDWICH, sandwichBefore + 1);      // impossible line

        assertFalse(inv.reserveAll(lines));
        assertEquals(pizzaBefore, inv.getStock(MenuItems.PIZZA));          // pizza untouched
        assertEquals(sandwichBefore, inv.getStock(MenuItems.SANDWICH));
    }

    @Test
    void duplicateStaffNameIsRejected(){
        Restaurant r = Newrestaurant();
        assertTrue(r.chefNamesAdd("Ravi"));
        assertFalse(r.chefNamesAdd("ravi"));        // same name, different case
        assertFalse(r.waiterNamesAdd("Ravi"));      // cannot be a waiter too
        assertEquals(1, r.chefCount());
        assertEquals(0, r.waiterCount());
    }
}
