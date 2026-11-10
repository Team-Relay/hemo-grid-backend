package com.sentinel.hemo_grid;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.security.jwt-secret=test-only-secret-for-jwt-signing-32-bytes")
class HemoGridApplicationTests {

	@Test
	void contextLoads() {
	}

}
