<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import {
  getRole,
  getRoleAllIds,
  getRoleUsers,
  listRolePerms,
  type RoleResp,
} from "~/api/role";
import RoleEditDialog from "./components/RoleEditDialog.vue";

const { t } = useI18n();
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
    ElMessage.error(t("role.error.fetch"));
  }
};

const fetchRoleDetail = async () => {
  if (Number.isNaN(roleId.value)) {
    ElMessage.error(t("role.error.invalidId"));
    router.back();
    return;
  }

  loading.value = true;
  try {
    const res = await getRole(roleId.value);
    roleDetail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("role.error.fetch"));
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
    ElMessage.error(t("role.error.fetchPermissions"));
  }
};

const fetchRoleUsersPreview = async () => {
  if (Number.isNaN(roleId.value)) return;
  try {
    const all: SimpleOption[] = [];
    let page = 0;
    let totalPages = 1;
    while (page < totalPages) {
      const res = await getRoleUsers(roleId.value, page, 100);
      (res.data.content || []).forEach((item) => {
        all.push({ id: item.userUid, name: `${item.username}${item.nickname ? ` (${item.nickname})` : ""}` });
      });
      totalPages = res.data.page.totalPages || 0;
      page += 1;
    }
    assignedUserOptions.value = all;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("role.error.fetchUsers"));
  }
};

const openEditDialog = () => {
  if (Number.isNaN(roleId.value)) return;
  editDialogVisible.value = true;
};

const handleEditSuccess = async () => {
  await Promise.all([fetchRoleDetail(), fetchRoleOptions()]);
};

const gotoPermissionDetail = (id: number) => {
  router.push({ name: "permissionDetail", params: { id } });
};

const gotoUserDetail = (uid: number) => {
  router.push({ name: "userDetail", params: { uid } });
};

const gotoRoleDetail = (id: number) => {
  router.push({ name: "roleDetail", params: { id } });
};

onMounted(async () => {
  await Promise.all([fetchRoleOptions(), fetchRoleDetail(), fetchAssignedPermissions(), fetchRoleUsersPreview()]);
});
</script>

<template>
  <div class="role-detail" v-loading="loading">
    <CardContainer title="role.detail">
      <template #header-right>
        <el-button text @click="router.back()">{{ t("back.title") }}</el-button>
        <el-button type="primary" plain @click="openEditDialog">{{ t("operation.edit") }}</el-button>
      </template>

      <el-descriptions v-if="roleDetail" :title="t('role.basicInfo')" :column="2" border>
        <el-descriptions-item label="ID">{{ roleDetail.id }}</el-descriptions-item>
        <el-descriptions-item :label="t('role.name')">{{ roleDetail.name }}</el-descriptions-item>
        <el-descriptions-item :label="t('role.code')">{{ roleDetail.code || t('none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('role.parentId')">
          <el-link v-if="roleDetail.parentId != null" type="primary" :underline="false" @click="gotoRoleDetail(roleDetail.parentId)">
            {{ roleDetail.parentId }}
          </el-link>
          <span v-else>{{ t('none.title') }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('role.weight')">{{ roleDetail.orderWeight ?? t('none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('role.isSystem')">
          <el-tag size="small" :type="roleDetail.isSystem ? 'warning' : 'info'">
            {{ roleDetail.isSystem ? t('yes.title') : t('no.title') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('role.description')">{{ roleDetail.description || t('none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('create.time')">{{ formatISOData(roleDetail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item :label="t('update.time')">{{ formatISOData(roleDetail.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
    </CardContainer>

    <CardContainer style="margin-top: 12px" title="role.permissionConfig">
      <el-collapse v-model="collapseActive">
        <el-collapse-item :title="t('role.assignedPermissions')" name="permissions">
          <el-row :gutter="10">
            <el-col v-for="item in assignedPermissionOptions" :key="`perm-${item.id}`" :xs="24" :sm="12" :md="8" :lg="6" class="option-col">
              <el-link type="primary" :underline="false" @click="gotoPermissionDetail(item.id)">{{ item.name }}</el-link>
            </el-col>
            <el-col v-if="assignedPermissionOptions.length === 0" :span="24">{{ t('none.description') }}</el-col>
          </el-row>
        </el-collapse-item>
        <el-collapse-item :title="t('role.assignedUsers')" name="users">
          <el-row :gutter="10">
            <el-col v-for="item in assignedUserOptions" :key="`user-${item.id}`" :xs="24" :sm="12" :md="8" :lg="6" class="option-col">
              <el-link type="primary" :underline="false" @click="gotoUserDetail(item.id)">{{ item.name }}</el-link>
            </el-col>
            <el-col v-if="assignedUserOptions.length === 0" :span="24">{{ t('none.description') }}</el-col>
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

