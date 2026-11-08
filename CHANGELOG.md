# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). O projeto segue [versionamento semântico](https://semver.org/lang/pt-BR/).

## [v1.0.0-rc1] - 2026-11-08

Primeira release candidate. Decisão de qualidade: **liberar com ressalvas** (`docs/decisao-qualidade.md`).

### Adicionado

- **Login com emissão de JWT** (`POST /api/auth/login`), com senha BCrypt, claim `perfil` e três usuários de demonstração (migração `V2`). Desenvolvido em **TDD** (commits `test` → `feat` → `refactor`). *Luan Bezerra Guimarães, PR #3.*
- **Gestão de recursos pelo Administrador:** cadastro, listagem e entrada/saída de manutenção com o mesmo lock pessimista da criação de reservas, tornando a RN-04 testável pela API. *Luan Bezerra Guimarães, PR #4.*
- **Notificação externa** ao solicitante na aprovação e na rejeição, em melhor esforço e com timeout de 2 s, testada com **WireMock** (sucesso, falha 503 e lentidão). *Yasmin de Souza Santos, PR #5.*
- **Teste de carga JMeter:** 50 solicitações simultâneas ao mesmo horário (1 aceita, 49 recusadas com 409) e leitura sustentada (P95 de 30 ms, 0% de erros). *Yasmin de Souza Santos, PR #6.*
- **Integração com o SonarCloud** no CI, com espera pelo Quality Gate (`sonar.qualitygate.wait=true`). *Yasmin de Souza Santos, PR #1.*
- Execução manual do CI (`workflow_dispatch`) e CI disparado em tags `v*`. *Yasmin de Souza Santos, PR #2.*
- Núcleo do sistema: entidades, máquina de estados, reservas com concorrência em três camadas (lock, checagem dentro do lock, EXCLUDE gist), auditoria imutável, RBAC, tratamento global de erros, Testcontainers e JaCoCo. *Yasmin de Souza Santos, commits iniciais.*
- Documentação de entrega: RTM, plano de testes, diagramas C4 e de sequência, registro de defeitos, checklist de clone limpo, índice de evidências, decisão de qualidade e relatório executivo. *Yasmin de Souza Santos e Luan Bezerra Guimarães.*

### Alterado

- `TokenEmitido` e a montagem do JWT foram extraídos do `AutenticacaoServico` para `EmissorToken` (refatoração do TDD). *Luan Bezerra Guimarães.*
- `actions/checkout` e `actions/setup-java` atualizados para `@v5` (Node 24). *Yasmin de Souza Santos.*

### Corrigido

- DEF-01: incompatibilidade do Testcontainers com o Docker 29.
- DEF-02: cobertura abaixo do gate (59%/53% → 98,2%/96,7%).
- DEF-03: UUID inválido, rota inexistente e método não permitido respondiam 500.
- DEF-04: build não reproduzível sem Maven Wrapper.
- DEF-06: teste de integração com WireMock instável (timeout menor que o aquecimento do dublê).

Detalhes em `docs/defeitos/README.md`.

### Segurança

- Hotspot "CSRF desabilitado" analisado e justificado como falso positivo (API stateless com JWT no cabeçalho, sem cookies), registrado em DEF-05 e R-08.

### Ressalvas conhecidas

Consulta de disponibilidade, alteração de reserva, bloqueios de agenda, gestão de usuários e listagem de retiradas/devoluções **não** fazem parte desta versão. Riscos R-01 a R-03 em `docs/decisao-qualidade.md`.
