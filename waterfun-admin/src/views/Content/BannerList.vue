<script setup lang="ts">
import { formatDate } from "@waterfun/web-core/src/timer";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { listBanners, type BannerPosition, type BannerResp, type BannerStatus } from "~/api/banner";
import type { PageOptions } from "~/types/api";
import BannerCreateDialog from "~/views/Content/components/BannerCreateDialog.vue";
import { ElMessage } from "element-plus";


const loading = ref(false);
const bannerList = ref<BannerResp[]>([]);
const createDialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentBannerId = ref<number>(0);

const searchForm = ref<{
  title: string;
  subtitle: string;
  position: BannerPosition | "";
  status: BannerStatus | "";
}>({
  title: "",
  subtitle: "",
  position: "",
  status: "",
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const positionOptions: { label: string; value: BannerPosition }[] = [
  { label: "首页", value: "HOME" },
  { label: "侧边栏", value: "SIDE" },
];

const statusOptions: { label: string; value: BannerStatus }[] = [
  { label: "显示", value: "SHOW" },
  { label: "隐藏", value: "HIDE" },
];

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listBanners({
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      title: searchForm.value.title || undefined,
      subtitle: searchForm.value.subtitle || undefined,
      position: searchForm.value.position || undefined,
      status: searchForm.value.status || undefined,
    });
    bannerList.value = res.data.content || [];
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
    subtitle: "",
    position: "",
    status: "",
  };
  fetchData();
};

const handleAdd = () => {
  dialogMode.value = "create";
  currentBannerId.value = 0;
  createDialogVisible.value = true;
};

const handleEdit = (id: number) => {
  dialogMode.value = "edit";
  currentBannerId.value = id;
  createDialogVisible.value = true;
};

const handleCreateSuccess = () => {
  fetchData();
};

onMounted(() => {
  fetchData();
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
        <el-form-item label="副标题">
          <el-input
            v-model="searchForm.subtitle"
            placeholder="请输入副标题"
          />
        </el-form-item>
        <el-form-item label="位置">
          <el-select
            v-model="searchForm.position"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in positionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
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
      title="轮播图管理"
      show-add-btn
      :show-remove-btn="false"
      :total="pageOpts.total"
      @add="handleAdd"
      @change="fetchData"
    >
      <el-table
        v-loading="loading"
        :data="bannerList"
        border
        fit
        highlight-current-row
        style="width: 100%"
      >
        <el-table-column
          prop="id"
          label="ID"
          width="90"
        />
        <el-table-column
          prop="title"
          label="标题"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="subtitle"
          label="副标题"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="position"
          label="位置"
          width="110"
        >
          <template #default="{ row }">
            {{ ({ home: '首页', side: '侧边栏' })[row.position?.toLowerCase()] || '无' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="110"
        >
          <template #default="{ row }">
            {{ ({ show: '显示', hide: '隐藏' })[row.status?.toLowerCase()] || '无' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="sortNo"
          label="排序"
          width="110"
        />
        <el-table-column
          prop="linkUrl"
          label="跳转链接"
          min-width="220"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-link
              v-if="row.linkUrl"
              :href="row.linkUrl"
              target="_blank"
              type="primary"
            >
              {{ row.linkUrl }}
            </el-link>
            <span v-else>无</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="startAt"
          label="开始时间"
          min-width="170"
        >
          <template #default="{ row }">
            {{ formatDate(row.startAt) || '无' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="endAt"
          label="结束时间"
          min-width="170"
        >
          <template #default="{ row }">
            {{ formatDate(row.endAt) || '无' }}
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
          prop="updatedAt"
          label="更新时间"
          min-width="170"
        >
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) || '无' }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="140"
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
          </template>
        </el-table-column>
      </el-table>
    </TableContainer>

    <BannerCreateDialog
      v-model="createDialogVisible"
      :mode="dialogMode"
      :banner-id="currentBannerId"
      @success="handleCreateSuccess"
    />
  </div>
</template>
