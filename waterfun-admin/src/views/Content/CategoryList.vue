<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { formatDate } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import ListPage from "~/components/ListPage.vue";
import { deleteCategories, deleteCategory, getCategoryOptions, listCategories, type CategoryResp } from "~/api/category";
import { getUserOptions } from "~/api/user";
import type { PageOptions } from "~/types/api";
import CategoryCreateDialog from "~/views/Content/components/CategoryCreateDialog.vue";
import { ElMessage } from "element-plus";
import RemoteSelect from "~/components/RemoteSelect.vue";

const router = useRouter();
const loading = ref(false);
const loadingOptions = ref(false);
const categoryList = ref<CategoryResp[]>([]);
const selectedCategoryIds = ref<Set<number>>(new Set());
const createDialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentCategoryId = ref<number>(0);
const categoryOptions = ref<OptionResItem<number>[]>([]);

const searchForm = ref<{ name: string; slug: string; parentId: number | null; creatorId: string | null }>({ name: "", slug: "", parentId: null, creatorId: null });
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 10, total: 0 });

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listCategories({ page: pageOpts.value.currentPage || 1, size: pageOpts.value.pageSize, name: searchForm.value.name || undefined, slug: searchForm.value.slug || undefined, parentId: searchForm.value.parentId ?? undefined, creatorId: searchForm.value.creatorId ?? undefined });
    categoryList.value = res.data.content || [];
    pageOpts.value.total = res.data.totalElements ?? res.data.page?.totalElements ?? 0;
  } catch (e) { console.error(e); ElMessage.error('获取数据失败'); } finally { loading.value = false; }
};

const loadOptions = async () => {
  loadingOptions.value = true;
  try { const categoryRes = await getCategoryOptions(); categoryOptions.value = categoryRes.data || []; } catch (e) { console.error(e); ElMessage.error('获取数据失败'); } finally { loadingOptions.value = false; }
};

const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleReset = () => { pageOpts.value.currentPage = 1; searchForm.value = { name: "", slug: "", parentId: null, creatorId: null }; fetchData(); };
const handleAdd = () => { dialogMode.value = "create"; currentCategoryId.value = 0; createDialogVisible.value = true; };
const handleEdit = (id: number | string) => { dialogMode.value = "edit"; currentCategoryId.value = Number(id); createDialogVisible.value = true; };
const gotoDetail = (id: number | string) => { router.push({ name: "contentCategoryDetail", params: { id } }); };
const handleCreateSuccess = () => fetchData();

const toggleSelect = (id: number) => { const s = new Set(selectedCategoryIds.value); s.has(id) ? s.delete(id) : s.add(id); selectedCategoryIds.value = s; };
const toggleSelectAll = () => {
  if (selectedCategoryIds.value.size === categoryList.value.length) selectedCategoryIds.value = new Set();
  else selectedCategoryIds.value = new Set(categoryList.value.map(r => r.id));
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该分类吗？', '删除', { type: "warning" });
    await deleteCategory(id); ElMessage.success('分类删除成功'); fetchData();
  } catch (e) { if (e !== "cancel") { console.error(e); ElMessage.error('分类删除失败'); } }
};

const handleBatchDelete = async () => {
  const ids = [...selectedCategoryIds.value];
  if (!ids.length) return;
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个分类吗？`, '删除', { type: "warning" });
    const res = await deleteCategories(ids);
    const result = res.data;
    if (result.success === result.requested) ElMessage.success('分类删除成功');
    else if (result.success === 0) ElMessage.error('分类删除失败');
    else ElMessage.warning(`分类删除成功 ${result.success}/${result.requested}`);
    selectedCategoryIds.value = new Set(); fetchData();
  } catch (e) { if (e !== "cancel") { console.error(e); ElMessage.error('分类删除失败'); } }
};

onMounted(() => { fetchData(); loadOptions(); });
</script>

<template>
  <ListPage title="分类管理" :loading="loading" :total="pageOpts.total" v-model:page="pageOpts.currentPage" v-model:pageSize="pageOpts.pageSize" @change="fetchData">
    <template #search>
      <div class="search-form">
        <div class="search-field"><label>分类名</label><input v-model="searchForm.name" placeholder="请输入分类名" @keyup.enter="handleSearch" /></div>
        <div class="search-field"><label>标识符</label><input v-model="searchForm.slug" placeholder="唯一标识符" @keyup.enter="handleSearch" /></div>
        <div class="search-field">
          <label>父级</label>
          <select v-model="searchForm.parentId">
            <option :value="null">全部</option>
            <option v-for="item in categoryOptions" :key="item.id" :value="item.id" :disabled="item.disabled">{{ item.id }} ({{ item.name }})</option>
          </select>
        </div>
        <div class="search-field">
          <label>创建人</label>
          <RemoteSelect
            :fetch-fn="(keyword, limit) => getUserOptions(keyword, limit).then(r => r.data ?? [])"
            :model-value="searchForm.creatorId"
            placeholder="全部"
            clearable
            @update:model-value="(v: any) => searchForm.creatorId = v ?? null"
          />
        </div>
        <div class="search-actions">
          <button class="btn btn-primary" @click="handleSearch">查询</button>
          <button class="btn btn-default" @click="handleReset">重置</button>
        </div>
      </div>
    </template>
    <template #header>
      <button class="btn btn-primary" @click="handleAdd"><i class="fa-solid fa-plus"></i> 新增</button>
      <button class="btn btn-danger" :disabled="selectedCategoryIds.size === 0" @click="handleBatchDelete"><i class="fa-solid fa-trash"></i> 删除</button>
    </template>

    <table class="data-table">
      <thead>
        <tr>
          <th style="width:40px"><input type="checkbox" :checked="selectedCategoryIds.size === categoryList.length && categoryList.length > 0" @change="toggleSelectAll" /></th>
          <th style="width:80px">ID</th>
          <th>分类名</th>
          <th>标识符</th>
          <th style="width:80px">父级ID</th>
          <th style="width:70px">排序</th>
          <th style="width:80px">创建人ID</th>
          <th style="width:80px">启用</th>
          <th style="width:160px">创建时间</th>
          <th style="width:160px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in categoryList" :key="row.id">
          <td><input type="checkbox" :checked="selectedCategoryIds.has(row.id)" @change="toggleSelect(row.id)" /></td>
          <td>{{ row.id }}</td>
          <td><a class="link" @click="gotoDetail(row.id)">{{ row.name }}</a></td>
          <td>{{ row.slug }}</td>
          <td>{{ row.parentId ?? '无' }}</td>
          <td>{{ row.sortOrder }}</td>
          <td>{{ row.creatorId }}</td>
          <td><span :class="['badge', row.isActive ? 'badge-green' : 'badge-gray']">{{ row.isActive ? '是' : '否' }}</span></td>
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
    <CategoryCreateDialog v-model="createDialogVisible" :mode="dialogMode" :category-id="currentCategoryId" @success="handleCreateSuccess" />
  </ListPage>
</template>
