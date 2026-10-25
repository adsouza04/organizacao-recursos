package br.edu.recursos.repositorio;

import br.edu.recursos.dominio.Reserva;
import br.edu.recursos.dominio.StatusReserva;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepositorio extends JpaRepository<Reserva, UUID> {

    @Query("""
            select count(r) > 0 from Reserva r
            where r.recurso.id = :recursoId
              and r.status in :bloqueantes
              and r.inicio < :fim
              and r.fim > :inicio
            """)
    boolean existeSobreposicao(@Param("recursoId") UUID recursoId,
                               @Param("inicio") Instant inicio,
                               @Param("fim") Instant fim,
                               @Param("bloqueantes") Collection<StatusReserva> bloqueantes);

    long countByRecursoId(UUID recursoId);
}
