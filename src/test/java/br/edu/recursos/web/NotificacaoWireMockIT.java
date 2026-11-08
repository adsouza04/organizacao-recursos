package br.edu.recursos.web;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.recursos.BaseIntegracaoPostgres;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificacaoWireMockIT extends BaseIntegracaoPostgres {

    private static final Instant INICIO = Instant.parse("2027-06-01T14:00:00Z");
    private static final Instant FIM = INICIO.plusSeconds(3600);

    @RegisterExtension
    static WireMockExtension servicoNotificacao = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void urlDoServicoDeNotificacao(DynamicPropertyRegistry registro) {
        registro.add("app.notificacao.url", servicoNotificacao::baseUrl);
        registro.add("app.notificacao.timeout-ms", () -> 2000);
    }

    @Autowired
    MockMvc mvc;

    @Test
    @Order(1)
    void aprovacaoEnviaNotificacaoAoSolicitante() throws Exception {
        servicoNotificacao.stubFor(WireMock.post("/notificacoes").willReturn(WireMock.aResponse().withStatus(202)));
        String reserva = novaReserva();

        decidir(reserva, "aprovar").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));

        servicoNotificacao.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/notificacoes"))
                .withHeader("Content-Type", WireMock.containing("application/json"))
                .withRequestBody(WireMock.matchingJsonPath("$.reservaId", WireMock.equalTo(reserva)))
                .withRequestBody(WireMock.matchingJsonPath("$.destinatario", WireMock.equalTo("solicitante")))
                .withRequestBody(WireMock.matchingJsonPath("$.status", WireMock.equalTo("APROVADA"))));
    }

    @Test
    @Order(2)
    void rejeicaoEnviaNotificacaoComStatusRejeitada() throws Exception {
        servicoNotificacao.stubFor(WireMock.post("/notificacoes").willReturn(WireMock.aResponse().withStatus(202)));
        String reserva = novaReserva();

        decidir(reserva, "rejeitar").andExpect(status().isOk());

        servicoNotificacao.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/notificacoes"))
                .withRequestBody(WireMock.matchingJsonPath("$.status", WireMock.equalTo("REJEITADA"))));
    }

    @Test
    @Order(3)
    void falhaDoServicoExternoNaoDesfazAAprovacao() throws Exception {
        servicoNotificacao.stubFor(WireMock.post("/notificacoes").willReturn(WireMock.aResponse().withStatus(503)));
        String reserva = novaReserva();

        decidir(reserva, "aprovar").andExpect(status().isOk());

        mvc.perform(get("/api/reservas/" + reserva).header(AUTHORIZATION, token("solicitante")))
                .andExpect(jsonPath("$.status").value("APROVADA"));
        servicoNotificacao.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/notificacoes")));
    }

    @Test
    @Order(4)
    void servicoLentoEhInterrompidoPeloTimeoutSemDesfazerAAprovacao() throws Exception {
        servicoNotificacao.stubFor(WireMock.post("/notificacoes")
                .willReturn(WireMock.aResponse().withStatus(202).withFixedDelay(4000)));
        String reserva = novaReserva();

        decidir(reserva, "aprovar").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
    }

    private String novaReserva() throws Exception {
        String recurso = idDe(mvc.perform(post("/api/recursos").header(AUTHORIZATION, token("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Sala %s\",\"tipo\":\"SALA\",\"restrito\":false}".formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        return idDe(mvc.perform(post("/api/reservas").header(AUTHORIZATION, token("solicitante"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recursoId\":\"%s\",\"inicio\":\"%s\",\"fim\":\"%s\"}".formatted(recurso, INICIO, FIM)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private ResultActions decidir(String reserva, String acao) throws Exception {
        return mvc.perform(post("/api/reservas/" + reserva + "/" + acao).header(AUTHORIZATION, token("responsavel")));
    }

    private String token(String usuario) throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"%s\",\"senha\":\"Senha@123\"}".formatted(usuario)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    private static String idDe(String json) {
        return JsonPath.read(json, "$.id");
    }
}