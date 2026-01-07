package com.scalaris;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.scalaris")
public class ScalarisApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScalarisApplication.class, args);
    }
}
