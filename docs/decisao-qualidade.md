# Decisão de qualidade: v1.0.0-rc1

| Item | Valor |
|---|---|
| Versão avaliada | `v1.0.0-rc1` (tag anotada) |
| Data | 08/11/2026 |
| Responsáveis pela decisão | Yasmin de Souza Santos e Luan Bezerra Guimarães |

## 1. Decisão

# ✅ LIBERAR COM RESSALVAS

Todos os critérios obrigatórios de qualidade foram atendidos com evidência: build limpo, CI verde, cobertura, SonarCloud, regras críticas, autorização, concorrência, TDD, WireMock e JMeter. As ressalvas referem-se a funcionalidades secundárias **não implementadas** nesta versão e a riscos técnicos conhecidos, todos com impacto avaliado, justificativa e responsável nominal (seção 4).

## 2. Critérios usados e evidência de cada um

| # | Critério | Meta | Resultado | Evidência | Situação |
|---|---|---|---|---|---|
| C1 | Build a partir de clone limpo | `./mvnw -B verify` verde | BUILD SUCCESS em 59,4 s, 83 testes, 0 falhas | `docs/entrega/checklist.md` e `docs/entrega/clone-limpo-verify.log` | ✅ |
| C2 | Pipeline de CI em Pull Requests | Existe e verde | CI em todos os PRs (#1 a #7), na `main` e na tag | `docs/evidencias/README.md`, seções 1 e 9 | ✅ |
| C3 | Cobertura de linhas | ≥ 80% | **98,2%** | JaCoCo (`jacoco:check`); SonarCloud 98,1% | ✅ |
| C4 | Cobertura de branches | ≥ 70% | **96,7%** | JaCoCo | ✅ |
| C5 | Requisitos críticos na RTM | 100% | **7/7** (RN-01 a RN-07) com teste verde | `docs/rtm.md` | ✅ |
| C6 | Bugs e vulnerabilidades críticas | 0 | **0 bugs, 0 vulnerabilidades, 0 hotspots**, Quality Gate Passed | SonarCloud da `main` | ✅ |
| C7 | Autorização nos endpoints | Nenhuma falha | 401/403 testados por perfil em reservas, recursos e login | `ReservaApiIT`, `RecursoApiIT`, `AutenticacaoApiIT` | ✅ |
| C8 | Impossibilidade de dupla reserva | Exatamente 1 aceita | IT com 12 threads; JMeter com 50 simultâneas: **1 × 201, 49 × 409** | `ReservaConcorrenciaIT`, `docs/performance/resultado-01.md` | ✅ |
| C9 | Funcionalidade em TDD com evidência | ≥ 1 | Login JWT: `test` (CI #9 ❌) → `feat` (CI #10 ✅) → `refactor` | PR #3, `docs/evidencias/prints-evidencias.pdf` | ✅ |
| C10 | Integração externa testada com WireMock | Sim | Sucesso, rejeição, falha 503 e timeout | `NotificacaoWireMockIT` | ✅ |
| C11 | Teste de carga JMeter | Plano + resultado | 7.462 requisições, 0% de erro, P95 de leitura 30 ms | `docs/performance/` | ✅ |
| C12 | Validação (`@Valid`) e tratamento global de erros | Respostas 4xx coerentes, sem vazar detalhes | 400/401/403/404/405/409/422/500 testados | `ReservaApiIT`, `TratadorGlobalExcecoesTest` | ✅ |
| C13 | Defeitos Críticos ou Altos em aberto | 0 | 0 (8 registrados: 6 corrigidos, 1 falso positivo justificado, 1 aceito) | `docs/defeitos/README.md` | ✅ |
| C14 | Escopo funcional completo | 100% dos requisitos funcionais | 7 de 11 requisitos funcionais implementados | `docs/rtm.md`, seção 2 | ⚠️ ressalva |

## 3. Justificativa

- **Por que não "liberar" sem ressalvas:** o critério C14 não foi atendido por completo. Consulta de disponibilidade, alteração de reserva, bloqueios de agenda, gestão de usuários e listagem de retiradas/devoluções não estão nesta versão. Declarar a liberação plena seria omitir essas lacunas.
- **Por que não "bloquear":** nenhuma lacuna compromete as regras críticas, a segurança ou a consistência dos dados. Tudo o que está implementado está testado, com cobertura acima de 96%, e os quatro "critérios zero" do plano (build limpo, CI, autorização e dupla reserva) estão comprovados. As funcionalidades ausentes têm contorno operacional (seção 4).
- **Priorização adotada:** com prazo fixo, priorizamos primeiro o que pode zerar a avaliação (critérios zero) e as exigências de qualidade (TDD, WireMock, JMeter, SonarCloud, RTM), depois a funcionalidade secundária. Essa ordem está registrada no histórico de PRs (#1 a #7).

## 4. Matriz de riscos aceitos

| ID | Risco / ressalva | Impacto | Prob. | Justificativa para aceitar | Mitigação / plano | Responsável |
|---|---|---|---|---|---|---|
| R-01 | **Consulta de disponibilidade** e **alteração de reserva** não implementadas | Médio: o solicitante descobre o conflito ao tentar reservar (409) e precisa cancelar e recriar para alterar | Alta | Os conflitos continuam **impossíveis** (RN-02/RN-03 garantidas). A ausência afeta a conveniência, não a consistência | Implementar `GET /api/recursos/{id}/disponibilidade` e `PATCH /api/reservas/{id}` na v1.0.0, com testes de API e de concorrência na alteração | **Yasmin de Souza Santos** |
| R-02 | **Bloqueios de agenda** e **gestão de usuários** (CRUD) pelo Administrador não implementados. Por consequência, a parte "nem bloqueado" da RN-04 não se aplica | Médio: bloqueios institucionais exigem uma reserva comum; usuários só podem ser criados por migração | Média | A manutenção de recursos (parte principal da RN-04) está implementada e testada ponta a ponta. Usuários são poucos e fixos no contexto acadêmico da entrega | Implementar bloqueios como entidade própria sujeita à mesma checagem de sobreposição e ao mesmo lock; CRUD de usuários com senha BCrypt; ambos com testes de RBAC | **Luan Bezerra Guimarães** |
| R-03 | **Listagem de retiradas e devoluções** para o Responsável não implementada | Baixo: as transições de retirada (iniciar) e devolução (concluir) funcionam e são auditadas; falta só a tela/consulta agregada | Média | A trilha completa existe em `registro_auditoria` e pode ser consultada no banco | `GET /api/reservas?status=EM_USO` com filtro por período | **Luan Bezerra Guimarães** |
| R-04 | **Spring Boot 3.3.5 fora do suporte OSS** | Médio: correções de segurança futuras do framework não chegam a esta linha | Média | Atualizar o framework às vésperas da entrega arriscaria regressões sem tempo de validação. O SonarCloud não aponta vulnerabilidades no código | Atualizar para a linha 3.5.x suportada no início da próxima versão, com a suíte completa como rede de segurança | **Yasmin de Souza Santos** |
| R-05 | **Usuários de demonstração com senha conhecida** (`Senha@123`) criados pela migração `V2` | Alto **se** for para produção como está | Baixa (ambiente acadêmico) | Necessários para o professor e o JMeter usarem o login sem cadastro prévio; senha guardada como hash BCrypt | Mover a carga desses usuários para um perfil `dev` ou trocar as senhas no primeiro deploy; documentado no README | **Luan Bezerra Guimarães** |
| R-06 | **Notificação em melhor esforço, sem retentativa nem fila** | Baixo: em falha do serviço externo, o solicitante não recebe o aviso, mas a decisão é gravada e auditada | Média | Desfazer a aprovação por falha de e-mail seria pior para o negócio. A falha é registrada em log | Fila (outbox) com retentativa assíncrona | **Yasmin de Souza Santos** |
| R-07 | **Teste de carga com JMeter e aplicação na mesma máquina** | Baixo: os tempos absolutos são conservadores e não representam um servidor de produção | Alta | O objetivo principal (provar a RN-03 sob disputa real) não é afetado; a contenção extra só fortalece a evidência | Repetir a carga em ambiente dedicado antes de dimensionar a produção | **Yasmin de Souza Santos** |
| R-08 | **Hotspot de CSRF suprimido com `// NOSONAR`** | Baixo | Baixa | API stateless, JWT no cabeçalho `Authorization`, sem cookies nem sessão: CSRF não se aplica (DEF-05) | Revisar se algum dia a API passar a usar cookies | **Yasmin de Souza Santos** |
| R-09 | **Segredo JWT de desenvolvimento** como padrão no `application.yml` | Alto **se** usado em produção | Baixa | O valor é substituível pela variável `JWT_SECRET`, exigência documentada no README | Falhar a inicialização sem `JWT_SECRET` fora do perfil de desenvolvimento | **Luan Bezerra Guimarães** |
| R-10 | **Migração `V1` editada após aplicada** (só comentários, commit `5a0a82d`) | Baixo: só afetaria um banco persistente criado antes do commit, e não existe nenhum | Baixa | O esquema não mudou e todos os ambientes são descartáveis (DEF-08) | Regra de processo: migrações versionadas são imutáveis | **Yasmin de Souza Santos** |
| R-11 | **PR #5 integrado antes da revisão** do outro integrante | Baixo: revisão feita depois do merge, sem achados que exigissem mudança | Baixa | CI e Quality Gate estavam verdes no momento do merge | Ativar proteção da `main` exigindo 1 aprovação antes do merge | **Yasmin de Souza Santos** |

## 5. Condições para a v1.0.0 (versão final)

1. Fechar R-01, R-02 e R-03 com testes (API, RBAC e concorrência na alteração de reserva).
2. Atualizar o Spring Boot para uma linha suportada (R-04).
3. Remover os usuários de demonstração do perfil padrão (R-05) e exigir `JWT_SECRET` (R-09).
4. Manter todos os critérios C1 a C13 verdes.

## 6. Declaração de autoria

Declaramos que o trabalho abaixo foi realizado pelos integrantes indicados e que cada commit do repositório está registrado com o nome e o e-mail de quem o escreveu. Parte das sessões foi feita **em par, no mesmo computador** (o da Yasmin). Nelas, os commits do Luan foram feitos com a identidade dele (`git -c user.name=... -c user.email=...`), e os Pull Requests foram abertos e revisados por cada um na própria conta do GitHub.

### Yasmin de Souza Santos ([@adsouza04](https://github.com/adsouza04))

- Estrutura inicial do sistema: entidades, máquina de estados, migração `V1` (CHECK, EXCLUDE gist, triggers), `ReservaServico` com concorrência em três camadas, segurança JWT/RBAC e tratamento global de erros.
- Testes `StatusReservaTest`, `RecursoTest`, `TratadorGlobalExcecoesTest`, `ReservaConcorrenciaIT` e `ReservaApiIT`.
- CI no GitHub Actions, Maven Wrapper, integração com o SonarCloud (PRs #1 e #2).
- Integração externa de notificação e `NotificacaoWireMockIT`, incluindo a investigação do teste instável (PR #5).
- Plano e execução do teste de carga JMeter (PR #6).
- Documentação: README, RTM, CHANGELOG, índice de evidências, checklist de clone limpo, esta decisão e o relatório executivo.
- Revisão e aprovação dos PRs #3 e #4.

*Yasmin de Souza Santos*

### Luan Bezerra Guimarães ([@Luan46guimaraes](https://github.com/Luan46guimaraes))

- Login com emissão de JWT em **TDD**: teste vermelho, implementação e refatoração com o `EmissorToken` (PR #3, commits `b8b4c85`, `2c06e40` e `91fa2f5`), incluindo a migração `V2` e o `AutenticacaoApiIT`.
- Gestão de recursos e manutenção pelo Administrador, com `RecursoApiIT` cobrindo a RN-04 ponta a ponta (PR #4).
- Documentação: diagramas C4 e de sequência, registro de defeitos e plano de testes.
- Revisão e aprovação dos PRs #1, #2 e #6; revisão do PR #5 após o merge; merge do PR #6.

*Luan Bezerra Guimarães*

## 7. Texto para o formulário de entrega do Hub

```text
Sistema de Organização de Recursos — Release Candidate v1.0.0-rc1

Repositório público: https://github.com/adsouza04/organizacao-recursos
Tag anotada: v1.0.0-rc1 — https://github.com/adsouza04/organizacao-recursos/tree/v1.0.0-rc1
CI da tag: https://github.com/adsouza04/organizacao-recursos/actions/workflows/ci.yml?query=branch%3Av1.0.0-rc1
Evidências: docs/evidencias/README.md | Decisão: docs/decisao-qualidade.md

Decisão de qualidade: LIBERAR COM RESSALVAS.
Build limpo verde (./mvnw -B verify, 83 testes); cobertura 98,2% linhas / 96,7% branches;
SonarCloud: Quality Gate aprovado, 0 bugs, 0 vulnerabilidades; RTM com 7/7 regras críticas verdes;
dupla reserva impossível (JMeter: 50 simultâneas -> 1 aceita, 49 com 409); login JWT em TDD; WireMock.

Integrantes: Yasmin de Souza Santos (@adsouza04) e Luan Bezerra Guimarães (@Luan46guimaraes).
```
