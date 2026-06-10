<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { formatISOData } from "@waterfun/web-core/src/timer";
import { useRoute, useRouter } from "vue-router";
import { getPermOptions, getPermUsers, getPermission, type PermissionResp } from "~/api/permission";
import { ElMessage } from "element-plus";
import PermEditDialog from "./components/PermEditDialog.vue";

const route = useRoute();
const router = useRouter();

const permissionId = computed(() => Number(route.params.id));
const loading = ref(false);
const permOptions = ref<OptionResItem[]>([]);
const permissionDetail = ref<PermissionResp | null>(null);
const editDialogVisible = ref(false);
const collapseUsers = ref(true);

type SimpleOption = { id: string; name: string };
const assignedUserOptions = ref<SimpleOption[]>([]);

const fetchPermOptions = async () => {
  try { const res = await getPermOptions(); permOptions.value = res.data; }
  catch { ElMessage.error('获取权限信息失败'); }
};

const fetchPermDetail = async () => {
  if (Number.isNaN(permissionId.value)) { ElMessage.error('无效的权限 ID'); router.back(); return; }
  loading.value = true;
  try { const res = await getPermission(permissionId.value); permissionDetail.value = res.data; }
  catch { ElMessage.error('获取权限信息失败'); }
  finally { loading.value = false; }
};

const fetchPermUsersPreview = async () => {
  if (Number.isNaN(permissionId.value)) return;
  try {
    const all: SimpleOption[] = []; let page = 0; let totalPages = 1;
    while (page < totalPages) {
      const res = await getPermUsers(permissionId.value, page, 100);
      (res.data.content || []).forEach(item => all.push({ id: item.userUid, name: `${item.username}${item.nickname ? ` (${item.nickname})` : ""}` }));
      totalPages = (res.data.totalPages ?? res.data.page?.totalPages ?? 0) || 0; page += 1;
    }
    assignedUserOptions.value = all;
  } catch { ElMessage.error('获取权限用户失败'); }
};

const openEditDialog = () => { if (!Number.isNaN(permissionId.value)) editDialogVisible.value = true; };
const gotoPermissionDetail = (id: number) => router.push({ name: "permissionDetail", params: { id } });
const gotoUserDetail = (uid: string) => router.push({ name: "userDetail", params: { uid: String(uid) } });
const handleEditSuccess = async () => { await Promise.all([fetchPermDetail(), fetchPermOptions()]) };

onMounted(async () => { await Promise.all([fetchPermOptions(), fetchPermDetail(), fetchPermUsersPreview()]); });
</script>

<template>
  <div>
    <CardContainer title="权限详情">
      <template #header-right>
        <button class="btn" @click="router.back()">返回</button>
        <button class="btn btn-primary" @click="openEditDialog">编辑</button>
      </template>

      <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
      <table v-else-if="permissionDetail" class="detail-table">
        <tbody>
        <tr>
          <td class="label">ID</td>
          <td class="value">{{ permissionDetail.id }}</td>
          <td class="label">权限名称</td>
          <td class="value">{{ permissionDetail.name }}</td>
        </tr>
        <tr>
          <td class="label">权限编码</td>
          <td class="value">{{ permissionDetail.code || '无' }}</td>
          <td class="label">权限类型</td>
          <td class="value">{{ ({ menu: '菜单', button: '按钮', api: '接口', data: '数据', other: '其他' })[permissionDetail.type.toLowerCase()] || '其他' }}</td>
        </tr>
        <tr>
          <td class="label">资源标识</td>
          <td class="value">{{ permissionDetail.resource || '无' }}</td>
          <td class="label">父级权限</td>
          <td class="value">
            <span v-if="permissionDetail.parentId != null" class="link" @click="gotoPermissionDetail(permissionDetail.parentId)">{{ permissionDetail.parentId }}</span>
            <span v-else>无</span>
          </td>
        </tr>
        <tr>
          <td class="label">排序权重</td>
          <td class="value">{{ permissionDetail.orderWeight ?? '无' }}</td>
          <td class="label">系统权限</td>
          <td class="value"><span :class="['badge', permissionDetail.isSystem ? 'badge-yellow' : 'badge-gray']">{{ permissionDetail.isSystem ? '是' : '否' }}</span></td>
        </tr>
        <tr>
          <td class="label">描述</td>
          <td class="value" colspan="3">{{ permissionDetail.description || '无' }}</td>
        </tr>
        <tr>
          <td class="label">创建时间</td>
          <td class="value">{{ formatISOData(permissionDetail.createdAt) }}</td>
          <td class="label">更新时间</td>
          <td class="value">{{ formatISOData(permissionDetail.updatedAt) }}</td>
        </tr>
      </tbody>
      </table>
    </CardContainer>

    <CardContainer style="margin-top: 12px" title="已分配用户">
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

    <PermEditDialog v-model="editDialogVisible" mode="edit" :permission-id="permissionId" :perm-options="permOptions" :disabled-parent-ids="[permissionId]" @success="handleEditSuccess" />
  </div>
</template>
