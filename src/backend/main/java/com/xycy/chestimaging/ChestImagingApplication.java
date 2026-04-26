package com.xycy.chestimaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChestImagingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChestImagingApplication.class, args);
    }
}