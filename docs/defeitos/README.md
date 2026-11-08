# Registro de defeitos

Defeitos reais encontrados durante o desenvolvimento da `v1.0.0-rc1`, com evidência de detecção e de correção. Nenhum foi inventado para "encher" o registro. Riscos que **não** são defeitos (por exemplo, a versão do Spring Boot fora do suporte) estão na matriz de riscos de `docs/decisao-qualidade.md`.

## Critérios de classificação

| Severidade | Significado |
|---|---|
| **Crítica** | Impede o build/CI ou viola uma regra crítica (RN-01 a RN-07) |
| **Alta** | Resposta incorreta ao usuário, falha de segurança ou gate de qualidade reprovado |
| **Média** | Instabilidade, falta de reprodutibilidade ou falso alarme que mascara problemas reais |
| **Baixa** | Aviso, débito técnico ou risco de processo sem efeito funcional |

| Prioridade | Significado |
|---|---|
| **P1** | Corrigir antes de qualquer outro trabalho (bloqueia a entrega) |
| **P2** | Corrigir antes da release candidate |
| **P3** | Pode ser aceito com justificativa |

## Resumo

| ID | Título | Severidade | Prioridade | Situação | Onde foi detectado | Correção |
|---|---|---|---|---|---|---|
| DEF-01 | Testcontainers não conversa com o Docker 29 | Crítica | P1 | ✅ Corrigido | CI #1 (vermelho), commit `559c851` | commit `c50e8f9` |
| DEF-02 | Cobertura abaixo do gate (59% linhas / 53% branches) | Alta | P1 | ✅ Corrigido | CI #2 (vermelho), commit `c50e8f9` | commit `325018d` (CI #3 verde) |
| DEF-03 | UUID inválido, rota inexistente e método não permitido respondiam 500 | Alta | P1 | ✅ Corrigido | `ReservaApiIT` ao ser escrito | commit `325018d` |
| DEF-04 | Build não reproduzível com `./mvnw` (wrapper ausente) | Média | P2 | ✅ Corrigido | Revisão do checklist de entrega | commit `cd97011` (CI #4 verde) |
| DEF-05 | Hotspot de segurança "CSRF desabilitado" no SonarCloud | Média | P2 | ✅ Tratado (falso positivo justificado) | Primeira análise do SonarCloud (nota D) | PR #1 |
| DEF-06 | Teste de integração com WireMock instável | Média | P1 | ✅ Corrigido | `NotificacaoWireMockIT` em execução local | PR #5 |
| DEF-07 | Avisos de depreciação do Node 20 nas GitHub Actions | Baixa | P3 | ⚠️ Parcial (aceito) | Anotações do CI #5 | PR #2 |
| DEF-08 | Migração Flyway `V1` alterada depois de aplicada | Baixa | P3 | ✅ Mitigado (regra de processo) | Revisão do diff do PR #1 | Regra registrada abaixo |

**Totais:** 8 defeitos. 6 corrigidos, 1 tratado como falso positivo justificado e 1 parcialmente corrigido e aceito. Nenhum defeito de severidade Crítica ou Alta está em aberto.

---

## DEF-01: Testcontainers não conversa com o Docker 29

- **Sintoma:** todos os testes de integração falhavam antes de começar, com erro de versão da API do Docker. O CI #1 ficou vermelho.
- **Causa raiz:** o Docker 29 deixou de aceitar a versão antiga da API usada por padrão pelo cliente `docker-java` do Testcontainers.
- **Correção:** arquivo `src/test/resources/docker-java.properties` com `api.version=1.44`, fixando a versão da API.
- **Verificação:** no CI #2, os testes de integração passaram a executar. O CI #2 só falhou depois, no gate de cobertura (DEF-02).
- **Lição:** atualizações do ambiente (Docker) quebram testes sem mudança de código. O ajuste fica versionado no repositório, e não só na máquina de quem encontrou o problema.

## DEF-02: Cobertura abaixo do gate

- **Sintoma:** o CI #2 reprovou no `jacoco:check` com 59% de linhas e 53% de branches, contra metas de 80% e 70%.
- **Causa raiz:** controlador, segurança e tratador de erros não tinham testes de API.
- **Correção:** `ReservaApiIT` com 27 testes caixa-preta (JWT real, RBAC, erros 400/401/403/404/405/409/422 e fluxo completo com auditoria).
- **Verificação:** CI #3 verde. Na `v1.0.0-rc1`, a cobertura está em 98,2% de linhas e 96,7% de branches (JaCoCo local) e 98,1% no SonarCloud.

## DEF-03: Erros do cliente respondiam 500

- **Sintoma:** `GET /api/reservas/abc` (UUID inválido), rota inexistente e método HTTP não permitido devolviam **500 Internal Server Error**, em vez de 400, 404 e 405.
- **Causa raiz:** o `@RestControllerAdvice` tinha um tratador genérico para `Exception` que capturava também as exceções que o Spring MVC já sabe traduzir.
- **Correção:** `TratadorGlobalExcecoes` passou a estender `ResponseEntityExceptionHandler`, que trata corretamente as exceções do próprio framework.
- **Verificação:** testes `identificadorMalformadoResponde400`, `rotaInexistenteResponde404`, `metodoNaoPermitidoResponde405` e `jsonMalformadoResponde400` em `ReservaApiIT`.

## DEF-04: Build não reproduzível com `./mvnw`

- **Sintoma:** o checklist de entrega exige `./mvnw -B verify` a partir de um clone limpo, mas o repositório não tinha Maven Wrapper. O build dependia do Maven instalado em cada máquina.
- **Correção:** Maven Wrapper adicionado (`mvnw`, `mvnw.cmd`, `.mvn/wrapper`, Maven 3.9.11) e usado no CI.
- **Verificação:** CI #4 verde com `./mvnw` e clone limpo registrado em `docs/entrega/checklist.md`.

## DEF-05: Hotspot "CSRF desabilitado" no SonarCloud

- **Sintoma:** a primeira análise apontou "Make sure disabling Spring Security's CSRF protection is safe here" em `ConfiguracaoSeguranca`, com nota de segurança D.
- **Análise:** a API é **stateless**, autentica pelo cabeçalho `Authorization: Bearer` e não usa cookies nem sessão. CSRF depende de o navegador enviar credenciais automaticamente (cookies), o que não acontece aqui. Trata-se de um falso positivo neste contexto.
- **Tratamento:** comentário `// NOSONAR` na linha, com a justificativa registrada aqui e no risco R-08 de `docs/decisao-qualidade.md`.
- **Verificação:** SonarCloud da `main` com 0 hotspots, 0 vulnerabilidades e nota A em segurança.

## DEF-06: Teste com WireMock instável

- **Sintoma:** em `NotificacaoWireMockIT`, o primeiro teste a rodar falhava com `Requests received: [ ]`, enquanto os demais passavam. Trocar a ordem dos testes apenas mudava qual deles falhava.
- **Investigação:** a primeira hipótese (interferência da resposta atrasada do teste de lentidão) foi testada fixando a ordem com `@Order` e **descartada**, porque o primeiro teste continuou falhando. O padrão "sempre o primeiro" indicou a causa real.
- **Causa raiz:** o teste configurava o timeout do cliente em 500 ms, valor menor que o tempo de "aquecimento" do WireMock na primeira requisição. O cliente desistia antes de o WireMock registrar a chamada.
- **Correção:** timeout do teste alinhado ao de produção (2 s) e atraso do teste de lentidão aumentado para 4 s, para continuar provando o timeout. A ordem fixa foi mantida para tornar o comportamento determinístico.
- **Verificação:** 49 testes de integração verdes, localmente, no CI do PR #5 e no clone limpo.
- **Lição:** timeout de teste deve ter folga sobre o tempo de inicialização do dublê. Testes instáveis são investigados até a causa raiz, e não "consertados" com novas tentativas.

## DEF-07: Depreciação do Node 20 nas Actions

- **Sintoma:** anotações "Node.js 20 is deprecated" no CI.
- **Correção parcial:** `actions/checkout` e `actions/setup-java` atualizados para `@v5` (PR #2).
- **Pendência aceita:** `actions/upload-artifact@v5` ainda gera o aviso. Não há impacto no resultado do CI. A atualização para `@v6` fica para a próxima versão.

## DEF-08: Migração `V1` alterada depois de aplicada

- **Sintoma:** o commit `5a0a82d` (PR #1) removeu comentários de `V1__schema_inicial.sql`. O Flyway guarda o checksum de cada migração aplicada e recusa iniciar um banco em que a `V1` já tenha sido aplicada com outro conteúdo.
- **Impacto real:** nenhum. Não existe banco persistente fora do ambiente de testes (Testcontainers cria um banco novo a cada execução), e o esquema não mudou, só os comentários.
- **Regra adotada:** migrações já versionadas **nunca** são editadas. Toda mudança de esquema vai em uma migração nova, como foi feito na `V2__usuario.sql`.
