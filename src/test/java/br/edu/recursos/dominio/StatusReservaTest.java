package br.edu.recursos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.recursos.excecao.RegraDeNegocioException;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Testes unitários (sem mocks, sem Spring) da máquina de estados e do período (RN-01). */
class StatusReservaTest {

    static Stream<Arguments> transicoesValidas() {
        return Stream.of(
                Arguments.of(StatusReserva.SOLICITADA, StatusReserva.APROVADA),
                Arguments.of(StatusReserva.SOLICITADA, StatusReserva.REJEITADA),
                Arguments.of(StatusReserva.SOLICITADA, StatusReserva.CANCELADA),
                Arguments.of(StatusReserva.APROVADA, StatusReserva.EM_USO),
                Arguments.of(StatusReserva.APROVADA, StatusReserva.CANCELADA),
                Arguments.of(StatusReserva.APROVADA, StatusReserva.NAO_COMPARECEU),
                Arguments.of(StatusReserva.EM_USO, StatusReserva.CONCLUIDA));
    }

    static Stream<Arguments> transicoesInvalidas() {
        return Stream.of(
                Arguments.of(StatusReserva.SOLICITADA, StatusReserva.EM_USO),
                Arguments.of(StatusReserva.SOLICITADA, StatusReserva.CONCLUIDA),
                Arguments.of(StatusReserva.APROVADA, StatusReserva.CONCLUIDA),
                Arguments.of(StatusReserva.EM_USO, StatusReserva.CANCELADA),
                Arguments.of(StatusReserva.CONCLUIDA, StatusReserva.SOLICITADA),
                Arguments.of(StatusReserva.REJEITADA, StatusReserva.APROVADA),
                Arguments.of(StatusReserva.CANCELADA, StatusReserva.APROVADA),
                Arguments.of(StatusReserva.NAO_COMPARECEU, StatusReserva.EM_USO));
    }

    @ParameterizedTest(name = "{0} -> {1} é permitida")
    @MethodSource("transicoesValidas")
    void permiteTransicoesValidas(StatusReserva origem, StatusReserva destino) {
        assertThat(origem.podeTransicionarPara(destino)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} é proibida")
    @MethodSource("transicoesInvalidas")
    void proibeTransicoesInvalidas(StatusReserva origem, StatusReserva destino) {
        assertThat(origem.podeTransicionarPara(destino)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class, names = {"EM_USO", "CONCLUIDA"})
    void estadosIniciadosSaoMarcadosComoIniciados(StatusReserva status) {
        assertThat(status.iniciada()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class,
            names = {"SOLICITADA", "APROVADA", "REJEITADA", "CANCELADA", "NAO_COMPARECEU"})
    void estadosNaoIniciadosPodemSerApagados(StatusReserva status) {
        assertThat(status.iniciada()).isFalse();
    }

    @Test
    void transicaoInvalidaNaEntidadeLancaRegraDeNegocio() {
        Reserva reserva = novaReserva(Instant.parse("2026-11-01T10:00:00Z"), Instant.parse("2026-11-01T11:00:00Z"));
        assertThatThrownBy(() -> reserva.transicionarPara(StatusReserva.CONCLUIDA))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void transicaoValidaDevolveEstadoAnterior() {
        Reserva reserva = novaReserva(Instant.parse("2026-11-01T10:00:00Z"), Instant.parse("2026-11-01T11:00:00Z"));
        assertThat(reserva.transicionarPara(StatusReserva.APROVADA)).isEqualTo(StatusReserva.SOLICITADA);
        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.APROVADA);
    }

    static Stream<Arguments> periodosInvalidos() {
        Instant t = Instant.parse("2026-11-01T10:00:00Z");
        return Stream.of(
                Arguments.of(t, t),
                Arguments.of(t, t.minusSeconds(1)),
                Arguments.of(null, t),
                Arguments.of(t, null));
    }

    @ParameterizedTest
    @MethodSource("periodosInvalidos")
    void rejeitaPeriodoInvalido(Instant inicio, Instant fim) {
        assertThatThrownBy(() -> Reserva.validarPeriodo(inicio, fim))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    private static Reserva novaReserva(Instant inicio, Instant fim) {
        return new Reserva(new Recurso("Sala 101", TipoRecurso.SALA, false), "ana", inicio, fim);
    }
}
