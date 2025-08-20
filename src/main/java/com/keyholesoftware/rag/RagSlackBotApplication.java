package com.keyholesoftware.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RagSlackBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagSlackBotApplication.class, args);
	}

}
