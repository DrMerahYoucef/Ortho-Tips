/*
 * Ortho Tips — extension host bridge (API version 2).
 *
 * Injected as the first script of every extension page, before any of the
 * page's own code. It turns the raw Android interface `OrthoTipsNative` into:
 *
 *   window.OrthoTips   the documented host API (see guide.md)
 *   window.storage     Claude-artifact compatible async key/value storage
 *   localStorage       redirected to the same app-managed store
 *   <a download>       blob:/data: downloads saved through Android's file picker
 *
 * Version 2 adds the permission system (extension guide, section 4):
 * `OrthoTips.permissions`, and the gated `files`, `assets` and `database` APIs.
 * A gated call without its permission rejects with a PermissionError and never
 * throws synchronously; the app enforces the same rule natively.
 *
 * Everything an extension saves lives in the app, per extension, and is erased
 * when the extension is deleted or its data is cleared from the Extensions tab.
 */
(function () {
  'use strict';
  if (window.OrthoTips) return;
  var N = window.OrthoTipsNative;
  if (!N) return;

  var info = {};
  try { info = JSON.parse(N.info()) || {}; } catch (e) { info = {}; }

  // ---- async requests answered by the app ----------------------------------
  var pending = {};
  var seq = 0;
  function request(start) {
    return new Promise(function (resolve) {
      var id = 'q' + (++seq);
      pending[id] = resolve;
      try { start(id); } catch (e) { delete pending[id]; resolve(false); }
    });
  }
  function resolveRequest(id, value) {
    var done = pending[id];
    if (!done) return;
    delete pending[id];
    done(value);
  }

  // ---- permissions ---------------------------------------------------------
  var granted = Array.isArray(info.permissions) ? info.permissions.map(String) : [];
  function has(name) { return granted.indexOf(String(name)) !== -1; }

  function PermissionError(permission) {
    var e = new Error("PermissionError: '" + permission + "' not granted");
    e.name = 'PermissionError';
    e.permission = permission;
    return e;
  }
  function denied(permission) {
    var e = PermissionError(permission);
    try { console.warn(e.message); } catch (x) {}
    return Promise.reject(e);
  }

  // ---- promise-based native calls (files, assets, database) ------------------
  var calls = {};
  var callSeq = 0;
  function call(method, args) {
    return new Promise(function (resolve, reject) {
      var id = 'c' + (++callSeq);
      calls[id] = { resolve: resolve, reject: reject };
      try { N.call(id, method, JSON.stringify(args || {})); }
      catch (e) { delete calls[id]; reject(e); }
    });
  }
  function settleCall(id, ok, value) {
    var c = calls[id];
    if (!c) return;
    delete calls[id];
    if (ok) { c.resolve(value); return; }
    var message = String(value);
    var e = new Error(message);
    if (message.indexOf('PermissionError') === 0) e.name = 'PermissionError';
    c.reject(e);
  }
  function gated(permission, method, args) {
    return has(permission) ? call(method, args) : denied(permission);
  }
  function fromBase64(b64) {
    var bin = atob(b64);
    var bytes = new Uint8Array(bin.length);
    for (var i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    return bytes;
  }
  /** Decodes base64 from the app as text (default), 'base64' or 'arraybuffer'. */
  function decode(b64, as) {
    if (b64 === null || b64 === undefined) return null;
    if (as === 'base64') return b64;
    var bytes = fromBase64(b64);
    if (as === 'arraybuffer') return bytes.buffer;
    return new TextDecoder('utf-8').decode(bytes);
  }

  // ---- storage -------------------------------------------------------------
  var store = {
    get: function (key) {
      var v = N.storageGet(String(key));
      return v === null || v === undefined ? null : String(v);
    },
    set: function (key, value) {
      if (!N.storageSet(String(key), String(value))) {
        throw new Error('Ortho Tips storage: value too large or storage full');
      }
    },
    remove: function (key) { N.storageDelete(String(key)); },
    keys: function (prefix) {
      try { return JSON.parse(N.storageKeys(prefix == null ? '' : String(prefix))); }
      catch (e) { return []; }
    }
  };

  // Same shape as the storage API of Claude artifacts, so programs made there
  // keep their data here without changes. `get` resolves to null for a missing
  // key (Claude rejects instead; code written for both uses try/catch).
  if (!window.storage) {
    window.storage = {
      get: function (key, shared) {
        return Promise.resolve().then(function () {
          var v = store.get(key);
          return v === null ? null : { key: key, value: v, shared: !!shared };
        });
      },
      set: function (key, value, shared) {
        return Promise.resolve().then(function () {
          store.set(key, value);
          return { key: key, value: String(value), shared: !!shared };
        });
      },
      delete: function (key, shared) {
        return Promise.resolve().then(function () {
          store.remove(key);
          return { key: key, deleted: true, shared: !!shared };
        });
      },
      list: function (prefix, shared) {
        return Promise.resolve().then(function () {
          return { keys: store.keys(prefix), prefix: prefix, shared: !!shared };
        });
      }
    };
  }

  // localStorage, backed by the same store. If the WebView refuses the swap,
  // the native localStorage stays in place and still persists.
  try {
    var ls = {
      getItem: function (k) { return store.get(k); },
      setItem: function (k, v) { store.set(k, v); },
      removeItem: function (k) { store.remove(k); },
      clear: function () { store.keys('').forEach(function (k) { store.remove(k); }); },
      key: function (i) { var ks = store.keys(''); return i >= 0 && i < ks.length ? ks[i] : null; }
    };
    Object.defineProperty(ls, 'length', {
      configurable: true, get: function () { return store.keys('').length; }
    });
    var proxy = new Proxy(ls, {
      get: function (t, p) {
        if (p in t) return t[p];
        if (typeof p !== 'string') return undefined;
        var v = store.get(p);
        return v === null ? undefined : v;
      },
      set: function (t, p, v) {
        if (typeof p === 'string' && !(p in t)) store.set(p, v);
        return true;
      },
      deleteProperty: function (t, p) {
        if (typeof p === 'string') store.remove(p);
        return true;
      },
      has: function (t, p) { return (p in t) || (typeof p === 'string' && store.get(p) !== null); },
      ownKeys: function () { return store.keys(''); },
      getOwnPropertyDescriptor: function (t, p) {
        var v = typeof p === 'string' ? store.get(p) : null;
        return v === null ? undefined : { value: v, enumerable: true, configurable: true, writable: true };
      }
    });
    Object.defineProperty(window, 'localStorage', {
      configurable: true, enumerable: true, get: function () { return proxy; }
    });
  } catch (e) { /* keep the native localStorage */ }

  // ---- files -----------------------------------------------------------------
  function toBytes(data) {
    if (typeof Blob !== 'undefined' && data instanceof Blob) {
      return data.arrayBuffer().then(function (b) { return new Uint8Array(b); });
    }
    if (data instanceof ArrayBuffer) return Promise.resolve(new Uint8Array(data));
    if (ArrayBuffer.isView(data)) {
      return Promise.resolve(new Uint8Array(data.buffer, data.byteOffset, data.byteLength));
    }
    return Promise.resolve(new TextEncoder().encode(data == null ? '' : String(data)));
  }
  function toBase64(bytes) {
    var out = '';
    var CHUNK = 0x8000;
    for (var i = 0; i < bytes.length; i += CHUNK) {
      out += String.fromCharCode.apply(null, bytes.subarray(i, i + CHUNK));
    }
    return btoa(out);
  }
  function guessMime(name) {
    var ext = String(name || '').split('.').pop().toLowerCase();
    return {
      json: 'application/json', txt: 'text/plain', csv: 'text/csv', html: 'text/html',
      md: 'text/markdown', pdf: 'application/pdf', png: 'image/png', jpg: 'image/jpeg',
      jpeg: 'image/jpeg', svg: 'image/svg+xml'
    }[ext] || 'application/octet-stream';
  }
  /** Asks where to save, then writes. Resolves to true when the file was written. */
  function saveFile(fileName, data, mimeType) {
    if (!has('import_export')) {
      try { console.warn(PermissionError('import_export').message); } catch (x) {}
      return Promise.resolve(false);
    }
    var name = String(fileName || 'fichier');
    var mime = mimeType || (data && data.type) || guessMime(name);
    return toBytes(data).then(function (bytes) {
      var b64 = toBase64(bytes);
      return request(function (id) { N.saveFile(id, name, mime, b64); });
    }).catch(function () { return false; });
  }
  function saveFromUrl(url, name) {
    return fetch(url)
      .then(function (r) { return r.blob(); })
      .then(function (blob) { return saveFile(name, blob, blob.type || guessMime(name)); })
      .catch(function () { return false; });
  }
  function isLocalDownload(a) {
    return a && a.hasAttribute && a.hasAttribute('download') && /^(blob:|data:)/i.test(a.href || '');
  }
  // Android's WebView cannot follow <a download> to a blob: or data: URL, which
  // is how browser programs usually export a file. Route those through saveFile.
  var nativeClick = HTMLAnchorElement.prototype.click;
  HTMLAnchorElement.prototype.click = function () {
    if (isLocalDownload(this)) {
      saveFromUrl(this.href, this.getAttribute('download') || 'fichier');
      return;
    }
    return nativeClick.apply(this, arguments);
  };
  document.addEventListener('click', function (e) {
    var a = e.target && e.target.closest ? e.target.closest('a[download]') : null;
    if (isLocalDownload(a)) {
      e.preventDefault();
      saveFromUrl(a.href, a.getAttribute('download') || 'fichier');
    }
  }, true);

  // ---- theme, back, misc -----------------------------------------------------
  var themeListeners = [];
  var backHandlers = [];

  function markTheme(t) {
    try { document.documentElement.setAttribute('data-ortho-theme', t); } catch (e) {}
  }

  var api = {
    bridgeVersion: 2,
    extensionId: String(info.extensionId || ''),
    extensionVersion: String(info.extensionVersion || ''),
    appVersion: String(info.appVersion || ''),
    language: String(info.language || 'fr'),
    theme: info.theme === 'dark' ? 'dark' : 'light',

    storage: { get: store.get, set: store.set, remove: store.remove, keys: store.keys },

    permissions: {
      granted: granted.slice(),
      has: has
    },
    // Requires `storage`: a private folder for data too big for `storage`.
    files: {
      write: function (path, data) {
        if (!has('storage')) return denied('storage');
        return toBytes(data).then(function (bytes) {
          return call('files.write', { path: String(path), base64: toBase64(bytes) });
        });
      },
      read: function (path, options) {
        var as = options && options.as;
        return gated('storage', 'files.read', { path: String(path) })
          .then(function (b64) { return decode(b64, as); });
      },
      list: function (prefix) {
        return gated('storage', 'files.list', { prefix: prefix == null ? '' : String(prefix) });
      },
      delete: function (path) { return gated('storage', 'files.delete', { path: String(path) }); }
    },
    // Requires `assets`: read-only access to other files shipped in the package.
    assets: {
      read: function (path, options) {
        var as = options && options.as;
        return gated('assets', 'assets.read', { path: String(path) })
          .then(function (b64) { return decode(b64, as); });
      }
    },
    // Requires `database`: read-only clinical reference data, never patients.
    database: {
      query: function (spec) { return gated('database', 'database.query', spec || {}); }
    },

    saveFile: saveFile,
    toast: function (message) { N.toast(String(message)); },
    close: function () { N.close(); },
    onThemeChange: function (cb) { if (typeof cb === 'function') themeListeners.push(cb); },
    onBack: function (cb) { if (typeof cb === 'function') backHandlers.push(cb); },

    // Called by the app, not by extensions.
    __resolve: resolveRequest,
    __settle: settleCall,
    __setTheme: function (t) {
      var theme = t === 'dark' ? 'dark' : 'light';
      if (theme === api.theme) return;
      api.theme = theme;
      markTheme(theme);
      themeListeners.forEach(function (cb) { try { cb(theme); } catch (e) {} });
      try { window.dispatchEvent(new CustomEvent('orthotips:theme', { detail: theme })); } catch (e) {}
    },
    __handleBack: function () {
      for (var i = backHandlers.length - 1; i >= 0; i--) {
        try { if (backHandlers[i]() === true) return true; } catch (e) {}
      }
      return false;
    }
  };

  markTheme(api.theme);
  window.OrthoTips = api;
})();
