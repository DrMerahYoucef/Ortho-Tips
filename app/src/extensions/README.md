# Bundled extension sources

Each folder here is the source of one extension shipped with the app:

    <id>/manifest.json   the package manifest (see ExtensionManifest)
    <id>/content/        the files packed into the .OTips payload

Nothing here goes into the APK as-is. On every build, `packBundledExtensions`
(buildSrc) packs each folder into `bundled_extensions/<id>.OTips` with the
app's own `OTipsWriter`, and only those packages are added to the assets.
Editing a source is enough: the next `assembleDebug` repacks it.

`BundledExtensionsTest` checks that every source packs into a package its
handler accepts (for classifications: every chapter file and figure exists).

- `operative-guide/` — the guide itself; its content (`bones_database.zip`)
  is the user's own data, imported after installing, never bundled.
- `classifications/` — index, chapter files and figures of the reference.
