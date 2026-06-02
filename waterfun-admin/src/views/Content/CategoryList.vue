<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { formatDate } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";

import { useRouter } from "vue-router";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { deleteCategories, deleteCategory, getCategoryOptions, listCategories, type CategoryResp } from "~/api/category";
import { getUserOptions } from "~/api/user";
import type { PageOptions } from "~/types/api";
import CategoryCreateDialog from "~/views/Content/components/CategoryCreateDialog.vue";
import { ElMessage } from "element-plus";


const router = useRouter();

const loading = ref(false);
const loadingOptions = ref(false);
const categoryList = ref<CategoryResp[]>([]);
const selectedCategoryIds = ref<number[]>([]);
const createDialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentCategoryId = ref<number>(0);
const categoryOptions = ref<OptionResItem<number>[]>([]);
const userOptions = ref<OptionResItem<string>[]>([]);

const searchForm = ref<{
  name: string;
  slug: string;
  parentId: number | null;
  creatorId: string | null;
}>({
  name: "",
  slug: "",
  parentId: null,
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
    const res = await listCategories({
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      name: searchForm.value.name || undefined,
      slug: searchForm.value.slug || undefined,
      parentId: searchForm.value.parentId ?? undefined,
      creatorId: searchForm.value.creatorId ?? undefined,
    });
    categoryList.value = res.data.content || [];
    pageOpts.value.total = res.data.page?.totalElements || 0;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取数据失败');
  } finally {
    loading.value = false;
  }
};

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

const handleSearch = () => {
  pageOpts.value.currentPage = 1;
  fetchData();
};

const handleReset = () => {
  pageOpts.value.currentPage = 1;
  searchForm.value = {
    name: "",
    slug: "",
    parentId: null,
    creatorId: null,
  };
  fetchData();
};

const handleAdd = () => {
  dialogMode.value = "create";
  currentCategoryId.value = 0;
  createDialogVisible.value = true;
};

const handleEdit = (id: number) => {
  dialogMode.value = "edit";
  currentCategoryId.value = id;
  createDialogVisible.value = true;
};

const gotoDetail = (id: number) => {
  router.push({ name: "contentCategoryDetail", params: { id } });
};

const handleCreateSuccess = () => {
  fetchData();
};

const handleSelectionChange = (rows: CategoryResp[]) => {
  selectedCategoryIds.value = rows.map((item) => item.id);
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该分类吗？', '删除', {
      type: "warning",
    });
    await deleteCategory(id);
    ElMessage.success('分类删除成功');
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error('分类删除失败');
    }
  }
};

const handleBatchDelete = async () => {
  if (!selectedCategoryIds.value.length) return;

  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedCategoryIds.value.length} 个分类吗？`,
      '删除',
      { type: "warning" }
    );

    const res = await deleteCategories(selectedCategoryIds.value);
    const result = res.data;

    if (result.success === result.requested) {
      ElMessage.success('分类删除成功');
    } else if (result.success === 0) {
      ElMessage.error('分类删除失败');
    } else {
      ElMessage.warning(`${'分类删除成功'} ${result.success}/${result.requested}`);
    }

    selectedCategoryIds.value = [];
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error('分类删除失败');
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
        <el-form-item label="分类名">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入分类名"
          />
        </el-form-item>
        <el-form-item label="唯一标识符">
          <el-input
            v-model="searchForm.slug"
            placeholder="请输入唯一标识符"
          />
        </el-form-item>
        <el-form-item label="父级ID">
          <el-select
            v-model="searchForm.parentId"
            clearable
            filterable
            :loading="loadingOptions"
            placeholder="父级ID"
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
        <el-form-item label="创建人ID">
          <el-select
            v-model="searchForm.creatorId"
            clearable
            filterable
            :loading="loadingOptions"
            placeholder="创建人ID"
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
      title="分类管理"
      show-add-btn
      :show-remove-btn="true"
      :disable-delete="selectedCategoryIds.length === 0"
      :total="pageOpts.total"
      @add="handleAdd"
      @remove="handleBatchDelete"
      @change="fetchData"
    >
      <el-table
        v-loading="loading"
        :data="categoryList"
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
          width="90"
        />
        <el-table-column
          prop="name"
          label="分类名"
          min-width="150"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-link
              type="primary"
              :underline="false"
              @click="gotoDetail(row.id)"
            >
              {{ row.name }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          prop="slug"
          label="唯一标识符"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="parentId"
          label="父级ID"
          width="110"
        />
        <el-table-column
          prop="sortOrder"
          label="排序"
          width="110"
        />
        <el-table-column
          prop="creatorId"
          label="创建人ID"
          width="110"
        />
        <el-table-column
          prop="isActive"
          label="是否启用"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.isActive ? 'success' : 'info'"
            >
              {{ row.isActive ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
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

      <CategoryCreateDialog
        v-model="createDialogVisible"
        :mode="dialogMode"
        :category-id="currentCategoryId"
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

