# Backend Agent Guide

## Local Map

- `controller/`: HTTP translation and validation only.
- `service/`: use-case interfaces.
- `serviceimpl/`: orchestration and business behavior.
- `repository/`: persistence interfaces and memory/MyBatis implementations.
- `domain/`: records and enums; keep framework dependencies out where practical.
- `ai/`, `vector/`, `parser/`: external capability boundaries and adapters.
- `config/`, `security/`, `exception/`: cross-cutting runtime concerns.
- `src/main/resources/mapper/`: MyBatis SQL mappings.

## Boundary Rules

- Allowed request flow: controller -> service -> repository/AI/vector/parser adapter.
- Controllers must not import `repository`, `mapper`, `serviceimpl`, or `vector` packages.
- Provider selection belongs in configuration; business services depend on interfaces.
- Keep API records explicit. Avoid returning persistence objects from controllers.
- Preserve both memory and MyBatis modes when changing persistence behavior.
- Time, IDs, and provider responses should be injectable or controllable in tests.

## Tests

- Mirror the production package under `src/test/java`.
- Prefer deterministic unit tests for services and adapters.
- Add integration tests only where Spring wiring, SQL mappings, or HTTP contracts are the behavior under test.
- Never require a real API key, MySQL, or Qdrant for the default test suite.

## Commands

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd package
```

Run the repository-level `scripts/verify.ps1` before handoff.
