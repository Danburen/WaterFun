import { fileURLToPath, URL } from 'url' // 移除 node: 前缀
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue'],
      dts: 'src/auto-imports.d.ts', // 生成自动导入的类型文件
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts', // 生成组件类型文件
    }),
  ],
  assetsInclude: ['**/*.svg'],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '~': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
