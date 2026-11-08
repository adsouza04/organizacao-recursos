# Teste de carga 01 — plano e resultado (JMeter)

Execução realizada em **08/11/2026, 18:12–18:13 (BRT)**, por Yasmin de Souza Santos, sobre o commit da `main` que contém os PRs #1 a #5 (login JWT, gestão de recursos e notificação externa).

## 1. Objetivo

1. Demonstrar, **sob carga concorrente real** (HTTP, aplicação empacotada, PostgreSQL real), que solicitações simultâneas ao mesmo recurso e horário produzem **exatamente uma reserva aceita** (RN-03), complementando o teste automatizado `ReservaConcorrenciaIT`.
2. Medir tempo de resposta e vazão da API em leitura sustentada com usuários autenticados (JWT emitido pelo próprio login).

## 2. Artefatos

| Artefato | Caminho |
|---|---|
| Plano JMeter | `docs/performance/plano-carga-01.jmx` |
| Resultados brutos (uma linha por requisição) | `docs/performance/resultado-01.csv` |
| Painel HTML gerado pelo JMeter | `docs/performance/relatorio-01/index.html` |

## 3. Ambiente

| Item | Valor |
|---|---|
| Máquina | Notebook Windows, 8 GB de RAM (Docker Desktop/WSL2 limitado a 3 GB) |
| Aplicação | `organizacao-recursos-1.0.0-SNAPSHOT.jar` (Java 21, Spring Boot 3.3.5), porta 8080 |
| Banco | PostgreSQL 16 (`postgres:16-alpine` no Docker), migrações Flyway V1 e V2 aplicadas no início |
| Gerador de carga | Apache JMeter 5.6.3 em modo não gráfico (`-n`) |
| Comando | `jmeter -n -t docs/performance/plano-carga-01.jmx -l docs/performance/resultado-01.csv -e -o docs/performance/relatorio-01` |

**Limitação conhecida:** gerador de carga, aplicação e banco rodaram na mesma máquina, disputando CPU e memória. Os tempos absolutos são, portanto, conservadores (piores que em um servidor dedicado). Para a regra de concorrência, isso não é limitação: quanto mais disputa, mais forte a evidência.

## 4. Cenários do plano

Os grupos de usuários rodam em sequência (`serialize_threadgroups=true`).

| Grupo | Usuários | Comportamento |
|---|---|---|
| Preparação (setUp) | 1 | Login como `admin` e criação de um recurso novo (`POST /api/recursos`), cujo id é compartilhado com os demais grupos |
| Cenário 1 — disputa | 50 | Cada usuário faz login como `solicitante`; um **Synchronizing Timer** segura as 50 requisições e as libera **no mesmo instante** para `POST /api/reservas` com o **mesmo recurso e o mesmo horário** (15/01/2030, 10h–11h UTC) |
| Cenário 2 — leitura sustentada | 30 (rampa de 10 s) | Login único por usuário como `responsavel`, seguido de `GET /api/recursos` em laço por 60 s, com pausa de 200 ms entre requisições |

## 5. Critérios de aceitação (definidos antes da execução)

| # | Critério | Meta |
|---|---|---|
| C1 | Reservas aceitas na disputa | **exatamente 1** resposta 201 |
| C2 | Reservas recusadas na disputa | as demais (49) com **409 Conflict**, nenhuma 5xx |
| C3 | Erros inesperados no teste inteiro | **0%** |
| C4 | Leitura sustentada — percentil 95 | **< 500 ms** |

## 6. Resultados

### 6.1 Resumo geral (saída do JMeter)

| Métrica | Valor |
|---|---|
| Requisições | 7.462 em 1 min 11 s |
| Vazão média | 105,8 req/s |
| Tempo médio | 21 ms |
| Erros | **0 (0,00%)** |

### 6.2 Por operação

| Operação | N | Média (ms) | P95 (ms) | Máx (ms) |
|---|---:|---:|---:|---:|
| POST /api/auth/login (admin) — preparação | 1 | 1447 | 1447 | 1447 |
| POST /api/recursos (admin) — preparação | 1 | 1675 | 1675 | 1675 |
| POST /api/auth/login (solicitante) | 50 | 110 | 169 | 220 |
| **POST /api/reservas (mesmo recurso e horário)** | **50** | **745** | **986** | **990** |
| POST /api/auth/login (responsavel) | 30 | 235 | 361 | 419 |
| GET /api/recursos | 7330 | 15 | 30 | 535 |

### 6.3 Resultado da disputa (contagem por código HTTP)

| Código | Quantidade | Significado |
|---|---:|---|
| **201 Created** | **1** | única reserva aceita |
| **409 Conflict** | **49** | recusadas por sobreposição de horário |

Contagem obtida diretamente do CSV:
`Import-Csv docs\performance\resultado-01.csv | Group-Object label, responseCode`

## 7. Avaliação dos critérios

| # | Resultado | Situação |
|---|---|---|
| C1 | 1 resposta 201 | ✅ atendido |
| C2 | 49 respostas 409, nenhuma 5xx | ✅ atendido |
| C3 | 0,00% de erros | ✅ atendido |
| C4 | P95 de 30 ms em `GET /api/recursos` | ✅ atendido |

## 8. Análise

- **Concorrência (RN-03):** as 50 requisições liberadas no mesmo instante foram serializadas pelo lock pessimista sobre o recurso (`SELECT ... FOR UPDATE`). A primeira gravou a reserva e as 49 seguintes encontraram a sobreposição dentro do lock e receberam 409. A constraint `EXCLUDE USING gist` do banco permanece como última barreira, mas nem precisou ser acionada para produzir o resultado correto.
- **Custo da serialização:** o tempo médio de 745 ms (P95 986 ms) da disputa é esperado e aceitável: as requisições ao mesmo recurso esperam em fila pelo lock. Requisições a recursos diferentes não competem por esse lock.
- **Primeiras requisições lentas (1,4 s e 1,7 s):** ocorreram na preparação, com a JVM ainda "fria" (carregamento de classes e compilação JIT na primeira chamada de cada caminho). Não se repetiram nas requisições seguintes.
- **Login:** a média entre 110 e 235 ms reflete o custo intencional do BCrypt (fator 10), que dificulta ataques de força bruta.
- **Leitura:** 7.330 consultas com média de 15 ms e P95 de 30 ms, sem erros, com 30 usuários simultâneos.

## 9. Conclusão

O sistema **atende** a todos os critérios definidos: sob 50 solicitações simultâneas ao mesmo horário, **exatamente uma** reserva foi aceita, e a API manteve tempos de leitura baixos e 0% de erros inesperados. O risco residual (tempos medidos com gerador e aplicação na mesma máquina) está registrado em `docs/decisao-qualidade.md`.

## 10. Como reproduzir

```powershell
docker run -d --name recursos-db -e POSTGRES_DB=recursos -e POSTGRES_USER=recursos -e POSTGRES_PASSWORD=recursos -p 5432:5432 postgres:16-alpine
./mvnw -B -DskipTests package
java -jar target/organizacao-recursos-1.0.0-SNAPSHOT.jar        # em outro terminal
jmeter -n -t docs/performance/plano-carga-01.jmx -l docs/performance/resultado-XX.csv -e -o docs/performance/relatorio-XX
```

O host e a porta podem ser trocados com `-Jhost=... -Jport=...`. A pasta passada em `-o` precisa estar vazia ou não existir.
