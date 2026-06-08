<script setup lang="ts">
import { formatDate } from "@waterfun/web-core/src/timer";
import { useRouter } from "vue-router";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { listBanners, deleteBanner, type BannerPosition, type BannerResp, type BannerStatus } from "~/api/banner";
import type { PageOptions } from "~/types/api";
import BannerCreateDialog from "~/views/Content/components/BannerCreateDialog.vue";
import { ElMessage, ElMessageBox } from "element-plus";

const router = useRouter();

const loading = ref(false);
const bannerList = ref<BannerResp[]>([]);
const createDialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentBannerId = ref<string>("");

interface SearchForm {
  title: string;
  subtitle: string;
  position: BannerPosition | "";
  status: BannerStatus | "";
  startAt: string;
  endAt: string;
  isDeleted: boolean | "";
}

const searchForm = ref<SearchForm>({
  title: "",
  subtitle: "",
  position: "",
  status: "",
  startAt: "",
  endAt: "",
  isDeleted: "",
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

const deletedOptions: { label: string; value: boolean }[] = [
  { label: "正常", value: false },
  { label: "已删除", value: true },
];

const getValidity = (row: BannerResp): { text: string; type: "success" | "warning" | "danger" | "info" } => {
  const now = Date.now();
  const start = row.startAt ? new Date(row.startAt).getTime() : null;
  const end = row.endAt ? new Date(row.endAt).getTime() : null;
  if (start && now < start) return { text: "未生效", type: "warning" };
  if (end && now >= end) return { text: "已过期", type: "danger" };
  if (end === null && start && now >= start) return { text: "永久有效", type: "info" };
  return { text: "生效中", type: "success" };
};

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
      startAt: searchForm.value.startAt || undefined,
      endAt: searchForm.value.endAt || undefined,
      isDeleted: searchForm.value.isDeleted === "" ? undefined : searchForm.value.isDeleted,
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
    startAt: "",
    endAt: "",
    isDeleted: "",
  };
  fetchData();
};

const handleAdd = () => {
  dialogMode.value = "create";
  currentBannerId.value = "";
  createDialogVisible.value = true;
};

const handleEdit = (id: string) => {
  dialogMode.value = "edit";
  currentBannerId.value = id;
  createDialogVisible.value = true;
};

const handleDelete = async (id: string, title: string) => {
  try {
    await ElMessageBox.confirm(`确定要删除横幅「${title}」吗？`, "确认删除", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    });
    await deleteBanner(id);
    ElMessage.success("删除成功");
    fetchData();
  } catch {
    // cancelled or error
  }
};

const handleCreateSuccess = () => {
  fetchData();
};

const goDetail = (id: string) => {
  router.push({ name: "contentBannerDetail", params: { id } });
};

onMounted(() => {
  fetchData();
});
</script>

<template>
  <div class="list-layout">
    <SearchContainer>
      <el-form inline class="search-form" :model="searchForm">
        <el-form-item label="标题">
          <el-input v-model="searchForm.title" placeholder="请输入标题" clearable />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="searchForm.subtitle" placeholder="请输入副标题" clearable />
        </el-form-item>
        <el-form-item label="位置">
          <el-select v-model="searchForm.position" clearable placeholder="全部" style="width: 120px">
            <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="全部" style="width: 120px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="searchForm.startAt"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 190px"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="searchForm.endAt"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 190px"
          />
        </el-form-item>
        <el-form-item label="删除状态">
          <el-select v-model="searchForm.isDeleted" clearable placeholder="全部" style="width: 120px">
            <el-option v-for="item in deletedOptions" :key="String(item.value)" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
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
      <el-table v-loading="loading" :data="bannerList" border fit highlight-current-row style="width: 100%">
        <el-table-column label="图片" width="140" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.coverageUrl?.url"
              :src="row.coverageUrl.url"
              style="width: 120px; height: 68px; border-radius: 4px"
              fit="cover"
            />
            <span v-else style="color: #999; font-size: 12px">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column label="有效期状态" width="105" align="center">
          <template #default="{ row }">
            <el-tag :type="getValidity(row).type" size="small" effect="plain">
              {{ getValidity(row).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="position" label="位置" width="80" align="center">
          <template #default="{ row }">
            {{ ({ home: '首页', side: '侧边栏' })[row.position?.toLowerCase()] || '无' }}
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="70" align="center" />
        <el-table-column prop="status" label="状态" width="75" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SHOW' ? 'success' : 'info'" size="small" effect="plain">
              {{ row.status === 'SHOW' ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用时间" width="165">
          <template #default="{ row }">
            {{ formatDate(row.startAt) || '无' }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="165">
          <template #default="{ row }">
            {{ formatDate(row.endAt) || '无' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="goDetail(String(row.id))">详情</el-button>
            <el-button size="small" type="primary" @click="handleEdit(String(row.id))">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(String(row.id), row.title)">删除</el-button>
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
