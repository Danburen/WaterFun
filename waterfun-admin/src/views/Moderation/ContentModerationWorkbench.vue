<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox, ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { User, Clock, Files } from "@element-plus/icons-vue";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { listModerations, approveModerationById, rejectModerationById, approveModerations, rejectModerations, type ModerateTaskResp, type TargetType, type ModerateRejectType, type AuditStatus } from "~/api/moderation";
import { useDictStore } from "~/stores/dictStore";
import type { PageOptions } from "~/types/api";
import BaseDialog from "~/components/BaseDialog.vue";

const router = useRouter();
const dictStore = useDictStore();

const loading = ref(false);
const taskData = ref<ModerateTaskResp[]>([]);

const searchForm = ref<{ taskType: TargetType | ""; submitterId: string; submitAtStart: string; submitAtEnd: string }>({ taskType: "", submitterId: "", submitAtStart: "", submitAtEnd: "" });
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 12, total: 0 });
const selectedIds = ref<Set<string>>(new Set());

const targetTypeOptions = [{ label: "帖子", value: "POST" }, { label: "用户头像", value: "USER_AVATAR" }, { label: "帖子封面图", value: "POST_COVERAGE_IMAGE" }, { label: "帖子内容图", value: "POST_CONTENT_IMAGE" }, { label: "轮播图", value: "BANNER_IMAGE" }, { label: "帖子内容", value: "POST_CONTENT" }, { label: "未知", value: "UNKNOWN" }];
const rejectTypeOptions = [{ label: "违反社区准则", value: "VIOLATION_OF_GUIDELINES" }, { label: "不当内容", value: "INAPPROPRIATE_CONTENT" }, { label: "广告", value: "ADVERTISEMENT" }, { label: "暴力内容", value: "VIOLENCE" }, { label: "敏感内容", value: "SENSITIVE" }, { label: "级联驳回", value: "CASCADE" }, { label: "其他", value: "OTHER" }];

const rejectDialogVisible = ref(false);
const rejectBatchMode = ref(false);
const rejectSubmitting = ref(false);
const currentRejectTaskId = ref<string | null>(null);
const rejectType = ref<ModerateRejectType>("OTHER");
const rejectReason = ref("");

const pageTotal = (payload: Record<string, unknown>): number =>
  typeof payload.totalElements === "number" ? payload.totalElements
  : typeof (payload as any).page?.totalElements === "number" ? (payload as any).page.totalElements as number
  : 0;

const statusTagClass = (status?: AuditStatus): string => {
  if (status === "PENDING") return "badge-yellow";
  if (status === "APPROVED") return "badge-green";
  if (status === "REJECTED") return "badge-red";
  return "badge-gray";
};

const parsePostMeta = (meta?: Record<string, unknown>): { title?: string; summary?: string } | null => meta ? meta as { title?: string; summary?: string } : null;

const getPostTitle = (task: ModerateTaskResp): string => {
  const payload = task.payload;
  if (!payload) return "审核任务 #" + (task.id ?? "?");
  const meta = parsePostMeta(payload.meta);
  if (meta?.title) return meta.title;
  if (payload.type === "RICH_TEXT" || payload.type === "PLAIN_TEXT") return payload.content ? payload.content.substring(0, 60) : "[内容审核]";
  if (payload.type === "SINGLE_RESOURCE") return "[资源审核] " + ((payload.resources?.[0] as any)?.resourceUuid?.substring(0, 16) ?? "");
  return "审核任务 #" + (task.id ?? "?");
};

const getContentPreview = (task: ModerateTaskResp): string => {
  const payload = task.payload;
  if (!payload) return "";
  if (payload.type === "RICH_TEXT" || payload.type === "PLAIN_TEXT") { const meta = parsePostMeta(payload.meta); if (meta?.summary) return meta.summary; if (payload.content) return payload.content.substring(0, 120); return ""; }
  return "";
};

const getResourceCount = (task: ModerateTaskResp): number => task.payload?.resources?.length ?? 0;

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listModerations({ page: (pageOpts.value.currentPage || 1) - 1, size: pageOpts.value.pageSize, taskType: searchForm.value.taskType || undefined, submitterId: searchForm.value.submitterId || undefined, submitAtStart: searchForm.value.submitAtStart ? new Date(searchForm.value.submitAtStart).toISOString() : undefined, submitAtEnd: searchForm.value.submitAtEnd ? new Date(searchForm.value.submitAtEnd).toISOString() : undefined });
    taskData.value = res.data.content || [];
    taskData.value.sort((a, b) => { const aP = a.payload?.type ? 0 : 1; const bP = b.payload?.type ? 0 : 1; return aP - bP; });
    pageOpts.value.total = pageTotal(res.data as unknown as Record<string, unknown>);
  } catch { ElMessage.error("获取审核任务失败"); }
  finally { loading.value = false; }
};

const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleReset = () => { pageOpts.value.currentPage = 1; searchForm.value = { taskType: "", submitterId: "", submitAtStart: "", submitAtEnd: "" }; fetchData(); };
const toggleSelection = (id: string) => { if (selectedIds.value.has(id)) selectedIds.value.delete(id); else selectedIds.value.add(id); };
const selectAll = computed(() => taskData.value.length > 0 && taskData.value.every(t => t.id && selectedIds.value.has(t.id)));
const toggleAll = () => { if (selectAll.value) { selectedIds.value.clear(); } else { taskData.value.forEach(t => { if (t.id) selectedIds.value.add(t.id); }); } };

const gotoDetail = (task: ModerateTaskResp) => { if (task.id) router.push({ name: "moderationTaskDetail", params: { taskId: task.id } }); };

const handleApproveTask = async (taskId?: string) => {
  if (!taskId) return;
  try { await ElMessageBox.confirm("确定通过该审核任务吗？", "通过", { type: "warning" }); await approveModerationById(taskId); ElMessage.success("审核通过成功"); await fetchData(); }
  catch (e) { if (e !== "cancel") { ElMessage.error("审核通过失败"); } }
};

const handleBatchApprove = async () => {
  if (selectedIds.value.size === 0) { ElMessage.warning("请先选择要审核的任务"); return; }
  try { await ElMessageBox.confirm(`确定批量通过 ${selectedIds.value.size} 个审核任务吗？`, "批量通过", { type: "warning" }); await approveModerations({ auditTaskIds: [...selectedIds.value] }); ElMessage.success("批量审核通过成功"); selectedIds.value.clear(); await fetchData(); }
  catch (e) { if (e !== "cancel") { ElMessage.error("批量审核通过失败"); } }
};

const openRejectDialog = (taskId?: string) => { rejectBatchMode.value = false; currentRejectTaskId.value = taskId ?? null; rejectType.value = "OTHER"; rejectReason.value = ""; rejectDialogVisible.value = true; };
const openBatchRejectDialog = () => { if (selectedIds.value.size === 0) { ElMessage.warning("请先选择要驳回的任务"); return; } rejectBatchMode.value = true; currentRejectTaskId.value = null; rejectType.value = "OTHER"; rejectReason.value = ""; rejectDialogVisible.value = true; };

const submitReject = async () => {
  rejectSubmitting.value = true;
  try {
    const payload = { rejectType: rejectType.value, rejectReason: rejectReason.value || undefined };
    if (rejectBatchMode.value) { const res = await rejectModerations({ auditTaskIds: [...selectedIds.value], ...payload }); ElMessage.success(res.data ? `批量驳回成功: ${res.data.success}, 失败: ${res.data.failed}` : "批量驳回成功"); selectedIds.value.clear(); }
    else { if (!currentRejectTaskId.value) return; await rejectModerationById(currentRejectTaskId.value, payload); ElMessage.success("审核驳回成功"); }
    rejectDialogVisible.value = false; await fetchData();
  } catch { ElMessage.error("审核驳回失败"); }
  finally { rejectSubmitting.value = false; }
};

const targetTypeLabel = (type?: TargetType): string => ({ POST: "帖子", USER_AVATAR: "用户头像", POST_COVERAGE_IMAGE: "帖子封面图", POST_CONTENT_IMAGE: "帖子内容图", BANNER_IMAGE: "轮播图", POST_CONTENT: "帖子内容", UNKNOWN: "未知" })[type || ""] || type || "未知";

onMounted(async () => { dictStore.ensureLoaded(); fetchData(); });
</script>

<template>
  <div class="list-layout">
    <SearchContainer>
      <div class="search-form">
        <div class="search-field"><label>任务类型</label><select v-model="searchForm.taskType" class="form-select" style="max-width: 160px;"><option value="">全部</option><option v-for="item in targetTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></div>
        <div class="search-field"><label>提交人ID</label><input v-model="searchForm.submitterId" class="form-input" placeholder="请输入提交人ID" style="max-width: 180px;" /></div>
        <div class="search-field"><label>提交时间起</label><input v-model="searchForm.submitAtStart" type="datetime-local" class="form-input" style="max-width: 200px;" /></div>
        <div class="search-field"><label>提交时间止</label><input v-model="searchForm.submitAtEnd" type="datetime-local" class="form-input" style="max-width: 200px;" /></div>
        <div class="search-actions" style="padding-bottom: 1px;">
          <button class="btn btn-primary" @click="handleSearch">查询</button>
          <button class="btn" @click="handleReset">重置</button>
        </div>
      </div>
    </SearchContainer>

    <TableContainer v-model:page-size="pageOpts.pageSize" v-model:current-page="pageOpts.currentPage" title="审核工作台" :show-add-btn="false" :show-remove-btn="false" :total="pageOpts.total" @change="fetchData">
      <template #action-buttons>
        <button class="btn" style="color: #15803d; border-color: #15803d;" :disabled="selectedIds.size === 0" @click="handleBatchApprove"><i class="fa-solid fa-check"></i> 批量通过</button>
        <button class="btn" style="color: #b45309; border-color: #b45309;" :disabled="selectedIds.size === 0" @click="openBatchRejectDialog"><i class="fa-solid fa-xmark"></i> 批量驳回</button>
      </template>

      <div class="task-grid-wrap">
        <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
        <div v-else-if="taskData.length === 0" class="empty-box">暂无数据</div>
        <div v-else class="task-list">
          <div class="task-card" v-for="task in taskData" :key="task.id">
            <div class="task-card-inner">
              <input type="checkbox" v-if="task.id" :checked="task.id ? selectedIds.has(task.id) : false" @change="task.id && toggleSelection(task.id)" @click.stop class="card-checkbox" />
              <div class="card-body" @click="gotoDetail(task)">
                <div class="card-header">
                  <div class="title-area">
                    <span class="task-id-badge">#{{ task.id }}</span>
                    <span class="post-title">{{ getPostTitle(task) }}</span>
                  </div>
                  <div class="header-tags">
                    <span :class="['badge', statusTagClass('PENDING')]" v-if="!task.id">待审核</span>
                    <span class="badge badge-blue">{{ targetTypeLabel(task.targetType) }}</span>
                  </div>
                </div>
                <div v-if="getContentPreview(task)" class="content-preview">{{ getContentPreview(task) }}</div>
                <div class="card-meta">
                  <span class="meta-item"><i class="fa-regular fa-user"></i> 提交人 #{{ task.submitterId || "?" }}</span>
                  <span class="meta-divider">|</span>
                  <span class="meta-item"><i class="fa-regular fa-clock"></i> {{ formatISOData(task.submitAt || "") || "未知" }}</span>
                  <span v-if="getResourceCount(task) > 0" class="meta-divider">|</span>
                  <span v-if="getResourceCount(task) > 0" class="meta-item"><i class="fa-regular fa-file"></i> {{ getResourceCount(task) }} 个资源</span>
                  <span v-if="task.targetId" class="meta-divider">|</span>
                  <span v-if="task.targetId" class="meta-item">目标ID: {{ task.targetId }}</span>
                </div>
              </div>
              <div class="card-actions">
                <button class="btn" @click.stop="gotoDetail(task)">详情</button>
                <button class="btn" style="color: #15803d; border-color: #15803d;" :disabled="!task.id" @click.stop="handleApproveTask(task.id)"><i class="fa-solid fa-check"></i> 通过</button>
                <button class="btn" style="color: #b45309; border-color: #b45309;" :disabled="!task.id" @click.stop="openRejectDialog(task.id)"><i class="fa-solid fa-xmark"></i> 驳回</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </TableContainer>

    <BaseDialog v-model="rejectDialogVisible" :title="rejectBatchMode ? '批量驳回' : '驳回审核任务'" width="520px">
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
.task-grid-wrap { min-height: 400px; }
.task-list { display: flex; flex-direction: column; gap: 10px; }
.task-card { background: var(--bg-white); border: 1px solid var(--border); border-radius: var(--radius); padding: 16px; transition: box-shadow 0.2s; }
.task-card:hover { box-shadow: var(--shadow-md); }
.task-card-inner { display: flex; align-items: flex-start; gap: 12px; }
.card-checkbox { margin-top: 4px; flex-shrink: 0; }
.card-body { flex: 1; min-width: 0; cursor: pointer; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 8px; }
.title-area { display: flex; align-items: center; gap: 8px; min-width: 0; flex: 1; }
.task-id-badge { font-size: 12px; color: var(--text-muted); background: var(--bg); padding: 0 8px; border-radius: 4px; line-height: 22px; white-space: nowrap; flex-shrink: 0; }
.post-title { font-size: 15px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.header-tags { display: flex; gap: 6px; flex-shrink: 0; }
.content-preview { font-size: 13px; color: var(--text-secondary); line-height: 1.5; margin-bottom: 10px; background: var(--bg); padding: 8px 12px; border-radius: 6px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; word-break: break-all; }
.card-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; font-size: 13px; color: var(--text-muted); }
.meta-item { display: inline-flex; align-items: center; gap: 3px; }
.meta-divider { color: var(--border); }
.card-actions { display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; padding-top: 2px; }
</style>
