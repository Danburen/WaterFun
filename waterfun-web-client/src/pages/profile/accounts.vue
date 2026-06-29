<script setup lang="ts">
import { useAccountPoolStore } from '~/stores/accountPoolStore'
import { useAuth } from '~/composables/useAuth'
import { useUserInfoStore } from '~/stores/userInfoStore'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const accountPoolStore = useAccountPoolStore()
const { switchAccount, logout } = useAuth()
const userInfoStore = useUserInfoStore()
const router = useRouter()

const handleSwitch = async (uid: string) => {
  await switchAccount(uid)
}

const handleRemove = async (uid: string) => {
  const isCurrent = uid === accountPoolStore.activeUid
  const acc = accountPoolStore.accounts.find(a => a.uid === uid)
  try {
    await ElMessageBox.confirm(
      `确定要移除账号「${acc?.nickname || acc?.username || uid}」吗？` +
      (isCurrent ? '当前账号将被登出。' : ''),
      '移除账号',
      { confirmButtonText: '移除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  if (isCurrent) {
    await logout()
    router.push('/login')
  } else {
    accountPoolStore.removeAccount(uid)
    ElMessage.success('已移除账号')
  }
}

const handleAddAccount = () => {
  router.push('/login')
}

const formatExpire = (expire: number): string => {
  const remaining = expire - Date.now()
  if (remaining <= 0) return '已过期'
  const hours = Math.floor(remaining / 3600000)
  const mins = Math.floor((remaining % 3600000) / 60000)
  return hours > 0 ? `${hours}小时${mins}分` : `${mins}分`
}
</script>

<template>
  <div class="accounts-container">
    <div class="g-form">
      <div class="g-form-section">
        <div class="g-section-title">已登录账号</div>
        <div class="accounts-list">
          <div
            v-for="acc in accountPoolStore.accounts"
            :key="acc.uid"
            class="account-card"
            :class="{ active: acc.uid === accountPoolStore.activeUid }"
          >
            <img
              :src="acc.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
              class="account-avatar"
              alt="avatar"
            />
            <div class="account-body">
              <div class="account-name">{{ acc.nickname || acc.username }}</div>
              <div class="account-uid">UID: {{ acc.uid }}</div>
              <div class="account-expire">
                <span :class="accountPoolStore.isTokenValid(acc.uid) ? 'status-valid' : 'status-expired'">
                  {{ accountPoolStore.isTokenValid(acc.uid) ? '有效' : '已过期' }}
                </span>
                <span v-if="accountPoolStore.isTokenValid(acc.uid)" class="expire-hint">
                  ({{ formatExpire(acc.expire) }}后过期)
                </span>
              </div>
            </div>
            <div class="account-actions">
              <button
                v-if="acc.uid !== accountPoolStore.activeUid"
                class="action-btn switch-btn"
                @click="handleSwitch(acc.uid)"
              >
                切换
              </button>
              <span v-else class="current-badge">当前</span>
              <button
                class="action-btn remove-btn"
                @click="handleRemove(acc.uid)"
              >
                移除
              </button>
            </div>
          </div>
        </div>
        <div v-if="!accountPoolStore.accounts.length" class="empty-hint">
          暂无已登录的账号
        </div>
      </div>

      <div class="g-form-section">
        <div class="g-section-title">添加账号</div>
        <div class="add-account-area">
          <p class="add-hint">切换账号不会影响当前账号的登录状态，您可以随时切换回来。</p>
          <button class="add-btn" @click="handleAddAccount">登录新账号</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.accounts-container {
  max-width: 800px;
  margin: 0 auto;
}

.accounts-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.account-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  transition: all 0.2s;
}
.account-card.active {
  border-color: #409eff;
  background: #f0f7ff;
}

.account-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.account-body {
  flex: 1;
  min-width: 0;
}
.account-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 2px;
}
.account-uid {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.account-expire {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.status-valid {
  color: #67c23a;
}
.status-expired {
  color: #f56c6c;
}
.expire-hint {
  color: #909399;
}

.account-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}
.action-btn {
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  transition: all 0.2s;
  white-space: nowrap;
}
.action-btn:hover {
  border-color: #409eff;
  color: #409eff;
}
.switch-btn {
  border-color: #409eff;
  color: #409eff;
}
.switch-btn:hover {
  background: #409eff;
  color: #fff;
}
.remove-btn:hover {
  border-color: #f56c6c;
  color: #f56c6c;
}
.current-badge {
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 10px;
  border-radius: 4px;
  text-align: center;
}

.empty-hint {
  text-align: center;
  padding: 40px 0;
  color: #909399;
  font-size: 14px;
}

.add-account-area {
  padding: 10px 0;
}
.add-hint {
  font-size: 13px;
  color: #909399;
  margin: 0 0 16px 0;
}
.add-btn {
  padding: 10px 24px;
  border: 1px dashed #409eff;
  border-radius: 8px;
  background: transparent;
  color: #409eff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.add-btn:hover {
  background: #ecf5ff;
  border-style: solid;
}
</style>
