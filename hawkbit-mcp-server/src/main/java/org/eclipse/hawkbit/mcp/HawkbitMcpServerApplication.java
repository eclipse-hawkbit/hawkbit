package org.eclipse.hawkbit.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HawkbitMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(HawkbitMcpServerApplication.class, args);
	}

}
