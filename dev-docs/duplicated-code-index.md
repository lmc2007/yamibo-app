# Duplicated Code Index

Baseline: current working tree on 2026-06-20. The tree already contained uncommitted feature work; this audit does not revert or normalize it.

## Method

- Primary signal: IntelliJ IDEA Ultimate 2026.1.3 `DuplicatedCode` inspection.
- Raw reports: `.tmp/qa/duplicated-code/idea-isolated-20260620/DuplicatedCode.json` and `DuplicatedCode_aggregate.json`.
- Scope: production Kotlin under `composeApp` and `shared` in `commonMain`, `androidMain`, and `iosMain`.
- Excluded: tests, `buildSrc`, generated/build output, SQL migrations, data files, and `YamiboIcons.kt`.
- Eligibility: at least 20 executable lines or at least three occurrences, followed by semantic review.
- `Extract` means an exact behavior-preserving extraction/delegation. `Intentional` is required parallel code. `Deferred` is below the value threshold or would require over-parameterization. `Blocked` overlaps active uncommitted work or cannot currently be proven equivalent.

The Android Studio 2025.3.3 headless runner could not initialize its inspection tools. The successful report came from the isolated IntelliJ IDEA 2026.1.3 runner; its Kotlin/Native forward-declaration warning did not prevent `DuplicatedCode.json` from being emitted.

## Index

| ID | Priority | Status | Kind | Size | Locations | Dirty | Decision / owner | Batch | Validation |
|---|---|---|---|---:|---|---|---|---|---|
| DC-001 | P2 | Blocked | Cross-file UI | 2x20 | `App.kt:328-347`; `AppUpdateScreen.kt:199-218` | Yes | Update prompt overlaps active `App.kt` work; do not migrate without isolating that change. | - | Baseline only |
| DC-002 | P3 | Deferred | Cross-file UI | 2x5 | `components/user/UserIdentityRow.kt:45-49`; `userspace/blog/components/UserLine.kt:27-31` | No | Below threshold; extraction would add a cosmetic-only abstraction. | - | Baseline only |
| DC-003 | P3 | Deferred | Cross-file workflow | 2x17 | `favorite/FavoriteActions.kt:142-156`; `history/ReadHistoryPage.kt:229-245` | No | Similar feedback flow but messages and refresh ownership differ. | - | Baseline only |
| DC-004 | P2 | Deferred | Cross-file workflow | 2x20 | `favorite/FavoriteActions.kt:177-196`; `history/ReadHistoryPage.kt:254-272` | No | Existing shared helper already owns one variant; merging requires message/control-flow parameterization beyond pure migration. | - | Baseline only |
| DC-005 | P3 | Deferred | Same-file mapping | 2x12 | `favorite/FavoriteActions.kt:372-383`; `favorite/FavoriteActions.kt:394-405` | No | Below threshold; different target variants. | - | Baseline only |
| DC-006 | P3 | Deferred | Cross-file screen setup | 2x7 | `favorite/FavoriteCategoryEditorScreen.kt:45-51`; `FavoriteCategoryManageScreen.kt:43-49` | No | Below threshold. | - | Baseline only |
| DC-007 | P3 | Deferred | Cross-file state | 2x10 | `FavoriteCategoryEditorScreen.kt:61-70`; `FavoriteCategoryManageScreen.kt:54-63` | No | Below threshold; screen-owned state. | - | Baseline only |
| DC-008 | P3 | Deferred | Cross-file effect | 2x14 | `FavoriteCategoryEditorScreen.kt:104-117`; `FavoriteCategoryManageScreen.kt:89-102` | No | Below threshold; extraction would obscure screen lifecycle. | - | Baseline only |
| DC-009 | P3 | Deferred | Cross-file dialog | 2x10 | `FavoriteCategoryEditorScreen.kt:310-319`; `FavoriteCategoryManageScreen.kt:184-193` | No | Below threshold. | - | Baseline only |
| DC-010 | P3 | Deferred | Cross-file declarations | 4x5 | `FavoritePage.kt:155-158`; `ReadHistoryPage.kt:87-90`; `NovelThreadDetailScreen.kt:86-89`; `ImagesReaderScreen.kt:203-207` | Mixed | State declarations are not a reusable behavior; two locations are dirty. | - | Baseline only |
| DC-011 | P3 | Deferred | Same-file UI | 2x6 | `FavoritePageCards.kt:151-156`; `FavoritePageCards.kt:250-255` | Yes | Below threshold and overlaps active card changes. | - | Baseline only |
| DC-012 | P3 | Deferred | Same-file UI | 2x9 | `FavoritePageCards.kt:171-179`; `FavoritePageCards.kt:230-238` | Yes | Below threshold and overlaps active card changes. | - | Baseline only |
| DC-013 | P2 | Blocked | Cross-file UI | 3x10 | `FavoritePageCards.kt:344-353`; `ReadHistoryCard.kt:262-271`; `MessageCenterMainContent.kt:384-393` | Yes | All three locations contain active feature edits. | - | Baseline only |
| DC-014 | P2 | Resolved | Same-file adapters | 3x17 | `favorite/components/FavoritePageScrollbars.kt:43-58`; `:65-80`; `:87-103` | No | Delegated the three state adapters to one private scrollbar-state renderer. | 1 | Compile passed; fresh IntelliJ group removed |
| DC-015 | P3 | Deferred | Cross-file formatting | 2x17 | `FavoriteSyncProgressScreen.kt:429-445`; `history/components/ReadHistoryUtils.kt:38-54` | No | Below threshold; feature ownership differs. | - | Baseline only |
| DC-016 | P3 | Deferred | Cross-file formatting | 2x4 | `FavoriteSyncProgressScreen.kt:446-449`; `ReadHistoryUtils.kt:59-62` | No | Below threshold. | - | Baseline only |
| DC-017 | P3 | Deferred | Same-file coroutine flow | 2x18 | `favorite/sync/FavoriteSyncRunner.kt:51-68`; `:75-92` | No | Just below threshold; separate sync directions. | - | Baseline only |
| DC-018 | P3 | Deferred | Same-file runner | 2x9 | `favorite/updates/FavoriteUpdateRunner.kt:49-57`; `:62-70` | No | Below threshold. | - | Baseline only |
| DC-019 | P3 | Deferred | Same-file UI | 2x16 | `forum/ForumPageScreen.kt:338-353`; `:361-376` | No | Below threshold. | - | Baseline only |
| DC-020 | P2 | Resolved | Cross-file error UI | 3x14 | `ForumPageScreen.kt:670-683`; `HomePageScreen.kt:586-599`; `ThreadLoadingError.kt:121-134` | No | Extracted the full error-card structure with explicit style values; each caller retains its colors and padding. | 2 | Compile/build passed; fresh IntelliJ group removed |
| DC-021 | P3 | Deferred | Cross-file declarations | 2x6 | `ReadHistoryPage.kt:85-90`; `NovelThreadDetailScreen.kt:84-89` | Mixed | Subset of DC-010. | - | Baseline only |
| DC-022 | P3 | Deferred | Cross-file declarations | 2x5 | `ReadHistoryPage.kt:86-90`; `NovelThreadDetailScreen.kt:85-89` | Mixed | Subset of DC-010. | - | Baseline only |
| DC-023 | P2 | Blocked | Cross-file card UI | 2x29 | `ReadHistoryCard.kt:228-256`; `TagMangaHistoryCard.kt:163-191` | Yes | Both cards contain active cover-resolution changes. | - | Baseline only |
| DC-024 | P3 | Deferred | Cross-file top bar | 2x15 | `ReadHistoryTopBars.kt:112-126`; `MessageCenterScreen.kt:1106-1120` | No | Below threshold and different navigation ownership. | - | Baseline only |
| DC-025 | P3 | Deferred | Cross-file tabs | 2x15 | `MessageCenterScreen.kt:503-517`; `UserSpaceScreen.kt:544-558` | No | Below threshold; tab label contracts differ. | - | Baseline only |
| DC-026 | P2 | Resolved | Cross-file tab row | 2x22 | `MessageCenterScreen.kt:581-602`; `UserSpaceScreen.kt:596-617` | No | Moved the exact tab-row shell to shared navigation components and delegated both screens. | 1 | Compile passed; fresh IntelliJ group removed; emulator message tabs passed |
| DC-027 | P3 | Deferred | Same-file tab content | 2x9 | `MessageCenterScreen.kt:635-643`; `:664-672` | No | Below threshold. | - | Baseline only |
| DC-028 | P3 | Deferred | Same-file tab content | 2x9 | `MessageCenterScreen.kt:649-657`; `:678-686` | No | Below threshold. | - | Baseline only |
| DC-029 | P2 | Deferred | Same-file generic list | 2x23 | `MessageCenterScreen.kt:782-804`; `:807-829` | No | Different key/id/model types; generic extraction would add mapping callbacks and reduce clarity. | - | Baseline only |
| DC-030 | P2 | Resolved | Same-file action row | 2x31 | `profile/about/AboutScreen.kt:192-222`; `:231-261` | No | Extracted the shared row layout with an icon content slot and retained both overloads. | 1 | Compile passed; fresh IntelliJ group removed; emulator About screen passed |
| DC-031 | P3 | Deferred | Same-file settings UI | 2x14 | `SettingsCategoryScreen.kt:473-486`; `:518-531` | Yes | Below threshold and overlaps active settings work. | - | Baseline only |
| DC-032 | P2 | Resolved | Same-file actions | 2x21 | `profile/sign/SignInfoScreen.kt:196-216`; `:231-251` | No | Extracted the exact WebView/refresh button pair to one feature-local composable. | 2 | Compile/build passed; fresh IntelliJ group removed; emulator Sign screen passed |
| DC-033 | P3 | Deferred | Cross-file screen setup | 2x7 | `NovelThreadDetailScreen.kt:55-61`; `TagDetailScreen.kt:61-67` | Yes | Below threshold and both screens are dirty. | - | Baseline only |
| DC-034 | P3 | Deferred | Cross-file effects | 2x13 | `NovelThreadDetailScreen.kt:72-84`; `TagDetailScreen.kt:77-89` | Yes | Below threshold and lifecycle keys differ. | - | Baseline only |
| DC-035 | P3 | Deferred | Cross-file effects | 2x12 | `NovelThreadDetailScreen.kt:73-84`; `TagDetailScreen.kt:78-89` | Yes | Subset of DC-034. | - | Baseline only |
| DC-036 | P3 | Deferred | Cross-file effects | 2x6 | `NovelThreadDetailScreen.kt:88-93`; `TagDetailScreen.kt:90-95` | Yes | Below threshold. | - | Baseline only |
| DC-037 | P3 | Deferred | Cross-file effects | 2x4 | `NovelThreadDetailScreen.kt:90-93`; `TagDetailScreen.kt:92-95` | Yes | Subset of DC-036. | - | Baseline only |
| DC-038 | P2 | Blocked | Cross-file header UI | 2x28 | `novel/components/ThreadHeader.kt:275-302`; `tag/components/TagDetailHeaderCard.kt:172-199` | Yes | Both headers contain active content-cover work. | - | Baseline only |
| DC-039 | P3 | Deferred | Cross-file header UI | 2x16 | `ThreadHeader.kt:323-338`; `TagDetailHeaderCard.kt:220-235` | Yes | Below threshold and overlaps active work. | - | Baseline only |
| DC-040 | P2 | Resolved | Same-file menu actions | 2x40 | `thread/image/ImageContextMenu.kt:154-193`; `:203-242` | No | Extracted the identical menu-item sequence while retaining each parent row's layout. | 2 | Compile/build passed; fresh IntelliJ group removed |
| DC-041 | P3 | Deferred | Same-file reader setup | 2x10 | `thread/reader/CommentReaderScreen.kt:100-107`; `:501-510` | No | Below threshold. | - | Baseline only |
| DC-042 | P3 | Deferred | Same-file list rendering | 2x17 | `CommentReaderScreen.kt:296-312`; `:468-484` | No | Below threshold; list contexts differ. | - | Baseline only |
| DC-043 | P3 | Deferred | Cross-file catalog UI | 2x18 | `ReaderCatalogPanel.kt:155-172`; `TagMangaReaderComponents.kt:207-224` | No | Below threshold; different catalog models. | - | Baseline only |
| DC-044 | P3 | Deferred | Cross-layer parsing | 2x7 | `HtmlRenderer.kt:166-172`; `DefaultInAppLinkNavigationRepository.kt:348-354` | No | Accidental token similarity across UI and repository layers. | - | Baseline only |
| DC-045 | P3 | Deferred | Same-file SQL mapping | 2x5 | `LocalFavoriteRepositoryImpl.kt:210-214`; `:278-282` | Yes | Below threshold and active repository work. | - | Baseline only |
| DC-046 | P3 | Deferred | Same-file SQL mapping | 2x9 | `LocalFavoriteRepositoryImpl.kt:221-229`; `:290-298` | Yes | Below threshold and active repository work. | - | Baseline only |
| MAN-001 | P2 | Intentional | Platform repository | Large | `AndroidReadHistoryRepository.kt`; `IOSReadHistoryRepository.kt` | No | User decision: index platform-parallel implementations but do not move them to `commonMain` in this pass. | - | Manual review |
| MAN-002 | P2 | Blocked | Suppressed reader duplication | Unknown | `ImagesReaderScreen.kt:88` | Yes | File-level `@Suppress("DuplicatedCode")` hides the exact IDE groups and the file has active reader changes. Preserve suppression this pass. | - | Manual review |

## Batch Results

| Batch | Groups | Result | Checks |
|---|---|---|---|
| 1 | DC-014, DC-026, DC-030 | Passed | `compileDebugKotlinAndroid`; fresh IntelliJ: 46 -> 43 groups; emulator message/About smoke |
| 2 | DC-020, DC-032, DC-040 | Passed | `compileDebugKotlinAndroid`; fresh IntelliJ: 43 -> 40 groups; full `build`; emulator Sign smoke |

## Final Validation

- `./gradlew build --console=plain`: passed (313 tasks; existing KLIB duplicate-name and iOS Elvis warnings remain).
- IntelliJ IDEA 2026.1.3 fresh-cache reports: six targeted high-value groups removed; 46 filtered baseline groups reduced to 40.
- Emulator (`emulator-5554`): app launch, Message Center tabs, About action rows, and Sign action buttons rendered; crash buffer empty.
- QA artifacts: `.tmp/qa/duplicated-code/final/`.
- `git diff --check`: the only reported issue is a pre-existing extra EOF blank line in `update/changelogs/3.changelog`, outside this task's edits.
