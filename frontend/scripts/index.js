'use strict';

// Copies the locale-redirect page to dist/hermanas-client/index.html.
// Favicons used to be copied here too, but they now live in public/ and are
// emitted to dist/ automatically by Angular CLI's asset pipeline.

const fs = require('fs');
const path = require('path');

const redirectFile = path.resolve(path.dirname(__filename), '../src/redirect.html');
const indexFile = path.resolve(path.dirname(__filename), '../dist/hermanas-client/index.html');

fs.copyFile(redirectFile, indexFile, () => {
    console.log(`### INFO: File copied from : ${redirectFile} to ${indexFile}`);
});
