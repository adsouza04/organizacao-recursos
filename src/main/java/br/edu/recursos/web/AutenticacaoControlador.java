package br.edu.recursos.web;

import br.edu.recursos.servico.AutenticacaoServico;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoControlador {

    private final AutenticacaoServico servico;

    public AutenticacaoControlador(AutenticacaoServico servico) {
        this.servico = servico;
    }

    @PostMapping("/login")
    public LoginResposta login(@Valid @RequestBody LoginRequisicao requisicao) {
        return LoginResposta.de(servico.autenticar(requisicao.usuario(), requisicao.senha()));
    }
}