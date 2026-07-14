# Smart Learning Agent - Codex Map

This repository is developed with coding agents. Treat the repository as the system of record: if a decision, invariant, or workflow is not recorded here, it is not durable context.

## Start Here

1. Read `docs/PRODUCT.md` for scope and product invariants.
2. Read `docs/ARCHITECTURE.md` before changing boundaries or data flow.
3. Read the nearest nested `AGENTS.md` for local rules.
4. For non-trivial work, create a task file from `docs/tasks/TEMPLATE.md`.
5. Read `thesis/AGENTS.md` when a change affects a thesis claim, experiment, figure, or formal document.
6. Run `powershell -ExecutionPolicy Bypass -File scripts/verify.ps1` before handoff.

## Repository Map

- `backend/`: Java 21, Spring Boot, MyBatis, AI/vector adapters. See `backend/AGENTS.md`.
- `frontend/`: Vue 3, TypeScript, Vite, Element Plus. See `frontend/AGENTS.md`.
- `docs/`: product, architecture, quality, and harness guidance.
- `harness/`: machine-readable quality budgets.
- `scripts/`: local feedback and full verification commands.
- `thesis/`: graduation-design metadata, templates, paper outline, evidence, and experiments.
- `deploy/` and `docker-compose.yml`: deployment topology.
- `毕业设计_系统设计方案.md`: detailed design background; keep it aligned with implemented behavior.
- `毕业设计_开题报告.md`: academic context, not the source for runtime behavior.

## Non-negotiable Invariants

- Generated questions must remain traceable to source material and retrieval evidence.
- Generated questions start in a reviewable state; AI output is never silently treated as approved truth.
- Memory mode must remain usable for fast local development without MySQL, Qdrant, or paid AI APIs.
- External AI, embedding, vector, and persistence providers stay behind interfaces/adapters.
- Secrets never enter source control. Add names and safe placeholders to `.env.example` only.
- Controllers depend on services, not concrete repositories, mappers, or service implementations.
- Every bug fix adds a regression test when the behavior can be reproduced deterministically.
- Every academic claim must point to versioned implementation, test, experiment, or source evidence.

## Working Agreement

- Preserve unrelated user changes in a dirty worktree.
- Prefer small, reviewable changes with explicit acceptance criteria.
- Do not expand `frontend/src/App.vue`, `frontend/src/style.css`, or `frontend/src/api.ts`; extract code while touching those areas.
- Do not weaken tests or quality budgets to make a change pass. Update a budget only with a recorded reason in `docs/QUALITY.md`.
- Use mock providers in automated tests. Real network calls belong in explicitly requested integration checks.
- Update documentation in the same change when behavior, commands, contracts, or architecture change.
- Update `thesis/evidence/claim-evidence-matrix.csv` when a change strengthens or invalidates a paper claim.

## Definition of Done

- Acceptance criteria are satisfied and the task file records any deliberate follow-up.
- Relevant tests cover the change, including failure paths for risky behavior.
- `scripts/verify.ps1` passes.
- The diff contains no credentials, generated build output, or unrelated edits.
- The final handoff states what changed, how it was verified, and any remaining risk.
- Thesis-impacting work records its evidence and does not describe planned behavior as implemented behavior.
