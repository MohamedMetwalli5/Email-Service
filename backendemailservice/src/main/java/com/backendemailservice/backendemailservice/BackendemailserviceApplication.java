package com.backendemailservice.backendemailservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableCaching
public class BackendemailserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendemailserviceApplication.class, args);
	}

}
