<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { useRoute, useRouter } from "vue-router";
import { getUserDetail, type AccountStatus, type UserDetailDto } from "~/api/user";
import { ElMessage } from "element-plus";
import UserEditDialog from "./components/UserEditDialog.vue";

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

const statusLabel = (status: AccountStatus) => status.toLowerCase();

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
    ElMessage.error('无效的用户UID');
    router.back();
    return;
  }

  loading.value = true;
  try {
    const res = await getUserDetail(uid.value);
    detail.value = res.data;
  } catch (error) {
    console.error(error);
    ElMessage.error('获取用户详情失败');
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
    <CardContainer title="用户详情">
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
          @click="gotoAssignRolePage"
        >
          分配角色
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
          @click="gotoEdit"
        >
          编辑
        </el-button>
      </template>

      <el-descriptions
        v-if="detail"
        title="基本信息"
        :column="2"
        border
      >
        <el-descriptions-item label="用户ID">
          {{ detail.info.uid }}
        </el-descriptions-item>
        <el-descriptions-item label="用户名">
          {{ detail.info.username }}
        </el-descriptions-item>
        <el-descriptions-item label="昵称">
          {{ detail.info.nickname || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="用户状态">
          <el-tag
            size="small"
            :type="statusTypeMap[detail.info.accountStatus]"
          >
            {{ statusLabel(detail.info.accountStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatISOData(detail.info.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatISOData(detail.info.updatedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="最后活跃">
          {{ formatISOData(detail.info.lastActiveAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="头像">
          <el-image
            v-if="detail.info.avatarUrl"
            :src="detail.info.avatarUrl"
            style="width: 48px; height: 48px; border-radius: 50%"
            fit="cover"
          />
          <span v-else>无</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions
        v-if="detail"
        title="档案信息"
        :column="2"
        border
        class="section-gap"
      >
        <el-descriptions-item label="简介">
          {{ detail.profile.bio || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ detail.profile.gender || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="生日">
          {{ detail.profile.birthDate || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="居住地">
          {{ detail.profile.residence || '无' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions
        v-if="detail"
        title="联系方式"
        :column="2"
        border
        class="section-gap"
      >
        <el-descriptions-item label="邮箱">
          {{ detail.maskedData.emailMasked || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          {{ detail.maskedData.phoneMasked || '无' }}
        </el-descriptions-item>
      </el-descriptions>

      <CardContainer
        v-if="detail"
        class="section-gap"
        title="认证信息"
      >
        <el-collapse v-model="collapseActive">
          <el-collapse-item
            title="角色管理"
            name="roles"
          >
            <el-row :gutter="10">
              <el-col
                v-for="item in detail.roles"
                :key="`r-${item.id}`"
                :xs="24"
                :sm="12"
                :md="8"
                :lg="6"
                class="option-col"
              >
                <el-link
                  type="primary"
                  :underline="false"
                  @click="gotoRoleDetail(item.id)"
                >
                  {{ item.name }}
                </el-link>
              </el-col>
              <el-col
                v-if="detail.roles.length === 0"
                :span="24"
              >
                暂无数据
              </el-col>
            </el-row>
          </el-collapse-item>
          <el-collapse-item
            title="权限管理"
            name="permissions"
          >
            <el-row :gutter="10">
              <el-col
                v-for="item in detail.permissions"
                :key="`p-${item.id}`"
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
                v-if="detail.permissions.length === 0"
                :span="24"
              >
                暂无数据
              </el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>
      </CardContainer>

      <UserEditDialog
        v-model="editDialogVisible"
        :uid="uid"
        @success="handleEditSuccess"
      />
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


