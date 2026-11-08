# Arquitetura (C4 em Mermaid)

Diagramas da versão `v1.0.0-rc1`. O GitHub renderiza os blocos Mermaid diretamente nesta página.

## Nível 1: Contexto

```mermaid
C4Context
  title Sistema de Organização de Recursos - Contexto
  Person(sol, "Solicitante", "Professor/Coordenador: cria e cancela as próprias reservas")
  Person(resp, "Responsável", "Aprova/rejeita (único em recurso restrito), registra retirada e devolução")
  Person(adm, "Administrador", "Cadastra recursos e controla manutenção")
  System(sis, "Organização de Recursos", "API REST Spring Boot 3.3 / Java 21")
  SystemDb(bd, "PostgreSQL 16", "Usuários, recursos, reservas e auditoria imutável")
  System_Ext(notif, "Serviço de notificação", "Serviço externo da instituição (POST /notificacoes)")
  Rel(sol, sis, "HTTPS + JWT")
  Rel(resp, sis, "HTTPS + JWT")
  Rel(adm, sis, "HTTPS + JWT")
  Rel(sis, bd, "JDBC (Flyway, JPA)")
  Rel(sis, notif, "HTTP/JSON, melhor esforço, timeout 2 s")
```

## Nível 2: Contêineres

```mermaid
flowchart LR
  U[Clientes HTTP<br/>Solicitante, Responsável, Administrador] -->|JSON + Bearer JWT| API[API REST<br/>Spring Boot, porta 8080]
  API -->|JDBC| BD[(PostgreSQL 16<br/>CHECK, EXCLUDE gist, triggers)]
  API -->|POST /notificacoes| N[Serviço de notificação<br/>externo]
  CI[GitHub Actions] -->|./mvnw verify + Testcontainers| API
  CI -->|análise + Quality Gate| SC[SonarCloud]
```

## Nível 3: Componentes da API

```mermaid
flowchart TB
  subgraph web
    AC[AutenticacaoControlador<br/>POST /api/auth/login]
    RC[ReservaControlador<br/>/api/reservas]
    RCO[RecursoControlador<br/>/api/recursos]
    TE[TratadorGlobalExcecoes<br/>400/401/403/404/405/409/422/500]
  end
  subgraph seguranca
    SEG[ConfiguracaoSeguranca<br/>JWT HS256 + RBAC por rota]
    ET[EmissorToken<br/>claim perfil]
  end
  subgraph servico
    AS[AutenticacaoServico<br/>BCrypt]
    RS[ReservaServico<br/>lock + sobreposição + auditoria]
    RES[RecursoServico<br/>manutenção com lock]
  end
  subgraph integracao
    NC[NotificacaoCliente<br/>RestClient com timeout]
  end
  subgraph repositorio
    UR[UsuarioRepositorio]
    RR[RecursoRepositorio<br/>buscarPorIdComBloqueio]
    VR[ReservaRepositorio<br/>existeSobreposicao]
    AR[RegistroAuditoriaRepositorio]
  end
  SEG -.protege.-> RC
  SEG -.protege.-> RCO
  TE -.traduz erros.-> RC
  AC --> AS
  AS --> UR
  AS --> ET
  RC --> RS
  RC --> NC
  RCO --> RES
  RES --> RR
  RS --> RR
  RS --> VR
  RS --> AR
  UR --> BD[(PostgreSQL)]
  RR --> BD
  VR --> BD
  AR --> BD
  NC --> EXT[Serviço de notificação]
```

## Sequência: login e emissão do JWT

```mermaid
sequenceDiagram
  participant C as Cliente
  participant AC as AutenticacaoControlador
  participant AS as AutenticacaoServico
  participant BD as PostgreSQL
  participant ET as EmissorToken
  C->>AC: POST /api/auth/login {usuario, senha}
  AC->>AS: autenticar(usuario, senha)
  AS->>BD: SELECT usuario WHERE login = ? AND ativo
  alt usuário inexistente, inativo ou senha incorreta (BCrypt)
    AS-->>AC: CredenciaisInvalidasException
    AC-->>C: 401 "Usuário ou senha inválidos" (mensagem única)
  else credenciais válidas
    AS->>ET: emitir(login, perfil)
    ET-->>AS: JWT HS256 (sub, perfil, exp = 1 h)
    AC-->>C: 200 {token, tipo: Bearer, expiraEmSegundos, perfil}
  end
```

## Sequência: criação concorrente (RN-03)

```mermaid
sequenceDiagram
  participant A as Requisição A
  participant B as Requisição B
  participant S as ReservaServico
  participant BD as PostgreSQL
  A->>S: criar(recurso X, 10h-11h)
  B->>S: criar(recurso X, 10h-11h)
  S->>BD: A: SELECT recurso X FOR UPDATE (obtém o lock)
  S->>BD: B: SELECT recurso X FOR UPDATE (aguarda)
  S->>BD: A: em manutenção? não. Sobreposição? não. INSERT reserva + registro_auditoria
  BD-->>S: A: COMMIT (libera o lock)
  S-->>A: 201 Created
  S->>BD: B: lock obtido; sobreposição? sim
  S-->>B: ConflitoException -> 409 Conflict
  Note over BD: Rede de segurança: EXCLUDE USING gist rejeita<br/>qualquer INSERT sobreposto, mesmo fora do serviço
```

Evidências: `ReservaConcorrenciaIT` (12 threads) e teste de carga JMeter com 50 requisições simultâneas, com resultado 1 × 201 e 49 × 409 (`docs/performance/resultado-01.md`).

## Sequência: transição de estado com auditoria e notificação (RN-07)

```mermaid
sequenceDiagram
  participant R as Responsável
  participant C as ReservaControlador
  participant S as ReservaServico
  participant E as Reserva
  participant BD as PostgreSQL
  participant N as Serviço de notificação
  R->>C: POST /api/reservas/{id}/aprovar (JWT)
  C->>S: aprovar(id, ator)
  S->>E: transicionarPara(APROVADA) (valida a máquina de estados)
  S->>BD: UPDATE reserva + INSERT registro_auditoria(anterior, novo, ator, instante)
  Note over BD: trigger bloqueia UPDATE/DELETE em registro_auditoria
  S-->>C: reserva aprovada (transação já confirmada)
  C->>N: POST /notificacoes {destinatario, reservaId, recursoId, status}
  alt serviço responde 2xx
    N-->>C: 202 Accepted
  else falha 5xx ou timeout de 2 s
    N--xC: erro registrado em log (melhor esforço)
  end
  C-->>R: 200 OK {status: APROVADA}
```

## Sequência: manutenção de recurso (RN-04)

```mermaid
sequenceDiagram
  participant A as Administrador
  participant RC as RecursoControlador
  participant RS as RecursoServico
  participant BD as PostgreSQL
  participant S as Solicitante
  A->>RC: POST /api/recursos/{id}/manutencao
  RC->>RS: entrarEmManutencao(id)
  RS->>BD: SELECT recurso FOR UPDATE (mesmo lock da criação de reservas)
  RS->>BD: UPDATE recurso SET status = MANUTENCAO
  RC-->>A: 200 {status: MANUTENCAO}
  S->>RC: POST /api/reservas (recurso em manutenção)
  Note over RC,BD: ReservaServico verifica o status dentro do lock
  RC-->>S: 422 Unprocessable Entity
```

## Decisões de design

- **Recurso unificado:** sala, professor e material são `Recurso` com `TipoRecurso`. A mesma regra de sobreposição vale para os três.
- **Defesa em profundidade:** cada regra crítica existe na aplicação e no banco (CHECK de período, EXCLUDE gist, triggers de exclusão e de auditoria).
- **Mesmo lock para reserva e manutenção:** reserva e entrada em manutenção do mesmo recurso nunca se cruzam.
- **Auditoria sem FK:** o histórico sobrevive à remoção de reservas ainda não iniciadas.
- **Notificação após o commit e em melhor esforço:** falha ou lentidão do serviço externo não desfaz decisões gravadas.
- **Emissão do token isolada (`EmissorToken`):** a regra de autenticação não conhece detalhes de JWT, resultado do refactor do TDD.
