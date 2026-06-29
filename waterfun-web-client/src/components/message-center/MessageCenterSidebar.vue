<template>
  <el-card class="sidebar-card" shadow="never">
    <h3 class="sidebar-title">{{ $t('notification.center') }}</h3>
    <ul class="menu-list">
      <li
        v-for="item in menuItems"
        :key="item.key"
        :class="['menu-item', { active: activeTab === item.key }]"
        @click="emit('tab-change', item.key)"
      >
        <span>{{ item.label }}</span>
        <el-tag v-if="item.count > 0" size="small" type="danger" round class="badge">
          {{ item.count > 99 ? '99+' : item.count }}
        </el-tag>
      </li>
    </ul>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted } from "vue"
import { useNotificationStore } from "~/stores/notificationStore"
import { useI18n } from "vue-i18n"

export type MessageTabType = "subscribe" | "reply" | "mention" | "system";

const props = defineProps<{
  activeTab: MessageTabType;
}>();

const emit = defineEmits<{
  (event: "tab-change", tab: MessageTabType): void;
}>();

const notificationStore = useNotificationStore()
const unreadCount = computed(() => notificationStore.unreadCount)
const tabCount = (key: string) => notificationStore.tabUnreadCounts[key] || 0
const { t } = useI18n()

void props;

const menuItems = computed<{ key: MessageTabType; label: string; count: number }[]>(() => [
  { key: "system", label: t('notification.system'), count: tabCount('system') },
  { key: "subscribe", label: t('notification.subscribe'), count: tabCount('subscribe') },
  { key: "reply", label: t('notification.reply'), count: tabCount('reply') },
  { key: "mention", label: t('notification.mention'), count: tabCount('mention') },
]);

onMounted(() => {
  notificationStore.fetchUnreadCount()
})
</script>

<style scoped>
.sidebar-card { border-radius: 12px; }
.sidebar-title { margin: 0 0 14px; font-size: 18px; color: #111827; }
.menu-list { list-style: none; margin: 0; padding: 0; }
.menu-item {
  display: flex; align-items: center; justify-content: space-between;
  height: 44px; padding: 0 14px; border-radius: 10px;
  color: #374151; cursor: pointer; transition: all 0.2s ease; margin-bottom: 6px;
}
.menu-item:hover { background: #f3f7ff; }
.menu-item.active { background: #e8f2ff; color: #2563eb; font-weight: 600; }
.badge { margin-left: auto; }
</style>
