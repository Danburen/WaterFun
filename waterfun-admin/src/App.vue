<script setup lang="ts">

import MainLayout from "@/layouts/MainLayout.vue";
import EmptyLayout from "@/layouts/EmptyLayout.vue";
import {computed} from "vue";
import {useRoute} from "vue-router";

const route = useRoute()
const noLayout = computed(()=> route.meta.layout == false)


const layoutComponent = computed(() => {
  return route.meta.layout === false ? EmptyLayout : MainLayout
})
</script>

<template>
  <div id="app">
    <RouterView />
    <!-- <MainLayout v-if="!noLayout" />
    <RouterView v-else /> -->
  </div>
</template>
<style scoped>
</style>
