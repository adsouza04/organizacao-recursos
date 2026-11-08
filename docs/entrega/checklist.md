# Checklist de entrega: clone limpo

Checklist auditável da `v1.0.0-rc1`, executado a partir de um **clone novo** do repositório público, sem reaproveitar nenhum arquivo local.

## 1. Execução

| Item | Valor |
|---|---|
| Data e hora | 08/11/2026, término às 18:44:45 (BRT) |
| Executado por | Yasmin de Souza Santos |
| Máquina | Windows, 8 GB de RAM, Docker Desktop/WSL2 |
| Java | OpenJDK Temurin `21.0.12.1` LTS |
| Docker | `29.9.2` |
| Pasta | `C:\entrega-clone` (fora da pasta de trabalho e fora do OneDrive) |
| Commit clonado | `fc1e3be`: "Merge pull request #6 from adsouza04/feature/performance-jmeter" |

**Observação:** o commit clonado contém **todo o código** da release. A tag `v1.0.0-rc1` aponta para o commit seguinte, que acrescenta apenas documentação (PR #7) e passa pelo mesmo build no CI da tag (`docs/evidencias/README.md`).

## 2. Comandos executados

```powershell
git clone https://github.com/adsouza04/organizacao-recursos.git C:\entrega-clone
cd C:\entrega-clone
git log -1 --format="%h %an %s"
java -version
docker --version
.\mvnw.cmd -B verify | Tee-Object -FilePath "$HOME\entrega-clone-verify.log"
```

No Linux e no macOS, o comando equivalente é `./mvnw -B verify`. Log completo: [`clone-limpo-verify.log`](clone-limpo-verify.log).

## 3. Resultado do build

| Verificação | Resultado |
|---|---|
| Maven Wrapper baixou o Maven sem instalação prévia | ✅ |
| Compilação | ✅ |
| Testes unitários (Surefire) | ✅ 34 executados, 0 falhas |
| `AutenticacaoApiIT` | ✅ 8 / 0 falhas |
| `NotificacaoWireMockIT` | ✅ 4 / 0 falhas |
| `RecursoApiIT` | ✅ 7 / 0 falhas |
| `ReservaApiIT` | ✅ 27 / 0 falhas |
| `ReservaConcorrenciaIT` | ✅ 3 / 0 falhas |
| **Total de integração (Failsafe)** | ✅ **49 executados, 0 falhas, 0 erros, 0 ignorados** |
| Gate do JaCoCo | ✅ "All coverage checks have been met" (98,2% linhas / 96,7% branches) |
| **Resultado final** | ✅ **BUILD SUCCESS em 59,4 s** |

## 4. Itens de entrega

| # | Entregável | Caminho | Situação |
|---|---|---|---|
| 1 | Instruções do zero (build, execução, testes) | `README.md` | ✅ |
| 2 | Matriz de rastreabilidade | `docs/rtm.md` | ✅ 7/7 regras críticas com teste verde |
| 3 | Diagramas C4 e sequências (concorrência e auditoria) | `docs/diagramas/arquitetura.md` | ✅ |
| 4 | Checklist de clone limpo | `docs/entrega/checklist.md` | ✅ (este arquivo) |
| 5 | CHANGELOG com a release candidate | `CHANGELOG.md` | ✅ `v1.0.0-rc1` |
| 6 | Índice de evidências | `docs/evidencias/README.md` | ✅ |
| 7 | Plano e resultado de carga | `docs/performance/resultado-01.md` | ✅ |
| 8 | Defeitos priorizados e corrigidos | `docs/defeitos/README.md` | ✅ 8 registrados |
| 9 | Decisão de qualidade, riscos e autoria | `docs/decisao-qualidade.md` | ✅ |
| 10 | Relatório executivo | `docs/relatorio-qualidade.md` | ✅ |
| — | Plano de testes | `docs/plano-testes.md` | ✅ |

## 5. Critérios de qualidade

| Critério | Meta | Medido | Situação |
|---|---|---|---|
| Build limpo | verde | BUILD SUCCESS | ✅ |
| Pipeline de CI em PR | existe e verde | PRs #1 a #6 verdes | ✅ |
| Cobertura de linhas | ≥ 80% | 98,2% | ✅ |
| Cobertura de branches | ≥ 70% | 96,7% | ✅ |
| Requisitos críticos na RTM | 100% | 7/7 | ✅ |
| Bugs / vulnerabilidades (SonarCloud) | 0 | 0 / 0 | ✅ |
| Autorização nos endpoints | sem falhas | testes de RBAC verdes | ✅ |
| Dupla reserva | impossível | IT com 12 threads + JMeter 1 × 201 / 49 × 409 | ✅ |
| TDD com evidência | ≥ 1 funcionalidade | login JWT (PR #3) | ✅ |
| WireMock | integração externa testada | `NotificacaoWireMockIT` | ✅ |
| JMeter | plano + resultado | `resultado-01.md` | ✅ |

## 6. Bloqueadores

**Nenhum bloqueador encontrado.** As pendências conhecidas são funcionalidades fora do escopo da `v1.0.0-rc1` e estão registradas como ressalvas (riscos R-01 a R-03) em `docs/decisao-qualidade.md`.

## 7. Sequência de liberação após este checklist

1. Documentação de entrega integrada à `main` pelo PR #7, com CI verde e revisão do outro integrante.
2. Tag **anotada** `v1.0.0-rc1` criada sobre o commit da `main` resultante do PR #7 e enviada ao GitHub.
3. O push da tag dispara o CI. O resultado fica consultável em https://github.com/adsouza04/organizacao-recursos/actions/workflows/ci.yml?query=branch%3Av1.0.0-rc1, link também registrado em `docs/evidencias/README.md`.
4. Texto do formulário do Hub (seção 7 de `docs/decisao-qualidade.md`) enviado com o link do repositório e da tag.
