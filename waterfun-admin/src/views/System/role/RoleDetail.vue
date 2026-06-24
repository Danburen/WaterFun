<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { useRoute, useRouter } from "vue-router";
import { getRole, getRoleAllIds, listRoleUsers, listRolePerms, type RoleResp } from "~/api/role";
import { ElMessage } from "element-plus";
import RoleEditDialog from "./components/RoleEditDialog.vue";

const route = useRoute();
const router = useRouter();

const roleId = computed(() => Number(route.params.id));
const loading = ref(false);
const roleOptions = ref<OptionResItem[]>([]);
const roleDetail = ref<RoleResp | null>(null);
const editDialogVisible = ref(false);

type SimpleOption = { id: number | string; name: string };
const assignedPermissionOptions = ref<SimpleOption[]>([]);
const assignedUserOptions = ref<SimpleOption[]>([]);
const collapsePerms = ref(true);
const collapseUsers = ref(true);

const fetchRoleOptions = async () => {
  try { const res = await getRoleAllIds(); roleOptions.value = res.data; }
  catch { ElMessage.error('获取角色信息失败'); }
};

const fetchRoleDetail = async () => {
  if (Number.isNaN(roleId.value)) { ElMessage.error('无效的角色 ID'); router.back(); return; }
  loading.value = true;
  try { const res = await getRole(roleId.value); roleDetail.value = res.data; }
  catch { ElMessage.error('获取角色信息失败'); }
  finally { loading.value = false; }
};

const fetchAssignedPermissions = async () => {
  if (Number.isNaN(roleId.value)) return;
  try {
    const all: SimpleOption[] = []; let page = 0; let totalPages = 1;
    while (page < totalPages) {
      const res = await listRolePerms(roleId.value, { page, size: 100 });
      (res.data.content || []).forEach(item => all.push({ id: item.id, name: `${item.name} (${item.code || item.id})` }));
      totalPages = (res.data.totalPages ?? res.data.page?.totalPages ?? 0) || 0; page += 1;
    }
    assignedPermissionOptions.value = all;
  } catch { ElMessage.error('获取角色权限失败'); }
};

const fetchRoleUsersPreview = async () => {
  if (Number.isNaN(roleId.value)) return;
  try {
    const all: SimpleOption[] = []; let page = 0; let totalPages = 1;
    while (page < totalPages) {
      const res = await listRoleUsers(roleId.value, { page, size: 100 });
      (res.data.content || []).forEach(item => all.push({ id: item.userUid, name: `${item.username}${item.nickname ? ` (${item.nickname})` : ""}` }));
      totalPages = (res.data.totalPages ?? res.data.page?.totalPages ?? 0) || 0; page += 1;
    }
    assignedUserOptions.value = all;
  } catch { ElMessage.error('获取角色用户失败'); }
};

const openEditDialog = () => { if (!Number.isNaN(roleId.value)) editDialogVisible.value = true; };
const gotoAssignPermPage = () => { if (!Number.isNaN(roleId.value)) router.push({ name: "rolePermissionAssign", params: { id: roleId.value } }); };
const gotoAssignUserPage = () => { if (!Number.isNaN(roleId.value)) router.push({ name: "roleUserAssign", params: { id: roleId.value } }); };
const gotoRoleDetail = (id: number) => router.push({ name: "roleDetail", params: { id } });
const gotoPermissionDetail = (id: number) => router.push({ name: "permissionDetail", params: { id } });
const gotoUserDetail = (uid: number | string) => router.push({ name: "userDetail", params: { uid } });

const handleEditSuccess = async () => { await Promise.all([fetchRoleDetail(), fetchRoleOptions()]) };

onMounted(async () => { await Promise.all([fetchRoleOptions(), fetchRoleDetail(), fetchAssignedPermissions(), fetchRoleUsersPreview()]); });
</script>

<template>
  <div>
    <CardContainer title="角色详情">
      <template #header-right>
        <button class="btn" @click="router.back()">返回</button>
        <button class="btn btn-primary" @click="gotoAssignPermPage">分配权限</button>
        <button class="btn btn-primary" @click="gotoAssignUserPage">分配用户</button>
        <button class="btn btn-primary" @click="openEditDialog">编辑</button>
      </template>

      <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
      <table v-else-if="roleDetail" class="detail-table">
        <tr>
          <td class="label">ID</td>
          <td class="value">{{ roleDetail.id }}</td>
          <td class="label">角色名称</td>
          <td class="value">{{ roleDetail.name }}</td>
        </tr>
        <tr>
          <td class="label">角色编码</td>
          <td class="value">{{ roleDetail.code || '无' }}</td>
          <td class="label">父级角色</td>
          <td class="value">
            <span v-if="roleDetail.parentId != null" class="link" @click="gotoRoleDetail(roleDetail.parentId)">{{ roleDetail.parentId }}</span>
            <span v-else>无</span>
          </td>
        </tr>
        <tr>
          <td class="label">排序权重</td>
          <td class="value">{{ roleDetail.orderWeight ?? '无' }}</td>
          <td class="label">系统角色</td>
          <td class="value"><span :class="['badge', roleDetail.isSystem ? 'badge-yellow' : 'badge-gray']">{{ roleDetail.isSystem ? '是' : '否' }}</span></td>
        </tr>
        <tr>
          <td class="label">描述</td>
          <td class="value" colspan="3">{{ roleDetail.description || '无' }}</td>
        </tr>
        <tr>
          <td class="label">创建时间</td>
          <td class="value">{{ formatISOData(roleDetail.createdAt) }}</td>
          <td class="label">更新时间</td>
          <td class="value">{{ formatISOData(roleDetail.updatedAt) }}</td>
        </tr>
      </table>
    </CardContainer>

    <CardContainer style="margin-top: 12px" title="关联配置">
      <div class="collapse-section">
        <div class="collapse-title" @click="collapsePerms = !collapsePerms">
          已分配权限 ({{ assignedPermissionOptions.length }})
          <i :class="collapsePerms ? 'fa-solid fa-chevron-up' : 'fa-solid fa-chevron-down'"></i>
        </div>
        <div v-show="collapsePerms" class="collapse-content">
          <div v-if="assignedPermissionOptions.length" class="option-grid">
            <span v-for="item in assignedPermissionOptions" :key="`perm-${item.id}`" class="link" @click="gotoPermissionDetail(Number(item.id))">{{ item.name }}</span>
          </div>
          <span v-else style="color: var(--text-muted); font-size: 14px;">暂无数据</span>
        </div>
      </div>
      <div class="collapse-section">
        <div class="collapse-title" @click="collapseUsers = !collapseUsers">
          已分配用户 ({{ assignedUserOptions.length }})
          <i :class="collapseUsers ? 'fa-solid fa-chevron-up' : 'fa-solid fa-chevron-down'"></i>
        </div>
        <div v-show="collapseUsers" class="collapse-content">
          <div v-if="assignedUserOptions.length" class="option-grid">
            <span v-for="item in assignedUserOptions" :key="`user-${item.id}`" class="link" @click="gotoUserDetail(item.id)">{{ item.name }}</span>
          </div>
          <span v-else style="color: var(--text-muted); font-size: 14px;">暂无数据</span>
        </div>
      </div>
    </CardContainer>

    <RoleEditDialog v-model="editDialogVisible" mode="edit" :role-id="roleId" :role-options="roleOptions" :disabled-parent-ids="[roleId]" @success="handleEditSuccess" />
  </div>
</template>
