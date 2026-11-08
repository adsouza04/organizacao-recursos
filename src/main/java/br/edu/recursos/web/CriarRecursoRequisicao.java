package br.edu.recursos.web;

import br.edu.recursos.dominio.TipoRecurso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarRecursoRequisicao(
        @NotBlank @Size(max = 120) String nome,
        @NotNull TipoRecurso tipo,
        boolean restrito) {
}