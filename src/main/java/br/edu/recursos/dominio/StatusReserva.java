package br.edu.recursos.dominio;

import java.util.EnumSet;
import java.util.Set;

/**
 * Máquina de estados da reserva.
 * SOLICITADA -> APROVADA -> EM_USO -> CONCLUIDA, com saídas alternativas
 * REJEITADA, CANCELADA e NAO_COMPARECEU.
 */
public enum StatusReserva {
    SOLICITADA, APROVADA, EM_USO, CONCLUIDA, REJEITADA, CANCELADA, NAO_COMPARECEU;

    /** Estados que ocupam o recurso no período (entram na checagem de sobreposição). */
    public static final Set<StatusReserva> BLOQUEANTES =
            EnumSet.of(SOLICITADA, APROVADA, EM_USO, CONCLUIDA);

    /** Estados de reserva já iniciada: não podem ser apagados (RN-06). */
    public static final Set<StatusReserva> INICIADOS = EnumSet.of(EM_USO, CONCLUIDA);

    public Set<StatusReserva> proximosPermitidos() {
        return switch (this) {
            case SOLICITADA -> EnumSet.of(APROVADA, REJEITADA, CANCELADA);
            case APROVADA -> EnumSet.of(EM_USO, CANCELADA, NAO_COMPARECEU);
            case EM_USO -> EnumSet.of(CONCLUIDA);
            case CONCLUIDA, REJEITADA, CANCELADA, NAO_COMPARECEU -> EnumSet.noneOf(StatusReserva.class);
        };
    }

    public boolean podeTransicionarPara(StatusReserva destino) {
        return proximosPermitidos().contains(destino);
    }

    public boolean iniciada() {
        return INICIADOS.contains(this);
    }
}
