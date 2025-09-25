<script setup lang="ts">

import MainLayout from "@/layouts/MainLayout.vue";
import {computed} from "vue";
import router from "@/router/index.js";
import {useRoute} from "vue-router";

const route = useRoute()
const noLayout = computed(()=> route.meta.public == true)
</script>

<template>
  <div id="app">
    <MainLayout v-if="!noLayout"/>
    <RouterView v-else/>
  </div>
</template>
<style scoped>
</style>
