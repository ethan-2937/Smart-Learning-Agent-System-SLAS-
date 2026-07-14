# Frontend Agent Guide

## Current Shape

The UI works, but `App.vue`, `style.css`, and `api.ts` are migration hotspots. They have temporary line budgets in `harness/quality-baseline.json`; do not use those budgets as design targets.

## Target Shape

- `src/features/<feature>/`: feature components, composables, and feature types.
- `src/components/`: reusable presentation components without business API calls.
- `src/api/`: transport client and domain-specific endpoint modules.
- `src/styles/`: tokens, layout, and feature styles.
- `src/App.vue`: application shell, navigation, and top-level composition only.

Create these directories incrementally when a touched feature is extracted. Avoid a big-bang rewrite.

## Rules

- Do not add new feature logic directly to `App.vue`.
- Do not call `fetch` outside the API transport layer.
- Keep server contract types explicit; do not use `any` to bypass a mismatch.
- Preserve authentication, role visibility, and error/loading states during extraction.
- Use CSS variables and shared tokens instead of repeating literal colors and spacing.
- When fixing a UI defect, add a focused test after the frontend test runner is introduced; until then, record the missing test in the task file.

## Commands

```powershell
cd frontend
npm ci
npm run build
```

Run the repository-level `scripts/verify.ps1` before handoff.
