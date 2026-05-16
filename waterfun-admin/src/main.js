import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import '@/assets/global.css'
import "element-plus/dist/index.css"
import App from './App.vue'
import i18n from "@/utils/i18n";
import router from "@/router/index";
import {createPinia} from "pinia";
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';
import ECharts from 'vue-echarts'
import { useAuthStore } from '@/stores/authStore';

const pinia = createPinia();
pinia.use(piniaPluginPersistedstate);

const app = createApp(App)
app.component('VChart', ECharts)
app.use(pinia)
app.use(router)
app.use(ElementPlus)
    .use(i18n)

const authStore = useAuthStore();
authStore.restore();

app.mount('#app')

