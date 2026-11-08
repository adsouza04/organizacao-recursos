CREATE TABLE usuario (
    id         UUID PRIMARY KEY,
    login      VARCHAR(60)  NOT NULL UNIQUE,
    senha_hash VARCHAR(100) NOT NULL,
    perfil     VARCHAR(20)  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('SOLICITANTE', 'RESPONSAVEL', 'ADMINISTRADOR'))
);

-- Usuários de demonstração (senha Senha@123, armazenada como hash BCrypt).
-- Em produção devem ser removidos ou ter a senha trocada.
INSERT INTO usuario (id, login, senha_hash, perfil) VALUES
    (gen_random_uuid(), 'solicitante', '$2a$10$2tpRKlkfddec.uYFkR5oqezWF5FeQvpeXJ5laCpnPBU25SMGCWtlm', 'SOLICITANTE'),
    (gen_random_uuid(), 'responsavel', '$2a$10$2tpRKlkfddec.uYFkR5oqezWF5FeQvpeXJ5laCpnPBU25SMGCWtlm', 'RESPONSAVEL'),
    (gen_random_uuid(), 'admin',       '$2a$10$2tpRKlkfddec.uYFkR5oqezWF5FeQvpeXJ5laCpnPBU25SMGCWtlm', 'ADMINISTRADOR');