<script setup lang="ts">
import { formatDate } from "@waterfun/web-core/src/timer";
import { useRouter } from "vue-router";
import ListPage from "~/components/ListPage.vue";
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

interface SearchForm { title: string; subtitle: string; position: BannerPosition | ""; status: BannerStatus | ""; startAt: string; endAt: string; isDeleted: boolean | ""; }
const searchForm = ref<SearchForm>({ title: "", subtitle: "", position: "", status: "", startAt: "", endAt: "", isDeleted: "" });
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 10, total: 0 });

const getValidity = (row: BannerResp): { text: string; cls: string } => {
  const now = Date.now();
  const start = row.startAt ? new Date(row.startAt).getTime() : null;
  const end = row.endAt ? new Date(row.endAt).getTime() : null;
  if (start && now < start) return { text: "未生效", cls: "badge-yellow" };
  if (end && now >= end) return { text: "已过期", cls: "badge-red" };
  if (end === null && start && now >= start) return { text: "永久有效", cls: "badge-blue" };
  return { text: "生效中", cls: "badge-green" };
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listBanners({ page: (pageOpts.value.currentPage || 1) - 1, size: pageOpts.value.pageSize, title: searchForm.value.title || undefined, subtitle: searchForm.value.subtitle || undefined, position: searchForm.value.position || undefined, status: searchForm.value.status || undefined, startAt: searchForm.value.startAt || undefined, endAt: searchForm.value.endAt || undefined, isDeleted: searchForm.value.isDeleted === "" ? undefined : searchForm.value.isDeleted });
    bannerList.value = res.data.content || [];
    pageOpts.value.total = res.data.totalElements ?? res.data.page?.totalElements ?? 0;
  } catch (e) { console.error(e); ElMessage.error('获取数据失败'); } finally { loading.value = false; }
};

const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleReset = () => { pageOpts.value.currentPage = 1; searchForm.value = { title: "", subtitle: "", position: "", status: "", startAt: "", endAt: "", isDeleted: "" }; fetchData(); };
const handleAdd = () => { dialogMode.value = "create"; currentBannerId.value = ""; createDialogVisible.value = true; };
const handleEdit = (id: string) => { dialogMode.value = "edit"; currentBannerId.value = id; createDialogVisible.value = true; };
const handleDelete = async (id: string, title: string) => {
  try {
    await ElMessageBox.confirm(`确定要删除横幅「${title}」吗？`, "确认删除", { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" });
    await deleteBanner(id); ElMessage.success("删除成功"); fetchData();
  } catch { }
};
const handleCreateSuccess = () => fetchData();
const goDetail = (id: string) => { router.push({ name: "contentBannerDetail", params: { id } }); };

onMounted(() => { fetchData(); });
</script>

<template>
  <ListPage title="轮播图管理" :loading="loading" :total="pageOpts.total" v-model:page="pageOpts.currentPage" v-model:pageSize="pageOpts.pageSize" @change="fetchData">
    <template #search>
      <div class="search-form">
        <div class="search-field"><label>标题</label><input v-model="searchForm.title" placeholder="请输入标题" @keyup.enter="handleSearch" /></div>
        <div class="search-field"><label>副标题</label><input v-model="searchForm.subtitle" placeholder="请输入副标题" @keyup.enter="handleSearch" /></div>
        <div class="search-field">
          <label>位置</label>
          <select v-model="searchForm.position">
            <option value="">全部</option>
            <option value="HOME">首页</option>
            <option value="SIDE">侧边栏</option>
          </select>
        </div>
        <div class="search-field">
          <label>状态</label>
          <select v-model="searchForm.status">
            <option value="">全部</option>
            <option value="SHOW">显示</option>
            <option value="HIDE">隐藏</option>
          </select>
        </div>
        <div class="search-field">
          <label>删除状态</label>
          <select v-model="searchForm.isDeleted">
            <option value="">全部</option>
            <option :value="false">正常</option>
            <option :value="true">已删除</option>
          </select>
        </div>
        <div class="search-actions">
          <button class="btn btn-primary" @click="handleSearch">查询</button>
          <button class="btn btn-default" @click="handleReset">重置</button>
        </div>
      </div>
    </template>
    <template #header>
      <button class="btn btn-primary" @click="handleAdd"><i class="fa-solid fa-plus"></i> 新增</button>
    </template>

    <table class="data-table">
      <thead>
        <tr>
          <th style="width:120px">图片</th>
          <th>标题</th>
          <th style="width:90px">有效期</th>
          <th style="width:70px">位置</th>
          <th style="width:60px">排序</th>
          <th style="width:60px">状态</th>
          <th style="width:155px">启用时间</th>
          <th style="width:155px">结束时间</th>
          <th style="width:200px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in bannerList" :key="row.id">
          <td>
            <img v-if="row.coverageUrl?.url" :src="row.coverageUrl.url" style="width:100px;height:56px;border-radius:4px;object-fit:cover" />
            <span v-else style="color:#999;font-size:12px">无</span>
          </td>
          <td>{{ row.title }}</td>
          <td><span :class="['badge', getValidity(row).cls]">{{ getValidity(row).text }}</span></td>
          <td>{{ row.position === 'HOME' ? '首页' : '侧边栏' }}</td>
          <td>{{ row.sortNo }}</td>
          <td><span :class="['badge', row.status === 'SHOW' ? 'badge-green' : 'badge-gray']">{{ row.status === 'SHOW' ? '显示' : '隐藏' }}</span></td>
          <td>{{ formatDate(row.startAt) || '无' }}</td>
          <td>{{ formatDate(row.endAt) || '无' }}</td>
          <td>
            <div class="table-actions">
              <button class="action-btn" @click="goDetail(String(row.id))">详情</button>
              <button class="action-btn" @click="handleEdit(String(row.id))">编辑</button>
              <button class="action-btn danger" @click="handleDelete(String(row.id), row.title)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <BannerCreateDialog v-model="createDialogVisible" :mode="dialogMode" :banner-id="currentBannerId" @success="handleCreateSuccess" />
  </ListPage>
</template>
