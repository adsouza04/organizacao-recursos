package br.edu.recursos.web;

import br.edu.recursos.servico.RecursoServico;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recursos")
public class RecursoControlador {

    private final RecursoServico servico;

    public RecursoControlador(RecursoServico servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<RecursoResposta> criar(@Valid @RequestBody CriarRecursoRequisicao requisicao) {
        var criado = servico.criar(requisicao.nome(), requisicao.tipo(), requisicao.restrito());
        return ResponseEntity.status(HttpStatus.CREATED).body(RecursoResposta.de(criado));
    }

    @GetMapping
    public List<RecursoResposta> listar() {
        return servico.listar().stream().map(RecursoResposta::de).toList();
    }

    @PostMapping("/{id}/manutencao")
    public RecursoResposta entrarEmManutencao(@PathVariable UUID id) {
        return RecursoResposta.de(servico.entrarEmManutencao(id));
    }

    @DeleteMapping("/{id}/manutencao")
    public RecursoResposta sairDaManutencao(@PathVariable UUID id) {
        return RecursoResposta.de(servico.sairDaManutencao(id));
    }
}