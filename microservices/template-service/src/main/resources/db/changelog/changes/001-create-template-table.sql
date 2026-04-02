-- liquibase formatted sql

-- changeset dev:001-create-template-table
CREATE TABLE IF NOT EXISTS template (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    content     TEXT,
    status      VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_by  VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_template_status   ON template(status);
CREATE INDEX idx_template_created  ON template(created_at DESC);

-- rollback DROP TABLE IF EXISTS template;

