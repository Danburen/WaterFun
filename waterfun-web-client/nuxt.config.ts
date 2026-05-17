// https://nuxt.com/docs/api/configuration/nuxt-config
import {fileURLToPath} from "node:url";
export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },
  srcDir: 'src/', // resource
  runtimeConfig:{
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE,
      avatarExpireTime: process.env.NUXT_PUBLIC_AVATAR_EXPIRE_TIME || '3600' // 默认1小时
    }
  },
  // nitro: {
  //   routeRules: {
  //     '/auth/**': { proxy: process.env.NUXT_PUBLIC_API_BASE + "/auth/" }
  //   }
  // },
  alias:{
    '@': fileURLToPath(new URL('./src', import.meta.url)),
    '~': fileURLToPath(new URL('./src', import.meta.url)),
  },
  modules:['@nuxtjs/i18n',
    ['@element-plus/nuxt', {
      importStyle: 'css',
      icon: 'auto', 
    }],
    '@pinia/nuxt','pinia-plugin-persistedstate/nuxt'],
  plugins: ['@/plugins/async-validator.client'],
  i18n:{
    strategy: 'no_prefix', // no prefix router
    detectBrowserLanguage: {
        useCookie: true,     // use cookie to store language
        cookieKey: 'i18n_redirected'
    },
    locales: [
        { code: 'en' , file: "en_US.json"},
        { code: 'zh' , file: "zh_CN.json"},
    ],
    lazy: true,
    langDir: 'locales',
    bundle: {
      optimizeTranslationDirective: false // 显式禁用
    },
  },
  css: ['@/assets/styles/global.css'],
})