<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import type { OptionResItem } from "@waterfun/web-core/src/types";

import { useRoute, useRouter } from "vue-router";
import {
  getRole,
  getRoleAllIds,
  listRoleUsers,
  listRolePerms,
  type RoleResp,
} from "~/api/role";
import { ElMessage } from "element-plus";
import RoleEditDialog from "./components/RoleEditDialog.vue";


const route = useRoute();
const router = useRouter();

const roleId = computed(() => Number(route.params.id));
const loading = ref(false);
const roleOptions = ref<OptionResItem[]>([]);
const roleDetail = ref<RoleResp | null>(null);

const editDialogVisible = ref(false);
type SimpleOption = { id: number; name: string };
const assignedPermissionOptions = ref<SimpleOption[]>([]);
const assignedUserOptions = ref<SimpleOption[]>([]);
const collapseActive = ref(["permissions", "users"]);

const fetchRoleOptions = async () => {
  try {
    const res = await getRoleAllIds();
    roleOptions.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取角色信息失败');
  }
};

const fetchRoleDetail = async () => {
  if (Number.isNaN(roleId.value)) {
    ElMessage.error('无效的角色 ID');
    router.back();
    return;
  }

  loading.value = true;
  try {
    const res = await getRole(roleId.value);
    roleDetail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取角色信息失败');
  } finally {
    loading.value = false;
  }
};

const fetchAssignedPermissions = async () => {
  if (Number.isNaN(roleId.value)) return;
  try {
    const all: SimpleOption[] = [];
    let page = 0;
    let totalPages = 1;
    while (page < totalPages) {
      const res = await listRolePerms(roleId.value, { page, size: 100 });
      (res.data.content || []).forEach((item) => {
        all.push({ id: item.id, name: `${item.name} (${item.code || item.id})` });
      });
      totalPages = res.data.page.totalPages || 0;
      page += 1;
    }
    assignedPermissionOptions.value = all;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取角色权限失败');
  }
};

const fetchRoleUsersPreview = async () => {
  if (Number.isNaN(roleId.value)) return;
  try {
    const all: SimpleOption[] = [];
    let page = 0;
    let totalPages = 1;
    while (page < totalPages) {
      const res = await listRoleUsers(roleId.value, { page, size: 100 });
      (res.data.content || []).forEach((item) => {
        all.push({ id: item.userUid, name: `${item.username}${item.nickname ? ` (${item.nickname})` : ""}` });
      });
      totalPages = res.data.page.totalPages || 0;
      page += 1;
    }
    assignedUserOptions.value = all;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取角色用户失败');
  }
};

const openEditDialog = () => {
  if (Number.isNaN(roleId.value)) return;
  editDialogVisible.value = true;
};

const gotoAssignPermPage = () => {
  if (Number.isNaN(roleId.value)) return;
  router.push({ name: "rolePermissionAssign", params: { id: roleId.value } });
};

const gotoAssignUserPage = () => {
  if (Number.isNaN(roleId.value)) return;
  router.push({ name: "roleUserAssign", params: { id: roleId.value } });
};

const handleEditSuccess = async () => {
  await Promise.all([fetchRoleDetail(), fetchRoleOptions()]);
};

const gotoPermissionDetail = (id: number) => {
  router.push({ name: "permissionDetail", params: { id } });
};

const gotoUserDetail = (uid: string) => {
  router.push({ name: "userDetail", params: { uid: String(uid) } });
};

const gotoRoleDetail = (id: number) => {
  router.push({ name: "roleDetail", params: { id } });
};

onMounted(async () => {
  await Promise.all([fetchRoleOptions(), fetchRoleDetail(), fetchAssignedPermissions(), fetchRoleUsersPreview()]);
});
</script>

<template>
  <div
    v-loading="loading"
    class="role-detail"
  >
    <CardContainer title="role.detail">
      <template #header-right>
        <el-button
          text
          @click="router.back()"
        >
          返回
        </el-button>
        <el-button
          type="primary"
          plain
          @click="gotoAssignPermPage"
        >
          分配权限
        </el-button>
        <el-button
          type="primary"
          plain
          @click="gotoAssignUserPage"
        >
          分配用户
        </el-button>
        <el-button
          type="primary"
          plain
          @click="openEditDialog"
        >
          编辑
        </el-button>
      </template>

      <el-descriptions
        v-if="roleDetail"
        title="基础信息"
        :column="2"
        border
      >
        <el-descriptions-item label="ID">
          {{ roleDetail.id }}
        </el-descriptions-item>
        <el-descriptions-item label="角色名称">
          {{ roleDetail.name }}
        </el-descriptions-item>
        <el-descriptions-item label="角色编码">
          {{ roleDetail.code || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="父级角色ID">
          <el-link
            v-if="roleDetail.parentId != null"
            type="primary"
            :underline="false"
            @click="gotoRoleDetail(roleDetail.parentId)"
          >
            {{ roleDetail.parentId }}
          </el-link>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="排序权重">
          {{ roleDetail.orderWeight ?? '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="系统角色">
          <el-tag
            size="small"
            :type="roleDetail.isSystem ? 'warning' : 'info'"
          >
            {{ roleDetail.isSystem ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="角色描述">
          {{ roleDetail.description || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatISOData(roleDetail.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatISOData(roleDetail.updatedAt) }}
        </el-descriptions-item>
      </el-descriptions>
    </CardContainer>

    <CardContainer
      style="margin-top: 12px"
      title="role.permissionConfig"
    >
      <el-collapse v-model="collapseActive">
        <el-collapse-item
          title="已分配权限"
          name="permissions"
        >
          <el-row :gutter="10">
            <el-col
              v-for="item in assignedPermissionOptions"
              :key="`perm-${item.id}`"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="6"
              class="option-col"
            >
              <el-link
                type="primary"
                :underline="false"
                @click="gotoPermissionDetail(item.id)"
              >
                {{ item.name }}
              </el-link>
            </el-col>
            <el-col
              v-if="assignedPermissionOptions.length === 0"
              :span="24"
            >
              暂无数据
            </el-col>
          </el-row>
        </el-collapse-item>
        <el-collapse-item
          title="已分配用户"
          name="users"
        >
          <el-row :gutter="10">
            <el-col
              v-for="item in assignedUserOptions"
              :key="`user-${item.id}`"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="6"
              class="option-col"
            >
              <el-link
                type="primary"
                :underline="false"
                @click="gotoUserDetail(item.id)"
              >
                {{ item.name }}
              </el-link>
            </el-col>
            <el-col
              v-if="assignedUserOptions.length === 0"
              :span="24"
            >
              暂无数据
            </el-col>
          </el-row>
        </el-collapse-item>
      </el-collapse>
    </CardContainer>

    <RoleEditDialog
      v-model="editDialogVisible"
      mode="edit"
      :role-id="roleId"
      :role-options="roleOptions"
      :disabled-parent-ids="[roleId]"
      @success="handleEditSuccess"
    />
  </div>
</template>

<style scoped>
.option-col {
  margin-bottom: 8px;
}
</style>


