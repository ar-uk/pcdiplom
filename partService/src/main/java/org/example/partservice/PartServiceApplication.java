package org.example.partservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PartServiceApplication.class, args);
    }

}
