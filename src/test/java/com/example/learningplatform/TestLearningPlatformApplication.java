package com.example.learningplatform;

import org.springframework.boot.SpringApplication;

public class TestLearningPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(LearningPlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
