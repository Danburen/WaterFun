<script setup lang="ts">
import router, {menuRoutes, type routeType} from '@/router/index.js'
import IconComponent from "@/components/IconComponent.vue";
const props = defineProps<{
  collapse: boolean;
}>()

const handleMenuClick = (routeName:string) => {
  router.push({ name: routeName })
}
</script>

<template>
  <el-menu class="aside-menu" default-active="1" :collapse="collapse">
    <template v-for="(route,index) in menuRoutes">
      <el-sub-menu v-if="route.children && route.children.length" :index="index.toString()">
        <template #title>
          <el-icon class="menu-icon"><IconComponent :icon="route.meta.icon" /></el-icon>
          <span>{{ $t(`nav.${route.meta.locale}`) }}</span>
        </template>
        <el-menu-item @click="handleMenuClick(child.name)" v-for="(child,cIndex) in route.children" :index="`${index}-${cIndex.toString()}`">
          <span>{{ $t(`nav.${child.meta.locale}`)}}</span>
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item @click="handleMenuClick(route.name)" v-else :index="index.toString()">
        <el-icon class="menu-icon"><IconComponent :icon="route.meta.icon" /></el-icon>
        <span>{{ $t(`nav.${route.meta.locale}`)}}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<style scoped>
.menu-icon{
  width: 1em;
  height: 1em;
  margin-right: 8px
}

.aside-menu {
  min-height: 100%;
  width: 175px;
}

a {
  text-decoration: none;
}
</style>