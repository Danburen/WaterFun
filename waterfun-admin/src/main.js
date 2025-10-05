import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import '@/assets/global.css'
import App from './App.vue'
import i18n from "@/utils/i18n";
import router from "@/router/index";
import {createPinia} from "pinia";

const pinia = createPinia()

const app = createApp(App)
app.use(ElementPlus)
    .use(i18n)
    .use(router)
    .use(pinia)

app.mount('#app')
