# Graduation-design Agent Guide

`thesis/` is a first-class product area. Its purpose is to keep the opening report, implementation, experiments, paper, and final Word deliverables mutually consistent.

## Start Here

1. Read `metadata.yml` for confirmed student and title information.
2. Read `paper/outline.md` before drafting a chapter.
3. Read `evidence/claim-evidence-matrix.csv` before making or repeating an academic claim.
4. Read `experiments/registry.json` before running or interpreting an experiment.
5. Run `scripts/check-thesis.ps1` after thesis-related edits.

## Evidence Rules

- Never describe `planned`, `partial`, or UI-only behavior as completed research results.
- A claim needs a code path plus a deterministic test, experiment, or authoritative citation as appropriate.
- Raw experiment output is immutable. Regenerate summaries and figures from raw results rather than editing them by hand.
- Record Git commit, dataset hash, configuration, model/provider, parameters, timestamp, and environment for every formal run.
- Negative and inconclusive results remain in the repository.
- Screenshots illustrate behavior; they do not replace tests or quantitative evidence.

## Document Rules

- Files under `templates/` are immutable references. Verify their SHA-256 before every build.
- The opening-report layout reference contains an old project's prose; replace every body slot and never copy its claims.
- Do not invent missing dates, results, participant counts, citations, or advisor feedback.
- Build formal DOCX files from template copies, never from blank documents and never by editing the retained template.
- A formal DOCX is not done until every page has been rendered and visually inspected. If rendering is unavailable, report that limitation explicitly.

## Release Gate

Daily work runs `scripts/check-thesis.ps1`. Submission preparation runs:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-thesis.ps1 -Release
```

Release mode requires dates, completed evidence, resolved placeholders, and final DOCX deliverables.
