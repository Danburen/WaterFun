<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listOnlineUsers, getOnlineCount, forceOffline } from '@/api/onlineUsers'
import type { OnlineUserVO, OnlineCountVO } from '@/api/onlineUsers'
import ListPage from '~/components/ListPage.vue'

const users = ref<OnlineUserVO[]>([])
const totalElements = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const keyword = ref('')
const userTypeFilter = ref('')
const onlineCount = ref<OnlineCountVO | null>(null)

const formatTime = (timeStr: string | null | undefined): string => {
  if (!timeStr) return '--'
  const now = Date.now()
  const t = new Date(timeStr).getTime()
  const diff = Math.floor((now - t) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  return new Date(timeStr).toLocaleString()
}

const getUserTypeLabel = (userType: string) => ({ 'COMMON': '普通用户', 'ADMIN': '管理员', 'VIP': 'VIP用户', 'MODERATOR': '审核员', 'BOT': '机器人' }[userType] || userType)
const getUserTypeClass = (userType: string) => ({ 'ADMIN': 'badge-red', 'VIP': 'badge-yellow', 'COMMON': 'badge-gray' }[userType] || 'badge-gray')

const fetchUsers = async () => {
  loading.value = true
  try {
    const params: any = { page: currentPage.value, size: pageSize.value }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (userTypeFilter.value) params.userType = userTypeFilter.value

    const [usersRes, countRes] = await Promise.all([listOnlineUsers(params), getOnlineCount()])
    users.value = usersRes.data?.content || []
    totalElements.value = usersRes.data?.totalElements ?? usersRes.data?.page?.totalElements ?? 0
    onlineCount.value = countRes.data ?? null
  } catch (e) { console.error('Fetch online users failed:', e) }
  finally { loading.value = false }
}

const search = () => { currentPage.value = 1; fetchUsers() }
const handleForceOffline = async (uid: number) => {
  if (!confirm('确定强制该用户下线？')) return
  try { await forceOffline(uid); fetchUsers() }
  catch (e) { console.error('Force offline failed:', e) }
}

onMounted(() => { fetchUsers() })
</script>

<template>
  <div>
    <div class="stat-bar">
      <div class="stat-pill">
        <div class="stat-pill-icon blue"><i class="fa-solid fa-users"></i></div>
        <div class="stat-pill-info">
          <div class="stat-pill-label">当前在线</div>
          <div class="stat-pill-value">{{ onlineCount?.onlineCount ?? totalElements }}</div>
        </div>
      </div>
      <div class="stat-pill">
        <div class="stat-pill-icon green"><i class="fa-solid fa-user-shield"></i></div>
        <div class="stat-pill-info">
          <div class="stat-pill-label">管理员在线</div>
          <div class="stat-pill-value">{{ onlineCount?.adminOnlineCount ?? '-' }}</div>
        </div>
      </div>
      <div class="stat-pill">
        <div class="stat-pill-icon orange"><i class="fa-solid fa-clock"></i></div>
        <div class="stat-pill-info">
          <div class="stat-pill-label">今日峰值</div>
          <div class="stat-pill-value">{{ onlineCount?.pealOnlineCount ?? '-' }}</div>
        </div>
      </div>
    </div>

    <div class="search-bar">
      <div class="search-input-wrap">
        <i class="fa-solid fa-search"></i>
        <input type="text" class="search-input" placeholder="搜索用户昵称、UID..." v-model="keyword" @keyup.enter="search" />
      </div>
      <select class="filter-select" v-model="userTypeFilter" @change="search">
        <option value="">全部用户类型</option>
        <option value="COMMON">普通用户</option>
        <option value="VIP">VIP用户</option>
        <option value="ADMIN">管理员</option>
        <option value="MODERATOR">审核员</option>
        <option value="BOT">机器人</option>
      </select>
      <button class="btn" @click="fetchUsers"><i class="fa-solid fa-sync-alt" :class="{ 'fa-spin': loading }"></i> 刷新</button>
    </div>

    <ListPage :loading="loading" :total="totalElements" v-model:page="currentPage" v-model:pageSize="pageSize" @change="fetchUsers">
      <table class="data-table">
        <thead>
          <tr>
            <th>用户信息</th>
            <th>UID</th>
            <th>等级</th>
            <th>用户类型</th>
            <th>最后活跃</th>
            <th>Session ID</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.uid">
            <td>
              <div class="table-user">
                <img :src="user.userBrief?.avatar?.url || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + user.uid" alt="avatar" class="table-user-avatar" />
                <div>
                  <div class="table-user-name">{{ user.userBrief?.displayName }}</div>
                  <div class="table-user-meta">IP: {{ user.ip || '--' }}</div>
                </div>
              </div>
            </td>
            <td>{{ user.uid }}</td>
            <td>LV.{{ user.userBrief?.level }}</td>
            <td><span :class="['badge', getUserTypeClass(user.userBrief?.userType || 'COMMON')]">{{ getUserTypeLabel(user.userBrief?.userType || 'COMMON') }}</span></td>
            <td>
              <span class="status-badge status-online">在线</span>
              <span class="time-meta">{{ formatTime(user.lastActive) }}</span>
            </td>
            <td class="session-id">{{ user.sessionId || '--' }}</td>
            <td>
              <div class="table-actions">
                <button class="action-icon-btn" title="查看详情"><i class="fa-regular fa-eye"></i></button>
                <button class="action-icon-btn danger" title="强制下线" @click="handleForceOffline(user.uid)"><i class="fa-solid fa-sign-out-alt"></i></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </ListPage>
  </div>
</template>

<style scoped>
.stat-bar { display: flex; gap: 20px; margin-bottom: 24px; }
.stat-pill { display: flex; align-items: center; gap: 10px; padding: 12px 20px; background: var(--bg-white); border: 1px solid var(--border); border-radius: var(--radius); flex: 1; }
.stat-pill-icon { width: 40px; height: 40px; border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; font-size: 16px; }
.stat-pill-icon.blue { background: var(--primary-light); color: var(--primary); }
.stat-pill-icon.green { background: #dcfce7; color: #15803d; }
.stat-pill-icon.orange { background: #fef3c7; color: #b45309; }
.stat-pill-info { flex: 1; }
.stat-pill-label { font-size: 12px; color: var(--text-muted); }
.stat-pill-value { font-size: 20px; font-weight: 700; color: var(--text-primary); }

.search-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.search-input-wrap { position: relative; }
.search-input { width: 280px; padding: 10px 14px 10px 38px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; color: var(--text-primary); background: var(--bg-white); outline: none; transition: all 0.2s ease; }
.search-input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }
.search-input-wrap i { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--text-muted); font-size: 14px; }
.filter-select { padding: 10px 14px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; color: var(--text-secondary); background: var(--bg-white); cursor: pointer; outline: none; }
.filter-select:focus { border-color: var(--primary); }

.data-table { width: 100%; border-collapse: collapse; }
.data-table th { text-align: left; padding: 14px 20px; font-size: 12px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid var(--border); background: var(--bg); white-space: nowrap; }
.data-table td { padding: 14px 20px; font-size: 14px; color: var(--text-primary); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
.data-table tr:hover td { background: var(--bg); }

.table-user { display: flex; align-items: center; gap: 12px; }
.table-user-avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; }
.table-user-name { font-weight: 600; color: var(--text-primary); }
.table-user-meta { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

.status-badge { display: inline-flex; align-items: center; gap: 5px; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; }
.status-online { background: #dcfce7; color: #15803d; }
.status-online::before { content: ''; width: 7px; height: 7px; background: #10b981; border-radius: 50%; animation: pulse 2s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

.time-meta { margin-left: 6px; font-size: 12px; color: var(--text-muted); }
.session-id { font-family: monospace; font-size: 12px; color: var(--text-muted); }

.action-icon-btn { width: 32px; height: 32px; border: none; background: transparent; color: var(--text-muted); border-radius: 6px; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 14px; transition: all 0.2s; }
.action-icon-btn:hover { background: var(--primary-light); color: var(--primary); }
.action-icon-btn.danger:hover { background: #fee2e2; color: #dc2626; }

@media (max-width: 1200px) { .stat-bar { flex-wrap: wrap; } .stat-pill { min-width: 200px; } }
</style>
