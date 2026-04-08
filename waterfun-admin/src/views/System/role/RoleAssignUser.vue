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
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import SelectUserDialog from "../components/SelectUserDialog.vue";

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const roleId = computed(() => Number(route.params.id));
const roleName = ref("");

const loading = ref(false);
const tableData = ref<AssignedUserRes[]>([]);
const selectedIds = ref<number[]>([]);
const pickerDialogVisible = ref(false);
const assignExpiresAt = ref<Date | null>(null);

const searchForm = ref({
  userUid: undefined as number | undefined,
  username: "",
  nickname: "",
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
    item: t("user.entity"),
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
    const res = await listRoleUsers(roleId.value, {
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      userUid: searchForm.value.userUid,
      username: searchForm.value.username || undefined,
      nickname: searchForm.value.nickname || undefined,
    });
    tableData.value = res.data.content || [];
    pageOpts.value.total = res.data.page.totalElements || 0;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("role.error.fetchUsers"));
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  if (Number.isNaN(roleId.value)) return;
  pickerDialogVisible.value = true;
};

const handleConfirmAddUsers = async (ids: number[]) => {
  if (ids.length === 0 || Number.isNaN(roleId.value)) return;
  const expiresAt = assignExpiresAt.value ? assignExpiresAt.value.toISOString() : undefined;
  try {
    await assignUserRoles(roleId.value, { userUids: ids, expiresAt });
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
    await deleteUserRoles(roleId.value, { userIds: selectedIds.value });
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
  searchForm.value = { userUid: undefined, username: "", nickname: "" };
  fetchData();
};

const handleSelectionChange = (rows: AssignedUserRes[]) => {
  selectedIds.value = rows.map((r) => r.userUid);
};

onMounted(async () => {
  await Promise.all([fetchRoleBase(), fetchData()]);
});
</script>

<template>
  <div class="assign-layout">
    <SearchContainer>
      <el-form inline :model="searchForm" class="search-form">
        <el-form-item :label="t('user.uid')">
          <el-input v-model="searchForm.userUid" :placeholder="t('user.input.uid')" />
        </el-form-item>
        <el-form-item :label="t('user.username')">
          <el-input v-model="searchForm.username" :placeholder="t('user.input.username')" />
        </el-form-item>
        <el-form-item :label="t('user.nickname')">
          <el-input v-model="searchForm.nickname" :placeholder="t('user.input.nickname')" />
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
      :total="pageOpts.total"
      :disable-delete="selectedIds.length === 0"
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
      @change="fetchData"
      @add="handleAdd"
      @remove="handleRemove"
    >
      <template #action-buttons>
        <el-button type="primary" size="small" @click="handleAdd">{{ t('user.select') }}</el-button>
        <el-button type="danger" size="small" :disabled="selectedIds.length === 0" @click="handleRemove">{{ t('btn.delete') }}</el-button>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        fit
        highlight-current-row
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="userUid" :label="t('user.uid')" />
        <el-table-column prop="username" :label="t('user.username')" />
        <el-table-column prop="nickname" :label="t('user.nickname')" />
      </el-table>
    </TableContainer>

    <SelectUserDialog
      v-model="pickerDialogVisible"
      :disabled-user-uids="tableData.map((item) => item.userUid)"
      @confirm="handleConfirmAddUsers"
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
    </SelectUserDialog>
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
