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

Web extensions shipped with the app (one `content/index.html` each; offered under
« Extensions fournies » in the import screen, never installed automatically):

- `focus-flow/` — Pomodoro, statistics, challenges and background radio.
- `ma-voiture/` — car maintenance, fuel and reminders.
- `medicaments/` — drug summaries with Gemini (reimbursement in Algeria).
- `mes-finances/` — accounts, goals, debts, CCP SMS (the `sms` permission is
  simply ignored in the build without SMS).
- `mes-plantes/` — plants and watering reminders.
- `pptx-patients/` — PowerPoint → patient records with Gemini (permission `patients`).

To ship a new version of one of them: replace its `content/index.html` and the
`version` in its `manifest.json` (the same values as in the `.OTips` built by the
extensions chat — `BundledExtensionsTest` checks it still packs and validates).
Installed copies then show « Mise à jour disponible » in the import screen.
