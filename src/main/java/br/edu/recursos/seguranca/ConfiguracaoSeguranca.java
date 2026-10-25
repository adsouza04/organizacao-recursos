package br.edu.recursos.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * RBAC stateless com JWT (HS256). O claim "perfil" define o papel
 * (SOLICITANTE, RESPONSAVEL, ADMINISTRADOR).
 */
@Configuration
@EnableWebSecurity
public class ConfiguracaoSeguranca {

    @Bean
    SecurityFilterChain cadeiaDeFiltros(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reservas")
                        .hasAnyRole("SOLICITANTE", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/**")
                        .hasAnyRole("SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/cancelar")
                        .hasAnyRole("SOLICITANTE", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservas/*")
                        .hasAnyRole("SOLICITANTE", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/aprovar",
                                "/api/reservas/*/rejeitar", "/api/reservas/*/iniciar",
                                "/api/reservas/*/concluir", "/api/reservas/*/nao-compareceu")
                        .hasAnyRole("RESPONSAVEL", "ADMINISTRADOR")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversorJwt())));
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter conversorJwt() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(jwt -> {
            String perfil = jwt.getClaimAsString("perfil");
            Collection<GrantedAuthority> autoridades =
                    perfil == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + perfil));
            return autoridades;
        });
        return conversor;
    }

    @Bean
    SecretKey chaveJwt(@Value("${app.jwt.segredo}") String segredo) {
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder decodificadorJwt(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder codificadorJwt(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }
}
