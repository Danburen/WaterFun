import globals from 'globals';
import js from '@eslint/js';
import ts from 'typescript-eslint';

export default [
  js.configs.recommended,
  ...ts.configs.recommended,
  {
    languageOptions: {
    globals: {
        console: 'readonly',
        window: 'readonly',
        document: 'readonly',
        ...globals.browser,
      }
    },
    rules: {
      'no-console': 'warn',
      '@typescript-eslint/no-unused-vars': 'error',
      'no-console': ['warn', { allow: ['error', 'warn'] }]
    }
  }
];