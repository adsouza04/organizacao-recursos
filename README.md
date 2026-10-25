# Sistema de Organização de Recursos

API REST (Java 21, Spring Boot 3.3) para alocar **salas, professores e materiais** sem conflitos de horário, com RBAC por JWT, auditoria imutável e proteção contra dupla reserva sob concorrência.

> **Status:** em desenvolvimento. Veja `docs/rtm.md` para o que já está implementado e testado e o que ainda está planejado.

## Requisitos

- JDK 21
- Maven 3.9 ou superior
- Docker em execução (os testes de integração usam Testcontainers com PostgreSQL)

## Build e testes (do zero)

```bash
git clone <URL-DO-REPOSITORIO>
cd organizacao-recursos
mvn -B verify
```

`verify` executa os testes unitários (`*Test`), os de integração (`*IT`) e o gate de cobertura do JaCoCo (mínimo 80% de linhas e 70% de branches). O relatório fica em `target/site/jacoco/index.html`.

Somente os testes unitários (não precisam de Docker):

```bash
mvn -B test
```

## Executando a aplicação

1. Suba um PostgreSQL:
   ```bash
   docker run -d --name recursos-db -p 5432:5432 \
     -e POSTGRES_USER=recursos -e POSTGRES_PASSWORD=recursos -e POSTGRES_DB=recursos \
     postgres:16-alpine
   ```
2. Inicie a API:
   ```bash
   mvn spring-boot:run
   ```
   O Flyway cria o esquema na primeira execução. A API responde em `http://localhost:8080`.

Variáveis de ambiente opcionais: `DB_URL`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET` (**obrigatório trocar em produção**, mínimo de 32 bytes).

## Perfis de acesso (RBAC)

| Perfil | Papel |
|---|---|
| `SOLICITANTE` | Cria reservas e cancela/exclui as próprias |
| `RESPONSAVEL` | Aprova, rejeita, inicia, conclui e marca não comparecimento; único que aprova recurso restrito |
| `ADMINISTRADOR` | Gestão global (CRUD de recursos, usuários e manutenção ainda a implementar) |

O token JWT (HS256) deve conter `sub` (usuário) e `perfil` (um dos valores acima).

## Fluxo de estados da reserva

`SOLICITADA → APROVADA → EM_USO → CONCLUIDA`, com saídas alternativas `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU`. Toda transição gera um registro em `registro_auditoria`, imutável também no banco (trigger).

## Endpoints

| Método | Caminho | Perfis |
|---|---|---|
| POST | `/api/reservas` | SOLICITANTE, ADMINISTRADOR |
| GET | `/api/reservas/{id}` | todos |
| POST | `/api/reservas/{id}/cancelar` | SOLICITANTE (própria), ADMINISTRADOR |
| DELETE | `/api/reservas/{id}` | SOLICITANTE (própria), ADMINISTRADOR; nunca se `EM_USO` ou `CONCLUIDA` |
| POST | `/api/reservas/{id}/aprovar` | RESPONSAVEL, ADMINISTRADOR (recurso restrito: só RESPONSAVEL) |
| POST | `/api/reservas/{id}/rejeitar` | RESPONSAVEL, ADMINISTRADOR |
| POST | `/api/reservas/{id}/iniciar` | RESPONSAVEL, ADMINISTRADOR |
| POST | `/api/reservas/{id}/concluir` | RESPONSAVEL, ADMINISTRADOR |
| POST | `/api/reservas/{id}/nao-compareceu` | RESPONSAVEL, ADMINISTRADOR |

Exemplo de criação:

```json
POST /api/reservas
{ "recursoId": "<uuid>", "inicio": "2026-12-10T10:00:00Z", "fim": "2026-12-10T11:00:00Z" }
```

Respostas de erro (`ProblemDetail`): `400` validação, `403` sem permissão, `404` não encontrado, `409` conflito/dupla reserva, `422` regra de negócio.

## Concorrência

Três camadas: (1) lock pessimista (`SELECT ... FOR UPDATE`) na linha do recurso; (2) verificação de sobreposição dentro do lock; (3) constraint `EXCLUDE USING gist` no PostgreSQL. Detalhes e diagramas em `docs/diagramas/arquitetura.md`.

## Estrutura

```
src/main/java/br/edu/recursos/
  dominio/ repositorio/ servico/ seguranca/ web/ excecao/
src/main/resources/db/migration/   # Flyway
docs/                              # RTM, diagramas, evidências
.github/workflows/ci.yml           # pipeline de CI
```
