<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox, ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { deleteUser, deleteUsers, getUserList, type AccountStatus, type UserAdminDto } from "~/api/user";
import type { PageOptions } from "~/types";
import UserEditDialog from "./components/UserEditDialog.vue";

const router = useRouter();

const loading = ref(false);
const userList = ref<UserAdminDto[]>([]);
const selectedUserUids = ref<string[]>([]);
const editDialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("edit");
const currentEditUid = ref<string | null>(null);
const selectable = (row: UserAdminDto) => row.userType !== 2 && row.userType !== 3 && row.userType !== 4;
const searchForm = ref<{
  username: string;
  nickname: string;
  accountStatus: "" | AccountStatus;
}>({
  username: "",
  nickname: "",
  accountStatus: "",
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const statusTypeMap: Record<AccountStatus, "success" | "warning" | "danger" | "info"> = {
  ACTIVE: "success",
  SUSPENDED: "warning",
  DEACTIVATED: "danger",
  DELETED: "info",
};

const statusLabel = (status: AccountStatus) =>
  ({ ACTIVE: '正常', SUSPENDED: '已停用', DEACTIVATED: '已注销', DELETED: '已删除' })[status];

const userTypeTagTypeMap: Record<number, "primary" | "success" | "warning" | "danger" | "info"> = {
  0: "primary",
  1: "warning",
  2: "danger",
  3: "info",
  4: "danger",
};

const userTypeI18nKeyMap: Record<number, string> = {
  0: "normal",
  1: "tester",
  2: "admin",
  3: "system",
  4: "superAdmin",
};

const userTypeLabel = (userType: number) =>
  ({ 0: '普通用户', 1: '测试用户', 2: '管理员', 3: '系统', 4: '超级管理员' })[userType] ?? '普通用户';

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
    pageOpts.value.total = res.data.page.totalElements || 0;
  } catch (error) {
    console.error(error);
    ElMessage.error('获取用户列表失败');
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
    username: "",
    nickname: "",
    accountStatus: "",
  };
  fetchData();
};

const gotoDetail = (uid: string) => {
  router.push({ name: "userDetail", params: { uid: String(uid) } });
};

const gotoEdit = (uid: string) => {
  dialogMode.value = "edit";
  currentEditUid.value = String(uid);
  editDialogVisible.value = true;
};

const handleAdd = () => {
  dialogMode.value = "create";
  currentEditUid.value = null;
  editDialogVisible.value = true;
};

const handleDelete = async (uid: string) => {
  try {
    await ElMessageBox.confirm('确定删除该用户吗？', '删除', {
      type: "warning",
    });
    await deleteUser(uid);
    ElMessage.success('用户删除成功');
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error('删除用户失败');
    }
  }
};

const handleSelectionChange = (rows: UserAdminDto[]) => {
  selectedUserUids.value = rows.map(item => item.uid);
};

const handleBatchDelete = async () => {
  if (selectedUserUids.value.length === 0) {
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedUserUids.value.length} 个用户吗？`,
      '删除',
      { type: "warning" }
    );

    const res = await deleteUsers(selectedUserUids.value);
    const result = res.data;

    if (result.success === result.requested) {
      ElMessage.success('用户删除成功');
    } else if (result.success === 0) {
      ElMessage.error('删除用户失败');
    } else {
      ElMessage.warning(`${'用户删除成功'} ${result.success}/${result.requested}`);
    }

    selectedUserUids.value = [];
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error('删除用户失败');
    }
  }
};

const handleEditSuccess = () => {
  fetchData();
};

onMounted(fetchData);
</script>

<template>
  <div class="list-layout">
    <SearchContainer>
      <el-form
        inline
        :model="searchForm"
        class="search-form"
      >
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
          />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input
            v-model="searchForm.nickname"
            placeholder="请输入昵称"
          />
        </el-form-item>
        <el-form-item label="用户状态">
          <el-select
            v-model="searchForm.accountStatus"
            clearable
            style="width: 150px"
          >
            <el-option
              :label="statusLabel('ACTIVE')"
              value="ACTIVE"
            />
            <el-option
              :label="statusLabel('SUSPENDED')"
              value="SUSPENDED"
            />
            <el-option
              :label="statusLabel('DEACTIVATED')"
              value="DEACTIVATED"
            />
            <el-option
              :label="statusLabel('DELETED')"
              value="DELETED"
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
      title="用户管理"
      show-add-btn
      :show-remove-btn="true"
      :disable-delete="selectedUserUids.length === 0"
      :total="pageOpts.total"
      @add="handleAdd"
      @remove="handleBatchDelete"
      @change="fetchData"
    >
      <el-table
        v-loading="loading"
        :data="userList"
        border
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
          prop="uid"
          label="UID"
          min-width="140"
        />
        <el-table-column
          prop="username"
          label="用户名"
        >
          <template #default="{ row }">
            <el-link
              type="primary"
              :underline="false"
              @click="gotoDetail(row.uid)"
            >
              {{ row.username }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          prop="nickname"
          label="昵称"
        >
          <template #default="{ row }">
            {{ row.nickname || '无' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="userType"
          label="用户类型"
          min-width="120"
        >
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="userTypeTagTypeMap[row.userType]"
            >
              {{ userTypeLabel(row.userType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="accountStatus"
          label="用户状态"
        >
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="statusTypeMap[row.accountStatus]"
            >
              {{ statusLabel(row.accountStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createdAt"
          label="创建时间"
        >
          <template #default="{ row }">
            {{ formatISOData(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          min-width="120"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              @click="gotoEdit(row.uid)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="handleDelete(row.uid)"
            >
              删除
            </el-button>
            <el-popover
              placement="bottom"
              trigger="click"
              :width="100"
              popper-style="min-width: auto; padding: 8px;"
            >
              <template #reference>
                <el-button
                  size="small"
                  type="success"
                  style="margin-left: 12px;"
                >
                  更多
                </el-button>
              </template>
              <div style="display: flex; flex-direction: column; gap: 8px;">
                <el-button
                  size="small"
                  type="primary"
                  plain
                  style="margin: 0; width: 100%;"
                  @click="router.push({ name: 'userRoleAssign', params: { uid: String(row.uid) } })"
                >
                  分配角色
                </el-button>
                <el-button
                  size="small"
                  type="primary"
                  plain
                  style="margin: 0; width: 100%;"
                  @click="router.push({ name: 'userPermissionAssign', params: { uid: String(row.uid) } })"
                >
                  分配权限
                </el-button>
              </div>
            </el-popover>
          </template>
        </el-table-column>
      </el-table>

      <UserEditDialog
        v-model="editDialogVisible"
        :mode="dialogMode"
        :uid="currentEditUid"
        @success="handleEditSuccess"
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


