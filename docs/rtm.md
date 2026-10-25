# Matriz de Rastreabilidade de Requisitos (RTM)

> Status das evidências: **PLANEJADO** = teste ainda não escrito; **ESCRITO** = teste existe no repositório mas ainda não tem execução de CI vinculada à tag `v1.0.0-rc1`; **VERDE** = execução de CI verde registrada em `docs/evidencias/README.md`. Nada está marcado como VERDE até o CI rodar de fato.

| ID | Requisito | Risco (P×I) | Implementação | Teste(s) | Status |
|----|-----------|-------------|---------------|----------|--------|
| RN-01 | Término estritamente posterior ao início | Reserva de duração zero/negativa corrompe a agenda (M×A) | `Reserva.validarPeriodo`; `CHECK ck_reserva_periodo` (V1) | `StatusReservaTest#rejeitaPeriodoInvalido` (parametrizado); API: `ReservaApiIT` (a escrever) | ESCRITO (unit) / PLANEJADO (API) |
| RN-02 | Sem sobreposição no mesmo recurso (sala, professor, material) | Conflito de horário, objetivo central do sistema (A×A) | `ReservaRepositorio.existeSobreposicao`; `EXCLUDE USING gist` (V1) | `ReservaConcorrenciaIT#reservasAdjacentesNaoSaoConflito`; sobreposição parcial/total/contida (a escrever) | ESCRITO (parcial) |
| RN-03 | Concorrência: exatamente 1 reserva aceita | Dupla reserva = reprovação imediata (M×Crítico) | `buscarPorIdComBloqueio` (lock pessimista) + exclusão gist | `ReservaConcorrenciaIT#duplaReservaSimultaneaProduzExatamenteUmaReservaAceita` (12 threads, PostgreSQL real) | ESCRITO |
| RN-04 | Recurso em manutenção não pode ser reservado/bloqueado | Alocar recurso indisponível (M×M) | `ReservaServico.criar` | `ReservaServicoIT#recursoEmManutencao` (a escrever) | PLANEJADO |
| RN-05 | Só Responsável aprova recurso restrito | Aprovação indevida / falha de autorização = nota zero (M×Crítico) | `ReservaServico.aprovar`; `ConfiguracaoSeguranca` | Testes de autorização por perfil e por recurso restrito (a escrever) | PLANEJADO |
| RN-06 | Reserva EM_USO/CONCLUIDA não pode ser apagada | Perda de histórico de uso (B×A) | `ReservaServico.excluir`; trigger `reserva_bloquear_exclusao` (V1) | `StatusReservaTest#estadosIniciados*`; IT de DELETE (serviço, API e SQL direto) (a escrever) | ESCRITO (unit) / PLANEJADO (IT) |
| RN-07 | Toda mudança de estado gera auditoria imutável | Falta de trilha auditável (M×A) | `RegistroAuditoria` (`@Immutable`); trigger `registro_auditoria_imutavel` (V1) | `ReservaConcorrenciaIT#criacaoGeraRegistroDeAuditoria`; IT de transições e de UPDATE/DELETE bloqueado (a escrever) | ESCRITO (parcial) |
| RF-ST | Máquina de estados SOLICITADA→APROVADA→EM_USO→CONCLUIDA + REJEITADA/CANCELADA/NAO_COMPARECEU | Fluxo inválido (M×A) | `StatusReserva`, `Reserva.transicionarPara` | `StatusReservaTest` (válidas e inválidas, parametrizado) | ESCRITO |
| RNF-SEG | Autenticação JWT e RBAC por endpoint | Acesso indevido (M×Crítico) | `ConfiguracaoSeguranca` | Matriz perfil × endpoint (a escrever) | PLANEJADO |
| RNF-ERR | Validação `@Valid` e `@RestControllerAdvice` | Vazamento de stack trace / respostas inconsistentes (M×M) | `TratadorGlobalExcecoes`, `CriarReservaRequisicao` | Testes de API para 400/403/404/409/422 (a escrever) | PLANEJADO |
| RNF-DES | Desempenho sob carga | Degradação sob concorrência (M×M) | — | Plano JMeter em `docs/performance/` (a escrever) | PLANEJADO |

## Lacunas conhecidas
- Ainda não existem: CRUD de recursos (perfil Administrador), endpoint de login/emissão de JWT, consulta de disponibilidade, bloqueios/janelas de manutenção, uso de WireMock (exige uma integração externa real para justificar), workflow de CI, configuração SonarCloud e plano JMeter.
- A cobertura de 80% linhas / 70% branches só será avaliada quando `./mvnw -B verify` rodar; hoje os testes cobrem apenas parte do código.
