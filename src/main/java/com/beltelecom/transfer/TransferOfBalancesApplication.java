package com.beltelecom.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TransferOfBalancesApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransferOfBalancesApplication.class, args);
    }
}
