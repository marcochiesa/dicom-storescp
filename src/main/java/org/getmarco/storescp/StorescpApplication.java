package org.getmarco.storescp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StorescpApplication {

	public static void main(String[] args) {
		System.out.println("starting storescp");
		SpringApplication.run(StorescpApplication.class, args);
	}
}
