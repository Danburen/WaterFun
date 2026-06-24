<script setup lang="ts">
import { ElMessage } from "element-plus";
import {
  listTickets,
  reviewTicket,
  type TicketResponse,
  type TicketReviewRequest,
} from "~/api/tickets";
import type { PageOptions } from "~/types/api";
import { statusLabel, statusChipClass, snapshotTypeClass, snapshotTypeLabel, penaltyLabel, previewImage } from "~/composables/useModeration";
import UserBrief from "~/components/moderation/UserBrief.vue";

const loading = ref(false);
const ticketList = ref<TicketResponse[]>([]);
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 10, total: 0 });
const filterStatus = ref<string>("PENDING");
const currentAction = ref<"overturn" | "maintain" | null>(null);
const actionNote = ref("");
const currentTicketId = ref<number | null>(null);

const penaltyIconClass = (t?: string) => {
  if (t === "BAN_LOGIN" || t === "BAN_POST" || t === "BAN_COMMENT" || t?.startsWith("BAN_")) return "ban";
  if (t === "OTHER") return "delete";
  return "mute";
};
const penaltyFaIcon = (t?: string) => {
  if (t === "BAN_LOGIN") return "fa-solid fa-ban";
  if (t === "BAN_POST" || t === "BAN_COMMENT") return "fa-solid fa-microphone-slash";
  if (t === "DELETE") return "fa-solid fa-trash-alt";
  return "fa-solid fa-exclamation-triangle";
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      ticketTypes: "ACCOUNT_APPEAL",
    };
    if (filterStatus.value) params.status = filterStatus.value;
    const res = await listTickets(params);
    ticketList.value = res.data.content || [];
    pageOpts.value.total = res.data.page?.totalElements ?? res.data.totalElements ?? 0;
  } catch {
    ElMessage.error("获取申诉列表失败");
  } finally {
    loading.value = false;
  }
};

const showAction = (id: number, action: "overturn" | "maintain") => {
  currentTicketId.value = id;
  currentAction.value = action;
  actionNote.value = "";
};
const hideAction = () => {
  currentAction.value = null;
  currentTicketId.value = null;
  actionNote.value = "";
};
const confirmAction = async () => {
  if (!currentTicketId.value || !currentAction.value) return;
  if (!actionNote.value.trim()) {
    ElMessage.warning(currentAction.value === "overturn" ? "请填写撤销说明" : "请填写维持说明");
    return;
  }
  try {
    const data: TicketReviewRequest = {
      action: currentAction.value === "overturn" ? "APPROVE" : "REJECT",
      auditNote: actionNote.value,
      replyContent: actionNote.value,
      rejectType: currentAction.value === "overturn" ? "APPEAL_ACCEPTED" : "NO_VIOLATION",
    };
    await reviewTicket(currentTicketId.value, data);
    ElMessage.success(currentAction.value === "overturn" ? "处罚已撤销，工单已完成" : "已维持原判，工单已完成");
    hideAction();
    await fetchData();
  } catch {
    ElMessage.error("操作失败");
  }
};

onMounted(() => fetchData());
</script>

<template>
  <div class="moderation-view">
    <div class="queue-info">
      <div class="queue-info-left">
        <div class="queue-stat"><i class="fa-solid fa-inbox"></i> 待处理 <strong>{{ ticketList.filter(t => t.status === 'PENDING').length }}</strong></div>
        <div class="queue-stat"><i class="fa-solid fa-check-circle"></i> 已撤销 <strong>{{ ticketList.filter(t => t.status === 'RESOLVED').length }}</strong></div>
        <div class="queue-stat"><i class="fa-solid fa-times-circle"></i> 已维持 <strong>{{ ticketList.filter(t => t.status === 'REJECTED').length }}</strong></div>
      </div>
      <div class="queue-info-right">
        <button :class="['filter-btn', { active: filterStatus === 'PENDING' }]" @click="filterStatus = 'PENDING'; pageOpts.currentPage = 1; fetchData()">待复核</button>
        <button :class="['filter-btn', { active: filterStatus === 'RESOLVED' }]" @click="filterStatus = 'RESOLVED'; pageOpts.currentPage = 1; fetchData()">已撤销</button>
        <button :class="['filter-btn', { active: filterStatus === 'REJECTED' }]" @click="filterStatus = 'REJECTED'; pageOpts.currentPage = 1; fetchData()">已维持</button>
        <button :class="['filter-btn', { active: filterStatus === '' }]" @click="filterStatus = ''; pageOpts.currentPage = 1; fetchData()">全部</button>
      </div>
    </div>

    <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>

    <template v-else>
      <div v-if="ticketList.length === 0" class="empty-box">暂无申诉工单</div>

      <div v-for="item in ticketList" :key="item.ticketId ?? item.id" class="review-card">
        <div class="review-card-header">
          <div class="review-meta">
            <span class="review-type-badge badge-appeal"><i class="fa-solid fa-gavel"></i> 申诉</span>
            <span class="review-id">#APL-{{ item.ticketId ?? item.id }}</span>
            <span class="review-time"><i class="fa-regular fa-clock"></i> {{ item.createdAt?.replace('T', ' ').substring(0, 16) || '-' }}</span>
          </div>
          <div class="review-status-bar">
            <span :class="['status-chip', statusChipClass(item.status, 'ACCOUNT_APPEAL')]">
              <i class="fa-solid fa-hourglass-half"></i> {{ statusLabel(item.status, 'ACCOUNT_APPEAL') }}
            </span>
          </div>
        </div>

        <div class="review-body">
          <div class="appeal-layout">
            <div class="appeal-content-panel">
              <div class="appeal-reason-box">
                <div class="panel-label"><i class="fa-solid fa-edit"></i> 申诉理由</div>
                <div class="appeal-reason-text">
                  <p>{{ item.content || "(无内容)" }}</p>
                </div>
                <div v-if="item.attachments?.length" class="appeal-attachments">
                  <div class="attachments-label">申诉材料</div>
                  <div class="attachments-grid">
                    <img v-for="(att, i) in item.attachments" :key="i" :src="att" alt="材料" @click="previewImage(att)">
                  </div>
                </div>
              </div>
              <div class="penalty-card">
                <div class="panel-label"><i class="fa-solid fa-exclamation-triangle"></i> 原处罚记录</div>
                <div class="penalty-summary">
                  <div class="penalty-icon" :class="penaltyIconClass(item.penaltyType)"><i :class="penaltyFaIcon(item.penaltyType)"></i></div>
                  <div class="penalty-info">
                    <div class="penalty-type">{{ penaltyLabel(item.banReasonType) }}</div>
                    <div class="penalty-desc">处罚类型: {{ penaltyLabel(item.penaltyType) }}</div>
                    <div class="penalty-time"><i class="fa-regular fa-clock"></i> 处罚时间: {{ item.createdAt?.replace('T', ' ').substring(0, 19) || '-' }}</div>
                  </div>
                </div>
              </div>
              <div v-if="item.currentBans" class="active-bans-card">
                <div class="panel-label"><i class="fa-solid fa-shield"></i> 当前封禁状态</div>
                <div class="ban-status-header">
                  <span :class="['ban-badge', item.currentBans.banned ? 'banned' : 'active']">
                    <i :class="item.currentBans.banned ? 'fa-solid fa-lock' : 'fa-solid fa-check-circle'"></i>
                    {{ item.currentBans.banned ? '已封禁' : '正常' }}
                  </span>
                </div>
                <div v-if="item.currentBans.restrictions.length" class="active-bans-list">
                  <div v-for="(ban, i) in item.currentBans.restrictions" :key="i" class="ban-row">
                    <div class="ban-row-left">
                      <div class="ban-perm-name">{{ ban.permissionName || ban.permissionCode }}</div>
                      <div class="ban-reason-type" v-if="ban.banReasonType">{{ ban.banReasonType }}</div>
                    </div>
                    <div class="ban-row-right">
                      <span v-if="ban.permanent" class="ban-expiry permanent">永久</span>
                      <span v-else class="ban-expiry">{{ ban.expiresAt?.replace('T', ' ').substring(0, 16) }}</span>
                    </div>
                  </div>
                </div>
                <div v-else-if="!item.currentBans.banned" class="no-active-bans">该用户当前无活跃限制</div>
              </div>
              <div class="panel-label"><i class="fa-solid fa-eye"></i> 被处罚的原内容</div>
              <div class="content-snapshot">
                <span class="snapshot-type" :class="snapshotTypeClass(item.targetType)">
                  <i class="fa-solid fa-comment"></i> {{ snapshotTypeLabel(item.targetType) }}
                </span>
                <div class="snapshot-body">
                  <p>{{ item.content || "(无内容)" }}</p>
                </div>
              </div>
            </div>

            <div>
              <div class="info-card">
                <div class="info-card-title"><i class="fa-solid fa-user"></i> 申诉人</div>
                <UserBrief :user="item.submitter" size="lg" />
                <div class="info-item">
                  <div class="info-icon" style="background: var(--success-light); color: #15803d;"><i class="fa-solid fa-shield-alt"></i></div>
                  <div class="info-detail">
                    <div class="info-label">账号状态</div>
                    <div class="info-value">{{ item.status === 'PENDING' ? '正常（待复核）' : statusLabel(item.status, 'ACCOUNT_APPEAL') }}</div>
                  </div>
                </div>
                <div class="info-item">
                  <div class="info-icon" style="background: var(--primary-light); color: var(--primary);"><i class="fa-solid fa-history"></i></div>
                  <div class="info-detail">
                    <div class="info-label">历史申诉</div>
                    <div class="info-value">{{ item.submitter?.reportCount || 0 }} 次</div>
                  </div>
                </div>
              </div>
              <div class="info-card" style="margin-top: 16px;">
                <div class="info-card-title"><i class="fa-solid fa-stream"></i> 处理时间线</div>
                <div class="appeal-history-item">
                  <div class="history-dot penalty"></div>
                  <div class="history-content">
                    <div class="history-title">内容被处罚</div>
                    <div class="history-desc">{{ penaltyLabel(item.penaltyType) || '系统处理' }}</div>
                    <div class="history-time">{{ item.createdAt?.replace('T', ' ').substring(0, 19) || '-' }}</div>
                  </div>
                </div>
                <div class="appeal-history-item">
                  <div class="history-dot appeal"></div>
                  <div class="history-content">
                    <div class="history-title">用户提交申诉</div>
                    <div class="history-desc">请求人工复核</div>
                    <div class="history-time">{{ item.updatedAt?.replace('T', ' ').substring(0, 19) || item.createdAt?.replace('T', ' ').substring(0, 19) || '-' }}</div>
                  </div>
                </div>
                <div class="appeal-history-item">
                  <div class="history-dot review"></div>
                  <div class="history-content">
                    <div class="history-title">等待人工复核</div>
                    <div class="history-desc">当前状态</div>
                    <div class="history-time">--</div>
                  </div>
                </div>
              </div>
              <div class="info-card" style="margin-top: 16px;">
                <div class="info-card-title"><i class="fa-solid fa-link"></i> 关联信息</div>
                <div class="info-item">
                  <div class="info-icon" style="background: var(--primary-light); color: var(--primary);"><i class="fa-solid fa-file-alt"></i></div>
                  <div class="info-detail">
                    <div class="info-value">{{ item.relatedTitle || '目标 #' + (item.targetId || '-') }}</div>
                    <div class="info-label">{{ snapshotTypeLabel(item.targetType) }}</div>
                  </div>
                  <a v-if="item.targetId" href="#" style="color: var(--primary); font-size: 12px; font-weight: 500; text-decoration: none;" @click.prevent="$router.push('/content/' + (item.targetType === 'POST' ? 'post' : 'comment') + '/' + item.targetId)">查看</a>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="review-actions">
          <div class="review-actions-left">
            <button class="action-btn action-btn-detail"><i class="fa-solid fa-history"></i> 申诉历史</button>
            <button class="action-btn action-btn-detail"><i class="fa-solid fa-user"></i> 用户详情</button>
          </div>
          <div class="review-actions-right">
            <button class="action-btn action-btn-skip" @click="fetchData"><i class="fa-solid fa-forward"></i> 跳过</button>
            <button class="action-btn action-btn-reject" @click="showAction(item.ticketId ?? item.id!, 'maintain')"><i class="fa-solid fa-gavel"></i> 维持原判</button>
            <button class="action-btn action-btn-pass" @click="showAction(item.ticketId ?? item.id!, 'overturn')"><i class="fa-solid fa-undo"></i> 撤销处罚</button>
          </div>
        </div>

        <div v-if="currentAction && currentTicketId === (item.ticketId ?? item.id)" :class="['decision-note-wrap', 'show', { 'danger-note': currentAction === 'maintain' }]">
          <label><i class="fa-solid fa-comment-alt"></i> {{ currentAction === 'overturn' ? '撤销说明（将通知用户）' : '维持说明（将通知用户）' }}</label>
          <textarea v-model="actionNote" :placeholder="currentAction === 'overturn' ? '请填写撤销原因...' : '请填写维持原判原因...'"></textarea>
          <div class="decision-actions">
            <button class="action-btn action-btn-skip" @click="hideAction">取消</button>
            <button :class="['action-btn', currentAction === 'overturn' ? 'action-btn-pass' : 'action-btn-reject']" @click="confirmAction">确认{{ currentAction === 'overturn' ? '撤销' : '维持' }}</button>
          </div>
        </div>
      </div>

      <div class="pagination-bar" v-if="pageOpts.total && pageOpts.total > (pageOpts.pageSize || 10)">
        <button class="btn" :disabled="(pageOpts.currentPage || 1) <= 1" @click="pageOpts.currentPage = (pageOpts.currentPage || 1) - 1; fetchData()">上一页</button>
        <span class="page-info">第 {{ pageOpts.currentPage }} / {{ Math.ceil((pageOpts.total || 0) / (pageOpts.pageSize || 10)) }} 页 (共 {{ pageOpts.total }} 条)</span>
        <button class="btn" :disabled="(pageOpts.currentPage || 1) >= Math.ceil((pageOpts.total || 0) / (pageOpts.pageSize || 10))" @click="pageOpts.currentPage = (pageOpts.currentPage || 1) + 1; fetchData()">下一页</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
@import "@/assets/moderation.css";

/* ---- Appeal badge ---- */
.badge-appeal { background: #e0e7ff; color: #4338ca; }

/* ---- Appeal-specific layout & components ---- */
.appeal-layout { display: grid; grid-template-columns: 1fr 380px; gap: 24px; }
.appeal-content-panel { display: flex; flex-direction: column; gap: 16px; }
@media (max-width: 1200px) { .appeal-layout { grid-template-columns: 1fr; } }
@media (max-width: 768px) {
  .appeal-layout { grid-template-columns: 1fr; }
}

.appeal-reason-box {
  padding: 20px; background: var(--primary-light); border-radius: var(--radius-sm);
  border: 1px solid #bfdbfe; border-left: 4px solid var(--primary);
}
.appeal-reason-box .panel-label { color: var(--primary); }
.appeal-reason-text { font-size: 15px; line-height: 1.8; color: var(--text-primary); margin-top: 8px; }
.appeal-reason-text p { margin-bottom: 8px; }
.appeal-attachments { margin-top: 12px; }
.attachments-label { font-size: 12px; font-weight: 600; color: var(--text-muted); margin-bottom: 8px; }
.attachments-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.attachments-grid img { width: 100%; height: 80px; object-fit: cover; border-radius: 6px; cursor: pointer; border: 1px solid #bfdbfe; }

.penalty-card {
  padding: 20px; background: var(--danger-light); border-radius: var(--radius-sm);
  border: 1px solid #fecaca; border-left: 4px solid var(--danger);
}
.penalty-card .panel-label { color: var(--danger); }
.penalty-summary { display: flex; align-items: center; gap: 16px; margin-top: 12px; padding: 16px; background: var(--bg-white); border-radius: var(--radius-sm); }
.penalty-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.penalty-icon.mute { background: var(--warning-light); color: #b45309; }
.penalty-icon.ban { background: var(--danger-light); color: #dc2626; }
.penalty-icon.delete { background: #fce7f3; color: #be185d; }
.penalty-info { flex: 1; }
.penalty-type { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.penalty-desc { font-size: 13px; color: var(--text-secondary); margin-top: 2px; }
.penalty-time { font-size: 12px; color: var(--text-muted); margin-top: 4px; }

.appeal-history-item { display: flex; align-items: flex-start; gap: 10px; padding: 12px 0; border-bottom: 1px solid var(--border-light); }
.appeal-history-item:last-child { border-bottom: none; padding-bottom: 0; }
.appeal-history-item:first-child { padding-top: 0; }
.history-dot { width: 10px; height: 10px; border-radius: 50%; margin-top: 4px; flex-shrink: 0; }
.history-dot.penalty { background: var(--danger); }
.history-dot.appeal { background: var(--primary); }
.history-dot.review { background: var(--warning); }
.history-content { flex: 1; }
.history-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.history-desc { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
.history-time { font-size: 11px; color: var(--text-muted); margin-top: 4px; }

/* ---- Active bans card ---- */
.active-bans-card {
  padding: 20px; background: var(--warning-light); border-radius: var(--radius-sm);
  border: 1px solid #fde68a; border-left: 4px solid var(--warning);
}
.active-bans-card .panel-label { color: #b45309; }
.ban-status-header { margin-top: 12px; }
.ban-badge { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: 20px; font-size: 13px; font-weight: 600; }
.ban-badge.banned { background: var(--danger-light); color: #dc2626; }
.ban-badge.active { background: #d1fae5; color: #15803d; }
.active-bans-list { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.ban-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; background: var(--bg-white); border-radius: var(--radius-sm); }
.ban-row-left { display: flex; flex-direction: column; gap: 2px; }
.ban-perm-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.ban-reason-type { font-size: 12px; color: var(--text-muted); }
.ban-row-right { flex-shrink: 0; }
.ban-expiry { font-size: 12px; color: var(--text-secondary); }
.ban-expiry.permanent { color: #dc2626; font-weight: 600; }
.no-active-bans { font-size: 13px; color: var(--text-muted); margin-top: 12px; padding: 12px; text-align: center; }
</style>
