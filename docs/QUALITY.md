# Quality Strategy

## Feedback Layers

1. `scripts/check-harness.ps1` gives sub-second structural feedback: required context, file budgets, test presence, and controller boundaries.
2. `backend/mvnw test` compiles the backend and runs deterministic tests.
3. `frontend/npm run build` performs TypeScript checking and a production build.
4. `.github/workflows/ci.yml` repeats the same checks on every push and pull request.
5. Docker and real-provider checks are opt-in integration checks, not default unit tests.

The canonical local gate is:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify.ps1
```

## Complexity Ratchet

The first harness does not pretend the current frontend is already modular. Exact budgets in `harness/quality-baseline.json` keep known hotspots from growing while lower defaults apply to new files.

- `App.vue`, `style.css`, and `api.ts` are debt, not examples.
- Extract code when working in a hotspot and lower its budget after extraction.
- Never increase a budget only to pass CI. Record the reason, owner, and removal condition here if an exceptional temporary increase is necessary.

## Test Priorities

1. Evidence-grounded retrieval and question generation.
2. Authentication and role enforcement.
3. Review state transitions and question edits.
4. Practice scoring and mastery calculations.
5. Persistence parity between memory and MyBatis modes.
6. Frontend feature behavior after a test runner is introduced.

## Executable Multi-Agent Coverage

The default backend suite now exercises the multi-agent boundary without network, MySQL, Qdrant, or a paid model:

- `AgentRunServiceImplTest` checks role order, prior-output handoff, actual call records, timing, failure persistence, and downstream stop behavior.
- `QualityReviewAgentTest` checks independent `PASS`, `REJECT`, `REVISE`, and `HUMAN_REVIEW` decisions.
- `QuestionWorkflowIntegrationTest` runs `DIRECT`, `RAG_ONLY`, and `RAG_MULTI_AGENT` with memory/mock dependencies and verifies evidence plus `PENDING_REVIEW` invariants and failed-task behavior.
- `MyBatisAgentRunRepositoryTest` checks the expanded trace mapping without requiring an external database.

These tests establish implementation evidence and experiment readiness. They do not establish that one mode has better question quality; that claim remains gated by formal E03 raw results and analysis.

## Current Debt Register

| Debt | Risk | Next reduction step | Removal condition |
| --- | --- | --- | --- |
| Frontend state and several feature views remain concentrated in `App.vue` and `api.ts` | Changes can still collide across course, material, question, and practice screens | Continue the completed auth/dashboard/shell/agent extraction pattern one feature at a time | App shell contains only navigation/composition and domain API modules are separated |
| Backend began with zero tests | Refactors have weak behavioral feedback | Add tests around each touched use case | Critical flows have unit and contract coverage |
| Only controller import boundary is enforced | Layer drift can occur deeper in the backend | Introduce ArchUnit after package rules settle | Main dependency directions run in tests |
| No frontend test runner | Build catches types, not interactions | Add Vitest and Vue Test Utils during first extraction | Each extracted feature has focused tests |
| Real-provider evaluation is informal | Model changes may reduce grounding quality | Add a small versioned evaluation dataset | Provider changes report repeatable quality metrics |
| Multi-agent quality benefit is not yet measured | Executable role traces could be mistaken for evidence of higher question quality | Run registered E03 on the versioned dataset and retain raw results, including negative findings | E03 is complete and C07 is supported or rejected from evidence |
| Thesis evidence is not yet automated | Results can drift from the submitted paper | Register experiments and link every claim to evidence | Release check passes with reproducible results |
