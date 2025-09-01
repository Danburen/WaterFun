<script setup lang="ts">
import router, {routes, type routeType} from '@/router'
import IconComponent from "@/components/IconComponent.vue";
const props = defineProps<{
  collapse: boolean;
}>()

const handleMenuClick = (routeName:string) => {
  router.push({ name: routeName })
}
</script>

<template>
  <el-menu class="aside-menu" default-active="1" :collapse="collapse" >
    <template v-for="(route,index) in routes">
      <el-sub-menu v-if="route.children && route.children.length" :index="index.toString()">
        <template #title>
          <el-icon class="menu-icon"><IconComponent :icon="route.meta.icon" /></el-icon>
          <span>{{ $t(`nav.${route.meta.locale}`) }}</span>
        </template>
        <el-menu-item @click="handleMenuClick(child.name)" v-for="child in route.children">
          <span>{{ $t(`nav.${child.meta.locale}`)}}</span>
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item @click="handleMenuClick(route.name)" v-else>
        <el-icon class="menu-icon"><IconComponent :icon="route.meta.icon" /></el-icon>
        <span>{{ $t(`nav.${route.meta.locale}`)}}</span>
      </el-menu-item>
    </template>
  </el-menu>
  <!--    <el-menu-item index="1">-->
  <!--      <el-icon class="menu-icon"><img src="@/assets/dashboard.svg" alt="Dashboard"></el-icon>-->
  <!--      <template #title>-->
  <!--        <span>{{ $t('nav.home') }}</span>-->
  <!--      </template>-->
  <!--    </el-menu-item>-->
  <!--    <el-sub-menu index="2">-->
  <!--      <template #title>-->
  <!--        <el-icon class="menu-icon"><Edit /></el-icon>-->
  <!--        <span>{{ $t('nav.manager') }}</span>-->
  <!--      </template>-->
  <!--      <el-menu-item><span>{{ $t('nav.userManager') }}</span></el-menu-item>-->
  <!--      <el-menu-item><span>{{ $t('nav.roleManager') }}</span></el-menu-item>-->
  <!--      <el-menu-item><span>{{ $t('nav.permissionManager') }}</span></el-menu-item>-->
  <!--    </el-sub-menu>-->
  <!--    <el-sub-menu index="3">-->
  <!--      <template #title>-->
  <!--        <el-icon class="menu-icon"><VideoCamera /></el-icon>-->
  <!--        <span>{{ $t('nav.statusMonitor') }}</span>-->
  <!--      </template>-->
  <!--      <el-menu-item><span>{{ $t('nav.serverMonitor') }}</span></el-menu-item>-->
  <!--      <el-menu-item><span>{{ $t('nav.dynamicWorld') }}</span></el-menu-item>-->
  <!--      <el-menu-item><span>{{ $t('nav.onlineUser') }}</span></el-menu-item>-->
  <!--    </el-sub-menu>-->
</template>

<style scoped>
.menu-icon{
  width: 1em;
  height: 1em;
  margin-right: 8px
}

.aside-menu {
  min-height: 100%;
}

a {
  text-decoration: none;
}
</style>