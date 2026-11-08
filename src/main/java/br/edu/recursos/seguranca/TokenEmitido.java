package br.edu.recursos.seguranca;

import br.edu.recursos.dominio.Perfil;

public record TokenEmitido(String token, long expiraEmSegundos, Perfil perfil) {
}