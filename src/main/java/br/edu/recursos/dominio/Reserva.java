package br.edu.recursos.dominio;

import br.edu.recursos.excecao.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id")
    private Recurso recurso;

    private String solicitante;

    @Column(name = "inicio_em")
    private Instant inicio;

    @Column(name = "fim_em")
    private Instant fim;

    @Enumerated(EnumType.STRING)
    private StatusReserva status;

    @Version
    @Column(name = "versao")
    private long versao;

    @Column(name = "criado_em")
    private Instant criadoEm;

    protected Reserva() {
    }

    public Reserva(Recurso recurso, String solicitante, Instant inicio, Instant fim) {
        validarPeriodo(inicio, fim);
        this.id = UUID.randomUUID();
        this.recurso = recurso;
        this.solicitante = solicitante;
        this.inicio = inicio;
        this.fim = fim;
        this.status = StatusReserva.SOLICITADA;
        this.criadoEm = Instant.now();
    }

    /** RN-01: término estritamente posterior ao início. */
    public static void validarPeriodo(Instant inicio, Instant fim) {
        if (inicio == null || fim == null) {
            throw new RegraDeNegocioException("Início e término são obrigatórios");
        }
        if (!fim.isAfter(inicio)) {
            throw new RegraDeNegocioException("O término deve ser estritamente posterior ao início");
        }
    }

    /** Aplica a transição validando a máquina de estados; devolve o estado anterior. */
    public StatusReserva transicionarPara(StatusReserva destino) {
        if (!status.podeTransicionarPara(destino)) {
            throw new RegraDeNegocioException("Transição inválida: " + status + " -> " + destino);
        }
        StatusReserva anterior = status;
        this.status = destino;
        return anterior;
    }

    public UUID getId() {
        return id;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFim() {
        return fim;
    }

    public StatusReserva getStatus() {
        return status;
    }
}
