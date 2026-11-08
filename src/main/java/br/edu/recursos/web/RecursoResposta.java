package br.edu.recursos.web;

import br.edu.recursos.dominio.Recurso;
import br.edu.recursos.dominio.StatusRecurso;
import br.edu.recursos.dominio.TipoRecurso;
import java.util.UUID;

public record RecursoResposta(UUID id, String nome, TipoRecurso tipo, boolean restrito, StatusRecurso status) {

    static RecursoResposta de(Recurso recurso) {
        return new RecursoResposta(recurso.getId(), recurso.getNome(), recurso.getTipo(),
                recurso.isRestrito(), recurso.getStatus());
    }
}