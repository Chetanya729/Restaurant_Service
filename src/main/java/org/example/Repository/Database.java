package org.example.Repository;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public final class Database {
    private Database(){

    }
    public static EntityManagerFactory open(){
        System.setProperty("org.jboss.logging.provider", "slf4j");
        return Persistence.createEntityManagerFactory("restaurant");
    }
}
