<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { formatDate } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
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
import { ElMessage } from "element-plus";

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
  categoryId: string | null;
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
  { label: "草稿", value: "DRAFT" },
  { label: "待审核", value: "PENDING" },
  { label: "已发布", value: "PUBLISHED" },
  { label: "已拒绝", value: "REJECTED" },
  { label: "已归档", value: "ARCHIVED" },
];

const loadOptions = async () => {
  loadingOptions.value = true;
  try {
    const [categoryRes, userRes] = await Promise.all([getCategoryOptions(), getUserOptions()]);
    categoryOptions.value = categoryRes.data || [];
    userOptions.value = userRes.data || [];
  } catch (e) {
    console.error(e);
    ElMessage.error('获取数据失败');
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
    ElMessage.error('获取数据失败');
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
    await ElMessageBox.confirm('确定删除该文章吗？', '删除', {
      type: "warning",
    });
    await deletePostById(id);
    ElMessage.success('文章删除成功');
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error('文章删除失败');
    }
  }
};

const handleBatchDelete = async () => {
  if (!selectedPostIds.value.length) return;

  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedPostIds.value.length} 篇文章吗？`,
      '删除',
      { type: "warning" }
    );

    const res = await deletePosts(selectedPostIds.value);
    const result = res.data;

    if (result.success === result.requested) {
      ElMessage.success('文章删除成功');
    } else if (result.success === 0) {
      ElMessage.error('文章删除失败');
    } else {
      ElMessage.warning(`${'文章删除成功'} ${result.success}/${result.requested}`);
    }

    selectedPostIds.value = [];
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error('文章删除失败');
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
      <el-form
        inline
        class="search-form"
        :model="searchForm"
      >
        <el-form-item label="标题">
          <el-input
            v-model="searchForm.title"
            placeholder="请输入标题"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in postStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分类ID">
          <el-select
            v-model="searchForm.categoryId"
            clearable
            filterable
            :loading="loadingOptions"
            placeholder="请选择分类"
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
        <el-form-item label="作者ID">
          <el-select
            v-model="searchForm.authorId"
            clearable
            filterable
            :loading="loadingOptions"
            placeholder="请选择作者"
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
        <el-form-item label="唯一标识符">
          <el-input
            v-model="searchForm.slug"
            placeholder="请输入唯一标识符"
          />
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
      title="文章管理"
      show-add-btn
      :show-remove-btn="true"
      :disable-delete="selectedPostIds.length === 0"
      :total="pageOpts.total"
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
        <el-table-column
          type="selection"
          width="55"
        />
        <el-table-column
          prop="id"
          label="ID"
          width="200"
        />
        <el-table-column
          prop="title"
          label="标题"
          min-width="180"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-link
              type="primary"
              :underline="false"
              @click="gotoDetail(row.id)"
            >
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="130"
        >
          <template #default="{ row }">
            {{ ({ draft: '草稿', pending: '待审核', published: '已发布', rejected: '已拒绝', archived: '已归档' })[row.status?.toLowerCase()] || '无' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="categoryId"
          label="分类ID"
          width="110"
        />
        <el-table-column
          prop="authorId"
          label="作者ID"
          width="110"
        />
        <el-table-column
          prop="slug"
          label="唯一标识符"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="createdAt"
          label="创建时间"
          min-width="170"
        >
          <template #default="{ row }">
            {{ formatDate(row.createdAt) || '无' }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="180"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              @click="handleEdit(row.id)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="handleDelete(row.id)"
            >
              删除
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

