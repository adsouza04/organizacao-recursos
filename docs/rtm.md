# Matriz de Rastreabilidade de Requisitos (RTM)

Versão `v1.0.0-rc1`. Cadeia: **Requisito → Risco → Implementação → Teste → Evidência**.

**Legenda de status:**
- **VERDE** = teste automatizado existente e aprovado no CI do GitHub Actions. O PR indicado mostra o check verde, e a execução final consolidada é o CI da tag `v1.0.0-rc1` (`docs/evidencias/README.md`).
- **NÃO IMPLEMENTADO** = funcionalidade fora da `v1.0.0-rc1`, aceita como ressalva em `docs/decisao-qualidade.md`.

Nenhuma linha está marcada como VERDE sem execução de CI correspondente.

**Risco** = Probabilidade × Impacto (B = baixo, M = médio, A = alto, C = crítico).

## 1. Regras críticas de negócio (RN)

| ID | Requisito | Risco | Implementação | Testes | Evidência | Status |
|---|---|---|---|---|---|---|
| RN-01 | Término estritamente posterior ao início | Reserva de duração zero/negativa corrompe a agenda (M×A) | `Reserva.validarPeriodo`; `CHECK ck_reserva_periodo` (V1) | `StatusReservaTest#rejeitaPeriodoInvalido` (parametrizado); `ReservaApiIT#terminoIgualAoInicioResponde422` | CI #3 (commit `325018d`); PRs #1 a #6 | **VERDE** |
| RN-02 | Sem sobreposição no mesmo recurso (sala, professor ou material) | Conflito de horário, objetivo central do sistema (A×A) | `ReservaRepositorio.existeSobreposicao`; `EXCLUDE USING gist` (V1) | `ReservaApiIT#reservaSobrepostaResponde409`; `ReservaConcorrenciaIT#reservasAdjacentesNaoSaoConflito`; `ReservaApiIT#reservaRejeitadaLiberaOHorarioParaNovaReserva` | CI #3; PRs #1 a #6 | **VERDE** |
| RN-03 | Concorrência: exatamente 1 reserva aceita | Dupla reserva = reprovação imediata (M×C) | `RecursoRepositorio.buscarPorIdComBloqueio` (lock pessimista) + checagem dentro do lock + EXCLUDE gist | `ReservaConcorrenciaIT#duplaReservaSimultaneaProduzExatamenteUmaReservaAceita` (12 threads, PostgreSQL real); JMeter, cenário 1 (50 requisições simultâneas) | CI #3; `docs/performance/resultado-01.md` (1 × 201, 49 × 409) | **VERDE** |
| RN-04 | Recurso em manutenção não pode ser reservado | Alocar recurso indisponível (M×M) | `ReservaServico.criar` (checagem dentro do lock); `RecursoServico.entrarEmManutencao` (mesmo lock) | `ReservaApiIT#recursoEmManutencaoNaoPodeSerReservado`; `RecursoApiIT#recursoEmManutencaoRecusaReservaEVoltaAAceitarAoSair` (ponta a ponta pela API); `RecursoTest#manutencaoAlternaOStatus` | PR #4 | **VERDE** (reserva). A parte "nem bloqueado" depende da funcionalidade de bloqueios (R-02) |
| RN-05 | Somente Responsável aprova recurso restrito | Aprovação indevida / falha de autorização (M×C) | `ReservaServico.aprovar`; `ConfiguracaoSeguranca` | `ReservaApiIT#somenteResponsavelAprovaRecursoRestrito`; `#solicitanteNaoAprovaReserva`; `#administradorAprovaRecursoNaoRestrito` | CI #3; PRs #1 a #6 | **VERDE** |
| RN-06 | Reserva EM_USO ou CONCLUIDA não pode ser apagada | Perda de histórico de uso (B×A) | `ReservaServico.excluir`; trigger `reserva_bloquear_exclusao` (V1) | `StatusReservaTest#estadosIniciadosSaoMarcadosComoIniciados`; `#estadosNaoIniciadosPodemSerApagados`; `ReservaApiIT#fluxoCompletoGeraAuditoriaEBloqueiaExclusaoDeReservaIniciada`; `#donoExcluiReservaNaoIniciada` | CI #3; PRs #1 a #6 | **VERDE** |
| RN-07 | Toda mudança de estado gera auditoria imutável | Falta de trilha auditável (M×A) | `RegistroAuditoria` (`@Immutable`); trigger `registro_auditoria_imutavel` (V1) | `ReservaConcorrenciaIT#criacaoGeraRegistroDeAuditoria`; `ReservaApiIT#fluxoCompletoGeraAuditoriaEBloqueiaExclusaoDeReservaIniciada` | CI #3; PRs #1 a #6 | **VERDE** |

**Cobertura dos requisitos críticos na RTM: 7 de 7 (100%) com teste verde em CI.**

## 2. Requisitos funcionais (RF)

| ID | Requisito | Risco | Implementação | Testes | Evidência | Status |
|---|---|---|---|---|---|---|
| RF-ST | Máquina de estados SOLICITADA → APROVADA → EM_USO → CONCLUIDA, mais REJEITADA, CANCELADA e NAO_COMPARECEU | Fluxo inválido (M×A) | `StatusReserva`, `Reserva.transicionarPara` | `StatusReservaTest` (28 casos parametrizados); `ReservaApiIT#transicaoForaDeOrdemResponde422`, `#responsavelRejeitaReserva`, `#responsavelMarcaNaoComparecimentoDeReservaAprovada` | CI #3; PRs #1 a #6 | **VERDE** |
| RF-AUT | Login emite JWT com o perfil do usuário (**TDD**) | Sem login, o RBAC não é utilizável (M×A) | `AutenticacaoControlador`, `AutenticacaoServico`, `EmissorToken`, `V2__usuario.sql` | `AutenticacaoApiIT` (8 testes: login por perfil, 401, 400, token autentica, perfil respeitado) | PR #3: CI #9 vermelho (`b8b4c85`), CI #10 verde (`2c06e40`), refactor `91fa2f5` | **VERDE** |
| RF-REC | Administrador cadastra e lista recursos | Recursos só cadastráveis por SQL (M×M) | `RecursoControlador`, `RecursoServico` | `RecursoApiIT#administradorCriaRecurso`, `#qualquerPerfilAutenticadoListaRecursos`, `#nomeEmBrancoResponde400` | PR #4 | **VERDE** |
| RF-MAN | Administrador coloca e tira recurso de manutenção | RN-04 inacessível pela API (M×M) | `RecursoServico.entrarEmManutencao` / `sairDaManutencao` | `RecursoApiIT#recursoEmManutencaoRecusaReservaEVoltaAAceitarAoSair`, `#responsavelNaoPodeColocarRecursoEmManutencao`, `#manutencaoDeRecursoInexistenteResponde404` | PR #4 | **VERDE** |
| RF-CAN | Solicitante cancela a própria reserva; Administrador cancela qualquer uma | Cancelamento indevido (M×M) | `ReservaServico.cancelar` | `ReservaApiIT#donoCancelaAPropriaReserva`, `#solicitanteNaoCancelaReservaDeOutro`, `#administradorCancelaReservaDeOutro` | CI #3 | **VERDE** |
| RF-RET | Retirada (iniciar) e devolução (concluir) registradas pelo Responsável | Uso sem registro (M×M) | `ReservaServico.iniciar` / `concluir` | `ReservaApiIT#fluxoCompletoGeraAuditoriaEBloqueiaExclusaoDeReservaIniciada` | CI #3 | **VERDE** (transições). A listagem de acompanhamento não está implementada (R-03) |
| RF-NOT | Notificar o solicitante na aprovação ou rejeição (integração externa) | Solicitante não fica sabendo da decisão; falha externa derrubar a aprovação (M×M) | `NotificacaoCliente` (RestClient, timeout de 2 s, melhor esforço) | `NotificacaoWireMockIT` (sucesso, rejeição, falha 503, timeout) | PR #5 | **VERDE** |
| RF-DISP | Consulta de disponibilidade | Solicitante tenta reservar às cegas (M×M) | — | — | — | **NÃO IMPLEMENTADO** (R-01) |
| RF-ALT | Alteração de reserva | Solicitante precisa cancelar e recriar (B×M) | — | — | — | **NÃO IMPLEMENTADO** (R-01) |
| RF-BLQ | Bloqueios de agenda pelo Administrador | Eventos institucionais sem reserva formal (M×M) | — | — | — | **NÃO IMPLEMENTADO** (R-02) |
| RF-USR | Gestão de usuários pelo Administrador | Usuários só cadastráveis por migração (M×M) | Tabela `usuario` (V2) | — | — | **NÃO IMPLEMENTADO** (R-02) |

## 3. Requisitos não funcionais (RNF)

| ID | Requisito | Risco | Implementação | Testes / verificação | Evidência | Status |
|---|---|---|---|---|---|---|
| RNF-SEG | Autenticação JWT e RBAC por endpoint | Acesso indevido = nota zero na dimensão (M×C) | `ConfiguracaoSeguranca` (regras por método e rota) | `ReservaApiIT#semTokenResponde401`, `#tokenInvalidoResponde401`, `#responsavelNaoPodeCriarReserva`, `#solicitanteNaoAprovaReserva`; `RecursoApiIT#solicitanteNaoPodeCriarRecurso`; `AutenticacaoApiIT#tokenDoSolicitanteNaoPermiteAprovarReserva` | PRs #3 e #4 | **VERDE** |
| RNF-VAL | Validação de entrada com `@Valid` | Dados inválidos no banco (M×M) | `CriarReservaRequisicao`, `LoginRequisicao`, `CriarRecursoRequisicao` | `ReservaApiIT#corpoSemCamposObrigatoriosResponde400`, `#jsonMalformadoResponde400`; `AutenticacaoApiIT#camposEmBrancoResponde400`; `RecursoApiIT#nomeEmBrancoResponde400` | PRs #3 e #4 | **VERDE** |
| RNF-ERR | Tratamento global de erros (`@RestControllerAdvice`) | Vazamento de stack trace ou respostas inconsistentes (M×M) | `TratadorGlobalExcecoes` (estende `ResponseEntityExceptionHandler`) | `TratadorGlobalExcecoesTest#erroInesperadoResponde500SemVazarDetalhes`; `ReservaApiIT#identificadorMalformadoResponde400`, `#rotaInexistenteResponde404`, `#metodoNaoPermitidoResponde405` | CI #3 (DEF-03) | **VERDE** |
| RNF-DES | Desempenho e consistência sob carga | Degradação ou dupla reserva sob concorrência (M×A) | Lock pessimista; pool padrão do HikariCP | JMeter: 7.462 requisições, 0% de erro, P95 de leitura 30 ms, disputa 1 × 201 / 49 × 409 | `docs/performance/resultado-01.md`, PR #6 | **VERDE** (execução manual registrada) |
| RNF-COB | Cobertura ≥ 80% linhas e ≥ 70% branches | Código crítico sem teste (M×A) | `jacoco-maven-plugin` (`check` no `verify`) | Gate do JaCoCo em todo build | 98,2% linhas / 96,7% branches (clone limpo); SonarCloud 98,1% | **VERDE** |
| RNF-EST | SonarCloud com 0 bugs e 0 vulnerabilidades | Defeitos e falhas de segurança não detectados (M×A) | Análise no CI com `sonar.qualitygate.wait=true` | Quality Gate em todo PR e na `main` | `main` (`fc1e3be`): Quality Gate Passed, 0 bugs, 0 vulnerabilidades, 0 hotspots | **VERDE** |
| RNF-CI | CI em todo Pull Request | Regressão entra sem ser vista (M×A) | `.github/workflows/ci.yml` (PR, push na `main`, tags `v*`) | Execução em todos os PRs (#1 a #6) | Aba Actions | **VERDE** |
| RNF-REP | Build reproduzível a partir de clone limpo | O professor não consegue executar = nota zero na dimensão (B×C) | Maven Wrapper; `docker-java.properties` | `./mvnw -B verify` em clone limpo | `docs/entrega/checklist.md` | **VERDE** |

## 4. Resumo

| Grupo | Total | VERDE | NÃO IMPLEMENTADO |
|---|---:|---:|---:|
| Regras críticas (RN) | 7 | **7 (100%)** | 0 |
| Funcionais (RF) | 11 | 7 | 4 (ressalvas R-01 e R-02) |
| Não funcionais (RNF) | 8 | 8 | 0 |
