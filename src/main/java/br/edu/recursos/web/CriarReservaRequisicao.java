package br.edu.recursos.web;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CriarReservaRequisicao(
        @NotNull UUID recursoId,
        @NotNull Instant inicio,
        @NotNull Instant fim) {
}
