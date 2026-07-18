<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTicketStore } from '~/stores/ticketStore'
import { storeToRefs } from 'pinia'
import type { TicketType, TicketStatus, CreateUserReportReq, ReportType } from '~/api/ticketApi'

const router = useRouter()
const ticketStore = useTicketStore()
const { tickets, ticketStats, pagination, loading, currentTicket, detailLoading } = storeToRefs(ticketStore)

const activeFilterType = ref<TicketType | ''>('')
const activeFilterStatus = ref<TicketStatus | ''>('')
const activeMenuType = ref<string>('all')
const showCreateDialog = ref(false)
const showDetailDrawer = ref(false)
const reasonExpanded = ref(false)

const createForm = reactive<CreateUserReportReq & { replyContent?: string }>({
  ticketType: 'FEATURE_FEEDBACK',
  type: 'VIOLATION_OF_GUIDELINES',
  reason: '',
  targetId: '',
  targetType: 'DEFAULT',
  resourceUuids: [],
})

const ticketTypeOptions: { value: TicketType; label: string; icon: string; desc: string }[] = [
  { value: 'CONTENT_REPORT', label: '内容举报', icon: 'fas fa-flag', desc: '举报违规内容' },
  { value: 'ACCOUNT_APPEAL', label: '账号申诉', icon: 'fas fa-user-shield', desc: '账号相关问题申诉' },
  { value: 'FEATURE_FEEDBACK', label: '功能反馈', icon: 'fas fa-lightbulb', desc: '反馈产品功能问题' },
  { value: 'SUGGESTION', label: '意见建议', icon: 'fas fa-comment-dots', desc: '提出你的建议' },
]

const ticketStatusOptions: { value: TicketStatus; label: string; cls: string }[] = [
  { value: 'PENDING', label: '待处理', cls: 'status-pending' },
  { value: 'RESOLVED', label: '已解决', cls: 'status-resolved' },
  { value: 'REJECTED', label: '已驳回', cls: 'status-rejected' },
  { value: 'CANCELLED', label: '已取消', cls: 'status-cancelled' },
]

const reportTypeOptions: { value: ReportType; label: string }[] = [
  { value: 'VIOLATION_OF_GUIDELINES', label: '违反社区准则' },
  { value: 'INAPPROPRIATE_CONTENT', label: '内容不当' },
  { value: 'ADVERTISEMENT', label: '垃圾广告' },
  { value: 'VIOLENCE', label: '暴力内容' },
  { value: 'SENSITIVE', label: '敏感内容' },
  { value: 'CASCADE', label: '刷屏引战' },
  { value: 'OTHER', label: '其他' },
]

const menuItems = computed(() => {
  const s = ticketStats.value
  const allCount = s ? s.reportCount + s.appealCount + s.feedbackCount + s.suggestionCount : 0
  return [
    { key: 'all', label: '全部工单', icon: 'fas fa-list', badge: allCount },
    { key: 'CONTENT_REPORT', label: '我的举报', icon: 'fas fa-flag', badge: s?.reportCount || 0 },
    { key: 'ACCOUNT_APPEAL', label: '我的申诉', icon: 'fas fa-gavel', badge: s?.appealCount || 0 },
    { key: 'SUGGESTION', label: '我的建议', icon: 'fas fa-lightbulb', badge: s?.suggestionCount || 0 },
    { key: 'FEATURE_FEEDBACK', label: '我的反馈', icon: 'fas fa-comment-dots', badge: s?.feedbackCount || 0 },
  ]
})

const helpItems = [
  { key: 'rules', label: '社区规范', icon: 'fas fa-book' },
  { key: 'security', label: '安全中心', icon: 'fas fa-shield-alt' },
  { key: 'faq', label: '常见问题', icon: 'fas fa-question-circle' },
  { key: 'contact', label: '联系管理员', icon: 'fas fa-envelope' },
]

const statsItems = computed(() => {
  if (!ticketStats.value) return []
  return [
    { label: '内容举报', count: ticketStats.value.reportCount, icon: 'fas fa-flag', cls: 'stat-red' },
    { label: '账号申诉', count: ticketStats.value.appealCount, icon: 'fas fa-user-shield', cls: 'stat-orange' },
    { label: '功能反馈', count: ticketStats.value.feedbackCount, icon: 'fas fa-lightbulb', cls: 'stat-blue' },
    { label: '意见建议', count: ticketStats.value.suggestionCount, icon: 'fas fa-comment-dots', cls: 'stat-green' },
  ]
})

const handleMenuClick = (key: string) => {
  activeMenuType.value = key
  if (key === 'all') {
    activeFilterType.value = ''
  } else {
    activeFilterType.value = key as TicketType
  }
  ticketStore.setFilters({ ticketType: activeFilterType.value || undefined } as any)
  loadTickets(1)
}

const handleFilterTypeChange = (type: TicketType | '') => {
  activeFilterType.value = type
  activeMenuType.value = type || 'all'
  ticketStore.setFilters({ ticketType: type || undefined } as any)
  loadTickets(1)
}

const handleFilterStatusChange = (status: TicketStatus | '') => {
  activeFilterStatus.value = status
  ticketStore.setFilters({ status: status || undefined } as any)
  loadTickets(1)
}

const loadTickets = async (page: number = 1) => {
  await ticketStore.fetchTickets(page)
}

const handlePageChange = (page: number) => {
  loadTickets(page)
}

const openCreateDialog = () => {
  createForm.ticketType = 'FEATURE_FEEDBACK'
  createForm.type = 'VIOLATION_OF_GUIDELINES'
  createForm.reason = ''
  createForm.targetId = ''
  createForm.targetType = 'DEFAULT'
  createForm.resourceUuids = []
  reasonExpanded.value = false
  showCreateDialog.value = true
}

const isContentReportOther = computed(() =>
  createForm.ticketType === 'CONTENT_REPORT' && createForm.type === 'OTHER'
)
const isReasonRequired = computed(() =>
  createForm.ticketType !== 'CONTENT_REPORT' || isContentReportOther.value
)

const submitTicket = async () => {
  // CONTENT_REPORT: reason required only when type is OTHER
  // Other ticket types: reason always required
  if (isReasonRequired.value && !(createForm.reason || '').trim()) {
    ElMessage.warning(isContentReportOther.value ? '请填写其他问题的详细描述' : '请填写问题描述')
    return
  }
  if (createForm.ticketType === 'CONTENT_REPORT' && !createForm.targetId?.trim()) {
    ElMessage.warning('举报时必须指定目标ID（帖子ID/评论ID/用户UID）')
    return
  }
  try {
    const payload: CreateUserReportReq = {
      ticketType: createForm.ticketType,
    }
    if (createForm.ticketType === 'CONTENT_REPORT') {
      payload.type = createForm.type
      payload.targetId = createForm.targetId
      payload.targetType = createForm.targetType
      if (createForm.resourceUuids?.length) {
        payload.resourceUuids = createForm.resourceUuids
      }
    }
    // Only send reason when provided; CONTENT_REPORT (non-OTHER) allows empty reason
    if ((createForm.reason || '').trim()) {
      payload.reason = (createForm.reason || '').trim()
    }
    await ticketStore.createTicket(payload)
    ElMessage.success('工单提交成功，请耐心等待处理')
    showCreateDialog.value = false
    loadTickets(1)
    ticketStore.fetchStats()
  } catch (err) {
    console.error('提交工单失败:', err)
  }
}

const openDetail = async (id: string) => {
  await ticketStore.fetchTicketDetail(id)
  showDetailDrawer.value = true
}

const handleCancelTicket = async (id: string) => {
  try {
    await ElMessageBox.confirm('确定要取消这个工单吗？', '确认取消', {
      confirmButtonText: '确定取消',
      cancelButtonText: '返回',
      type: 'warning',
    })
    await ticketStore.cancelTicket(id)
    ElMessage.success('工单已取消')
    loadTickets(pagination.value.number + 1)
  } catch { /* cancelled */ }
}

const formatDateTime = (dateStr: string | null) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const getTicketTypeLabel = (type: TicketType) => {
  return ticketTypeOptions.find(o => o.value === type)?.label || type
}

const getStatusInfo = (status: TicketStatus) => {
  return ticketStatusOptions.find(o => o.value === status) || { label: status, cls: '' }
}

const formatCount = (n: number) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

onMounted(async () => {
  await Promise.all([loadTickets(), ticketStore.fetchStats()])
})
</script>

<template>
  <div class="cs-page">
    <HeaderNavMenu />

    <main class="main">
      <aside class="sidebar">
        <div class="sidebar-card">
          <div class="sidebar-header">
            <button class="back-btn" @click="router.back()">
              <i class="fas fa-arrow-left"></i>
            </button>
            <i class="fas fa-headset"></i> 客服中心
          </div>
          <div class="sidebar-menu">
            <a
              v-for="item in menuItems"
              :key="item.key"
              :class="['menu-item', { active: activeMenuType === item.key }]"
              @click.prevent="handleMenuClick(item.key)"
            >
              <i :class="item.icon"></i>
              <span>{{ item.label }}</span>
              <span v-if="item.badge" class="menu-badge">{{ item.badge }}</span>
            </a>
          </div>
          <button class="new-ticket-btn" @click="openCreateDialog">
            <i class="fas fa-plus"></i> 提交新工单
          </button>
        </div>

        <div class="sidebar-card">
          <div class="sidebar-header">
            <i class="fas fa-info-circle"></i> 帮助链接
          </div>
          <div class="sidebar-menu">
            <a
              v-for="item in helpItems"
              :key="item.key"
              href="#"
              class="menu-item"
            >
              <i :class="item.icon"></i>
              <span>{{ item.label }}</span>
            </a>
          </div>
        </div>
      </aside>

      <div class="content-area">
        <div class="page-header">
          <div>
            <h1 class="page-title">
              {{ activeMenuType === 'all' ? '全部工单' : (ticketTypeOptions.find(o => o.value === activeMenuType)?.label || '工单') }}
            </h1>
            <p class="page-subtitle">查看和管理你提交的所有工单</p>
          </div>
        </div>

        <div class="stats-row">
          <div
            v-for="(item, idx) in statsItems"
            :key="idx"
            :class="['stat-card', item.cls]"
            @click="ticketTypeOptions[idx] && handleFilterTypeChange(ticketTypeOptions[idx].value)"
          >
            <div class="stat-icon"><i :class="item.icon"></i></div>
            <div class="stat-value">{{ formatCount(item.count) }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>

        <div class="filter-bar">
          <div class="filter-tabs">
            <button
              :class="['filter-tab', { active: activeFilterStatus === '' }]"
              @click="handleFilterStatusChange('')"
            >全部</button>
            <button
              v-for="opt in ticketStatusOptions"
              :key="opt.value"
              :class="['filter-tab', { active: activeFilterStatus === opt.value }]"
              @click="handleFilterStatusChange(opt.value)"
            >{{ opt.label }}</button>
          </div>
          <div class="filter-right">
            <select class="filter-select">
              <option>按时间排序</option>
              <option>按类型排序</option>
              <option>按状态排序</option>
            </select>
            <input type="text" class="search-input" placeholder="搜索工单...">
          </div>
        </div>

        <div v-if="loading && !tickets.length" class="empty-state">
          <div class="empty-icon"><i class="fas fa-spinner fa-pulse"></i></div>
          <div class="empty-title">加载中...</div>
        </div>

        <div v-else-if="!tickets.length" class="empty-state">
          <div class="empty-icon"><i class="fas fa-ticket-alt"></i></div>
          <div class="empty-title">暂无工单</div>
          <div class="empty-desc">提交新工单，我们会在第一时间为你处理</div>
        </div>

        <div v-else class="ticket-list">
          <div
            v-for="ticket in tickets"
            :key="ticket.ticketId"
            class="ticket-card"
            @click="openDetail(ticket.ticketId)"
          >
            <div class="ticket-header">
              <div class="ticket-badges">
                <span :class="['ticket-type-badge', `badge-${ticket.ticketType === 'CONTENT_REPORT' ? 'report' : ticket.ticketType === 'ACCOUNT_APPEAL' ? 'appeal' : ticket.ticketType === 'SUGGESTION' ? 'suggest' : 'feedback'}`]">
                  <i :class="ticket.ticketType === 'CONTENT_REPORT' ? 'fas fa-flag' : ticket.ticketType === 'ACCOUNT_APPEAL' ? 'fas fa-gavel' : ticket.ticketType === 'SUGGESTION' ? 'fas fa-lightbulb' : 'fas fa-comment-dots'"></i>
                  {{ getTicketTypeLabel(ticket.ticketType) }}
                </span>
                <span :class="['ticket-status', getStatusInfo(ticket.status).cls]">
                  <i :class="ticket.status === 'PENDING' ? 'fas fa-clock' : ticket.status === 'RESOLVED' ? 'fas fa-check-circle' : ticket.status === 'REJECTED' ? 'fas fa-times-circle' : 'fas fa-clock'"></i>
                  {{ getStatusInfo(ticket.status).label }}
                </span>
              </div>
            </div>
            <div class="ticket-title">{{ ticket.content || '无描述' }}</div>
            <div class="ticket-desc">{{ ticket.content || '暂无描述内容' }}</div>
            <div class="ticket-footer">
              <div class="ticket-meta">
                <span><i class="far fa-calendar"></i> {{ formatDateTime(ticket.createdAt) }}</span>
                <span v-if="ticket.targetId"><i class="fas fa-bullseye"></i> 目标: {{ ticket.targetId }}</span>
                <span v-if="ticket.evidenceCount > 0"><i class="far fa-file-alt"></i> 附 {{ ticket.evidenceCount }} 个附件</span>
              </div>
              <div class="ticket-actions">
                <button class="ticket-btn" @click.stop="openDetail(ticket.ticketId)"><i class="fas fa-eye"></i> 查看</button>
                <button
                  v-if="ticket.status === 'PENDING'"
                  class="ticket-btn danger"
                  @click.stop="handleCancelTicket(ticket.ticketId)"
                ><i class="fas fa-times"></i> 取消</button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="pagination.totalPages > 1" class="pagination-wrap">
          <button
            :class="['page-btn', { disabled: pagination.number === 0 }]"
            :disabled="pagination.number === 0"
            @click="handlePageChange(pagination.number)"
          >
            <i class="fas fa-chevron-left"></i>
          </button>
          <button
            v-for="p in pagination.totalPages"
            :key="p"
            :class="['page-btn', { active: p === pagination.number + 1 }]"
            @click="handlePageChange(p)"
            v-show="Math.abs(p - (pagination.number + 1)) <= 2 || p === 1 || p === pagination.totalPages"
          >{{ p }}</button>
          <button
            :class="['page-btn', { disabled: pagination.number + 1 >= pagination.totalPages }]"
            :disabled="pagination.number + 1 >= pagination.totalPages"
            @click="handlePageChange(pagination.number + 2)"
          >
            <i class="fas fa-chevron-right"></i>
          </button>
        </div>
      </div>
    </main>

    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-links">
          <router-link to="/about">关于我们</router-link>
          <a href="#">帮助中心</a>
          <router-link to="/EulaView">用户协议</router-link>
          <router-link to="/legal/terms">服务条款</router-link>
          <router-link to="/legal/privacy">隐私政策</router-link>
          <a href="#">联系客服</a>
        </div>
        <div class="footer-copyright">&copy; {{ new Date().getFullYear() }} WaterFun. All rights reserved.</div>
      </div>
    </footer>

    <div v-if="showCreateDialog" class="modal-overlay" @click.self="showCreateDialog = false">
      <div class="modal">
        <div class="modal-header">
          <div class="modal-title"><i class="fas fa-plus-circle" style="color: var(--primary);"></i> 提交新工单</div>
          <button class="modal-close" @click="showCreateDialog = false"><i class="fas fa-times"></i></button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">工单类型 <span class="required">*</span></label>
            <div class="type-select">
              <div
                v-for="opt in ticketTypeOptions"
                :key="opt.value"
                :class="['type-option', { active: createForm.ticketType === opt.value }]"
                @click="createForm.ticketType = opt.value"
              >
                <i :class="opt.icon"></i>
                <span>{{ opt.label }}</span>
              </div>
            </div>
          </div>

          <div v-if="createForm.ticketType === 'CONTENT_REPORT'" class="form-group">
            <label class="form-label">举报类型 <span class="required">*</span></label>
            <select v-model="createForm.type" class="form-input">
              <option v-for="opt in reportTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>

          <div v-if="createForm.ticketType === 'CONTENT_REPORT'" class="form-group">
            <label class="form-label">目标类型 <span class="required">*</span></label>
            <div class="type-select">
              <div
                :class="['type-option', { active: createForm.targetType === 'POST' }]"
                @click="createForm.targetType = 'POST'"
              >
                <i class="fas fa-file-alt"></i>
                <span>帖子</span>
              </div>
              <div
                :class="['type-option', { active: createForm.targetType === 'COMMENT' }]"
                @click="createForm.targetType = 'COMMENT'"
              >
                <i class="fas fa-comment"></i>
                <span>评论</span>
              </div>
              <div
                :class="['type-option', { active: createForm.targetType === 'USER' }]"
                @click="createForm.targetType = 'USER'"
              >
                <i class="fas fa-user"></i>
                <span>用户</span>
              </div>
            </div>
          </div>

          <div class="form-group">
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px;">
              <label class="form-label" style="margin-bottom:0;">问题描述 <span v-if="isReasonRequired" class="required">*</span></label>
              <button type="button" @click="reasonExpanded = !reasonExpanded" style="background:none;border:1px solid var(--border-color,#e2e8f0);border-radius:6px;padding:2px 10px;font-size:12px;color:var(--text-muted,#94a3b8);cursor:pointer;display:inline-flex;align-items:center;gap:4px;">
                <i :class="reasonExpanded ? 'fas fa-chevron-up' : 'fas fa-chevron-down'" style="font-size:10px;"></i>
                {{ reasonExpanded ? '收起' : '展开' }}
              </button>
            </div>
            <div v-show="reasonExpanded">
              <textarea
                v-model="createForm.reason"
                class="form-input"
                rows="5"
                placeholder="请详细描述你遇到的问题或建议..."
                maxlength="2000"
              ></textarea>
              <div class="form-hint">{{ (createForm.reason || '').length }}/2000</div>
            </div>
          </div>

          <div v-if="createForm.ticketType === 'CONTENT_REPORT'" class="form-group">
            <label class="form-label">
              目标ID
              <span class="required">*</span>
            </label>
            <input
              v-model="createForm.targetId"
              class="form-input"
              placeholder="请输入帖子ID、评论ID或用户UID"
            />
          </div>
        </div>
        <div class="modal-footer">
          <div class="modal-actions">
            <button class="btn btn-secondary" @click="showCreateDialog = false">取消</button>
          </div>
          <div class="modal-actions">
            <button class="btn btn-primary" @click="submitTicket"><i class="fas fa-paper-plane"></i> 提交工单</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showDetailDrawer && currentTicket" class="modal-overlay" @click.self="showDetailDrawer = false">
      <div class="modal" style="max-width: 720px;">
        <div class="modal-header">
          <div class="modal-title">
            <span :class="['ticket-type-badge', `badge-${currentTicket.ticketType === 'CONTENT_REPORT' ? 'report' : currentTicket.ticketType === 'ACCOUNT_APPEAL' ? 'appeal' : currentTicket.ticketType === 'SUGGESTION' ? 'suggest' : 'feedback'}`]">
              <i :class="currentTicket.ticketType === 'CONTENT_REPORT' ? 'fas fa-flag' : currentTicket.ticketType === 'ACCOUNT_APPEAL' ? 'fas fa-gavel' : currentTicket.ticketType === 'SUGGESTION' ? 'fas fa-lightbulb' : 'fas fa-comment-dots'"></i>
              {{ getTicketTypeLabel(currentTicket.ticketType) }}
            </span>
            工单详情 #<span>{{ currentTicket.ticketId }}</span>
          </div>
          <button class="modal-close" @click="showDetailDrawer = false"><i class="fas fa-times"></i></button>
        </div>
        <div class="modal-body">
          <div v-if="detailLoading" style="text-align:center;padding:40px;">
            <i class="fas fa-spinner fa-pulse" style="font-size:32px;color:var(--text-muted);"></i>
          </div>
          <template v-else>
            <div class="detail-section">
              <div class="detail-label">工单标题</div>
              <div class="detail-content">{{ currentTicket.content || '无描述' }}</div>
            </div>

            <div class="detail-grid">
              <div class="detail-item">
                <div class="detail-item-label">工单类型</div>
                <div class="detail-item-value">{{ getTicketTypeLabel(currentTicket.ticketType) }}</div>
              </div>
              <div class="detail-item">
                <div class="detail-item-label">当前状态</div>
                <div class="detail-item-value">
                  <span :class="['ticket-status', getStatusInfo(currentTicket.status).cls]">
                    {{ getStatusInfo(currentTicket.status).label }}
                  </span>
                </div>
              </div>
              <div class="detail-item">
                <div class="detail-item-label">提交时间</div>
                <div class="detail-item-value">{{ formatDateTime(currentTicket.createdAt) }}</div>
              </div>
              <div class="detail-item" v-if="currentTicket.targetId">
                <div class="detail-item-label">目标对象</div>
                <div class="detail-item-value">{{ currentTicket.targetId }}</div>
              </div>
            </div>

            <div class="detail-section">
              <div class="detail-label">详细描述</div>
              <div class="detail-content">
                <p>{{ currentTicket.content || '暂无描述内容' }}</p>
              </div>
            </div>

            <div v-if="currentTicket.evidence && currentTicket.evidence.length" class="detail-section">
              <div class="detail-label">附件证据</div>
              <div class="detail-content">
                <div style="display: flex; gap: 12px; flex-wrap: wrap;">
                  <div
                    v-for="ev in currentTicket.evidence"
                    :key="ev.uuid"
                    style="width: 160px; height: 120px; border-radius: var(--radius-sm); background: var(--bg); display: flex; align-items: center; justify-content: center; border: 1px solid var(--border); cursor: pointer;"
                  >
                    <a :href="ev.url" target="_blank" style="color:var(--primary);font-size:13px;text-align:center;word-break:break-all;padding:8px;">
                      <i class="fas fa-file" style="font-size:24px;color:var(--text-muted);display:block;margin-bottom:4px;"></i>
                      {{ ev.uuid.substring(0, 8) }}...
                    </a>
                  </div>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <div class="detail-label">处理进度</div>
              <div class="timeline">
                <div class="timeline-item">
                  <div class="timeline-dot create"></div>
                  <div class="timeline-time">{{ formatDateTime(currentTicket.createdAt) }}</div>
                  <div class="timeline-content"><strong>工单提交</strong> - 你提交了{{ getTicketTypeLabel(currentTicket.ticketType) }}工单，等待管理员审核</div>
                </div>
                <div v-if="currentTicket.timeline?.reviewedAt" class="timeline-item">
                  <div class="timeline-dot process"></div>
                  <div class="timeline-time">{{ formatDateTime(currentTicket.timeline.reviewedAt) }}</div>
                  <div class="timeline-content"><strong>工单审核</strong> - 工单已被处理</div>
                </div>
              </div>
            </div>

            <div v-if="currentTicket.replies && currentTicket.replies.length" class="detail-section">
              <div class="detail-label">沟通记录</div>
              <div class="timeline">
                <div v-for="reply in currentTicket.replies" :key="reply.id" class="timeline-item" style="padding-bottom:16px;">
                  <div class="timeline-dot" style="background:var(--primary);"></div>
                  <div class="timeline-time">{{ formatDateTime(reply.createdAt) }}</div>
                  <div class="timeline-content"><strong>{{ reply.senderName }}</strong> - {{ reply.content }}</div>
                </div>
              </div>
            </div>
          </template>
        </div>
        <div class="modal-footer">
          <div class="modal-actions">
            <button class="btn btn-secondary" @click="showDetailDrawer = false">关闭</button>
          </div>
          <div class="modal-actions" v-if="currentTicket && (currentTicket.status === 'PENDING')">
            <button class="btn btn-ghost" @click="handleCancelTicket(currentTicket.ticketId)"><i class="fas fa-times"></i> 取消工单</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cs-page {
  min-height: 100vh;
  background: var(--wf-bg, #f8fafc);
}

.main {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-card {
  background: var(--wf-bg-white, #ffffff);
  border: 1px solid var(--wf-border, #e2e8f0);
  border-radius: var(--wf-radius, 12px);
  overflow: hidden;
  box-shadow: var(--wf-shadow, 0 1px 3px 0 rgba(0, 0, 0, 0.05));
}

.sidebar-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--wf-border-light, #f1f5f9);
  font-size: 15px;
  font-weight: 600;
  color: var(--wf-text-primary, #1e293b);
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar-header i {
  color: var(--wf-primary, #3b82f6);
  font-size: 14px;
}

.back-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: var(--wf-bg, #f8fafc);
  border-radius: 6px;
  color: var(--wf-text-secondary, #64748b);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
  font-size: 13px;
}

.back-btn:hover {
  background: var(--wf-primary-light, #eff6ff);
  color: var(--wf-primary, #3b82f6);
}

.sidebar-menu {
  padding: 8px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  font-size: 14px;
  color: var(--wf-text-secondary, #64748b);
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
  border-left: 3px solid transparent;
}

.menu-item:hover {
  color: var(--wf-primary, #3b82f6);
  background: var(--wf-primary-light, #eff6ff);
}

.menu-item.active {
  color: var(--wf-primary, #3b82f6);
  background: var(--wf-primary-light, #eff6ff);
  border-left-color: var(--wf-primary, #3b82f6);
  font-weight: 500;
}

.menu-item i {
  width: 20px;
  text-align: center;
  font-size: 15px;
}

.menu-badge {
  margin-left: auto;
  padding: 2px 8px;
  background: #e2e8f0;
  color: #64748b;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}

.new-ticket-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  margin: 16px 20px;
  background: var(--wf-primary, #3b82f6);
  color: white;
  border: none;
  border-radius: var(--wf-radius-sm, 8px);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
  width: calc(100% - 40px);
}

.new-ticket-btn:hover {
  background: var(--wf-primary-hover, #2563eb);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  transform: translateY(-1px);
}

.content-area {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--wf-text-primary, #1e293b);
  letter-spacing: -0.5px;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: var(--wf-text-muted, #94a3b8);
  margin-top: 4px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: var(--wf-bg-white, #ffffff);
  border: 1px solid var(--wf-border, #e2e8f0);
  border-radius: var(--wf-radius, 12px);
  padding: 20px;
  box-shadow: var(--wf-shadow, 0 1px 3px 0 rgba(0, 0, 0, 0.05));
  transition: all 0.2s ease;
  cursor: pointer;
}

.stat-card:hover {
  box-shadow: var(--wf-shadow-md, 0 4px 6px -1px rgba(0, 0, 0, 0.05));
  transform: translateY(-1px);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--wf-radius-sm, 8px);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-bottom: 12px;
}

.stat-red .stat-icon { background: #fef2f2; color: #ef4444; }
.stat-orange .stat-icon { background: #fffbeb; color: #f59e0b; }
.stat-blue .stat-icon { background: #eff6ff; color: #3b82f6; }
.stat-green .stat-icon { background: #ecfdf5; color: #10b981; }

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--wf-text-primary, #1e293b);
}

.stat-label {
  font-size: 13px;
  color: var(--wf-text-muted, #94a3b8);
  margin-top: 2px;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  background: var(--wf-bg-white, #ffffff);
  border: 1px solid var(--wf-border, #e2e8f0);
  border-radius: var(--wf-radius, 12px);
  box-shadow: var(--wf-shadow, 0 1px 3px 0 rgba(0, 0, 0, 0.05));
}

.filter-tabs {
  display: flex;
  gap: 4px;
}

.filter-tab {
  padding: 6px 14px;
  border: none;
  background: transparent;
  color: var(--wf-text-secondary, #64748b);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--wf-radius-sm, 8px);
  transition: all 0.2s ease;
}

.filter-tab:hover {
  color: var(--wf-primary, #3b82f6);
  background: var(--wf-primary-light, #eff6ff);
}

.filter-tab.active {
  background: var(--wf-primary, #3b82f6);
  color: white;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-select {
  padding: 6px 12px;
  border: 1px solid var(--wf-border, #e2e8f0);
  border-radius: var(--wf-radius-sm, 8px);
  font-size: 13px;
  color: var(--wf-text-secondary, #64748b);
  background: var(--wf-bg-white, #ffffff);
  cursor: pointer;
  outline: none;
}

.filter-select:focus {
  border-color: var(--wf-primary, #3b82f6);
}

.search-input {
  padding: 6px 12px 6px 32px;
  border: 1px solid var(--wf-border, #e2e8f0);
  border-radius: var(--wf-radius-sm, 8px);
  font-size: 13px;
  color: var(--wf-text-primary, #1e293b);
  background: var(--wf-bg, #f8fafc) url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='2'%3E%3Ccircle cx='11' cy='11' r='8'/%3E%3Cpath d='m21 21-4.35-4.35'/%3E%3C/svg%3E") no-repeat 10px center;
  outline: none;
  width: 200px;
}

.search-input:focus {
  border-color: var(--wf-primary, #3b82f6);
  background-color: var(--wf-bg-white, #ffffff);
}

.ticket-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ticket-card {
  background: var(--wf-bg-white, #ffffff);
  border: 1px solid var(--wf-border, #e2e8f0);
  border-radius: var(--wf-radius, 12px);
  padding: 20px;
  box-shadow: var(--wf-shadow, 0 1px 3px 0 rgba(0, 0, 0, 0.05));
  transition: all 0.2s ease;
  cursor: pointer;
}

.ticket-card:hover {
  box-shadow: var(--wf-shadow-md, 0 4px 6px -1px rgba(0, 0, 0, 0.05));
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

.ticket-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.ticket-badges {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ticket-type-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.badge-report { background: #fee2e2; color: #ef4444; }
.badge-appeal { background: #fef3c7; color: #f59e0b; }
.badge-suggest { background: #d1fae5; color: #10b981; }
.badge-feedback { background: #e0e7ff; color: #6366f1; }

.ticket-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending { background: #fef3c7; color: #f59e0b; }
.status-resolved { background: #d1fae5; color: #10b981; }
.status-rejected { background: #fee2e2; color: #ef4444; }
.status-cancelled { background: #e5e7eb; color: #6b7280; }

.ticket-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--wf-text-primary, #1e293b);
  margin-bottom: 8px;
  line-height: 1.5;
}

.ticket-desc {
  font-size: 14px;
  color: var(--wf-text-secondary, #64748b);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
}

.ticket-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid var(--wf-border-light, #f1f5f9);
}

.ticket-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--wf-text-muted, #94a3b8);
}

.ticket-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.ticket-actions {
  display: flex;
  gap: 8px;
}

.ticket-btn {
  padding: 5px 12px;
  border: 1px solid var(--wf-border, #e2e8f0);
  background: var(--wf-bg-white, #ffffff);
  border-radius: var(--wf-radius-sm, 8px);
  font-size: 13px;
  font-weight: 500;
  color: var(--wf-text-secondary, #64748b);
  cursor: pointer;
  transition: all 0.2s ease;
}

.ticket-btn:hover {
  border-color: var(--wf-primary, #3b82f6);
  color: var(--wf-primary, #3b82f6);
  background: var(--wf-primary-light, #eff6ff);
}

.ticket-btn.danger:hover {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  background: var(--wf-bg-white, #ffffff);
  border: 1px solid var(--wf-border, #e2e8f0);
  border-radius: var(--wf-radius, 12px);
  box-shadow: var(--wf-shadow, 0 1px 3px 0 rgba(0, 0, 0, 0.05));
}

.empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--wf-primary-light, #eff6ff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  color: var(--wf-primary, #3b82f6);
  margin-bottom: 20px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--wf-text-primary, #1e293b);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--wf-text-muted, #94a3b8);
  margin-bottom: 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  padding: 16px 0;
}

.page-btn {
  min-width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--wf-border, #e2e8f0);
  background: var(--wf-bg-white, #ffffff);
  border-radius: 6px;
  font-size: 14px;
  color: var(--wf-text-secondary, #64748b);
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover {
  border-color: var(--wf-primary, #3b82f6);
  color: var(--wf-primary, #3b82f6);
}

.page-btn.active {
  background: var(--wf-primary, #3b82f6);
  color: white;
  border-color: var(--wf-primary, #3b82f6);
}

.page-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.modal {
  background: var(--wf-bg-white, #ffffff);
  border-radius: var(--wf-radius, 12px);
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--wf-border, #e2e8f0);
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--wf-text-primary, #1e293b);
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background: var(--wf-bg, #f8fafc);
  border-radius: var(--wf-radius-sm, 8px);
  color: var(--wf-text-muted, #94a3b8);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: #fee2e2;
  color: #ef4444;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section:last-child { margin-bottom: 0; }

.detail-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--wf-text-muted, #94a3b8);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.detail-content {
  font-size: 14px;
  color: var(--wf-text-secondary, #64748b);
  line-height: 1.7;
}

.detail-content p { margin-bottom: 8px; }
.detail-content p:last-child { margin-bottom: 0; }

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item-label {
  font-size: 12px;
  color: var(--wf-text-muted, #94a3b8);
}

.detail-item-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--wf-text-primary, #1e293b);
}

.timeline {
  position: relative;
  padding-left: 24px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--wf-border, #e2e8f0);
}

.timeline-item {
  position: relative;
  padding-bottom: 20px;
}

.timeline-item:last-child { padding-bottom: 0; }

.timeline-dot {
  position: absolute;
  left: -24px;
  top: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 3px solid var(--wf-bg-white, #ffffff);
}

.timeline-dot.create { background: var(--wf-primary, #3b82f6); }
.timeline-dot.process { background: #f59e0b; }
.timeline-dot.resolve { background: #10b981; }
.timeline-dot.cancel { background: var(--wf-text-muted, #94a3b8); }

.timeline-time {
  font-size: 12px;
  color: var(--wf-text-muted, #94a3b8);
  margin-bottom: 4px;
}

.timeline-content {
  font-size: 14px;
  color: var(--wf-text-secondary, #64748b);
  line-height: 1.6;
}

.timeline-content strong {
  color: var(--wf-text-primary, #1e293b);
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-top: 1px solid var(--wf-border, #e2e8f0);
  background: var(--wf-bg, #f8fafc);
}

.modal-actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 8px 20px;
  border-radius: var(--wf-radius-sm, 8px);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-secondary {
  background: var(--wf-bg-white, #ffffff);
  color: var(--wf-text-secondary, #64748b);
  border: 1px solid var(--wf-border, #e2e8f0);
}

.btn-secondary:hover {
  border-color: #cbd5e1;
  color: var(--wf-text-primary, #1e293b);
}

.btn-primary {
  background: var(--wf-primary, #3b82f6);
  color: white;
}

.btn-primary:hover {
  background: var(--wf-primary-hover, #2563eb);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.btn-ghost {
  background: transparent;
  color: var(--wf-text-muted, #94a3b8);
  border: none;
}

.btn-ghost:hover {
  color: var(--wf-text-secondary, #64748b);
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--wf-text-primary, #1e293b);
  margin-bottom: 8px;
}

.form-label .required {
  color: #ef4444;
  margin-left: 2px;
}

.form-hint {
  font-size: 13px;
  color: var(--wf-text-muted, #94a3b8);
  margin-top: 6px;
  text-align: right;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--wf-border, #e2e8f0);
  border-radius: var(--wf-radius-sm, 8px);
  font-size: 14px;
  color: var(--wf-text-primary, #1e293b);
  background: var(--wf-bg-white, #ffffff);
  outline: none;
  transition: all 0.2s ease;
  font-family: inherit;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: var(--wf-primary, #3b82f6);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

textarea.form-input {
  min-height: 120px;
  resize: vertical;
  line-height: 1.7;
}

.type-select {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.type-option {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border: 1.5px solid var(--wf-border, #e2e8f0);
  border-radius: var(--wf-radius-sm, 8px);
  font-size: 14px;
  font-weight: 500;
  color: var(--wf-text-secondary, #64748b);
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--wf-bg-white, #ffffff);
}

.type-option:hover {
  border-color: #cbd5e1;
  color: var(--wf-text-primary, #1e293b);
}

.type-option.active {
  border-color: var(--wf-primary, #3b82f6);
  color: var(--wf-primary, #3b82f6);
  background: var(--wf-primary-light, #eff6ff);
}

.footer {
  background: var(--wf-bg-white, #ffffff);
  border-top: 1px solid var(--wf-border, #e2e8f0);
  padding: 32px 24px;
  margin-top: 20px;
}

.footer-inner {
  max-width: 1280px;
  margin: 0 auto;
  text-align: center;
}

.footer-links {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 16px;
}

.footer-links a {
  font-size: 14px;
  color: var(--wf-text-secondary, #64748b);
  text-decoration: none;
  transition: color 0.2s;
}

.footer-links a:hover { color: var(--wf-primary, #3b82f6); }

.footer-copyright {
  font-size: 13px;
  color: var(--wf-text-muted, #94a3b8);
}

@media (max-width: 1024px) {
  .main {
    grid-template-columns: 1fr;
  }
  .sidebar {
    display: none;
  }
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .main { padding: 16px; }
  .stats-row { grid-template-columns: 1fr 1fr; }
  .filter-bar { flex-direction: column; align-items: stretch; }
  .filter-right { justify-content: space-between; }
  .detail-grid { grid-template-columns: 1fr; }
}
</style>
