<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import { getUserDetail, type AccountStatus, type UserDetailDto } from "~/api/user";
import UserEditDialog from "./components/UserEditDialog.vue";

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const uid = computed(() => String(route.params.uid ?? ""));
const loading = ref(false);
const detail = ref<UserDetailDto | null>(null);
const editDialogVisible = ref(false);
const collapseActive = ref(["roles", "permissions"]);

const statusTypeMap: Record<AccountStatus, "success" | "warning" | "danger" | "info"> = {
  ACTIVE: "success",
  SUSPENDED: "warning",
  DEACTIVATED: "danger",
  DELETED: "info",
};

const statusLabel = (status: AccountStatus) => t(`user.statusMap.${status.toLowerCase()}`);

const isValidUid = computed(() => /^\d+$/.test(uid.value));

const gotoEdit = () => {
  if (!isValidUid.value) return;
  editDialogVisible.value = true;
};

const gotoAssignRolePage = () => {
  if (!isValidUid.value) return;
  router.push({ name: "userRoleAssign", params: { uid: uid.value } });
};

const gotoAssignPermPage = () => {
  if (!isValidUid.value) return;
  router.push({ name: "userPermissionAssign", params: { uid: uid.value } });
};

const gotoRoleDetail = (id: number) => {
  router.push({ name: "roleDetail", params: { id } });
};

const gotoPermissionDetail = (id: number) => {
  router.push({ name: "permissionDetail", params: { id } });
};

const handleEditSuccess = () => {
  fetchDetail();
};

const fetchDetail = async () => {
  if (!isValidUid.value) {
    ElMessage.error(t("user.error.invalidId"));
    router.back();
    return;
  }

  loading.value = true;
  try {
    const res = await getUserDetail(uid.value);
    detail.value = res.data;
  } catch (error) {
    console.error(error);
    ElMessage.error(t("user.error.fetchDetail"));
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await fetchDetail();
});
</script>

<template>
  <div v-loading="loading">
    <CardContainer title="user.detail">
      <template #header-right>
        <el-button text @click="router.back()">{{ t("common.action.back") }}</el-button>
        <el-button type="primary" plain @click="gotoAssignRolePage">{{ t("role.assign") }}</el-button>
        <el-button type="primary" plain @click="gotoAssignPermPage">{{ t("permission.assign") }}</el-button>
        <el-button type="primary" plain @click="gotoEdit">{{ t("common.action.edit") }}</el-button>
      </template>

      <el-descriptions v-if="detail" :title="t('user.basicInfo')" :column="2" border>
        <el-descriptions-item :label="t('user.uid')">{{ detail.info.uid }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.username')">{{ detail.info.username }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.nickname')">{{ detail.info.nickname || t('common.none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.status')">
          <el-tag size="small" :type="statusTypeMap[detail.info.accountStatus]">
            {{ statusLabel(detail.info.accountStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.time.create')">{{ formatISOData(detail.info.createdAt) }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.time.update')">{{ formatISOData(detail.info.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.lastActiveAt')">{{ formatISOData(detail.info.lastActiveAt) }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.avatar')">
          <el-image
            v-if="detail.info.avatarUrl"
            :src="detail.info.avatarUrl"
            style="width: 48px; height: 48px; border-radius: 50%"
            fit="cover"
          />
          <span v-else>{{ t('common.none.title') }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions v-if="detail" :title="t('user.profileInfo')" :column="2" border class="section-gap">
        <el-descriptions-item :label="t('user.bio')">{{ detail.profile.bio || t('common.none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.gender')">{{ detail.profile.gender || t('common.none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.birthDate')">{{ detail.profile.birthDate || t('common.none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.residence')">{{ detail.profile.residence || t('common.none.title') }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions v-if="detail" :title="t('user.contactInfo')" :column="2" border class="section-gap">
        <el-descriptions-item :label="t('user.email')">{{ detail.maskedData.emailMasked || t('common.none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.phone')">{{ detail.maskedData.phoneMasked || t('common.none.title') }}</el-descriptions-item>
      </el-descriptions>

      <CardContainer v-if="detail" class="section-gap" title="user.authInfo">
        <el-collapse v-model="collapseActive">
          <el-collapse-item :title="t('role.title')" name="roles">
            <el-row :gutter="10">
              <el-col v-for="item in detail.roles" :key="`r-${item.id}`" :xs="24" :sm="12" :md="8" :lg="6" class="option-col">
                <el-link type="primary" :underline="false" @click="gotoRoleDetail(item.id)">{{ item.name }}</el-link>
              </el-col>
              <el-col v-if="detail.roles.length === 0" :span="24">{{ t('common.none.description') }}</el-col>
            </el-row>
          </el-collapse-item>
          <el-collapse-item :title="t('permission.title')" name="permissions">
            <el-row :gutter="10">
              <el-col v-for="item in detail.permissions" :key="`p-${item.id}`" :xs="24" :sm="12" :md="8" :lg="6" class="option-col">
                <el-link type="primary" :underline="false" @click="gotoPermissionDetail(item.id)">{{ item.name }}</el-link>
              </el-col>
              <el-col v-if="detail.permissions.length === 0" :span="24">{{ t('common.none.description') }}</el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>
      </CardContainer>

      <UserEditDialog v-model="editDialogVisible" :uid="uid" @success="handleEditSuccess" />
    </CardContainer>
  </div>
</template>

<style scoped>
.section-gap {
  margin-top: 12px;
}

.option-col {
  margin-bottom: 8px;
}
</style>


