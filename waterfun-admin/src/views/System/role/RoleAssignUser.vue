<script setup lang="ts">
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import {
  assignUserRoles,
  deleteUserRoles,
  getRole,
  listRoleUsers,
  type AssignedUserRes,
} from "~/api/role";
import type { PageOptions } from "~/types";

import { useRoute, useRouter } from "vue-router";
import SelectUserDialog from "../components/SelectUserDialog.vue";

const route = useRoute();
const router = useRouter();

const roleId = computed(() => Number(route.params.id));
const roleName = ref("");

const loading = ref(false);
const tableData = ref<AssignedUserRes[]>([]);
const selectedIds = ref<Set<string>>(new Set());
const pickerDialogVisible = ref(false);
const assignExpiresAt = ref<Date | null>(null);

const searchForm = ref({
  userUid: undefined as string | undefined,
  username: "",
  nickname: "",
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const assignTitle = computed(() =>
  '为角色 ' + (roleName.value || roleId.value) + ' 分配用户'
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
    const res = await listRoleUsers(roleId.value, {
      page: pageOpts.value.currentPage || 1,
      size: pageOpts.value.pageSize,
      userUid: searchForm.value.userUid,
      username: searchForm.value.username || undefined,
      nickname: searchForm.value.nickname || undefined,
    });
    tableData.value = res.data.content || [];
    pageOpts.value.total = res.data.totalElements ?? res.data.page?.totalElements ?? 0;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取角色用户失败');
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  if (Number.isNaN(roleId.value)) return;
  pickerDialogVisible.value = true;
};

const handleConfirmAddUsers = async (ids: string[]) => {
  if (ids.length === 0 || Number.isNaN(roleId.value)) return;
  const expiresAt = assignExpiresAt.value ? assignExpiresAt.value.toISOString() : undefined;
  try {
    await assignUserRoles(roleId.value, { userUids: ids, expiresAt });
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
    await deleteUserRoles(roleId.value, { userIds: [...selectedIds.value] });
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
  searchForm.value = { userUid: undefined, username: "", nickname: "" };
  fetchData();
};

const toggleSelect = (uid: string) => {
  const s = new Set(selectedIds.value);
  s.has(uid) ? s.delete(uid) : s.add(uid);
  selectedIds.value = s;
};

const toggleSelectAll = () => {
  if (selectedIds.value.size === tableData.value.length) selectedIds.value = new Set();
  else selectedIds.value = new Set(tableData.value.map(r => r.userUid));
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
          <label>用户ID</label>
          <input v-model="searchForm.userUid" placeholder="请输入用户ID" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>用户名</label>
          <input v-model="searchForm.username" placeholder="请输入用户名" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>昵称</label>
          <input v-model="searchForm.nickname" placeholder="请输入昵称" @keyup.enter="handleSearch" />
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
              <th>用户ID</th>
              <th>用户名</th>
              <th>昵称</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in tableData" :key="row.userUid">
              <td><input type="checkbox" :checked="selectedIds.has(row.userUid)" @change="toggleSelect(row.userUid)" /></td>
              <td>{{ row.userUid }}</td>
              <td>{{ row.username }}</td>
              <td>{{ row.nickname }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </TableContainer>

    <SelectUserDialog
      v-model="pickerDialogVisible"
      :disabled-user-uids="tableData.map((item) => item.userUid)"
      @confirm="handleConfirmAddUsers"
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
    </SelectUserDialog>
  </div>
</template>

<style scoped>
.date-picker-label {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
