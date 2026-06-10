<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    title: string;
    showPagination?: boolean;
    total?: number;
    pageSize?: number;
    currentPage?: number;
    pageSizes?: number[];
    back?: boolean;
  }>(),
  {
    showPagination: true,
    total: 0,
    pageSize: 10,
    currentPage: 1,
    pageSizes: () => [10, 20, 50, 100],
    back: false,
  }
);

const emit = defineEmits<{
  "update:pageSize": [size: number];
  "update:currentPage": [page: number];
  change: [];
  back: [];
}>();

const localPageSize = computed({
  get: () => props.pageSize,
  set: (val) => {
    emit("update:pageSize", val);
    emit("change");
  },
});

const localCurrentPage = computed({
  get: () => props.currentPage,
  set: (val) => {
    emit("update:currentPage", val);
    emit("change");
  },
});

const totalPages = computed(() =>
  Math.max(1, Math.ceil(props.total / props.pageSize))
);

const pageNumbers = computed(() => {
  const total = totalPages.value;
  const current = localCurrentPage.value;
  const pages: number[] = [];
  const start = Math.max(1, current - 2);
  const end = Math.min(total, current + 2);
  for (let i = start; i <= end; i++) pages.push(i);
  return pages;
});

const goPrev = () => {
  if (localCurrentPage.value > 1) {
    localCurrentPage.value = localCurrentPage.value - 1;
  }
};

const goNext = () => {
  if (localCurrentPage.value < totalPages.value) {
    localCurrentPage.value = localCurrentPage.value + 1;
  }
};

const goPage = (p: number) => {
  localCurrentPage.value = p;
};
</script>

<template>
  <CardContainer
    :title="title"
    :back="back"
    @back="emit('back')"
  >
    <template #header-right>
      <slot name="header-right" />
    </template>

    <slot />

    <div v-if="showPagination" class="pagination-wrap">
      <span class="pagination-info">共 {{ total }} 条</span>
      <div class="pagination">
        <select v-model="localPageSize" class="page-size-select">
          <option v-for="s in pageSizes" :key="s" :value="s">{{ s }} 条/页</option>
        </select>
        <button class="page-btn" :disabled="localCurrentPage <= 1" @click="goPrev"><i class="fa-solid fa-chevron-left"></i></button>
        <button v-for="p in pageNumbers" :key="p" :class="['page-btn', { active: localCurrentPage === p }]" @click="goPage(p)">{{ p }}</button>
        <button class="page-btn" :disabled="localCurrentPage >= totalPages" @click="goNext"><i class="fa-solid fa-chevron-right"></i></button>
      </div>
    </div>
  </CardContainer>
</template>

<style scoped>
.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid var(--border-light);
}

.pagination-info {
  font-size: 13px;
  color: var(--text-muted);
}

.pagination {
  display: flex;
  gap: 4px;
  align-items: center;
}

.page-size-select {
  height: 30px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: var(--bg-white);
  padding: 0 8px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-right: 8px;
}

.page-btn {
  min-width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border);
  background: var(--bg-white);
  border-radius: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
}

.page-btn.active {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
