package org.example;


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
    CommandLineRunner menu(Restaurant restaurant) {
        return args -> Restaurant.runMenu(new Scanner(System.in), restaurant);
    }
}
