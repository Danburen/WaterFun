<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessageBox, ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { User, Clock, Files } from "@element-plus/icons-vue";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import {
  listModerations,
  approveModerationById,
  rejectModerationById,
  approveModerations,
  rejectModerations,
  type ModerateTaskResp,
  type TargetType,
  type ModerateRejectType,
  type AuditStatus,
} from "~/api/moderation";
import { useDictStore } from "~/stores/dictStore";
import type { PageOptions } from "~/types/api";

const router = useRouter();
const dictStore = useDictStore();

const loading = ref(false);
const taskData = ref<ModerateTaskResp[]>([]);

const searchForm = ref<{
  taskType: TargetType | "";
  submitterId: string;
  submitAtRange: [Date, Date] | null;
}>({
  taskType: "",
  submitterId: "",
  submitAtRange: null,
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 12,
  total: 0,
});

const selectedIds = ref<number[]>([]);

const targetTypeOptions: Array<{ label: string; value: TargetType }> = [
  { label: "帖子", value: "POST" },
  { label: "用户头像", value: "USER_AVATAR" },
  { label: "帖子封面图", value: "POST_COVERAGE_IMAGE" },
  { label: "帖子内容图", value: "POST_CONTENT_IMAGE" },
  { label: "帖子内容", value: "POST_CONTENT" },
  { label: "未知", value: "UNKNOWN" },
];

const rejectTypeOptions: Array<{ label: string; value: ModerateRejectType }> = [
  { label: "违反社区准则", value: "VIOLATION_OF_GUIDELINES" },
  { label: "不当内容", value: "INAPPROPRIATE_CONTENT" },
  { label: "广告", value: "ADVERTISEMENT" },
  { label: "暴力内容", value: "VIOLENCE" },
  { label: "敏感内容", value: "SENSITIVE" },
  { label: "其他", value: "OTHER" },
];

const rejectDialogVisible = ref(false);
const rejectBatchMode = ref(false);
const rejectSubmitting = ref(false);
const currentRejectTaskId = ref<number | null>(null);
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

const pageTotal = (payload: Record<string, unknown>): number => {
  if (payload.page && typeof (payload.page as Record<string, unknown>).totalElements === "number")
    return (payload.page as Record<string, unknown>).totalElements as number;
  if (typeof payload.totalElements === "number") return payload.totalElements;
  return 0;
};

const statusTagType = (status?: AuditStatus): "warning" | "success" | "danger" | "info" => {
  if (status === "PENDING") return "warning";
  if (status === "APPROVED") return "success";
  if (status === "REJECTED") return "danger";
  return "info";
};

const parsePostMeta = (meta?: Record<string, unknown>): { title?: string; summary?: string } | null => {
  if (!meta) return null;
  return meta as { title?: string; summary?: string };
};

const getPostTitle = (task: ModerateTaskResp): string => {
  const payload = task.payload;
  if (!payload) return "审核任务 #" + (task.id ?? "?");
  const meta = parsePostMeta(payload.meta);
  if (meta?.title) return meta.title;
  if (payload.type === "RICH_TEXT" || payload.type === "PLAIN_TEXT") {
    if (payload.content) return payload.content.substring(0, 60);
    return "[内容审核]";
  }
  if (payload.type === "SINGLE_RESOURCE") {
    return "[资源审核] " + (payload.singleResource?.resourceUuid?.substring(0, 16) ?? "");
  }
  return "审核任务 #" + (task.id ?? "?");
};

const getContentPreview = (task: ModerateTaskResp): string => {
  const payload = task.payload;
  if (!payload) return "";
  if (payload.type === "RICH_TEXT" || payload.type === "PLAIN_TEXT") {
    const meta = parsePostMeta(payload.meta);
    if (meta?.summary) return meta.summary;
    if (payload.content) return payload.content.substring(0, 120);
    return "";
  }
  return "";
};

const getResourceCount = (task: ModerateTaskResp): number => {
  return task.payload?.resources?.length ?? 0;
};

const fetchData = async () => {
  loading.value = true;
  try {
    const [start, end] = searchForm.value.submitAtRange || [];
    const res = await listModerations({
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      taskType: searchForm.value.taskType || undefined,
      submitterId: searchForm.value.submitterId ? Number(searchForm.value.submitterId) : undefined,
      submitAtStart: start ? start.toISOString() : undefined,
      submitAtEnd: end ? end.toISOString() : undefined,
    });

    taskData.value = res.data.content || [];
    taskData.value.sort((a, b) => {
      const aStatus = (a as any)._computedStatus || "";
      const bStatus = (b as any)._computedStatus || "";
      const aPending = a.payload?.type ? 0 : 1;
      const bPending = b.payload?.type ? 0 : 1;
      return aPending - bPending;
    });
    pageOpts.value.total = pageTotal(res.data as unknown as Record<string, unknown>);
  } catch (e) {
    console.error(e);
    ElMessage.error("获取审核任务失败");
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
    taskType: "",
    submitterId: "",
    submitAtRange: null,
  };
  fetchData();
};

const toggleSelection = (id: number, checked: boolean) => {
  if (checked) {
    selectedIds.value.push(id);
  } else {
    selectedIds.value = selectedIds.value.filter(i => i !== id);
  }
};

const gotoDetail = (task: ModerateTaskResp) => {
  if (!task.id) return;
  router.push({
    name: "moderationTaskDetail",
    params: { taskId: task.id },
  });
};

const handleApproveTask = async (taskId?: number) => {
  if (!taskId) return;
  try {
    await ElMessageBox.confirm("确定通过该审核任务吗？", "通过", { type: "warning" });
    await approveModerationById(taskId);
    ElMessage.success("审核通过成功");
    await fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error("审核通过失败");
    }
  }
};

const handleBatchApprove = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning("请先选择要审核的任务");
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确定批量通过 ${selectedIds.value.length} 个审核任务吗？`,
      "批量通过",
      { type: "warning" }
    );
    await approveModerations({ auditTaskIds: selectedIds.value });
    ElMessage.success("批量审核通过成功");
    selectedIds.value = [];
    await fetchData();
  } catch (e) {
    if (e !== "cancel") {
      console.error(e);
      ElMessage.error("批量审核通过失败");
    }
  }
};

const openRejectDialog = (taskId?: number) => {
  rejectBatchMode.value = false;
  currentRejectTaskId.value = taskId ?? null;
  rejectForm.value = { rejectType: "OTHER", rejectReason: "" };
  rejectDialogVisible.value = true;
};

const openBatchRejectDialog = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning("请先选择要驳回的任务");
    return;
  }
  rejectBatchMode.value = true;
  currentRejectTaskId.value = null;
  rejectForm.value = { rejectType: "OTHER", rejectReason: "" };
  rejectDialogVisible.value = true;
};

const submitReject = async () => {
  rejectSubmitting.value = true;
  try {
    if (rejectBatchMode.value) {
      const res = await rejectModerations({
        auditTaskIds: selectedIds.value,
        rejectType: rejectForm.value.rejectType,
        rejectReason: rejectForm.value.rejectReason || undefined,
      });
      if (res.data) {
        ElMessage.success(`批量驳回成功: ${res.data.success}, 失败: ${res.data.failed}`);
      } else {
        ElMessage.success("批量驳回成功");
      }
      selectedIds.value = [];
    } else {
      if (!currentRejectTaskId.value) return;
      await rejectModerationById(currentRejectTaskId.value, {
        rejectType: rejectForm.value.rejectType,
        rejectReason: rejectForm.value.rejectReason || undefined,
      });
      ElMessage.success("审核驳回成功");
    }
    rejectDialogVisible.value = false;
    await fetchData();
  } catch (e) {
    console.error(e);
    ElMessage.error("审核驳回失败");
  } finally {
    rejectSubmitting.value = false;
  }
};

const targetTypeLabel = (type?: TargetType): string => {
  if (!type) return "未知";
  const map: Record<string, string> = {
    POST: "帖子",
    USER_AVATAR: "用户头像",
    POST_COVERAGE_IMAGE: "帖子封面图",
    POST_CONTENT_IMAGE: "帖子内容图",
    POST_CONTENT: "帖子内容",
    UNKNOWN: "未知",
  };
  return map[type] || type;
};

onMounted(async () => {
  dictStore.ensureLoaded();
  fetchData();
});
</script>

<template>
  <div class="list-layout">
    <SearchContainer>
      <el-form inline class="search-form" :model="searchForm">
        <el-form-item label="任务类型">
          <el-select v-model="searchForm.taskType" clearable style="width: 160px">
            <el-option
              v-for="item in targetTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="提交人ID">
          <el-input v-model="searchForm.submitterId" placeholder="请输入提交人ID" />
        </el-form-item>
        <el-form-item label="提交时间">
          <el-date-picker
            v-model="searchForm.submitAtRange"
            type="datetimerange"
            range-separator="~"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </SearchContainer>

    <TableContainer
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
      title="审核工作台"
      :show-add-btn="false"
      :show-remove-btn="false"
      :total="pageOpts.total"
      @change="fetchData"
    >
      <template #action-buttons>
        <el-button type="success" size="small" :disabled="selectedIds.length === 0" @click="handleBatchApprove">
          批量通过
        </el-button>
        <el-button type="warning" size="small" :disabled="selectedIds.length === 0" @click="openBatchRejectDialog">
          批量驳回
        </el-button>
      </template>

      <div v-loading="loading" class="task-grid-wrap">
        <div v-if="taskData.length === 0" class="empty-box">
          <el-empty description="暂无数据" />
        </div>

        <div v-else class="task-list">
          <el-card
            v-for="task in taskData"
            :key="task.id"
            class="task-card"
            shadow="hover"
          >
            <div class="task-card-inner">
              <el-checkbox
                v-if="task.id"
                :model-value="selectedIds.includes(task.id!)"
                @change="(val: boolean) => toggleSelection(task.id!, val)"
                @click.stop
                class="card-checkbox"
              />

              <div class="card-body" @click="gotoDetail(task)">
                <div class="card-header">
                  <div class="title-area">
                    <span class="task-id-badge">#{{ task.id }}</span>
                    <span class="post-title">{{ getPostTitle(task) }}</span>
                  </div>
                  <div class="header-tags">
                    <el-tag size="small" :type="statusTagType('PENDING')" v-if="!task.id">待审核</el-tag>
                    <el-tag size="small" type="info">{{ targetTypeLabel(task.targetType) }}</el-tag>
                  </div>
                </div>

                <div v-if="getContentPreview(task)" class="content-preview">
                  {{ getContentPreview(task) }}
                </div>

                <div class="card-meta">
                  <span class="meta-item">
                    <el-icon><User /></el-icon>
                    提交人 #{{ task.submitterId || "?" }}
                  </span>
                  <span class="meta-divider">|</span>
                  <span class="meta-item">
                    <el-icon><Clock /></el-icon>
                    {{ formatISOData(getInstantIso(task.submitAt)) || "未知" }}
                  </span>
                  <span v-if="getResourceCount(task) > 0" class="meta-divider">|</span>
                  <span v-if="getResourceCount(task) > 0" class="meta-item">
                    <el-icon><Files /></el-icon>
                    {{ getResourceCount(task) }} 个资源
                  </span>
                  <span v-if="task.targetId" class="meta-divider">|</span>
                  <span v-if="task.targetId" class="meta-item">
                    目标ID: {{ task.targetId }}
                  </span>
                </div>
              </div>

              <div class="card-actions">
                <el-button size="small" type="primary" plain @click.stop="gotoDetail(task)">
                  详情
                </el-button>
                <el-button
                  size="small"
                  type="success"
                  :disabled="!task.id"
                  @click.stop="handleApproveTask(task.id)"
                >
                  通过
                </el-button>
                <el-button
                  size="small"
                  type="warning"
                  :disabled="!task.id"
                  @click.stop="openRejectDialog(task.id)"
                >
                  驳回
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </TableContainer>

    <el-dialog v-model="rejectDialogVisible" title="驳回审核任务" width="520">
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
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因（可选）"
          />
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
.list-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-grid-wrap {
  min-height: 520px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-card {
  border-radius: 8px;
  transition: box-shadow 0.2s ease;
}

.task-card-inner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.task-card :deep(.el-card__body) {
  padding: 16px;
}

.card-checkbox {
  padding-top: 4px;
  flex-shrink: 0;
}

.card-body {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.title-area {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.task-id-badge {
  font-size: 12px;
  color: #909399;
  background: #f0f2f5;
  padding: 0 8px;
  border-radius: 4px;
  line-height: 22px;
  white-space: nowrap;
  flex-shrink: 0;
}

.post-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-tags {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.content-preview {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 10px;
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}

.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.meta-divider {
  color: #dcdfe6;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
  padding-top: 2px;
}

.empty-box {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 420px;
}
</style>
