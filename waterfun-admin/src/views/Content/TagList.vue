<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { formatDate } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { getUserOptions } from "~/api/user";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { deleteTag, deleteTags, listTags, type TagResp } from "~/api/tag";
import type { PageOptions } from "~/types/api";
import TagCreateDialog from "~/views/Content/components/TagCreateDialog.vue";

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const loadingOptions = ref(false);
const tagList = ref<TagResp[]>([]);
const selectedTagIds = ref<number[]>([]);
const createDialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentTagId = ref<number>(0);
const userOptions = ref<OptionResItem<string>[]>([]);

const searchForm = ref<{
  name: string;
  slug: string;
  creatorId: string | null;
}>({
  name: "",
  slug: "",
  creatorId: null,
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listTags({
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      name: searchForm.value.name || undefined,
      slug: searchForm.value.slug || undefined,
      creatorId: searchForm.value.creatorId ?? undefined,
    });
    tagList.value = res.data.content || [];
    pageOpts.value.total = res.data.page?.totalElements || 0;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("error.fetch"));
  } finally {
    loading.value = false;
  }
};

const loadOptions = async () => {
  loadingOptions.value = true;
  try {
    const res = await getUserOptions();
    userOptions.value = res.data || [];
  } catch (e) {
    console.error(e);
    ElMessage.error(t("error.fetch"));
  } finally {
    loadingOptions.value = false;
  }
};

const handleSearch = () => {
  pageOpts.value.currentPage = 1;
  fetchData();
};

const handleReset = () => {
  pageOpts.value.currentPage = 1;
  searchForm.value = {
    name: "",
    slug: "",
    creatorId: null,
  };
  fetchData();
};

const handleAdd = () => {
  dialogMode.value = "create";
  currentTagId.value = 0;
  createDialogVisible.value = true;
};

const handleEdit = (id: number) => {
  dialogMode.value = "edit";
  currentTagId.value = id;
  createDialogVisible.value = true;
};

const gotoDetail = (id: number) => {
  router.push({ name: "contentTagDetail", params: { id } });
};

const handleCreateSuccess = () => {
  fetchData();
};

const handleSelectionChange = (rows: TagResp[]) => {
  selectedTagIds.value = rows.map((item) => item.id);
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(t("content.tag.confirm.delete"), t("common.action.delete"), {
      type: "warning",
    });
    await deleteTag(id);
    ElMessage.success(t("content.tag.success.delete"));
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error(t("content.tag.error.delete"));
    }
  }
};

const handleBatchDelete = async () => {
  if (!selectedTagIds.value.length) return;

  try {
    await ElMessageBox.confirm(
      t("content.tag.confirm.batchDelete", { count: selectedTagIds.value.length }),
      t("common.action.delete"),
      { type: "warning" }
    );

    const res = await deleteTags(selectedTagIds.value);
    const result = res.data;

    if (result.success === result.requested) {
      ElMessage.success(t("content.tag.success.delete"));
    } else if (result.success === 0) {
      ElMessage.error(t("content.tag.error.delete"));
    } else {
      ElMessage.warning(`${t("content.tag.success.delete")} ${result.success}/${result.requested}`);
    }

    selectedTagIds.value = [];
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error(t("content.tag.error.delete"));
    }
  }
};

onMounted(() => {
  fetchData();
  loadOptions();
});
</script>

<template>
  <div class="list-layout">
    <SearchContainer>
      <el-form inline class="search-form" :model="searchForm">
        <el-form-item :label="t('content.tag.field.name')">
          <el-input v-model="searchForm.name" :placeholder="t('content.tag.input.name')" />
        </el-form-item>
        <el-form-item :label="t('content.tag.field.slug')">
          <el-input v-model="searchForm.slug" :placeholder="t('content.tag.input.slug')" />
        </el-form-item>
        <el-form-item :label="t('content.tag.field.creatorId')">
          <el-select
            v-model="searchForm.creatorId"
            clearable
            filterable
            :loading="loadingOptions"
            :placeholder="t('content.tag.field.creatorId')"
            style="width: 220px"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="`${item.id} (${item.name}${item.code ? ` / ${item.code}` : ''})`"
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
      title="content.tag.title"
      showAddBtn
      :show-remove-btn="true"
      :disable-delete="selectedTagIds.length === 0"
      :total="pageOpts.total"
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
      @add="handleAdd"
      @remove="handleBatchDelete"
      @change="fetchData"
    >
      <el-table
        v-loading="loading"
        :data="tagList"
        border
        fit
        highlight-current-row
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="name" :label="t('content.tag.field.name')" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="gotoDetail(row.id)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="slug" :label="t('content.tag.field.slug')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="usageCount" :label="t('content.tag.field.usageCount')" width="120" />
        <el-table-column prop="creatorId" :label="t('content.tag.field.creatorId')" width="110" />
        <el-table-column prop="createdAt" :label="t('common.time.create')" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) || t('common.none.title') }}
          </template>
        </el-table-column>
        <el-table-column :label="t('common.operation.title')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row.id)">
              {{ t('common.action.edit') }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">
              {{ t('common.action.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <TagCreateDialog
        v-model="createDialogVisible"
        :mode="dialogMode"
        :tag-id="currentTagId"
        @success="handleCreateSuccess"
      />
    </TableContainer>
  </div>
</template>

<style scoped>
.list-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>

