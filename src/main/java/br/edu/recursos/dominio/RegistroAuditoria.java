package br.edu.recursos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Registro de auditoria (RN-07): imutável na aplicação (@Immutable) e no banco (trigger). */
@Entity
@Immutable
@Table(name = "registro_auditoria")
public class RegistroAuditoria {

    @Id
    private UUID id;

    private UUID reservaId;

    @Enumerated(EnumType.STRING)
    private StatusReserva statusAnterior;

    @Enumerated(EnumType.STRING)
    private StatusReserva statusNovo;

    private String ator;

    @Column(name = "ocorrido_em")
    private Instant ocorridoEm;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(UUID reservaId, StatusReserva statusAnterior,
                             StatusReserva statusNovo, String ator) {
        this.id = UUID.randomUUID();
        this.reservaId = reservaId;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.ator = ator;
        this.ocorridoEm = Instant.now();
    }

    public UUID getReservaId() {
        return reservaId;
    }

    public StatusReserva getStatusAnterior() {
        return statusAnterior;
    }

    public StatusReserva getStatusNovo() {
        return statusNovo;
    }

    public String getAtor() {
        return ator;
    }
}
