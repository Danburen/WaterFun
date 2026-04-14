<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { generateFingerprint } from '@waterfun/web-core/src/fingerprint'
import {
  getCurrentUserInfo,
  logout,
  updateCurrentUserProfile,
  type AdminUserInfoResponse,
  type UpdateUserProfileRequest
} from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()

const loading = ref(false)
const saving = ref(false)
const profileDialogVisible = ref(false)

const userInfo = ref<AdminUserInfoResponse | null>(null)

const profileForm = reactive<UpdateUserProfileRequest>({
  bio: '',
  gender: 'UNKNOWN',
  birthday: '',
  residence: ''
})

const displayName = computed(() => {
  if (!userInfo.value) return 'Admin'
  return userInfo.value.nickname || userInfo.value.username || 'Admin'
})

const avatarUrl = computed(() => userInfo.value?.avatar?.url || '')

const fetchCurrentUserInfo = async () => {
  loading.value = true
  try {
    const res = await getCurrentUserInfo()
    userInfo.value = res.data
  } catch (e) {
    ElMessage.error('获取当前用户信息失败')
  } finally {
    loading.value = false
  }
}

const openProfileDialog = async () => {
  profileDialogVisible.value = true
  await fetchCurrentUserInfo()
}

const saveProfile = async () => {
  saving.value = true
  try {
    await updateCurrentUserProfile({
      bio: profileForm.bio?.trim() || undefined,
      gender: profileForm.gender || undefined,
      birthday: profileForm.birthday || undefined,
      residence: profileForm.residence?.trim() || undefined
    })
    ElMessage.success('个人资料更新成功')
    profileDialogVisible.value = false
  } catch (e) {
    ElMessage.error('个人资料更新失败')
  } finally {
    saving.value = false
  }
}

const handleChangePassword = () => {
  ElMessage.info('修改密码功能暂未开放')
}

const handleLogout = async () => {
  try {
    const deviceFp = await generateFingerprint()
    await logout(deviceFp)
  } catch (e) {
    // ignore logout api error and continue local cleanup
  } finally {
    authStore.removeToken()
    await router.push({ name: 'login' })
    ElMessage.success('已退出登录')
  }
}

onMounted(() => {
  fetchCurrentUserInfo()
})
</script>

<template>
  <div class="user-menu" v-loading="loading">
    <el-dropdown trigger="click">
      <el-button link class="dropdown-header">
        <el-avatar :src="avatarUrl" class="avatar">{{ displayName.slice(0, 1).toUpperCase() }}</el-avatar>
        <span class="display">{{ displayName }}</span>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item @click="openProfileDialog">个人中心</el-dropdown-item>
          <el-dropdown-item @click="handleChangePassword">修改密码</el-dropdown-item>
          <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <el-dialog
      v-model="profileDialogVisible"
      title="个人中心"
      width="580px"
      :close-on-click-modal="false"
    >
      <div class="profile-grid">
        <div class="left-panel">
          <el-avatar :src="avatarUrl" :size="64" class="mb-8">{{ displayName.slice(0, 1).toUpperCase() }}</el-avatar>
          <div class="title">{{ displayName }}</div>
          <div class="desc">账号：{{ userInfo?.username || '-' }}</div>
          <div class="desc">状态：{{ userInfo?.accountStatus || '-' }}</div>
          <div class="desc">角色：{{ userInfo?.roles?.join(', ') || '-' }}</div>
        </div>

        <div class="right-panel">
          <el-form label-width="90px">
            <el-form-item label="个人简介">
              <el-input
                v-model="profileForm.bio"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="请输入个人简介"
              />
            </el-form-item>
            <el-form-item label="性别">
              <el-select v-model="profileForm.gender" placeholder="请选择">
                <el-option label="未知" value="UNKNOWN" />
                <el-option label="男" value="MALE" />
                <el-option label="女" value="FEMALE" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
            <el-form-item label="生日">
              <el-date-picker
                v-model="profileForm.birthday"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择生日"
              />
            </el-form-item>
            <el-form-item label="居住地">
              <el-input
                v-model="profileForm.residence"
                maxlength="50"
                show-word-limit
                placeholder="请输入居住地"
              />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-menu {
  position: relative;
  display: inline-block;
}

.avatar {
  height: 32px;
  width: 32px;
  margin-right: 5px;
}

.dropdown-header {
  font-size: 14px;
  cursor: pointer;
  display: flex;
  height: 40px;
  color: var(--default-dark);
}

.display {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-grid {
  display: flex;
  gap: 18px;
}

.left-panel {
  min-width: 170px;
  border-right: 1px solid #f0f0f0;
  padding-right: 10px;
}

.right-panel {
  flex: 1;
}

.title {
  font-weight: 600;
  margin-bottom: 8px;
}

.desc {
  color: #909399;
  font-size: 12px;
  margin-bottom: 6px;
}

.mb-8 {
  margin-bottom: 8px;
}
</style>