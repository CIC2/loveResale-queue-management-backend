package com.resale.homeflyqueuemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QueueManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueManagementApplication.class, args);
    }

}


