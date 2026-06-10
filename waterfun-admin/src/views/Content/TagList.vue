<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { formatDate } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import { getUserOptions } from "~/api/user";
import ListPage from "~/components/ListPage.vue";
import { deleteTag, deleteTags, listTags, type TagResp } from "~/api/tag";
import type { PageOptions } from "~/types/api";
import TagCreateDialog from "~/views/Content/components/TagCreateDialog.vue";
import { ElMessage } from "element-plus";

const router = useRouter();
const loading = ref(false);
const loadingOptions = ref(false);
const tagList = ref<TagResp[]>([]);
const selectedTagIds = ref<Set<number>>(new Set());
const createDialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentTagId = ref<number>(0);
const userOptions = ref<OptionResItem<string>[]>([]);

const searchForm = ref<{ name: string; slug: string; creatorId: string | null }>({ name: "", slug: "", creatorId: null });
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 10, total: 0 });

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listTags({ page: (pageOpts.value.currentPage || 1) - 1, size: pageOpts.value.pageSize, name: searchForm.value.name || undefined, slug: searchForm.value.slug || undefined, creatorId: searchForm.value.creatorId ?? undefined });
    tagList.value = res.data.content || [];
    pageOpts.value.total = res.data.totalElements ?? res.data.page?.totalElements ?? 0;
  } catch (e) { console.error(e); ElMessage.error('获取数据失败'); } finally { loading.value = false; }
};

const loadOptions = async () => {
  loadingOptions.value = true;
  try { const res = await getUserOptions(); userOptions.value = res.data || []; } catch (e) { console.error(e); ElMessage.error('获取数据失败'); } finally { loadingOptions.value = false; }
};

const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleReset = () => { pageOpts.value.currentPage = 1; searchForm.value = { name: "", slug: "", creatorId: null }; fetchData(); };
const handleAdd = () => { dialogMode.value = "create"; currentTagId.value = 0; createDialogVisible.value = true; };
const handleEdit = (id: number) => { dialogMode.value = "edit"; currentTagId.value = id; createDialogVisible.value = true; };
const gotoDetail = (id: number) => { router.push({ name: "contentTagDetail", params: { id } }); };
const handleCreateSuccess = () => fetchData();

const toggleSelect = (id: number) => { const s = new Set(selectedTagIds.value); s.has(id) ? s.delete(id) : s.add(id); selectedTagIds.value = s; };
const toggleSelectAll = () => {
  if (selectedTagIds.value.size === tagList.value.length) selectedTagIds.value = new Set();
  else selectedTagIds.value = new Set(tagList.value.map(r => r.id));
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该标签吗？', '删除', { type: "warning" });
    await deleteTag(id); ElMessage.success('标签删除成功'); fetchData();
  } catch (e) { if (e !== "cancel") { console.error(e); ElMessage.error('标签删除失败'); } }
};

const handleBatchDelete = async () => {
  const ids = [...selectedTagIds.value];
  if (!ids.length) return;
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个标签吗？`, '删除', { type: "warning" });
    const res = await deleteTags(ids);
    const result = res.data;
    if (result.success === result.requested) ElMessage.success('标签删除成功');
    else if (result.success === 0) ElMessage.error('标签删除失败');
    else ElMessage.warning(`标签删除成功 ${result.success}/${result.requested}`);
    selectedTagIds.value = new Set(); fetchData();
  } catch (e) { if (e !== "cancel") { console.error(e); ElMessage.error('标签删除失败'); } }
};

onMounted(() => { fetchData(); loadOptions(); });
</script>

<template>
  <ListPage title="标签管理" :loading="loading" :total="pageOpts.total" v-model:page="pageOpts.currentPage" v-model:pageSize="pageOpts.pageSize" @change="fetchData">
    <template #search>
      <div class="search-form">
        <div class="search-field"><label>标签名</label><input v-model="searchForm.name" placeholder="请输入标签名" @keyup.enter="handleSearch" /></div>
        <div class="search-field"><label>标识符</label><input v-model="searchForm.slug" placeholder="唯一标识符" @keyup.enter="handleSearch" /></div>
        <div class="search-field">
          <label>创建人</label>
          <select v-model="searchForm.creatorId">
            <option :value="null">全部</option>
            <option v-for="item in userOptions" :key="item.id" :value="item.id" :disabled="item.disabled">{{ item.id }} ({{ item.name }})</option>
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
      <button class="btn btn-danger" :disabled="selectedTagIds.size === 0" @click="handleBatchDelete"><i class="fa-solid fa-trash"></i> 删除</button>
    </template>

    <table class="data-table">
      <thead>
        <tr>
          <th style="width:40px"><input type="checkbox" :checked="selectedTagIds.size === tagList.length && tagList.length > 0" @change="toggleSelectAll" /></th>
          <th style="width:80px">ID</th>
          <th>标签名</th>
          <th>标识符</th>
          <th style="width:90px">使用次数</th>
          <th style="width:80px">创建人ID</th>
          <th style="width:160px">创建时间</th>
          <th style="width:160px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in tagList" :key="row.id">
          <td><input type="checkbox" :checked="selectedTagIds.has(row.id)" @change="toggleSelect(row.id)" /></td>
          <td>{{ row.id }}</td>
          <td><a class="link" @click="gotoDetail(row.id)">{{ row.name }}</a></td>
          <td>{{ row.slug }}</td>
          <td>{{ row.usageCount }}</td>
          <td>{{ row.creatorId }}</td>
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
    <TagCreateDialog v-model="createDialogVisible" :mode="dialogMode" :tag-id="currentTagId" @success="handleCreateSuccess" />
  </ListPage>
</template>
