<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import {
  approveModerationResource,
  listModerationResources,
  rejectModerationResource,
  type AuditStatus,
  type ModerateRejectType,
  type ModerationResourceResp,
} from "~/api/moderation";
import type { PageOptions } from "~/types/api";

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const cardData = ref<ModerationResourceResp[]>([]);

const searchForm = ref<{
  taskId: string;
  status: AuditStatus | "";
  auditorId: string;
  auditAtRange: [Date, Date] | null;
}>({
  taskId: "",
  status: "PENDING",
  auditorId: "",
  auditAtRange: null,
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 12,
  total: 0,
});

const statusOptions: Array<{ label: string; value: AuditStatus }> = [
  { label: "moderation.status.pending", value: "PENDING" },
  { label: "moderation.status.approved", value: "APPROVED" },
  { label: "moderation.status.rejected", value: "REJECTED" },
];

const rejectTypeOptions: Array<{ label: string; value: ModerateRejectType }> = [
  { label: "moderation.rejectType.violation_of_guidelines", value: "VIOLATION_OF_GUIDELINES" },
  { label: "moderation.rejectType.inappropriate_content", value: "INAPPROPRIATE_CONTENT" },
  { label: "moderation.rejectType.advertisement", value: "ADVERTISEMENT" },
  { label: "moderation.rejectType.violence", value: "VIOLENCE" },
  { label: "moderation.rejectType.sensitive", value: "SENSITIVE" },
  { label: "moderation.rejectType.other", value: "OTHER" },
];

const rejectDialogVisible = ref(false);
const rejectSubmitting = ref(false);
const currentRejectResourceId = ref<string>("");
const rejectForm = ref<{ rejectType: ModerateRejectType; rejectReason: string }>({
  rejectType: "OTHER",
  rejectReason: "",
});

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

const previewUrl = (item: ModerationResourceResp): string => {
  return item.presignedUrl?.url || "";
};

const pageTotal = (payload: Record<string, any>): number => {
  if (typeof payload.totalElements === "number") return payload.totalElements;
  if (typeof payload.total === "number") return payload.total;
  if (payload.page && typeof payload.page.totalElements === "number") return payload.page.totalElements;
  return 0;
};

const statusTagType = (status?: AuditStatus): "warning" | "success" | "danger" | "info" => {
  if (status === "PENDING") return "warning";
  if (status === "APPROVED") return "success";
  if (status === "REJECTED") return "danger";
  return "info";
};

const fetchData = async () => {
  loading.value = true;
  try {
    const [start, end] = searchForm.value.auditAtRange || [];
    const res = await listModerationResources({
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      resourceType: "IMAGE",
      taskId: searchForm.value.taskId || undefined,
      status: searchForm.value.status || undefined,
      auditorId: searchForm.value.auditorId || undefined,
      auditAtStart: start ? start.toISOString() : undefined,
      auditAtEnd: end ? end.toISOString() : undefined,
    });

    cardData.value = res.data.content || [];
    pageOpts.value.total = pageTotal(res.data as unknown as Record<string, any>);
  } catch (e) {
    console.error(e);
    ElMessage.error(t("moderation.error.fetch"));
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pageOpts.value.currentPage = 1;
  fetchData();
};

const handleReset = () => {
  pageOpts.value.currentPage = 1;
  searchForm.value = {
    taskId: "",
    status: "PENDING",
    auditorId: "",
    auditAtRange: null,
  };
  fetchData();
};

const gotoDetail = (id?: string) => {
  if (!id) return;
  router.push({ name: "moderationResourceDetail", params: { resourceId: id } });
};

const handleApproveResource = async (resourceId?: string) => {
  if (!resourceId) return;
  try {
    await ElMessageBox.confirm(t("moderation.confirm.approveResource"), t("moderation.action.approve"), {
      type: "warning",
    });
    await approveModerationResource(resourceId);
    ElMessage.success(t("moderation.success.approve"));
    await fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error(t("moderation.error.approve"));
    }
  }
};

const openRejectDialog = (resourceId?: string) => {
  if (!resourceId) return;
  currentRejectResourceId.value = resourceId;
  rejectForm.value = { rejectType: "OTHER", rejectReason: "" };
  rejectDialogVisible.value = true;
};

const submitReject = async () => {
  if (!currentRejectResourceId.value) return;
  rejectSubmitting.value = true;
  try {
    await rejectModerationResource(currentRejectResourceId.value, {
      rejectType: rejectForm.value.rejectType,
      rejectReason: rejectForm.value.rejectReason || undefined,
    });
    ElMessage.success(t("moderation.success.reject"));
    rejectDialogVisible.value = false;
    await fetchData();
  } catch (e) {
    console.error(e);
    ElMessage.error(t("moderation.error.reject"));
  } finally {
    rejectSubmitting.value = false;
  }
};

onMounted(fetchData);
</script>

<template>
  <div class="list-layout">
    <SearchContainer>
      <el-form inline class="search-form" :model="searchForm">
        <el-form-item :label="t('moderation.field.taskId')">
          <el-input v-model="searchForm.taskId" :placeholder="t('moderation.input.taskId')" />
        </el-form-item>
        <el-form-item :label="t('moderation.field.status')">
          <el-select v-model="searchForm.status" clearable style="width: 160px">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="t(item.label)"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('moderation.field.auditorId')">
          <el-input v-model="searchForm.auditorId" :placeholder="t('moderation.input.auditorId')" />
        </el-form-item>
        <el-form-item :label="t('moderation.field.auditAt')">
          <el-date-picker
            v-model="searchForm.auditAtRange"
            type="datetimerange"
            range-separator="~"
            :start-placeholder="t('moderation.input.startTime')"
            :end-placeholder="t('moderation.input.endTime')"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('common.query.title') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset.title') }}</el-button>
        </el-form-item>
      </el-form>
    </SearchContainer>

    <TableContainer
      title="moderation.workbench"
      :show-add-btn="false"
      :show-remove-btn="false"
      :total="pageOpts.total"
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
      @change="fetchData"
    >
      <div v-loading="loading" class="resource-grid-wrap">
        <div v-if="cardData.length === 0" class="empty-box">
          <el-empty :description="t('common.none.description')" />
        </div>

        <div v-else class="resource-grid">
          <el-card
            v-for="item in cardData"
            :key="item.id"
            class="resource-card"
            shadow="hover"
            @click="gotoDetail(item.id)"
          >
            <div class="image-box">
              <img
                v-if="previewUrl(item)"
                :src="previewUrl(item)"
                :alt="item.resourceKey || String(item.id || '')"
                class="preview-image"
              />
              <div v-else class="image-empty">{{ t('common.none.title') }}</div>
              <div class="hover-mask">{{ t('detail') }}</div>
            </div>

            <div class="card-main">
              <div class="title-row">
                <span class="resource-id">#{{ item.id || t('common.none.title') }}</span>
                <el-tag :type="statusTagType(item.status)">
                  {{ t(`moderation.status.${(item.status || 'pending').toLowerCase()}`) }}
                </el-tag>
              </div>

              <div class="meta-row">
                <span>{{ t('moderation.field.mimeType') }}: {{ item.mimeType || t('common.none.title') }}</span>
              </div>
              <div class="meta-row">
                <span>{{ t('moderation.field.fileSize') }}: {{ formatFileSize(item.sizeBytes) }}</span>
              </div>
              <div class="meta-row">
                <span>{{ t('moderation.field.auditAt') }}: {{ formatISOData(getInstantIso(item.auditAt)) || t('common.none.title') }}</span>
              </div>

              <div class="action-row">
                <el-button
                  size="small"
                  type="success"
                  :disabled="!item.id"
                  @click.stop="handleApproveResource(item.id)"
                >
                  {{ t('moderation.action.approve') }}
                </el-button>
                <el-button size="small" type="warning" :disabled="!item.id" @click.stop="openRejectDialog(item.id)">
                  {{ t('moderation.action.reject') }}
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </TableContainer>

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
.list-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resource-grid-wrap {
  min-height: 520px;
}

.resource-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  align-items: stretch;
}

.resource-card {
  cursor: pointer;
  border-radius: 10px;
}

.resource-card :deep(.el-card__body) {
  padding: 10px;
}

.image-box {
  position: relative;
  width: 100%;
  height: 180px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.2s ease;
}

.resource-card:hover .preview-image {
  transform: scale(1.04);
}

.image-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.hover-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.38);
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s ease;
  font-size: 13px;
}

.resource-card:hover .hover-mask {
  opacity: 1;
}

.card-main {
  margin-top: 10px;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.resource-id {
  color: #303133;
  font-weight: 600;
}

.meta-row {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  word-break: break-all;
}

.action-row {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.empty-box {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 420px;
}

@media (max-width: 768px) {
  .image-box {
    height: 210px;
  }
}
</style>
