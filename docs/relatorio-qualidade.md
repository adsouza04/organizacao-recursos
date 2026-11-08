# Relatório executivo de qualidade: v1.0.0-rc1

**Sistema de Organização de Recursos** · 08/11/2026 · Yasmin de Souza Santos e Luan Bezerra Guimarães

## Resumo

A release candidate `v1.0.0-rc1` está **apta para liberação com ressalvas**. Todas as metas mensuráveis de qualidade foram atingidas com margem, e as quatro condições que zeram a avaliação (build limpo, CI, autorização e dupla reserva) estão comprovadas por testes automatizados e por teste de carga. As ressalvas são funcionalidades secundárias ainda não implementadas, todas com responsável e plano.

## Painel de metas

| Meta | Alvo | Resultado | |
|---|---:|---:|:-:|
| Cobertura de linhas (JaCoCo) | ≥ 80% | **98,2%** | ✅ |
| Cobertura de branches (JaCoCo) | ≥ 70% | **96,7%** | ✅ |
| Requisitos críticos na RTM | 100% | **100% (7/7)** | ✅ |
| Bugs e vulnerabilidades (SonarCloud) | 0 | **0 / 0** | ✅ |
| Quality Gate (SonarCloud) | aprovado | **Passed** | ✅ |
| Testes automatizados | — | **83** (34 unitários, 49 de integração), 0 falhas | ✅ |
| Dupla reserva sob carga | 1 aceita | **1 × 201, 49 × 409** | ✅ |
| Erros no teste de carga | 0% | **0,00%** (7.462 requisições) | ✅ |
| Build a partir de clone limpo | verde | **BUILD SUCCESS em 59 s** | ✅ |

## O que foi entregue

- **Reservas sem conflito, inclusive sob concorrência:** três barreiras independentes (lock pessimista, checagem dentro do lock e constraint `EXCLUDE gist` no PostgreSQL). Provado por um teste com 12 threads e por 50 requisições HTTP simultâneas no JMeter.
- **Segurança:** login com senha BCrypt emitindo JWT (desenvolvido em TDD) e RBAC por rota para três perfis, com testes de 401 e 403.
- **Rastreabilidade:** toda mudança de estado gera auditoria imutável, protegida também por trigger no banco.
- **Gestão de recursos:** cadastro e manutenção pelo Administrador; recurso em manutenção recusa reservas (RN-04 testada ponta a ponta).
- **Integração externa resiliente:** notificação ao solicitante com timeout; falha externa não desfaz a decisão (testado com WireMock).

## Como a qualidade foi construída

| Prática | Como aparece no projeto |
|---|---|
| Testes com ambiente real | PostgreSQL de verdade via Testcontainers; nenhuma regra de banco simulada |
| TDD | Login JWT: commit de teste falhando (CI vermelho), implementação (CI verde) e refatoração (PR #3) |
| Integração contínua | GitHub Actions em todo PR, na `main` e na tag; gate de cobertura e Quality Gate bloqueiam regressões |
| Revisão cruzada | Todo PR revisado pelo outro integrante (o PR #5 foi revisado após o merge, risco R-11) |
| Gestão de defeitos | 8 defeitos reais registrados com causa raiz; nenhum Crítico ou Alto em aberto |
| Honestidade das evidências | A RTM só marca VERDE o que tem CI; lacunas aparecem como "NÃO IMPLEMENTADO" |

## Defeitos relevantes encontrados e corrigidos

1. **Incompatibilidade Testcontainers × Docker 29** (Crítica): CI vermelho até fixar a versão da API do Docker.
2. **Cobertura em 59%** (Alta): o gate reprovou o build; a criação de 27 testes de API elevou a cobertura e revelou o defeito 3.
3. **Erros do cliente respondendo 500** (Alta): UUID inválido e rota inexistente passaram a responder 400 e 404.
4. **Teste WireMock instável** (Média): investigado até a causa raiz (timeout menor que o aquecimento do dublê), descartando uma primeira hipótese errada.

## Ressalvas (resumo da matriz de riscos)

| Ressalva | Impacto | Responsável |
|---|---|---|
| Consulta de disponibilidade e alteração de reserva | Médio (conveniência; conflitos seguem impossíveis) | Yasmin |
| Bloqueios de agenda e CRUD de usuários | Médio | Luan |
| Listagem de retiradas/devoluções | Baixo (transições e auditoria existem) | Luan |
| Spring Boot 3.3.5 fora do suporte OSS | Médio | Yasmin |
| Usuários de demonstração e segredo JWT padrão | Alto só se forem para produção sem ajuste | Luan |

Detalhes, justificativas e plano em `docs/decisao-qualidade.md`.

## Recomendação

**Liberar a `v1.0.0-rc1` com as ressalvas acima.** Para a `v1.0.0`, implementar as funcionalidades das ressalvas R-01 a R-03 mantendo os mesmos gates, atualizar o Spring Boot e endurecer a configuração de produção (usuários de demonstração e segredo JWT).
