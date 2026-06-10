<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { generateFingerprint } from '@waterfun/web-core/src/fingerprint'
import { logout } from '@/api/auth'
import { getCurrentUserInfo, updateCurrentUserProfile } from '@/api/me'
import type { AdminUserInfoResponse, UpdateUserProfileRequest } from '@/api/me'
import { useAuthStore } from '@/stores/authStore'
import BaseDialog from '~/components/BaseDialog.vue'

const authStore = useAuthStore()

const loading = ref(false)
const saving = ref(false)
const profileDialogVisible = ref(false)
const userInfo = ref<AdminUserInfoResponse | null>(null)

const profileForm = reactive<UpdateUserProfileRequest>({
  bio: '', gender: 'UNKNOWN', birthday: '', residence: ''
})

const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || 'Admin')
const avatarUrl = computed(() => userInfo.value?.avatar?.url || '')

const fetchCurrentUserInfo = async () => {
  loading.value = true
  try { const res = await getCurrentUserInfo(); userInfo.value = res.data; Object.assign(profileForm, { bio: '', gender: 'UNKNOWN', birthday: '', residence: '' }) }
  catch { ElMessage.error('获取当前用户信息失败') }
  finally { loading.value = false }
}

const openProfileDialog = async () => {
  profileDialogVisible.value = true
  await fetchCurrentUserInfo()
}

const saveProfile = async () => {
  saving.value = true
  try {
    await updateCurrentUserProfile({ bio: profileForm.bio?.trim() || undefined, gender: profileForm.gender || undefined, birthday: profileForm.birthday || undefined, residence: profileForm.residence?.trim() || undefined })
    ElMessage.success('个人资料更新成功'); profileDialogVisible.value = false
  } catch { ElMessage.error('个人资料更新失败') }
  finally { saving.value = false }
}

const handleChangePassword = () => { ElMessage.info('修改密码功能暂未开放') }

const handleLogout = async () => {
  try { const deviceFp = await generateFingerprint(); await logout(deviceFp) } catch { /* ignore */ }
  finally { authStore.removeToken(); await router.push({ name: 'login' }); ElMessage.success('已退出登录') }
}

onMounted(() => { fetchCurrentUserInfo() })
</script>

<template>
  <div class="user-menu">
    <div class="dropdown">
      <button class="user-btn">
        <div class="user-avatar">
          <img v-if="avatarUrl" :src="avatarUrl" :alt="displayName" />
          <span v-else>{{ displayName.slice(0, 1).toUpperCase() }}</span>
        </div>
        <span class="user-name">{{ displayName }}</span>
        <i class="fa-solid fa-chevron-down" style="font-size: 10px; color: var(--text-muted);"></i>
      </button>
      <div class="dropdown-menu">
        <a @click="openProfileDialog"><i class="fa-regular fa-user"></i> 个人中心</a>
        <a @click="handleChangePassword"><i class="fa-solid fa-key"></i> 修改密码</a>
        <div class="dropdown-divider"></div>
        <a @click="handleLogout"><i class="fa-solid fa-sign-out-alt"></i> 退出登录</a>
      </div>
    </div>

    <BaseDialog v-model="profileDialogVisible" title="个人中心" width="600px">
      <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
      <div v-else class="profile-grid">
        <div class="left-panel">
          <div class="profile-avatar">
            <img v-if="avatarUrl" :src="avatarUrl" :alt="displayName" />
            <span v-else>{{ displayName.slice(0, 1).toUpperCase() }}</span>
          </div>
          <div class="profile-name">{{ displayName }}</div>
          <div class="profile-meta">账号：{{ userInfo?.username || '-' }}</div>
          <div class="profile-meta">状态：{{ ({ ACTIVE: '正常', SUSPENDED: '已停用', DEACTIVATED: '已注销' })[userInfo?.accountStatus || ''] || '-' }}</div>
          <div class="profile-meta">角色：{{ userInfo?.roles?.join(', ') || '-' }}</div>
        </div>
        <div class="right-panel">
          <div class="form-block">
            <div class="form-field">
              <label class="form-label" style="width: 80px; min-width: 80px;">个人简介</label>
              <div class="form-content"><textarea v-model="profileForm.bio" class="form-textarea" rows="3" maxlength="500" placeholder="请输入个人简介"></textarea></div>
            </div>
            <div class="form-field">
              <label class="form-label" style="width: 80px; min-width: 80px;">性别</label>
              <div class="form-content">
                <select v-model="profileForm.gender" class="form-select" style="max-width: 200px;">
                  <option value="UNKNOWN">未知</option>
                  <option value="MALE">男</option>
                  <option value="FEMALE">女</option>
                  <option value="OTHER">其他</option>
                </select>
              </div>
            </div>
            <div class="form-field">
              <label class="form-label" style="width: 80px; min-width: 80px;">生日</label>
              <div class="form-content"><input v-model="profileForm.birthday" type="date" class="form-input" style="max-width: 200px;" /></div>
            </div>
            <div class="form-field">
              <label class="form-label" style="width: 80px; min-width: 80px;">居住地</label>
              <div class="form-content"><input v-model="profileForm.residence" class="form-input" maxlength="50" placeholder="请输入居住地" /></div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <button class="btn" @click="profileDialogVisible = false">取消</button>
        <button class="btn btn-primary" :disabled="saving" @click="saveProfile"><i v-if="saving" class="fa-solid fa-spinner fa-spin"></i> 保存</button>
      </template>
    </BaseDialog>
  </div>
</template>

<style scoped>
.user-menu { position: relative; }
.user-btn { display: flex; align-items: center; gap: 8px; padding: 6px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg-white); cursor: pointer; transition: all 0.2s; font-family: inherit; }
.user-btn:hover { border-color: var(--primary); }
.user-avatar { width: 32px; height: 32px; border-radius: 50%; overflow: hidden; display: flex; align-items: center; justify-content: center; background: var(--primary-light); color: var(--primary); font-weight: 600; font-size: 14px; flex-shrink: 0; }
.user-avatar img { width: 100%; height: 100%; object-fit: cover; }
.user-name { font-size: 14px; color: var(--text-primary); max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.dropdown-menu { min-width: 160px; }
.dropdown-menu a { display: flex; align-items: center; gap: 8px; }
.dropdown-divider { height: 1px; background: var(--border); margin: 4px 0; }

.profile-grid { display: flex; gap: 24px; }
.left-panel { min-width: 160px; border-right: 1px solid var(--border); padding-right: 16px; display: flex; flex-direction: column; align-items: center; }
.profile-avatar { width: 64px; height: 64px; border-radius: 50%; overflow: hidden; display: flex; align-items: center; justify-content: center; background: var(--primary-light); color: var(--primary); font-weight: 600; font-size: 24px; margin-bottom: 12px; }
.profile-avatar img { width: 100%; height: 100%; object-fit: cover; }
.profile-name { font-weight: 600; font-size: 16px; margin-bottom: 8px; text-align: center; }
.profile-meta { color: var(--text-muted); font-size: 12px; margin-bottom: 4px; text-align: center; }
.right-panel { flex: 1; }
</style>
