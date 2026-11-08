package br.edu.recursos.seguranca;

import br.edu.recursos.dominio.Perfil;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class EmissorToken {

    private static final String CLAIM_PERFIL = "perfil";

    private final JwtEncoder codificadorJwt;
    private final Duration validade;

    public EmissorToken(JwtEncoder codificadorJwt, @Value("${app.jwt.ttl-minutos}") long ttlMinutos) {
        this.codificadorJwt = codificadorJwt;
        this.validade = Duration.ofMinutes(ttlMinutos);
    }

    public TokenEmitido emitir(String login, Perfil perfil) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(login)
                .claim(CLAIM_PERFIL, perfil.name())
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = codificadorJwt.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenEmitido(token, validade.toSeconds(), perfil);
    }
}