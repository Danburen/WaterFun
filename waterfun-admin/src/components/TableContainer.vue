

<script setup lang="ts">
import { computed } from 'vue';
import {useI18n} from "vue-i18n";


const { t } = useI18n();
// Props
const props = withDefaults(defineProps<{
  title: string;
  titleI18n?: boolean;
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
  titleI18n: true,
  showTitle: true,
  showAddBtn: false,
  showPagination: true,
  showRemoveBtn: true,
  disableDelete: true,
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
  <CardContainer :title="title" :title-i18n="titleI18n" :show-title="showTitle">
    <div class="operation-bar">
      <el-button v-if="showAddBtn" type="primary" size="small" @click="$emit('add')">
        {{ t('btn.create') }}
      </el-button>
      <el-button v-if="showRemoveBtn" :disabled="disableDelete" type="danger" size="small" @click="$emit('remove')">
        {{ t('btn.delete') }}
      </el-button>
      <slot name="action-buttons" />
    </div>
    <slot name="search"></slot>
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
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
</style>