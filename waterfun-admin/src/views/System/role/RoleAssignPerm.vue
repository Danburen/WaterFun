<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import {
  assignPermissions,
  deleteRolePerms,
  getRole,
  listRolePerms,
  type AssignedPermissionRes,
} from "~/api/role";
import type { PageOptions } from "~/types";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import SelectPermissionDialog from "../components/SelectPermissionDialog.vue";

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const roleId = computed(() => Number(route.params.id));
const roleName = ref("");

const loading = ref(false);
const tableData = ref<AssignedPermissionRes[]>([]);
const selectedIds = ref<number[]>([]);
const pickerDialogVisible = ref(false);
const assignExpiresAt = ref<Date | null>(null);

const searchForm = ref({
  name: "",
  code: "",
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const assignTitle = computed(() =>
  t("assign.forTarget", {
    target: t("role.entity"),
    name: roleName.value || roleId.value,
    item: t("permission.entity"),
  })
);

const fetchRoleBase = async () => {
  if (Number.isNaN(roleId.value)) {
    ElMessage.error(t("role.error.invalidId"));
    router.back();
    return;
  }
  try {
    const res = await getRole(roleId.value);
    roleName.value = res.data.name || "";
  } catch (e) {
    console.error(e);
    ElMessage.error(t("role.error.fetch"));
  }
};

const fetchData = async () => {
  if (Number.isNaN(roleId.value)) return;
  loading.value = true;
  try {
    const res = await listRolePerms(roleId.value, {
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      name: searchForm.value.name || undefined,
      code: searchForm.value.code || undefined,
    });
    tableData.value = res.data.content || [];
    pageOpts.value.total = res.data.page.totalElements || 0;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("role.error.fetchPermissions"));
  } finally {
    loading.value = false;
  }
};

const handleAdd = async () => {
  if (Number.isNaN(roleId.value)) return;
  pickerDialogVisible.value = true;
};

const handleConfirmAddPerms = async (ids: number[]) => {
  if (ids.length === 0 || Number.isNaN(roleId.value)) return;
  const expiresAt = assignExpiresAt.value ? assignExpiresAt.value.toISOString() : undefined;
  try {
    await assignPermissions(roleId.value, {
      perms: ids.map((permissionId) => ({ permissionId, expiresAt })),
    });
    ElMessage.success(t("role.success.update"));
    await fetchData();
    assignExpiresAt.value = null;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("role.error.save"));
  }
};

const handleRemove = async () => {
  if (selectedIds.value.length === 0 || Number.isNaN(roleId.value)) return;
  try {
    await deleteRolePerms(roleId.value, { ids: selectedIds.value });
    selectedIds.value = [];
    ElMessage.success(t("role.success.update"));
    await fetchData();
  } catch (e) {
    console.error(e);
    ElMessage.error(t("role.error.save"));
  }
};

const handleSearch = () => {
  pageOpts.value.currentPage = 1;
  fetchData();
};

const handleReset = () => {
  pageOpts.value.currentPage = 1;
  searchForm.value = { name: "", code: "" };
  fetchData();
};

const handleSelectionChange = (rows: AssignedPermissionRes[]) => {
  selectedIds.value = rows.map((r) => r.id);
};

onMounted(async () => {
  await Promise.all([fetchRoleBase(), fetchData()]);
});
</script>

<template>
  <div class="assign-layout">
    <SearchContainer>
      <el-form inline :model="searchForm" class="search-form">
        <el-form-item :label="t('permission.name')">
          <el-input v-model="searchForm.name" :placeholder="t('permission.input.name')" />
        </el-form-item>
        <el-form-item :label="t('permission.code')">
          <el-input v-model="searchForm.code" :placeholder="t('permission.input.code')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('query.title') }}</el-button>
          <el-button @click="handleReset">{{ t('reset.title') }}</el-button>
        </el-form-item>
      </el-form>
    </SearchContainer>

    <TableContainer
      :title="assignTitle"
      :title-i18n="false"
      showAddBtn
      :total="pageOpts.total"
      :disable-delete="selectedIds.length === 0"
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
      @change="fetchData"
      @add="handleAdd"
      @remove="handleRemove"
    >
      <el-table
        v-loading="loading"
        :data="tableData"
        fit
        highlight-current-row
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" :label="t('permission.name')" />
        <el-table-column prop="code" :label="t('permission.code')" />
        <el-table-column prop="assignedAt" :label="t('create.time')">
          <template #default="{ row }">{{ formatISOData(row.assignedAt) }}</template>
        </el-table-column>
        <el-table-column prop="expiresAt" :label="t('expiresAt.title')">
          <template #default="{ row }">{{ row.expiresAt ? formatISOData(row.expiresAt) : t('none.title') }}</template>
        </el-table-column>
      </el-table>
    </TableContainer>

    <SelectPermissionDialog
      v-model="pickerDialogVisible"
      :disabled-permission-ids="tableData.map((item) => item.id)"
      @confirm="handleConfirmAddPerms"
    >
      <template #footer-extra>
        <el-form inline>
          <el-form-item :label="t('expiresAt.title')">
            <el-date-picker
              v-model="assignExpiresAt"
              type="datetime"
              clearable
              :placeholder="t('expiresAt.placeholder')"
            />
          </el-form-item>
        </el-form>
      </template>
    </SelectPermissionDialog>
  </div>
</template>

<style scoped>
.assign-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-form {
  width: 100%;
}
</style>
