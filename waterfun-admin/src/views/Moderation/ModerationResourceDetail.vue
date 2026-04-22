<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import {
  approveModerationResource,
  getModerationResource,
  rejectModerationResource,
  type AuditStatus,
  type ModerateRejectType,
  type ModerationResourceResp,
} from "~/api/moderation";

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const loading = ref(false);
const detail = ref<ModerationResourceResp | null>(null);

const resourceId = computed(() => String(route.params.resourceId || ""));

const rejectDialogVisible = ref(false);
const rejectSubmitting = ref(false);
const rejectForm = ref<{ rejectType: ModerateRejectType; rejectReason: string }>({
  rejectType: "OTHER",
  rejectReason: "",
});

const rejectTypeOptions: Array<{ label: string; value: ModerateRejectType }> = [
  { label: "moderation.rejectType.violation_of_guidelines", value: "VIOLATION_OF_GUIDELINES" },
  { label: "moderation.rejectType.inappropriate_content", value: "INAPPROPRIATE_CONTENT" },
  { label: "moderation.rejectType.advertisement", value: "ADVERTISEMENT" },
  { label: "moderation.rejectType.violence", value: "VIOLENCE" },
  { label: "moderation.rejectType.sensitive", value: "SENSITIVE" },
  { label: "moderation.rejectType.other", value: "OTHER" },
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
  if (!parsed || Number.isNaN(parsed) || parsed < 0) return t("common.none.title");
  if (parsed < 1024) return `${parsed} B`;
  if (parsed < 1024 * 1024) return `${(parsed / 1024).toFixed(2)} KB`;
  return `${(parsed / (1024 * 1024)).toFixed(2)} MB`;
};

const statusTagType = (status?: AuditStatus): "warning" | "success" | "danger" | "info" => {
  if (status === "PENDING") return "warning";
  if (status === "APPROVED") return "success";
  if (status === "REJECTED") return "danger";
  return "info";
};

const previewUrl = computed(() => detail.value?.presignedUrl?.url || "");

const fetchDetail = async () => {
  if (!resourceId.value) return;
  loading.value = true;
  try {
    const res = await getModerationResource(resourceId.value);
    detail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("moderation.error.fetch"));
  } finally {
    loading.value = false;
  }
};

const handleApprove = async () => {
  if (!resourceId.value) return;
  try {
    await ElMessageBox.confirm(t("moderation.confirm.approveResource"), t("moderation.action.approve"), {
      type: "warning",
    });
    await approveModerationResource(resourceId.value);
    ElMessage.success(t("moderation.success.approve"));
    await fetchDetail();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error(t("moderation.error.approve"));
    }
  }
};

const openRejectDialog = () => {
  rejectForm.value = { rejectType: "OTHER", rejectReason: "" };
  rejectDialogVisible.value = true;
};

const submitReject = async () => {
  if (!resourceId.value) return;
  rejectSubmitting.value = true;
  try {
    await rejectModerationResource(resourceId.value, {
      rejectType: rejectForm.value.rejectType,
      rejectReason: rejectForm.value.rejectReason || undefined,
    });
    ElMessage.success(t("moderation.success.reject"));
    rejectDialogVisible.value = false;
    await fetchDetail();
  } catch (e) {
    console.error(e);
    ElMessage.error(t("moderation.error.reject"));
  } finally {
    rejectSubmitting.value = false;
  }
};

onMounted(fetchDetail);
watch(resourceId, fetchDetail);
</script>

<template>
  <div class="resource-detail" v-loading="loading">
    <CardContainer title="moderation.resourceDetail">
      <template #header-right>
        <el-button text @click="router.back()">{{ t('common.action.back') }}</el-button>
        <el-button type="success" :disabled="!resourceId" @click="handleApprove">{{ t('moderation.action.approve') }}</el-button>
        <el-button type="warning" :disabled="!resourceId" @click="openRejectDialog">{{ t('moderation.action.reject') }}</el-button>
      </template>

      <div v-if="detail" class="detail-layout">
        <div class="image-pane">
          <div class="image-stage">
            <img
              v-if="previewUrl"
              :src="previewUrl"
              :alt="detail.resourceKey || String(detail.id || '')"
              class="full-image"
            />
            <el-empty v-else :description="t('common.none.description')" />
          </div>
          <div class="image-actions" v-if="previewUrl">
            <el-link :href="previewUrl" target="_blank" type="primary">{{ t('moderation.action.openOriginal') }}</el-link>
          </div>
        </div>

        <div class="info-pane">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="ID">{{ detail.id || t('common.none.title') }}</el-descriptions-item>
            <el-descriptions-item :label="t('moderation.field.taskId')">
              {{ detail.taskId || t('common.none.title') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('moderation.field.status')">
              <el-tag :type="statusTagType(detail.status)">
                {{ t(`moderation.status.${(detail.status || 'pending').toLowerCase()}`) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="t('moderation.field.mimeType')">
              {{ detail.mimeType || t('common.none.title') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('moderation.field.fileSize')">
              {{ formatFileSize(detail.sizeBytes) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('moderation.field.resourceKey')">
              <span class="break-all">{{ detail.resourceKey || t('common.none.title') }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('moderation.field.auditorId')">
              {{ detail.auditorId || t('common.none.title') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('moderation.field.auditAt')">
              {{ formatISOData(getInstantIso(detail.auditAt)) || t('common.none.title') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('moderation.field.rejectType')">
              {{ detail.rejectType ? t(`moderation.rejectType.${detail.rejectType.toLowerCase()}`) : t('common.none.title') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('moderation.field.rejectReason')">
              {{ detail.rejectReason || t('common.none.title') }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <el-empty v-else :description="t('common.none.description')" />
    </CardContainer>

    <el-dialog v-model="rejectDialogVisible" :title="t('moderation.dialog.rejectTitle')" width="520">
      <el-form label-width="100px">
        <el-form-item :label="t('moderation.field.rejectType')">
          <el-select v-model="rejectForm.rejectType" style="width: 100%">
            <el-option
              v-for="item in rejectTypeOptions"
              :key="item.value"
              :label="t(item.label)"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('moderation.field.rejectReason')">
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            :placeholder="t('moderation.input.rejectReason')"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rejectDialogVisible = false">{{ t('common.action.cancel') }}</el-button>
        <el-button type="primary" :loading="rejectSubmitting" @click="submitReject">{{ t('common.action.save') }}</el-button>
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

.break-all {
  word-break: break-all;
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
