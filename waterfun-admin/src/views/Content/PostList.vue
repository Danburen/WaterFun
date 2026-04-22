<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { formatDate } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { getCategoryOptions } from "~/api/category";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import {
  deletePostById,
  deletePosts,
  listPosts,
  type PostResp,
  type PostStatus,
} from "~/api/post";
import { getUserOptions } from "~/api/user";
import type { PageOptions } from "~/types/api";
import PostCreateDialog from "~/views/Content/components/PostCreateDialog.vue";

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const loadingOptions = ref(false);
const postList = ref<PostResp[]>([]);
const selectedPostIds = ref<string[]>([]);
const createDialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentPostId = ref<string>("");
const categoryOptions = ref<OptionResItem<number>[]>([]);
const userOptions = ref<OptionResItem<string>[]>([]);

const searchForm = ref<{
  title: string;
  status: PostStatus | "";
  categoryId: number | null;
  authorId: string | null;
  slug: string;
}>({
  title: "",
  status: "",
  categoryId: null,
  authorId: null,
  slug: "",
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const postStatusOptions: { label: string; value: PostStatus }[] = [
  { label: "content.post.status.draft", value: "DRAFT" },
  { label: "content.post.status.pending", value: "PENDING" },
  { label: "content.post.status.published", value: "PUBLISHED" },
  { label: "content.post.status.rejected", value: "REJECTED" },
  { label: "content.post.status.archived", value: "ARCHIVED" },
];

const loadOptions = async () => {
  loadingOptions.value = true;
  try {
    const [categoryRes, userRes] = await Promise.all([getCategoryOptions(), getUserOptions()]);
    categoryOptions.value = categoryRes.data || [];
    userOptions.value = userRes.data || [];
  } catch (e) {
    console.error(e);
    ElMessage.error(t("error.fetch"));
  } finally {
    loadingOptions.value = false;
  }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listPosts({
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      title: searchForm.value.title || undefined,
      status: searchForm.value.status || undefined,
      categoryId: searchForm.value.categoryId ?? undefined,
      authorId: searchForm.value.authorId ?? undefined,
      slug: searchForm.value.slug || undefined,
    });
    postList.value = res.data.content || [];
    pageOpts.value.total = res.data.page?.totalElements || 0;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("error.fetch"));
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
    title: "",
    status: "",
    categoryId: null,
    authorId: null,
    slug: "",
  };
  fetchData();
};

const handleAdd = () => {
  dialogMode.value = "create";
  currentPostId.value = "";
  createDialogVisible.value = true;
};

const handleEdit = (id?: string | number) => {
  if (!id) return;
  dialogMode.value = "edit";
  currentPostId.value = String(id);
  createDialogVisible.value = true;
};

const gotoDetail = (id?: string | number) => {
  if (!id) return;
  router.push({ name: "contentPostDetail", params: { id: String(id) } });
};

const handleCreateSuccess = () => {
  fetchData();
};

const handleSelectionChange = (rows: PostResp[]) => {
  selectedPostIds.value = rows.map((item) => item.id).filter((id) => !!id);
};

const handleDelete = async (id?: string | number) => {
  if (!id) return;
  try {
    await ElMessageBox.confirm(t("content.post.confirm.delete"), t("common.action.delete"), {
      type: "warning",
    });
    await deletePostById(id);
    ElMessage.success(t("content.post.success.delete"));
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error(t("content.post.error.delete"));
    }
  }
};

const handleBatchDelete = async () => {
  if (!selectedPostIds.value.length) return;

  try {
    await ElMessageBox.confirm(
      t("content.post.confirm.batchDelete", { count: selectedPostIds.value.length }),
      t("common.action.delete"),
      { type: "warning" }
    );

    const res = await deletePosts(selectedPostIds.value);
    const result = res.data;

    if (result.success === result.requested) {
      ElMessage.success(t("content.post.success.delete"));
    } else if (result.success === 0) {
      ElMessage.error(t("content.post.error.delete"));
    } else {
      ElMessage.warning(`${t("content.post.success.delete")} ${result.success}/${result.requested}`);
    }

    selectedPostIds.value = [];
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error(t("content.post.error.delete"));
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
        <el-form-item :label="t('content.post.field.title')">
          <el-input v-model="searchForm.title" :placeholder="t('content.post.input.title')" />
        </el-form-item>
        <el-form-item :label="t('content.post.field.status')">
          <el-select v-model="searchForm.status" clearable style="width: 150px">
            <el-option
              v-for="item in postStatusOptions"
              :key="item.value"
              :label="t(item.label)"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('content.post.field.categoryId')">
          <el-select
            v-model="searchForm.categoryId"
            clearable
            filterable
            :loading="loadingOptions"
            :placeholder="t('content.post.input.categoryId')"
            style="width: 220px"
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="`${item.id} (${item.name})`"
              :value="item.id"
              :disabled="item.disabled || false"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('content.post.field.authorId')">
          <el-select
            v-model="searchForm.authorId"
            clearable
            filterable
            :loading="loadingOptions"
            :placeholder="t('content.post.input.authorId')"
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
        <el-form-item :label="t('content.post.field.slug')">
          <el-input v-model="searchForm.slug" :placeholder="t('content.post.input.slug')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('common.query.title') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset.title') }}</el-button>
        </el-form-item>
      </el-form>
    </SearchContainer>

    <TableContainer
      title="content.post.title"
      showAddBtn
      :show-remove-btn="true"
      :disable-delete="selectedPostIds.length === 0"
      :total="pageOpts.total"
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
      @add="handleAdd"
      @remove="handleBatchDelete"
      @change="fetchData"
    >
      <el-table
        v-loading="loading"
        :data="postList"
        border
        fit
        highlight-current-row
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="200" />
        <el-table-column prop="title" :label="t('content.post.field.title')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="gotoDetail(row.id)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('content.post.field.status')" width="130">
          <template #default="{ row }">
            {{ row.status ? t(`content.post.status.${row.status.toLowerCase()}`) : t('common.none.title') }}
          </template>
        </el-table-column>
        <el-table-column prop="categoryId" :label="t('content.post.field.categoryId')" width="110" />
        <el-table-column prop="authorId" :label="t('content.post.field.authorId')" width="110" />
        <el-table-column prop="slug" :label="t('content.post.field.slug')" min-width="150" show-overflow-tooltip />
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

      <PostCreateDialog
        v-model="createDialogVisible"
        :mode="dialogMode"
        :post-id="currentPostId"
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

