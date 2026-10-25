package br.edu.recursos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RecursoTest {

    @Test
    void novoRecursoNasceAtivoEForaDeManutencao() {
        Recurso recurso = new Recurso("Sala 101", TipoRecurso.SALA, true);

        assertThat(recurso.getId()).isNotNull();
        assertThat(recurso.getNome()).isEqualTo("Sala 101");
        assertThat(recurso.isRestrito()).isTrue();
        assertThat(recurso.getStatus()).isEqualTo(StatusRecurso.ATIVO);
        assertThat(recurso.emManutencao()).isFalse();
    }

    @Test
    void manutencaoAlternaOStatus() {
        Recurso recurso = new Recurso("Projetor", TipoRecurso.MATERIAL, false);

        recurso.entrarEmManutencao();
        assertThat(recurso.emManutencao()).isTrue();

        recurso.sairDaManutencao();
        assertThat(recurso.emManutencao()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(TipoRecurso.class)
    void guardaOTipoInformado(TipoRecurso tipo) {
        assertThat(new Recurso("R", tipo, false).getTipo()).isEqualTo(tipo);
    }
}
