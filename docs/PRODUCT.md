# Product Contract

## Purpose

Smart Learning Agent turns course material into reviewable learning content. Its distinguishing behavior is evidence-grounded generation: retrieval evidence, generation steps, review state, and learner feedback remain inspectable instead of hiding behind a single model response.

## Primary Users

- Teachers upload and organize material, generate questions, inspect evidence, and approve or reject questions.
- Students practice approved questions and review attempts, mistakes, and mastery signals.
- Administrators manage accounts, roles, runtime configuration, and operational health.

## Product Invariants

1. A generated question is linked to material evidence or explicitly marked as an ungrounded fallback.
2. Generation output is reviewable before it becomes trusted learning content.
3. Student submissions retain enough information to explain scoring and support later analysis.
4. Memory/mock mode supports a complete local demonstration without external infrastructure.
5. MySQL/Qdrant/real-model modes may improve durability or quality but must preserve the same domain contract.
6. Authorization is enforced by the backend; hiding a frontend control is not a security boundary.

## Current Scope

- Material upload, parsing, chunking, and retrieval.
- Course, chapter, and knowledge-point organization.
- Question generation, evidence inspection, review, editing, and export.
- Practice sets, attempts, wrong-question review, and mastery summaries.
- Password authentication, roles, and account administration.
- Memory and MyBatis repositories; mock and OpenAI-compatible provider adapters.

## Out of Scope Unless a Task Adds It

- Autonomous publishing of unreviewed generated questions.
- Default tests that spend money or depend on external model availability.
- Treating the multi-agent workflow display as proof of generation quality.
- Replacing product evidence with prompts or chat history that are not versioned in the repository.

## Sources of Truth

- This file: stable product intent and invariants.
- `docs/ARCHITECTURE.md`: implemented boundaries and target evolution.
- API code and automated tests: executable behavior.
- `毕业设计_系统设计方案.md`: detailed design narrative; update it when the implemented design materially changes.
- A task file under `docs/tasks/`: acceptance criteria for one change, not permanent architecture.

## Graduation-design Deliverables

The system and the thesis are co-products. `thesis/` owns student metadata, template provenance, the paper outline, experiments, and the claim-to-evidence matrix. Formal writing may summarize code and results, but it must not claim behavior that is only planned or represented by a UI label.
