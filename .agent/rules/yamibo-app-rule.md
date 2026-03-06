---
trigger: model_decision
description: when you are coding yamibo app
---

# Yamibo App Code Style and Development Rules

## UI/UX Style Guidelines
- **Yaml/Kotlin Indent**: Always use 4 spaces for indentation. Never use tabs or 2 spaces.
- **Color Palette**: Always use the centralized `YamiboTheme` object (located in `theme/YamiboTheme.kt`) for all colors. NEVER define `private object YamiboColors` locally in individual files. Access colors via `YamiboTheme.colors.xxx`.
- **Component Styling**:
  - Prefer rounded corners for cards and chips (e.g., `RoundedCornerShape(24.dp)` or `16.dp`).
  - Use `AnimatedYamiboButton` and `AnimatedYamiboChip` for interactive elements to ensure consistent hover, press, and scale animations.
  - Implement smooth fade/scale `AnimatedContent` for state transitions in Composables.

## Architecture and Data Management
- **Store Layer**: Use `UserStore`, `CookieStore` to handle raw persistence (EncryptedSharedPreferences format for Android, etc). Keep them isolated from business logic.
- **Repository Layer**: Combine Store caches with API data (`YamiboClient`). Handle mappings like UserStore DTO vs `ProfilePage` logic here and provide clean `IResult` sealed interfaces for views.
- **API Communication**: Exclusively use `io.github.littlesurvival.YamiboClient` and its domain routes/DTOs. Do not implement ad-hoc HTTP requests in the UI or app layer unless migrating existing legacy calls.
- **CompositionLocals**: Always provide Repositories globally via `CompositionLocalProvider` (e.g., `LocalAuthRepository`, `LocalForumRepository`) instead of passing them down through long parameter chains in UI.

## Code formatting
- Maintain existing whitespace formats: space after commas, brackets consistently matched.
- Use explicit types only when necessary or ambiguous, let Kotlin infer standard cases.
- Leverage Ktor plugins (ContentNegotiation, logging) and Compose Navigation `Navigatable` standard structures.
- Comment headers should be simple: `/**  Description  */` instead of fancy decorative lines like `/** ───── Description ───── */`.
- Group variables thoughtfully with simple `/** */` bloc header comments when organizing complex files.
- Keep composable files focused: split large screens into `components/` sub-files if a single file exceeds ~300 lines.

## External Libraries
- **yamibo-api Library**: The source code is located at `C:\Users\allen\OneDrive\Desktop\Projects\kotlin\kotlin-libs\yamibo-api\library\src\commonMain\kotlin\`. This is the authoritative source for `io.github.littlesurvival.*` imports.

## AI Agent Workflow Rules
- **Clean Up**: NEVER leave debug log txt files (like `build_log.txt`, `compile_test.txt`) that you generate. Always remove them after you finish consuming them.
- **yamibo-api Reference**: Whenever the yamibo-api library is mentioned or relevant DTOs/routes/clients are needed, always review the corresponding source code under the yamibo-api library path before implementing. Do not guess API shapes — read the actual source files.
- **R.jar Lock Fix**: When encountering the `processDebugResources` R.jar file lock error on Windows/OneDrive, run the following PowerShell script to kill the locking process before retrying the build:

```powershell
$jar = Get-ChildItem -Path . -Recurse -Filter R.jar | Where-Object {
    $_.FullName -like "*compile_and_runtime_not_namespaced_r_class_jar*"
} | Select-Object -First 1

if ($jar -eq $null) {
    Write-Host "R.jar not found"
    exit
}

$handleOutput = & "C:\tools\handle.exe" -accepteula $jar.FullName

$pid = ($handleOutput | Select-String -Pattern "pid: (\d+)" -AllMatches).Matches.Value |
    ForEach-Object { ($_ -replace "pid: ","") } |
    Select-Object -First 1

if ($pid) {
    Write-Host "Killing PID $pid locking $($jar.Name)"
    Stop-Process -Id $pid -Force
} else {
    Write-Host "No locking process found"
}
```

- **Non-R.jar Lock Fallback**: If a build fails due to a file lock (`FileSystemException`) but it's NOT R.jar (e.g., `classes.jar` or other build artifacts), just run:

```powershell
taskkill /im java.exe /f
```
