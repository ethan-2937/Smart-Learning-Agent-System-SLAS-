# Task: Bootstrap a Codex-ready repository harness

## Problem

The project was implemented by agents but had no durable agent entry point, no portable backend command, no tests, and no single verification gate. Future Codex sessions would need large prompts and could not reliably distinguish intent from implementation accidents.

## Scope

- In scope: repository navigation, product and architecture context, mechanical checks, portable commands, CI, and one deterministic backend test.
- Out of scope: refactoring existing frontend hotspots, broad backend coverage, frontend test-runner adoption, and real-provider evaluations.

## Acceptance Criteria

- [x] Codex can discover repository intent and local rules from root and nested `AGENTS.md` files.
- [x] Product, architecture, quality, and task-contract context are versioned in `docs/`.
- [x] One command runs structural checks, backend tests, and the frontend production build.
- [x] CI repeats the canonical local verification command.
- [x] Known frontend hotspots cannot grow past explicit temporary budgets.
- [x] Existing unrelated frontend worktree changes remain untouched.

## Constraints and Invariants

- Relevant product invariant: retrieval evidence and reviewability remain explicit.
- Relevant architecture boundary: controllers depend on service interfaces rather than concrete lower layers.
- Unrelated worktree changes to preserve: `frontend/index.html`, public logos, `frontend/src/App.vue`, and `frontend/src/style.css`.

## Verification

- Added `InMemoryVectorStoreTest` for material filtering and relevance ordering.
- Ran `powershell -ExecutionPolicy Bypass -File scripts/verify.ps1` successfully on 2026-07-12.
- Canonical command: `powershell -ExecutionPolicy Bypass -File scripts/verify.ps1`

## Handoff

- Decisions made: use additive migration, quality ratchets, mock-provider tests, and a Maven Wrapper.
- Remaining risks: backend coverage is still minimal; frontend behavior has no automated interaction tests; CI has not run remotely yet.
- Follow-up work deliberately deferred: feature-by-feature frontend extraction and a versioned question-quality evaluation set.
