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

const showPenaltyModal = ref(false);
const selectedPenalty = ref<string>("");
const currentTicketId = ref<number | null>(null);
const actionNote = ref("");
const currentReviewAction = ref<"dismiss" | "accept" | null>(null);

const reportTypeBadgeClass = (type?: string) =>
  ({ SPAM: "badge-spam", HARASSMENT: "badge-harass", ILLEGAL: "badge-illegal" })[type || ""] || "badge-report";
const reportTypeLabel = (type?: string) =>
  ({ SPAM: "垃圾广告", HARASSMENT: "人身攻击", ILLEGAL: "违法违规", PORNOGRAPHY: "色情低俗" })[type || ""] || type || "其他举报";

const penaltyIcon = (p: string) => {
  const map: Record<string, string> = { delete: "fa-trash-alt", warn: "fa-exclamation-triangle", mute: "fa-microphone-slash", ban: "fa-ban" };
  return map[p] || "fa-gavel";
};
const penaltyColor = (p: string) => {
  const map: Record<string, string> = { delete: "#b45309", warn: "#b45309", mute: "#dc2626", ban: "#dc2626" };
  return map[p] || "var(--text-secondary)";
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params: any = { page: (pageOpts.value.currentPage || 1) - 1, size: pageOpts.value.pageSize, ticketTypes: "CONTENT_REPORT" };
    if (filterStatus.value) params.status = filterStatus.value;
    const res = await listTickets(params);
    ticketList.value = res.data.content || [];
    pageOpts.value.total = res.data.page?.totalElements ?? res.data.totalElements ?? 0;
  } catch {
    ElMessage.error("获取举报列表失败");
  } finally {
    loading.value = false;
  }
};

const handleReview = (id: number, action: "dismiss" | "accept") => {
  currentTicketId.value = id;
  currentReviewAction.value = action;
  if (action === "dismiss") {
    actionNote.value = "";
    showActionNote.value = true;
    showPenaltyModal.value = false;
  } else {
    showPenaltyModal.value = true;
    showActionNote.value = false;
    selectedPenalty.value = "";
  }
};

const showActionNote = ref(false);
const hideActionNote = () => { showActionNote.value = false; currentTicketId.value = null; actionNote.value = ""; };

const confirmDismiss = async () => {
  if (!currentTicketId.value) return;
  if (!actionNote.value.trim()) { ElMessage.warning("请填写驳回原因"); return; }
  try {
    await reviewTicket(currentTicketId.value, { action: "REJECT", auditNote: actionNote.value, replyContent: actionNote.value });
    ElMessage.success("举报已驳回");
    hideActionNote();
    await fetchData();
  } catch { ElMessage.error("操作失败"); }
};

const confirmPenalty = async () => {
  if (!currentTicketId.value || !selectedPenalty.value) { ElMessage.warning("请选择处罚措施"); return; }
  try {
    const opt = penaltyMap[selectedPenalty.value];
    await reviewTicket(currentTicketId.value, { action: "APPROVE", auditNote: `处罚: ${opt.label}`, penaltyType: opt.penaltyType as any, penaltyDurationHours: opt.hours != null ? String(opt.hours) : undefined });
    ElMessage.success("举报已采纳并执行处罚");
    showPenaltyModal.value = false;
    currentTicketId.value = null;
    selectedPenalty.value = "";
    await fetchData();
  } catch { ElMessage.error("操作失败"); }
};

const penaltyMap: Record<string, { penaltyType: string; label: string; desc: string; hours?: number }> = {
  delete: { penaltyType: "UNSPECIFIED", label: "仅删除内容", desc: "删除被举报的帖子/评论，不处罚用户" },
  warn: { penaltyType: "OTHER", label: "警告用户", desc: "发送系统警告，记录违规次数" },
  mute: { penaltyType: "BAN_COMMENT", label: "禁言 7 天", desc: "禁止发帖、评论，可浏览内容", hours: 168 },
  ban: { penaltyType: "BAN_LOGIN", label: "封号处理", desc: "永久封禁账号，不可解封" },
};
const penaltyOptions = Object.entries(penaltyMap).map(([key, val]) => ({ value: key, ...val }));

onMounted(() => fetchData());
</script>

<template>
  <div class="moderation-view">
    <div class="queue-info">
      <div class="queue-info-left">
        <div class="queue-stat"><i class="fa-solid fa-inbox"></i> 待处理 <strong>{{ ticketList.filter(t => t.status === 'PENDING').length }}</strong></div>
        <div class="queue-stat"><i class="fa-solid fa-check-circle"></i> 已采纳 <strong>{{ ticketList.filter(t => t.status === 'RESOLVED').length }}</strong></div>
        <div class="queue-stat"><i class="fa-solid fa-times-circle"></i> 已驳回 <strong>{{ ticketList.filter(t => t.status === 'REJECTED').length }}</strong></div>
      </div>
      <div class="queue-info-right">
        <button :class="['filter-btn', { active: filterStatus === 'PENDING' }]" @click="filterStatus = 'PENDING'; pageOpts.currentPage = 1; fetchData()">待处理</button>
        <button :class="['filter-btn', { active: filterStatus === 'RESOLVED' }]" @click="filterStatus = 'RESOLVED'; pageOpts.currentPage = 1; fetchData()">已采纳</button>
        <button :class="['filter-btn', { active: filterStatus === 'REJECTED' }]" @click="filterStatus = 'REJECTED'; pageOpts.currentPage = 1; fetchData()">已驳回</button>
        <button :class="['filter-btn', { active: filterStatus === '' }]" @click="filterStatus = ''; pageOpts.currentPage = 1; fetchData()">全部</button>
      </div>
    </div>

    <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>

    <template v-else>
      <div v-if="ticketList.length === 0" class="empty-box">暂无举报工单</div>

      <div v-for="item in ticketList" :key="item.ticketId ?? item.id" class="review-card">
        <div class="review-card-header">
          <div class="review-meta">
            <span class="review-type-badge badge-report"><i class="fa-solid fa-flag"></i> 举报</span>
            <span class="review-id">#RPT-{{ item.ticketId ?? item.id }}</span>
            <span class="review-time"><i class="fa-regular fa-clock"></i> {{ item.createdAt?.replace('T', ' ').substring(0, 16) || '-' }}</span>
          </div>
          <div class="review-status-bar">
            <span :class="['status-chip', statusChipClass(item.status, 'CONTENT_REPORT')]">
              <i class="fa-solid fa-hourglass-half"></i> {{ statusLabel(item.status, 'CONTENT_REPORT') }}
            </span>
          </div>
        </div>

        <div class="review-body">
          <div class="report-parties">
            <div class="party-card">
              <div class="party-label"><i class="fa-solid fa-user"></i> 举报人</div>
              <UserBrief :user="item.submitter" />
              <div v-if="item.submitter?.reportCount !== undefined" class="reporter-stats">
                <div class="reporter-stat"><div class="num">{{ item.submitter?.reportCount || 0 }}</div><div class="label">历史举报</div></div>
                <div class="reporter-stat"><div class="num">{{ item.submitter?.penaltyCount || 0 }}</div><div class="label">被采纳</div></div>
              </div>
            </div>
            <div class="party-card">
              <div class="party-label"><i class="fa-solid fa-bullseye"></i> 被举报人</div>
              <UserBrief :user="item.targetUser" :warn="true" />
            </div>
          </div>

          <div class="report-layout">
            <div class="reported-content-panel">
              <div class="panel-label"><i class="fa-solid fa-eye"></i> 被举报内容</div>
              <div class="content-snapshot">
                <span :class="['snapshot-type', snapshotTypeClass(item.targetType)]">
                  <i class="fa-solid fa-file-alt"></i> {{ snapshotTypeLabel(item.targetType) }}
                </span>
                <div class="snapshot-title">{{ item.content?.substring(0, 80) || '无内容' }}</div>
                <div class="snapshot-body"><p>{{ item.content || '(无内容)' }}</p></div>
              </div>

              <div class="report-detail-box">
                <div class="panel-label"><i class="fa-solid fa-flag"></i> 举报详情</div>
                <div class="report-reason"><i class="fa-solid fa-exclamation-circle"></i> {{ reportTypeLabel(item.banReasonType) || '内容举报' }}</div>
                <div class="report-desc">{{ item.content || '无详细描述' }}</div>
                <div v-if="(item.evidenceUrls ?? item.evidenceResourceUuids ?? item.attachments)?.length" class="report-evidence">
                  <div class="evidence-label">举报证据截图</div>
                  <div class="evidence-grid">
                    <img v-for="(att, i) in (item.evidenceUrls ?? item.evidenceResourceUuids ?? item.attachments ?? [])" :key="i" :src="att" alt="证据">
                  </div>
                </div>
              </div>
            </div>

            <div>
              <div class="info-card">
                <div class="info-card-title"><i class="fa-solid fa-info-circle"></i> 举报信息</div>
                <div class="info-item">
                  <div class="info-icon" style="background: var(--danger-light); color: var(--danger);"><i class="fa-solid fa-flag"></i></div>
                  <div class="info-detail">
                    <div class="info-label">举报类型</div>
                    <div class="info-value">{{ reportTypeLabel(item.banReasonType) || '内容举报' }}</div>
                  </div>
                </div>
                <div class="info-item">
                  <div class="info-icon" style="background: var(--warning-light); color: #b45309;"><i class="fa-solid fa-clock"></i></div>
                  <div class="info-detail">
                    <div class="info-label">举报时间</div>
                    <div class="info-value">{{ item.createdAt?.replace('T', ' ').substring(0, 19) || '-' }}</div>
                  </div>
                </div>
                <div class="info-item">
                  <div class="info-icon" style="background: var(--primary-light); color: var(--primary);"><i class="fa-solid fa-mobile-alt"></i></div>
                  <div class="info-detail">
                    <div class="info-label">举报来源</div>
                    <div class="info-value">{{ item.source || 'App端' }}</div>
                  </div>
                </div>
              </div>

              <div class="info-card" style="margin-top: 16px;">
                <div class="info-card-title"><i class="fa-solid fa-history"></i> 被举报人记录</div>
                <div class="info-item">
                  <div class="info-icon" style="background: var(--danger-light); color: var(--danger);"><i class="fa-solid fa-ban"></i></div>
                  <div class="info-detail">
                    <div class="info-label">历史被举报</div>
                    <div class="info-value">{{ item.targetUser?.reportCount || 0 }} 次</div>
                  </div>
                </div>
                <div class="info-item">
                  <div class="info-icon" style="background: var(--warning-light); color: #b45309;"><i class="fa-solid fa-gavel"></i></div>
                  <div class="info-detail">
                    <div class="info-label">历史处罚</div>
                    <div class="info-value">{{ item.targetUser?.penaltyCount || 0 }} 次</div>
                  </div>
                </div>
                <div v-if="item.currentBans" class="current-bans-section">
                  <div class="info-item">
                    <div class="info-icon" :style="{ background: item.currentBans.banned ? 'var(--danger-light)' : 'var(--success-light)', color: item.currentBans.banned ? '#dc2626' : '#15803d' }">
                      <i :class="item.currentBans.banned ? 'fa-solid fa-lock' : 'fa-solid fa-check-circle'"></i>
                    </div>
                    <div class="info-detail">
                      <div class="info-label">{{ item.currentBans.banned ? '当前已封禁' : '当前状态' }}</div>
                      <div class="info-value">
                        <span :class="['ban-status-badge', item.currentBans.banned ? 'banned' : 'normal']">
                          {{ item.currentBans.banned ? '已封禁' : '正常' }}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div v-if="item.currentBans.restrictions.length" class="active-restrictions">
                    <div v-for="(ban, i) in item.currentBans.restrictions" :key="i" class="restriction-row">
                      <span class="restriction-name">{{ ban.permissionName || ban.permissionCode }}</span>
                      <span v-if="ban.permanent" class="restriction-expiry permanent">永久</span>
                      <span v-else class="restriction-expiry">{{ ban.expiresAt?.replace('T', ' ').substring(0, 16) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="info-card" style="margin-top: 16px;">
                <div class="info-card-title"><i class="fa-solid fa-link"></i> 关联内容</div>
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
            <button class="action-btn action-btn-detail"><i class="fa-solid fa-history"></i> 举报历史</button>
            <button class="action-btn action-btn-detail"><i class="fa-solid fa-user"></i> 被举报人详情</button>
          </div>
          <div class="review-actions-right">
            <button class="action-btn action-btn-skip" @click="fetchData"><i class="fa-solid fa-forward"></i> 跳过</button>
            <button class="action-btn action-btn-reject" @click="handleReview(item.ticketId ?? item.id!, 'dismiss')"><i class="fa-solid fa-times"></i> 驳回举报</button>
            <button class="action-btn action-btn-pass" @click="handleReview(item.ticketId ?? item.id!, 'accept')"><i class="fa-solid fa-check"></i> 采纳举报</button>
          </div>
        </div>

        <div v-if="showActionNote && currentReviewAction === 'dismiss' && currentTicketId === (item.ticketId ?? item.id)" class="decision-note-wrap show danger-note">
          <label><i class="fa-solid fa-comment-alt"></i> 驳回原因（将通知举报人）</label>
          <textarea v-model="actionNote" placeholder="请填写驳回原因..."></textarea>
          <div class="decision-actions">
            <button class="action-btn action-btn-skip" @click="hideActionNote">取消</button>
            <button class="action-btn action-btn-pass" @click="confirmDismiss">确认驳回</button>
          </div>
        </div>
      </div>

      <div class="pagination-bar" v-if="pageOpts.total && pageOpts.total > (pageOpts.pageSize || 10)">
        <button class="btn" :disabled="(pageOpts.currentPage || 1) <= 1" @click="pageOpts.currentPage = (pageOpts.currentPage || 1) - 1; fetchData()">上一页</button>
        <span class="page-info">第 {{ pageOpts.currentPage }} / {{ Math.ceil((pageOpts.total || 0) / (pageOpts.pageSize || 10)) }} 页 (共 {{ pageOpts.total }} 条)</span>
        <button class="btn" :disabled="(pageOpts.currentPage || 1) >= Math.ceil((pageOpts.total || 0) / (pageOpts.pageSize || 10))" @click="pageOpts.currentPage = (pageOpts.currentPage || 1) + 1; fetchData()">下一页</button>
      </div>

      <Teleport to="body">
        <div v-if="showPenaltyModal" class="modal-overlay" @click.self="showPenaltyModal = false">
          <div class="modal">
            <div class="modal-header">
              <h3><i class="fa-solid fa-gavel" style="color: var(--danger);"></i> 选择处罚措施</h3>
              <p>举报已采纳，请对被举报人选择相应的处罚</p>
            </div>
            <div class="modal-body">
              <div class="penalty-options">
                <div v-for="opt in penaltyOptions" :key="opt.value"
                  :class="['penalty-option', { selected: selectedPenalty === opt.value }]"
                  @click="selectedPenalty = opt.value">
                  <div class="info-icon" :style="{ background: 'var(--warning-light)', color: penaltyColor(opt.value) }">
                    <i :class="['fa-solid', penaltyIcon(opt.value)]"></i>
                  </div>
                  <div class="penalty-info">
                    <div class="penalty-name">{{ opt.label }}</div>
                    <div class="penalty-desc">{{ opt.desc }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="action-btn action-btn-skip" @click="showPenaltyModal = false">取消</button>
              <button class="action-btn action-btn-reject" @click="confirmPenalty">确认处罚</button>
            </div>
          </div>
        </div>
      </Teleport>
    </template>
  </div>
</template>

<style scoped>
@import "@/assets/moderation.css";

/* ---- Report badge ---- */
.badge-report { background: var(--danger-light); color: #dc2626; }

/* ---- Report parties grid ---- */
.report-parties { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.party-card { padding: 16px; background: var(--bg); border-radius: var(--radius-sm); border: 1px solid var(--border-light); }
.party-label { font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 12px; display: flex; align-items: center; gap: 6px; }
.reporter-stats { display: flex; gap: 12px; margin-top: 10px; }
.reporter-stat { text-align: center; flex: 1; padding: 8px; background: var(--bg-white); border-radius: 8px; border: 1px solid var(--border-light); }
.reporter-stat .num { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.reporter-stat .label { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.report-layout { display: grid; grid-template-columns: 1fr 380px; gap: 24px; }
.reported-content-panel { display: flex; flex-direction: column; gap: 16px; }

/* Report-specific badges */
.badge-spam { background: #fce7f3; color: #be185d; }
.badge-harass { background: #e0e7ff; color: #4338ca; }
.badge-illegal { background: #fef3c7; color: #b45309; }

.snapshot-title { font-size: 16px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; line-height: 1.4; }

.report-detail-box { padding: 16px; background: var(--danger-light); border-radius: var(--radius-sm); border: 1px solid #fecaca; }
.report-detail-box .panel-label { color: var(--danger); }
.report-reason { font-size: 15px; font-weight: 600; color: var(--danger); margin: 8px 0; display: flex; align-items: center; gap: 8px; }
.report-desc { font-size: 14px; color: var(--text-secondary); line-height: 1.7; }
.report-evidence { margin-top: 12px; }
.evidence-label { font-size: 12px; font-weight: 600; color: var(--text-muted); margin-bottom: 8px; }
.evidence-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.evidence-grid img { width: 100%; height: 80px; object-fit: cover; border-radius: 6px; cursor: pointer; border: 1px solid #fecaca; }

/* Penalty modal */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 200; display: flex; align-items: center; justify-content: center; }
.modal { background: var(--bg-white); border-radius: var(--radius); width: 480px; max-width: 90%; box-shadow: 0 20px 60px rgba(0,0,0,0.2); overflow: hidden; }
.modal-header { padding: 20px 24px; border-bottom: 1px solid var(--border-light); }
.modal-header h3 { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.modal-header p { font-size: 13px; color: var(--text-muted); margin-top: 4px; }
.modal-body { padding: 20px 24px; }
.penalty-options { display: flex; flex-direction: column; gap: 8px; }
.penalty-option { display: flex; align-items: center; gap: 12px; padding: 14px 16px; border: 2px solid var(--border); border-radius: var(--radius-sm); cursor: pointer; transition: all 0.2s; }
.penalty-option:hover { border-color: var(--primary); background: var(--primary-light); }
.penalty-option.selected { border-color: var(--primary); background: var(--primary-light); }
.penalty-option .info-icon { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 14px; flex-shrink: 0; }
.modal-footer { padding: 16px 24px; border-top: 1px solid var(--border-light); display: flex; justify-content: flex-end; gap: 8px; }

/* Current ban status for target user */
.current-bans-section { border-top: 1px solid var(--border-light); margin-top: 12px; padding-top: 8px; }
.ban-status-badge { font-size: 12px; font-weight: 600; padding: 2px 10px; border-radius: 10px; display: inline-block; }
.ban-status-badge.banned { background: var(--danger-light); color: #dc2626; }
.ban-status-badge.normal { background: var(--success-light); color: #15803d; }
.active-restrictions { margin: 8px 0 0 12px; display: flex; flex-direction: column; gap: 4px; }
.restriction-row { display: flex; justify-content: space-between; align-items: center; font-size: 12px; padding: 4px 8px; background: var(--bg); border-radius: 6px; }
.restriction-name { color: var(--text-primary); font-weight: 500; }
.restriction-expiry { color: var(--text-muted); font-size: 11px; }
.restriction-expiry.permanent { color: var(--danger); font-weight: 600; }

/* Media queries */
@media (max-width: 1200px) {
  .report-parties, .report-layout { grid-template-columns: 1fr; }
}
</style>
