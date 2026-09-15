# Telegram Drive integration for Ortho Tips

Extract this archive over the root of the Ortho Tips repository. Every path
mirrors the existing tree, so files either land as new sources or replace the
file they are meant to replace.

```
unzip ortho-tips-telegram-drive.zip -d /path/to/Ortho-Tips
```

---

## 1. What this adds

Telegram Drive is an **optional** second storage backend. Local storage keeps
working exactly as before and remains the default. Switching backends never
moves or deletes a byte — migration is a separate, explicit action.

| Area | Added |
| --- | --- |
| Storage | `StorageProvider` abstraction with local and Telegram implementations |
| Telegram | TDLib client ported from TeleDrive (auth, drive channel, transfers) |
| Security | Keystore-wrapped keys, chunked AES-256-GCM, SHA-256 verification |
| Persistence | Room v5 — three new tables, purely additive |
| Sync | Manifest pull, transfer planning, tombstones, whole-database snapshots |
| Background | WorkManager queue worker + periodic sync with constraint handling |
| UI | 11-step setup wizard, Telegram Drive settings section, diagnostics screen |
| Cutover | `PatientRepository` and the media read paths now go through the abstraction |
| Tests | JVM tests for the cipher and wire format; instrumented tests for v5 |

---

## 2. Files in this archive

### Modified (replace existing)

```
gradle/libs.versions.toml                                    TDLib, WorkManager, documentfile
settings.gradle.kts                                          JitPack, scoped to com.github.tdlibx
app/build.gradle.kts                                         new deps, jniLibs packaging, Room schema export
app/proguard-rules.pro                                       TDLib + serialization keep rules
app/src/main/AndroidManifest.xml                             network + notification permissions
app/src/main/java/com/example/MainActivity.kt                three new routes
app/src/main/java/com/example/OrthoApplication.kt            drive session restore on cold start
app/src/main/java/com/example/di/AppModule.kt                drive object graph
app/src/main/java/com/example/data/local/OrthoDatabase.kt    v5 + MIGRATION_4_5
app/src/main/java/com/example/navigation/OrthoNavigation.kt  three destinations
app/src/main/java/com/example/ui/screens/main/MainScreen.kt  one parameter, passed through
app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt   one "Cloud storage" row
app/src/main/java/com/example/data/repository/PatientRepository.kt    media routed through the gateway
app/src/main/java/com/example/viewmodel/ImageAnnotationViewModel.kt   resolve before editing pixels
app/src/main/java/com/example/viewmodel/PatientDataTransferViewModel.kt  prefetch before export
```

The five screen/activity/navigation files were patched programmatically from
your originals rather than retyped, so nothing outside the named change differs
by a single character.

### New

```
data/telegram/     TelegramModels, TelegramAuthState, TelegramClient,
                   TdLibTelegramClient, DriveLog
data/crypto/       KeyVault (KeystoreManager, WrappedKeyRepository, CryptoKeys,
                   KeystoreTdlibKeyProvider), StreamCrypto, Hashing
data/storage/      StorageModels, StorageProvider, LocalStorageProvider,
                   TelegramStorageProvider, StorageProviderRegistry,
                   PatientMediaGateway, DriveMediaInterceptor
data/sync/         DriveManifest + ManifestCodec, DatabaseSnapshot, SyncEngine
data/local/        entity/SyncEntities, dao/SyncDaos
data/prefs/        TelegramDriveRepository
work/              DriveWorkers (TransferQueueWorker, SyncWorker, SyncScheduler)
viewmodel/         TelegramDriveViewModels (setup + settings)
ui/screens/storage/ TelegramSetupWizardScreen, TelegramDriveSettingsScreen,
                   StorageDiagnosticsScreen
test/              DriveCryptoAndFormatTest
androidTest/       DriveSchemaTest
```

---

## 3. Decisions worth knowing about

**No Hilt.** Ortho Tips uses a hand-rolled `AppModule` with `by lazy`
properties, reached through `OrthoApplication`. TeleDrive is Hilt throughout.
Introducing Hilt would mean annotating every ViewModel, Activity and factory in
your app — a large change with real regression risk, for no benefit here. All
ported classes had their `@Inject`/`@Singleton` annotations removed and are
constructed in `AppModule` instead. Workers reach the graph through
`(applicationContext as OrthoApplication).appModule`.

**minSdk stays 24, Java stays 11.** TeleDrive is minSdk 26 / Java 17. Raising
either would drop existing users or churn the build for no gain; the TDLib AAR
and everything ported here run fine at 24.

**Compose stays on BOM 2024.09.00.** All new UI is written against Material 3
1.3 APIs only — no wavy progress indicators, no `ButtonGroup`, nothing newer.

**Local paths are unchanged.** `LocalStorageProvider` resolves every
`StorageKey` to the exact path `PatientMediaStorage` already produces
(`filesDir/patients/{id}/images/...`). Existing installs read and write the same
bytes in the same places; there is no migration step and no risk window.

**The drive channel has its own marker.** TeleDrive claims channels titled
`TeleDrive` with `#teledrive-storage`; this uses `Ortho Tips Drive` with
`#orthotips-drive`, so both apps can run on one account without adopting each
other's storage.

**Encryption is on by default, unlike TeleDrive.** Patient imaging is medical
data, so uploads are AES-256-GCM encrypted before leaving the device, remote
filenames are opaque hashes, and captions are encrypted too. The plaintext
SHA-256 travels inside the manifest so integrity is verifiable without
decrypting.

**Deletion goes through tombstones, never through absence.** "Not in the local
set" is indistinguishable from "not downloaded yet"; treating those the same is
how sync engines destroy data. `delete()` is always soft — bytes survive until
an explicit `purge()` past the retention window.

**Record sync uses a generic table snapshot.** `DatabaseSnapshot` walks
`sqlite_master` and cursor metadata rather than typed DAOs, so it covers every
table you have now and every table you add later, and cannot drift out of step
with the entity classes. Restore is `INSERT OR REPLACE` in one transaction:
rows present locally but absent from the snapshot are kept.

---

## 4. First run

1. Build and install. Nothing changes — the backend is `LOCAL`.
2. Settings → **Cloud storage → Telegram Drive** → *Run Telegram Drive setup*.
3. The wizard needs an API id and hash from <https://my.telegram.org> →
   *API development tools*. These are the user's own credentials; they are
   sealed with the Android Keystore before they touch disk and are never sent
   anywhere except Telegram.
4. The backend only switches to `TELEGRAM` at the initial-sync step. Backing out
   before that leaves everything as it was.

---

## 5. Before you ship

These are the things I could not verify from here, and they are the first places
to look if the build complains.

- **I could not compile this.** There is no Android SDK, Gradle or Google Maven
  access in the environment I wrote it in. Expect to resolve a small number of
  import or signature issues on the first build. The logic is sound; the
  mechanical details are unverified.
- **Check the TDLib artifact resolves.** `com.github.tdlibx:td:1.8.56` comes
  from JitPack. If your CI runs behind a proxy, whitelist `jitpack.io`.
- **APK size.** TDLib ships four ABIs of prebuilt `.so`, roughly 40 MB
  uncompressed. TeleDrive handles this with ABI splits; I deliberately did not
  add splits because they rename the output file and your `main.yml` uploads a
  single fixed artifact. Add them when you are ready to change the workflow.
- **`fallbackToDestructiveMigration(true)` is still in `OrthoDatabase`.** I left
  it exactly as it was, since removing it changes failure behaviour on existing
  installs. It does mean a schema mismatch wipes patient data — worth revisiting
  now that a drive backup exists to restore from.
- **Room schema export is now on.** After the first v5 build, `app/schemas/`
  will contain `5.json`. From that point a real `MigrationTestHelper` test can
  be written for v5 → v6. It cannot be written for 4 → 5 because no `4.json` was
  ever exported.

---

## 6. How the cutover works

`PatientMediaGateway` is the seam. `PatientRepository` gained an optional third
constructor parameter; when it is null — as in every existing unit test — the
repository behaves exactly as it did before, hard deletes included.

- **Saving.** `saveImage` and `saveDocument` queue an upload after the row is
  committed, never before, so a transfer cannot reference a record the database
  does not yet have. Images and thumbnails are queued separately: a second device
  needs thumbnails for the grid long before anyone opens an original.
- **Deleting.** `deleteImage`, `deleteDocument`, `deletePatient(s)` now soft-delete
  through the gateway — files move to a recoverable `.trash`, and a tombstone is
  recorded so the removal propagates instead of the drive resurrecting it on the
  next pull.
- **Reading.** `resolveImageFile`, `resolveThumbnailFile` and `resolveDocumentFile`
  return a readable `File`, fetching from the drive when the local copy is gone.
- **Display.** `DriveMediaInterceptor` is installed on the app-wide Coil loader via
  `OrthoApplication : ImageLoaderFactory`. Every existing `AsyncImage` call site
  renders drive-only images with no change to a single screen; the interception
  happens below them. The common path costs one `File.exists()` check.
- **Editing pixels.** `ImageAnnotationViewModel` resolves before rotating or
  cropping — mutating a missing file would destroy the image — and re-saves the
  row afterwards so the altered bytes take a new revision in the drive.
- **Exporting.** `PatientDataTransferViewModel` warms the cache before zipping,
  because the exporter skips files it cannot find and would otherwise write a
  silently incomplete archive.

---

## 7. Fixes after the first device run

Two bugs surfaced on the first real install, both at step 6 of the wizard.

**The wizard deadlocked on "Create the drive".** `canAdvance` gated that step on
`chatId != null`, but `chatId` is produced by the very work that `advance()`
runs — and `advance()` returned early when `canAdvance` was false. The step could
never start, so Continue stayed disabled forever with the previous step's status
text ("Checking code…") still on screen.

Fixed by separating the two concerns. `SetupStep.isAutomatic` marks the five
steps that do work rather than collect input; those now run from a
`LaunchedEffect(state.step)` as soon as they appear, record themselves in
`completedSteps`, and gate Continue on that instead of on their own output.
`advance()` is pure navigation for them. A failure now shows a **Try again**
button rather than a dead end, and `goTo()` is the single place that changes
step so stale status text cannot carry over.

**Drive discovery was quadratic.** `ensureStorageChat` retried `findStorageChat`
eight times, and each attempt fell through to walking both chat lists with one
`GetChat` per chat — up to sixteen thousand sequential requests on an account
with a normal number of chats. It would have looked like a hang even once the
deadlock was cleared, and would likely have tripped Telegram's flood limits.

Split into `searchStorageChat` (two title searches plus lookups for whatever
matched — cheap, and the one that is retried while the chat list syncs) and
`scanChatListsForStorageChat` (the expensive walk, run exactly once, and only as
a fallback for a drive channel that was renamed). Retries dropped from eight to
four now that each is cheap.

Also corrected: the connection-test step claimed to be "sending a test object"
when it only queried account limits. It now says what it does and reports the
signed-in account.

---

## 8. Not yet done

Honest list, so nothing here surprises you later.

- **`PatientZipImporter` writes through DAOs, not the repository.** Imported
  media therefore lands on disk without being queued directly; the next sync
  picks it up from the local scan, so it is correct but slower to reach the
  drive than a camera capture. Routing the importer through
  `repository.saveImage/saveDocument` would close the gap.
- **Foreground service for large transfers.** `TransferQueueWorker` runs as an
  ordinary worker. A 2 GB MRI series wants `setForeground` with a progress
  notification; the permissions are already in the manifest.
- **Thumbnail generation and progressive/streaming media.** TeleDrive's
  `ThumbnailStore` and `TelegramDataSource` were not ported — streaming needs
  media3, which Ortho Tips does not currently depend on.
- **Conflict resolution UI.** `ConflictPolicy.ASK` is implemented in the engine
  and reports conflicts in `SyncReport`; no screen surfaces them for a manual
  choice yet.
- **Key backup/restore.** TeleDrive's `KeyBackupCodec` lets a user carry the
  content key to a second device. Without it, an encrypted drive is readable
  only on the device that created it — the settings screen shows the key
  fingerprint so this is at least visible, but the export flow is not built.
- **Export/import of drive content to a ZIP**, and the reading-progress /
  recent-history / tags / collections categories from your brief, which do not
  exist as tables in the current schema.
