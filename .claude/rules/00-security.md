# Security Rules

## Secrets
- Never commit API keys, tokens, or credentials to source control
- All secrets go in `.env.local` or `.env` (gitignored) — reference `.env.example` for the template
- In CI, use GitHub Actions secrets or platform environment variables
- If you see a secret in staged files, **stop and alert** before committing

## Input Validation
- Validate and sanitize all external input (API routes, CLI args, form data)
- Use parameterized queries for database access — never string interpolation
- Sanitize email headers to prevent injection

## Dependencies
- Run security audit before merging PRs (`npm audit` / `pip audit` / `uv pip audit`)
- Pin major versions; allow minor/patch updates via `^` or `~=`
- Review dependency changelogs before upgrading major versions

## Safe Logging
- Never log secrets, tokens, passwords, or personal data
- Log request metadata (path, status, timing) not content
- In production, use structured JSON logging
