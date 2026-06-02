<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { getUserList, type AccountStatus, type UserAdminDto } from "~/api/user";
import type { PageOptions } from "~/types";
import { ElMessage } from "element-plus";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    disabledUserUids?: string[];
  }>(),
  {
    disabledUserUids: () => [],
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  confirm: [ids: string[]];
}>();


const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});

const loading = ref(false);
const tableData = ref<UserAdminDto[]>([]);
const selectedIds = ref<string[]>([]);

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
  <el-dialog
    v-model="visible"
    title="选择用户"
    width="1200"
    destroy-on-close
  >
    <div class="picker-layout">
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
        title="用户列表"
        :show-add-btn="false"
        :show-remove-btn="false"
        :total="pageOpts.total"
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
          <el-table-column
            type="selection"
            :selectable="selectable"
            width="55"
          />
          <el-table-column
            prop="uid"
            label="UID"
            min-width="120"
          />
          <el-table-column
            prop="username"
            label="用户名"
            min-width="180"
          />
          <el-table-column
            prop="nickname"
            label="昵称"
            min-width="180"
          >
            <template #default="{ row }">
              {{ row.nickname || '无' }}
            </template>
          </el-table-column>
          <el-table-column
            prop="accountStatus"
            label="用户状态"
            width="140"
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
            min-width="170"
          >
            <template #default="{ row }">
              {{ formatISOData(row.createdAt) }}
            </template>
          </el-table-column>
        </el-table>
      </TableContainer>

      <div class="dialog-extra">
        <slot
          name="footer-extra"
          :selected-ids="selectedIds"
        />
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">
        取消
      </el-button>
      <el-button
        type="primary"
        :disabled="selectedIds.length === 0"
        @click="handleConfirm"
      >
        保存
      </el-button>
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

