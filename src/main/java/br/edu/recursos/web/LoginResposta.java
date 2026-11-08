package br.edu.recursos.web;

import br.edu.recursos.dominio.Perfil;
import br.edu.recursos.servico.AutenticacaoServico.TokenEmitido;

public record LoginResposta(String token, String tipo, long expiraEmSegundos, Perfil perfil) {

    static LoginResposta de(TokenEmitido emitido) {
        return new LoginResposta(emitido.token(), "Bearer", emitido.expiraEmSegundos(), emitido.perfil());
    }
}