import { createApp } from 'vue'
import 'element-plus/dist/index.css'
import '@/assets/global.css'
import App from './App.vue'
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

const authStore = useAuthStore();
authStore.restore();

router.onError((error) => {
  console.error('Router navigation error:', error);
})

app.mount('#app')

