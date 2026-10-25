package br.edu.recursos.repositorio;

import br.edu.recursos.dominio.Recurso;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecursoRepositorio extends JpaRepository<Recurso, UUID> {

    /** SELECT ... FOR UPDATE: serializa reservas concorrentes do mesmo recurso. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Recurso r where r.id = :id")
    Optional<Recurso> buscarPorIdComBloqueio(@Param("id") UUID id);
}
