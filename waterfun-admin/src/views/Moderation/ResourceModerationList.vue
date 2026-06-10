<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox, ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { listModerationResources, approveModerationResource, rejectModerationResource, type AuditStatus, type ModerateRejectType, type ModerationResourceRes, type ResourceType } from "~/api/moderation";
import type { PageOptions } from "~/types/api";
import BaseDialog from "~/components/BaseDialog.vue";

const router = useRouter();

const loading = ref(false);
const cardData = ref<ModerationResourceRes[]>([]);

const searchForm = ref<{ taskId: string; status: AuditStatus | ""; resourceType: ResourceType | ""; auditorId: string; auditAtStart: string; auditAtEnd: string }>({ taskId: "", status: "PENDING", resourceType: "", auditorId: "", auditAtStart: "", auditAtEnd: "" });
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 12, total: 0 });

const statusOptions = [{ label: "待审核", value: "PENDING" }, { label: "已通过", value: "APPROVED" }, { label: "已驳回", value: "REJECTED" }, { label: "可疑", value: "SUSPECT" }];
const resourceTypeOptions = [{ label: "图片", value: "IMAGE" }, { label: "视频", value: "VIDEO" }, { label: "音频", value: "AUDIO" }, { label: "文档", value: "DOCUMENT" }, { label: "文本", value: "TEXT" }, { label: "其他", value: "OTHER" }];
const rejectTypeOptions = [{ label: "违反社区准则", value: "VIOLATION_OF_GUIDELINES" }, { label: "不当内容", value: "INAPPROPRIATE_CONTENT" }, { label: "广告", value: "ADVERTISEMENT" }, { label: "暴力内容", value: "VIOLENCE" }, { label: "敏感内容", value: "SENSITIVE" }, { label: "级联驳回", value: "CASCADE" }, { label: "其他", value: "OTHER" }];

const rejectDialogVisible = ref(false);
const rejectSubmitting = ref(false);
const currentRejectTaskId = ref("");
const currentRejectResourceUuid = ref("");
const rejectType = ref<ModerateRejectType>("OTHER");
const rejectReason = ref("");

const formatFileSize = (size?: number | string): string => {
  const parsed = Number(size);
  if (!parsed || Number.isNaN(parsed) || parsed < 0) return "无";
  if (parsed < 1024) return `${parsed} B`;
  if (parsed < 1024 * 1024) return `${(parsed / 1024).toFixed(2)} KB`;
  return `${(parsed / (1024 * 1024)).toFixed(2)} MB`;
};

const previewUrl = (item: ModerationResourceRes): string => item.presignedUrl?.url || "";

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

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listModerationResources({ page: (pageOpts.value.currentPage || 1) - 1, size: pageOpts.value.pageSize, taskId: searchForm.value.taskId || undefined, status: searchForm.value.status || undefined, resourceType: searchForm.value.resourceType || undefined, auditorId: searchForm.value.auditorId || undefined, auditAtStart: searchForm.value.auditAtStart ? new Date(searchForm.value.auditAtStart).toISOString() : undefined, auditAtEnd: searchForm.value.auditAtEnd ? new Date(searchForm.value.auditAtEnd).toISOString() : undefined });
    cardData.value = res.data.content || [];
    pageOpts.value.total = pageTotal(res.data as unknown as Record<string, unknown>);
  } catch { ElMessage.error("获取资源列表失败"); }
  finally { loading.value = false; }
};

const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleReset = () => { pageOpts.value.currentPage = 1; searchForm.value = { taskId: "", status: "PENDING", resourceType: "", auditorId: "", auditAtStart: "", auditAtEnd: "" }; fetchData(); };
const gotoDetail = (item: ModerationResourceRes) => { if (item.taskId && item.resourceUuid) router.push({ name: "moderationResourceDetail", params: { taskId: String(item.taskId), resourceUuid: item.resourceUuid } }); };

const handleApproveResource = async (item: ModerationResourceRes) => {
  if (!item.taskId || !item.resourceUuid) return;
  try { await ElMessageBox.confirm("确定通过该审核资源吗？", "通过", { type: "warning" }); await approveModerationResource(item.taskId, item.resourceUuid); ElMessage.success("审核通过成功"); await fetchData(); }
  catch (e) { if (e !== "cancel") { ElMessage.error("审核通过失败"); } }
};

const openRejectDialog = (item: ModerationResourceRes) => {
  if (!item.taskId || !item.resourceUuid) return;
  currentRejectTaskId.value = item.taskId; currentRejectResourceUuid.value = item.resourceUuid; rejectType.value = "OTHER"; rejectReason.value = ""; rejectDialogVisible.value = true;
};

const submitReject = async () => {
  if (!currentRejectTaskId.value || !currentRejectResourceUuid.value) return;
  rejectSubmitting.value = true;
  try { await rejectModerationResource(currentRejectTaskId.value, currentRejectResourceUuid.value, { rejectType: rejectType.value, rejectReason: rejectReason.value || undefined }); ElMessage.success("审核驳回成功"); rejectDialogVisible.value = false; await fetchData(); }
  catch { ElMessage.error("审核驳回失败"); }
  finally { rejectSubmitting.value = false; }
};

onMounted(fetchData);
</script>

<template>
  <div class="list-layout">
    <SearchContainer>
      <div class="search-form">
        <div class="search-field"><label>任务ID</label><input v-model="searchForm.taskId" class="form-input" placeholder="请输入任务ID" style="max-width: 180px;" /></div>
        <div class="search-field"><label>资源类型</label><select v-model="searchForm.resourceType" class="form-select" style="max-width: 140px;"><option value="">全部</option><option v-for="item in resourceTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></div>
        <div class="search-field"><label>审核状态</label><select v-model="searchForm.status" class="form-select" style="max-width: 140px;"><option value="">全部</option><option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></div>
        <div class="search-field"><label>审核人ID</label><input v-model="searchForm.auditorId" class="form-input" placeholder="请输入审核人ID" style="max-width: 180px;" /></div>
        <div class="search-field"><label>审核时间起</label><input v-model="searchForm.auditAtStart" type="datetime-local" class="form-input" style="max-width: 200px;" /></div>
        <div class="search-field"><label>审核时间止</label><input v-model="searchForm.auditAtEnd" type="datetime-local" class="form-input" style="max-width: 200px;" /></div>
        <div class="search-actions" style="padding-bottom: 1px;">
          <button class="btn btn-primary" @click="handleSearch">查询</button>
          <button class="btn" @click="handleReset">重置</button>
        </div>
      </div>
    </SearchContainer>

    <TableContainer v-model:page-size="pageOpts.pageSize" v-model:current-page="pageOpts.currentPage" title="资源列表" :show-add-btn="false" :show-remove-btn="false" :total="pageOpts.total" @change="fetchData">
      <div class="resource-grid-wrap">
        <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
        <div v-else-if="cardData.length === 0" class="empty-box">暂无数据</div>
        <div v-else class="resource-list">
          <div v-for="item in cardData" :key="item.resourceUuid" class="resource-card">
            <div class="resource-card-body" @click="gotoDetail(item)">
              <div class="image-box">
                <img v-if="previewUrl(item)" :src="previewUrl(item)" :alt="item.resourceUuid || ''" class="preview-image" />
                <div v-else class="image-empty">无</div>
                <div class="hover-mask">详情</div>
              </div>
              <div class="card-main">
                <div class="title-row">
                  <span class="resource-uuid">#{{ item.resourceUuid?.substring(0, 12) || "无" }}...</span>
                  <span :class="['badge', statusTagClass(item.status)]">{{ ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', SUSPECT: '可疑' })[item.status || ''] || '未知' }}</span>
                </div>
                <div class="meta-row">任务ID: {{ item.taskId || "无" }}</div>
                <div class="meta-row">MIME: {{ item.fileProbeResult?.mimeType || "无" }}</div>
                <div class="meta-row">大小: {{ formatFileSize(item.fileProbeResult?.size) }}</div>
                <div class="meta-row">审核时间: {{ formatISOData(item.auditAt || "") || "无" }}</div>
              </div>
            </div>
            <div class="card-actions">
              <button class="btn" style="color: #15803d; border-color: #15803d;" @click.stop="handleApproveResource(item)"><i class="fa-solid fa-check"></i> 通过</button>
              <button class="btn" style="color: #b45309; border-color: #b45309;" @click.stop="openRejectDialog(item)"><i class="fa-solid fa-xmark"></i> 驳回</button>
            </div>
          </div>
        </div>
      </div>
    </TableContainer>

    <BaseDialog v-model="rejectDialogVisible" title="驳回审核资源" width="520px">
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
.resource-grid-wrap { min-height: 400px; }
.resource-list { display: flex; flex-direction: column; gap: 12px; }
.resource-card { background: var(--bg-white); border: 1px solid var(--border); border-radius: var(--radius); padding: 12px; cursor: pointer; transition: box-shadow 0.2s; display: flex; gap: 16px; align-items: flex-start; }
.resource-card:hover { box-shadow: var(--shadow-md); }
.resource-card-body { flex: 1; display: flex; gap: 16px; min-width: 0; }
.image-box { position: relative; width: 120px; height: 120px; flex-shrink: 0; border-radius: 6px; overflow: hidden; background: var(--bg); }
.preview-image { width: 100%; height: 100%; object-fit: cover; transition: transform 0.2s; }
.resource-card:hover .preview-image { transform: scale(1.04); }
.image-empty { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: var(--text-muted); font-size: 13px; }
.hover-mask { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.38); color: #fff; opacity: 0; transition: opacity 0.2s; font-size: 13px; }
.resource-card:hover .hover-mask { opacity: 1; }
.card-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.title-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 8px; }
.resource-uuid { color: var(--text-primary); font-weight: 600; font-size: 14px; }
.meta-row { font-size: 13px; color: var(--text-secondary); line-height: 1.8; word-break: break-all; }
.card-actions { display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; }
</style>
