package br.edu.recursos.dominio;

import java.util.EnumSet;
import java.util.Set;


public enum StatusReserva {
    SOLICITADA, APROVADA, EM_USO, CONCLUIDA, REJEITADA, CANCELADA, NAO_COMPARECEU;

    public static final Set<StatusReserva> BLOQUEANTES =
            EnumSet.of(SOLICITADA, APROVADA, EM_USO, CONCLUIDA);

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
