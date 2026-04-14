<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import {
  deletePerms,
  deletePermission,
  getPermOptions,
  listPermissions,
  type PermissionResp,
  type PermissionType,
} from "~/api/permission";
import type { PageOptions } from "~/types";
import PermEditDialog from "./components/PermEditDialog.vue";

const { t } = useI18n();
const router = useRouter();

const permissionList = ref<PermissionResp[]>([]);
const permOptions = ref<OptionResItem[]>([]);

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

const loading = ref(false);
const dialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentPermId = ref<number | null>(null);
const selectedPermIds = ref<number[]>([]);

const permTypeOptions: { label: string; value: PermissionType }[] = [
  { label: "permission.type.menu", value: "MENU" },
  { label: "permission.type.button", value: "BUTTON" },
  { label: "permission.type.api", value: "API" },
  { label: "permission.type.data", value: "DATA" },
  { label: "permission.type.other", value: "OTHER" },
];

const selectable = (row: PermissionResp) => row.isSystem === false;

const fetchPermOptions = async () => {
  try {
    const res = await getPermOptions();
    permOptions.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("permission.error.fetch"));
  }
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

    permissionList.value = res.data.content || [];
    pageOpts.value.total = res.data.page.totalElements || 0;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("error.fetch"));
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  dialogMode.value = "create";
  currentPermId.value = null;
  dialogVisible.value = true;
};

const handleEdit = (row: PermissionResp) => {
  dialogMode.value = "edit";
  currentPermId.value = row.id;
  dialogVisible.value = true;
};

const handleDelete = async (row: PermissionResp) => {
  try {
    await ElMessageBox.confirm(t("permission.confirm.delete"), t("operation.delete"), {
      type: "warning",
    });
    await deletePermission(row.id);
    ElMessage.success(t("permission.success.delete"));
    await fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error(t("permission.error.delete"));
    }
  }
};

const handleSelectionChange = (rows: PermissionResp[]) => {
  selectedPermIds.value = rows.map(item => item.id);
};

const handleBatchDelete = async () => {
  if (selectedPermIds.value.length === 0) {
    return;
  }

  try {
    await ElMessageBox.confirm(
      t("permission.confirm.batchDelete", { count: selectedPermIds.value.length }),
      t("operation.delete"),
      { type: "warning" }
    );

    const res = await deletePerms(selectedPermIds.value);
    const result = res.data;

    if (result.success === result.requested) {
      ElMessage.success(t("permission.success.delete"));
    } else if (result.success === 0) {
      ElMessage.error(t("permission.error.delete"));
    } else {
      ElMessage.warning(`${t("permission.success.delete")} ${result.success}/${result.requested}`);
    }

    selectedPermIds.value = [];
    await fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error(t("permission.error.delete"));
    }
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

const handleDialogSuccess = async () => {
  await Promise.all([fetchData(), fetchPermOptions()]);
};

const gotoDetail = (row: PermissionResp | number) => {
  const id = typeof row === "number" ? row : row.id;
  router.push({ name: "permissionDetail", params: { id } });
};

onMounted(async () => {
  await Promise.all([fetchData(), fetchPermOptions()]);
});
</script>

<template>
  <div class="list-layout">
    <SearchContainer>
      <el-form inline class="search-form" :model="searchForm">
        <el-form-item :label="t('permission.name')">
          <el-input v-model="searchForm.name" :placeholder="t('permission.input.name')" />
        </el-form-item>

        <el-form-item :label="t('permission.code')">
          <el-input v-model="searchForm.code" :placeholder="t('permission.input.code')" />
        </el-form-item>

        <el-form-item :label="t('permission.type.title')">
          <el-select v-model="searchForm.type" clearable style="width: 140px">
            <el-option v-for="item in permTypeOptions" :key="item.value" :label="t(item.label)" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('permission.resource')">
          <el-input v-model="searchForm.resource" :placeholder="t('permission.input.resource')" />
        </el-form-item>

        <el-form-item :label="t('permission.parentId')">
          <el-select
            v-model="searchForm.parentId"
            clearable
            :placeholder="t('permission.input.parentId')"
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
          <el-button type="primary" @click="handleSearch">{{ t('query.title') }}</el-button>
          <el-button @click="handleReset">{{ t('reset.title') }}</el-button>
        </el-form-item>
      </el-form>
    </SearchContainer>

    <TableContainer
      title="permission.title"
      showAddBtn
      :show-remove-btn="true"
      :disable-delete="selectedPermIds.length === 0"
      @add="handleAdd"
      @remove="handleBatchDelete"
      @change="fetchData"
      :total="pageOpts.total"
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
    >

    <el-table
      v-loading="loading"
      :data="permissionList"
      border
      fit
      highlight-current-row
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" :selectable="selectable" width="55" />
      <el-table-column prop="id" label="ID" width="80" />

      <el-table-column prop="name" :label="t('permission.name')" min-width="140">
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="gotoDetail(row)">{{ row.name }}</el-link>
        </template>
      </el-table-column>

      <el-table-column prop="code" :label="t('permission.code')" min-width="140" />

      <el-table-column prop="type" :label="t('permission.type.title')" width="110">
        <template #default="{ row }">
          {{ t(`permission.type.${row.type?.toLowerCase?.() || 'other'}`) }}
        </template>
      </el-table-column>

      <el-table-column prop="resource" :label="t('permission.resource')" min-width="180" show-overflow-tooltip />

      <el-table-column prop="parentId" :label="t('permission.parentId')" width="120">
        <template #default="{ row }">
          <el-link v-if="row.parentId != null" type="primary" :underline="false" @click="gotoDetail(row.parentId)">
            {{ row.parentId }}
          </el-link>
          <span v-else>{{ t('none.title') }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="orderWeight" :label="t('permission.weight')" width="90" />

      <el-table-column prop="isSystem" :label="t('permission.isSystem')" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.isSystem ? 'warning' : 'info'">
            {{ row.isSystem ? t('yes.title') : t('no.title') }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="description" :label="t('permission.description')" min-width="180" show-overflow-tooltip />

      <el-table-column prop="createdAt" :label="t('create.time')" min-width="170">
        <template #default="{ row }">
          <span>{{ formatISOData(row.createdAt) }}</span>
        </template>
      </el-table-column>

      <el-table-column :label="t('operation.title')" width="150" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" @click="handleEdit(scope.row)">{{ t('operation.edit') }}</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row)">{{ t('operation.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

      <PermEditDialog
        v-model="dialogVisible"
        :mode="dialogMode"
        :permission-id="currentPermId"
        :perm-options="permOptions"
        :disabled-parent-ids="currentPermId ? [currentPermId] : []"
        @success="handleDialogSuccess"
      />
    </TableContainer>
  </div>
</template>

<style scoped>
.search-form {
  margin-bottom: 0;
}

.list-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>

