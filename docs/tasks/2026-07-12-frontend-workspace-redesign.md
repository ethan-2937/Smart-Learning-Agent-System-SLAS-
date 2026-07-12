# Task: Redesign the role-based learning workspace

## Problem

The frontend presented every feature in one long scrolling page, which made navigation, current context, and role-specific next actions difficult to recognize. The existing logo also lost clarity at navigation and favicon sizes.

## Scope

- In scope: login experience, SLAS visual identity, role-aware navigation, teacher/student dashboards, responsive layout, empty states, and accessible form labels.
- Out of scope: backend contracts, authorization rules, new learning features, and persistence behavior.

## Acceptance Criteria

- [x] Administrators, teachers, and students see only the navigation allowed for their role.
- [x] Each primary feature opens as a focused workspace view instead of requiring page scrolling.
- [x] Desktop and 390px mobile layouts have no page-level horizontal overflow.
- [x] Login, empty, loading, disabled, and service-status states remain visible and understandable.
- [x] The new SVG logo remains recognizable in login, sidebar, and favicon sizes.

## Constraints and Invariants

- Relevant product invariant: frontend role visibility complements but never replaces backend authorization.
- Relevant architecture boundary: UI changes keep all network access in the existing API module.
- Unrelated worktree changes to preserve: repository guidance, harness, backend tests, docs, README, and scripts created outside this task.

## Verification

- Automated tests to add or update: no frontend runner exists yet; interaction coverage remains a recorded follow-up.
- Manual/integration evidence: browser checks for admin, teacher, student, question/practice views, 1280px desktop, and 390px mobile navigation.
- Canonical command: `powershell -ExecutionPolicy Bypass -File scripts/verify.ps1` (passed on 2026-07-12).

## Handoff

- Decisions made: use a persistent role-aware sidebar, compact page context header, task dashboard, restrained blue/teal/yellow status palette, and an open-book knowledge-path logo.
- Remaining risks: frontend interaction checks are manual until Vitest and Vue Test Utils are introduced.
- Follow-up work deliberately deferred: route persistence, class management, assignments, and automated frontend interaction tests.
