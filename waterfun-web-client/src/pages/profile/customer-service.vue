<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTicketStore } from '~/stores/ticketStore'
import { storeToRefs } from 'pinia'
import { useUserInfoStore } from '~/stores/userInfoStore'
import type { TicketType, TicketStatus, CreateUserReportReq, ReportType, UserTicketDetailResponse, ReplyItem } from '~/api/ticketApi'

const ticketStore = useTicketStore()
const userInfoStore = useUserInfoStore()
const { tickets, ticketStats, pagination, loading, currentTicket, detailLoading } = storeToRefs(ticketStore)

const activeFilterType = ref<TicketType | ''>('')
const activeFilterStatus = ref<TicketStatus | ''>('')
const showCreateDialog = ref(false)
const showDetailDrawer = ref(false)
const showReplyDialog = ref(false)
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

const statsItems = computed(() => {
  if (!ticketStats.value) return []
  return [
    { label: '内容举报', count: ticketStats.value.reportCount, icon: 'fas fa-flag', cls: 'stat-red' },
    { label: '账号申诉', count: ticketStats.value.appealCount, icon: 'fas fa-user-shield', cls: 'stat-orange' },
    { label: '功能反馈', count: ticketStats.value.feedbackCount, icon: 'fas fa-lightbulb', cls: 'stat-blue' },
    { label: '意见建议', count: ticketStats.value.suggestionCount, icon: 'fas fa-comment-dots', cls: 'stat-green' },
  ]
})

const handleFilterTypeChange = (type: TicketType | '') => {
  activeFilterType.value = type
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
  if (isReasonRequired.value && !createForm.reason.trim()) {
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
    if (createForm.reason.trim()) {
      payload.reason = createForm.reason.trim()
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
  <div class="customer-service">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">客服中心</h1>
        <p class="page-desc">提交工单，我们会在第一时间为你处理</p>
      </div>
      <button class="create-btn" @click="openCreateDialog">
        <i class="fas fa-plus"></i> 新建工单
      </button>
    </div>

    <div v-if="statsItems.length" class="stats-bar">
      <div
        v-for="(item, idx) in statsItems"
        :key="idx"
        :class="['stat-card', item.cls]"
        @click="handleFilterTypeChange(ticketTypeOptions[idx].value)"
      >
        <div class="stat-icon"><i :class="item.icon"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ formatCount(item.count) }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="filter-group">
            <span class="filter-label">类型：</span>
            <button
              :class="['filter-btn', { active: activeFilterType === '' }]"
              @click="handleFilterTypeChange('')"
            >全部</button>
            <button
              v-for="opt in ticketTypeOptions"
              :key="opt.value"
              :class="['filter-btn', { active: activeFilterType === opt.value }]"
              @click="handleFilterTypeChange(opt.value)"
            >{{ opt.label }}</button>
          </div>
        </div>
        <div class="toolbar-right">
          <div class="filter-group">
            <span class="filter-label">状态：</span>
            <button
              :class="['filter-btn', { active: activeFilterStatus === '' }]"
              @click="handleFilterStatusChange('')"
            >全部</button>
            <button
              v-for="opt in ticketStatusOptions"
              :key="opt.value"
              :class="['filter-btn', { active: activeFilterStatus === opt.value }]"
              @click="handleFilterStatusChange(opt.value)"
            >{{ opt.label }}</button>
          </div>
        </div>
      </div>

      <div v-if="loading && !tickets.length" class="empty-state">
        <div class="empty-icon"><i class="fas fa-spinner fa-pulse"></i></div>
        <div class="empty-title">加载中...</div>
      </div>

      <div v-else-if="!tickets.length" class="empty-state">
        <div class="empty-icon"><i class="fas fa-ticket-alt"></i></div>
        <div class="empty-title">暂无工单</div>
        <div class="empty-desc">创建一个新的工单，我们将尽快为你处理</div>
      </div>

      <div v-else class="ticket-list">
        <div
          v-for="ticket in tickets"
          :key="ticket.ticketId"
          class="ticket-item"
          @click="openDetail(ticket.ticketId)"
        >
          <div class="ticket-header">
            <div class="ticket-type">
              <i v-if="ticket.ticketType === 'CONTENT_REPORT'" class="fas fa-flag type-icon report"></i>
              <i v-else-if="ticket.ticketType === 'ACCOUNT_APPEAL'" class="fas fa-user-shield type-icon appeal"></i>
              <i v-else-if="ticket.ticketType === 'FEATURE_FEEDBACK'" class="fas fa-lightbulb type-icon feedback"></i>
              <i v-else class="fas fa-comment-dots type-icon suggestion"></i>
              <span>{{ getTicketTypeLabel(ticket.ticketType) }}</span>
            </div>
            <div :class="['ticket-status', getStatusInfo(ticket.status).cls]">
              {{ getStatusInfo(ticket.status).label }}
            </div>
          </div>
          <div class="ticket-content">{{ ticket.content || '无描述' }}</div>
          <div class="ticket-meta">
            <span class="ticket-time">
              <i class="far fa-clock"></i>
              {{ formatDateTime(ticket.createdAt) }}
            </span>
            <span v-if="ticket.evidenceCount > 0" class="ticket-evidence">
              <i class="fas fa-paperclip"></i>
              {{ ticket.evidenceCount }} 个附件
            </span>
          </div>
        </div>
      </div>

      <div v-if="pagination.totalPages > 1" class="pagination">
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

    <div v-if="showCreateDialog" class="wf-dialog-overlay" @click.self="showCreateDialog = false">
      <div class="wf-dialog-panel">
        <div class="wf-dialog-header">
          <h2>新建工单</h2>
          <button class="wf-dialog-close" @click="showCreateDialog = false"><i class="fas fa-times"></i></button>
        </div>
        <div class="wf-dialog-body">
          <div class="wf-form-group">
            <label class="wf-form-label">工单类型 <span class="wf-required">*</span></label>
            <div class="type-selector">
              <button
                v-for="opt in ticketTypeOptions"
                :key="opt.value"
                :class="['type-card', { active: createForm.ticketType === opt.value }]"
                @click="createForm.ticketType = opt.value"
              >
                <i :class="opt.icon"></i>
                <span class="type-name">{{ opt.label }}</span>
                <span class="type-desc">{{ opt.desc }}</span>
              </button>
            </div>
          </div>

          <div v-if="createForm.ticketType === 'CONTENT_REPORT'" class="wf-form-group">
            <label class="wf-form-label">举报类型 <span class="wf-required">*</span></label>
            <select v-model="createForm.type" class="wf-form-control">
              <option v-for="opt in reportTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>

          <div v-if="createForm.ticketType === 'CONTENT_REPORT'" class="wf-form-group">
            <label class="wf-form-label">目标类型 <span class="wf-required">*</span></label>
            <div class="type-selector">
              <button
                :class="['type-card', { active: createForm.targetType === 'POST' }]"
                @click="createForm.targetType = 'POST'"
              >
                <i class="fas fa-file-alt"></i>
                <span class="type-name">帖子</span>
              </button>
              <button
                :class="['type-card', { active: createForm.targetType === 'COMMENT' }]"
                @click="createForm.targetType = 'COMMENT'"
              >
                <i class="fas fa-comment"></i>
                <span class="type-name">评论</span>
              </button>
              <button
                :class="['type-card', { active: createForm.targetType === 'USER' }]"
                @click="createForm.targetType = 'USER'"
              >
                <i class="fas fa-user"></i>
                <span class="type-name">用户</span>
              </button>
            </div>
          </div>

          <div class="wf-form-group">
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px;">
              <label class="wf-form-label" style="margin-bottom:0;">问题描述 <span v-if="isReasonRequired" class="wf-required">*</span></label>
              <button type="button" @click="reasonExpanded = !reasonExpanded" style="background:none;border:1px solid var(--wf-border,#e2e8f0);border-radius:6px;padding:2px 10px;font-size:12px;color:var(--wf-text-muted,#94a3b8);cursor:pointer;display:inline-flex;align-items:center;gap:4px;">
                <i :class="reasonExpanded ? 'fas fa-chevron-up' : 'fas fa-chevron-down'" style="font-size:10px;"></i>
                {{ reasonExpanded ? '收起' : '展开' }}
              </button>
            </div>
            <div v-show="reasonExpanded">
              <textarea
                v-model="createForm.reason"
                class="wf-form-control"
                rows="5"
                placeholder="请详细描述你遇到的问题或建议..."
                maxlength="2000"
              ></textarea>
              <span class="wf-char-count">{{ createForm.reason.length }}/2000</span>
            </div>
          </div>

          <div v-if="createForm.ticketType === 'CONTENT_REPORT'" class="wf-form-group">
            <label class="wf-form-label">
              目标ID
              <span class="wf-required">*</span>
            </label>
            <input
              v-model="createForm.targetId"
              class="wf-form-control"
              placeholder="请输入帖子ID、评论ID或用户UID"
            />
          </div>
        </div>
        <div class="wf-dialog-footer">
          <button class="wf-btn-cancel" @click="showCreateDialog = false">取消</button>
          <button class="wf-btn-submit" @click="submitTicket">提交工单</button>
        </div>
      </div>
    </div>

    <div v-if="showDetailDrawer && currentTicket" class="drawer-overlay" @click.self="showDetailDrawer = false">
      <div class="drawer-panel">
        <div class="drawer-header">
          <h2>工单详情</h2>
          <button class="drawer-close" @click="showDetailDrawer = false"><i class="fas fa-times"></i></button>
        </div>
        <div class="drawer-body">
          <div v-if="detailLoading" class="empty-state">
            <div class="empty-icon"><i class="fas fa-spinner fa-pulse"></i></div>
          </div>
          <template v-else>
            <div class="detail-section">
              <div class="detail-row">
                <span class="detail-label">工单编号</span>
                <span class="detail-value">#{{ currentTicket.ticketId }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">工单类型</span>
                <span class="detail-value">{{ getTicketTypeLabel(currentTicket.ticketType) }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">当前状态</span>
                <span :class="['detail-status', getStatusInfo(currentTicket.status).cls]">
                  {{ getStatusInfo(currentTicket.status).label }}
                </span>
              </div>
              <div class="detail-row">
                <span class="detail-label">提交时间</span>
                <span class="detail-value">{{ formatDateTime(currentTicket.createdAt) }}</span>
              </div>
              <div class="detail-row" v-if="currentTicket.updatedAt">
                <span class="detail-label">更新时间</span>
                <span class="detail-value">{{ formatDateTime(currentTicket.updatedAt) }}</span>
              </div>
              <div class="detail-row" v-if="currentTicket.auditAt">
                <span class="detail-label">审核时间</span>
                <span class="detail-value">{{ formatDateTime(currentTicket.auditAt) }}</span>
              </div>
              <div class="detail-row" v-if="currentTicket.auditNote">
                <span class="detail-label">审核备注</span>
                <span class="detail-value">{{ currentTicket.auditNote }}</span>
              </div>
            </div>

            <div class="detail-section">
              <h3 class="section-title">问题描述</h3>
              <p class="detail-content">{{ currentTicket.content || '无描述' }}</p>
            </div>

            <div v-if="currentTicket.evidence && currentTicket.evidence.length" class="detail-section">
              <h3 class="section-title">附件证据</h3>
              <div class="evidence-list">
                <div v-for="ev in currentTicket.evidence" :key="ev.uuid" class="evidence-item">
                  <i class="fas fa-file"></i>
                  <a :href="ev.url" target="_blank">{{ ev.uuid }}</a>
                </div>
              </div>
            </div>

            <div v-if="currentTicket.timeline" class="detail-section">
              <h3 class="section-title">处理时间线</h3>
              <div class="timeline">
                <div class="timeline-item">
                  <div class="timeline-dot"></div>
                  <div class="timeline-content">
                    <span class="timeline-time">{{ formatDateTime(currentTicket.timeline.submittedAt) }}</span>
                    <span class="timeline-text">工单已提交</span>
                  </div>
                </div>
                <div v-if="currentTicket.timeline.reviewedAt" class="timeline-item">
                  <div class="timeline-dot"></div>
                  <div class="timeline-content">
                    <span class="timeline-time">{{ formatDateTime(currentTicket.timeline.reviewedAt) }}</span>
                    <span class="timeline-text">工单已审核</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="currentTicket.replies && currentTicket.replies.length" class="detail-section">
              <h3 class="section-title">沟通记录</h3>
              <div class="reply-list">
                <div v-for="reply in currentTicket.replies" :key="reply.id" class="reply-item">
                  <div class="reply-header">
                    <span class="reply-sender">{{ reply.senderName }}</span>
                    <span class="reply-time">{{ formatDateTime(reply.createdAt) }}</span>
                  </div>
                  <p class="reply-content">{{ reply.content }}</p>
                </div>
              </div>
            </div>

            <div class="detail-actions">
              <button
                v-if="currentTicket.status === 'PENDING'"
                class="btn-cancel-ticket"
                @click="handleCancelTicket(currentTicket.ticketId)"
              >
                <i class="fas fa-times-circle"></i> 取消工单
              </button>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.customer-service {
  max-width: 900px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}

.header-left {
  flex: 1;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px;
  letter-spacing: -0.3px;
}

.page-desc {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}

.create-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
  white-space: nowrap;
}

.create-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35);
}

.stats-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
  cursor: pointer;
}

.stat-card:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;
}

.stat-red .stat-icon { background: #fef2f2; color: #ef4444; }
.stat-orange .stat-icon { background: #fffbeb; color: #f59e0b; }
.stat-blue .stat-icon { background: #eff6ff; color: #3b82f6; }
.stat-green .stat-icon { background: #ecfdf5; color: #10b981; }

.stat-info { flex: 1; }

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 2px;
}

.content-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid #f1f5f9;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 13px;
  color: #94a3b8;
  margin-right: 4px;
}

.filter-btn {
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  background: transparent;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.filter-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background: #eff6ff;
}

.filter-btn.active {
  background: #3b82f6;
  color: #fff;
  border-color: #3b82f6;
}

.ticket-list {
  padding: 0;
}

.ticket-item {
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.2s ease;
}

.ticket-item:last-child { border-bottom: none; }

.ticket-item:hover { background: #f8fafc; }

.ticket-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.ticket-type {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.type-icon {
  font-size: 14px;
}

.type-icon.report { color: #ef4444; }
.type-icon.appeal { color: #f59e0b; }
.type-icon.feedback { color: #3b82f6; }
.type-icon.suggestion { color: #10b981; }

.ticket-status {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 20px;
}

.status-pending { background: #fffbeb; color: #f59e0b; }
.status-resolved { background: #ecfdf5; color: #10b981; }
.status-rejected { background: #fef2f2; color: #ef4444; }

.ticket-content {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ticket-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: #94a3b8;
}

.ticket-meta i { margin-right: 3px; }

.empty-state {
  padding: 50px 20px;
  text-align: center;
}

.empty-icon {
  width: 70px;
  height: 70px;
  background: #f1f5f9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
  font-size: 28px;
  color: #94a3b8;
}

.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}

.empty-desc {
  font-size: 14px;
  color: #94a3b8;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  padding: 16px 20px;
  border-top: 1px solid #f1f5f9;
}

.page-btn {
  min-width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 6px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.page-btn.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.page-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.drawer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: flex-end;
  z-index: 1000;
}

.drawer-panel {
  background: #fff;
  width: 520px;
  max-width: 85vw;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  animation: slideIn 0.2s ease;
}

@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.drawer-header h2 {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.drawer-close {
  width: 32px;
  height: 32px;
  border: none;
  background: #f1f5f9;
  border-radius: 8px;
  color: #94a3b8;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.drawer-close:hover {
  background: #e2e8f0;
  color: #64748b;
}

.drawer-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.detail-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.detail-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 12px;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  padding: 8px 0;
}

.detail-label {
  width: 90px;
  flex-shrink: 0;
  font-size: 13px;
  color: #94a3b8;
}

.detail-value {
  font-size: 14px;
  color: #1e293b;
  word-break: break-all;
}

.detail-status {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 20px;
}

.detail-content {
  font-size: 14px;
  color: #475569;
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}

.evidence-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.evidence-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
}

.evidence-item i { color: #3b82f6; }

.evidence-item a {
  color: #3b82f6;
  text-decoration: none;
  word-break: break-all;
}

.evidence-item a:hover { text-decoration: underline; }

.timeline {
  position: relative;
  padding-left: 20px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 4px;
  bottom: 4px;
  width: 2px;
  background: #e2e8f0;
}

.timeline-item {
  position: relative;
  padding-bottom: 20px;
}

.timeline-item:last-child { padding-bottom: 0; }

.timeline-dot {
  position: absolute;
  left: -16px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #3b82f6;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px #3b82f6;
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.timeline-time {
  font-size: 12px;
  color: #94a3b8;
}

.timeline-text {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-item {
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.reply-sender {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.reply-time {
  font-size: 12px;
  color: #94a3b8;
}

.reply-content {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
}

.detail-actions {
  display: flex;
  gap: 10px;
  padding-top: 16px;
}

.btn-cancel-ticket {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #ef4444;
  background: #fef2f2;
  border: 1px solid #fca5a5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel-ticket:hover {
  background: #fee2e2;
  border-color: #ef4444;
}
</style>
