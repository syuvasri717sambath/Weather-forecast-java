package com.example.weather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching; // 1. Import pannirukku

@SpringBootApplication
@EnableCaching // 2. Caching-a enable panrathukku ithu mukkiyam
public class WeatherApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeatherApplication.class, args);
    }
}