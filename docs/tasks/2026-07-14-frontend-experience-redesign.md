# Task: Redesign the frontend learning workspace

## Problem

The current frontend exposes the product's complete teacher, student, and administrator workflows, but its visual hierarchy does not make the next action obvious enough. Evidence from `frontend/src/App.vue` and a local browser inspection shows nine top-level destinations using similar card and heading treatments, while upload, retrieval, generation, review, and practice actions compete at nearly the same visual weight. The shell and feature views are functional, but they read as a generic administration console rather than a coherent, evidence-first teaching workspace.

`frontend/src/App.vue` is also 1,565 lines and still owns the application shell plus every feature view. `frontend/AGENTS.md` identifies this file as a migration hotspot and requires touched UI to move incrementally toward feature and reusable component boundaries.

## Scope

- In scope:
  - Establish a distinctive, accessible visual system for login, shell, dashboard, and all feature views.
  - Make primary actions, current scope, status, evidence, and review state visually self-explanatory.
  - Extract the reusable authenticated application shell and navigation from `App.vue`.
  - Preserve teacher, student, and administrator role visibility and all existing API operations.
  - Verify desktop and mobile layouts in a real browser.
- Out of scope:
  - Backend or API contract changes.
  - New product capabilities, routes, or authentication behavior.
  - Changing generated-question review or evidence invariants.
  - Introducing remote fonts or other runtime network dependencies.

## Acceptance Criteria

- [x] Login and authenticated views share a coherent, high-quality visual direction with intentional typography, color, spacing, and motion.
- [x] Each workspace view presents one clear primary task and preserves its supporting status, loading, empty, error, and evidence information.
- [x] Navigation remains role-aware and usable without instructional onboarding.
- [x] Existing teacher content management, student practice, and administrator account actions remain reachable and functional.
- [x] The layout works at desktop and mobile widths without horizontal overflow or inaccessible controls.
- [x] The authenticated shell is extracted from `App.vue` into a reusable component boundary.

## Constraints and Invariants

- Relevant product invariant: generated questions remain source-traceable and visibly reviewable; the UI must not imply that pending AI output is approved.
- Relevant architecture boundary: feature components do not call `fetch`; API calls remain in the existing transport layer and controllers/contracts are unchanged.
- Unrelated worktree changes to preserve: all existing backend multi-agent, Harness, thesis, documentation, wrapper, README, and `.gitignore` changes.
- Use local font fallbacks and existing assets so memory/mock development remains offline-capable.

## Verification

- Automated tests to add or update: no frontend test runner currently exists; the TypeScript production build and repository Harness pass. This known interaction-test gap remains recorded in `docs/QUALITY.md`.
- Manual/integration evidence, if needed: browser checks covered the 1280px login and teacher workspace, materials, questions, and agent trace views; student-only navigation and dashboard; administrator navigation, role labels, and account dialog; and the 390px login, dashboard, mobile drawer, and question prerequisite state. The browser console reported no errors or warnings, and measured mobile document widths did not exceed the viewport.
- Canonical command: `powershell -ExecutionPolicy Bypass -File scripts/verify.ps1`

## Handoff

- Decisions made: implemented the editorial "teaching studio" direction with warm paper surfaces, deep teal navigation, amber task cues, evidence-oriented status treatments, and restrained staged motion. The authenticated shell and agent trace presentation are now independent components. Role labels use stable role codes so corrupted optional backend display text cannot degrade the interface.
- Remaining risks: browser checks cover representative workflows but cannot replace focused component tests until Vitest and Vue Test Utils are introduced. The main JavaScript bundle remains large because Element Plus is currently bundled as one application chunk.
- Follow-up work deliberately deferred: API module extraction, route-level code splitting, and full feature-by-feature state extraction remain separate migration tasks.
