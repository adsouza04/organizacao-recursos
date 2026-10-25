package br.edu.recursos.servico;

import br.edu.recursos.dominio.Perfil;

/** Quem está executando a ação (extraído do JWT na camada web). */
public record Ator(String usuario, Perfil perfil) {
}
