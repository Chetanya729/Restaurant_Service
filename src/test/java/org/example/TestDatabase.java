package org.example;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Gives every test class its own throw-away in-memory database. */
final class TestDatabase {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private TestDatabase() {
    }

    static EntityManagerFactory inMemory() {
        System.setProperty("org.jboss.logging.provider", "slf4j");
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("jakarta.persistence.jdbc.url",
                "jdbc:h2:mem:test" + COUNTER.incrementAndGet() + ";DB_CLOSE_DELAY=-1");
        overrides.put("hibernate.hbm2ddl.auto", "create-drop");
        overrides.put("hibernate.show_sql", "false");
        return Persistence.createEntityManagerFactory("restaurant", overrides);
    }
}
