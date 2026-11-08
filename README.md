# Sistema de Organização de Recursos

API REST (Java 21, Spring Boot 3.3) para alocar **salas, professores e materiais** sem conflitos de horário, com login JWT, RBAC por perfil, auditoria imutável, proteção contra dupla reserva sob concorrência e notificação externa.

[![CI](https://github.com/adsouza04/organizacao-recursos/actions/workflows/ci.yml/badge.svg)](https://github.com/adsouza04/organizacao-recursos/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=adsouza04_organizacao-recursos&metric=alert_status)](https://sonarcloud.io/summary/overall?id=adsouza04_organizacao-recursos)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=adsouza04_organizacao-recursos&metric=coverage)](https://sonarcloud.io/summary/overall?id=adsouza04_organizacao-recursos)

**Versão:** `v1.0.0-rc1` (release candidate). Veja o [CHANGELOG](CHANGELOG.md), a [decisão de qualidade](docs/decisao-qualidade.md) e o [índice de evidências](docs/evidencias/README.md).

## Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| JDK | 21 | Testado com Temurin 21.0.12 |
| Docker | em execução | Os testes de integração sobem um PostgreSQL com Testcontainers |
| Maven | não precisa | O repositório traz o Maven Wrapper (`mvnw`, Maven 3.9.11) |

**Docker 29 ou mais novo:** o arquivo `src/test/resources/docker-java.properties` (`api.version=1.44`) já está no repositório e resolve a incompatibilidade com o Testcontainers. Se ainda assim aparecer erro de versão da API, crie também `~/.docker-java.properties` com a mesma linha (veja DEF-01 em `docs/defeitos/`).

## Build e testes (do zero)

```bash
git clone https://github.com/adsouza04/organizacao-recursos.git
cd organizacao-recursos
./mvnw -B verify          # Windows (PowerShell): .\mvnw.cmd -B verify
```

O `verify` executa:

1. **34 testes unitários** (`*Test`), sem Docker;
2. **49 testes de integração** (`*IT`) com PostgreSQL real (Testcontainers) e WireMock;
3. o **gate de cobertura** do JaCoCo (mínimo de 80% de linhas e 70% de branches; a `v1.0.0-rc1` tem 98,2% e 96,7%).

Relatórios gerados: `target/site/jacoco/index.html` (cobertura), `target/surefire-reports` e `target/failsafe-reports` (testes).

Para rodar só os testes unitários (sem Docker): `./mvnw -B test`.

## Executando a aplicação

1. Suba um PostgreSQL:
   ```bash
   docker run -d --name recursos-db -p 5432:5432 \
     -e POSTGRES_USER=recursos -e POSTGRES_PASSWORD=recursos -e POSTGRES_DB=recursos \
     postgres:16-alpine
   ```
2. Gere e inicie a API:
   ```bash
   ./mvnw -B -DskipTests package
   java -jar target/organizacao-recursos-1.0.0-SNAPSHOT.jar
   ```
   O Flyway cria o esquema (migrações `V1` e `V2`) na primeira execução. A API responde em `http://localhost:8080`.

### Variáveis de ambiente

| Variável | Padrão | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/recursos` | Banco |
| `DB_USER` / `DB_PASSWORD` | `recursos` / `recursos` | Credenciais do banco |
| `JWT_SECRET` | valor de desenvolvimento | **Obrigatório trocar em produção** (mínimo de 32 bytes) |
| `NOTIFICACAO_URL` | `http://localhost:8089` | Serviço externo de notificação |

## Login e usuários de demonstração

A migração `V2` cria três usuários, todos com a senha **`Senha@123`** (armazenada como hash BCrypt). Eles existem só para demonstração e devem ser removidos em produção (risco R-05).

| Usuário | Perfil |
|---|---|
| `solicitante` | SOLICITANTE |
| `responsavel` | RESPONSAVEL |
| `admin` | ADMINISTRADOR |

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","senha":"Senha@123"}'
# {"token":"eyJ...","tipo":"Bearer","expiraEmSegundos":3600,"perfil":"ADMINISTRADOR"}
```

Use o token nas demais chamadas: `Authorization: Bearer <token>`. O JWT é HS256 e contém `sub` (usuário) e `perfil`.

## Perfis de acesso (RBAC)

| Perfil | Pode |
|---|---|
| `SOLICITANTE` | Criar reservas; cancelar e excluir as próprias; consultar reservas e recursos |
| `RESPONSAVEL` | Aprovar (único em recurso restrito), rejeitar, iniciar (retirada), concluir (devolução), marcar não comparecimento |
| `ADMINISTRADOR` | Cadastrar recursos, colocar e tirar recursos de manutenção, cancelar e excluir reservas de qualquer usuário |

## Fluxo de estados da reserva

`SOLICITADA → APROVADA → EM_USO → CONCLUIDA`, com saídas alternativas `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU`. Toda transição gera um registro em `registro_auditoria`, imutável também no banco (trigger). Aprovação e rejeição notificam o solicitante pelo serviço externo, em melhor esforço: falha ou lentidão do serviço não desfaz a decisão.

## Endpoints

| Método | Caminho | Perfis |
|---|---|---|
| POST | `/api/auth/login` | público |
| POST | `/api/recursos` | ADMINISTRADOR |
| GET | `/api/recursos` | qualquer perfil autenticado |
| POST | `/api/recursos/{id}/manutencao` | ADMINISTRADOR |
| DELETE | `/api/recursos/{id}/manutencao` | ADMINISTRADOR |
| POST | `/api/reservas` | SOLICITANTE, ADMINISTRADOR |
| GET | `/api/reservas/{id}` | qualquer perfil autenticado |
| POST | `/api/reservas/{id}/cancelar` | SOLICITANTE (própria), ADMINISTRADOR |
| DELETE | `/api/reservas/{id}` | SOLICITANTE (própria), ADMINISTRADOR; nunca se `EM_USO` ou `CONCLUIDA` |
| POST | `/api/reservas/{id}/aprovar` | RESPONSAVEL, ADMINISTRADOR (recurso restrito: só RESPONSAVEL) |
| POST | `/api/reservas/{id}/rejeitar` | RESPONSAVEL, ADMINISTRADOR |
| POST | `/api/reservas/{id}/iniciar` | RESPONSAVEL, ADMINISTRADOR |
| POST | `/api/reservas/{id}/concluir` | RESPONSAVEL, ADMINISTRADOR |
| POST | `/api/reservas/{id}/nao-compareceu` | RESPONSAVEL, ADMINISTRADOR |

### Códigos de erro (corpo `application/problem+json`)

| Código | Quando |
|---|---|
| 400 | Corpo inválido (`@Valid`), JSON malformado, identificador malformado |
| 401 | Sem token, token inválido ou credenciais de login incorretas |
| 403 | Perfil sem permissão |
| 404 | Recurso, reserva ou rota inexistente |
| 405 | Método HTTP não suportado |
| 409 | Sobreposição de horário (RN-02/RN-03) |
| 422 | Regra de negócio: período inválido, recurso em manutenção, transição fora de ordem, exclusão de reserva iniciada |
| 500 | Erro inesperado, sem detalhes internos na resposta |

## Teste de carga

Plano JMeter e resultado em [`docs/performance/resultado-01.md`](docs/performance/resultado-01.md): 50 solicitações simultâneas ao mesmo horário resultaram em **1 reserva aceita e 49 recusadas (409)**, e a leitura sustentada teve P95 de 30 ms com 0% de erros.

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/rtm.md`](docs/rtm.md) | Matriz requisito → risco → teste → evidência |
| [`docs/plano-testes.md`](docs/plano-testes.md) | Estratégia, inventário da suíte e TDD |
| [`docs/diagramas/arquitetura.md`](docs/diagramas/arquitetura.md) | C4 (contexto, contêineres, componentes) e sequências (login, concorrência, auditoria, manutenção) |
| [`docs/defeitos/`](docs/defeitos/README.md) | Defeitos encontrados, priorizados e corrigidos |
| [`docs/performance/resultado-01.md`](docs/performance/resultado-01.md) | Plano e resultado do teste de carga |
| [`docs/entrega/checklist.md`](docs/entrega/checklist.md) | Checklist executado em clone limpo |
| [`docs/evidencias/README.md`](docs/evidencias/README.md) | Índice de evidências (CI, JaCoCo, SonarCloud, JMeter, TDD) |
| [`docs/decisao-qualidade.md`](docs/decisao-qualidade.md) | Decisão de liberação, riscos aceitos e autoria |
| [`docs/relatorio-qualidade.md`](docs/relatorio-qualidade.md) | Relatório executivo |

## Equipe

- **Yasmin de Souza Santos** ([@adsouza04](https://github.com/adsouza04))
- **Luan Bezerra Guimarães** ([@Luan46guimaraes](https://github.com/Luan46guimaraes))

A contribuição de cada integrante está declarada em [`docs/decisao-qualidade.md`](docs/decisao-qualidade.md#6-declaração-de-autoria).
