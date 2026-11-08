package br.edu.recursos.web;

import jakarta.validation.constraints.NotBlank;

public record LoginRequisicao(@NotBlank String usuario, @NotBlank String senha) {
}