-- aileapro Phase 8: search and retrieval.
--
-- Postgres FTS first (ADR 0006). OpenSearch waits until hybrid quality needs it.
-- Embeddings live in content_chunks with pgvector; the fake embedder uses 64
-- dimensions so CI and local never need a paid key. A real model later needs its
-- own dimension and a migration that rebuilds the column.
--
-- search_tsv columns are maintained by the indexer, not by triggers: the same
-- Java path that writes chunks also writes the FTS vectors, so H2 tests that
-- never see these columns and the MemoryContentIndex stay in lockstep.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE learning_units
    ADD COLUMN search_tsv tsvector;

ALTER TABLE unit_blocks
    ADD COLUMN search_tsv tsvector;

CREATE INDEX idx_learning_units_search ON learning_units USING GIN (search_tsv);
CREATE INDEX idx_unit_blocks_search ON unit_blocks USING GIN (search_tsv);
CREATE INDEX idx_learning_units_title_trgm ON learning_units USING GIN (title gin_trgm_ops);

CREATE TABLE content_chunks (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    block_id UUID NOT NULL REFERENCES unit_blocks (id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    text TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    token_estimate INT NOT NULL DEFAULT 0,
    embedding vector(64) NOT NULL,
    model_slug VARCHAR(64) NOT NULL,
    knowledge_class VARCHAR(32) NOT NULL DEFAULT 'STABLE',
    block_updated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_content_chunks_block_index UNIQUE (block_id, chunk_index),
    CONSTRAINT ck_content_chunks_index CHECK (chunk_index >= 0),
    CONSTRAINT ck_content_chunks_knowledge_class CHECK (knowledge_class IN (
        'FOUNDATIONAL', 'STABLE', 'VERSION_SPECIFIC', 'CURRENT',
        'EMERGING', 'EXPERIMENTAL', 'DEPRECATED'))
);

CREATE INDEX idx_content_chunks_unit ON content_chunks (unit_id);
CREATE INDEX idx_content_chunks_block ON content_chunks (block_id);
CREATE INDEX idx_content_chunks_text ON content_chunks USING GIN (to_tsvector('english', text));
-- Exact scan is fine for the seed corpus; an HNSW index waits until there is
-- enough volume for approximate search to matter.
CREATE INDEX idx_content_chunks_hash ON content_chunks (content_hash);
