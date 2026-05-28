// @ts-check
/**
 * Flat config (ESLint 9). Replaces the legacy `.eslintrc.json`.
 *
 * Two slices:
 *   - the `.ts` block carries the same set of TypeScript / Angular rules the
 *     project used under eslintrc, just translated to the flat-config shape.
 *   - the `.html` block runs the Angular template parser on Angular component
 *     templates only (the standalone `index.html` and `redirect.html` stay
 *     ignored).
 *
 * Adding a rule? Drop it inside the right `rules` object. The plugin objects
 * keep their dotted ids (`@angular-eslint/component-selector`, etc.) so
 * existing eslint-disable comments still resolve.
 */

const angular = require('@angular-eslint/eslint-plugin');
const angularTemplate = require('@angular-eslint/eslint-plugin-template');
const angularTemplateParser = require('@angular-eslint/template-parser');
const tseslint = require('@typescript-eslint/eslint-plugin');
const tsParser = require('@typescript-eslint/parser');
const prettierPlugin = require('eslint-plugin-prettier');
const prettierConfig = require('eslint-config-prettier');

module.exports = [
    {
        ignores: [
            'projects/**/*',
            'src/redirect.html',
            'src/index.html',
            // Generated, vendored or output dirs
            'dist/**',
            'out-tsc/**',
            'coverage/**',
            '.angular/**',
            'node_modules/**',
            // Build / runtime helpers (CJS) that don't ship to the browser
            'eslint.config.js',
            'vitest.config.ts',
            'scripts/**',
        ],
    },
    {
        files: ['**/*.ts'],
        languageOptions: {
            parser: tsParser,
            parserOptions: {
                project: ['tsconfig.json'],
                tsconfigRootDir: __dirname,
            },
        },
        plugins: {
            '@angular-eslint': angular,
            '@typescript-eslint': tseslint,
            prettier: prettierPlugin,
        },
        rules: {
            // Spread the canonical recommended sets first, then override below.
            ...tseslint.configs.recommended.rules,
            ...angular.configs.recommended.rules,
            ...prettierConfig.rules,

            'prettier/prettier': 'error',

            // Angular rules
            '@angular-eslint/component-class-suffix': ['error'],
            '@angular-eslint/component-selector': [
                'error',
                {
                    type: 'element',
                    prefix: ['app', 'sb', 'hermanas'],
                    style: 'kebab-case',
                },
            ],
            '@angular-eslint/contextual-lifecycle': ['error'],
            '@angular-eslint/directive-class-suffix': ['error'],
            '@angular-eslint/directive-selector': [
                'error',
                {
                    type: 'attribute',
                    prefix: ['app', 'sb', 'hermanas'],
                    style: 'camelCase',
                },
            ],
            '@angular-eslint/no-conflicting-lifecycle': ['error'],
            '@angular-eslint/no-input-rename': ['error'],
            '@angular-eslint/prefer-standalone': 'error',
            '@angular-eslint/prefer-inject': 'off',
            '@angular-eslint/no-inputs-metadata-property': ['error'],
            '@angular-eslint/no-output-native': ['error'],
            '@angular-eslint/no-output-on-prefix': ['error'],
            '@angular-eslint/no-output-rename': ['error'],
            '@angular-eslint/no-outputs-metadata-property': ['error'],
            '@angular-eslint/use-lifecycle-interface': ['error'],
            '@angular-eslint/use-pipe-transform-interface': ['error'],

            // TypeScript rules
            '@typescript-eslint/consistent-type-assertions': [
                'error',
                { assertionStyle: 'as' },
            ],
            '@typescript-eslint/explicit-member-accessibility': [
                'off',
                { accessibility: 'explicit' },
            ],
            '@typescript-eslint/member-ordering': [
                'error',
                {
                    default: [
                        'static-field',
                        'instance-field',
                        'static-method',
                        'instance-method',
                    ],
                },
            ],
            '@typescript-eslint/no-empty-function': 'off',
            '@typescript-eslint/no-empty-object-type': 'off',
            '@typescript-eslint/no-explicit-any': 'off',
            '@typescript-eslint/no-inferrable-types': ['error', { ignoreParameters: true }],
            '@typescript-eslint/no-non-null-assertion': ['error'],
            '@typescript-eslint/no-require-imports': 'off',
            '@typescript-eslint/no-unused-vars': [
                'error',
                {
                    argsIgnorePattern: '^_',
                    varsIgnorePattern: '^_',
                    caughtErrorsIgnorePattern: '^_',
                },
            ],
            '@typescript-eslint/no-var-requires': 'off',
            '@typescript-eslint/no-wrapper-object-types': 'off',
            '@typescript-eslint/no-unsafe-function-type': 'off',

            // Core rules
            'no-console': ['error', { allow: ['error'] }],
            'no-duplicate-imports': ['error'],
            'no-fallthrough': ['error'],
            'max-len': [
                'error',
                {
                    code: 140,
                    ignorePattern: '^import |^export ',
                    ignoreUrls: true,
                },
            ],
            quotes: [
                'error',
                'single',
                { allowTemplateLiterals: true },
            ],
            'prefer-const': 'error',
            'no-var': 'error',
        },
    },
    {
        files: ['**/*.html'],
        languageOptions: {
            parser: angularTemplateParser,
        },
        plugins: {
            '@angular-eslint/template': angularTemplate,
            prettier: prettierPlugin,
        },
        rules: {
            ...angularTemplate.configs.recommended.rules,
            ...prettierConfig.rules,

            'prettier/prettier': 'error',
            '@angular-eslint/template/banana-in-box': ['error'],
            '@angular-eslint/template/no-negated-async': ['error'],
        },
    },
];
