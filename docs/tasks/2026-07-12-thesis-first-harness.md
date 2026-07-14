# Task: Make graduation-design evidence a first-class repository output

## Problem

The system can build and run, but the opening report and final thesis could drift from implementation behavior. Academic claims, experiments, figures, and school-formatted Word deliverables need a durable evidence chain.

## Scope

- In scope: confirmed student metadata, retained template provenance, paper outline, claim-evidence matrix, experiment registry, working/release checks, and repository guidance.
- Out of scope: filling unknown dates, implementing the real multi-agent workflow, running formal experiments, drafting full chapters, and generating final DOCX files.

## Acceptance Criteria

- [x] Confirmed student and title metadata are versioned; unknown dates remain empty.
- [x] Thesis and opening-report reference templates are copied without modification and protected by SHA-256 checks.
- [x] Seven paper chapters have explicit evidence requirements.
- [x] Existing, partial, planned, and unsupported claims can be distinguished mechanically.
- [x] Four formal experiments are registered without fabricated results.
- [x] Daily thesis checks pass while release checks fail on incomplete evidence and deliverables.
- [x] The canonical repository verification includes thesis checks.

## Constraints and Invariants

- Relevant product invariant: generated questions remain traceable and reviewable.
- Relevant architecture boundary: the retained multi-agent title requires executed role behavior, not only recorded labels.
- Unrelated worktree changes to preserve: all frontend redesign work and existing harness files.

## Verification

- `scripts/check-harness.ps1` passes.
- `scripts/check-thesis.ps1` passes in working mode.
- `scripts/check-thesis.ps1 -Release` intentionally fails until dates, evidence, experiments, chapters, and final DOCX files are complete.
- Canonical command: `powershell -ExecutionPolicy Bypass -File scripts/verify.ps1`

## Handoff

- Decisions made: retain the multi-agent title; use template hashes; keep missing dates blank; make raw evidence and negative results durable.
- Remaining risks: multi-agent execution is not yet independent; formal datasets and evaluations do not exist; DOCX rendering requires a working LibreOffice or Word path.
- Follow-up work deliberately deferred: implement role execution and pre-register E01-E04 before collecting formal results.
