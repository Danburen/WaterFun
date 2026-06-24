<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox, ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import ListPage from "~/components/ListPage.vue";
import { deleteUser, deleteUsers, getUserList, type AccountStatus, type UserAdminDto, type UserType } from "~/api/user";
import type { PageOptions } from "~/types";
import UserEditDialog from "./components/UserEditDialog.vue";

const router = useRouter();

const loading = ref(false);
const userList = ref<UserAdminDto[]>([]);
const selectedUserUids = ref<Set<string>>(new Set());
const editDialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("edit");
const currentEditUid = ref<string | null>(null);
const searchForm = ref<{ username: string; nickname: string; accountStatus: "" | AccountStatus }>({
  username: "", nickname: "", accountStatus: "",
});
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 10, total: 0 });

const statusLabel = (s: AccountStatus) => ({ ACTIVE: '正常', SUSPENDED: '已停用', DEACTIVATED: '已注销', DELETED: '已删除' })[s];
const userTypeLabel = (t: UserType) => ({ COMMON: '普通用户', ADMIN: '管理员', BOT: '机器人', MODERATOR: '审核员', VIP: 'VIP用户' })[t] ?? '未知';

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getUserList({
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      username: searchForm.value.username || undefined,
      nickname: searchForm.value.nickname || undefined,
      accountStatus: searchForm.value.accountStatus || undefined,
    });
    userList.value = res.data.content || [];
    pageOpts.value.total = res.data.totalElements ?? res.data.page?.totalElements ?? 0;
  } catch (error) {
    console.error(error);
    ElMessage.error('获取用户列表失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleReset = () => {
  pageOpts.value.currentPage = 1;
  searchForm.value = { username: "", nickname: "", accountStatus: "" };
  fetchData();
};
const gotoDetail = (uid: string) => router.push({ name: "userDetail", params: { uid } });
const gotoEdit = (uid: string) => { dialogMode.value = "edit"; currentEditUid.value = uid; editDialogVisible.value = true; };
const handleAdd = () => { dialogMode.value = "create"; currentEditUid.value = null; editDialogVisible.value = true; };

const toggleSelect = (uid: string) => {
  const s = new Set(selectedUserUids.value);
  s.has(uid) ? s.delete(uid) : s.add(uid);
  selectedUserUids.value = s;
};
const toggleSelectAll = () => {
  if (selectedUserUids.value.size === userList.value.length) {
    selectedUserUids.value = new Set();
  } else {
    selectedUserUids.value = new Set(userList.value.map(u => u.uid));
  }
};

const handleDelete = async (uid: string) => {
  try {
    await ElMessageBox.confirm('确定删除该用户吗？', '删除', { type: "warning" });
    await deleteUser(uid);
    ElMessage.success('用户删除成功');
    fetchData();
  } catch (e) {
    if (e !== "cancel") { console.error(e); ElMessage.error('删除用户失败'); }
  }
};

const handleBatchDelete = async () => {
  const ids = [...selectedUserUids.value];
  if (!ids.length) return;
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个用户吗？`, '删除', { type: "warning" });
    const res = await deleteUsers(ids);
    const result = res.data;
    if (result.success === result.requested) ElMessage.success('用户删除成功');
    else if (result.success === 0) ElMessage.error('删除用户失败');
    else ElMessage.warning(`用户删除成功 ${result.success}/${result.requested}`);
    selectedUserUids.value = new Set();
    fetchData();
  } catch (e) {
    if (e !== "cancel") { console.error(e); ElMessage.error('删除用户失败'); }
  }
};

const handleEditSuccess = () => fetchData();
onMounted(fetchData);
</script>

<template>
  <ListPage
    title="用户管理"
    :loading="loading"
    :total="pageOpts.total"
    v-model:page="pageOpts.currentPage"
    v-model:pageSize="pageOpts.pageSize"
    @change="fetchData"
  >
    <template #search>
      <div class="search-form">
        <div class="search-field">
          <label>用户名</label>
          <input v-model="searchForm.username" placeholder="请输入用户名" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>昵称</label>
          <input v-model="searchForm.nickname" placeholder="请输入昵称" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>用户状态</label>
          <select v-model="searchForm.accountStatus">
            <option value="">全部</option>
            <option value="ACTIVE">正常</option>
            <option value="SUSPENDED">已停用</option>
            <option value="DEACTIVATED">已注销</option>
            <option value="DELETED">已删除</option>
          </select>
        </div>
        <div class="search-actions">
          <button class="btn btn-primary" @click="handleSearch">查询</button>
          <button class="btn btn-default" @click="handleReset">重置</button>
        </div>
      </div>
    </template>
    <template #header>
      <button class="btn btn-primary" @click="handleAdd"><i class="fa-solid fa-plus"></i> 新增</button>
      <button class="btn btn-danger" :disabled="selectedUserUids.size === 0" @click="handleBatchDelete"><i class="fa-solid fa-trash"></i> 删除</button>
    </template>

    <table class="data-table">
      <thead>
        <tr>
          <th style="width:40px"><input type="checkbox" :checked="selectedUserUids.size === userList.length && userList.length > 0" @change="toggleSelectAll" /></th>
          <th>UID</th>
          <th>用户名</th>
          <th>昵称</th>
          <th>用户类型</th>
          <th>用户状态</th>
          <th>创建时间</th>
          <th style="width:200px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in userList" :key="row.uid">
          <td><input type="checkbox" :checked="selectedUserUids.has(row.uid)" @change="toggleSelect(row.uid)" /></td>
          <td>{{ row.uid }}</td>
          <td><a class="link" @click="gotoDetail(row.uid)">{{ row.username }}</a></td>
          <td>{{ row.nickname || '无' }}</td>
          <td><span :class="['badge', row.userType === 'ADMIN' || row.userType === 'MODERATOR' ? 'badge-red' : row.userType === 'VIP' ? 'badge-yellow' : row.userType === 'BOT' ? 'badge-blue' : 'badge-gray']">{{ userTypeLabel(row.userType) }}</span></td>
          <td><span :class="['badge', row.accountStatus === 'ACTIVE' ? 'badge-green' : row.accountStatus === 'SUSPENDED' ? 'badge-yellow' : 'badge-gray']">{{ statusLabel(row.accountStatus) }}</span></td>
          <td>{{ formatISOData(row.createdAt) }}</td>
          <td>
            <div class="table-actions">
              <button class="action-btn" @click="gotoEdit(row.uid)">编辑</button>
              <button class="action-btn danger" @click="handleDelete(row.uid)">删除</button>
              <div class="dropdown">
                <button class="action-btn">更多</button>
                <div class="dropdown-menu">
                  <a @click="router.push({ name: 'userRoleAssign', params: { uid: row.uid } })">分配角色</a>
                  <a @click="router.push({ name: 'userPermissionAssign', params: { uid: row.uid } })">分配权限</a>
                </div>
              </div>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <UserEditDialog v-model="editDialogVisible" :mode="dialogMode" :uid="currentEditUid" @success="handleEditSuccess" />
  </ListPage>
</template>
