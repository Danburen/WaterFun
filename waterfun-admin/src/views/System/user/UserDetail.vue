<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { useRoute, useRouter } from "vue-router";
import { getUserDetail, AccountStatusLabel, AccountStatusBadge, type AccountStatus, type UserDetailDto } from "~/api/user";
import { ElMessage } from "element-plus";
import UserEditDialog from "./components/UserEditDialog.vue";

const route = useRoute();
const router = useRouter();

const uid = computed(() => String(route.params.uid));
const loading = ref(false);
const detail = ref<UserDetailDto | null>(null);
const editDialogVisible = ref(false);
const collapseRoles = ref(true);
const collapsePerms = ref(true);

const statusBadgeClass: Record<AccountStatus, string> = AccountStatusBadge;

const isValidUid = computed(() => uid.value != null && uid.value !== '' && uid.value !== '0');

const gotoEdit = () => { if (isValidUid.value) editDialogVisible.value = true; };
const gotoAssignRolePage = () => { if (isValidUid.value) router.push({ name: "userRoleAssign", params: { uid: uid.value } }); };
const gotoAssignPermPage = () => { if (isValidUid.value) router.push({ name: "userPermissionAssign", params: { uid: uid.value } }); };
const gotoRoleDetail = (id: number) => router.push({ name: "roleDetail", params: { id } });
const gotoPermissionDetail = (id: number) => router.push({ name: "permissionDetail", params: { id } });
const handleEditSuccess = () => { fetchDetail() };

const fetchDetail = async () => {
  if (!isValidUid.value) { ElMessage.error('无效的用户UID'); router.back(); return; }
  loading.value = true;
  try { const res = await getUserDetail(uid.value); detail.value = res.data; }
  catch { ElMessage.error('获取用户详情失败'); }
  finally { loading.value = false; }
};

onMounted(async () => { await fetchDetail(); });
</script>

<template>
  <div>
    <CardContainer title="用户详情">
      <template #header-right>
        <button class="btn" @click="router.back()">返回</button>
        <button class="btn btn-primary" @click="gotoAssignRolePage">分配角色</button>
        <button class="btn btn-primary" @click="gotoAssignPermPage">分配权限</button>
        <button class="btn btn-primary" @click="gotoEdit">编辑</button>
      </template>

      <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
      <template v-else-if="detail">
        <table class="detail-table">
          <tbody>
            <tr>
              <td class="label">用户ID</td>
              <td class="value">{{ detail.info.uid }}</td>
              <td class="label">用户名</td>
              <td class="value">{{ detail.info.username }}</td>
            </tr>
            <tr>
              <td class="label">昵称</td>
              <td class="value">{{ detail.info.nickname || '无' }}</td>
              <td class="label">用户状态</td>
               <td class="value"><span :class="['badge', statusBadgeClass[detail.info.accountStatus]]">{{ AccountStatusLabel[detail.info.accountStatus] }}</span></td>
            </tr>
            <tr>
              <td class="label">创建时间</td>
              <td class="value">{{ formatISOData(detail.info.createdAt) }}</td>
              <td class="label">更新时间</td>
              <td class="value">{{ formatISOData(detail.info.updatedAt) }}</td>
            </tr>
            <tr>
              <td class="label">最后活跃</td>
              <td class="value">{{ formatISOData(detail.info.lastActiveAt) }}</td>
              <td class="label">头像</td>
              <td class="value"><img v-if="detail.info.avatar" :src="detail.info.avatar" style="width: 48px; height: 48px; border-radius: 50%; object-fit: cover;" /><span v-else>无</span></td>
            </tr>
          </tbody>
        </table>

        <table class="detail-table" style="margin-top: 12px;">
          <caption style="caption-side: top; text-align: left; padding: 12px 16px; font-weight: 600; font-size: 14px; background: var(--bg); border: 1px solid var(--border); border-bottom: none;">档案信息</caption>
          <tbody>
            <tr>
              <td class="label">简介</td>
              <td class="value">{{ detail.profile.bio || '无' }}</td>
              <td class="label">性别</td>
              <td class="value">{{ detail.profile.gender || '无' }}</td>
            </tr>
            <tr>
              <td class="label">生日</td>
              <td class="value">{{ detail.profile.birthDate || '无' }}</td>
              <td class="label">居住地</td>
              <td class="value">{{ detail.profile.residence || '无' }}</td>
            </tr>
          </tbody>
        </table>

        <table class="detail-table" style="margin-top: 12px;">
          <caption style="caption-side: top; text-align: left; padding: 12px 16px; font-weight: 600; font-size: 14px; background: var(--bg); border: 1px solid var(--border); border-bottom: none;">联系方式</caption>
          <tbody>
            <tr>
              <td class="label">邮箱</td>
              <td class="value">{{ detail.maskedData.emailMasked || '无' }}</td>
              <td class="label">手机号</td>
              <td class="value">{{ detail.maskedData.phoneMasked || '无' }}</td>
            </tr>
          </tbody>
        </table>

        <CardContainer style="margin-top: 12px" title="认证信息">
          <div class="collapse-section">
            <div class="collapse-title" @click="collapseRoles = !collapseRoles">
              角色管理 ({{ detail.roles.length }})
              <i :class="collapseRoles ? 'fa-solid fa-chevron-up' : 'fa-solid fa-chevron-down'"></i>
            </div>
            <div v-show="collapseRoles" class="collapse-content">
              <div v-if="detail.roles.length" class="option-grid">
                <span v-for="item in detail.roles" :key="`r-${item.id}`" class="link" @click="gotoRoleDetail(item.id)">{{ item.name }}</span>
              </div>
              <span v-else style="color: var(--text-muted); font-size: 14px;">暂无数据</span>
            </div>
          </div>
          <div class="collapse-section">
            <div class="collapse-title" @click="collapsePerms = !collapsePerms">
              权限管理 ({{ detail.permissions.length }})
              <i :class="collapsePerms ? 'fa-solid fa-chevron-up' : 'fa-solid fa-chevron-down'"></i>
            </div>
            <div v-show="collapsePerms" class="collapse-content">
              <div v-if="detail.permissions.length" class="option-grid">
                <span v-for="item in detail.permissions" :key="`p-${item.id}`" class="link" @click="gotoPermissionDetail(item.id)">{{ item.name }}</span>
              </div>
              <span v-else style="color: var(--text-muted); font-size: 14px;">暂无数据</span>
            </div>
          </div>
        </CardContainer>
      </template>
    </CardContainer>

    <UserEditDialog v-model="editDialogVisible" :uid="uid" @success="handleEditSuccess" />
  </div>
</template>

<style scoped>
.section-gap { margin-top: 12px; }
</style>
