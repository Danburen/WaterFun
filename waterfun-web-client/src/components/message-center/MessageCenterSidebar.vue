<template>
  <el-card class="sidebar-card" shadow="never">
    <h3 class="sidebar-title">消息中心</h3>
    <ul class="menu-list">
      <li
        v-for="item in menuItems"
        :key="item.key"
        :class="['menu-item', { active: activeTab === item.key }]"
        @click="emit('tab-change', item.key)"
      >
        <span>{{ item.label }}</span>
      </li>
    </ul>
  </el-card>
</template>

<script setup lang="ts">
export type MessageTabType = "subscribe" | "reply" | "mention" | "system";

const props = defineProps<{
  activeTab: MessageTabType;
}>();

const emit = defineEmits<{
  (event: "tab-change", tab: MessageTabType): void;
}>();

void props;

const menuItems: { key: MessageTabType; label: string }[] = [
  { key: "system", label: "系统通知" },
  { key: "subscribe", label: "订阅消息" },
  { key: "reply", label: "回复我的" },
  { key: "mention", label: "提及消息" },
];
</script>

<style scoped>
.sidebar-card {
  border-radius: 12px;
}

.sidebar-title {
  margin: 0 0 14px;
  font-size: 18px;
  color: #111827;
}

.menu-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.menu-item {
  display: flex;
  align-items: center;
  height: 44px;
  padding: 0 14px;
  border-radius: 10px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 6px;
}

.menu-item:hover {
  background: #f3f7ff;
}

.menu-item.active {
  background: #e8f2ff;
  color: #2563eb;
  font-weight: 600;
}
</style>
