package br.edu.recursos;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de integração: PostgreSQL real via Testcontainers (sem mocks de persistência).
 * O container é um singleton: sobe uma única vez e é compartilhado por todas as classes *IT,
 * evitando que contextos do Spring em cache apontem para um container já encerrado.
 */
@SpringBootTest
public abstract class BaseIntegracaoPostgres {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propriedadesDoBanco(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
