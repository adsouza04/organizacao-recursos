package br.edu.recursos.repositorio;

import br.edu.recursos.dominio.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByLoginAndAtivoTrue(String login);
}