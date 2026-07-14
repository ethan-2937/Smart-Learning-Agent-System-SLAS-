# Task: Build an executable multi-agent question workflow

## Problem

The current implementation cannot support the thesis claim that multiple roles actually collaborate. `QuestionGenerationServiceImpl` completes retrieval and question generation before calling `AgentRunServiceImpl`, while `AgentRunServiceImpl` then constructs six successful step labels and three successful tool-call labels from already completed results. The labels therefore do not prove role execution.

Evidence-backed gaps:

- `backend/src/main/java/com/hlju/learning/serviceimpl/QuestionGenerationServiceImpl.java` performs retrieval, fallback selection, draft generation, persistence, and only then calls `runQuestionWorkflow`.
- `backend/src/main/java/com/hlju/learning/serviceimpl/AgentRunServiceImpl.java` creates every step with `FINISHED` and does not execute role-specific behavior.
- `AgentStepReport` has no structured input/output trace, start/end time, attempt count, or failure details.
- `AgentToolCall` has no role, call identity, timing, or failure details, and existing records are constructed after the underlying work rather than around an actual invocation.
- `AgentRunRecord` has no workflow mode, failure role, error summary, completion time, or experiment-observation metrics.
- A generation exception leaves no consistently persisted failed task/run path.
- Memory and MyBatis repositories persist the same overstated trace; the frontend can only render the resulting static summaries.
- `thesis/evidence/claim-evidence-matrix.csv` correctly keeps C03 `planned`, C07 `planned`, and `thesis/experiments/registry.json` keeps E03 `blocked`.

## Scope

- In scope:
  - Replace post-hoc labels with an orchestrator that invokes independently testable role executors.
  - Implement material understanding, retrieval planning/execution, draft generation, independent quality review, deduplication/difficulty assessment, and teaching composition.
  - Add `DIRECT`, `RAG_ONLY`, and `RAG_MULTI_AGENT` execution modes through an additive request field with `RAG_MULTI_AGENT` as the compatibility default.
  - Persist real step/tool timing, inputs, outputs, status, failure role/error, and deterministic experiment-observation metrics in memory and MyBatis modes.
  - Keep generated candidates `PENDING_REVIEW`; retain source references or an explicit ungrounded/manual-confirmation marker.
  - Add deterministic unit tests and focused wiring/persistence tests where needed.
  - Update architecture, quality, thesis chapters, C03 evidence, and E03 readiness without recording formal results.
- Out of scope:
  - Running E03 formally or recording accuracy, pass-rate, sample-size, latency conclusions, or other experiment results.
  - Marking C03 `supported`, C07 above `planned`, or E03 `complete`.
  - Introducing a distributed agent framework, real-provider calls in default tests, autonomous publishing, or final thesis DOCX generation.
  - Broad frontend refactoring unrelated to API compatibility.

## Acceptance Criteria

- [x] The orchestrator invokes the applicable real role executors in order and each step consumes the previous step's output.
- [x] Every executed role has a typed input/output boundary, independent status, start/end time, actual tool or deterministic-rule call, and test seam.
- [x] Independent quality review can pass, reject, request revision, or require human review based on candidate/evidence content.
- [x] A role failure persists `FAILED`, identifies the role/error, does not mark later roles `FINISHED`, and leaves the generation task consistently `FAILED`.
- [x] Tool-call records are created around calls that actually execute, including failed calls.
- [x] `DIRECT`, `RAG_ONLY`, and `RAG_MULTI_AGENT` are distinguishable, runnable in memory/mock mode, and expose collection-ready run observations without formal experiment values.
- [x] Every returned question remains `PENDING_REVIEW` and contains retrieved source evidence or an explicit ungrounded/manual-confirmation reference.
- [x] Existing agent/task APIs remain compatible through additive fields and the default mode retains the full review workflow.

## Constraints and Invariants

- Relevant product invariant: generated questions remain traceable or explicitly ungrounded and never become trusted/published without teacher review.
- Relevant architecture boundary: controllers depend on services; repositories, AI, embedding, vector search, and persistence remain behind interfaces/adapters.
- Default tests use memory repositories and mock/deterministic providers only; no API key, MySQL, Qdrant, or paid model is required.
- Unrelated worktree changes to preserve: all pre-existing tracked and untracked Harness, thesis, wrapper, workflow, README, `.gitignore`, and test changes shown by the initial `git status`.

## Verification

- Automated tests to add or update:
  - Orchestrator ordering, real output handoff, actual tool calls, timestamps, and mode-specific role selection.
  - Quality-review pass/reject/revise/human-review decisions.
  - Failure persistence and downstream-step behavior.
  - Question evidence plus `PENDING_REVIEW` invariants.
  - Deterministic memory/mock generation and existing API behavior.
  - MyBatis JSON/domain mapping for the expanded trace where practical without an external database.
- Manual/integration evidence, if needed: inspect an API run record in each mode; do not treat it as a formal E03 result.
- Canonical command: `powershell -ExecutionPolicy Bypass -File scripts/verify.ps1`
- Thesis command: `powershell -ExecutionPolicy Bypass -File scripts/check-thesis.ps1`
- Result: backend suite passed 11 tests; repository harness, working-mode thesis checks, and frontend production build passed through `scripts/verify.ps1`.

## Handoff

- Decisions made: use small Spring role executors plus one orchestrator; use additive API fields and a full multi-agent default; keep experiment observations separate from formal results; do not retry failed roles automatically.
- Remaining risks: deterministic review rules are implementation checks rather than validated quality scorers; real-provider and live-MySQL behavior have no external integration run; providers may omit token usage; the existing-database migration must be applied manually once.
- Follow-up work deliberately deferred: populate and freeze the formal E03 dataset, run the three modes against one fixed configuration, retain immutable raw results and environment metadata, perform statistical analysis, and update C03/C07 only from that evidence.
