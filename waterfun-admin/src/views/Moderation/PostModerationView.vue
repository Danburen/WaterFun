<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox, ElMessage } from "element-plus";
import MarkdownIt from 'markdown-it'
import {
  listPostModerations,
  approveModerationById,
  rejectModerationById,
  getModerationStats,
  type AuditResponsePostAuditPayload,
  type AuditStatus,
  type ModerateRejectType,
  type ModerationStatsResp,
} from "~/api/moderation";
import type { PageOptions } from "~/types/api";
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { getTagOptions } from "~/api/tag";
import BaseDialog from "~/components/BaseDialog.vue";

const md = new MarkdownIt({ html: true, breaks: true, linkify: true });

const loading = ref(false);
const statsLoading = ref(false);
const taskData = ref<AuditResponsePostAuditPayload[]>([]);
const statsData = ref<ModerationStatsResp>({});

const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 10, total: 0 });
const filterStatus = ref<AuditStatus | "">("PENDING");

const rejectDialogVisible = ref(false);
const rejectSubmitting = ref(false);
const currentRejectTaskId = ref<string | null>(null);
const rejectType = ref<ModerateRejectType>("OTHER");
const rejectReason = ref("");

const tagOptions = ref<OptionResItem<number>[]>([]);
const tagNameMap = computed(() => {
  const m = new Map<number, string>();
  tagOptions.value.forEach(i => m.set(i.id, i.name));
  return m;
});

const renderedContent = (task: AuditResponsePostAuditPayload): string => {
  const raw = task.payload?.contentHtml || task.payload?.content || task.payload?.summary || '';
  if (!raw) return '(无内容)';
  return md.render(raw);
};

const rejectTypeOptions = [
  { label: "违反社区准则", value: "VIOLATION_OF_GUIDELINES" },
  { label: "不当内容", value: "INAPPROPRIATE_CONTENT" },
  { label: "广告", value: "ADVERTISEMENT" },
  { label: "暴力内容", value: "VIOLENCE" },
  { label: "敏感内容", value: "SENSITIVE" },
  { label: "其他", value: "OTHER" },
];

const fetchStats = async () => {
  statsLoading.value = true;
  try {
    const res = await getModerationStats("POST");
    statsData.value = res.data;
  } catch { /* silent */ }
  finally { statsLoading.value = false; }
};

const fetchData = async () => {
  loading.value = true;
  try {
    if (!tagOptions.value.length) {
      const tr = await getTagOptions();
      tagOptions.value = tr.data || [];
    }
    const query: any = {};
    if (filterStatus.value) query.status = filterStatus.value;
    const res = await listPostModerations(query, pageOpts.value.currentPage || 1, pageOpts.value.pageSize);
    taskData.value = res.data.content || [];
    pageOpts.value.total = typeof res.data.totalElements === "number" ? res.data.totalElements
      : (res.data as any)?.page?.totalElements || 0;
  } catch { ElMessage.error("获取帖子审核列表失败"); }
  finally { loading.value = false; }
};

const handleApprove = async (taskId?: string) => {
  if (!taskId) return;
  try {
    await ElMessageBox.confirm("确定通过该帖子审核吗？", "通过", { type: "warning" });
    await approveModerationById(taskId);
    ElMessage.success("审核通过成功");
    await fetchData();
    await fetchStats();
  } catch (e) {
    if (e !== "cancel") ElMessage.error("审核通过失败");
  }
};

const openRejectDialog = (taskId?: string) => {
  if (!taskId) return;
  currentRejectTaskId.value = taskId;
  rejectType.value = "OTHER";
  rejectReason.value = "";
  rejectDialogVisible.value = true;
};

const submitReject = async () => {
  if (!currentRejectTaskId.value) return;
  rejectSubmitting.value = true;
  try {
    await rejectModerationById(String(currentRejectTaskId.value), {
      rejectType: rejectType.value,
      rejectReason: rejectReason.value || undefined,
    });
    ElMessage.success("审核驳回成功");
    rejectDialogVisible.value = false;
    await fetchData();
    await fetchStats();
  } catch { ElMessage.error("审核驳回失败"); }
  finally { rejectSubmitting.value = false; }
};

const riskLevelLabel = (r?: string): string =>
  ({ LOW: "低风险", MEDIUM: "中风险", HIGH: "高风险" })[r || ""] || "-";

const riskLevelClass = (r?: string): string =>
  ({ LOW: "risk-low", MEDIUM: "risk-medium", HIGH: "risk-high" })[r || ""] || "";

const statusLabel = (s?: string): string =>
  ({ PENDING: "待审核", APPROVED: "已通过", REJECTED: "已驳回", SUSPECT: "可疑" })[s || ""] || s || "未知";

const statusTagClass = (s?: string): string =>
  ({ PENDING: "badge-yellow", APPROVED: "badge-green", REJECTED: "badge-red", SUSPECT: "badge-blue" })[s || ""] || "badge-gray";

const priorityLabel = (p?: string): string =>
  ({ EMERGENCY: "紧急", HIGH: "高", MEDIUM: "中", LOW: "低" })[p || ""] || p || "-";

const priorityClass = (p?: string): string =>
  ({ EMERGENCY: "risk-high", HIGH: "risk-high", MEDIUM: "risk-medium", LOW: "risk-low" })[p || ""] || "";

onMounted(async () => {
  await fetchStats();
  await fetchData();
});
</script>

<template>
  <div class="list-layout">
    <div class="queue-info">
      <div class="queue-info-left">
        <div class="queue-stat"><i class="fa-solid fa-inbox"></i> 待审核 <strong>{{ statsData.pending ?? "-" }}</strong></div>
        <div class="queue-stat"><i class="fa-solid fa-check-circle"></i> 今日已通过 <strong>{{ statsData.todayApproved ?? "-" }}</strong></div>
        <div class="queue-stat"><i class="fa-solid fa-times-circle"></i> 今日已拒绝 <strong>{{ statsData.todayRejected ?? "-" }}</strong></div>
      </div>
      <div class="queue-info-right">
        <button :class="['filter-btn', { active: filterStatus === '' }]" @click="filterStatus = ''; pageOpts.currentPage = 1; fetchData()">全部</button>
        <button :class="['filter-btn', { active: filterStatus === 'PENDING' }]" @click="filterStatus = 'PENDING'; pageOpts.currentPage = 1; fetchData()">待审核</button>
        <button :class="['filter-btn', { active: filterStatus === 'APPROVED' }]" @click="filterStatus = 'APPROVED'; pageOpts.currentPage = 1; fetchData()">已通过</button>
        <button :class="['filter-btn', { active: filterStatus === 'REJECTED' }]" @click="filterStatus = 'REJECTED'; pageOpts.currentPage = 1; fetchData()">已驳回</button>
      </div>
    </div>

    <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>

    <template v-else>
      <div v-if="taskData.length === 0" class="empty-box">暂无帖子审核任务</div>

      <div v-for="task in taskData" :key="task.taskId" class="review-card">
        <div class="review-card-header">
          <div class="review-meta">
            <span class="review-type-badge badge-post"><i class="fa-solid fa-file-alt"></i> 帖子审核</span>
            <span class="review-id">#{{ task.taskId }}</span>
            <span class="review-time"><i class="fa-regular fa-clock"></i> {{ task.triggerSource || "系统" }}</span>
          </div>
          <div class="review-status-bar">
            <span v-if="task.priority" :class="['author-risk', priorityClass(task.priority)]">{{ priorityLabel(task.priority) }}</span>
            <span :class="['status-chip', statusTagClass(task.status)]">
              <i class="fa-solid" :class="task.status === 'APPROVED' ? 'fa-check-circle' : task.status === 'REJECTED' ? 'fa-times-circle' : 'fa-hourglass-half'"></i> {{ statusLabel(task.status) }}
            </span>
          </div>
        </div>

        <div class="review-body">
          <div class="author-info">
            <img v-if="task.submitter?.avatar?.url" :src="task.submitter.avatar.url" alt="avatar" class="author-avatar">
            <div v-else class="author-avatar-placeholder"><i class="fa-solid fa-user"></i></div>
            <div class="author-details">
              <div class="author-name">{{ task.submitter?.displayName || "提交人 #" + (task.submitter?.uid ?? "?") }}</div>
              <div class="author-meta">UID: {{ task.submitter?.uid ?? "?" }} · LV.{{ task.submitter?.level ?? "?" }} · 发帖 {{ task.submitter?.postCount ?? "?" }} · 注册 {{ task.submitter?.registrationDate ? formatISOData(task.submitter.registrationDate) : "?" }} · 触发: {{ task.triggerType || "-" }}</div>
            </div>
            <span v-if="task.submitter?.riskLevel" :class="['author-risk', riskLevelClass(task.submitter.riskLevel)]"><i class="fa-solid fa-shield-alt"></i> {{ riskLevelLabel(task.submitter.riskLevel) }}</span>
          </div>

          <div class="post-review-layout" v-if="task.payload">
            <div class="post-content-panel">
              <div class="panel-label"><i class="fa-solid fa-font"></i> 文本内容</div>
              <div class="post-title-display">{{ task.payload.title || "(无标题)" }}</div>
              <div class="post-content-display" v-html="renderedContent(task)"></div>
                <div v-if="task.payload.categoryId || (task.payload.tagIds?.length)" class="post-tags">
                  <span v-if="task.payload.categoryId" class="badge badge-blue">分类: {{ task.payload.categoryId }}</span>
                  <span v-for="tid in task.payload.tagIds" :key="tid" class="badge badge-green">标签: {{ tagNameMap.get(tid) || tid }}</span>
                  <span v-for="tname in task.payload.newTagNames" :key="tname" class="badge badge-yellow">新标签: {{ tname }}</span>
                </div>
            </div>

            <div class="image-panel" v-if="task.linkedResources?.length">
              <div class="panel-label"><i class="fa-solid fa-image"></i> 关联图片 ({{ task.linkedResources.length }}张)</div>
              <div class="image-grid">
                <div v-for="res in task.linkedResources" :key="res.resourceUuid" class="image-review-item">
                  <img v-if="res.presignedUrl?.url" :src="res.presignedUrl.url" :alt="res.resourceUuid || ''">
                  <div v-else class="image-placeholder"><i class="fa-solid fa-image"></i></div>
                  <div class="image-overlay">
                    <span>{{ res.resourceUuid?.substring(0, 12) || "未知" }}</span>
                    <span :class="['image-status', 'image-status-' + (res.status === 'APPROVED' ? 'passed' : res.status === 'REJECTED' ? 'rejected' : 'pending')]">
                      {{ statusLabel(res.status) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="source-card" v-if="task.sourceContext?.sourcePostBrief">
            <div class="source-card-title"><i class="fa-solid fa-link"></i> 来源信息</div>
            <div class="source-item" v-if="task.sourceContext.sourcePostBrief.title">
              <div class="source-icon" style="background: var(--primary-light); color: var(--primary);"><i class="fa-solid fa-file-alt"></i></div>
              <div class="source-info">
                <div class="source-name">{{ task.sourceContext.sourcePostBrief.title }}</div>
                <div class="source-meta">作者: {{ task.sourceContext.sourcePostBrief.author?.displayName || "未知" }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="review-actions">
          <div class="review-actions-left">
            <span class="review-id">{{ task.submitter?.displayName || "提交人 #" + (task.submitter?.uid ?? "?") }}</span>
          </div>
          <div class="review-actions-right">
            <button class="action-btn action-btn-skip" @click="fetchData"><i class="fa-solid fa-forward"></i> 刷新</button>
            <button class="action-btn action-btn-reject" @click="openRejectDialog(task.taskId)"><i class="fa-solid fa-times"></i> 不通过</button>
            <button class="action-btn action-btn-pass" @click="handleApprove(task.taskId)"><i class="fa-solid fa-check"></i> 通过</button>
          </div>
        </div>
      </div>

      <div class="pagination-bar" v-if="pageOpts.total && pageOpts.total > (pageOpts.pageSize || 10)">
        <button class="btn" :disabled="(pageOpts.currentPage || 1) <= 1" @click="pageOpts.currentPage = (pageOpts.currentPage || 1) - 1; fetchData()">上一页</button>
        <span class="page-info">第 {{ pageOpts.currentPage }} / {{ Math.ceil((pageOpts.total || 0) / (pageOpts.pageSize || 10)) }} 页 (共 {{ pageOpts.total }} 条)</span>
        <button class="btn" :disabled="(pageOpts.currentPage || 1) >= Math.ceil((pageOpts.total || 0) / (pageOpts.pageSize || 10))" @click="pageOpts.currentPage = (pageOpts.currentPage || 1) + 1; fetchData()">下一页</button>
      </div>
    </template>

    <BaseDialog v-model="rejectDialogVisible" title="驳回帖子审核" width="520px">
      <div class="form-block">
        <div class="form-field"><label class="form-label">驳回类型</label><div class="form-content"><select v-model="rejectType" class="form-select"><option v-for="item in rejectTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></div></div>
        <div class="form-field"><label class="form-label">驳回原因</label><div class="form-content"><textarea v-model="rejectReason" class="form-textarea" rows="4" placeholder="请输入驳回原因（可选）"></textarea></div></div>
      </div>
      <template #footer>
        <button class="btn" @click="rejectDialogVisible = false">取消</button>
        <button class="btn btn-primary" :disabled="rejectSubmitting" @click="submitReject"><i v-if="rejectSubmitting" class="fa-solid fa-spinner fa-spin"></i> 保存</button>
      </template>
    </BaseDialog>
  </div>
</template>

<style scoped>
.queue-info {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px; padding: 12px 20px;
  background: var(--bg-white); border: 1px solid var(--border); border-radius: var(--radius-sm);
}
.queue-info-left { display: flex; align-items: center; gap: 20px; }
.queue-stat { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text-secondary); }
.queue-stat strong { color: var(--text-primary); font-size: 16px; }
.queue-info-right { display: flex; align-items: center; gap: 8px; }
.review-card {
  background: var(--bg-white); border: 1px solid var(--border);
  border-radius: var(--radius); overflow: hidden;
  box-shadow: var(--shadow); margin-bottom: 24px;
}
.review-card-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 24px; border-bottom: 1px solid var(--border-light); background: var(--bg);
}
.review-meta { display: flex; align-items: center; gap: 16px; }
.review-type-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600;
}
.badge-post { background: var(--primary-light); color: var(--primary); }
.review-id { font-size: 13px; color: var(--text-muted); font-family: 'SF Mono', monospace; }
.review-time { font-size: 13px; color: var(--text-muted); }
.review-status-bar { display: flex; align-items: center; gap: 8px; }
/* status-chip, status-pending/passed/rejected in global.css */

.review-body { padding: 24px; }

.author-info {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid var(--border-light);
}
.author-avatar { width: 44px; height: 44px; border-radius: 50%; object-fit: cover; border: 2px solid var(--border); }
.author-avatar-placeholder {
  width: 44px; height: 44px; border-radius: 50%;
  background: var(--bg); border: 2px solid var(--border);
  display: flex; align-items: center; justify-content: center; color: var(--text-muted);
}
.author-details { flex: 1; }
.author-name { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.author-meta { font-size: 13px; color: var(--text-muted); margin-top: 2px; }
.author-risk { display: flex; align-items: center; gap: 6px; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; }
/* risk-low/medium/high in global.css */

.post-review-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
.post-content-panel { display: flex; flex-direction: column; gap: 16px; }
.panel-label { font-size: 12px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; display: flex; align-items: center; gap: 6px; }
.panel-label i { font-size: 11px; }
.post-title-display {
  font-size: 20px; font-weight: 700; color: var(--text-primary); line-height: 1.4;
  padding: 16px; background: var(--bg); border-radius: var(--radius-sm); border-left: 4px solid var(--primary);
}
.post-content-display {
  padding: 20px; background: var(--bg); border-radius: var(--radius-sm);
  font-size: 15px; line-height: 1.8; color: var(--text-secondary);
  min-height: 120px; max-height: 300px; overflow-y: auto;
}
.post-content-display :deep(img) { max-width: 100%; border-radius: 6px; margin: 8px 0; }
.post-content-display :deep(p) { margin: 0 0 8px; }
.post-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
.post-tags .badge { font-size: 11px; }

.image-panel { display: flex; flex-direction: column; gap: 16px; }
.image-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.image-review-item {
  position: relative; border-radius: var(--radius-sm); overflow: hidden;
  border: 2px solid var(--border); cursor: pointer; transition: all 0.2s ease;
}
.image-review-item:hover { border-color: var(--primary); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.image-review-item img { width: 100%; height: 140px; object-fit: cover; display: block; }
.image-placeholder { width: 100%; height: 140px; display: flex; align-items: center; justify-content: center; background: var(--bg); color: var(--text-muted); }
.image-review-item .image-overlay {
  position: absolute; bottom: 0; left: 0; right: 0;
  padding: 8px 12px; background: linear-gradient(transparent, rgba(0,0,0,0.7));
  color: white; font-size: 12px; display: flex; justify-content: space-between; align-items: center;
}
.image-review-item .image-status { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.image-status-pending { background: var(--warning); }
.image-status-passed { background: var(--success); }
.image-status-rejected { background: var(--danger); }

.source-card {
  margin-top: 20px; padding: 16px; background: var(--bg);
  border-radius: var(--radius-sm); border: 1px solid var(--border-light);
}
.source-card-title { font-size: 12px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 12px; display: flex; align-items: center; gap: 6px; }
.source-card-title i { font-size: 11px; }
.source-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid var(--border-light); }
.source-item:last-child { border-bottom: none; padding-bottom: 0; }
.source-item:first-child { padding-top: 0; }
.source-icon { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 14px; flex-shrink: 0; }
.source-info { flex: 1; min-width: 0; }
.source-name { font-size: 14px; font-weight: 500; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.source-meta { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

.review-actions {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 24px; background: var(--bg); border-top: 1px solid var(--border-light); gap: 12px;
}
.review-actions-left { display: flex; align-items: center; gap: 8px; }
.review-actions-right { display: flex; align-items: center; gap: 12px; }
.action-btn {
  padding: 10px 24px; border-radius: var(--radius-sm); font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all 0.2s ease; border: none;
  display: inline-flex; align-items: center; gap: 8px;
}
.action-btn-pass { background: var(--success); color: white; }
.action-btn-pass:hover { background: #059669; box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35); transform: translateY(-1px); }
.action-btn-reject { background: var(--danger); color: white; }
.action-btn-reject:hover { background: #dc2626; box-shadow: 0 4px 12px rgba(239, 68, 68, 0.35); transform: translateY(-1px); }
.action-btn-skip { background: var(--bg-white); color: var(--text-secondary); border: 1px solid var(--border); }

/* pagination-bar, page-info in global.css */

@media (max-width: 1200px) {
  .post-review-layout { grid-template-columns: 1fr; }
  .image-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 768px) {
  .review-actions { flex-direction: column; }
  .review-actions-left, .review-actions-right { width: 100%; justify-content: center; }
  .image-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
