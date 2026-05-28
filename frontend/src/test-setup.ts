// Vitest + Angular setup. Mirrors what Karma's test.ts used to do (initialize
// the TestBed against the browser dynamic platform), but here we run inside
// jsdom + zone.js, so the polyfill order matters.

import '@analogjs/vitest-angular/setup-zone';

import { getTestBed } from '@angular/core/testing';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';

getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting());
