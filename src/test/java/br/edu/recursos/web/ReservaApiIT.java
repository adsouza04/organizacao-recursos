package br.edu.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.recursos.BaseIntegracaoPostgres;
import br.edu.recursos.dominio.Perfil;
import br.edu.recursos.dominio.Recurso;
import br.edu.recursos.dominio.RegistroAuditoria;
import br.edu.recursos.dominio.StatusReserva;
import br.edu.recursos.dominio.TipoRecurso;
import br.edu.recursos.repositorio.RecursoRepositorio;
import br.edu.recursos.repositorio.RegistroAuditoriaRepositorio;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Testes de API ponta a ponta: JWT real (assinado e validado pelos beans da aplicação),
 * RBAC, validação, tratamento de erros e auditoria, sobre PostgreSQL real.
 */
@AutoConfigureMockMvc
class ReservaApiIT extends BaseIntegracaoPostgres {

    private static final Instant INICIO = Instant.parse("2027-03-01T10:00:00Z");
    private static final Instant FIM = INICIO.plusSeconds(3600);

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtEncoder codificador;
    @Autowired
    RecursoRepositorio recursos;
    @Autowired
    RegistroAuditoriaRepositorio auditoria;

    // ---------- criação, validação e erros ----------

    @Test
    void solicitanteCriaReserva() throws Exception {
        Recurso recurso = novoRecurso(false);

        criar(recurso.getId(), "ana", Perfil.SOLICITANTE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SOLICITADA"))
                .andExpect(jsonPath("$.solicitante").value("ana"));
    }

    @Test
    void semTokenResponde401() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(UUID.randomUUID(), INICIO, FIM)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoResponde401() throws Exception {
        mvc.perform(get("/api/reservas/" + UUID.randomUUID()).header(AUTHORIZATION, "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void responsavelNaoPodeCriarReserva() throws Exception {
        criar(novoRecurso(false).getId(), "maria", Perfil.RESPONSAVEL)
                .andExpect(status().isForbidden());
    }

    @Test
    void corpoSemCamposObrigatoriosResponde400() throws Exception {
        mvc.perform(post("/api/reservas").header(AUTHORIZATION, bearer("ana", Perfil.SOLICITANTE))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void jsonMalformadoResponde400() throws Exception {
        mvc.perform(post("/api/reservas").header(AUTHORIZATION, bearer("ana", Perfil.SOLICITANTE))
                        .contentType(MediaType.APPLICATION_JSON).content("{ isto nao e json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void identificadorMalformadoResponde400() throws Exception {
        mvc.perform(get("/api/reservas/nao-e-um-uuid").header(AUTHORIZATION, bearer("ana", Perfil.SOLICITANTE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rotaInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/nao-existe").header(AUTHORIZATION, bearer("ana", Perfil.ADMINISTRADOR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void metodoNaoPermitidoResponde405() throws Exception {
        mvc.perform(post("/api/reservas/" + UUID.randomUUID())
                        .header(AUTHORIZATION, bearer("ana", Perfil.SOLICITANTE)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void terminoIgualAoInicioResponde422() throws Exception {
        Recurso recurso = novoRecurso(false);

        mvc.perform(post("/api/reservas").header(AUTHORIZATION, bearer("ana", Perfil.SOLICITANTE))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(recurso.getId(), INICIO, INICIO)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void recursoInexistenteResponde404() throws Exception {
        criar(UUID.randomUUID(), "ana", Perfil.SOLICITANTE).andExpect(status().isNotFound());
    }

    @Test
    void recursoEmManutencaoNaoPodeSerReservado() throws Exception {
        Recurso recurso = novoRecurso(false);
        recurso.entrarEmManutencao();
        recursos.saveAndFlush(recurso);

        criar(recurso.getId(), "ana", Perfil.SOLICITANTE).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void reservaSobrepostaResponde409() throws Exception {
        Recurso recurso = novoRecurso(false);

        criar(recurso.getId(), "ana", Perfil.SOLICITANTE).andExpect(status().isCreated());
        criar(recurso.getId(), "bia", Perfil.SOLICITANTE).andExpect(status().isConflict());
    }

    // ---------- fluxo de estados e auditoria ----------

    @Test
    void fluxoCompletoGeraAuditoriaEBloqueiaExclusaoDeReservaIniciada() throws Exception {
        Recurso recurso = novoRecurso(false);
        String id = idDe(criar(recurso.getId(), "ana", Perfil.SOLICITANTE).andExpect(status().isCreated()));

        acao("aprovar", id, "maria", Perfil.RESPONSAVEL)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
        acao("iniciar", id, "maria", Perfil.RESPONSAVEL)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_USO"));
        acao("concluir", id, "maria", Perfil.RESPONSAVEL)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDA"));

        mvc.perform(delete("/api/reservas/" + id).header(AUTHORIZATION, bearer("ana", Perfil.SOLICITANTE)))
                .andExpect(status().isUnprocessableEntity());

        var registros = auditoria.findByReservaId(UUID.fromString(id));
        assertThat(registros).extracting(RegistroAuditoria::getStatusNovo)
                .containsExactlyInAnyOrder(StatusReserva.SOLICITADA, StatusReserva.APROVADA,
                        StatusReserva.EM_USO, StatusReserva.CONCLUIDA);
        assertThat(registros).extracting(RegistroAuditoria::getAtor).contains("ana", "maria");
        assertThat(registros).extracting(RegistroAuditoria::getReservaId).containsOnly(UUID.fromString(id));
    }

    @Test
    void transicaoForaDeOrdemResponde422() throws Exception {
        String id = idDe(criar(novoRecurso(false).getId(), "ana", Perfil.SOLICITANTE));

        acao("iniciar", id, "maria", Perfil.RESPONSAVEL).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void responsavelRejeitaReserva() throws Exception {
        String id = idDe(criar(novoRecurso(false).getId(), "ana", Perfil.SOLICITANTE));

        acao("rejeitar", id, "maria", Perfil.RESPONSAVEL)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJEITADA"));
    }

    @Test
    void responsavelMarcaNaoComparecimentoDeReservaAprovada() throws Exception {
        String id = idDe(criar(novoRecurso(false).getId(), "ana", Perfil.SOLICITANTE));
        acao("aprovar", id, "maria", Perfil.RESPONSAVEL).andExpect(status().isOk());

        acao("nao-compareceu", id, "maria", Perfil.RESPONSAVEL)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NAO_COMPARECEU"));
    }

    @Test
    void reservaRejeitadaLiberaOHorarioParaNovaReserva() throws Exception {
        Recurso recurso = novoRecurso(false);
        String id = idDe(criar(recurso.getId(), "ana", Perfil.SOLICITANTE));
        acao("rejeitar", id, "maria", Perfil.RESPONSAVEL).andExpect(status().isOk());

        criar(recurso.getId(), "bia", Perfil.SOLICITANTE).andExpect(status().isCreated());
    }

    @Test
    void consultaDeReservaPorResponsavel() throws Exception {
        String id = idDe(criar(novoRecurso(false).getId(), "ana", Perfil.SOLICITANTE));

        mvc.perform(get("/api/reservas/" + id).header(AUTHORIZATION, bearer("maria", Perfil.RESPONSAVEL)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
    }

    // ---------- autorização (RN-05) ----------

    @Test
    void solicitanteNaoAprovaReserva() throws Exception {
        String id = idDe(criar(novoRecurso(false).getId(), "ana", Perfil.SOLICITANTE));

        acao("aprovar", id, "ana", Perfil.SOLICITANTE).andExpect(status().isForbidden());
    }

    @Test
    void somenteResponsavelAprovaRecursoRestrito() throws Exception {
        String id = idDe(criar(novoRecurso(true).getId(), "ana", Perfil.SOLICITANTE));

        acao("aprovar", id, "admin", Perfil.ADMINISTRADOR).andExpect(status().isForbidden());
        acao("aprovar", id, "maria", Perfil.RESPONSAVEL)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
    }

    @Test
    void administradorAprovaRecursoNaoRestrito() throws Exception {
        String id = idDe(criar(novoRecurso(false).getId(), "ana", Perfil.SOLICITANTE));

        acao("aprovar", id, "admin", Perfil.ADMINISTRADOR)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
    }

    // ---------- cancelamento e exclusão ----------

    @Test
    void donoCancelaAPropriaReserva() throws Exception {
        String id = idDe(criar(novoRecurso(false).getId(), "ana", Perfil.SOLICITANTE));

        acao("cancelar", id, "ana", Perfil.SOLICITANTE)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    void solicitanteNaoCancelaReservaDeOutro() throws Exception {
        String id = idDe(criar(novoRecurso(false).getId(), "ana", Perfil.SOLICITANTE));

        acao("cancelar", id, "bia", Perfil.SOLICITANTE).andExpect(status().isForbidden());
    }

    @Test
    void administradorCancelaReservaDeOutro() throws Exception {
        String id = idDe(criar(novoRecurso(false).getId(), "ana", Perfil.SOLICITANTE));

        acao("cancelar", id, "admin", Perfil.ADMINISTRADOR)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    void donoExcluiReservaNaoIniciada() throws Exception {
        String id = idDe(criar(novoRecurso(false).getId(), "ana", Perfil.SOLICITANTE));

        mvc.perform(delete("/api/reservas/" + id).header(AUTHORIZATION, bearer("ana", Perfil.SOLICITANTE)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/reservas/" + id).header(AUTHORIZATION, bearer("ana", Perfil.SOLICITANTE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void solicitanteNaoExcluiReservaDeOutro() throws Exception {
        String id = idDe(criar(novoRecurso(false).getId(), "ana", Perfil.SOLICITANTE));

        mvc.perform(delete("/api/reservas/" + id).header(AUTHORIZATION, bearer("bia", Perfil.SOLICITANTE)))
                .andExpect(status().isForbidden());
    }

    // ---------- utilitários ----------

    private Recurso novoRecurso(boolean restrito) {
        return recursos.saveAndFlush(new Recurso("Recurso " + UUID.randomUUID(), TipoRecurso.SALA, restrito));
    }

    private String bearer(String usuario, Perfil perfil) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario)
                .claim("perfil", perfil.name())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(3600))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return "Bearer " + codificador.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }

    private static String corpo(UUID recursoId, Instant inicio, Instant fim) {
        return "{\"recursoId\":\"%s\",\"inicio\":\"%s\",\"fim\":\"%s\"}".formatted(recursoId, inicio, fim);
    }

    private ResultActions criar(UUID recursoId, String usuario, Perfil perfil) throws Exception {
        return mvc.perform(post("/api/reservas")
                .header(AUTHORIZATION, bearer(usuario, perfil))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(recursoId, INICIO, FIM)));
    }

    private ResultActions acao(String acao, String id, String usuario, Perfil perfil) throws Exception {
        return mvc.perform(post("/api/reservas/" + id + "/" + acao)
                .header(AUTHORIZATION, bearer(usuario, perfil)));
    }

    private static String idDe(ResultActions resultado) throws Exception {
        String json = resultado.andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }
}
