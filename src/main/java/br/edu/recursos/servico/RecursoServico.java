package br.edu.recursos.servico;

import br.edu.recursos.dominio.Recurso;
import br.edu.recursos.dominio.TipoRecurso;
import br.edu.recursos.excecao.NaoEncontradoException;
import br.edu.recursos.repositorio.RecursoRepositorio;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecursoServico {

    private final RecursoRepositorio recursos;

    public RecursoServico(RecursoRepositorio recursos) {
        this.recursos = recursos;
    }

    @Transactional
    public Recurso criar(String nome, TipoRecurso tipo, boolean restrito) {
        return recursos.save(new Recurso(nome, tipo, restrito));
    }

    @Transactional(readOnly = true)
    public List<Recurso> listar() {
        return recursos.findAll(Sort.by("nome"));
    }

    @Transactional
    public Recurso entrarEmManutencao(UUID id) {
        Recurso recurso = buscarComBloqueio(id);
        recurso.entrarEmManutencao();
        return recurso;
    }

    @Transactional
    public Recurso sairDaManutencao(UUID id) {
        Recurso recurso = buscarComBloqueio(id);
        recurso.sairDaManutencao();
        return recurso;
    }

    private Recurso buscarComBloqueio(UUID id) {
        return recursos.buscarPorIdComBloqueio(id)
                .orElseThrow(() -> new NaoEncontradoException("Recurso não encontrado"));
    }
}