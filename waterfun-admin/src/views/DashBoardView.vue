<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getDashboardOverview,
  getTrend,
  getRecentActivities,
  getDashboardOnlineUsers,
  getLatestStatistic,
} from '@/api/dashboard'
import type { DashboardOverview, TrendPoint, DashboardRecentActivity, OnlineUserVO, SiteStatistic } from '@/api/dashboard'

const router = useRouter()

const overview = ref<DashboardOverview | null>(null)
const trendData = ref<TrendPoint[]>([])
const activities = ref<DashboardRecentActivity[]>([])
const onlineUsers = ref<OnlineUserVO[]>([])
const statistic = ref<SiteStatistic | null>(null)
const loading = ref(true)
const trendDays = ref(7)
const currentPage = ref(0)
const totalOnlineUsers = ref(0)

const goToUserDetail = (uid: number) => {
  router.push({ name: 'userDetail', params: { uid } })
}

const formatNumber = (n: number): string => {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

const getTimeAgo = (timeStr: string | number | null | undefined): string => {
  if (timeStr == null) return '--'
  const now = Date.now()
  const t = new Date(timeStr).getTime()
  const diff = Math.floor((now - t) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  return Math.floor(diff / 86400) + '天前'
}

const getActivityBadgeClass = (actionType: string) => {
  switch (actionType) {
    case 'CREATE': return 'badge-new-post'
    case 'REPORT': return 'badge-report'
    case 'INTERACTIVE': return 'badge-audit'
    default: return 'badge-new-user'
  }
}

const getActivityBadgeText = (actionType: string, businessType: string) => {
  if (actionType === 'CREATE' && businessType === 'POST') return '新帖'
  if (actionType === 'CREATE') return '创建'
  if (actionType === 'REPORT') return '举报'
  if (actionType === 'INTERACTIVE') return '互动'
  return '动态'
}

const getUserTypeLabel = (userType: string) => {
  const map: Record<string, string> = {
    'COMMON': '普通用户',
    'ADMIN': '管理员',
    'VIP': 'VIP用户',
    'MODERATOR': '审核员',
    'BOT': '机器人',
  }
  return map[userType] || userType
}

const getUserTypeClass = (userType: string) => {
  const map: Record<string, string> = {
    'ADMIN': 'type-admin',
    'VIP': 'type-vip',
    'COMMON': 'type-common',
  }
  return map[userType] || 'type-common'
}

const maxTrendValue = ref(0)

const fetchAllData = async () => {
  loading.value = true
  try {
    const [overviewRes, trendRes, activitiesRes, usersRes, statRes] = await Promise.all([
      getDashboardOverview(),
      getTrend(trendDays.value),
      getRecentActivities(10),
      getDashboardOnlineUsers(0, 10),
      getLatestStatistic(),
    ])
    overview.value = overviewRes.data
    trendData.value = trendRes.data || []
    activities.value = activitiesRes.data || []
    onlineUsers.value = usersRes.data?.content || []
    totalOnlineUsers.value = usersRes.data?.totalElements ?? usersRes.data?.page?.totalElements ?? 0
    statistic.value = statRes.data

    if (trendData.value.length > 0) {
      maxTrendValue.value = Math.max(...trendData.value.map(d => d.dailyPv), 1)
    }
  } catch (e) {
    console.error('Dashboard data fetch failed:', e)
  } finally {
    loading.value = false
  }
}

const switchTrendDays = (days: number) => {
  trendDays.value = days
  getTrend(days).then(res => {
    trendData.value = res.data || []
    if (trendData.value.length > 0) {
      maxTrendValue.value = Math.max(...trendData.value.map(d => d.dailyPv), 1)
    }
  })
}

onMounted(() => {
  fetchAllData()
})
</script>

<template>
  <div v-if="loading" class="loading-state">
    <i class="fa-solid fa-spinner fa-spin"></i> 加载中...
  </div>
  <template v-else>
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">在线用户</span>
          <div class="stat-icon blue"><i class="fa-solid fa-users"></i></div>
        </div>
        <div class="stat-value">{{ formatNumber(overview?.onlineUserCount || 0) }}</div>
        <div class="stat-change up">
          <i class="fa-solid fa-arrow-up"></i> 实时
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">总用户数</span>
          <div class="stat-icon green"><i class="fa-solid fa-user-plus"></i></div>
        </div>
        <div class="stat-value">{{ formatNumber(overview?.totalUsers || 0) }}</div>
        <div class="stat-change up">
          <i class="fa-solid fa-arrow-up"></i> {{ overview?.todayNewUsers || 0 }} 今日新增
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">总帖子数</span>
          <div class="stat-icon orange"><i class="fa-solid fa-file-alt"></i></div>
        </div>
        <div class="stat-value">{{ formatNumber(overview?.totalPosts || 0) }}</div>
        <div class="stat-change up">
          <i class="fa-solid fa-arrow-up"></i> {{ overview?.todayNewPosts || 0 }} 今日新增
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">待审核</span>
          <div class="stat-icon red"><i class="fa-solid fa-shield-alt"></i></div>
        </div>
        <div class="stat-value">{{ overview?.pendingModerations || 0 }}</div>
        <div class="stat-change down">
          <i class="fa-solid fa-arrow-down"></i> 待处理
        </div>
      </div>
    </div>

    <div class="section-grid">
      <div class="card">
        <div class="card-header">
          <div class="card-title"><i class="fa-solid fa-chart-bar"></i> 访问趋势</div>
          <div class="card-actions">
            <button :class="['card-action-btn', { active: trendDays === 7 }]" @click="switchTrendDays(7)">7天</button>
            <button :class="['card-action-btn', { active: trendDays === 30 }]" @click="switchTrendDays(30)">30天</button>
            <button :class="['card-action-btn', { active: trendDays === 90 }]" @click="switchTrendDays(90)">90天</button>
          </div>
        </div>
        <div class="card-body">
          <div v-if="trendData.length === 0" class="empty-state">暂无趋势数据</div>
          <div v-else class="chart-placeholder">
            <div
              v-for="(point, idx) in trendData"
              :key="idx"
              class="chart-bar"
              :style="{ height: (point.dailyPv / maxTrendValue * 100) + '%' }"
            >
              <span class="chart-bar-value">{{ point.dailyPv >= 1000 ? (point.dailyPv / 1000).toFixed(1) + 'k' : point.dailyPv }}</span>
              <span class="chart-bar-label">{{ point.date.slice(5) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-header">
          <div class="card-title"><i class="fa-solid fa-bolt"></i> 今日概览</div>
        </div>
        <div class="card-body">
          <div class="quick-list">
            <div class="quick-item">
              <div class="quick-icon" style="background: #dbeafe; color: #1d4ed8;"><i class="fa-solid fa-eye"></i></div>
              <div class="quick-info">
                <div class="quick-label">今日 PV</div>
                <div class="quick-value">{{ formatNumber(overview?.todayPv || statistic?.dailyPv || 0) }}</div>
              </div>
            </div>
            <div class="quick-item">
              <div class="quick-icon" style="background: #dcfce7; color: #15803d;"><i class="fa-solid fa-user-plus"></i></div>
              <div class="quick-info">
                <div class="quick-label">新增用户</div>
                <div class="quick-value">{{ formatNumber(overview?.todayNewUsers || statistic?.newUsers || 0) }}</div>
              </div>
            </div>
            <div class="quick-item">
              <div class="quick-icon" style="background: #fef3c7; color: #b45309;"><i class="fa-solid fa-file-alt"></i></div>
              <div class="quick-info">
                <div class="quick-label">新增帖子</div>
                <div class="quick-value">{{ formatNumber(overview?.todayNewPosts || statistic?.newPosts || 0) }}</div>
              </div>
            </div>
            <div class="quick-item">
              <div class="quick-icon" style="background: #fce7f3; color: #be185d;"><i class="fa-solid fa-fire"></i></div>
              <div class="quick-info">
                <div class="quick-label">峰值在线</div>
                <div class="quick-value">{{ formatNumber(statistic?.peakOnline || 0) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-bottom: 24px;">
      <div class="card-header">
        <div class="card-title"><i class="fa-solid fa-clock"></i> 最近动态</div>
      </div>
      <div v-if="activities.length === 0" class="empty-state" style="padding: 40px;">暂无动态</div>
      <div v-else class="activity-list">
        <div v-for="(act, idx) in activities" :key="idx" class="activity-item">
          <img
            :src="act.userBrief?.avatar?.url || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + act.userBrief?.uid"
            alt="avatar"
            class="activity-avatar"
          >
          <div class="activity-content">
            <div class="activity-text">
              <strong>{{ act.userBrief?.displayName || '未知用户' }}</strong>
              {{ act.description }}
            </div>
            <div class="activity-time">{{ getTimeAgo(act.actionTime) }}</div>
          </div>
          <span :class="['activity-badge', getActivityBadgeClass(act.actionType)]">
            {{ getActivityBadgeText(act.actionType, act.businessType) }}
          </span>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div class="card-title"><i class="fa-solid fa-user-clock"></i> 最近活跃用户</div>
      </div>
      <div v-if="onlineUsers.length === 0" class="empty-state" style="padding: 40px;">暂无数据</div>
      <div v-else style="overflow-x: auto;">
        <table class="data-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>UID</th>
              <th>等级</th>
              <th>类型</th>
              <th>最后活跃</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in onlineUsers" :key="user.uid">
              <td>
                <div class="table-user">
                  <img
                    :src="user.userBrief?.avatar?.url || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + user.uid"
                    alt="avatar"
                    class="table-user-avatar"
                  >
                  <div>
                    <div class="table-user-name">{{ user.userBrief?.displayName }}</div>
                    <div class="table-user-id">UID: {{ user.uid }}</div>
                  </div>
                </div>
              </td>
              <td>{{ user.uid }}</td>
              <td>LV.{{ user.userBrief?.level }}</td>
              <td><span :class="['user-type-badge', getUserTypeClass(user.userBrief?.userType || 'COMMON')]">{{ getUserTypeLabel(user.userBrief?.userType || 'COMMON') }}</span></td>
              <td>{{ getTimeAgo(user.lastActive) }}</td>
              <td><span class="table-action" @click="goToUserDetail(user.uid)">查看详情</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </template>
</template>

<style scoped>
.loading-state {
  text-align: center;
  padding: 60px;
  color: var(--text-muted);
  font-size: 16px;
}

.empty-state {
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 20px;
  transition: all 0.2s ease;
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
}

.stat-icon.blue { background: var(--primary-light); color: var(--primary); }
.stat-icon.green { background: #dcfce7; color: #15803d; }
.stat-icon.orange { background: #fef3c7; color: #b45309; }
.stat-icon.red { background: #fee2e2; color: #dc2626; }

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.stat-change {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-change.up { color: var(--success); }
.stat-change.down { color: var(--danger); }

.card-action-btn {
  padding: 4px 12px;
  border: 1px solid var(--border);
  background: var(--bg-white);
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.card-action-btn:hover, .card-action-btn.active {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-light);
}

.chart-placeholder {
  height: 280px;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 8px;
  padding: 20px 0;
}

.chart-bar {
  flex: 1;
  background: linear-gradient(to top, var(--primary), #93c5fd);
  border-radius: 4px 4px 0 0;
  min-width: 24px;
  position: relative;
  transition: all 0.3s ease;
}

.chart-bar:hover { opacity: 0.8; }

.chart-bar-label {
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
}

.chart-bar-value {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  font-weight: 600;
  color: var(--primary);
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg);
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.quick-item:hover { background: var(--primary-light); }

.quick-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.quick-info { flex: 1; }
.quick-label { font-size: 13px; color: var(--text-secondary); }
.quick-value { font-size: 18px; font-weight: 700; color: var(--text-primary); }

.activity-list { display: flex; flex-direction: column; }

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-light);
  transition: background 0.2s;
}

.activity-item:last-child { border-bottom: none; }
.activity-item:hover { background: var(--bg); }

.activity-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.activity-content { flex: 1; }
.activity-text { font-size: 14px; color: var(--text-primary); line-height: 1.5; }
.activity-text strong { font-weight: 600; }
.activity-time { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

.activity-badge {
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  flex-shrink: 0;
}

.badge-new-post { background: #dbeafe; color: #1d4ed8; }
.badge-new-user { background: #dcfce7; color: #15803d; }
.badge-report { background: #fee2e2; color: #dc2626; }
.badge-audit { background: #fef3c7; color: #b45309; }

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  text-align: left;
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--border);
  background: var(--bg);
}

.data-table td {
  padding: 14px 16px;
  font-size: 14px;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-light);
}

.data-table tr:hover td { background: var(--bg); }

.table-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.table-user-name { font-weight: 600; }
.table-user-id { font-size: 12px; color: var(--text-muted); }

.user-type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.type-admin { background: #fee2e2; color: #dc2626; }
.type-vip { background: #fef3c7; color: #b45309; }
.type-common { background: #f1f5f9; color: var(--text-muted); }

.table-action {
  color: var(--primary);
  font-size: 13px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.table-action:hover { opacity: 0.7; }

@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .stat-grid { grid-template-columns: 1fr; }
  .content-area { padding: 16px; }
}
</style>
