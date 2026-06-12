/// <reference types="vitest" />
import angular from '@analogjs/vite-plugin-angular';
import { defineConfig } from 'vitest/config';
import * as path from 'path';

/**
 * Vitest configuration for the Hermanas frontend.
 *
 * Migration notes (from Karma + Jasmine):
 *  - Browser-less: jsdom + Angular's zone.js based TestBed run in Node.
 *    Much faster than spinning up Chrome for every run.
 *  - Path aliases are mirrored from tsconfig.json so the `@modules/...`
 *    and `@common/...` imports keep working inside specs.
 *  - Globals enabled so we keep the Jasmine-style `describe / it / expect`
 *    surface without sprinkling Vitest imports in every spec file. The
 *    Jasmine-specific bits (jasmine.SpyObj, jasmine.createSpyObj) are
 *    translated by hand to `vi.fn()` patterns in the next commit.
 */
export default defineConfig({
    plugins: [angular()],
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: ['src/test-setup.ts'],
        include: ['src/**/*.spec.ts'],
        // Vitest 4 default-flipped lifecycle mocks to "leak across tests",
        // so vi.spyOn(console, 'debug') in one spec's beforeEach kept
        // counting calls from the next spec. Restoring on each test
        // mirrors the Vitest 3 behaviour our suite was written against.
        restoreMocks: true,
        // Karma was reporting in the terminal; keep the dot reporter for tight CI
        // output and `verbose` for local debugging via `npm test -- --reporter=verbose`.
        reporters: ['default'],
        coverage: {
            provider: 'v8',
            reporter: ['text', 'html', 'lcov'],
            include: ['src/**/*.ts'],
            exclude: ['src/**/*.spec.ts', 'src/test-setup.ts', 'src/testing/**'],
        },
    },
    resolve: {
        alias: {
            '@modules': path.resolve(__dirname, 'src/modules'),
            '@common': path.resolve(__dirname, 'src/modules/app-common'),
            '@testing': path.resolve(__dirname, 'src/testing'),
        },
    },
});
