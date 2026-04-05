

<script setup lang="ts">
import { computed } from 'vue';
import {Pagination} from "~/types";
import i18n from "../utils/i18n";
import {useI18n} from "vue-i18n";


const { t } = useI18n();
// Props
const props = withDefaults(defineProps<{
  title: string;
  showAddBtn?: boolean;
  showRemoveBtn?: boolean;
  disableDelete?: boolean;
  showPagination?: boolean;
  pagination?: Pagination;
  total?: number;
  pageSize?: number;
  currentPage?: number;
  pageSizes?: number[];
}>(), {
  showAddBtn: false,
  showPagination: true,
  showRemoveBtn: true,
  disableDelete: true,
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
  remove: [];
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
  <CardContainer :title="title">
    <div class="operation-bar">
      <slot name="action-buttons">
        <el-button v-if="showAddBtn" type="primary" size="small" @click="$emit('add')">
          {{ t('btn.create') }}
        </el-button>
        <el-button v-if="showRemoveBtn" :disabled="disableDelete" type="danger" size="small" @click="$emit('remove')">
          {{ t('btn.delete') }}
        </el-button>
      </slot>
    </div>
    <slot></slot>
    <div v-if="showPagination" class="pagination-container">
      <el-pagination
          v-model:current-page="localCurrentPage"
          v-model:page-size="localPageSize"
          :page-sizes="pageSizes"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
      />
    </div>
  </CardContainer>
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

.table-content {
  padding: 15px;
}

.pagination-container {
  margin-top: 10px;
  text-align: center;
  padding: 0 20px 20px 20px;
}

.operation-bar {
  margin-bottom: 10px;
}
</style>