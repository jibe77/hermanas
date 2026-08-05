'use strict';

// Rewrites the PWA manifest of each localized build so its `scope` and
// `start_url` point inside that locale.
//
// The Angular asset pipeline copies public/manifest.webmanifest verbatim into
// every locale directory, and `--localize` only rewrites <base href> in the
// HTML — never the JSON. Left as-is, every build ships `"start_url": "/dashboard"`,
// a path the server does not serve: the SPA lives under /fr-FR/, /en-US/ and
// /ro-RO/. iOS reads start_url when the user adds the app to the home screen,
// so the icon opened /dashboard and got a bare 404 payload.
//
// Locales are read from angular.json (i18n.sourceLocale + i18n.locales) so this
// stays correct when a language is added or removed.

const fs = require('fs');
const path = require('path');

const frontendRoot = path.resolve(path.dirname(__filename), '..');
const angularJsonFile = path.join(frontendRoot, 'angular.json');
const distDir = path.join(frontendRoot, 'dist', 'hermanas-client');

function readLocales() {
    const angularJson = JSON.parse(fs.readFileSync(angularJsonFile, 'utf8'));
    const i18n = angularJson.projects['hermanas-client'].i18n;
    return [i18n.sourceLocale, ...Object.keys(i18n.locales || {})];
}

function localizeManifest(locale) {
    const manifestFile = path.join(distDir, locale, 'manifest.webmanifest');
    if (!fs.existsSync(manifestFile)) {
        console.log(`### INFO: No manifest for locale ${locale}, skipping.`);
        return;
    }

    const manifest = JSON.parse(fs.readFileSync(manifestFile, 'utf8'));
    const base = `/${locale}/`;

    manifest.scope = base;
    // Keep the same landing route, but anchored in this locale.
    manifest.start_url = `${base}dashboard`;

    fs.writeFileSync(manifestFile, `${JSON.stringify(manifest, null, 2)}\n`);
    console.log(`### INFO: Manifest localized for ${locale}: scope=${manifest.scope} start_url=${manifest.start_url}`);
}

readLocales().forEach(localizeManifest);
