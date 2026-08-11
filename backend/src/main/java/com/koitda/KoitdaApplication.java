package com.koitda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KoitdaApplication {

	public static void main(String[] args) {
		SpringApplication.run(KoitdaApplication.class, args);
	}

}
