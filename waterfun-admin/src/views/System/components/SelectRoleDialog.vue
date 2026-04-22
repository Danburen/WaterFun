<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import type { OptionResItem } from "@waterfun/web-core/src/types";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { getRoleAllIds, listRoles, type RoleResp } from "~/api/role";
import type { PageOptions } from "~/types";
import { useI18n } from "vue-i18n";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    disabledRoleIds?: number[];
  }>(),
  {
    disabledRoleIds: () => [],
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
const tableData = ref<RoleResp[]>([]);
const selectedIds = ref<number[]>([]);
const roleOptions = ref<OptionResItem<number>[]>([]);

const searchForm = ref({
  name: "",
  code: "",
  parentId: null as number | null,
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const selectable = (row: RoleResp) => !props.disabledRoleIds.includes(row.id);

const fetchRoleOptions = async () => {
  try {
    const res = await getRoleAllIds();
    roleOptions.value = res.data || [];
  } catch (e) {
    console.error(e);
    ElMessage.error(t("role.error.fetch"));
  }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listRoles(
      (pageOpts.value.currentPage || 1) - 1,
      pageOpts.value.pageSize,
      searchForm.value.name || undefined,
      searchForm.value.code || undefined,
      searchForm.value.parentId ?? undefined
    );
    tableData.value = res.data.content || [];
    pageOpts.value.total = res.data.page.totalElements || 0;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("role.error.fetch"));
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
  searchForm.value = { name: "", code: "", parentId: null };
  fetchData();
};

const handleSelectionChange = (rows: RoleResp[]) => {
  selectedIds.value = rows.map((r) => r.id);
};

const handleConfirm = () => {
  emit("confirm", selectedIds.value);
  visible.value = false;
};

watch(
  () => visible.value,
  async (open) => {
    if (!open) return;
    selectedIds.value = [];
    pageOpts.value.currentPage = 1;
    await Promise.all([fetchRoleOptions(), fetchData()]);
  }
);
</script>

<template>
  <el-dialog v-model="visible" :title="t('role.select')" width="1200" destroy-on-close>
    <div class="picker-layout">
      <SearchContainer>
        <el-form inline :model="searchForm" class="search-form">
          <el-form-item :label="t('role.name')">
            <el-input v-model="searchForm.name" :placeholder="t('role.input.name')" />
          </el-form-item>
          <el-form-item :label="t('role.code')">
            <el-input v-model="searchForm.code" :placeholder="t('role.input.code')" />
          </el-form-item>
          <el-form-item :label="t('role.parentId')">
            <el-select v-model="searchForm.parentId" clearable :placeholder="t('role.input.parentId')" style="width: 180px">
              <el-option
                v-for="item in roleOptions"
                :key="item.id"
                :label="`${item.id} (${item.name}${item.code ? ` 【${item.code}】` : ''})`"
                :value="item.id"
                :disabled="item.disabled || false"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">{{ t('common.query.title') }}</el-button>
            <el-button @click="handleReset">{{ t('common.reset.title') }}</el-button>
          </el-form-item>
        </el-form>
      </SearchContainer>

      <TableContainer
        title="role.list"
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
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="name" :label="t('role.name')" min-width="180" />
          <el-table-column prop="code" :label="t('role.code')" min-width="180" />
          <el-table-column prop="parentId" :label="t('role.parentId')" width="120">
            <template #default="{ row }">
              <span>{{ row.parentId ?? t('common.none.title') }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" :label="t('common.time.create')" min-width="170">
            <template #default="{ row }">{{ formatISOData(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </TableContainer>

      <div class="dialog-extra">
        <slot name="footer-extra" :selected-ids="selectedIds" />
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.action.cancel') }}</el-button>
      <el-button type="primary" :disabled="selectedIds.length === 0" @click="handleConfirm">{{ t('common.action.save') }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.picker-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dialog-extra {
  padding-top: 4px;
}
</style>

