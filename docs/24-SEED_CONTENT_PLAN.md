# 24 — Seed content plan

**Rule:** no fake inflation. Seed must be *real teaching material* sufficient to prove architecture and the P0 loop.

## Inventory

### A. Taxonomy stubs

Create all domain units A–BE with title, description, volatility, knowledge_class, empty children OK.

### B. Vertical slice (must be complete)

**Path:** AI Engineer (abridged)

1. **Computer Fundamentals** — 1 chapter: “What a program is,” 2 knowledge units.  
2. **Python** — 1 chapter: types, functions, 3 units with code blocks + exercises (P0 lab language). Java as a **sandbox language** is P2; the **platform** is already Java.  
3. **Linear Algebra** — Vectors, Matrices, Matmul (3 units). Math + intuition. LOW volatility.  
4. **Neural Networks** — one unit with why, ELI5→intermediate, misconception (loss vs accuracy), tiny numpy-style example.  
5. **RAG** — one HIGH volatility unit: embeddings, retrieve, generate; version field; official doc resource links.  
6. **Project:** “Build a linear regression model” (beginner) with milestones and tests runnable in lab.  
7. **Project stub:** “Production RAG” outline only (P1 fill).

### C. Graph edges

Connect the example chain in doc 08. Each PREREQUISITE_OF asserted.

### D. Assessments

- Diagnostic bank: 25 items (Python, vectors, ML intuition).  
- Per complete unit: 5 MCQ + 1 short + 1 coding where applicable.  
- Reports consume tagged weaknesses.

### E. Resources (provenance filled)

For RAG unit: official docs URLs (OpenAI/Anthropic/LangChain or vendor-neutral papers + FAISS/pgvector docs) with last_verified dates.  
For linear algebra: a public-domain or openly licensed reference (e.g. 3Blue1Brown as *video resource* with URL, not ripped transcript).

### F. Flashcards

Golden cards for vectors/matmul/backprop/RAG terms (author-locked).

### G. Intelligence

2 manual `AIUpdate` examples (one model release, one deprecation) to drive Evolve UI without live scrapers.

## Quality bar for a “complete” unit

All of: why, 2+ explanation levels, one example, one misconception, one exercise, one quiz item, mastery criteria, at least one cited resource, difficulty, time, volatility, last_reviewed.

## What we will not do in seed

- 50k generated micro-concepts.  
- Wikipedia dumps.  
- Copyrighted book chapters.  
- Random quiz without misconception tags.
