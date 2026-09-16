# 22 — Risk register

| ID | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | Scope explosion (120 features) | High | High | P0 contract; flags; phase gates |
| R2 | AI cost overrun | High | High | router, cache, budgets, authored-first |
| R3 | Hallucinated curriculum | High | High | critic, sources, human gates |
| R4 | Sandbox escape | Low | Critical | isolated executor, no creds, gVisor/E2B |
| R5 | Prompt injection → tool abuse | Medium | High | allowlist, data/instruction split |
| R6 | Copyright ingestion | Medium | High | legal allowlist, no pirate books |
| R7 | “Always current” overclaim | High | High | honest timestamps; CI-09 |
| R8 | Graph/query performance | Medium | Medium | neighborhood fetch, closure table |
| R9 | Vendor lock | Medium | Medium | provider interface |
| R10 | Single-engineer bottleneck | High | Medium | docs first; modular boundaries |
| R11 | Fake progress UX | Medium | High | mastery spec; UX bar |
| R12 | Executor latency | Medium | Medium | warm pools |
| R13 | OpenSearch ops burden | Medium | Low | delay hybrid; SQL search P0 |
| R14 | Privacy of chats | Medium | High | retention controls, no train |
| R15 | Exam cheating via tutor | High | Medium | exam mode isolates tutor; honor + item bank |
| R16 | Stale seed while building | Medium | Medium | volatility tags; review dates |
| R17 | Windows DX (no /bin/sh; dual JDK+Node toolchain) | Medium | Medium | WSL2 + Temurin 21 + Git; Compose for JVM services |
| R18 | Regulatory (EU AI Act later) | Low | Medium | transparency, logs, human oversight on education updates |

## Top 5 we actively manage

R1, R2, R3, R4, R7.
