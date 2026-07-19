<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { formatDate } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import { getCategoryOptions } from "~/api/category";
import ListPage from "~/components/ListPage.vue";
import { deletePostById, deletePosts, listPosts, type PostResp, type PostStatus } from "~/api/post";
import { getUserOptions } from "~/api/user";
import type { PageOptions } from "~/types/api";
import { ElMessage } from "element-plus";

const router = useRouter();
const loading = ref(false);
const loadingOptions = ref(false);
const postList = ref<PostResp[]>([]);
const selectedPostIds = ref<Set<string>>(new Set());
const categoryOptions = ref<OptionResItem<number>[]>([]);
const userOptions = ref<OptionResItem<string>[]>([]);

const searchForm = ref<{ title: string; status: PostStatus | ""; categoryId: string | null; authorId: string | null; slug: string }>({
  title: "", status: "", categoryId: null, authorId: null, slug: "",
});

const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 10, total: 0 });

const postStatusLabel: Record<string, string> = { DRAFT: "草稿", PENDING: "待审核", PUBLISHED: "已发布", REJECTED: "已拒绝", ARCHIVED: "已归档" };
const postStatusBadge: Record<string, string> = { DRAFT: "badge-gray", PENDING: "badge-yellow", PUBLISHED: "badge-green", REJECTED: "badge-red", ARCHIVED: "badge-gray" };
const postTypeLabel: Record<string, string> = { COMMON: "普通", NOTICE: "公告" };
const postTypeBadge: Record<string, string> = { COMMON: "badge-gray", NOTICE: "badge-yellow" };

const loadOptions = async () => {
  loadingOptions.value = true;
  try {
    const [categoryRes, userRes] = await Promise.all([getCategoryOptions(), getUserOptions()]);
    categoryOptions.value = categoryRes.data || [];
    userOptions.value = userRes.data || [];
  } catch (e) { console.error(e); ElMessage.error('获取数据失败'); } finally { loadingOptions.value = false; }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listPosts({
      page: pageOpts.value.currentPage || 1, size: pageOpts.value.pageSize,
      title: searchForm.value.title || undefined, status: searchForm.value.status || undefined,
      categoryId: searchForm.value.categoryId ?? undefined, authorId: searchForm.value.authorId ?? undefined,
      slug: searchForm.value.slug || undefined,
    });
    postList.value = res.data.content || [];
    pageOpts.value.total = res.data.totalElements ?? res.data.page?.totalElements ?? 0;
  } catch (e) { console.error(e); ElMessage.error('获取数据失败'); } finally { loading.value = false; }
};

const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleReset = () => { pageOpts.value.currentPage = 1; searchForm.value = { title: "", status: "", categoryId: null, authorId: null, slug: "" }; fetchData(); };
const handleAdd = () => { router.push({ name: "contentPostEditor" }); };
const handleEdit = (id?: string | number) => { if (!id) return; router.push({ name: "contentPostEditor", query: { id: String(id) } }); };
const gotoDetail = (id?: string | number) => { if (!id) return; router.push({ name: "contentPostDetail", params: { id: String(id) } }); };
const handleCreateSuccess = () => fetchData();

const toggleSelect = (id: string) => { const s = new Set(selectedPostIds.value); s.has(id) ? s.delete(id) : s.add(id); selectedPostIds.value = s; };
const toggleSelectAll = () => {
  if (selectedPostIds.value.size === postList.value.length) selectedPostIds.value = new Set();
  else selectedPostIds.value = new Set(postList.value.map(r => r.id).filter(Boolean));
};

const handleDelete = async (id?: string | number) => {
  if (!id) return;
  try {
    await ElMessageBox.confirm('确定删除该文章吗？', '删除', { type: "warning" });
    await deletePostById(id); ElMessage.success('文章删除成功'); fetchData();
  } catch (e) { if (e !== "cancel") { console.error(e); ElMessage.error('文章删除失败'); } }
};

const handleBatchDelete = async () => {
  const ids = [...selectedPostIds.value];
  if (!ids.length) return;
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 篇文章吗？`, '删除', { type: "warning" });
    const res = await deletePosts(ids);
    const result = res.data;
    if (result.success === result.requested) ElMessage.success('文章删除成功');
    else if (result.success === 0) ElMessage.error('文章删除失败');
    else ElMessage.warning(`文章删除成功 ${result.success}/${result.requested}`);
    selectedPostIds.value = new Set(); fetchData();
  } catch (e) { if (e !== "cancel") { console.error(e); ElMessage.error('文章删除失败'); } }
};

onMounted(() => { fetchData(); loadOptions(); });
</script>

<template>
  <ListPage title="文章管理" :loading="loading" :total="pageOpts.total" v-model:page="pageOpts.currentPage" v-model:pageSize="pageOpts.pageSize" @change="fetchData">
    <template #search>
      <div class="search-form">
        <div class="search-field"><label>标题</label><input v-model="searchForm.title" placeholder="请输入标题" @keyup.enter="handleSearch" /></div>
        <div class="search-field">
          <label>状态</label>
          <select v-model="searchForm.status">
            <option value="">全部</option>
            <option v-for="(label, key) in postStatusLabel" :key="key" :value="key">{{ label }}</option>
          </select>
        </div>
        <div class="search-field">
          <label>分类</label>
          <select v-model="searchForm.categoryId">
            <option :value="null">全部</option>
            <option v-for="item in categoryOptions" :key="item.id" :value="item.id" :disabled="item.disabled">{{ item.id }} ({{ item.name }})</option>
          </select>
        </div>
        <div class="search-field">
          <label>作者</label>
          <select v-model="searchForm.authorId">
            <option :value="null">全部</option>
            <option v-for="item in userOptions" :key="item.id" :value="item.id" :disabled="item.disabled">{{ item.id }} ({{ item.name }})</option>
          </select>
        </div>
        <div class="search-field"><label>标识符</label><input v-model="searchForm.slug" placeholder="唯一标识符" @keyup.enter="handleSearch" /></div>
        <div class="search-actions">
          <button class="btn btn-primary" @click="handleSearch">查询</button>
          <button class="btn btn-default" @click="handleReset">重置</button>
        </div>
      </div>
    </template>
    <template #header>
      <button class="btn btn-primary" @click="handleAdd"><i class="fa-solid fa-plus"></i> 新增</button>
      <button class="btn btn-danger" :disabled="selectedPostIds.size === 0" @click="handleBatchDelete"><i class="fa-solid fa-trash"></i> 删除</button>
    </template>

    <table class="data-table">
      <thead>
        <tr>
          <th style="width:40px"><input type="checkbox" :checked="selectedPostIds.size === postList.length && postList.length > 0" @change="toggleSelectAll" /></th>
          <th style="width:80px">ID</th>
          <th>标题</th>
          <th style="width:90px">状态</th>
          <th style="width:80px">分类ID</th>
          <th style="width:80px">作者ID</th>
          <th>标识符</th>
          <th style="width:70px">类型</th>
          <th style="width:60px">置顶</th>
          <th style="width:160px">创建时间</th>
          <th style="width:160px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in postList" :key="row.id">
          <td><input type="checkbox" :checked="selectedPostIds.has(row.id)" @change="toggleSelect(row.id)" /></td>
          <td>{{ row.id }}</td>
          <td><a class="link" @click="gotoDetail(row.id)">{{ row.title }}</a></td>
          <td><span :class="['badge', postStatusBadge[row.status?.toUpperCase() || ''] || 'badge-gray']">{{ postStatusLabel[row.status?.toUpperCase() || ''] || '未知' }}</span></td>
          <td>{{ row.categoryId ?? '全局公告' }}</td>
          <td>{{ row.authorId ?? '系统' }}</td>
          <td>{{ row.slug }}</td>
          <td><span :class="['badge', postTypeBadge[row.type || ''] || 'badge-gray']">{{ postTypeLabel[row.type || ''] || '普通' }}</span></td>
          <td>{{ row.isPinned ? '✅' : '❌' }}</td>
          <td>{{ formatDate(row.createdAt) || '无' }}</td>
          <td>
            <div class="table-actions">
              <button class="action-btn" @click="handleEdit(row.id)">编辑</button>
              <button class="action-btn danger" @click="handleDelete(row.id)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </ListPage>
</template>
