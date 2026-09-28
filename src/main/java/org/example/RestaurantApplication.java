package org.example;

import jakarta.persistence.EntityManagerFactory;
import org.example.Domain.Inventory;
import org.example.Repository.OrderRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Scanner;

@SpringBootApplication
public class RestaurantApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantApplication.class, args);
    }
    @Bean
    CommandLineRunner menu(EntityManagerFactory entityManagerFactory) {
        return args -> {
            Scanner sc = new Scanner(System.in);
            Restaurant restaurant = new Restaurant(new Inventory(), new OrderRepo(entityManagerFactory));
            Restaurant.runMenu(sc, restaurant);
        };
    }
}
