# Architecture Map

## Runtime Topology

```text
Browser -> Vue/Vite or Nginx -> Spring Boot API
                                  |-> repository interface -> memory or MyBatis/MySQL
                                  |-> VectorStore          -> memory or Qdrant
                                  |-> EmbeddingClient      -> mock or OpenAI-compatible API
                                  `-> LlmClient            -> mock or OpenAI-compatible API
```

The default development path uses memory repositories, memory vector search, and mock AI providers. This is the fast feedback path and must stay green.

## Backend Boundaries

```text
controller -> service -> serviceimpl -> repository interface
                               |-----> parser interface/adapter
                               |-----> AI interface/adapter
                               `-----> vector interface/adapter
```

- `controller` owns HTTP concerns, request validation, and response translation.
- `service` names use cases; `serviceimpl` coordinates domain behavior.
- `repository` hides memory and MyBatis persistence choices.
- `ai`, `vector`, and `parser` isolate external or replaceable capabilities.
- `domain` holds the records exchanged across those boundaries.

Dependency direction is enforced first at the controller boundary by `scripts/check-harness.ps1`. Add narrower architecture checks as packages stabilize.

## Main Generation Flow

1. A teacher selects material, generation constraints, and an optional workflow mode.
2. `QuestionGenerationServiceImpl` creates a `RUNNING` task and delegates execution to `AgentRunServiceImpl`.
3. The orchestrator invokes the role executors allowed by the selected mode. Each executor receives a typed input produced from prior output and records calls through `AgentExecutionContext`.
4. The orchestrator persists each step transition, structured input/output JSON, timestamps, attempts, and actual tool/rule calls. A failed role stops the chain and links a `FAILED` generation task to the failed run.
5. Successful candidates retain source references or an explicit direct-mode ungrounded marker and remain `PENDING_REVIEW`.
6. A teacher approves, rejects, or edits candidates. Approved content can enter practice sets; attempts feed learner summaries.

When changing this flow, preserve evidence identifiers and review state across API, persistence, and UI layers.

## Executable Role Workflow

```text
MaterialUnderstandingAgent
  -> RetrievalPlanningAgent (plans queries and executes retrieval)
  -> QuestionGenerationAgent (creates new candidate objects)
  -> QualityReviewAgent (independent PASS/REVISE/HUMAN_REVIEW/REJECT decisions)
  -> DedupDifficultyAgent (existing/candidate duplicate checks and difficulty assessment)
  -> TeachingComposerAgent (final pending-review candidates and teacher advice)
```

`AgentRoleExecutor<I, O>` is the independently testable role boundary. `AgentRunServiceImpl` is the orchestrator, not a second generator. It passes the real typed output of one executor into the next and never creates successful reports for roles it did not invoke. `AgentRunTrace` persists `RUNNING`, `FINISHED`, and `FAILED` transitions incrementally. No retry is currently performed, so every step records `attempt = 1`.

The additive `workflowMode` request field provides the E03 ablation boundary:

| Mode | Executed roles | Evidence behavior |
| --- | --- | --- |
| `DIRECT` | generation | no retrieval; every source reference is an explicit ungrounded/manual-confirmation marker |
| `RAG_ONLY` | material understanding, retrieval planning/execution, generation | retrieved or material-fallback evidence; no independent review roles |
| `RAG_MULTI_AGENT` | all six roles | retrieval plus independent review, dedup/difficulty, and teaching composition |

Omitting `workflowMode` selects `RAG_MULTI_AGENT`, preserving the existing frontend request. `GET /api/agents/runs` remains compatible and now exposes additive mode, timing, failure, step, tool-call, and observation-metric fields; `workflowMode` can be supplied as a query filter for experiment collection. Operational observations are not formal experiment results.

## Agent Run Persistence

Memory and MyBatis repositories store the same expanded `AgentRunRecord`. MySQL initialization is defined in `backend/src/main/resources/db/schema-mysql.sql`; existing databases must apply `backend/src/main/resources/db/migration/2026-07-13-agent-run-execution-trace.sql` once before using the new mapper. Step and tool payloads remain JSON so role-specific structures can evolve without six role-specific tables. Scalar mode/failure/timing fields and `metrics_json` support filtering and E03 collection.

## Frontend Migration Direction

The current frontend is a working vertical prototype concentrated in three large files. Refactor by feature while delivering normal work:

```text
src/
  api/                 # transport plus domain endpoint modules
  components/          # reusable presentation components
  features/
    auth/
    courses/
    materials/
    questions/
    practice/
    admin/
  styles/              # tokens, shell, and feature styles
  App.vue              # shell and feature composition
```

Do not rewrite everything at once. Extract the feature being changed, preserve behavior, add a test seam, then lower the corresponding budget in `harness/quality-baseline.json`.

The current migration has extracted `LoginView`, `DashboardOverview`, the role-aware `WorkspaceShell`, and the multi-agent `AgentRunsView`. `WorkspaceShell` owns navigation, responsive drawer behavior, current-view metadata, service status, account actions, and the current-material shortcut; `App.vue` still coordinates shared state and the remaining course, material, question, practice, and administration views. The presentation layer is split into bounded style files under `src/styles/`, with shared design tokens kept in `tokens.css` and no remote font or asset dependency.

## Change Rules

- New provider integrations implement an existing interface or add a small interface before business code depends on them.
- New persistence behavior works in memory mode first, then receives MyBatis parity.
- Schema changes include migration/initialization updates and a deterministic verification path.
- New endpoints include validation, authorization, error behavior, and contract tests proportional to risk.
- Architectural exceptions must be documented in `docs/QUALITY.md` with an owner and removal condition.
