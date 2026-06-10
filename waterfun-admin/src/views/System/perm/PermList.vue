<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import ListPage from "~/components/ListPage.vue";
import { deletePerms, deletePermission, getPermOptions, listPermissions, type PermissionResp, type PermissionType } from "~/api/permission";
import { ElMessage } from "element-plus";
import type { PageOptions } from "~/types";
import PermEditDialog from "./components/PermEditDialog.vue";

const router = useRouter();
const permissionList = ref<PermissionResp[]>([]);
const permOptions = ref<OptionResItem[]>([]);
const searchForm = ref<{ name: string; code: string; type: PermissionType | ""; resource: string; parentId: number | null }>({ name: "", code: "", type: "", resource: "", parentId: null });
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 10, total: 0 });
const loading = ref(false);
const dialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentPermId = ref<number | null>(null);
const selectedPermIds = ref<Set<number>>(new Set());

const permTypeLabel: Record<string, string> = { MENU: "菜单", BUTTON: "按钮", API: "接口", DATA: "数据", OTHER: "其他" };
const permTypeOptions: { label: string; value: PermissionType }[] = [
  { label: "菜单", value: "MENU" }, { label: "按钮", value: "BUTTON" }, { label: "接口", value: "API" }, { label: "数据", value: "DATA" }, { label: "其他", value: "OTHER" },
];

const fetchPermOptions = async () => {
  try { const res = await getPermOptions(); permOptions.value = res.data; } catch (e) { console.error(e); ElMessage.error('获取权限信息失败'); }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listPermissions({ page: (pageOpts.value.currentPage || 1) - 1, size: pageOpts.value.pageSize, name: searchForm.value.name || undefined, code: searchForm.value.code || undefined, type: searchForm.value.type || undefined, resource: searchForm.value.resource || undefined, parentId: searchForm.value.parentId ?? undefined });
    permissionList.value = res.data.content || [];
    pageOpts.value.total = res.data.totalElements ?? res.data.page?.totalElements ?? 0;
  } catch (e) { console.error(e); ElMessage.error('获取数据失败'); } finally { loading.value = false; }
};

const handleAdd = () => { dialogMode.value = "create"; currentPermId.value = null; dialogVisible.value = true; };
const handleEdit = (row: PermissionResp) => { dialogMode.value = "edit"; currentPermId.value = row.id; dialogVisible.value = true; };

const handleDelete = async (row: PermissionResp) => {
  try {
    await ElMessageBox.confirm('确定删除该权限吗？', '删除', { type: "warning" });
    await deletePermission(row.id); ElMessage.success('权限删除成功'); await fetchData();
  } catch (e) { if (e !== "cancel") { console.error(e); ElMessage.error('删除权限失败'); } }
};

const toggleSelect = (id: number) => { const s = new Set(selectedPermIds.value); s.has(id) ? s.delete(id) : s.add(id); selectedPermIds.value = s; };
const toggleSelectAll = () => {
  if (selectedPermIds.value.size === permissionList.value.length) selectedPermIds.value = new Set();
  else selectedPermIds.value = new Set(permissionList.value.map(r => r.id));
};

const handleBatchDelete = async () => {
  const ids = [...selectedPermIds.value];
  if (!ids.length) return;
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个权限吗？`, '删除', { type: "warning" });
    const res = await deletePerms(ids);
    const result = res.data;
    if (result.success === result.requested) ElMessage.success('权限删除成功');
    else if (result.success === 0) ElMessage.error('删除权限失败');
    else ElMessage.warning(`权限删除成功 ${result.success}/${result.requested}`);
    selectedPermIds.value = new Set(); await fetchData();
  } catch (e) { if (e !== "cancel") { console.error(e); ElMessage.error('删除权限失败'); } }
};

const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleReset = () => { pageOpts.value.currentPage = 1; searchForm.value = { name: "", code: "", type: "", resource: "", parentId: null }; fetchData(); };
const handleDialogSuccess = async () => { await Promise.all([fetchData(), fetchPermOptions()]); };
const gotoDetail = (row: PermissionResp | number) => { const id = typeof row === "number" ? row : row.id; router.push({ name: "permissionDetail", params: { id } }); };

onMounted(async () => { await Promise.all([fetchData(), fetchPermOptions()]); });
</script>

<template>
  <ListPage title="权限管理" :loading="loading" :total="pageOpts.total" v-model:page="pageOpts.currentPage" v-model:pageSize="pageOpts.pageSize" @change="fetchData">
    <template #search>
      <div class="search-form">
        <div class="search-field"><label>权限名称</label><input v-model="searchForm.name" placeholder="权限名称" @keyup.enter="handleSearch" /></div>
        <div class="search-field"><label>权限编码</label><input v-model="searchForm.code" placeholder="权限编码" @keyup.enter="handleSearch" /></div>
        <div class="search-field">
          <label>权限类型</label>
          <select v-model="searchForm.type">
            <option value="">全部</option>
            <option v-for="item in permTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>
        <div class="search-field"><label>资源标识</label><input v-model="searchForm.resource" placeholder="资源标识" @keyup.enter="handleSearch" /></div>
        <div class="search-field">
          <label>父级权限</label>
          <select v-model="searchForm.parentId">
            <option :value="null">全部</option>
            <option v-for="item in permOptions" :key="item.id" :value="item.id" :disabled="item.disabled">{{ item.id }} ({{ item.name }})</option>
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
      <button class="btn btn-danger" :disabled="selectedPermIds.size === 0" @click="handleBatchDelete"><i class="fa-solid fa-trash"></i> 删除</button>
    </template>

    <table class="data-table">
      <thead>
        <tr>
          <th style="width:40px"><input type="checkbox" :checked="selectedPermIds.size === permissionList.length && permissionList.length > 0" @change="toggleSelectAll" /></th>
          <th style="width:60px">ID</th>
          <th>权限名称</th>
          <th>权限编码</th>
          <th style="width:80px">类型</th>
          <th>资源标识</th>
          <th style="width:100px">父级ID</th>
          <th style="width:80px">排序</th>
          <th style="width:80px">系统</th>
          <th>描述</th>
          <th style="width:150px">创建时间</th>
          <th style="width:130px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in permissionList" :key="row.id">
          <td><input type="checkbox" :checked="selectedPermIds.has(row.id)" @change="toggleSelect(row.id)" :disabled="row.isSystem" /></td>
          <td>{{ row.id }}</td>
          <td><a class="link" @click="gotoDetail(row)">{{ row.name }}</a></td>
          <td>{{ row.code }}</td>
          <td>{{ permTypeLabel[row.type] || row.type }}</td>
          <td>{{ row.resource }}</td>
          <td><a v-if="row.parentId != null" class="link" @click="gotoDetail(row.parentId)">{{ row.parentId }}</a><span v-else>无</span></td>
          <td>{{ row.orderWeight }}</td>
          <td><span :class="['badge', row.isSystem ? 'badge-yellow' : 'badge-gray']">{{ row.isSystem ? '是' : '否' }}</span></td>
          <td>{{ row.description }}</td>
          <td>{{ formatISOData(row.createdAt) }}</td>
          <td>
            <div class="table-actions">
              <button class="action-btn" @click="handleEdit(row)">编辑</button>
              <button class="action-btn danger" @click="handleDelete(row)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <PermEditDialog v-model="dialogVisible" :mode="dialogMode" :permission-id="currentPermId" :perm-options="permOptions" :disabled-parent-ids="currentPermId ? [currentPermId] : []" @success="handleDialogSuccess" />
  </ListPage>
</template>
