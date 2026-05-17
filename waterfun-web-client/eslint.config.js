import rootConfig from '../../eslint.config.js';
import vue from 'eslint-plugin-vue';

export default [
  ...rootConfig,
  ...vue.configs['flat/recommended'],
  {
    files: ['**/*.vue'],
    languageOptions: {
      parser: await import('vue-eslint-parser').then(m => m.default),
      parserOptions: {
        parser: await import('@typescript-eslint/parser').then(m => m.default),
        sourceType: 'module'
      }
    }
  },
  {
    rules: {
      'vue/multi-word-component-names': 'off'
    }
  }
];