<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { getUserList, type AccountStatus, type UserAdminDto } from "~/api/user";
import type { PageOptions } from "~/types";
import { useI18n } from "vue-i18n";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    disabledUserUids?: number[];
  }>(),
  {
    disabledUserUids: () => [],
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  confirm: [ids: number[]];
}>();

const { t } = useI18n();

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});

const loading = ref(false);
const tableData = ref<UserAdminDto[]>([]);
const selectedIds = ref<number[]>([]);

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

const selectable = (row: UserAdminDto) => !props.disabledUserUids.includes(row.uid);

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
    tableData.value = res.data.content || [];
    pageOpts.value.total = res.data.page.totalElements || 0;
  } catch (e) {
    console.error(e);
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

const handleSelectionChange = (rows: UserAdminDto[]) => {
  selectedIds.value = rows.map((r) => r.uid);
};

const handleConfirm = () => {
  emit("confirm", selectedIds.value);
  visible.value = false;
};

watch(
  () => visible.value,
  (open) => {
    if (!open) return;
    selectedIds.value = [];
    pageOpts.value.currentPage = 1;
    fetchData();
  }
);
</script>

<template>
  <el-dialog v-model="visible" :title="t('user.select')" width="1200" destroy-on-close>
    <div class="picker-layout">
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
            <el-button type="primary" @click="handleSearch">{{ t('query.title') }}</el-button>
            <el-button @click="handleReset">{{ t('reset.title') }}</el-button>
          </el-form-item>
        </el-form>
      </SearchContainer>

      <TableContainer
        title="user.list"
        :show-add-btn="false"
        :show-remove-btn="false"
        :total="pageOpts.total"
        v-model:page-size="pageOpts.pageSize"
        v-model:current-page="pageOpts.currentPage"
        @change="fetchData"
      >
        <el-table
          v-loading="loading"
          :data="tableData"
          border
          fit
          highlight-current-row
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" :selectable="selectable" width="55" />
          <el-table-column prop="uid" label="UID" min-width="120" />
          <el-table-column prop="username" :label="t('user.username')" min-width="180" />
          <el-table-column prop="nickname" :label="t('user.nickname')" min-width="180">
            <template #default="{ row }">{{ row.nickname || t('none.title') }}</template>
          </el-table-column>
          <el-table-column prop="accountStatus" :label="t('user.status')" width="140">
            <template #default="{ row }">
              <el-tag size="small" :type="statusTypeMap[row.accountStatus]">{{ statusLabel(row.accountStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" :label="t('create.time')" min-width="170">
            <template #default="{ row }">{{ formatISOData(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </TableContainer>

      <div class="dialog-extra">
        <slot name="footer-extra" :selected-ids="selectedIds" />
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('cancel.title') }}</el-button>
      <el-button type="primary" :disabled="selectedIds.length === 0" @click="handleConfirm">{{ t('save.title') }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.picker-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.dialog-extra {
  padding-top: 4px;
}
</style>
