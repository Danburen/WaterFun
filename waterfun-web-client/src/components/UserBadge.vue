<script setup lang="ts">
defineProps<{
  isPostAuthor?: boolean
  authorUid?: string
  currentUid?: string
}>()
</script>

<template>
  <span class="user-badge-list">
    <span v-if="isPostAuthor" class="user-badge author-badge">作者</span>
    <span v-if="currentUid && authorUid && currentUid === authorUid" class="user-badge me-badge">我</span>
  </span>
</template>

<style scoped>
.user-badge-list {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  vertical-align: middle;
}

.user-badge {
  display: inline-flex;
  align-items: center;
  padding: 0 6px;
  height: 18px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.author-badge {
  background: #fef3c7;
  color: #b45309;
}

.me-badge {
  background: #dbeafe;
  color: #1d4ed8;
}
</style>
