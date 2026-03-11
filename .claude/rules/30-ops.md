# Operations & Environment

## Runtime Versions
- Node.js version pinned in `.nvmrc` — run `nvm use` before development
- Python version pinned in `.python-version` — run `pyenv install` if needed
- Always match CI runtime version to the pinned version

## Setup
- Clone → install dependencies → copy env template → run
- Environment template: `.env.example` → `.env.local`
- Document all required env vars with descriptions in `.env.example`

## Build & Deploy
- Build command must work with only required env vars set
- Deploy target documented in this file or project README
- All deploy-time configuration via environment variables (not hardcoded)

## Package Management
- TypeScript: npm (standard) or pnpm (monorepo only) — lockfile committed
- Python: uv preferred, Poetry acceptable — lockfile committed
- Run `npm ci` (not `npm install`) in CI for deterministic builds

## Scripts
- Document all available npm/make/just scripts in README
- Destructive scripts require confirmation flags
- Generated artifacts documented — do not hand-edit
