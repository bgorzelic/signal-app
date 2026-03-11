# Testing Rules

## Test Requirements
- All repos with executable code MUST have a test command that runs in CI
- New utility functions always get unit tests
- Bug fixes include a regression test that would have caught the bug
- Critical user journeys get E2E/integration smoke tests

## TypeScript (Vitest)
- Location: `src/**/__tests__/*.test.ts` or `tests/*.test.ts`
- Run: `npm test` (single run) or `npm run test:watch`
- Coverage: `npm run test:coverage` if configured
- Use descriptive test names: `it('should calculate total with tax')`
- Mock external dependencies; test logic in isolation

## Python (pytest)
- Location: `tests/` directory at project root
- Run: `pytest` or `uv run pytest`
- Use `pytest.fixture` for shared setup
- Use `pytest.mark.parametrize` for data-driven tests
- Coverage: `pytest --cov`

## CI Integration
- CI runs: lint → typecheck → test → build (in that order)
- Tests must pass before merge — no exceptions
- Flaky tests must be fixed or quarantined, never ignored
