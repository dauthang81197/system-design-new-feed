--liquibase formatted sql

--changeset pv:001 labels:init comment:Create posts table
CREATE TABLE IF NOT EXISTS posts (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id   UUID        NOT NULL,
    caption     TEXT,
    media_url   TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_posts_author_id_created
    ON posts (author_id, created_at DESC);

--rollback DROP INDEX IF EXISTS idx_posts_author_id_created;
--rollback DROP TABLE IF EXISTS posts;

