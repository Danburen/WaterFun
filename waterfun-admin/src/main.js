import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@/assets/global.css'
import App from './App.vue'
import i18n from "@/utils/i18n";
import router from "@/router";
import {createPinia} from "pinia";
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

const app = createApp(App)
app.use(ElementPlus)
    .use(i18n)
    .use(router)
    .use(pinia)
app.mount('#app')
