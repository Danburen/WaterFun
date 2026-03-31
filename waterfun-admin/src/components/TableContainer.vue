

<script setup lang="ts">
import { computed } from 'vue';
import {Pagination} from "~/types";

// Props
const props = withDefaults(defineProps<{
  title: string;
  showAddButton?: boolean;
  addButtonText?: string;
  showPagination?: boolean;
  pagination?: Pagination;
  total?: number;
  pageSize?: number;
  currentPage?: number;
  pageSizes?: number[];
}>(), {
  showAddButton: false,
  addButtonText: 'btn.create',
  showPagination: true,
  pagination: {
    size: 10,
    number: 1,
    totalElements: 0,
    totalPages: 0,
  },
  pageSizes: () => [10, 20, 50, 100],
  total: 0,
  pageSize: 10,
  currentPage: 1
});

const emit = defineEmits<{
  add: [];
  'update:pageSize': [size: number];
  'update:currentPage': [page: number];
  change: [];
}>();

const localPageSize = computed({
  get: () => props.pageSize,
  set: val => {
    emit('update:pageSize', val)
    emit('change')
  }
});
const localCurrentPage = computed({
  get: () => props.currentPage,
  set: val => {
    emit('update:currentPage', val)
    emit('change')
  }
})

</script>
<template>
  <div class="base-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>{{ title }}</h3>
          </div>
          <div class="header-right">
            <slot name="action-buttons">
              <el-button v-if="showAddButton" type="primary" @click="$emit('add')">
                {{ addButtonText }}
              </el-button>
            </slot>
          </div>
        </div>
      </template>

      <div class="card-content">
        <slot></slot>
      </div>

      <div v-if="showPagination" class="pagination-container">
        <el-pagination
          v-model:current-page="localCurrentPage"
          v-model:page-size="localPageSize"
          :page-sizes="pageSizes"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.base-list-container {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
}

.header-left {
  flex: 1;
}

.header-left h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

.header-right {
  display: flex;
  gap: 10px;
}

.card-content {
  padding: 20px 0;
}

.pagination-container {
  margin-top: 20px;
  text-align: center;
  padding: 0 20px 20px 20px;
}
</style>