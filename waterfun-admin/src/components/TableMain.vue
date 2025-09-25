<script setup lang="ts">
import {computed} from "vue";
import type {TableDatasets} from "@/components/types.js";
//@ts-ignore
import { DocumentRemove  } from "@element-plus/icons-vue";

const props = defineProps<{
  datasets?: TableDatasets;
  loading?: boolean;
}>();

const emptyColumns = computed(() =>
    props.datasets?.columns || Array(5).fill({ prop: '', label: '' })
);

const columns  = computed(() => {
  if(props.datasets?.columns) {
    
  }
})
</script>

<template>
  <div class="container">
    <div class="toolbar">
      <div class="op-group flex items-center">
        <el-button class="btn" color="var(--default-green)">{{ $t('btn.create') }}</el-button>
        <el-button class="btn" color="var(--default-blue)">{{ $t('btn.update') }}</el-button>
        <el-button class="btn" color="var(--default-red)">{{ $t('btn.delete') }}</el-button>
      </div>
    </div>
    <el-table
        class="table"
        :data="props.datasets?.data || []"
        v-loading="props.loading"
        element-loading-text="加载中..."
    >
      <template v-if="props.loading" #em0pty>
        <el-skeleton :rows="5" animated />
      </template>

      <el-table-column
          v-for="(item, index) in emptyColumns"
          :key="index"
          :prop="item.prop"
          :label="item.label"
          :width="item?.width"
      >
        <template v-if="props.loading" #default>
          <el-skeleton-item variant="text" />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.toolbar {
  width: 100%;
  height: 35px;
  display: flex;
  flex-shrink: 0
}

.op-group {
  float: left;
  height: 35px;
  gap: 5px;
}

.btn  {
  height: 26px;
  font-size: 12px;
  color: var(--default-white);
  margin: 0 0;
}

.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  color: var(--el-text-color-secondary);
}
</style>