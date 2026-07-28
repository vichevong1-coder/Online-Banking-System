# Git Workflow

How we branch, commit, and merge for this project. Solo dev, no `develop`/`release` branches — just `main` plus short-lived branches per user story.

## The two kinds of branches

- **`main`** — always stable, always demoable. Never commit to it directly except for the one-time repo bootstrap below.
- **Story branches** — one per user story (e.g. `us-001-backend-setup`). Created off `main`, merged back into `main`, then deleted. They're temporary by design — think of them as a workbench you clear off once the work is put away.

Deleting a branch after merging is safe: merging already copied its commits into `main`, so nothing is lost. Deleting just removes the now-unneeded label so old branches don't pile up.

## One-time setup (repo bootstrap)

Files that aren't tied to a single user story — `.gitignore`, `README.md`, `.github/` (CI + templates) — go straight onto `main` once, before any story branches exist:

```bash
# from repo root
git branch -m master main          # rename local branch: master -> main
git add .gitignore README.md .github docs
git commit -m "chore: initial repo scaffold (README, gitignore, CI workflows, sprint docs)"
git push -u origin main
```

## The flow for every user story

1. **Branch off `main`.**
   `git checkout -b us-001-backend-setup main`
   This creates the new branch *and* switches you onto it in one step — you're no longer on `main`, so nothing you do next touches it.

2. **Make your changes, staying inside one layer's folder** (`backend/`, `web-admin/`, or `mobile/`) so the commit only touches that directory. This keeps the path-filtered CI workflows meaningful — a backend-only change should only trigger `backend-ci.yml`.
   ```bash
   cd backend
   git add .
   git commit -m "feat(backend): US-001 initial Spring Boot project setup"
   cd ..
   ```

3. **Push the branch** (not `main`):
   ```bash
   git push -u origin us-001-backend-setup
   ```

4. **Open a Pull Request** on GitHub, filling in the template (user story ID, layer, test plan). This lets the relevant CI workflow run and gives you a record of "this PR = this user story."

5. **Merge the PR into `main`** once CI passes. Prefer squash-merge so `main`'s history has one clean commit per story instead of a pile of WIP commits.

6. **Delete the branch** (GitHub offers a one-click button right after merging). The code lives on in `main`'s history — the branch label was only scaffolding to get it there.

## Commit message format

```
<type>(<layer>): <US-ID> <short description>
```

- **type**: `feat`, `fix`, `chore`, or `test`
- **layer**: `backend`, `web-admin`, or `mobile`
- **US-ID**: the user story this commit belongs to, e.g. `US-004`

Examples:
```
feat(backend): US-004 add KYC document upload endpoint
fix(mobile): US-006 handle expired token on login retry
test(backend): US-001 add health check endpoint test
```

The branch name and the commit message don't need to match — the branch is just where you're working, the commit message describes what actually changed.

## Quick reference: repeating this for US-002 and US-003

Same pattern, different folder each time:

```bash
git checkout -b us-002-web-admin-setup main
cd web-admin
git add .
git commit -m "feat(web-admin): US-002 initial React admin portal setup"
cd ..
git push -u origin us-002-web-admin-setup
# open PR -> merge -> delete branch

git checkout -b us-003-mobile-setup main
cd mobile
git add .
git commit -m "feat(mobile): US-003 initial Flutter app setup"
cd ..
git push -u origin us-003-mobile-setup
# open PR -> merge -> delete branch
```

Always branch off `main` fresh each time (`git checkout -b ... main`), not off whatever branch you happened to be on last — this keeps story branches independent of each other.
