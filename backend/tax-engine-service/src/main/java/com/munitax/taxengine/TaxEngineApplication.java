package com.munitax.taxengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class TaxEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxEngineApplication.class, args);
    }

}
