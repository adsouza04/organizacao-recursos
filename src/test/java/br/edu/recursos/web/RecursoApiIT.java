package br.edu.recursos.web;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.recursos.BaseIntegracaoPostgres;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;


@AutoConfigureMockMvc
class RecursoApiIT extends BaseIntegracaoPostgres {

    private static final Instant INICIO = Instant.parse("2027-05-10T10:00:00Z");
    private static final Instant FIM = INICIO.plusSeconds(3600);

    @Autowired
    MockMvc mvc;

    @Test
    void administradorCriaRecurso() throws Exception {
        criarRecurso(token("admin"), "Laboratório " + UUID.randomUUID())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("SALA"))
                .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void solicitanteNaoPodeCriarRecurso() throws Exception {
        criarRecurso(token("solicitante"), "Sala " + UUID.randomUUID())
                .andExpect(status().isForbidden());
    }

    @Test
    void nomeEmBrancoResponde400() throws Exception {
        mvc.perform(post("/api/recursos").header(AUTHORIZATION, token("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"tipo\":\"SALA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void qualquerPerfilAutenticadoListaRecursos() throws Exception {
        mvc.perform(get("/api/recursos").header(AUTHORIZATION, token("solicitante")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void responsavelNaoPodeColocarRecursoEmManutencao() throws Exception {
        String id = idDe(criarRecurso(token("admin"), "Projetor " + UUID.randomUUID()));

        mvc.perform(post("/api/recursos/" + id + "/manutencao").header(AUTHORIZATION, token("responsavel")))
                .andExpect(status().isForbidden());
    }

    @Test
    void manutencaoDeRecursoInexistenteResponde404() throws Exception {
        mvc.perform(post("/api/recursos/" + UUID.randomUUID() + "/manutencao").header(AUTHORIZATION, token("admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void recursoEmManutencaoRecusaReservaEVoltaAAceitarAoSair() throws Exception {
        String admin = token("admin");
        String solicitante = token("solicitante");
        String id = idDe(criarRecurso(admin, "Auditório " + UUID.randomUUID()));

        mvc.perform(post("/api/recursos/" + id + "/manutencao").header(AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MANUTENCAO"));

        reservar(solicitante, id).andExpect(status().isUnprocessableEntity());

        mvc.perform(delete("/api/recursos/" + id + "/manutencao").header(AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));

        reservar(solicitante, id).andExpect(status().isCreated());
    }

    private ResultActions criarRecurso(String token, String nome) throws Exception {
        return mvc.perform(post("/api/recursos").header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"%s\",\"tipo\":\"SALA\",\"restrito\":false}".formatted(nome)));
    }

    private ResultActions reservar(String token, String recursoId) throws Exception {
        return mvc.perform(post("/api/reservas").header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recursoId\":\"%s\",\"inicio\":\"%s\",\"fim\":\"%s\"}".formatted(recursoId, INICIO, FIM)));
    }

    private String token(String usuario) throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"%s\",\"senha\":\"Senha@123\"}".formatted(usuario)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    private static String idDe(ResultActions resultado) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id");
    }
}