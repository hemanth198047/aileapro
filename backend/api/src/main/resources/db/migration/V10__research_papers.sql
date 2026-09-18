-- aileapro Phase 11: research papers with forced citations.
--
-- DOI and abstract_text let the explainer retrieve from authored paper metadata
-- only. Full PDFs are not stored here; an empty retrieval set must refuse rather
-- than invent references.

ALTER TABLE sources
    ADD COLUMN doi VARCHAR(128),
    ADD COLUMN abstract_text TEXT;

CREATE UNIQUE INDEX uq_sources_doi ON sources (doi) WHERE doi IS NOT NULL;
