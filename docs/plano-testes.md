# Plano de testes

## 1. Objetivo

Garantir, com evidência automatizada e reproduzível, que as regras críticas RN-01 a RN-07 se mantêm sob uso normal, sob erro e sob concorrência, e que nenhum perfil acessa o que não deve.

## 2. Estratégia (pirâmide de testes)

| Nível | Ferramentas | O que cobre | Onde roda |
|---|---|---|---|
| **Unitário** | JUnit 5, `@ParameterizedTest` | Máquina de estados, validação de período, entidade `Recurso`, tradução de erro inesperado | `mvn test` (sem Docker) |
| **Integração com banco real** | JUnit 5, Testcontainers (PostgreSQL 16) | Concorrência com threads reais, constraints e triggers do banco, auditoria | `mvn verify` (fase `integration-test`) |
| **API caixa-preta** | MockMvc + JWT real (emitido pelo login) | Contrato HTTP, RBAC por perfil, códigos 400/401/403/404/405/409/422 | `mvn verify` |
| **Integração externa** | WireMock | Notificação: conteúdo da requisição, falha 503, timeout | `mvn verify` |
| **Ponta a ponta (fluxo)** | MockMvc com login real | Login → criação de recurso → manutenção → reserva recusada → saída de manutenção → reserva aceita; e login → reserva → aprovação → notificação | `RecursoApiIT`, `NotificacaoWireMockIT` |
| **Carga e concorrência** | Apache JMeter 5.6.3 | 50 requisições simultâneas ao mesmo horário e leitura sustentada | Manual, `docs/performance/` |
| **Análise estática** | SonarCloud | Bugs, vulnerabilidades, hotspots, duplicação, cobertura | CI (todo PR e a `main`) |

## 3. Política de dublês (mocks)

- **Mockito** é permitido apenas em testes unitários isolados. Persistência e concorrência **nunca** são simuladas: usam PostgreSQL real via Testcontainers.
- Serviços **externos** ao sistema (notificação) são simulados com **WireMock**, por HTTP de verdade, e não com mocks de classe.
- Não há H2 nem banco em memória: as regras do banco (EXCLUDE gist e triggers) só existem no PostgreSQL.

## 4. Inventário da suíte (v1.0.0-rc1)

| Classe | Tipo | Testes | Foco |
|---|---|---:|---|
| `StatusReservaTest` | Unitário parametrizado | 28 | Transições válidas e inválidas, estados iniciados, período (RN-01, RN-06, RF-ST) |
| `RecursoTest` | Unitário | 5 | Recurso nasce ativo, manutenção alterna o status |
| `TratadorGlobalExcecoesTest` | Unitário | 1 | 500 sem vazar detalhes internos |
| `ReservaConcorrenciaIT` | Integração (Testcontainers) | 3 | 12 threads disputando o mesmo horário (RN-03), adjacência, auditoria |
| `ReservaApiIT` | API caixa-preta | 27 | RBAC, erros, sobreposição, manutenção, fluxo com auditoria e exclusão bloqueada |
| `AutenticacaoApiIT` | API (TDD) | 8 | Login por perfil, 401, 400, token emitido autentica e respeita o perfil |
| `RecursoApiIT` | API / ponta a ponta | 7 | Gestão de recursos pelo Administrador, RN-04 ponta a ponta |
| `NotificacaoWireMockIT` | Integração externa | 4 | Sucesso, rejeição, falha 503 e timeout |
| **Total** | | **83** | 34 unitários e 49 de integração |

## 5. Funcionalidade desenvolvida em TDD

O **login com emissão de JWT** foi desenvolvido em ciclo vermelho → verde → refatoração, com um commit por etapa, todos do integrante Luan Bezerra Guimarães, no PR #3:

| Etapa | Commit | Evidência |
|---|---|---|
| Vermelho: teste escrito antes do código | `b8b4c85` | CI #9 vermelho com `Tests run: 8, Failures: 8` em `AutenticacaoApiIT` |
| Verde: implementação mínima | `2c06e40` | CI #10 verde, Quality Gate do SonarCloud aprovado |
| Refatoração: extração do `EmissorToken` | `91fa2f5` | Mesmos 8 testes verdes, cobertura mantida |

Os prints estão em `docs/evidencias/prints-evidencias.pdf`, indexados em `docs/evidencias/README.md`.

## 6. Critérios de entrada e saída

**Entrada de um PR na `main`:**
1. CI verde (`./mvnw -B verify`: testes, gate do JaCoCo e Quality Gate do SonarCloud).
2. Revisão aprovada pelo outro integrante.
3. Merge com "Create a merge commit", para preservar o histórico de commits (essencial para a evidência do TDD).

**Saída (liberação da release candidate):**
1. Cobertura ≥ 80% de linhas e ≥ 70% de branches.
2. SonarCloud com 0 bugs e 0 vulnerabilidades.
3. 100% dos requisitos críticos com teste verde na RTM.
4. Nenhum defeito de severidade Crítica ou Alta em aberto.
5. Build verde a partir de clone limpo.
6. CI verde na tag `v1.0.0-rc1`.

## 7. Ambientes

| Ambiente | Uso |
|---|---|
| Máquina local (Windows, Docker Desktop/WSL2) | Desenvolvimento, `mvnw verify`, teste de carga |
| GitHub Actions (`ubuntu-latest`) | CI de todo PR, da `main` e das tags `v*` |
| Testcontainers | PostgreSQL 16 descartável a cada execução da suíte |

## 8. Riscos do próprio plano

- O teste de carga roda na mesma máquina da aplicação, então os tempos absolutos são conservadores (risco R-07).
- Funcionalidades de prioridade P1 não implementadas (disponibilidade, alteração de reserva, bloqueios, CRUD de usuários) não têm testes porque não existem. Estão registradas como ressalvas (R-01 a R-03).
