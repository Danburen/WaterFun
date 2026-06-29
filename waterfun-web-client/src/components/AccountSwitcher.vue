<script setup lang="ts">
import { useAccountPoolStore } from '~/stores/accountPoolStore'
import { useAuth } from '~/composables/useAuth'
import { useRouter } from 'vue-router'

const accountPoolStore = useAccountPoolStore()
const { switchAccount } = useAuth()
const router = useRouter()

const switchToAccount = async (uid: string) => {
  switchAccount(uid)
}

const addAccount = () => {
  router.push('/login')
}
</script>

<template>
  <div class="account-switcher">
    <div class="switcher-header">
      <span>切换账号</span>
    </div>
    <div class="account-list">
      <div
        v-for="acc in accountPoolStore.accounts"
        :key="acc.uid"
        class="account-item"
        :class="{ active: acc.uid === accountPoolStore.activeUid }"
        @click="acc.uid !== accountPoolStore.activeUid && switchToAccount(acc.uid)"
      >
        <img
          :src="acc.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
          class="account-avatar"
          alt="avatar"
        />
        <div class="account-info">
          <span class="account-nickname">{{ acc.nickname || acc.username }}</span>
          <span v-if="acc.uid === accountPoolStore.activeUid" class="account-badge">当前</span>
        </div>
      </div>
    </div>
    <div class="switcher-footer">
      <button class="add-account-btn" @click="addAccount">
        <span>+</span> 添加账号
      </button>
    </div>
  </div>
</template>

<style scoped>
.account-switcher {
  min-width: 220px;
  padding: 4px 0;
}
.switcher-header {
  padding: 8px 16px;
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.account-list {
  max-height: 240px;
  overflow-y: auto;
}
.account-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.15s;
}
.account-item:hover {
  background: #f1f5f9;
}
.account-item.active {
  cursor: default;
  background: #f8fafc;
}
.account-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.account-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}
.account-nickname {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.account-badge {
  font-size: 11px;
  color: #3b82f6;
  background: #eff6ff;
  padding: 1px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}
.switcher-footer {
  border-top: 1px solid #e2e8f0;
  padding: 6px 16px;
}
.add-account-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: transparent;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}
.add-account-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background: #eff6ff;
}
</style>
