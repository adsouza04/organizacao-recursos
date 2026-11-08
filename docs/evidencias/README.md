# Índice de evidências: v1.0.0-rc1

Os prints de tela (TDD, JMeter e SonarCloud) estão reunidos em um único arquivo: [`prints-evidencias.pdf`](prints-evidencias.pdf).

Repositório público: https://github.com/adsouza04/organizacao-recursos

## 1. Release candidate e CI da tag

| Item | Link / local |
|---|---|
| Tag anotada `v1.0.0-rc1` (código exato da entrega) | https://github.com/adsouza04/organizacao-recursos/tree/v1.0.0-rc1 |
| **Execução do CI disparada pela tag** | https://github.com/adsouza04/organizacao-recursos/actions/workflows/ci.yml?query=branch%3Av1.0.0-rc1 |
| Página de releases e tags | https://github.com/adsouza04/organizacao-recursos/tags |
| Histórico completo do CI | https://github.com/adsouza04/organizacao-recursos/actions/workflows/ci.yml |

O workflow `.github/workflows/ci.yml` roda em todo Pull Request, em todo push na `main` e em todo push de tag `v*`. Cada execução publica dois artefatos para download: **`relatorio-jacoco`** (HTML do JaCoCo) e **`relatorios-de-teste`** (Surefire e Failsafe). Para baixá-los, abra a execução e veja a seção *Artifacts*, no final da página.

## 2. Build a partir de clone limpo

| Item | Local |
|---|---|
| Checklist auditável | [`docs/entrega/checklist.md`](../entrega/checklist.md) |
| Log completo do `mvnw -B verify` no clone limpo | [`docs/entrega/clone-limpo-verify.log`](../entrega/clone-limpo-verify.log) |

## 3. Cobertura (JaCoCo)

| Métrica | Medido | Meta | Fonte |
|---|---:|---:|---|
| Linhas | **98,2%** | ≥ 80% | `target/site/jacoco/jacoco.csv` do build local da `main` (`fc1e3be`) |
| Branches | **96,7%** | ≥ 70% | idem |
| Cobertura no SonarCloud | **98,1%** | — | análise da `main` (`fc1e3be`) |

O gate é verificado em todo build pelo `jacoco:check` ("All coverage checks have been met" no log do clone limpo). O relatório HTML de cada execução de CI está no artefato `relatorio-jacoco`.

## 4. Análise estática (SonarCloud)

| Item | Link / valor |
|---|---|
| Painel do projeto (`main`) | https://sonarcloud.io/summary/overall?id=adsouza04_organizacao-recursos |
| Quality Gate | **Passed** |
| Bugs / Vulnerabilidades / Hotspots | **0 / 0 / 0** (notas A em segurança, confiabilidade e manutenibilidade) |
| Duplicação | 0,0% |
| Print | [`prints-evidencias.pdf`](prints-evidencias.pdf), item 10 |

O Quality Gate também foi aprovado nas análises de PR, com comentário do bot `sonarqubecloud` nos PRs #1 e #3.

## 5. Funcionalidade em TDD (login JWT, PR #3)

| Etapa | Commit | CI | Print em [`prints-evidencias.pdf`](prints-evidencias.pdf) |
|---|---|---|---|
| Vermelho: teste antes do código | `b8b4c85` | CI #9 ❌ (`Tests run: 8, Failures: 8`) | itens 1 (terminal: 8 falhas), 2 (PR #3 com ❌) e 3 (log do CI) |
| Verde: implementação | `2c06e40` | CI #10 ✅ | itens 4 (terminal: 8 testes verdes) e 5 (Actions: CI #9 ❌ e CI #10 ✅) |
| Refatoração: `EmissorToken` | `91fa2f5` | ✅ | item 6 (testes verdes após o refactor) |

PR com a sequência de commits e checks: https://github.com/adsouza04/organizacao-recursos/pull/3

## 6. Concorrência e carga (JMeter)

| Item | Local |
|---|---|
| Plano | [`docs/performance/plano-carga-01.jmx`](../performance/plano-carga-01.jmx) |
| Resultado analisado | [`docs/performance/resultado-01.md`](../performance/resultado-01.md) |
| Dados brutos | [`docs/performance/resultado-01.csv`](../performance/resultado-01.csv) |
| Painel HTML do JMeter | [`docs/performance/relatorio-01/index.html`](../performance/relatorio-01/index.html) |
| Print da execução e da disputa (1 × 201, 49 × 409) | [`prints-evidencias.pdf`](prints-evidencias.pdf), item 9 |
| Teste automatizado de concorrência | `ReservaConcorrenciaIT#duplaReservaSimultaneaProduzExatamenteUmaReservaAceita` (12 threads, PostgreSQL real) |

## 7. Pull Requests (CI + revisão cruzada)

| PR | Conteúdo | Autor | Revisão |
|---|---|---|---|
| [#1](https://github.com/adsouza04/organizacao-recursos/pull/1) | Integração com o SonarCloud | Yasmin | Aprovado por Luan |
| [#2](https://github.com/adsouza04/organizacao-recursos/pull/2) | Atualização das actions e execução manual | Yasmin | Aprovado por Luan |
| [#3](https://github.com/adsouza04/organizacao-recursos/pull/3) | Login JWT em TDD | Luan | Aprovado por Yasmin |
| [#4](https://github.com/adsouza04/organizacao-recursos/pull/4) | Gestão de recursos e manutenção (RN-04) | Luan | Aprovado por Yasmin |
| [#5](https://github.com/adsouza04/organizacao-recursos/pull/5) | Notificação externa com WireMock | Yasmin | Revisado por Luan após o merge (risco R-11) |
| [#6](https://github.com/adsouza04/organizacao-recursos/pull/6) | Teste de carga JMeter | Yasmin | Aprovado por Luan |
| #7 | Documentação de entrega | Yasmin e Luan | Revisão cruzada |

## 8. Rastreabilidade e defeitos

| Item | Local |
|---|---|
| RTM (requisito → risco → teste → evidência) | [`docs/rtm.md`](../rtm.md) |
| Registro de defeitos | [`docs/defeitos/README.md`](../defeitos/README.md) (prints do DEF-06 nos itens 7 e 8 do PDF) |
| Plano de testes | [`docs/plano-testes.md`](../plano-testes.md) |
| Diagramas | [`docs/diagramas/arquitetura.md`](../diagramas/arquitetura.md) |
| Decisão de qualidade | [`docs/decisao-qualidade.md`](../decisao-qualidade.md) |
| Relatório executivo | [`docs/relatorio-qualidade.md`](../relatorio-qualidade.md) |

## 9. Histórico do CI até a release candidate

| Execução | Commit / PR | Resultado | Observação |
|---|---|---|---|
| CI #1 | `559c851` | ❌ | DEF-01 (Docker 29 × Testcontainers) |
| CI #2 | `c50e8f9` | ❌ | DEF-02 (cobertura 59% / 53%) |
| CI #3 | `325018d` | ✅ | Testes de API, gate de cobertura aprovado |
| CI #4 | `cd97011` | ✅ | Maven Wrapper |
| CI #5 | PR #1 | ✅ | SonarCloud, Quality Gate Passed |
| CI #7 | PR #2 | ✅ | Actions `@v5` |
| CI #9 | PR #3, `b8b4c85` | ❌ (esperado) | Vermelho do TDD |
| CI #10 | PR #3, `2c06e40` | ✅ | Verde do TDD |
| demais | PRs #3 a #6 e `main` | ✅ | — |
