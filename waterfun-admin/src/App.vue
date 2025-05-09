<script setup>
import Menu from './client/views/Community.vue'
import login from './client/views/login.vue'
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.js'
import HeaderNavMenu from "@/client/HeaderNavMenu.vue";
</script>

<template>
  <div class="header" v-if="!$route.meta.hideHeader">
    <HeaderNavMenu></HeaderNavMenu>
  </div>
  <main :style="{ marginTop: !$route.meta.hideHeader ? '70px' : '0' }">
    <router-view></router-view>
  </main>
</template>

<style scoped>
body{
  height: 100%;
}
</style>