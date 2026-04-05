<script setup lang="ts">

import {useI18n} from "vue-i18n";

const {t} = useI18n();
defineProps<{
  title: string
}>()
</script>

<template>
  <div class="table-container">
    <div class="table-header">
      <div class="header-left">
        <span>{{ t(title) }}</span>
      </div>
        <slot class="header-right" name="header-right"></slot>
    </div>
    <div class="header-divider"></div>
    <div class="table-content">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.table-container {
  border: 1px solid #ccc;
  box-shadow: #ccc 2px 2px 12px;
  border-radius: 4px;
  width: 100%;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
}
.header-left {
  flex: 1;
}

.header-divider {
  height: 1px;
  background-color: #ccc;
  margin: 0; /* 控制边距 */
}

.table-content {
  padding: 15px;
}

.header-right {
  display: flex;
  gap: 10px;
}

</style>