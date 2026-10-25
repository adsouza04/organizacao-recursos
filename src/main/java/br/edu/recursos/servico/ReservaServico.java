package br.edu.recursos.servico;

import br.edu.recursos.dominio.Perfil;
import br.edu.recursos.dominio.RegistroAuditoria;
import br.edu.recursos.dominio.Recurso;
import br.edu.recursos.dominio.Reserva;
import br.edu.recursos.dominio.StatusReserva;
import br.edu.recursos.excecao.ConflitoException;
import br.edu.recursos.excecao.NaoEncontradoException;
import br.edu.recursos.excecao.RegraDeNegocioException;
import br.edu.recursos.repositorio.RecursoRepositorio;
import br.edu.recursos.repositorio.RegistroAuditoriaRepositorio;
import br.edu.recursos.repositorio.ReservaRepositorio;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaServico {

    private final RecursoRepositorio recursos;
    private final ReservaRepositorio reservas;
    private final RegistroAuditoriaRepositorio auditoria;

    public ReservaServico(RecursoRepositorio recursos,
                          ReservaRepositorio reservas,
                          RegistroAuditoriaRepositorio auditoria) {
        this.recursos = recursos;
        this.reservas = reservas;
        this.auditoria = auditoria;
    }

    /**
     * Política de concorrência em 3 camadas:
     * 1) lock pessimista na linha do recurso (serializa quem disputa o mesmo recurso);
     * 2) verificação de sobreposição dentro do lock;
     * 3) constraint de exclusão (gist) no PostgreSQL como rede de segurança final.
     */
    @Transactional
    public Reserva criar(UUID recursoId, Instant inicio, Instant fim, Ator ator) {
        Reserva.validarPeriodo(inicio, fim);

        Recurso recurso = recursos.buscarPorIdComBloqueio(recursoId)
                .orElseThrow(() -> new NaoEncontradoException("Recurso não encontrado: " + recursoId));

        if (recurso.emManutencao()) {
            throw new RegraDeNegocioException("Recurso em manutenção não pode ser reservado");
        }
        if (reservas.existeSobreposicao(recursoId, inicio, fim, StatusReserva.BLOQUEANTES)) {
            throw new ConflitoException("Já existe reserva para este recurso no período informado");
        }

        Reserva reserva = new Reserva(recurso, ator.usuario(), inicio, fim);
        try {
            reservas.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoException("Conflito de reserva detectado pelo banco de dados");
        }
        auditoria.save(new RegistroAuditoria(reserva.getId(), null, StatusReserva.SOLICITADA, ator.usuario()));
        return reserva;
    }

    @Transactional(readOnly = true)
    public Reserva buscar(UUID id) {
        return localizar(id);
    }

    @Transactional
    public Reserva aprovar(UUID id, Ator ator) {
        Reserva reserva = localizar(id);
        // RN-05: recurso restrito só é aprovado por Responsável
        if (reserva.getRecurso().isRestrito() && ator.perfil() != Perfil.RESPONSAVEL) {
            throw new AccessDeniedException("Somente Responsáveis aprovam recursos restritos");
        }
        return transicionar(reserva, StatusReserva.APROVADA, ator);
    }

    @Transactional
    public Reserva rejeitar(UUID id, Ator ator) {
        return transicionar(localizar(id), StatusReserva.REJEITADA, ator);
    }

    @Transactional
    public Reserva iniciar(UUID id, Ator ator) {
        return transicionar(localizar(id), StatusReserva.EM_USO, ator);
    }

    @Transactional
    public Reserva concluir(UUID id, Ator ator) {
        return transicionar(localizar(id), StatusReserva.CONCLUIDA, ator);
    }

    @Transactional
    public Reserva marcarNaoComparecimento(UUID id, Ator ator) {
        return transicionar(localizar(id), StatusReserva.NAO_COMPARECEU, ator);
    }

    @Transactional
    public Reserva cancelar(UUID id, Ator ator) {
        Reserva reserva = localizar(id);
        exigirDonoSeSolicitante(reserva, ator, "Solicitante só cancela as próprias reservas");
        return transicionar(reserva, StatusReserva.CANCELADA, ator);
    }

    /** RN-06: reservas EM_USO ou CONCLUIDA não podem ser apagadas. */
    @Transactional
    public void excluir(UUID id, Ator ator) {
        Reserva reserva = localizar(id);
        exigirDonoSeSolicitante(reserva, ator, "Solicitante só remove as próprias reservas");
        if (reserva.getStatus().iniciada()) {
            throw new RegraDeNegocioException("Reserva iniciada não pode ser removida");
        }
        reservas.delete(reserva);
    }

    private void exigirDonoSeSolicitante(Reserva reserva, Ator ator, String mensagem) {
        boolean dono = reserva.getSolicitante().equals(ator.usuario());
        if (!dono && ator.perfil() == Perfil.SOLICITANTE) {
            throw new AccessDeniedException(mensagem);
        }
    }

    private Reserva transicionar(Reserva reserva, StatusReserva destino, Ator ator) {
        StatusReserva anterior = reserva.transicionarPara(destino);
        auditoria.save(new RegistroAuditoria(reserva.getId(), anterior, destino, ator.usuario()));
        return reserva;
    }

    private Reserva localizar(UUID id) {
        return reservas.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Reserva não encontrada: " + id));
    }
}
