<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { formatDate } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";

import { useRouter } from "vue-router";
import { getUserOptions } from "~/api/user";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { deleteTag, deleteTags, listTags, type TagResp } from "~/api/tag";
import type { PageOptions } from "~/types/api";
import TagCreateDialog from "~/views/Content/components/TagCreateDialog.vue";
import { ElMessage } from "element-plus";


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
    ElMessage.error('获取数据失败');
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
    await ElMessageBox.confirm('确定删除该标签吗？', '删除', {
      type: "warning",
    });
    await deleteTag(id);
    ElMessage.success('标签删除成功');
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error('标签删除失败');
    }
  }
};

const handleBatchDelete = async () => {
  if (!selectedTagIds.value.length) return;

  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedTagIds.value.length} 个标签吗？`,
      '删除',
      { type: "warning" }
    );

    const res = await deleteTags(selectedTagIds.value);
    const result = res.data;

    if (result.success === result.requested) {
      ElMessage.success('标签删除成功');
    } else if (result.success === 0) {
      ElMessage.error('标签删除失败');
    } else {
      ElMessage.warning(`${'标签删除成功'} ${result.success}/${result.requested}`);
    }

    selectedTagIds.value = [];
    fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error('标签删除失败');
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
        <el-form-item label="标签名">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入标签名"
          />
        </el-form-item>
        <el-form-item label="唯一标识符">
          <el-input
            v-model="searchForm.slug"
            placeholder="请输入唯一标识符"
          />
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
      title="标签管理"
      show-add-btn
      :show-remove-btn="true"
      :disable-delete="selectedTagIds.length === 0"
      :total="pageOpts.total"
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
          label="标签名"
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
          prop="usageCount"
          label="使用次数"
          width="120"
        />
        <el-table-column
          prop="creatorId"
          label="创建人ID"
          width="110"
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

