<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { useRoute, useRouter } from "vue-router";
import {
  getModerationTaskById,
  listResourcesByTask,
  approveModerationById,
  rejectModerationById,
  approveModerationResource,
  rejectModerationResource,
  type ModerateTaskResp,
  type ModerationResourceRes,
  type ModerateRejectType,
  type AuditStatus,
} from "~/api/moderation";
import { useDictStore } from "~/stores/dictStore";
import { renderContent } from "~/utils/markdown";
import CardContainer from "~/components/CardContainer.vue";

const dictStore = useDictStore();

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const resourcesLoading = ref(false);
const taskDetail = ref<ModerateTaskResp | null>(null);
const resources = ref<ModerationResourceRes[]>([]);

const taskId = computed(() => String(route.params.taskId));

const rejectDialogVisible = ref(false);
const rejectSubmitting = ref(false);
const rejectForm = ref<{ rejectType: ModerateRejectType; rejectReason: string }>({
  rejectType: "OTHER",
  rejectReason: "",
});

const resourceRejectDialogVisible = ref(false);
const resourceRejectSubmitting = ref(false);
const currentRejectResource = ref<{ taskId: string; resourceUuid: string } | null>(null);
const resourceRejectForm = ref<{ rejectType: ModerateRejectType; rejectReason: string }>({
  rejectType: "OTHER",
  rejectReason: "",
});

const rejectTypeOptions: Array<{ label: string; value: ModerateRejectType }> = [
  { label: "违反社区准则", value: "VIOLATION_OF_GUIDELINES" },
  { label: "不当内容", value: "INAPPROPRIATE_CONTENT" },
  { label: "广告", value: "ADVERTISEMENT" },
  { label: "暴力内容", value: "VIOLENCE" },
  { label: "敏感内容", value: "SENSITIVE" },
  { label: "级联驳回", value: "CASCADE" },
  { label: "其他", value: "OTHER" },
];

const getInstantIso = (value?: string | null): string => {
  if (!value) return "";
  return value;
};

const statusTagType = (status?: AuditStatus): "warning" | "success" | "danger" | "info" => {
  if (status === "PENDING") return "warning";
  if (status === "APPROVED") return "success";
  if (status === "REJECTED") return "danger";
  if (status === "SUSPECT") return "info";
  return "info";
};

const targetTypeLabel = (type?: string): string => {
  if (!type) return "未知";
  const map: Record<string, string> = {
    POST: "帖子",
    USER_AVATAR: "用户头像",
    POST_COVERAGE_IMAGE: "帖子封面图",
    POST_CONTENT_IMAGE: "帖子内容图",
    BANNER_IMAGE: "轮播图",
    POST_CONTENT: "帖子内容",
    UNKNOWN: "未知",
  };
  return map[type] || type;
};

const statusLabel = (status?: string): string => {
  if (status === "PENDING") return "待审核";
  if (status === "APPROVED") return "已通过";
  if (status === "REJECTED") return "已驳回";
  if (status === "SUSPECT") return "可疑";
  return status || "未知";
};

const rejectTypeLabel = (type?: string): string => {
  if (type === "VIOLATION_OF_GUIDELINES") return "违反社区准则";
  if (type === "INAPPROPRIATE_CONTENT") return "不当内容";
  if (type === "ADVERTISEMENT") return "广告";
  if (type === "VIOLENCE") return "暴力内容";
  if (type === "SENSITIVE") return "敏感内容";
  if (type === "CASCADE") return "级联驳回";
  if (type === "OTHER") return "其他";
  return type || "无";
};

const formatFileSize = (size?: number | string): string => {
  const parsed = Number(size);
  if (!parsed || Number.isNaN(parsed) || parsed < 0) return "无";
  if (parsed < 1024) return `${parsed} B`;
  if (parsed < 1024 * 1024) return `${(parsed / 1024).toFixed(2)} KB`;
  return `${(parsed / (1024 * 1024)).toFixed(2)} MB`;
};

const parsePostMeta = (meta?: Record<string, unknown>): Record<string, unknown> | null => {
  if (!meta || Object.keys(meta).length === 0) return null;
  return meta;
};

const fetchTaskDetail = async () => {
  if (!taskId.value) return;
  loading.value = true;
  try {
    const res = await getModerationTaskById(taskId.value);
    taskDetail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error("获取任务详情失败");
  } finally {
    loading.value = false;
  }
};

const fetchResources = async () => {
  if (!taskId.value) return;
  resourcesLoading.value = true;
  try {
    const res = await listResourcesByTask(taskId.value);
    resources.value = res.data || [];
  } catch (e) {
    console.error(e);
  } finally {
    resourcesLoading.value = false;
  }
};

const handleApprove = async () => {
  if (!taskId.value) return;
  try {
    await ElMessageBox.confirm("确定通过该审核任务吗？", "通过", { type: "warning" });
    await approveModerationById(taskId.value);
    ElMessage.success("审核通过成功");
    await fetchTaskDetail();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error("审核通过失败");
    }
  }
};

const openRejectDialog = () => {
  rejectForm.value = { rejectType: "OTHER", rejectReason: "" };
  rejectDialogVisible.value = true;
};

const submitReject = async () => {
  if (!taskId.value) return;
  rejectSubmitting.value = true;
  try {
    await rejectModerationById(taskId.value, {
      rejectType: rejectForm.value.rejectType,
      rejectReason: rejectForm.value.rejectReason || undefined,
    });
    ElMessage.success("审核驳回成功");
    rejectDialogVisible.value = false;
    await fetchTaskDetail();
  } catch (e) {
    console.error(e);
    ElMessage.error("审核驳回失败");
  } finally {
    rejectSubmitting.value = false;
  }
};

const handleApproveResource = async (res: ModerationResourceRes) => {
  if (!res.taskId || !res.resourceUuid) return;
  try {
    await ElMessageBox.confirm("确定通过该资源吗？", "通过资源", { type: "warning" });
    await approveModerationResource(res.taskId, res.resourceUuid);
    ElMessage.success("资源审核通过");
    await fetchResources();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error("资源审核失败");
    }
  }
};

const openResourceRejectDialog = (res: ModerationResourceRes) => {
  if (!res.taskId || !res.resourceUuid) return;
  currentRejectResource.value = { taskId: res.taskId, resourceUuid: res.resourceUuid };
  resourceRejectForm.value = { rejectType: "OTHER", rejectReason: "" };
  resourceRejectDialogVisible.value = true;
};

const submitResourceReject = async () => {
  if (!currentRejectResource.value) return;
  resourceRejectSubmitting.value = true;
  try {
    await rejectModerationResource(
      currentRejectResource.value.taskId,
      currentRejectResource.value.resourceUuid,
      {
        rejectType: resourceRejectForm.value.rejectType,
        rejectReason: resourceRejectForm.value.rejectReason || undefined,
      }
    );
    ElMessage.success("资源驳回成功");
    resourceRejectDialogVisible.value = false;
    await fetchResources();
  } catch (e) {
    console.error(e);
    ElMessage.error("资源驳回失败");
  } finally {
    resourceRejectSubmitting.value = false;
  }
};

const getPostMetaValue = (key: string): string | undefined => {
  const meta = taskDetail.value?.payload?.meta;
  if (!meta) return undefined;
  return meta[key] as string | undefined;
};

const getPostMetaArray = (key: string): number[] | undefined => {
  const meta = taskDetail.value?.payload?.meta;
  if (!meta) return undefined;
  return meta[key] as number[] | undefined;
};

const getPostMetaStringArray = (key: string): string[] | undefined => {
  const meta = taskDetail.value?.payload?.meta;
  if (!meta) return undefined;
  return meta[key] as string[] | undefined;
};

const displayCategoryName = computed(() => {
  const id = getPostMetaValue("categoryId");
  if (!id) return "-";
  const numId = Number(id);
  if (isNaN(numId)) return id;
  return dictStore.getCategoryName(numId) || `${numId} (已删除)`;
});

const displayTagNames = computed(() => {
  const ids = getPostMetaArray("tagIds");
  if (!ids || ids.length === 0) return null;
  return ids.map((id) => dictStore.getTagName(id) || `${id} (已删除)`);
});

const displayNewTagNames = computed(() => {
  return getPostMetaStringArray("newTagNames") ?? null;
});

const coverResPresignedUrl = computed<{ url?: string; expireAt?: string } | null>(() => {
  const meta = taskDetail.value?.payload?.meta;
  if (!meta) return null;
  const val = meta["coverResPresignedUrl"];
  if (!val || typeof val !== "object") return null;
  return val as { url?: string; expireAt?: string };
});

const payloadResources = computed(() => {
  return taskDetail.value?.payload?.resources ?? [];
});

const contentExpanded = ref(true);
const previewImageUrl = ref("");
const previewDialogVisible = ref(false);

function openImagePreview(url: string) {
  previewImageUrl.value = url;
  previewDialogVisible.value = true;
}

function onContentImageClick(e: MouseEvent) {
  const target = e.target as HTMLElement;
  if (target.tagName === "IMG") {
    const src = (target as HTMLImageElement).src;
    if (src) {
      openImagePreview(src);
    }
  }
}

onMounted(async () => {
  await dictStore.ensureLoaded();
  fetchTaskDetail();
  fetchResources();
});

watch(() => route.params.taskId, () => {
  fetchTaskDetail();
  fetchResources();
});
</script>

<template>
  <div class="task-detail">
    <div class="page-loading" v-if="loading">加载中...</div>
    <template v-else>
      <CardContainer title="任务详情" fixed-header>
        <template #header-right>
          <button class="btn btn-default" @click="router.back()">返回</button>
          <button class="btn btn-success" :disabled="!taskId" @click="handleApprove">通过</button>
          <button class="btn btn-warning" :disabled="!taskId" @click="openRejectDialog">驳回</button>
        </template>

        <div class="detail-layout">
          <el-descriptions :column="2" border title="基本信息">
            <el-descriptions-item label="任务 ID">
              <span class="task-id-value">#{{ taskId || "无" }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="任务类型">
              <el-tag size="small">{{ targetTypeLabel(taskDetail?.targetType) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="目标 ID">{{ taskDetail?.targetId || "无" }}</el-descriptions-item>
            <el-descriptions-item label="提交人">
              <span>#{{ taskDetail?.submitterId || "无" }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="提交时间">
              {{ formatISOData(getInstantIso(taskDetail?.submitAt)) || "无" }}
            </el-descriptions-item>
            <el-descriptions-item label="Payload 类型">
              <el-tag v-if="taskDetail?.payload?.type" size="small" type="primary">
                {{ taskDetail.payload.type }}
              </el-tag>
              <span v-else>无</span>
            </el-descriptions-item>
          </el-descriptions>

          <el-divider />

          <div v-if="taskDetail?.payload" class="payload-section">
            <h4 class="section-title">审核内容</h4>

            <div
              v-if="taskDetail.payload.type === 'RICH_TEXT' || taskDetail.payload.type === 'PLAIN_TEXT'"
              class="content-card"
            >
              <div v-if="parsePostMeta(taskDetail.payload.meta)" class="post-meta-grid">
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="标题">
                    {{ getPostMetaValue('title') || "-" }}
                  </el-descriptions-item>
                  <el-descriptions-item label="副标题">
                    {{ getPostMetaValue('subTitle') || "-" }}
                  </el-descriptions-item>
                  <el-descriptions-item label="摘要" :span="2">
                    {{ getPostMetaValue('summary') || "-" }}
                  </el-descriptions-item>
                  <el-descriptions-item label="分类">
                    {{ displayCategoryName }}
                  </el-descriptions-item>
                  <el-descriptions-item label="标签">
                    <span v-if="displayTagNames">{{ displayTagNames.join(", ") }}</span>
                    <span v-else>-</span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="displayNewTagNames" label="新标签">
                    <el-space wrap>
                      <el-tag v-for="tag in displayNewTagNames" :key="tag" size="small" type="success">
                        {{ tag }}
                      </el-tag>
                    </el-space>
                  </el-descriptions-item>
                </el-descriptions>
              </div>

              <div v-if="coverResPresignedUrl?.url" class="cover-section">
                <h5 class="content-title">封面图</h5>
                <el-image
                  :src="coverResPresignedUrl.url"
                  style="max-width: 360px; max-height: 200px; border-radius: 6px; cursor: pointer;"
                  fit="contain"
                  @click="openImagePreview(coverResPresignedUrl.url!)"
                />
              </div>

              <div v-if="taskDetail.payload.content" class="content-box">
                <div class="content-title-row">
                  <h5 class="content-title">正文内容</h5>
                  <button class="btn-text" @click="contentExpanded = !contentExpanded">
                    {{ contentExpanded ? "收起" : "展开" }}
                  </button>
                </div>
                <div
                  v-if="contentExpanded"
                  class="rendered-content"
                  v-html="renderContent(taskDetail.payload.content, taskDetail.payload.contentFormat)"
                  @click="onContentImageClick"
                />
              </div>
            </div>

          </div>

          <div v-else class="empty-payload">
            <el-empty description="无审核内容" />
          </div>

          <el-divider />

          <div class="resources-section">
            <div class="resources-header">
              <h4 class="section-title">关联资源 ({{ resources.length || payloadResources.length }})</h4>
            </div>

            <div :class="{ loading: resourcesLoading }">
              <div v-if="resources.length === 0 && payloadResources.length === 0" class="empty-box">
                <el-empty description="暂无关联资源" />
              </div>

              <div v-else class="table-wrapper">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>资源 UUID</th>
                      <th style="width:110px">审核状态</th>
                      <th style="width:140px">MIME 类型</th>
                      <th style="width:100px">文件大小</th>
                      <th style="width:100px">预览</th>
                      <th style="width:170px">审核时间</th>
                      <th>驳回信息</th>
                      <th style="width:150px">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in (resources.length ? resources : payloadResources)" :key="row.resourceUuid">
                      <td><span class="uuid-text">{{ row.resourceUuid }}</span></td>
                      <td><el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag></td>
                      <td>{{ row.fileProbeResult?.mimeType || "-" }}</td>
                      <td>{{ formatFileSize(row.fileProbeResult?.size) }}</td>
                      <td>
                        <el-image
                          v-if="row.presignedUrl?.url"
                          :src="row.presignedUrl.url"
                          style="width: 48px; height: 48px; border-radius: 4px; object-fit: cover;"
                          fit="cover"
                          :preview-src-list="[row.presignedUrl.url]"
                          preview-teleported
                        />
                        <span v-else>-</span>
                      </td>
                      <td>{{ formatISOData(getInstantIso(row.auditAt)) || "-" }}</td>
                      <td>
                        <div v-if="row.rejectType" class="reject-info">
                          <el-tag size="small" type="danger">{{ rejectTypeLabel(row.rejectType) }}</el-tag>
                          <span v-if="row.rejectReason" class="reject-reason">{{ row.rejectReason }}</span>
                        </div>
                        <span v-else>-</span>
                      </td>
                      <td>
                        <div class="table-actions">
                          <button class="btn btn-sm btn-success" :disabled="row.status !== 'PENDING'" @click="handleApproveResource(row)">通过</button>
                          <button class="btn btn-sm btn-warning" :disabled="row.status !== 'PENDING'" @click="openResourceRejectDialog(row)">驳回</button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </CardContainer>

      <el-dialog v-model="rejectDialogVisible" title="驳回审核任务" width="520">
        <div class="dialog-form">
          <div class="form-row">
            <label class="form-label">驳回类型</label>
            <el-select v-model="rejectForm.rejectType" style="width: 100%">
              <el-option v-for="item in rejectTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <div class="form-row">
            <label class="form-label">驳回原因</label>
            <el-input v-model="rejectForm.rejectReason" type="textarea" :rows="4" placeholder="请输入驳回原因（可选）" />
          </div>
        </div>
        <template #footer>
          <button class="btn btn-default" @click="rejectDialogVisible = false">取消</button>
          <button class="btn btn-primary" :disabled="rejectSubmitting" @click="submitReject">保存</button>
        </template>
      </el-dialog>

      <el-dialog v-model="resourceRejectDialogVisible" title="驳回资源" width="520">
        <div class="dialog-form">
          <div class="form-row">
            <label class="form-label">驳回类型</label>
            <el-select v-model="resourceRejectForm.rejectType" style="width: 100%">
              <el-option v-for="item in rejectTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <div class="form-row">
            <label class="form-label">驳回原因</label>
            <el-input v-model="resourceRejectForm.rejectReason" type="textarea" :rows="4" placeholder="请输入驳回原因（可选）" />
          </div>
        </div>
        <template #footer>
          <button class="btn btn-default" @click="resourceRejectDialogVisible = false">取消</button>
          <button class="btn btn-primary" :disabled="resourceRejectSubmitting" @click="submitResourceReject">保存</button>
        </template>
      </el-dialog>

      <el-dialog v-model="previewDialogVisible" title="图片预览" width="auto" :close-on-click-modal="true" align-center>
        <el-image
          v-if="previewImageUrl"
          :src="previewImageUrl"
          style="max-width: 80vw; max-height: 80vh;"
          fit="contain"
        />
      </el-dialog>
    </template>
  </div>
</template>

<style scoped>
.task-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #909399;
  font-size: 14px;
}

.detail-layout {
  padding: 16px 0;
}

.section-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
}

.task-id-value {
  font-weight: 600;
  font-size: 14px;
}

.content-card {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}

.post-meta-grid {
  margin-bottom: 8px;
}

.content-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.content-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.rendered-content {
  padding: 16px;
  background: #f8f9fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  line-height: 1.8;
  font-size: 14px;
  word-break: break-word;
}

.cover-section {
  margin-top: 12px;
  padding: 12px 0;
}

.rendered-content :deep(img) {
  max-width: 600px;
  max-height: 400px;
  width: auto;
  height: auto;
  display: block;
  margin: 8px 0;
  border-radius: 4px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.rendered-content :deep(img:hover) {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.rendered-content :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.rendered-content :deep(a:hover) {
  text-decoration: underline;
}

.rendered-content :deep(pre) {
  background: #2d2d2d;
  color: #f8f8f2;
  padding: 12px 16px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.5;
}

.rendered-content :deep(code) {
  font-family: "SF Mono", Monaco, "Cascadia Code", "Consolas", monospace;
  background: #e8eaed;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.9em;
}

.rendered-content :deep(pre code) {
  background: none;
  padding: 0;
  border-radius: 0;
  font-size: inherit;
}

.rendered-content :deep(blockquote) {
  border-left: 4px solid #409eff;
  margin: 8px 0;
  padding: 4px 12px;
  background: #f0f7ff;
  border-radius: 0 4px 4px 0;
}

.rendered-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
}

.rendered-content :deep(th),
.rendered-content :deep(td) {
  border: 1px solid #dcdfe6;
  padding: 6px 12px;
  text-align: left;
}

.rendered-content :deep(th) {
  background: #f0f2f5;
  font-weight: 600;
}

.rendered-content :deep(ul),
.rendered-content :deep(ol) {
  padding-left: 24px;
  margin: 4px 0;
}

.rendered-content :deep(h1),
.rendered-content :deep(h2),
.rendered-content :deep(h3),
.rendered-content :deep(h4) {
  margin: 12px 0 6px;
  font-weight: 600;
}

.rendered-content :deep(h1) { font-size: 20px; }
.rendered-content :deep(h2) { font-size: 18px; }
.rendered-content :deep(h3) { font-size: 16px; }
.rendered-content :deep(h4) { font-size: 15px; }

.rendered-content :deep(p) {
  margin: 6px 0;
}

.rendered-content :deep(hr) {
  border: none;
  border-top: 1px solid #dcdfe6;
  margin: 12px 0;
}

.empty-payload {
  padding: 40px 0;
}

.resources-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.resources-section {
  margin-top: 8px;
}

.empty-box {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}

.uuid-text {
  font-family: "SF Mono", Monaco, "Cascadia Code", "Consolas", monospace;
  font-size: 12px;
}

.reject-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.reject-reason {
  font-size: 12px;
  color: #909399;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.form-label {
  min-width: 100px;
  text-align: right;
  font-size: 14px;
  color: #606266;
  line-height: 32px;
  flex-shrink: 0;
}

.form-row .el-select,
.form-row .el-textarea {
  flex: 1;
}
</style>
