<script setup lang="ts">
const props = withDefaults(defineProps<{
  title?: string
  loading?: boolean
  total?: number
  page?: number
  pageSize?: number
  pageSizes?: number[]
}>(), {
  title: '',
  loading: false,
  total: 0,
  page: 1,
  pageSize: 20,
  pageSizes: () => [10, 20, 50, 100],
})

const emit = defineEmits<{
  'update:page': [v: number]
  'update:pageSize': [v: number]
  change: []
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const startPage = ref(0)
const endPage = ref(0)

watch([() => props.page, () => totalPages.value], () => {
  const total = totalPages.value
  const current = props.page
  let start = Math.max(0, current - 2)
  let end = Math.min(total - 1, current + 2)
  if (end - start < 4) {
    if (start === 0) end = Math.min(total - 1, start + 4)
    else start = Math.max(0, end - 4)
  }
  startPage.value = start
  endPage.value = end
}, { immediate: true })

const goToPage = (p: number) => {
  if (p < 1 || p > totalPages.value || p === props.page) return
  emit('update:page', p)
  emit('change')
}

const pageSizeOptions = computed(() => props.pageSizes)

const emitPageSize = (size: number) => {
  emit('update:pageSize', size)
  emit('update:page', 1)
  emit('change')
}
</script>

<template>
  <div class="list-page">
    <div v-if="$slots.search || $slots.header" class="list-header">
      <div v-if="$slots.search" class="search-area">
        <slot name="search" />
      </div>
      <div v-if="$slots.header" class="header-actions">
        <slot name="header" />
      </div>
    </div>

    <div class="card" style="position: relative;">
      <div v-if="loading" class="loading-overlay">
        <i class="fa-solid fa-spinner fa-spin"></i> 加载中...
      </div>
      <div class="table-wrapper">
        <slot />
      </div>

      <div v-if="total > 0" class="pagination-wrap">
        <div class="pagination-info">
          共 {{ total }} 条
          <span v-if="pageSizeOptions.length > 1" class="page-size-select">
            <select :value="pageSize" @change="emitPageSize(Number(($event.target as HTMLSelectElement).value))">
              <option v-for="s in pageSizeOptions" :key="s" :value="s">{{ s }}条/页</option>
            </select>
          </span>
        </div>
        <div class="pagination">
          <button :class="['page-btn', { disabled: page <= 1 }]" @click="goToPage(page - 1)">
            <i class="fa-solid fa-chevron-left"></i>
          </button>
          <template v-if="startPage > 0">
            <button class="page-btn" @click="goToPage(1)">1</button>
            <span v-if="startPage > 1" class="page-ellipsis">...</span>
          </template>
          <button
            v-for="p in endPage - startPage + 1"
            :key="p"
            :class="['page-btn', { active: startPage + p - 1 === page }]"
            @click="goToPage(startPage + p - 1)"
          >
            {{ startPage + p }}
          </button>
          <template v-if="endPage < totalPages - 1">
            <span class="page-ellipsis">...</span>
            <button class="page-btn" @click="goToPage(totalPages)">{{ totalPages }}</button>
          </template>
          <button :class="['page-btn', { disabled: page >= totalPages }]" @click="goToPage(page + 1)">
            <i class="fa-solid fa-chevron-right"></i>
          </button>
        </div>
      </div>
      <div v-else-if="!loading" class="empty-state">
        <slot name="empty">
          <span>暂无数据</span>
        </slot>
      </div>
    </div>
  </div>
</template>

<style scoped>
.list-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px 20px;
}

.search-area {
  width: 100%;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.loading-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255,255,255,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  color: var(--text-muted);
  font-size: 14px;
  gap: 8px;
}

.table-wrapper {
  overflow-x: auto;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
  font-size: 14px;
}

.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-top: 1px solid var(--border-light);
  flex-wrap: wrap;
  gap: 12px;
}

.pagination-info {
  font-size: 13px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-size-select select {
  padding: 4px 8px;
  border: 1px solid var(--border);
  border-radius: 4px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-white);
  cursor: pointer;
  outline: none;
}

.page-size-select select:focus {
  border-color: var(--primary);
}

.pagination {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border);
  background: var(--bg-white);
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
  padding: 0 8px;
}

.page-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.page-btn.active {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.page-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-ellipsis {
  color: var(--text-muted);
  padding: 0 4px;
  font-size: 14px;
}
</style>
