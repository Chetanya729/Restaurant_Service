package org.example;

import jakarta.persistence.EntityManagerFactory;
import org.example.Domain.Inventory;
import org.example.Domain.Role;
import org.example.Repository.OrderRepo;
import org.example.Repository.StaffRepo;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Hired staff survive a restart, because they live in the database. */
class StaffRepoTest {

    private static EntityManagerFactory emf;
    private StaffRepo staffRepo;

    @BeforeAll
    static void startDatabase() {
        emf = TestDatabase.inMemory();
    }

    @AfterAll
    static void stopDatabase() {
        emf.close();
    }

    @BeforeEach
    void freshRepo() {
        staffRepo = new StaffRepo(emf);
    }

    @Test
    void hiredStaffIsStored() {
        staffRepo.hire("Ravi", Role.CHEF);
        staffRepo.hire("Sam", Role.WAITER);

        assertTrue(staffRepo.findActiveNames(Role.CHEF).contains("Ravi"));
        assertTrue(staffRepo.findActiveNames(Role.WAITER).contains("Sam"));
    }

    @Test
    void firedStaffIsDeactivatedNotDeleted() {
        staffRepo.hire("Temp", Role.CHEF);
        staffRepo.fire("Temp", Role.CHEF);

        assertFalse(staffRepo.findActiveNames(Role.CHEF).contains("Temp"));
        assertTrue(staffRepo.findAll().stream().anyMatch(s -> s.getName().equals("Temp") && !s.isActive()),
                "the row stays so past orders keep their history");
    }

    @Test
    void rehiringReactivatesTheSameRow() {
        staffRepo.hire("Boomerang", Role.WAITER);
        staffRepo.fire("Boomerang", Role.WAITER);
        staffRepo.hire("Boomerang", Role.WAITER);

        assertTrue(staffRepo.findActiveNames(Role.WAITER).contains("Boomerang"));
        assertEquals(1, staffRepo.findAll().stream().filter(s -> s.getName().equals("Boomerang")).count());
    }

    @Test
    void restaurantLoadsStaffFromTheDatabase() {
        staffRepo.hire("Chef-Persist", Role.CHEF);
        staffRepo.hire("Waiter-Persist", Role.WAITER);

        // a brand new Restaurant, as if the program had been restarted
        Restaurant restarted = new Restaurant(new Inventory(), new OrderRepo(emf), staffRepo);
        restarted.loadStaff();

        List<String> chefs = restarted.chefs();
        assertTrue(chefs.contains("Chef-Persist"), chefs.toString());
        assertTrue(restarted.waiters().contains("Waiter-Persist"));
        assertTrue(restarted.canOpen());
    }
}
