package org.example.service;

import org.example.Domain.Inventory;
import org.example.Domain.MenuItems;
import org.example.Restaurant;
import org.junit.jupiter.api.Test;

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
        assertTrue(r.isOpen());
        assertFalse(r.canOpen());
    }

    @Test
    void inventoryDeductsExactQuantity(){
        Inventory inv = new Inventory();
        int before = inv.getStock(MenuItems.PIZZA);
    }
}
