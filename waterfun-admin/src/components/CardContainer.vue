<script setup lang="ts">
const props = withDefaults(defineProps<{
  title: string;
  showTitle?: boolean;
  fixedHeader?: boolean;
}>(), {
  showTitle: true,
  fixedHeader: false,
})
</script>

<template>
  <div class="table-container">
    <div
      v-if="props.showTitle"
      :class="['header', { 'is-fixed': props.fixedHeader }]"
    >
      <div class="table-header">
        <div class="header-left">
          <span>{{ title }}</span>
        </div>
        <div class="header-right">
          <slot name="header-right" />
        </div>
      </div>
      <div class="header-divider" />
    </div>
    <div class="table-content">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.table-container {
  border: 1px solid #ccc;
  box-shadow: #ccc 2px 2px 12px;
  border-radius: 4px;
  width: 100%;
  background: #fff;
}

.header.is-fixed {
  position: sticky;
  top: -1px;
  z-index: 100;
  background: #fff;
  margin: -1px -1px 0;
  padding: 0 1px;
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
  margin: 0;
}

.table-content {
  padding: 15px;
}

.header-right {
  display: flex;
  gap: 10px;
}

</style>