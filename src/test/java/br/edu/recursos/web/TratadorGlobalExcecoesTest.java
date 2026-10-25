package br.edu.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

/** Erro inesperado nunca vaza detalhes internos: responde 500 com mensagem genérica. */
class TratadorGlobalExcecoesTest {

    @Test
    void erroInesperadoResponde500SemVazarDetalhes() {
        ProblemDetail problema = new TratadorGlobalExcecoes()
                .inesperado(new IllegalStateException("segredo interno"));

        assertThat(problema.getStatus()).isEqualTo(500);
        assertThat(problema.getDetail()).isEqualTo("Erro interno");
    }
}
