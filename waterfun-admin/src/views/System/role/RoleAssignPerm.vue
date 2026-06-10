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
import SelectPermissionDialog from "../components/SelectPermissionDialog.vue";

const route = useRoute();
const router = useRouter();

const roleId = computed(() => Number(route.params.id));
const roleName = ref("");

const loading = ref(false);
const tableData = ref<AssignedPermissionRes[]>([]);
const selectedIds = ref<Set<number>>(new Set());
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
    pageOpts.value.total = res.data.totalElements ?? res.data.page?.totalElements ?? 0;
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
  if (selectedIds.value.size === 0 || Number.isNaN(roleId.value)) return;
  try {
    await deleteRolePerms(roleId.value, { ids: [...selectedIds.value] });
    selectedIds.value = new Set();
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

const toggleSelect = (id: number) => {
  const s = new Set(selectedIds.value);
  s.has(id) ? s.delete(id) : s.add(id);
  selectedIds.value = s;
};

const toggleSelectAll = () => {
  if (selectedIds.value.size === tableData.value.length) selectedIds.value = new Set();
  else selectedIds.value = new Set(tableData.value.map(r => r.id));
};

onMounted(async () => {
  await Promise.all([fetchRoleBase(), fetchData()]);
});
</script>

<template>
  <div class="list-layout">
    <SearchContainer>
      <div class="search-form">
        <div class="search-field">
          <label>权限名称</label>
          <input v-model="searchForm.name" placeholder="请输入权限名称" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>权限编码</label>
          <input v-model="searchForm.code" placeholder="请输入权限编码" @keyup.enter="handleSearch" />
        </div>
        <div class="search-actions">
          <button class="btn btn-primary" @click="handleSearch">查询</button>
          <button class="btn btn-default" @click="handleReset">重置</button>
        </div>
      </div>
    </SearchContainer>

    <TableContainer
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
      :title="assignTitle"
      show-add-btn
      :total="pageOpts.total"
      :disable-delete="selectedIds.size === 0"
      @change="fetchData"
      @add="handleAdd"
      @remove="handleRemove"
    >
      <div class="table-wrapper" :class="{ loading }">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width:40px"><input type="checkbox" :checked="selectedIds.size === tableData.length && tableData.length > 0" @change="toggleSelectAll" /></th>
              <th style="width:80px">ID</th>
              <th>权限名称</th>
              <th>权限编码</th>
              <th>创建时间</th>
              <th>过期时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in tableData" :key="row.id">
              <td><input type="checkbox" :checked="selectedIds.has(row.id)" @change="toggleSelect(row.id)" /></td>
              <td>{{ row.id }}</td>
              <td>{{ row.name }}</td>
              <td>{{ row.code }}</td>
              <td>{{ formatISOData(row.assignedAt) }}</td>
              <td>{{ row.expiresAt ? formatISOData(row.expiresAt) : '无' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </TableContainer>

    <SelectPermissionDialog
      v-model="pickerDialogVisible"
      :disabled-permission-ids="tableData.map((item) => item.id)"
      @confirm="handleConfirmAddPerms"
    >
      <template #footer-extra>
        <label class="date-picker-label">
          过期时间
          <el-date-picker
            v-model="assignExpiresAt"
            type="datetime"
            clearable
            placeholder="请选择过期时间（可选）"
          />
        </label>
      </template>
    </SelectPermissionDialog>
  </div>
</template>

<style scoped>
.date-picker-label {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
