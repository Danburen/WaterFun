<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import {
  getModerationResource,
  approveModerationResource,
  rejectModerationResource,
  type AuditStatus,
  type ModerateRejectType,
  type ModerationResourceRes,
} from "~/api/moderation";
import { ElMessage } from "element-plus";
import CardContainer from "~/components/CardContainer.vue";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const detail = ref<ModerationResourceRes | null>(null);

const taskId = computed(() => Number(route.params.taskId));
const resourceUuid = computed(() => String(route.params.resourceUuid || ""));

const rejectDialogVisible = ref(false);
const rejectSubmitting = ref(false);
const rejectForm = ref<{ rejectType: ModerateRejectType; rejectReason: string }>({
  rejectType: "OTHER",
  rejectReason: "",
});

const rejectTypeOptions: Array<{ label: string; value: ModerateRejectType }> = [
  { label: "违反社区准则", value: "VIOLATION_OF_GUIDELINES" },
  { label: "不当内容", value: "INAPPROPRIATE_CONTENT" },
  { label: "广告", value: "ADVERTISEMENT" },
  { label: "暴力内容", value: "VIOLENCE" },
  { label: "敏感内容", value: "SENSITIVE" },
  { label: "其他", value: "OTHER" },
];

const getInstantIso = (value?: { seconds?: number; nanos?: number } | string | null): string => {
  if (!value) return "";
  if (typeof value === "string") return value;
  const seconds = Number(value.seconds || 0);
  const nanos = Number(value.nanos || 0);
  if (!seconds && !nanos) return "";
  const ms = seconds * 1000 + Math.floor(nanos / 1_000_000);
  return new Date(ms).toISOString();
};

const formatFileSize = (size?: number | string): string => {
  const parsed = Number(size);
  if (!parsed || Number.isNaN(parsed) || parsed < 0) return "无";
  if (parsed < 1024) return `${parsed} B`;
  if (parsed < 1024 * 1024) return `${(parsed / 1024).toFixed(2)} KB`;
  return `${(parsed / (1024 * 1024)).toFixed(2)} MB`;
};

const statusLabel = (status?: string): string => {
  if (status === "PENDING") return "待审核";
  if (status === "APPROVED") return "已通过";
  if (status === "REJECTED") return "已驳回";
  return status || "未知";
};

const rejectTypeLabel = (type?: string): string => {
  if (type === "VIOLATION_OF_GUIDELINES") return "违反社区准则";
  if (type === "INAPPROPRIATE_CONTENT") return "不当内容";
  if (type === "ADVERTISEMENT") return "广告";
  if (type === "VIOLENCE") return "暴力内容";
  if (type === "SENSITIVE") return "敏感内容";
  if (type === "OTHER") return "其他";
  return type || "无";
};

const statusTagType = (status?: AuditStatus): "warning" | "success" | "danger" | "info" => {
  if (status === "PENDING") return "warning";
  if (status === "APPROVED") return "success";
  if (status === "REJECTED") return "danger";
  return "info";
};

const previewUrl = computed(() => detail.value?.presignedUrl?.url || "");

const fetchDetail = async () => {
  if (!taskId.value || !resourceUuid.value) return;
  loading.value = true;
  try {
    const res = await getModerationResource(taskId.value, resourceUuid.value);
    detail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error("获取资源详情失败");
  } finally {
    loading.value = false;
  }
};

const handleApprove = async () => {
  if (!taskId.value || !resourceUuid.value) return;
  try {
    await ElMessageBox.confirm("确定通过该审核资源吗？", "通过", { type: "warning" });
    await approveModerationResource(taskId.value, resourceUuid.value);
    ElMessage.success("审核通过成功");
    await fetchDetail();
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
  if (!taskId.value || !resourceUuid.value) return;
  rejectSubmitting.value = true;
  try {
    await rejectModerationResource(taskId.value, resourceUuid.value, {
      rejectType: rejectForm.value.rejectType,
      rejectReason: rejectForm.value.rejectReason || undefined,
    });
    ElMessage.success("审核驳回成功");
    rejectDialogVisible.value = false;
    await fetchDetail();
  } catch (e) {
    console.error(e);
    ElMessage.error("审核驳回失败");
  } finally {
    rejectSubmitting.value = false;
  }
};

onMounted(fetchDetail);
watch([taskId, resourceUuid], fetchDetail);
</script>

<template>
  <div v-loading="loading" class="resource-detail">
    <CardContainer title="资源详情">
      <template #header-right>
        <el-button text @click="router.back()">返回</el-button>
        <el-button type="success" :disabled="!resourceUuid" @click="handleApprove">通过</el-button>
        <el-button type="warning" :disabled="!resourceUuid" @click="openRejectDialog">驳回</el-button>
      </template>

      <div v-if="detail" class="detail-layout">
        <div class="image-pane">
          <div class="image-stage">
            <img
              v-if="previewUrl"
              :src="previewUrl"
              :alt="detail.resourceUuid || ''"
              class="full-image"
            >
            <el-empty v-else description="暂无数据" />
          </div>
          <div v-if="previewUrl" class="image-actions">
            <el-link :href="previewUrl" target="_blank" type="primary">打开原图</el-link>
          </div>
        </div>

        <div class="info-pane">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="资源UUID">
              {{ detail.resourceUuid || "无" }}
            </el-descriptions-item>
            <el-descriptions-item label="所属任务ID">
              {{ detail.taskId || "无" }}
            </el-descriptions-item>
            <el-descriptions-item label="审核状态">
              <el-tag :type="statusTagType(detail.status)">
                {{ statusLabel(detail.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="MIME类型">
              {{ detail.fileProbeResult?.mimeType || "无" }}
            </el-descriptions-item>
            <el-descriptions-item label="文件大小">
              {{ formatFileSize(detail.fileProbeResult?.size) }}
            </el-descriptions-item>
            <el-descriptions-item label="审核人ID">
              {{ detail.auditorId || "无" }}
            </el-descriptions-item>
            <el-descriptions-item label="审核时间">
              {{ formatISOData(getInstantIso(detail.auditAt)) || "无" }}
            </el-descriptions-item>
            <el-descriptions-item label="驳回类型">
              {{ rejectTypeLabel(detail.rejectType) }}
            </el-descriptions-item>
            <el-descriptions-item label="驳回原因">
              {{ detail.rejectReason || "无" }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <el-empty v-else description="暂无数据" />
    </CardContainer>

    <el-dialog v-model="rejectDialogVisible" title="驳回审核资源" width="520">
      <el-form label-width="100px">
        <el-form-item label="驳回类型">
          <el-select v-model="rejectForm.rejectType" style="width: 100%">
            <el-option
              v-for="item in rejectTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="驳回原因">
          <el-input v-model="rejectForm.rejectReason" type="textarea" :rows="4" placeholder="请输入驳回原因（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rejectSubmitting" @click="submitReject">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.resource-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(380px, 1fr) minmax(300px, 420px);
  gap: 16px;
}

.image-pane {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.image-stage {
  min-height: 500px;
  max-height: 70vh;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #f8f9fb;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.full-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.image-actions {
  display: flex;
  justify-content: flex-end;
}

.info-pane {
  overflow: hidden;
}

@media (max-width: 992px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .image-stage {
    min-height: 340px;
  }
}
</style>
