# Development Workflow

## Branch strategy

- `main`: stable, reviewed code.
- `develop`: integration branch once implementation begins.
- Feature branches: `feature/<short-scope>`.
- Fix branches: `fix/<short-scope>`.
- Documentation branches: `docs/<short-scope>`.

For a solo workflow, keep branches lightweight. Do not create many empty branches in advance. Create each branch from the current intended base when a phase begins.

## Commit convention

Use Conventional Commit-style messages:
- `docs: ...`
- `chore: ...`
- `feat: ...`
- `fix: ...`
- `test: ...`
- `refactor: ...`

Keep commits focused and reviewable.

## Antigravity operating instructions

At the start of each task, instruct Antigravity to:
1. Read README and all relevant docs, especially PROJECT_CONSTITUTION.md.
2. Inspect the current Git status, branch, tree, package manifests and existing implementation.
3. Summarize current state and identify conflicts before editing.
4. Work only on the requested phase and approved files.
5. Avoid unrelated refactoring or dependency upgrades.
6. Preserve existing working behavior.
7. Add/adjust tests for the change.
8. Run the actual relevant build/tests and report exact results.
9. Update docs and list changed files.
10. Never claim deployment, tests, integrations or manual checks occurred unless they did.

## Phase completion checklist

- Scope implemented.
- Backend and frontend contracts agree.
- Validation and authorization covered.
- Loading, empty, error and forbidden UI states covered.
- Tests pass (or failures explicitly recorded).
- No secrets or private data committed.
- Documentation updated.
- Diff reviewed.
- Commit created with meaningful message.
- Merge to main only after review.

## Initial project sequence

1. Review and approve docs/architecture.
2. Finalize ER diagram and physical MySQL schema.
3. Decide versions/build tools and environment variables.
4. Scaffold React and Spring Boot without business features.
5. Verify both applications start.
6. Implement authentication and security.
7. Implement marketplace and inventory.
8. Implement booking/payment workflows.
9. Implement contribution agreements and partner operations.
10. Implement admin, support and production hardening.
