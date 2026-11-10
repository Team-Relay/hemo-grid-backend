package com.sentinel.hemo_grid.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
		Security security,
		Cors cors
) {

	public record Security(
			String jwtSecret,
			String jwtIssuer,
			int accessTokenMinutes
	) {
	}

	public record Cors(
			List<String> allowedOrigins
	) {
	}
}
