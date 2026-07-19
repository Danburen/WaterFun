<script setup lang="ts">
import ListPage from "~/components/ListPage.vue";
import { PageOptions } from "~/types";
import { deleteRoles, deleteRole, getRoleAllIds, listRoles, RoleResp } from "~/api/role";
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessage } from "element-plus";
import { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { useRouter } from "vue-router";
import RoleEditDialog from "./components/RoleEditDialog.vue";
import { ElMessageBox } from "element-plus";

const router = useRouter();
const roleList = ref<RoleResp[]>([]);
const roleOptions = ref<OptionResItem[]>([]);
const searchForm = ref({ name: '', code: '', parentId: null as number | null });
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 10, total: 0 });
const loading = ref(false);
const dialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentRoleId = ref<number | null>(null);
const selectedRoleIds = ref<Set<number>>(new Set());

const handleAdd = () => { dialogMode.value = "create"; currentRoleId.value = null; dialogVisible.value = true; };

const fetchRoleIds = () => {
  getRoleAllIds().then(res => { roleOptions.value = res.data; }).catch(e => { console.error(e); ElMessage.error('获取角色信息失败'); });
};

const fetchData = () => {
  loading.value = true;
  listRoles(pageOpts.value.currentPage, pageOpts.value.pageSize, searchForm.value.name, searchForm.value.code, searchForm.value.parentId)
    .then(res => { roleList.value = res.data.content || []; pageOpts.value.total = res.data.totalElements ?? res.data.page?.totalElements ?? 0; })
    .catch(e => { console.log(e); ElMessage.error('获取数据失败'); })
    .finally(() => { loading.value = false; });
};

const handleReset = () => { pageOpts.value.currentPage = 1; searchForm.value = { name: "", code: "", parentId: null }; fetchData(); };
const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleEdit = (row: RoleResp) => { dialogMode.value = "edit"; currentRoleId.value = row.id; dialogVisible.value = true; };

const handleDelete = async (row: RoleResp) => {
  try {
    await ElMessageBox.confirm('确定删除该角色吗？', '删除', { type: "warning" });
    await deleteRole(row.id);
    ElMessage.success('角色删除成功');
    fetchData();
  } catch (e) { if (e !== "cancel") { console.error(e); ElMessage.error('删除角色失败'); } }
};

const toggleSelect = (id: number) => {
  const s = new Set(selectedRoleIds.value);
  s.has(id) ? s.delete(id) : s.add(id);
  selectedRoleIds.value = s;
};
const toggleSelectAll = () => {
  if (selectedRoleIds.value.size === roleList.value.length) { selectedRoleIds.value = new Set(); }
  else { selectedRoleIds.value = new Set(roleList.value.map(r => r.id)); }
};

const handleBatchDelete = async () => {
  const ids = [...selectedRoleIds.value];
  if (!ids.length) return;
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个角色吗？`, '删除', { type: "warning" });
    const res = await deleteRoles(ids);
    const result = res.data;
    if (result.success === result.requested) ElMessage.success('角色删除成功');
    else if (result.success === 0) ElMessage.error('删除角色失败');
    else ElMessage.warning(`角色删除成功 ${result.success}/${result.requested}`);
    selectedRoleIds.value = new Set();
    fetchData();
  } catch (e) { if (e !== "cancel") { console.error(e); ElMessage.error('删除角色失败'); } }
};

const handleDialogSuccess = () => { fetchData(); fetchRoleIds(); };
const gotoDetail = (id: number) => { router.push({ name: "roleDetail", params: { id } }); };
const getParentRoleName = (parentId: number | null | undefined) => {
  if (parentId == null) return "";
  const option = roleOptions.value.find((r) => r.id === parentId);
  return option?.name || '无';
};

onMounted(() => { fetchData(); fetchRoleIds(); });
</script>

<template>
  <ListPage title="角色管理" :loading="loading" :total="pageOpts.total" v-model:page="pageOpts.currentPage" v-model:pageSize="pageOpts.pageSize" @change="fetchData">
    <template #search>
      <div class="search-form">
        <div class="search-field"><label>角色名称</label><input v-model="searchForm.name" placeholder="请输入角色名称" @keyup.enter="handleSearch" /></div>
        <div class="search-field"><label>角色编码</label><input v-model="searchForm.code" placeholder="请输入角色编码" @keyup.enter="handleSearch" /></div>
        <div class="search-field">
          <label>父级角色</label>
          <select v-model="searchForm.parentId">
            <option :value="null">全部</option>
            <option v-for="item in roleOptions" :key="item.id" :value="item.id" :disabled="item.disabled">{{ item.id }} ({{ item.name }})</option>
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
      <button class="btn btn-danger" :disabled="selectedRoleIds.size === 0" @click="handleBatchDelete"><i class="fa-solid fa-trash"></i> 删除</button>
    </template>

    <table class="data-table">
      <thead>
        <tr>
          <th style="width:40px"><input type="checkbox" :checked="selectedRoleIds.size === roleList.length && roleList.length > 0" @change="toggleSelectAll" /></th>
          <th style="width:80px">ID</th>
          <th>角色名称</th>
          <th>角色编码</th>
          <th>父级角色</th>
          <th style="width:80px">排序</th>
          <th>描述</th>
          <th style="width:160px">创建时间</th>
          <th style="width:220px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in roleList" :key="row.id">
          <td><input type="checkbox" :checked="selectedRoleIds.has(row.id)" @change="toggleSelect(row.id)" :disabled="row.isSystem" /></td>
          <td>{{ row.id }}</td>
          <td><a class="link" @click="gotoDetail(row.id)">{{ row.name }}</a></td>
          <td>{{ row.code }}</td>
          <td><a v-if="row.parentId != null" class="link" @click="gotoDetail(row.parentId)">{{ row.parentId }} ({{ getParentRoleName(row.parentId) }})</a><span v-else>无</span></td>
          <td>{{ row.orderWeight }}</td>
          <td>{{ row.description }}</td>
          <td>{{ formatISOData(row.createdAt) }}</td>
          <td>
            <div class="table-actions">
              <button class="action-btn" @click="handleEdit(row)">编辑</button>
              <button v-if="!row.isSystem" class="action-btn danger" @click="handleDelete(row)">删除</button>
              <div class="dropdown">
                <button class="action-btn">更多</button>
                <div class="dropdown-menu">
                  <a @click="router.push({ name: 'rolePermissionAssign', params: { id: row.id } })">分配权限</a>
                  <a @click="router.push({ name: 'roleUserAssign', params: { id: row.id } })">分配用户</a>
                </div>
              </div>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <RoleEditDialog v-model="dialogVisible" :mode="dialogMode" :role-id="currentRoleId" :role-options="roleOptions" :disabled-parent-ids="currentRoleId ? [currentRoleId] : []" @success="handleDialogSuccess" />
  </ListPage>
</template>
