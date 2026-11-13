# Repository Instructions

## OpenSpec isolation

- Keep `/openspec/` in the repository-local exclude file returned by `git rev-parse --git-path info/exclude`. Unlike `.gitignore`, this rule survives switching to older branches; ensure it is present before switching branches. Local exclude rules are not included in commits and must be set up per clone.
- Ignore rules do not affect files already tracked by an older branch. Remove such files from that branch's index with `git rm --cached` while preserving the local files before including that branch in any integration.
- Never merge any file or change under `openspec/` into the `main` branch.
- Before merging into `main`, run `git diff --name-only main...HEAD -- openspec/` and require empty output.
- If a feature branch contains both implementation and `openspec/` changes, exclude the `openspec/` changes from the commits or history merged into `main`.

## Branch naming isolation

- Before creating any branch, read [Branch Naming Rule Document](dev-docs/branch-naming.md).
- Do not prefix repository branch names with `codex/`; branch names must start directly with the version/lineage pattern defined in the branch naming document.

## Main branch integration

- Integrate changes from any other branch into `main` only through a pull request; do not merge locally or push directly to `main` unless the user explicitly authorizes that specific exception.
- Release-preparation changes limited to Gradle app version fields, release/update metadata (including `update/manifest.json` and its generated `update/stable.json`), and `.changelog` files may be committed and pushed directly on `main` without a pull request. This exception does not cover application behavior, build logic, or release workflow changes.
- Treat the PR as a checkpoint. Its description must record the problem and root cause, implementation and design decisions, user-visible behavior, verification evidence, and known risks, limits, or rollback notes.
- Before opening or merging the PR, enforce the OpenSpec isolation guard above. Merge only after required checks pass, and prefer a merge commit to preserve branch lineage unless the user requests another strategy.

## Subagent worktree cleanup

- After the entire task is complete, archive or remove worktrees created for its subagents only after commits are pushed, PRs are recorded, and needed QA artifacts are preserved outside those worktrees. Do not remove active or unrelated worktrees, or lose uncommitted work.

## Test isolation (fork deviation)

- Upstream `main` is production-only: it archives all tests on `archived-tests` and enforces that policy with `.github/scripts/checks/validate_test_isolation.py` plus a `Test isolation` workflow.
- **This fork deliberately keeps a small test suite** covering its own features (pan-cloud backup/sync, forum novel packages, TXT export, update mirrors, backup policy). Do not delete those tests when syncing from upstream, and do not restore upstream's archived tests wholesale — they target APIs that no longer exist.
- The upstream `test-isolation` workflow is removed in this fork because it would reject the retained tests. The validator script and `dev-docs/test-isolation.md` are kept only as upstream reference.
- When syncing, upstream test deletions should be accepted for upstream-origin tests (they are archived upstream); fork-owned tests must survive and be kept compiling.

## Commit granularity

- Prefer a small number of complete, independently reviewable commits, organized by logical outcome (for example, one product fix and one reusable validation-tooling/documentation change).
- Consolidate implementation adjustments, corrections to earlier edits, naming/path changes, and documentation revisions locally before committing or pushing. Do not create a commit for each small adjustment, investigation attempt, progress note, or intermediate state.
- Apply the same discipline to validation branches. Batch prepared CI fixes into a coherent change; use a rerun of the existing commit for transient infrastructure failures rather than making an empty or cosmetic commit to trigger CI.
- Before PR delivery, review the introduced commit history and consolidate temporary/fixup commits into the final logical changes while preserving unrelated work and test/OpenSpec isolation. Keep validation-only history out of the production PR.
- Do not create a separate commit solely to record this preference or routine validation status. Include policy/documentation updates with the relevant logical change; record run outcomes in PR descriptions and Actions artifacts.

## Script organization

- Keep `.github/scripts/` scripts in purpose-based subfolders; see [Repository scripts](.github/scripts/README.md). Place new scripts in the relevant folder rather than the scripts root.
- When moving scripts, update all callers and paths derived from the script location, including workflows, documentation, and IDE run configurations.

## Git identity guard

- Before every commit, run `python .github/scripts/checks/validate_git_name.py` from the project root.
- Continue with the commit only when the validator exits successfully. Do not duplicate its identity prompt; its repository-local saved choice is authoritative.
- The validator script is located at [`.github/scripts/checks/validate_git_name.py`](.github/scripts/checks/validate_git_name.py).

## Architecture & development conventions

### i18n (compile-time generated)

- All new UI text must use `i18n("繁體中文 source text")`; never hard-code user-visible strings.
- `i18n/i18n.properties` sets `failOnMissingTerm=true`: every `i18n(...)` source text MUST have a matching row in `i18n/glossary.csv` (columns `source,en,zh-tw,zh-cn`), otherwise `generateI18nResources` fails the build.
- Add stable translations to `i18n/glossary.csv`; verify with `./gradlew checkI18n`.

### Module boundaries & visibility (shared vs composeApp)

- `internal` declarations in `shared` are NOT visible to `composeApp`. Any type used across the module boundary (e.g. `PanCloudApiClient`, `PanCloudAccountRepository`, `PanCloudBackupStorageProvider`, and their DTOs) must be `public`.
- A `public` function signature must not expose an `internal` type ("public function exposes internal type"). Keep AppSync engine interfaces (`AppSyncJournalRemote`, load/publish result types, `LoadedAppSyncJournal`, etc.) `internal` and expose them only through `public` methods such as `AppSyncService.attachPanCloud(...)`.
- `internal` types may be used inside a `public` function body as long as they never appear in its signature.

### Dependency injection (CompositionLocal)

- Repositories are exposed via `compositionLocalOf` in `composeApp/.../AppRepository.kt` and provided in `MainActivity` / `MainViewController` inside `remember { ... }`.
- Break construction cycles with deferred injection rather than reordering: `AppSyncService` needs `PanCloudAccountRepository`, which needs `AppSettingsRepository`, which derives from `AppSyncService` — resolve via `AppSyncService.attachPanCloud(...)` called after construction.

### Backup & sync (cloud drive)

- Snapshot backup: `BackupRepository` + `BackupStorageProvider`. Both the local backend (`AndroidBackupStorageProvider` / `IOSBackupStorageProvider`) and the cloud backend (`PanCloudBackupStorageProvider`) implement the same interface; `BackupRepositoryImpl` is storage-agnostic.
- Incremental sync: AppSync engine (`engine/`) depends only on `AppSyncJournalRemote`. The forum backend is `YamiboAppSyncJournalRemote`, the cloud backend is `PanCloudJournalRemote`, selected at runtime by `SwitchableAppSyncJournalRemote` via the `appsync.backend` setting.
- Cloud drive API: see root `API.md` — JWT auth (access 15min / refresh 7d, 401 auto-refresh+retry), unified `{success,data,message,error}`, upload ≤10MB direct / >10MB chunked, folder create idempotent via 409.
- Cloud sync file mapping: `index.json`, `journal-<replicaKey>.json`, `checkpoint-<id>.json`. Single-writer journals (each device writes only its own file) avoid lost updates, since the drive has no ETag/CAS. Switching backend resets the installation to Unbound (re-Seed/Join).

### Settings & credentials

- Settings use the `SettingsRegistry` DSL (`enumSetting`/`stringSetting`/`boolSetting`/`intSetting`); the storage key is `"$prefix.$propertyName.lowercase()"`.
- Sensitive/device-local settings (e.g. cloud drive `refresh_token`, `pan_cloud_password`) must be added to the `BackupRepositoryImpl.shouldSkipSetting` blacklist so they are excluded from backups.
- Cloud `access_token` lives only in memory (`PanCloudApiClient`); `refresh_token` and password persist via `EncryptedSettingsStore` (Android Keystore) / `NSUserDefaults` (iOS, not yet encrypted).

### Testing

- This fork keeps a small test suite for its own features (pan-cloud, forum novel packages, TXT export, update mirrors, backup policy). Upstream archives its full suite on `archived-tests`; the fork intentionally deviates from upstream production-only test isolation.
- `commonTest` uses `ktor-client-mock` (`MockEngine`) + `kotlin.test` + `runBlocking`, with an in-memory `SettingsStore`.
- For cloud API tests the base URL includes `/api`, so assert `request.url.encodedPath` with the `/api` prefix (e.g. `/api/auth/register`, not `/auth/register`).
- Read a binary request body in `MockEngine` via `request.body` (do not cast to `ByteArrayContent`); respond to downloads with `respond(byteArray)` — `respond(String)` corrupts binary.

### Build & verification

- `./gradlew :shared:compileDebugKotlinAndroid --console=plain`
- `./gradlew :composeApp:compileDebugKotlinAndroid --console=plain`
- `./gradlew :shared:testDebugUnitTest --tests "<pattern>" --console=plain`
- `./gradlew checkI18n --console=plain`
