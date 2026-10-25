package br.edu.recursos.web;

import br.edu.recursos.dominio.Perfil;
import br.edu.recursos.servico.Ator;
import br.edu.recursos.servico.ReservaServico;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
public class ReservaControlador {

    private final ReservaServico servico;

    public ReservaControlador(ReservaServico servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<ReservaResposta> criar(@Valid @RequestBody CriarReservaRequisicao requisicao,
                                                 Authentication autenticacao) {
        var criada = servico.criar(requisicao.recursoId(), requisicao.inicio(), requisicao.fim(),
                ator(autenticacao));
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResposta.de(criada));
    }

    @GetMapping("/{id}")
    public ReservaResposta buscar(@PathVariable UUID id) {
        return ReservaResposta.de(servico.buscar(id));
    }

    @PostMapping("/{id}/aprovar")
    public ReservaResposta aprovar(@PathVariable UUID id, Authentication autenticacao) {
        return ReservaResposta.de(servico.aprovar(id, ator(autenticacao)));
    }

    @PostMapping("/{id}/rejeitar")
    public ReservaResposta rejeitar(@PathVariable UUID id, Authentication autenticacao) {
        return ReservaResposta.de(servico.rejeitar(id, ator(autenticacao)));
    }

    @PostMapping("/{id}/iniciar")
    public ReservaResposta iniciar(@PathVariable UUID id, Authentication autenticacao) {
        return ReservaResposta.de(servico.iniciar(id, ator(autenticacao)));
    }

    @PostMapping("/{id}/concluir")
    public ReservaResposta concluir(@PathVariable UUID id, Authentication autenticacao) {
        return ReservaResposta.de(servico.concluir(id, ator(autenticacao)));
    }

    @PostMapping("/{id}/nao-compareceu")
    public ReservaResposta naoCompareceu(@PathVariable UUID id, Authentication autenticacao) {
        return ReservaResposta.de(servico.marcarNaoComparecimento(id, ator(autenticacao)));
    }

    @PostMapping("/{id}/cancelar")
    public ReservaResposta cancelar(@PathVariable UUID id, Authentication autenticacao) {
        return ReservaResposta.de(servico.cancelar(id, ator(autenticacao)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id, Authentication autenticacao) {
        servico.excluir(id, ator(autenticacao));
        return ResponseEntity.noContent().build();
    }

    private static Ator ator(Authentication autenticacao) {
        Jwt jwt = (Jwt) autenticacao.getPrincipal();
        return new Ator(jwt.getSubject(), Perfil.valueOf(jwt.getClaimAsString("perfil")));
    }
}
