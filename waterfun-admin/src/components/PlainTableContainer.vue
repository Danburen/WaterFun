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
</script>

<template>
  <CardContainer :title="title" :back="back" @back="emit('back')">
    <template #header-right>
      <slot name="header-right" />
    </template>

    <slot />

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
.pagination-container {
  margin-top: 10px;
  text-align: center;
  padding: 0 20px 20px 20px;
}
</style>

