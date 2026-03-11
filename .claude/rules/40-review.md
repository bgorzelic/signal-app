# Review & PR Standards

## Pre-PR Checklist
1. Lint passes with zero warnings
2. Type checking passes (if applicable)
3. All tests pass
4. Build succeeds
5. No secrets in staged files
6. Commit messages follow conventional format

## Commit Messages
Format: `type(scope): short description`

Types: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `build`, `ci`, `perf`

- Present tense, imperative mood: "Add user auth" not "Added user auth"
- Keep subject line under 72 characters
- Body for complex changes explaining the "why"

## PR Standards
- Keep PRs focused — one feature/fix per PR
- Title: short summary under 70 characters
- Description: what changed, why, and how to test
- Link related issues if applicable

## Code Review Focus
- Correctness: does it do what it claims? Edge cases handled?
- Security: no secrets exposed, input validated
- Performance: no unnecessary computation or re-renders
- Types: correct and specific (no `any` leaks)
- Backwards compatibility: no breaking changes without deprecation

## Merge Rules
- CI must pass before merge
- Self-review diff before requesting review
- Address all review comments before merging
