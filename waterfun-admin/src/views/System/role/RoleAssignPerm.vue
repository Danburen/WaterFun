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

import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import SelectPermissionDialog from "../components/SelectPermissionDialog.vue";


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
  '为角色 ' + (roleName.value || roleId.value) + ' 分配权限'
);

const fetchRoleBase = async () => {
  if (Number.isNaN(roleId.value)) {
    ElMessage.error('无效的角色 ID');
    router.back();
    return;
  }
  try {
    const res = await getRole(roleId.value);
    roleName.value = res.data.name || "";
  } catch (e) {
    console.error(e);
    ElMessage.error('获取角色信息失败');
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
    ElMessage.error('获取角色权限失败');
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
    ElMessage.success('角色更新成功');
    await fetchData();
    assignExpiresAt.value = null;
  } catch (e) {
    console.error(e);
    ElMessage.error('保存角色失败');
  }
};

const handleRemove = async () => {
  if (selectedIds.value.length === 0 || Number.isNaN(roleId.value)) return;
  try {
    await deleteRolePerms(roleId.value, { ids: selectedIds.value });
    selectedIds.value = [];
    ElMessage.success('角色更新成功');
    await fetchData();
  } catch (e) {
    console.error(e);
    ElMessage.error('保存角色失败');
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
      :title="assignTitle"
      show-add-btn
      :total="pageOpts.total"
      :disable-delete="selectedIds.length === 0"
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
        <el-table-column
          type="selection"
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
        />
        <el-table-column
          prop="code"
          label="权限编码"
        />
        <el-table-column
          prop="assignedAt"
          label="创建时间"
        >
          <template #default="{ row }">
            {{ formatISOData(row.assignedAt) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="expiresAt"
          label="过期时间"
        >
          <template #default="{ row }">
            {{ row.expiresAt ? formatISOData(row.expiresAt) : '无' }}
          </template>
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
          <el-form-item label="过期时间">
            <el-date-picker
              v-model="assignExpiresAt"
              type="datetime"
              clearable
              placeholder="请选择过期时间（可选）"
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

