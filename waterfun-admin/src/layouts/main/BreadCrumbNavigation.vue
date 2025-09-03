<script setup lang="ts">
import { watch } from 'vue';
import { useBreadcrumbs } from '@/composables/useBreadcrumbs.js';
import {useRoute} from "vue-router";

const bcStore = useBreadcrumbs();
</script>

<template>
  <el-breadcrumb class="bread-nav" separator="/">
    <el-breadcrumb-item v-for="item in bcStore.breadcrumbs.value" :to="item.to">{{ $t(item.locale) }}</el-breadcrumb-item>
  </el-breadcrumb>
</template>

<style scoped>

</style>