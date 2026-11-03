package edu.csumb.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.jwt-secret=test-secret-at-least-32-bytes-long-xx")
class ApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
