import rootConfig from '../eslint.config.js';
import vue from 'eslint-plugin-vue';
import tseslint from 'typescript-eslint';
import vueParser from 'vue-eslint-parser';
import autoImportJson from './.eslintrc-auto-import.json' with { type: 'json' };

const autoImportGlobals = {
  languageOptions: {
    globals: autoImportJson.globals || {}
  }
};

export default [
  autoImportGlobals,
  ...rootConfig,
  ...vue.configs['flat/recommended'],
  {
    files: ['**/*.vue'],
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: tseslint.parser,
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