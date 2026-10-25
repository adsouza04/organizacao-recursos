package br.edu.recursos.servico;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.recursos.BaseIntegracaoPostgres;
import br.edu.recursos.dominio.Perfil;
import br.edu.recursos.dominio.Recurso;
import br.edu.recursos.dominio.TipoRecurso;
import br.edu.recursos.excecao.ConflitoException;
import br.edu.recursos.repositorio.RecursoRepositorio;
import br.edu.recursos.repositorio.RegistroAuditoriaRepositorio;
import br.edu.recursos.repositorio.ReservaRepositorio;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** RN-03: requisições simultâneas ao mesmo recurso/período geram exatamente UMA reserva. */
class ReservaConcorrenciaIT extends BaseIntegracaoPostgres {

    private static final int THREADS = 12;

    @Autowired
    ReservaServico servico;
    @Autowired
    RecursoRepositorio recursos;
    @Autowired
    ReservaRepositorio reservas;
    @Autowired
    RegistroAuditoriaRepositorio auditoria;

    @Test
    void duplaReservaSimultaneaProduzExatamenteUmaReservaAceita() throws Exception {
        Recurso sala = recursos.saveAndFlush(new Recurso("Sala 201", TipoRecurso.SALA, false));
        Instant inicio = Instant.parse("2026-12-01T10:00:00Z");
        Instant fim = Instant.parse("2026-12-01T11:00:00Z");

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger aceitas = new AtomicInteger();
        AtomicInteger conflitos = new AtomicInteger();
        List<Future<?>> futuros = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            Ator ator = new Ator("prof" + i, Perfil.SOLICITANTE);
            Callable<Void> tarefa = () -> {
                largada.await();
                try {
                    servico.criar(sala.getId(), inicio, fim, ator);
                    aceitas.incrementAndGet();
                } catch (ConflitoException e) {
                    conflitos.incrementAndGet();
                }
                return null;
            };
            futuros.add(pool.submit(tarefa));
        }
        largada.countDown();
        for (Future<?> futuro : futuros) {
            futuro.get();
        }
        pool.shutdown();

        assertThat(aceitas.get()).isEqualTo(1);
        assertThat(conflitos.get()).isEqualTo(THREADS - 1);
        assertThat(reservas.countByRecursoId(sala.getId())).isEqualTo(1);
    }

    @Test
    void reservasAdjacentesNaoSaoConflito() {
        Recurso sala = recursos.saveAndFlush(new Recurso("Sala 202", TipoRecurso.SALA, false));
        Ator ator = new Ator("ana", Perfil.SOLICITANTE);
        Instant t0 = Instant.parse("2026-12-02T10:00:00Z");

        servico.criar(sala.getId(), t0, t0.plusSeconds(3600), ator);
        servico.criar(sala.getId(), t0.plusSeconds(3600), t0.plusSeconds(7200), ator);

        assertThat(reservas.countByRecursoId(sala.getId())).isEqualTo(2);
    }

    @Test
    void criacaoGeraRegistroDeAuditoria() {
        Recurso sala = recursos.saveAndFlush(new Recurso("Sala 203", TipoRecurso.SALA, false));
        var reserva = servico.criar(sala.getId(), Instant.parse("2026-12-03T10:00:00Z"),
                Instant.parse("2026-12-03T11:00:00Z"), new Ator("ana", Perfil.SOLICITANTE));

        assertThat(auditoria.findByReservaId(reserva.getId())).hasSize(1);
    }
}
