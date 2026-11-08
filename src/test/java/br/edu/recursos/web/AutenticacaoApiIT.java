package br.edu.recursos.web;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.recursos.BaseIntegracaoPostgres;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Usuários de demonstração criados pela migração V2, todos com a senha Senha@123.
 */
@AutoConfigureMockMvc
class AutenticacaoApiIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "Senha@123";

    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @CsvSource({"solicitante,SOLICITANTE", "responsavel,RESPONSAVEL", "admin,ADMINISTRADOR"})
    void loginValidoDevolveTokenComOPerfilDoUsuario(String usuario, String perfil) throws Exception {
        login(usuario, SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.perfil").value(perfil))
                .andExpect(jsonPath("$.expiraEmSegundos").value(3600))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void tokenEmitidoNoLoginAutenticaNaApi() throws Exception {
        String token = tokenDe("admin");

        mvc.perform(get("/api/reservas/" + UUID.randomUUID()).header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void tokenDoSolicitanteNaoPermiteAprovarReserva() throws Exception {
        String token = tokenDe("solicitante");

        mvc.perform(post("/api/reservas/" + UUID.randomUUID() + "/aprovar")
                        .header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void senhaIncorretaResponde401() throws Exception {
        login("admin", "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void usuarioInexistenteResponde401() throws Exception {
        login("ninguem", SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void camposEmBrancoResponde400() throws Exception {
        login("", "")
                .andExpect(status().isBadRequest());
    }

    private ResultActions login(String usuario, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"%s\",\"senha\":\"%s\"}".formatted(usuario, senha)));
    }

    private String tokenDe(String usuario) throws Exception {
        String json = login(usuario, SENHA)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }
}