<script setup lang="ts">
import { ElMessage } from "element-plus";
import {
  listTickets,
  reviewTicket,
  type TicketResponse,
  type TicketReviewRequest,
  type TicketType,
} from "~/api/tickets";
import type { PageOptions } from "~/types/api";
import { statusLabel, statusChipClass, previewImage } from "~/composables/useModeration";
import UserBrief from "~/components/moderation/UserBrief.vue";

const loading = ref(false);
const ticketList = ref<TicketResponse[]>([]);
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 10, total: 0 });
const filterType = ref<TicketType | "">("");
const filterStatus = ref<string>("PENDING");

const typeOptions: { label: string; value: TicketType }[] = [
  { label: "建议", value: "SUGGESTION" },
  { label: "反馈", value: "FEATURE_FEEDBACK" },
  { label: "投诉", value: "CONTENT_REPORT" },
];

const typeBadgeClass = (t?: string) =>
  ({ SUGGESTION: "badge-suggestion", FEATURE_FEEDBACK: "badge-feedback", CONTENT_REPORT: "badge-complaint", BUG: "badge-bug" })[t || ""] || "badge-feedback";
const typeLabel = (t?: string) =>
  ({ SUGGESTION: "建议", FEATURE_FEEDBACK: "反馈", CONTENT_REPORT: "投诉", BUG: "Bug" })[t || ""] || t || "未知";
const typeIcon = (t?: string) =>
  ({ SUGGESTION: "fa-lightbulb", FEATURE_FEEDBACK: "fa-comment-dots", CONTENT_REPORT: "fa-angry", BUG: "fa-bug" })[t || ""] || "fa-comment";

const fetchData = async () => {
  loading.value = true;
  try {
    const params: any = { page: (pageOpts.value.currentPage || 1) - 1, size: pageOpts.value.pageSize };
    if (filterType.value) {
      params.ticketTypes = filterType.value;
    } else {
      params.ticketTypes = ["SUGGESTION", "FEATURE_FEEDBACK", "CONTENT_REPORT"].join(",");
    }
    if (filterStatus.value) params.status = filterStatus.value;
    const res = await listTickets(params);
    ticketList.value = res.data.content || [];
    pageOpts.value.total = res.data.page?.totalElements ?? res.data.totalElements ?? 0;
  } catch {
    ElMessage.error("获取反馈列表失败");
  } finally {
    loading.value = false;
  }
};

const expandedSet = ref(new Set<number>());
const toggleExpand = (idx: number) => {
  const s = expandedSet.value;
  if (s.has(idx)) s.delete(idx); else s.add(idx);
  expandedSet.value = new Set(s);
};

const handleRestore = async (id?: number) => {
  if (!id) return;
  try {
    await reviewTicket(id, { action: "REJECT", auditNote: "工单已恢复", replyContent: "工单已恢复" });
    ElMessage.success("工单已恢复");
    await fetchData();
  } catch { ElMessage.error("恢复失败"); }
};

const handleReply = async (id?: number) => {
  if (!id) return;
  const textarea = document.getElementById(`reply-${id}`) as HTMLTextAreaElement;
  const reply = textarea?.value?.trim();
  if (!reply) { ElMessage.warning("请输入回复内容"); return; }
  try {
    const data: TicketReviewRequest = { action: "APPROVE", auditNote: "已回复并归档", replyContent: reply };
    await reviewTicket(id, data);
    ElMessage.success("回复已发送，工单已完成");
    textarea.value = "";
    await fetchData();
  } catch { ElMessage.error("回复失败"); }
};

const handleMarkRead = async (id?: number) => {
  if (!id) return;
  try {
    await reviewTicket(id, { action: "APPROVE", auditNote: "标记已读，工单完成", replyContent: "标记已读，工单完成" });
    ElMessage.success("已标记为已读，工单已完成");
    await fetchData();
  } catch { ElMessage.error("操作失败"); }
};

const showToast = (msg: string, type?: string) => {
  ElMessage({ message: msg, type: (type as any) || "info" });
};

onMounted(() => fetchData());
</script>

<template>
  <div class="moderation-view">
    <div class="queue-info">
      <div class="queue-info-left">
        <div class="queue-stat"><i class="fa-solid fa-inbox"></i> 未处理 <strong>{{ ticketList.filter(t => t.status === 'PENDING').length }}</strong></div>
        <div class="queue-stat"><i class="fa-solid fa-check-circle"></i> 已解决 <strong>{{ ticketList.filter(t => t.status === 'RESOLVED').length }}</strong></div>
      </div>
      <div class="queue-info-right">
        <button :class="['filter-btn', { active: filterType === '' }]" @click="filterType = ''; pageOpts.currentPage = 1; fetchData()">全部</button>
        <button v-for="opt in typeOptions" :key="opt.value" :class="['filter-btn', { active: filterType === opt.value }]"
          @click="filterType = filterType === opt.value ? '' : opt.value; pageOpts.currentPage = 1; fetchData()">
          {{ opt.label }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>

    <template v-else>
      <div v-if="ticketList.length === 0" class="empty-box">暂无反馈工单</div>

      <div v-for="(item, idx) in ticketList" :key="item.ticketId ?? item.id" class="review-card">
        <div class="review-card-header fb-header" @click="toggleExpand(idx)">
          <div class="review-meta">
            <span :class="['review-type-badge', typeBadgeClass(item.ticketType)]">
              <i :class="['fa-solid', typeIcon(item.ticketType)]"></i> {{ typeLabel(item.ticketType) }}
            </span>
            <span class="review-id">#TK-{{ item.ticketId ?? item.id }}</span>
            <span class="review-time"><i class="fa-regular fa-clock"></i> {{ item.createdAt?.replace('T', ' ').substring(0, 16) || '-' }}</span>
          </div>
          <div class="review-status-bar">
            <span :class="['status-chip', statusChipClass(item.status, item.ticketType)]">
              <i class="fa-solid fa-circle" style="font-size: 6px;"></i> {{ statusLabel(item.status, item.ticketType) }}
            </span>
            <i :class="['fa-solid', 'fa-chevron-down', { rotated: expandedSet.has(idx) }]"></i>
          </div>
        </div>

        <div class="fb-preview" @click="toggleExpand(idx)" v-if="!expandedSet.has(idx)">
          {{ item.content || "(无内容)" }}
        </div>

        <div class="review-body" v-if="expandedSet.has(idx)">
          <div class="feedback-content">
            <div class="feedback-main">
              <div class="panel-label"><i class="fa-solid fa-align-left"></i> 详细内容</div>
              <div class="feedback-text-box">
                <p>{{ item.content || "(无内容)" }}</p>
              </div>
              <div v-if="item.attachments?.length" class="feedback-screenshots">
                <img v-for="(att, i) in item.attachments" :key="i" :src="att" :alt="'附件' + i" @click.stop="previewImage(att)">
              </div>
              <div v-else-if="item.evidenceResourceUuids?.length" class="feedback-screenshots">
                <div class="evidence-label">证据资源 ({{ item.evidenceResourceUuids.length }})</div>
              </div>
              <div class="reply-box">
                <label><i class="fa-solid fa-reply"></i> 回复用户</label>
                <textarea :id="'reply-' + (item.ticketId ?? item.id)" placeholder="输入回复内容..."></textarea>
                <div class="reply-actions">
                  <button class="action-btn action-btn-skip" @click.stop="handleMarkRead(item.ticketId ?? item.id)"><i class="fa-solid fa-check"></i> 标记已读</button>
                  <button class="action-btn action-btn-primary" @click.stop="handleReply(item.ticketId ?? item.id)"><i class="fa-solid fa-paper-plane"></i> 发送回复</button>
                </div>
              </div>
              <div class="ticket-actions">
                <button v-if="item.status === 'PENDING'"
                  class="action-btn action-btn-pass" @click.stop="handleMarkRead(item.ticketId ?? item.id)">
                  <i class="fa-solid fa-check-circle"></i> 完成工单
                </button>
                <button v-if="item.status === 'RESOLVED' || item.status === 'REJECTED'"
                  class="action-btn action-btn-warn" @click.stop="handleRestore(item.ticketId ?? item.id)">
                  <i class="fa-solid fa-undo"></i> 恢复工单
                </button>
              </div>
            </div>
            <div>
              <div class="info-card">
                <div class="info-card-title"><i class="fa-solid fa-user"></i> 提交用户</div>
                <UserBrief :user="item.submitter" size="sm" />
                <div class="info-item">
                  <div class="info-icon" style="background: var(--primary-light); color: var(--primary);"><i class="fa-solid fa-mobile-alt"></i></div>
                  <div class="info-detail">
                    <div class="info-label">来源</div>
                    <div class="info-value">{{ item.source || 'App端' }}</div>
                  </div>
                </div>
                <div class="info-item">
                  <div class="info-icon" style="background: var(--success-light); color: #15803d;"><i class="fa-solid fa-clock"></i></div>
                  <div class="info-detail">
                    <div class="info-label">提交时间</div>
                    <div class="info-value">{{ item.createdAt?.replace('T', ' ').substring(0, 16) || '-' }}</div>
                  </div>
                </div>
                <div class="info-item">
                  <div class="info-icon" style="background: var(--warning-light); color: #b45309;"><i class="fa-solid fa-tag"></i></div>
                  <div class="info-detail">
                    <div class="info-label">工单类型</div>
                    <div class="info-value">{{ typeLabel(item.ticketType) }}</div>
                  </div>
                </div>
                <div v-if="item.submitter?.reportCount !== undefined" class="info-item">
                  <div class="info-icon" style="background: var(--danger-light); color: var(--danger);"><i class="fa-solid fa-history"></i></div>
                  <div class="info-detail">
                    <div class="info-label">历史反馈</div>
                    <div class="info-value">{{ item.submitter.reportCount }} 次</div>
                  </div>
                </div>
              </div>
              <div class="info-card" style="margin-top: 12px;">
                <div class="info-card-title"><i class="fa-solid fa-tags"></i> 快捷标签</div>
                <div class="quick-actions">
                  <span class="quick-tag" @click.stop="showToast('已记录', 'info')">已记录</span>
                  <span class="quick-tag" @click.stop="showToast('产品需求', 'info')">产品需求</span>
                  <span class="quick-tag" @click.stop="showToast('高优先级', 'info')">高优先级</span>
                  <span class="quick-tag" @click.stop="showToast('转工单', 'info')">转工单</span>
                  <span class="quick-tag" @click.stop="showToast('重复反馈', 'info')">重复反馈</span>
                </div>
              </div>
            </div>
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

/* ---- Feedback badge variants ---- */
.badge-suggestion { background: var(--primary-light); color: var(--primary); }
.badge-feedback { background: var(--success-light); color: #15803d; }
.badge-complaint { background: var(--danger-light); color: #dc2626; }
.badge-bug { background: var(--warning-light); color: #b45309; }
.status-new { background: var(--primary-light); color: var(--primary); }

/* ---- Feedback button variants ---- */
.action-btn-warn { background: var(--warning); color: white; }
.action-btn-warn:hover { background: #d97706; box-shadow: 0 4px 12px rgba(245, 158, 11, 0.35); }
.action-btn-primary { background: var(--primary); color: white; }
.action-btn-primary:hover { background: var(--primary-hover); box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35); }

/* ---- Feedback layout ---- */
.feedback-content { display: grid; grid-template-columns: 1fr 340px; gap: 20px; }
.feedback-main { display: flex; flex-direction: column; gap: 16px; }
@media (max-width: 1200px) { .feedback-content { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .feedback-content { grid-template-columns: 1fr; } }

/* ---- Feedback-specific components ---- */
.fb-header { cursor: pointer; }
.fb-header .fa-chevron-down { color: var(--text-muted); transition: transform 0.2s; }
.fb-header .fa-chevron-down.rotated { transform: rotate(180deg); }

.fb-preview {
  padding: 12px 20px; font-size: 14px; color: var(--text-secondary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; cursor: pointer;
}
.fb-preview:hover { color: var(--text-primary); }

.feedback-text-box {
  padding: 16px; background: var(--bg); border-radius: var(--radius-sm);
  font-size: 14px; line-height: 1.8; color: var(--text-secondary);
  border-left: 4px solid var(--primary);
}
.feedback-text-box p { margin-bottom: 8px; }
.feedback-text-box p:last-child { margin-bottom: 0; }

.feedback-screenshots { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.feedback-screenshots img {
  width: 100%; height: 100px; object-fit: cover;
  border-radius: 6px; cursor: pointer; border: 1px solid var(--border);
}

.reply-box {
  padding: 16px; background: var(--primary-light); border-radius: var(--radius-sm); border: 1px solid #bfdbfe;
}
.reply-box label { display: block; font-size: 12px; font-weight: 600; color: var(--primary); margin-bottom: 8px; }
.reply-box textarea {
  width: 100%; padding: 10px 14px; border: 1px solid #bfdbfe;
  border-radius: var(--radius-sm); font-size: 14px; font-family: inherit;
  resize: vertical; min-height: 80px; outline: none; margin-bottom: 10px;
}
.reply-box textarea:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }
.reply-actions { display: flex; justify-content: flex-end; gap: 8px; }

.ticket-actions { display: flex; justify-content: flex-end; gap: 8px; }

.quick-actions { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.quick-tag { padding: 4px 10px; background: var(--bg-white); border: 1px solid var(--border); border-radius: 6px; font-size: 12px; color: var(--text-secondary); cursor: pointer; transition: all 0.2s; }
.quick-tag:hover { border-color: var(--primary); color: var(--primary); background: var(--primary-light); }

@media (max-width: 1200px) {
  .feedback-screenshots { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 768px) {
  .feedback-screenshots { grid-template-columns: repeat(2, 1fr); }
}
</style>
