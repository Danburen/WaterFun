<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { formatISOData } from "@waterfun/web-core/src/timer";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import { getPermOptions, getPermUsers, getPermission, type PermissionResp } from "~/api/permission";
import PermEditDialog from "./components/PermEditDialog.vue";

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const permissionId = computed(() => Number(route.params.id));
const loading = ref(false);
const permOptions = ref<OptionResItem[]>([]);
const permissionDetail = ref<PermissionResp | null>(null);

const editDialogVisible = ref(false);
type SimpleOption = { id: number; name: string };
const assignedUserOptions = ref<SimpleOption[]>([]);
const collapseActive = ref(["users"]);

const fetchPermOptions = async () => {
  try {
    const res = await getPermOptions();
    permOptions.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("permission.error.fetch"));
  }
};

const fetchPermDetail = async () => {
  if (Number.isNaN(permissionId.value)) {
    ElMessage.error(t("permission.error.invalidId"));
    router.back();
    return;
  }

  loading.value = true;
  try {
    const res = await getPermission(permissionId.value);
    permissionDetail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("permission.error.fetch"));
  } finally {
    loading.value = false;
  }
};

const openEditDialog = () => {
  if (Number.isNaN(permissionId.value)) return;
  editDialogVisible.value = true;
};

const gotoPermissionDetail = (id: number) => {
  router.push({ name: "permissionDetail", params: { id } });
};

const gotoUserDetail = (uid: string) => {
  router.push({ name: "userDetail", params: { uid: String(uid) } });
};

const fetchPermUsersPreview = async () => {
  if (Number.isNaN(permissionId.value)) return;
  try {
    const all: SimpleOption[] = [];
    let page = 0;
    let totalPages = 1;
    while (page < totalPages) {
      const res = await getPermUsers(permissionId.value, page, 100);
      (res.data.content || []).forEach((item) => {
        all.push({ id: item.userUid, name: `${item.username}${item.nickname ? ` (${item.nickname})` : ""}` });
      });
      totalPages = res.data.page.totalPages || 0;
      page += 1;
    }
    assignedUserOptions.value = all;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("permission.error.fetchUsers"));
  }
};

const handleEditSuccess = async () => {
  await Promise.all([fetchPermDetail(), fetchPermOptions()]);
};

onMounted(async () => {
  await Promise.all([fetchPermOptions(), fetchPermDetail(), fetchPermUsersPreview()]);
});
</script>

<template>
  <div class="perm-detail" v-loading="loading">
    <CardContainer title="permission.detail">
      <template #header-right>
        <el-button text @click="router.back()">{{ t("common.action.back") }}</el-button>
        <el-button type="primary" plain @click="openEditDialog">{{ t("common.action.edit") }}</el-button>
      </template>

      <el-descriptions v-if="permissionDetail" :column="2" border>
        <el-descriptions-item label="ID">{{ permissionDetail.id }}</el-descriptions-item>
        <el-descriptions-item :label="t('permission.name')">{{ permissionDetail.name }}</el-descriptions-item>
        <el-descriptions-item :label="t('permission.code')">{{ permissionDetail.code || t('common.none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('permission.type.title')">
          {{ t(`permission.type.${permissionDetail.type.toLowerCase()}`) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('permission.resource')">{{ permissionDetail.resource || t('common.none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('permission.parentId')">
          <el-link
            v-if="permissionDetail.parentId != null"
            type="primary"
            :underline="false"
            @click="gotoPermissionDetail(permissionDetail.parentId)"
          >
            {{ permissionDetail.parentId }}
          </el-link>
          <span v-else>{{ t('common.none.title') }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('permission.weight')">
          {{ permissionDetail.orderWeight ?? t('common.none.title') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('permission.isSystem')">
          <el-tag size="small" :type="permissionDetail.isSystem ? 'warning' : 'info'">
            {{ permissionDetail.isSystem ? t('common.boolean.yes') : t('common.boolean.no') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('permission.description')">
          {{ permissionDetail.description || t('common.none.title') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.time.create')">{{ formatISOData(permissionDetail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.time.update')">{{ formatISOData(permissionDetail.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
    </CardContainer>

    <CardContainer style="margin-top: 12px" title="permission.assignedUsers">
      <el-collapse v-model="collapseActive">
        <el-collapse-item :title="t('permission.assignedUsers')" name="users">
          <el-row :gutter="10">
            <el-col v-for="item in assignedUserOptions" :key="`user-${item.id}`" :xs="24" :sm="12" :md="8" :lg="6" class="option-col">
              <el-link type="primary" :underline="false" @click="gotoUserDetail(item.id)">{{ item.name }}</el-link>
            </el-col>
            <el-col v-if="assignedUserOptions.length === 0" :span="24">{{ t('common.none.description') }}</el-col>
          </el-row>
        </el-collapse-item>
      </el-collapse>
    </CardContainer>

    <PermEditDialog
      v-model="editDialogVisible"
      mode="edit"
      :permission-id="permissionId"
      :perm-options="permOptions"
      :disabled-parent-ids="[permissionId]"
      @success="handleEditSuccess"
    />
  </div>
</template>

<style scoped>
.option-col {
  margin-bottom: 8px;
}
</style>


