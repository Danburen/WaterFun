<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { deleteUser, deleteUsers, getUserList, type AccountStatus, type UserAdminDto } from "~/api/user";
import type { PageOptions } from "~/types";
import UserEditDialog from "./components/UserEditDialog.vue";

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const userList = ref<UserAdminDto[]>([]);
const selectedUserUids = ref<number[]>([]);
const editDialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("edit");
const currentEditUid = ref<number | null>(null);
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

const statusLabel = (status: AccountStatus) => t(`user.statusMap.${status.toLowerCase()}`);

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

const userTypeLabel = (userType: number) => t(`user.typeMap.${userTypeI18nKeyMap[userType] ?? "normal"}`);

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
    ElMessage.error(t("user.error.fetchList"));
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

const gotoDetail = (uid: number) => {
  router.push({ name: "userDetail", params: { uid } });
};

const gotoEdit = (uid: number) => {
  dialogMode.value = "edit";
  currentEditUid.value = uid;
  editDialogVisible.value = true;
};

const handleAdd = () => {
  dialogMode.value = "create";
  currentEditUid.value = null;
  editDialogVisible.value = true;
};

const handleDelete = async (uid: number) => {
  try {
    await ElMessageBox.confirm(t("user.confirm.delete"), t("operation.delete"), {
      type: "warning",
    });
    await deleteUser(uid);
    ElMessage.success(t("user.success.delete"));
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error(t("user.error.delete"));
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
      t("user.confirm.batchDelete", { count: selectedUserUids.value.length }),
      t("operation.delete"),
      { type: "warning" }
    );

    const res = await deleteUsers(selectedUserUids.value);
    const result = res.data;

    if (result.success === result.requested) {
      ElMessage.success(t("user.success.delete"));
    } else if (result.success === 0) {
      ElMessage.error(t("user.error.delete"));
    } else {
      ElMessage.warning(`${t("user.success.delete")} ${result.success}/${result.requested}`);
    }

    selectedUserUids.value = [];
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error(t("user.error.delete"));
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
      <el-form inline :model="searchForm" class="search-form">
        <el-form-item :label="t('user.username')">
          <el-input v-model="searchForm.username" :placeholder="t('user.input.username')" />
        </el-form-item>
        <el-form-item :label="t('user.nickname')">
          <el-input v-model="searchForm.nickname" :placeholder="t('user.input.nickname')" />
        </el-form-item>
        <el-form-item :label="t('user.status')">
          <el-select v-model="searchForm.accountStatus" clearable style="width: 150px">
            <el-option :label="statusLabel('ACTIVE')" value="ACTIVE" />
            <el-option :label="statusLabel('SUSPENDED')" value="SUSPENDED" />
            <el-option :label="statusLabel('DEACTIVATED')" value="DEACTIVATED" />
            <el-option :label="statusLabel('DELETED')" value="DELETED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t("query.title") }}</el-button>
          <el-button @click="handleReset">{{ t("reset.title") }}</el-button>
        </el-form-item>
      </el-form>
    </SearchContainer>

    <TableContainer
      title="user.title"
      showAddBtn
      :show-remove-btn="true"
      :disable-delete="selectedUserUids.length === 0"
      :total="pageOpts.total"
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
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
      <el-table-column type="selection" :selectable="selectable" width="55" />
      <el-table-column prop="uid" label="UID" min-width="140" />
      <el-table-column prop="username" :label="t('user.username')">
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="gotoDetail(row.uid)">
            {{ row.username }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="nickname" :label="t('user.nickname')" >
        <template #default="{ row }">
          {{ row.nickname || t('none.title') }}
        </template>
      </el-table-column>
      <el-table-column prop="userType" :label="t('user.type')" min-width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="userTypeTagTypeMap[row.userType]">
            {{ userTypeLabel(row.userType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="accountStatus" :label="t('user.status')">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTypeMap[row.accountStatus]">
            {{ statusLabel(row.accountStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" :label="t('create.time')">
        <template #default="{ row }">
          {{ formatISOData(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('operation.title')" min-width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="gotoEdit(row.uid)">{{ t("operation.edit") }}</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.uid)">{{ t("operation.delete") }}</el-button>
          <el-popover placement="bottom" trigger="click" :width="100" popper-style="min-width: auto; padding: 8px;">
            <template #reference>
              <el-button size="small" type="success" style="margin-left: 12px;">{{ t("operation.more") }}</el-button>
            </template>
            <div style="display: flex; flex-direction: column; gap: 8px;">
              <el-button size="small" type="primary" plain @click="router.push({ name: 'userRoleAssign', params: { uid: row.uid } })" style="margin: 0; width: 100%;">{{ t("role.assign") }}</el-button>
              <el-button size="small" type="primary" plain @click="router.push({ name: 'userPermissionAssign', params: { uid: row.uid } })" style="margin: 0; width: 100%;">{{ t("permission.assign") }}</el-button>
            </div>
          </el-popover>
        </template>
      </el-table-column>
    </el-table>

      <UserEditDialog v-model="editDialogVisible" :mode="dialogMode" :uid="currentEditUid" @success="handleEditSuccess" />
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

