/* Application bootstrap and configuration-properties entry point for the HemoGrid API. */

package com.sentinel.hemo_grid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HemoGridApplication {

	public static void main(String[] args) {
		SpringApplication.run(HemoGridApplication.class, args);
	}

}
