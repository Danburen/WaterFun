<template>
  <el-card class="module-card" shadow="never">
    <template #header>
      <div class="module-header">
        <h3>系统通知</h3>
      </div>
    </template>

    <div class="system-list" @scroll.passive="onSystemScroll">
      <el-card
        v-for="(item, idx) in systemNotifications"
        :key="`${item.id ?? 'system'}-${idx}`"
        class="notification-card"
        shadow="hover"
      >
        <div class="notification-header">
          <div class="title-wrap">
            <span class="notification-title">{{ item.title || "系统通知" }}</span>
            <span v-if="item.isRead === false" class="unread-dot" />
          </div>
          <span class="notification-time">{{ formatInstant(item.createdAt) }}</span>
        </div>
        <div class="notification-content">{{ item.content }}</div>
      </el-card>

      <el-skeleton v-if="firstLoading" :rows="5" animated class="skeleton" />
      <el-empty
        v-else-if="!firstLoading && systemNotifications.length === 0"
        description="暂无系统通知"
      />

      <div v-if="systemLoading" class="list-footer">正在加载更多...</div>
      <div v-else-if="!hasNext && systemNotifications.length > 0" class="list-footer">已经到底了</div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { ElMessage } from "element-plus";
import {
  listSystemNotifications,
  type InstantDto,
  type SystemNotificationRes,
} from "~/api/notificationApi";

const props = defineProps<{
  active: boolean;
}>();

const systemNotifications = ref<SystemNotificationRes[]>([]);
const nextCursor = ref<number | undefined>(undefined);
const hasNext = ref(true);
const firstLoading = ref(false);
const systemLoading = ref(false);
const pageLimit = 20;
const loadedOnce = ref(false);

const formatInstant = (instant?: InstantDto): string => {
  if (!instant || typeof instant.seconds !== "number") {
    return "-";
  }
  const millis = instant.seconds * 1000 + Math.floor((instant.nanos || 0) / 1_000_000);
  return new Date(millis).toLocaleString("zh-CN", { hour12: false });
};

const loadSystemNotifications = async (reset = false) => {
  if (systemLoading.value) return;
  if (!hasNext.value && !reset) return;

  if (reset) {
    firstLoading.value = true;
    hasNext.value = true;
    nextCursor.value = undefined;
    systemNotifications.value = [];
  }

  systemLoading.value = true;
  try {
    const res = await listSystemNotifications({
      cursor: nextCursor.value,
      limit: pageLimit,
    });
    const page = res.data;
    const list = page?.list || [];

    systemNotifications.value = reset ? list : [...systemNotifications.value, ...list];
    nextCursor.value = page?.nextCursor ?? undefined;
    hasNext.value = Boolean(page?.hasNext);
    loadedOnce.value = true;
  } catch (error) {
    console.error("load system notifications failed", error);
    ElMessage.error("加载系统通知失败");
  } finally {
    firstLoading.value = false;
    systemLoading.value = false;
  }
};

const onSystemScroll = (event: Event) => {
  const target = event.target as HTMLDivElement;
  const threshold = 60;
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - threshold) {
    loadSystemNotifications();
  }
};

watch(
  () => props.active,
  (active) => {
    if (active && !loadedOnce.value) {
      loadSystemNotifications(true);
    }
  },
  { immediate: true }
);
</script>

<style scoped>
.module-card {
  border-radius: 12px;
}

.module-header h3 {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.system-list {
  height: calc(100vh - 240px);
  min-height: 460px;
  overflow-y: auto;
  padding-right: 8px;
}

.notification-card {
  margin-bottom: 14px;
  border-radius: 10px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.notification-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
}

.notification-time {
  font-size: 13px;
  color: #6b7280;
  white-space: nowrap;
}

.notification-content {
  margin-top: 10px;
  color: #374151;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.list-footer {
  text-align: center;
  color: #6b7280;
  padding: 14px 0 8px;
  font-size: 13px;
}

.skeleton {
  margin-top: 12px;
}

@media (max-width: 768px) {
  .system-list {
    height: auto;
    min-height: 360px;
  }
}
</style>
