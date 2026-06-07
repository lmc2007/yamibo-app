# Update Workflow Guide

This directory stores the source update manifest, the published update feed contract, and per-release changelogs for the app updater.

## Files

- `manifest.json`: the manually maintained source manifest. Edit this file only.
- `stable.json`: generated from `manifest.json` by `./gradlew syncStableManifest` to avoid manual sync mistakes.
- `changelogs/{versionCode}.changelog`: the GitHub Release body for a specific release, for example `changelogs/1.changelog`.

The source repository versions of `manifest.json` and `stable.json` must always keep:

```json
{
  "isReady": false,
  "assets": []
}
```

The source manifest must not contain `releaseUrl`. The ready manifest consumed by app clients is generated temporarily by the workflow only after APK build, signing, and upload complete. It is not committed back to `main`.

## Release Android APK Workflow

Workflow file:

- `.github/workflows/release.yml`

Trigger:

- Push any tag.
- The tag name must equal `update/manifest.json` `versionCode`.
- Example: if `versionCode = 1`, push tag `1`.

Manual preparation before release:

1. Update `yamiboAppVersionCode` and `yamiboAppVersionName` in `composeApp/build.gradle.kts`.
2. Update `update/manifest.json`, keeping `isReady=false`, `assets=[]`, and no `releaseUrl`.
3. Add or update `update/changelogs/{versionCode}.changelog`.
4. Run locally:

```powershell
.\gradlew syncStableManifest validateUpdateManifest --console=plain
```

Trigger the release:

```powershell
git tag 1
git push origin 1
```

Workflow steps:

1. Check out the tag.
2. Verify that the tag name equals `manifest.versionCode` and that the changelog exists.
3. Run `syncStableManifest validateUpdateManifest`, requiring the source manifest to remain `isReady=false`.
4. Build the release APK.
5. Zipalign, sign, and verify the APK with `mine.keystore`.
6. Create or update the GitHub Release.
7. Upload the APK asset.
8. Calculate the APK `sha256` and `size`.
9. Generate a published update folder in runner temp:
   - `isReady=true`
   - `releaseUrl`
   - APK asset `url`, `sha256`, and `size`
   - identical `manifest.json` and `stable.json`
10. Run `validatePublishedUpdateManifest`.
11. Force push the published `update` folder to the GitHub `update-release` branch.
12. Force push the published `update` folder to the Gitee and GitCode mirror repositories.
13. Upload the same APK to the Gitee and GitCode release assets.

Client update source order:

1. GitHub `update-release` branch: `update/stable.json`
2. Gitee mirror repo: `update/stable.json`
3. GitCode mirror repo: `update/stable.json`

## Sync Update Folder To Mirrors Workflow

Workflow file:

- `.github/workflows/sync-update-mirrors.yml`

Trigger:

- Run `Sync Update Folder To Mirrors` manually from the GitHub Actions page.

Input:

- `use_latest_release_asset=false`: default. Syncs the source `update/manifest.json` and `stable.json`, meaning `isReady=false`. Use this to pause public updates or reset the public feed to a not-ready state.
- `use_latest_release_asset=true`: reads the GitHub Release APK matching the current `manifest.versionCode`, regenerates an `isReady=true` published manifest, and syncs it to the GitHub `update-release` branch plus Gitee and GitCode mirrors.

Default sync flow:

1. Check out the source repo.
2. Run `syncStableManifest validateUpdateManifest`.
3. If `stable.json` was regenerated, commit it back to the source repo.
4. Copy source `update/manifest.json` and `stable.json` into runner temp.
5. Force push the temp `update` folder to the GitHub `update-release` branch.
6. Force push the temp `update` folder to the Gitee and GitCode mirror repositories.

`use_latest_release_asset=true` flow:

1. Check out the source repo.
2. Run `syncStableManifest validateUpdateManifest`.
3. Read `manifest.versionCode`, `versionName`, and `channel`.
4. Download the matching APK from GitHub Releases:

```text
yamibo-{channel}-v{versionName}.apk
```

5. Calculate the APK `sha256` and `size`.
6. Generate an `isReady=true` published manifest.
7. Run `validatePublishedUpdateManifest`.
8. Force push the ready `update` folder to the GitHub `update-release` branch.
9. Force push the ready `update` folder to the Gitee and GitCode mirror repositories.

## Which Workflow To Use

- Normal release: push a tag and use `Release Android APK`.
- Update feed did not sync correctly after a release: run `Sync Update Folder To Mirrors` manually with `use_latest_release_asset=true`.
- A released APK has a problem and app-side update prompts should be paused: run `Sync Update Folder To Mirrors` manually with `use_latest_release_asset=false`.

## Release Safety Rules

- Do not manually set source `manifest.json` or `stable.json` to `isReady=true`.
- Do not manually add APK assets to the source manifest.
- Do not manually add `releaseUrl` to the source manifest.
- `isReady=true` should exist only in the published update folder on the GitHub `update-release` branch and the Gitee/GitCode mirror repositories.
- The app client shows an available update and downloads the APK only when it reads `isReady=true` and the remote version is newer.
