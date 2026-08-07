package com.koitda;

import org.springframework.boot.SpringApplication;

public class TestKoitdaApplication {

	public static void main(String[] args) {
		SpringApplication.from(KoitdaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
