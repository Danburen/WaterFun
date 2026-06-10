<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  title: string;
  showAddBtn?: boolean;
  showRemoveBtn?: boolean;
  disableDelete?: boolean;
  showPagination?: boolean;
  showTitle?: boolean;
  total?: number;
  pageSize?: number;
  currentPage?: number;
  pageSizes?: number[];
}>(), {
  showTitle: true, showAddBtn: false, showPagination: true, showRemoveBtn: true,
  disableDelete: true, pageSizes: () => [10, 20, 50, 100], total: 0, pageSize: 10, currentPage: 1
});

const emit = defineEmits<{ add: []; remove: []; 'update:pageSize': [size: number]; 'update:currentPage': [page: number]; change: [] }>();

const localPageSize = computed({ get: () => props.pageSize, set: v => { emit('update:pageSize', v); emit('change'); } });
const localCurrentPage = computed({ get: () => props.currentPage, set: v => { emit('update:currentPage', v); emit('change'); } });

const pageInput = ref(props.currentPage);

const goPage = (p: number) => {
  if (p < 1 || p > Math.ceil(props.total / props.pageSize)) return;
  localCurrentPage.value = p;
  pageInput.value = p;
};
</script>

<template>
  <CardContainer :title="title" :show-title="showTitle">
    <div class="operation-bar" v-if="showAddBtn || showRemoveBtn || $slots['action-buttons']">
      <button v-if="showAddBtn" class="btn btn-primary" @click="$emit('add')">新增</button>
      <button v-if="showRemoveBtn" class="btn btn-danger" :disabled="disableDelete" @click="$emit('remove')">删除</button>
      <slot name="action-buttons" />
    </div>
    <slot name="search" />
    <slot />
    <div v-if="showPagination && total > 0" class="pagination-container">
      <div class="pagination-info">共 {{ total }} 条</div>
      <div class="pagination">
        <select v-model.number="localPageSize" class="page-size-select" @change="$emit('change')">
          <option v-for="s in pageSizes" :key="s" :value="s">{{ s }} 条/页</option>
        </select>
        <button class="page-btn" :disabled="localCurrentPage <= 1" @click="goPage(localCurrentPage - 1)"><i class="fa-solid fa-chevron-left"></i></button>
        <button v-for="p in Math.ceil(total / pageSize)" :key="p" :class="['page-btn', { active: localCurrentPage === p }]" @click="goPage(p)">{{ p }}</button>
        <button class="page-btn" :disabled="localCurrentPage >= Math.ceil(total / pageSize)" @click="goPage(localCurrentPage + 1)"><i class="fa-solid fa-chevron-right"></i></button>
        <span class="page-jump">跳至 <input v-model.number="pageInput" type="number" min="1" class="page-jump-input" @keyup.enter="goPage(pageInput)" /> 页</span>
      </div>
    </div>
  </CardContainer>
</template>

<style scoped>
.operation-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.pagination-container { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; padding: 0 20px 20px; }
.pagination-info { font-size: 13px; color: var(--text-muted); }
.pagination { display: flex; align-items: center; gap: 6px; }
.page-size-select { padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; background: var(--bg-white); outline: none; }
.page-btn { min-width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border: 1px solid var(--border); background: var(--bg-white); border-radius: var(--radius-sm); font-size: 13px; color: var(--text-secondary); cursor: pointer; transition: all 0.2s; }
.page-btn:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
.page-btn.active { background: var(--primary); color: white; border-color: var(--primary); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-jump { font-size: 13px; color: var(--text-muted); display: flex; align-items: center; gap: 4px; }
.page-jump-input { width: 48px; padding: 5px 6px; border: 1px solid var(--border); border-radius: 4px; font-size: 13px; text-align: center; outline: none; }
</style>
