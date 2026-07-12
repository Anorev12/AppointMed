package edu.cit.Verona.AppointMed.appointmed_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppointmedBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppointmedBackendApplication.class, args);
	}

}

