<script setup lang="ts">
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import {
  getPermOptions,
  listPermissions,
  type PermissionResp,
  type PermissionType,
} from "~/api/permission";
import type { PageOptions } from "~/types";
import { ElMessage } from "element-plus";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    disabledPermissionIds?: number[];
  }>(),
  {
    disabledPermissionIds: () => [],
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  confirm: [ids: number[]];
}>();


const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});

const loading = ref(false);
const tableData = ref<PermissionResp[]>([]);
const selectedIds = ref<number[]>([]);
const permOptions = ref<{ id: number; name: string; code?: string; disabled?: boolean }[]>([]);

const searchForm = ref<{
  name: string;
  code: string;
  type: PermissionType | "";
  resource: string;
  parentId: number | null;
}>({
  name: "",
  code: "",
  type: "",
  resource: "",
  parentId: null,
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const permTypeOptions: { label: string; value: PermissionType }[] = [
  { label: "菜单", value: "MENU" },
  { label: "按钮", value: "BUTTON" },
  { label: "接口", value: "API" },
  { label: "数据", value: "DATA" },
  { label: "其他", value: "OTHER" },
];

const selectable = (row: PermissionResp) => !props.disabledPermissionIds.includes(row.id);

const fetchPermOptions = async () => {
  const res = await getPermOptions();
  permOptions.value = res.data || [];
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listPermissions({
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      name: searchForm.value.name || undefined,
      code: searchForm.value.code || undefined,
      type: searchForm.value.type || undefined,
      resource: searchForm.value.resource || undefined,
      parentId: searchForm.value.parentId ?? undefined,
    });
    tableData.value = res.data.content || [];
    pageOpts.value.total = res.data.page.totalElements || 0;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取权限信息失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pageOpts.value.currentPage = 1;
  fetchData();
};

const handleReset = () => {
  pageOpts.value.currentPage = 1;
  searchForm.value = {
    name: "",
    code: "",
    type: "",
    resource: "",
    parentId: null,
  };
  fetchData();
};

const handleSelectionChange = (rows: PermissionResp[]) => {
  selectedIds.value = rows.map((r) => r.id);
};

const handleConfirm = () => {
  emit("confirm", selectedIds.value);
  visible.value = false;
};

watch(
  () => visible.value,
  async (open) => {
    if (!open) return;
    selectedIds.value = [];
    pageOpts.value.currentPage = 1;
    await Promise.all([fetchPermOptions(), fetchData()]);
  }
);
</script>

<template>
  <el-dialog
    v-model="visible"
    title="选择权限"
    width="1250"
    destroy-on-close
  >
    <div class="picker-layout">
      <SearchContainer>
        <el-form
          inline
          :model="searchForm"
          class="search-form"
        >
          <el-form-item label="权限名称">
            <el-input
              v-model="searchForm.name"
              placeholder="请输入权限名称"
            />
          </el-form-item>
          <el-form-item label="权限编码">
            <el-input
              v-model="searchForm.code"
              placeholder="请输入权限编码"
            />
          </el-form-item>
          <el-form-item label="权限类型">
            <el-select
              v-model="searchForm.type"
              clearable
              style="width: 140px"
            >
              <el-option
                v-for="item in permTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="资源标识">
            <el-input
              v-model="searchForm.resource"
              placeholder="请输入资源标识"
            />
          </el-form-item>
          <el-form-item label="父级权限ID">
            <el-select
              v-model="searchForm.parentId"
              clearable
              placeholder="请选择父级权限"
              style="width: 180px"
            >
              <el-option
                v-for="item in permOptions"
                :key="item.id"
                :label="`${item.id} (${item.name} 【${item.code}】)`"
                :value="item.id"
                :disabled="item.disabled || false"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              @click="handleSearch"
            >
              查询
            </el-button>
            <el-button @click="handleReset">
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </SearchContainer>

      <TableContainer
        v-model:page-size="pageOpts.pageSize"
        v-model:current-page="pageOpts.currentPage"
        title="权限列表"
        :show-add-btn="false"
        :show-remove-btn="false"
        :total="pageOpts.total"
        @change="fetchData"
      >
        <el-table
          v-loading="loading"
          :data="tableData"
          fit
          highlight-current-row
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column
            type="selection"
            :selectable="selectable"
            width="55"
          />
          <el-table-column
            prop="id"
            label="ID"
            width="80"
          />
          <el-table-column
            prop="name"
            label="权限名称"
            min-width="160"
          />
          <el-table-column
            prop="code"
            label="权限编码"
            min-width="160"
          />
          <el-table-column
            prop="type"
            label="权限类型"
            width="120"
          >
            <template #default="{ row }">
              {{ ({ menu: '菜单', button: '按钮', api: '接口', data: '数据', other: '其他' })[row.type?.toLowerCase?.() || 'other'] }}
            </template>
          </el-table-column>
          <el-table-column
            prop="resource"
            label="资源标识"
            min-width="220"
            show-overflow-tooltip
          />
        </el-table>
      </TableContainer>

      <div class="dialog-extra">
        <slot
          name="footer-extra"
          :selected-ids="selectedIds"
        />
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">
        取消
      </el-button>
      <el-button
        type="primary"
        :disabled="selectedIds.length === 0"
        @click="handleConfirm"
      >
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.picker-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dialog-extra {
  padding-top: 4px;
}
</style>

