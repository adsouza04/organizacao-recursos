package br.edu.recursos.servico;

import br.edu.recursos.dominio.Perfil;
import br.edu.recursos.dominio.Usuario;
import br.edu.recursos.excecao.CredenciaisInvalidasException;
import br.edu.recursos.repositorio.UsuarioRepositorio;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoServico {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificadorSenha;
    private final JwtEncoder codificadorJwt;
    private final long ttlMinutos;

    public AutenticacaoServico(UsuarioRepositorio usuarios, PasswordEncoder codificadorSenha,
                               JwtEncoder codificadorJwt, @Value("${app.jwt.ttl-minutos}") long ttlMinutos) {
        this.usuarios = usuarios;
        this.codificadorSenha = codificadorSenha;
        this.codificadorJwt = codificadorJwt;
        this.ttlMinutos = ttlMinutos;
    }

    @Transactional(readOnly = true)
    public TokenEmitido autenticar(String login, String senha) {
        Usuario usuario = usuarios.findByLoginAndAtivoTrue(login)
                .orElseThrow(CredenciaisInvalidasException::new);
        if (!codificadorSenha.matches(senha, usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }

        Instant agora = Instant.now();
        long segundos = ttlMinutos * 60;
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getLogin())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(segundos))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = codificadorJwt.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenEmitido(token, segundos, usuario.getPerfil());
    }

    public record TokenEmitido(String token, long expiraEmSegundos, Perfil perfil) {
    }
}