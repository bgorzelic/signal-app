# Style & Coding Conventions

## General
- Use `const` / `let` (never `var`) for JavaScript/TypeScript
- Prefer immutable data structures where practical
- Explicit is better than implicit — name things clearly
- Keep functions focused — one responsibility per function
- Max function length: ~50 lines (extract if longer)

## TypeScript (when applicable)
- Strict mode (`strict: true`) — no `any` types; use `unknown` with type guards
- Explicit return types on exported functions
- Prefer `import type { Foo }` for type-only imports
- Use optional chaining (`?.`) and nullish coalescing (`??`)
- Use `@/*` path alias for imports when configured

## Python (when applicable)
- Follow PEP 8, enforced by Ruff
- Type hints on all public functions
- Use `pathlib.Path` over `os.path`
- Prefer f-strings over `.format()` or `%`
- Use `dataclasses` or `pydantic` for structured data

## Imports
- Group: stdlib → external packages → internal → relative
- Sort alphabetically within groups
- Use named imports; avoid wildcard `import *`

## Error Handling
- Handle promise rejections / exceptions explicitly
- Use custom error types for domain errors
- Log errors with context before re-throwing
- API routes: return appropriate HTTP status codes with safe error messages
