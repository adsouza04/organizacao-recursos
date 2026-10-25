package br.edu.recursos.web;

import br.edu.recursos.dominio.Reserva;
import br.edu.recursos.dominio.StatusReserva;
import java.time.Instant;
import java.util.UUID;

public record ReservaResposta(
        UUID id,
        UUID recursoId,
        String solicitante,
        Instant inicio,
        Instant fim,
        StatusReserva status) {

    public static ReservaResposta de(Reserva r) {
        return new ReservaResposta(r.getId(), r.getRecurso().getId(), r.getSolicitante(),
                r.getInicio(), r.getFim(), r.getStatus());
    }
}
