'use strict';

const fs = require('fs');
const path = require('path');
const redirectFile = path.resolve(path.dirname(__filename), '../src/redirect.html');
const indexFile = path.resolve(path.dirname(__filename), '../dist/hermanas-client/index.html');

fs.copyFile(redirectFile, indexFile,  () => {
    console.log(`### INFO: File copied from : ${redirectFile} to ${indexFile}`);
});

let srcFaviconFile = path.resolve(path.dirname(__filename), '../src/favicon.ico');
let destFaviconFile = path.resolve(path.dirname(__filename), '../dist/hermanas-client/favicon.ico');

fs.copyFile(srcFaviconFile, destFaviconFile,  () => {
    console.log(`### INFO: File copied from : ${srcFaviconFile} to ${destFaviconFile}`);
});

srcFaviconFile = path.resolve(path.dirname(__filename), '../src/favicon144.png');
destFaviconFile = path.resolve(path.dirname(__filename), '../dist/hermanas-client/favicon144.png');

fs.copyFile(srcFaviconFile, destFaviconFile,  () => {
    console.log(`### INFO: File copied from : ${srcFaviconFile} to ${destFaviconFile}`);
});

srcFaviconFile = path.resolve(path.dirname(__filename), '../src/favicon512.png');
destFaviconFile = path.resolve(path.dirname(__filename), '../dist/hermanas-client/favicon512.png');

fs.copyFile(srcFaviconFile, destFaviconFile,  () => {
    console.log(`### INFO: File copied from : ${srcFaviconFile} to ${destFaviconFile}`);
});
