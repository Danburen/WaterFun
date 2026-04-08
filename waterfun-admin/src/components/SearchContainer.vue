<template>
  <div class="default-table-container search-container">
    <div class="search-content">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.search-container {
  padding: 12px;
}

.search-content {
  display: flex;
  width: 100%;
}

.search-content :deep(.search-form) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  width: 100%;
}

.search-content :deep(.search-form .el-form-item) {
  margin-bottom: 5px;
}

</style>
