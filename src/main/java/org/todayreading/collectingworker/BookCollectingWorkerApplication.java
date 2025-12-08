package org.todayreading.collectingworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "org.todayreading.collectingworker.naver")
public class BookCollectingWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookCollectingWorkerApplication.class, args);
	}

}
