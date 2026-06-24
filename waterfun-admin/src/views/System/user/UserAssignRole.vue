<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import {
  assignUserRoles,
  getUserDetail,
  listUserRoles,
  removeUserRoles,
  type AssignedRoleRes,
} from "~/api/user";
import type { PageOptions } from "~/types";

import { useRoute, useRouter } from "vue-router";
import SelectRoleDialog from "../components/SelectRoleDialog.vue";

const route = useRoute();
const router = useRouter();

const uid = computed(() => String(route.params.uid));
const isValidUid = computed(() => uid.value != null && uid.value !== '');
const userName = ref("");

const loading = ref(false);
const tableData = ref<AssignedRoleRes[]>([]);
const selectedIds = ref<Set<number>>(new Set());
const pickerDialogVisible = ref(false);
const assignExpiresAt = ref<Date | null>(null);

const searchForm = ref({
  roleId: undefined as number | undefined,
  name: "",
  code: "",
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const assignTitle = computed(() =>
  '为用户 ' + (userName.value || uid.value) + ' 分配角色'
);

const fetchUserBase = async () => {
  if (!isValidUid.value) {
    ElMessage.error('无效的用户UID');
    router.back();
    return;
  }
  try {
    const res = await getUserDetail(uid.value);
    userName.value = res.data.info.username || "";
  } catch (e) {
    console.error(e);
    ElMessage.error('获取用户详情失败');
  }
};

const fetchData = async () => {
  if (!isValidUid.value) return;
  loading.value = true;
  try {
    const res = await listUserRoles(
      uid.value,
      (pageOpts.value.currentPage || 1) - 1,
      pageOpts.value.pageSize,
      searchForm.value.roleId,
      searchForm.value.name || undefined,
      searchForm.value.code || undefined
    );
    tableData.value = res.data.content || [];
    pageOpts.value.total = res.data.totalElements ?? res.data.page?.totalElements ?? 0;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取角色信息失败');
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  if (!isValidUid.value) return;
  pickerDialogVisible.value = true;
};

const handleConfirmAddRoles = async (ids: number[]) => {
  if (ids.length === 0 || !isValidUid.value) return;
  const expiresAt = assignExpiresAt.value ? assignExpiresAt.value.toISOString() : undefined;
  try {
    await assignUserRoles(
      uid.value,
      ids.map((roleId) => ({ roleId, expiresAt }))
    );
    ElMessage.success('角色更新成功');
    await fetchData();
    assignExpiresAt.value = null;
  } catch (e) {
    console.error(e);
    ElMessage.error('保存角色失败');
  }
};

const handleRemove = async () => {
  if (selectedIds.value.size === 0 || !isValidUid.value) return;
  try {
    await removeUserRoles(uid.value, [...selectedIds.value]);
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
  searchForm.value = { roleId: undefined, name: "", code: "" };
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
  await Promise.all([fetchUserBase(), fetchData()]);
});
</script>

<template>
  <div class="list-layout">
    <SearchContainer>
      <div class="search-form">
        <div class="search-field">
          <label>ID</label>
          <input v-model="searchForm.roleId" placeholder="ID" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>角色名称</label>
          <input v-model="searchForm.name" placeholder="请输入角色名称" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>角色编码</label>
          <input v-model="searchForm.code" placeholder="请输入角色编码" @keyup.enter="handleSearch" />
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
              <th>角色名称</th>
              <th>角色编码</th>
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

    <SelectRoleDialog
      v-model="pickerDialogVisible"
      :disabled-role-ids="tableData.map((item) => item.id)"
      @confirm="handleConfirmAddRoles"
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
    </SelectRoleDialog>
  </div>
</template>

<style scoped>
.date-picker-label {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
