package br.edu.recursos.repositorio;

import br.edu.recursos.dominio.RegistroAuditoria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAuditoriaRepositorio extends JpaRepository<RegistroAuditoria, UUID> {

    List<RegistroAuditoria> findByReservaId(UUID reservaId);
}
