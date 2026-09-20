# Ortho Tips — Extension guide

This guide is for an AI or a developer asked to **write a new extension** for the
Ortho Tips Android app, or to **adapt an existing HTML program** so it runs well
inside it. Read it fully before writing code. Follow the rules marked **must**.

> **How to use this file.** Give it to the AI together with your request, for example:
> *"Using guide.md, make me an Ortho Tips extension that … . Output the HTML payload."*
> The app packages and encrypts that payload as a `.OTips` file. Import it from the
> **Extensions** tab → **Importer**.

---

## 1. What an extension is

An extension is **one self-contained HTML payload**: HTML, CSS and JavaScript in the
same file, no other files. On disk and during import/export it is an encrypted
`.OTips` container, not a readable `.html` file. The app lists it in the
**Extensions** tab and opens it full screen.

Where it runs:

| | |
|---|---|
| Engine | Android System WebView (Chromium, recent). Modern JS (ES2020+) and CSS are fine. |
| Screen | A phone in portrait, about **360–430 px wide**, under the app's top bar. |
| Network | **Blocked by default.** The file must work fully offline. |
| Origin | Its own private origin, `https://<id>.ext.orthotips.invalid/`. |
| Access to the app | Only through `window.OrthoTips` (section 4). It cannot see patients, the database or other extensions. |
| Data | Saved by the app, per extension. Deleting the extension erases its data. |

The same file must still **work when opened in a normal browser**. Detect the
app with `window.OrthoTips` and fall back to plain web APIs when it is absent.

---

## 2. File rules

1. **Must** be a single UTF-8 HTML payload, **15 MB maximum**. Everything is inline;
  the app's encrypted container is the only file users import or export: `.OTips`.
2. **Must** start with `<!DOCTYPE html>`, and have a `<head>` containing
   `<meta charset="utf-8">` and
   `<meta name="viewport" content="width=device-width, initial-scale=1">`.
3. **Must** contain the metadata block of section 3 near the top of `<head>`.
4. **No external resources**: no CDN scripts, no Google Fonts, no remote images.
   Use system fonts, inline SVG, and `data:` URIs for images. Libraries must be
   pasted inline. The only exception is section 3's `ortho-ext-network`.
5. **One page.** Build different screens as views inside the page and switch them
   in JavaScript. Do not link to other pages or reload with a different URL.
6. Inside `<script>`, never write the literal text `</script>` in a string.
   Write `<\/script>` instead.
7. The interface language is **French** unless the request says otherwise.
   `OrthoTips.language` gives the app language (`"fr"`, `"en"`) if needed.

---

## 3. Metadata block

Put these tags in `<head>`, right after the viewport tag. Always use double
quotes, and do not put `>` inside a `content` value.

```html
<meta name="ortho-ext-id" content="dems-programme">
<meta name="ortho-ext-name" content="Programme DEMS">
<meta name="ortho-ext-description" content="Programme de révision croisé avec les sujets 2015–2022.">
<meta name="ortho-ext-version" content="2.1">
<meta name="ortho-ext-icon" content="🎓">
<!-- optional -->
<meta name="ortho-ext-author" content="Dr …">
<meta name="ortho-ext-network" content="allow">
```

| Name | Required | Meaning |
|---|---|---|
| `ortho-ext-id` | **yes** | Permanent identity: lowercase letters, digits and `-`, 48 characters maximum. **Never change it** in later versions. Importing a file with an id that is already installed **replaces** the program and **keeps its data**; that is how updates work. |
| `ortho-ext-name` | **yes** | Name shown in the list and the top bar. Short, about 30 characters. |
| `ortho-ext-description` | recommended | One sentence, shown under the name (2 lines maximum). |
| `ortho-ext-version` | recommended | Free text such as `1.0` or `2.3`. Raise it for every new version. |
| `ortho-ext-icon` | recommended | **One emoji**. The default is 🧩. |
| `ortho-ext-author` | no | Free text. |
| `ortho-ext-network` | no | `allow` lets the page load from the internet. Avoid it: the app shows an "uses the internet" badge, and the extension breaks offline. |

A file without this block still imports, using `<title>` as its name. Its id is
then derived from the title, so updates are less reliable. Always include the block.

---

## 4. Host API — `window.OrthoTips`

The app injects this object **before any of the page's scripts run**, so it is
available synchronously at the top of your first `<script>`. In a normal
browser it is `undefined`.

```js
const HOST = window.OrthoTips || null;   // always feature-detect
```

The app injects the bridge only after the `.OTips` container has been decrypted in
its private WebView. A plain HTML file, a renamed ZIP, or a file encrypted by
another installation is rejected. The current `.OTips` envelope uses authenticated
AES-GCM and an app-keystore key; the key is never exported with the file.

### Properties

| Property | Type | Notes |
|---|---|---|
| `bridgeVersion` | number | `1` for this guide. |
| `extensionId` | string | The `ortho-ext-id` of this file. |
| `extensionVersion` | string | The installed version. |
| `appVersion` | string | Ortho Tips version name. |
| `language` | string | App language, such as `"fr"`. |
| `theme` | `"light"` \| `"dark"` | Current app theme. Kept up to date. |

The app also sets `data-ortho-theme="light|dark"` on `<html>`.

### Storage (synchronous)

```js
OrthoTips.storage.get(key)          // → string | null
OrthoTips.storage.set(key, value)   // value is converted to a string; throws if too large
OrthoTips.storage.remove(key)
OrthoTips.storage.keys(prefix)      // → sorted array of keys starting with prefix ('' = all)
```

The limits are 200 characters per key and 5 MB per value. Keep the total under
about 2 MB: store JSON, not images.

Programs written for other environments also keep working, because the same
store is exposed two more ways:

- **`localStorage`** is redirected to the app's store.
- **`window.storage`** matches the storage API of Claude artifacts (all methods
  return Promises):
  `get(key)` resolves to `{key, value}`, or to **`null`** if the key is missing
  (Claude rejects instead, so wrap calls in `try/catch` to support both);
  `set(key, value)` → `{key, value}`; `delete(key)` → `{key, deleted}`;
  `list(prefix)` → `{keys}`.

Do not use IndexedDB, cookies or Cache Storage. They bypass the app, so they are
not erased with the extension and not reliable.

### Files

```js
// Save: opens Android's "save as" screen. Resolves true if the file was written.
const ok = await OrthoTips.saveFile('backup-2026-09-19.json', jsonString, 'application/json');
// data can be a string, Blob, ArrayBuffer or Uint8Array.
```

- **Opening a file**: use a normal `<input type="file">`. The app shows Android's
  file picker (one file at a time). Read it with `await file.text()`. Do not rely
  on `accept` to filter non-media files; validate the content instead.
- The usual browser download trick (`<a download href="blob:…">` and `.click()`)
  also works in the app; it is routed to `saveFile`. Calling `saveFile` directly
  gives you the true/false result.

### Theme

```js
function applyTheme(t) { document.documentElement.dataset.theme = t; }
if (HOST) { applyTheme(HOST.theme); HOST.onThemeChange(applyTheme); }
else { /* use prefers-color-scheme, see the template */ }
```

A `window` event `orthotips:theme` (with `detail` = theme) is also dispatched.
Inside the app, **follow the app's theme** and hide any theme switch the page has
of its own.

### Back button

```js
OrthoTips.onBack(() => {
  if (secondaryViewIsOpen) { showMainView(); return true; }  // handled
  return false;                                              // let the app close the extension
});
```

Return `true` only when you actually handled it, otherwise the user cannot leave.
The arrow in the app's top bar always closes the extension.

### Other

```js
OrthoTips.toast('Sauvegarde enregistrée');   // short Android message
OrthoTips.close();                           // close the extension
```

`alert()`, `confirm()` and `prompt()` work and show Android dialogs. Prefer
in-page UI or `toast` for non-blocking messages.

---

## 5. Behaviour every extension must have

1. **Works offline** and **in a normal browser** (section 1).
2. **Light and dark themes** through CSS variables on `html[data-theme="dark"]`.
   Follow `OrthoTips.theme` in the app, and `prefers-color-scheme` in a browser.
3. **Mobile first**: readable at 360 px wide, no horizontal scrolling, touch
   targets at least 44 px, `font-size` at least 13 px.
4. **Never loses data**:
   - Save automatically. Debounce writes (about 200 ms) and also save on
     `visibilitychange` → hidden.
   - Store **one JSON document** under a key prefixed with the id, such as
     `"<id>:state"`, with a schema number: `{ v: 1, … }`.
   - On load, pass stored data through a `migrate()` function. When the shape
     changes, raise the schema number and convert old data. Never discard it.
   - Wrap `JSON.parse` of stored data in `try/catch` and fall back to defaults.
5. **Backup buttons**: an export that saves `{ app: "<id>", v, date, data }` as
   `.json`, and an import that validates `app` and the shape before replacing
  anything. Deleting an extension in the app erases its data, so this is the
   user's only copy.
6. **Escapes text** before inserting it with `innerHTML`, using an `esc()` helper
   as in the template.
7. **Fast**: build the content once, avoid heavy libraries, and keep data as a
   JS constant inside the file.

---

## 6. Design so it feels part of Ortho Tips

- Clean Material-like cards with rounded corners (about 16 px), a subtle border
  or shadow, and 16 px page padding.
- System font stack: `system-ui, Roboto, sans-serif`. Monospace:
  `ui-monospace, "Roboto Mono", monospace`.
- Do not add a big header with the extension's name: the app's top bar already
  shows the icon and name. Start with the content.
- Keep a sticky toolbar, if any, compact: it sits under the app bar.
- Suggested palette (adjust freely), light / dark:
  background `#f6f7f9` / `#121417`, surface `#ffffff` / `#1d2025`,
  text `#1b1d22` / `#e6e9ee`, secondary text `#5f6b76` / `#a3adb8`,
  accent `#3f51b5` / `#9fa8ff`.

---

## 7. What does not work (do not use)

| Avoid | Why / use instead |
|---|---|
| CDN scripts, web fonts, remote images | Network is blocked. Inline everything. |
| `window.open`, `target="_blank"` for internal pages, multi-page links | One page only. External `https://` links open in the phone's browser, which is fine for references. |
| `<form>` submitting to a URL | Use `addEventListener('submit', e => e.preventDefault())` and handle it in JS. |
| Service workers, `Notification`, geolocation, camera, microphone, clipboard read | Not available in the extension sandbox. |
| IndexedDB, cookies, Cache Storage | Not managed by the app. Use `OrthoTips.storage` or `localStorage`. |
| `<input type="file" multiple>` | One file at a time. |
| Assuming a desktop width | Design for 360 px first. |
| Patient-identifying data | Extensions are for study and reference tools. Keep patient data in the app itself. |

---

## 8. Updating an extension

1. Keep the **same `ortho-ext-id`**.
2. Raise `ortho-ext-version`.
3. If the saved-data shape changed, raise the schema number and extend `migrate()`.
4. Import the new file. The app replaces the program, keeps the data, and shows
   "updated to version X".

The `.OTips` file must be produced by the app's extension packager/export action.
Do not rename an HTML file to `.OTips`: the extension will not be accepted.

---

## 9. Adapting an existing HTML program (checklist)

This is what was done to the HTML payload of `programme-dems`:

1. Add the metadata block (section 3).
2. Remove external `<link>` and `<script src>` resources. Inline them, or
   replace fonts with system stacks (keep the original font first in the stack
   so it still shows in a browser where it is installed).
3. At the top of the script, add `const HOST = window.OrthoTips || null;`.
4. Theme: if `HOST`, hide the page's own theme switch, call
   `applyTheme(HOST.theme)`, and subscribe with `HOST.onThemeChange`.
5. Export: if `HOST`, use `await HOST.saveFile(name, json, 'application/json')`.
   Keep the blob download for browsers.
6. Back: if the program has tabs or views, register `HOST.onBack` to return to
   the main view first.
7. Any text that says "saved in this browser" should say "saved in Ortho Tips on
   this phone" when `HOST` is set.
8. Storage needs no change if it already uses `localStorage` or `window.storage`.

---

## 10. Testing before importing

1. Open the file in desktop Chrome, turn on device mode (DevTools) at **360 × 740**,
   and check light and dark (DevTools → Rendering → prefers-color-scheme).
2. Use every feature, reload the page, and confirm the data is still there.
3. Export a backup, change something, restore the backup, and confirm the change is undone.
4. Watch the DevTools console: there must be **no errors**.
5. In the app: import it, open it, switch the app theme, press Back from a
   secondary view, then use **⋮ → Console** in the extension's top bar to see
   any JavaScript errors from the phone.

---

## 11. Troubleshooting

| Symptom | Likely cause |
|---|---|
| "This file is not an HTML program" | The file has no `<html>`, `<head>`, `<body>` or `<script>` tag near the start. |
| Import created a second copy instead of updating | `ortho-ext-id` changed or is missing. |
| Blank page | A JavaScript error at load: check ⋮ → Console. Often a remote script that could not load. |
| Fonts or icons missing | They were loaded from the internet. Inline them or use system fonts / inline SVG. |
| Data gone after an update | The id changed, or `migrate()` discarded the old shape. |
| Export does nothing | The file picker was cancelled (`saveFile` resolved `false`). |
| Theme does not follow the app | The code did not use `OrthoTips.theme` / `onThemeChange`, or the page hard-codes colors instead of CSS variables. |

---

## 12. Starter template

A complete, working extension that shows every pattern above: metadata, host
detection, storage with schema and migration, debounced saves, light/dark
theme, two views with Back handling, export and import. Copy it and replace
the content.

```html
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="ortho-ext-id" content="exemple-checklist">
<meta name="ortho-ext-name" content="Checklist exemple">
<meta name="ortho-ext-description" content="Modèle de départ : liste à cocher, sauvegarde, thème clair/sombre.">
<meta name="ortho-ext-version" content="1.0">
<meta name="ortho-ext-icon" content="✅">
<title>Checklist exemple</title>
<style>
:root{
  --bg:#f6f7f9; --surface:#ffffff; --ink:#1b1d22; --ink-2:#5f6b76;
  --line:rgba(0,0,0,.08); --accent:#3f51b5; --on-accent:#ffffff; --danger:#c62828;
  --radius:16px;
}
html[data-theme="dark"]{
  --bg:#121417; --surface:#1d2025; --ink:#e6e9ee; --ink-2:#a3adb8;
  --line:rgba(255,255,255,.08); --accent:#9fa8ff; --on-accent:#121417; --danger:#ef9a9a;
}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--ink);
  font:15px/1.5 system-ui,Roboto,sans-serif;-webkit-font-smoothing:antialiased}
main{max-width:720px;margin:0 auto;padding:16px}
h1{font-size:20px;margin:4px 0 12px}
.card{background:var(--surface);border-radius:var(--radius);padding:4px 0;border:1px solid var(--line)}
.row{display:flex;align-items:center;gap:12px;padding:12px 16px;min-height:48px}
.row + .row{border-top:1px solid var(--line)}
.row input{width:22px;height:22px;accent-color:var(--accent)}
.row.done span{color:var(--ink-2);text-decoration:line-through}
.bar{display:flex;gap:8px;flex-wrap:wrap;margin:16px 0}
button{font:inherit;border:0;border-radius:999px;padding:10px 16px;min-height:44px;
  background:var(--surface);color:var(--ink);border:1px solid var(--line)}
button.primary{background:var(--accent);color:var(--on-accent);border-color:transparent}
.muted{color:var(--ink-2);font-size:13px}
[hidden]{display:none !important}
</style>
</head>
<body>
<main>
  <section id="view-list">
    <h1>Checklist exemple</h1>
    <div class="card" id="list"></div>
    <div class="bar">
      <button class="primary" id="btn-stats">Voir le résumé</button>
      <button id="btn-export">Sauvegarder</button>
      <button id="btn-import">Restaurer</button>
      <input type="file" id="file" accept="application/json,.json" hidden>
    </div>
    <p class="muted" id="where"></p>
  </section>

  <section id="view-stats" hidden>
    <h1>Résumé</h1>
    <div class="card"><div class="row"><span id="stats"></span></div></div>
    <div class="bar"><button id="btn-back">Retour</button></div>
  </section>
</main>

<script>
/* ---- Host detection: present inside Ortho Tips, absent in a browser ---- */
const HOST = window.OrthoTips || null;
const STORE_KEY = 'exemple-checklist:state';
const SCHEMA = 1;

const ITEMS = [
  {id: 'a', label: 'Relire la classification de Garden'},
  {id: 'b', label: 'Réviser les voies d’abord de la hanche'},
  {id: 'c', label: 'Faire la fiche fracture du scaphoïde'}
];

let state = {v: SCHEMA, done: {}};

/* ---- Storage: OrthoTips.storage in the app, localStorage in a browser ---- */
const Store = {
  get(key){
    try{ return HOST ? HOST.storage.get(key) : localStorage.getItem(key); }catch(e){ return null; }
  },
  set(key, value){
    try{ HOST ? HOST.storage.set(key, value) : localStorage.setItem(key, value); }catch(e){}
  }
};

function load(){
  try{
    const raw = JSON.parse(Store.get(STORE_KEY) || 'null');
    if(raw && typeof raw === 'object') state = migrate(raw);
  }catch(e){ /* first run or unreadable: keep defaults */ }
}
function migrate(raw){
  // Bump SCHEMA and convert older shapes here; never drop the user's data.
  return {v: SCHEMA, done: (raw.done && typeof raw.done === 'object') ? raw.done : {}};
}
let saveTimer = 0;
function save(){
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => Store.set(STORE_KEY, JSON.stringify(state)), 200);
}
document.addEventListener('visibilitychange', () => {
  if(document.visibilityState === 'hidden'){ clearTimeout(saveTimer); Store.set(STORE_KEY, JSON.stringify(state)); }
});

/* ---- Theme: follow the app, else the system ---- */
function applyTheme(t){ document.documentElement.dataset.theme = t; }
if(HOST){
  applyTheme(HOST.theme);
  HOST.onThemeChange(applyTheme);
}else{
  const mq = window.matchMedia('(prefers-color-scheme: dark)');
  applyTheme(mq.matches ? 'dark' : 'light');
  mq.addEventListener('change', e => applyTheme(e.matches ? 'dark' : 'light'));
}

/* ---- Small helpers ---- */
const $ = s => document.querySelector(s);
const esc = s => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/"/g,'&quot;');
function toast(msg){ if(HOST) HOST.toast(msg); else console.log(msg); }

/* ---- Views ---- */
function render(){
  $('#list').innerHTML = ITEMS.map(it => `
    <label class="row ${state.done[it.id] ? 'done' : ''}">
      <input type="checkbox" data-id="${it.id}" ${state.done[it.id] ? 'checked' : ''}>
      <span>${esc(it.label)}</span>
    </label>`).join('');
}
function showView(name){
  $('#view-list').hidden = name !== 'list';
  $('#view-stats').hidden = name !== 'stats';
  if(name === 'stats'){
    const n = ITEMS.filter(it => state.done[it.id]).length;
    $('#stats').textContent = n + ' / ' + ITEMS.length + ' faits';
  }
  window.scrollTo(0, 0);
}
// Back in the app: leave the secondary view first, then let the app close us.
if(HOST) HOST.onBack(() => {
  if(!$('#view-stats').hidden){ showView('list'); return true; }
  return false;
});

$('#list').addEventListener('change', e => {
  const id = e.target.dataset.id;
  if(!id) return;
  state.done[id] = e.target.checked;
  save(); render();
});
$('#btn-stats').addEventListener('click', () => showView('stats'));
$('#btn-back').addEventListener('click', () => showView('list'));

/* ---- Backup: export and restore a JSON file ---- */
$('#btn-export').addEventListener('click', async () => {
  const name = 'exemple-checklist-' + new Date().toISOString().slice(0, 10) + '.json';
  const json = JSON.stringify({app: 'exemple-checklist', v: SCHEMA, date: new Date().toISOString(), data: state}, null, 2);
  if(HOST){
    if(await HOST.saveFile(name, json, 'application/json')) toast('Sauvegarde enregistrée');
    return;
  }
  const url = URL.createObjectURL(new Blob([json], {type: 'application/json'}));
  const a = Object.assign(document.createElement('a'), {href: url, download: name});
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 5000);
});
$('#btn-import').addEventListener('click', () => $('#file').click());
$('#file').addEventListener('change', async e => {
  const file = e.target.files && e.target.files[0];
  e.target.value = '';
  if(!file) return;
  try{
    const raw = JSON.parse(await file.text());
    if(!raw || raw.app !== 'exemple-checklist' || !raw.data) throw new Error('format');
    state = migrate(raw.data);
    save(); render();
    toast('Sauvegarde restaurée');
  }catch(err){
    alert('Ce fichier n’est pas une sauvegarde de cette extension.');
  }
});

/* ---- Start ---- */
$('#where').textContent = HOST
  ? 'Enregistré dans Ortho Tips, sur ce téléphone.'
  : 'Enregistré dans ce navigateur.';
load();
render();
</script>
</body>
</html>
```

---

## 13. For developers: where the host lives in the app

| File | Role |
|---|---|
| `assets/extension_host/ortho-bridge.js` | Builds `window.OrthoTips`, `window.storage`, the `localStorage` redirect and the download routing. Injected first in every page. |
| `data/extensions/ExtensionRepository.kt` | Decrypt, validate, import, list, delete, export. Files live in `filesDir/extensions/<id>.OTips`. |
| `data/extensions/OrthoExtension.kt` | Model and parser for the `ortho-ext-*` metadata. |
| `data/extensions/ExtensionStorage.kt` | Per-extension key/value store (SharedPreferences `ext_data_<id>`). |
| `ui/screens/extensions/ExtensionWebBridge.kt` | The `OrthoTipsNative` Java interface behind the bridge. |
| `ui/screens/extensions/ExtensionRunnerScreen.kt` | Full-screen WebView: settings, file picker, save dialog, theme, Back, console. |
| `ui/screens/extensions/ExtensionsScreen.kt` | The Extensions tab: built-in guides/classifications, editor, import and export. |

To add a new capability, add a method to `ExtensionBridge` (annotated
`@JavascriptInterface`), expose it through `ortho-bridge.js`, raise
`bridgeVersion`, and document it here.
