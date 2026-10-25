# Arquitetura (C4 em Mermaid)

## Nível 1: Contexto

```mermaid
C4Context
  title Sistema de Organização de Recursos - Contexto
  Person(sol, "Solicitante", "Professor/Coordenador: consulta, cria, altera e cancela reservas")
  Person(resp, "Responsável", "Aprova recursos restritos, acompanha retiradas/devoluções")
  Person(adm, "Administrador", "Gerencia recursos, usuários, bloqueios e manutenção")
  System(sis, "Organização de Recursos", "API REST Spring Boot")
  SystemDb(bd, "PostgreSQL", "Reservas, recursos, auditoria")
  Rel(sol, sis, "HTTPS + JWT")
  Rel(resp, sis, "HTTPS + JWT")
  Rel(adm, sis, "HTTPS + JWT")
  Rel(sis, bd, "JDBC")
```

## Nível 2: Componentes

```mermaid
flowchart LR
  C[ReservaControlador] --> S[ReservaServico]
  S --> RR[RecursoRepositorio<br/>lock pessimista]
  S --> VR[ReservaRepositorio<br/>existeSobreposicao]
  S --> AR[RegistroAuditoriaRepositorio]
  SEG[ConfiguracaoSeguranca<br/>JWT + RBAC] -.protege.-> C
  TE[TratadorGlobalExcecoes] -.traduz erros.-> C
  RR --> BD[(PostgreSQL)]
  VR --> BD
  AR --> BD
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
  S->>BD: A: SELECT recurso X FOR UPDATE (obtém lock)
  S->>BD: B: SELECT recurso X FOR UPDATE (aguarda)
  S->>BD: A: existeSobreposicao? não -> INSERT reserva + registro_auditoria
  BD-->>S: A: COMMIT (libera lock)
  S->>BD: B: lock obtido; existeSobreposicao? sim
  S-->>B: ConflitoException -> HTTP 409
  Note over BD: Rede de segurança: EXCLUDE USING gist rejeita<br/>qualquer INSERT sobreposto mesmo fora do serviço
```

## Sequência: transição de estado com auditoria (RN-07)

```mermaid
sequenceDiagram
  participant R as Responsável
  participant C as ReservaControlador
  participant S as ReservaServico
  participant E as Reserva
  participant BD as PostgreSQL
  R->>C: POST /api/reservas/{id}/aprovar (JWT)
  C->>S: aprovar(id, ator)
  S->>E: transicionarPara(APROVADA) (valida máquina de estados)
  S->>BD: INSERT registro_auditoria(anterior, novo, ator, instante)
  Note over BD: trigger bloqueia UPDATE/DELETE em registro_auditoria
  S-->>C: reserva atualizada
  C-->>R: 200 OK
```

## Decisões de design
- **Recurso unificado**: sala, professor e material são `Recurso` com `TipoRecurso`; a mesma regra de sobreposição vale para os três.
- **Defesa em profundidade**: cada regra crítica existe na aplicação e no banco (CHECK, EXCLUDE gist, triggers).
- **Auditoria sem FK**: o histórico sobrevive à remoção de reservas ainda não iniciadas.
