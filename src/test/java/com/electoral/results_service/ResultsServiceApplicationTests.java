package com.electoral.results_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.electoral.results_service.publicacion.calendario.EstadoJornadaProvider;

@SpringBootTest(properties = {"srm1.base-url=http://localhost:8080"})
class ResultsServiceApplicationTests extends AbstractIntegrationTest {

	@MockBean
	private EstadoJornadaProvider estadoJornadaProvider;

	@Test
	void contextLoads() {
	}

}
