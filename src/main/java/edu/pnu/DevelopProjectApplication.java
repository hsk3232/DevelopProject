package edu.pnu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DevelopProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevelopProjectApplication.class, args);
	}

}
