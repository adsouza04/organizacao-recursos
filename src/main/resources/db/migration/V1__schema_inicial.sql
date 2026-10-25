CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE recurso (
    id       UUID PRIMARY KEY,
    nome     VARCHAR(120) NOT NULL,
    tipo     VARCHAR(20)  NOT NULL,
    restrito BOOLEAN      NOT NULL DEFAULT FALSE,
    status   VARCHAR(20)  NOT NULL,
    versao   BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE reserva (
    id          UUID PRIMARY KEY,
    recurso_id  UUID         NOT NULL REFERENCES recurso (id),
    solicitante VARCHAR(120) NOT NULL,
    inicio_em   TIMESTAMPTZ  NOT NULL,
    fim_em      TIMESTAMPTZ  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    versao      BIGINT       NOT NULL DEFAULT 0,
    criado_em   TIMESTAMPTZ  NOT NULL,
    -- RN-01: término estritamente posterior ao início
    CONSTRAINT ck_reserva_periodo CHECK (fim_em > inicio_em),
    -- RN-02/RN-03: última linha de defesa contra sobreposição e dupla reserva
    CONSTRAINT ex_reserva_sem_sobreposicao EXCLUDE USING gist (
        recurso_id WITH =,
        tstzrange(inicio_em, fim_em, '[)') WITH &&
    ) WHERE (status IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA'))
);

CREATE INDEX ix_reserva_recurso ON reserva (recurso_id, inicio_em);

-- RN-06: reservas iniciadas não podem ser apagadas (defesa em profundidade)
CREATE FUNCTION reserva_bloquear_exclusao() RETURNS trigger AS $$
BEGIN
    IF OLD.status IN ('EM_USO', 'CONCLUIDA') THEN
        RAISE EXCEPTION 'Reserva iniciada nao pode ser removida (status=%)', OLD.status;
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reserva_bloquear_exclusao
    BEFORE DELETE ON reserva
    FOR EACH ROW EXECUTE FUNCTION reserva_bloquear_exclusao();

-- RN-07: auditoria imutável (sem FK de propósito: o histórico sobrevive à reserva)
CREATE TABLE registro_auditoria (
    id              UUID PRIMARY KEY,
    reserva_id      UUID         NOT NULL,
    status_anterior VARCHAR(20),
    status_novo     VARCHAR(20)  NOT NULL,
    ator            VARCHAR(120) NOT NULL,
    ocorrido_em     TIMESTAMPTZ  NOT NULL
);

CREATE FUNCTION registro_auditoria_imutavel() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'registro_auditoria e imutavel';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_registro_auditoria_imutavel
    BEFORE UPDATE OR DELETE ON registro_auditoria
    FOR EACH ROW EXECUTE FUNCTION registro_auditoria_imutavel();
