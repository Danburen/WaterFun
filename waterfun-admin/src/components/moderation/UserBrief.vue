<script setup lang="ts">
import type { UserBriefWithStats } from "~/api/tickets";
import { avatarUrl } from "~/composables/useModeration";

const props = withDefaults(
  defineProps<{
    user?: UserBriefWithStats | null;
    size?: "sm" | "md" | "lg";
    /** Show warning styling (used for reported-user) */
    warn?: boolean;
  }>(),
  { size: "md", warn: false },
);

const avatarSize = { sm: 36, md: 40, lg: 48 };
const iconSizes = { sm: 14, md: 16, lg: 18 };
</script>

<template>
  <div class="ub-wrap" :class="[`ub-${size}`, { 'ub-warn': warn }]">
    <div class="ub-avatar" :style="{ width: avatarSize[size] + 'px', height: avatarSize[size] + 'px' }">
      <img v-if="avatarUrl(user?.avatar)" :src="avatarUrl(user?.avatar)!" alt="avatar">
      <i v-else class="fa-solid fa-user" :style="{ fontSize: iconSizes[size] + 'px' }"></i>
    </div>
    <div class="ub-info">
      <div class="ub-name">{{ user?.displayName || user?.nickname || "用户" }}</div>
      <div class="ub-meta">
        UID: {{ user?.uid ?? "-" }} · LV.{{ user?.level || "-" }}
        <template v-if="user?.registrationDate"> · 注册 {{ user.registrationDate.substring(0, 10) }}</template>
      </div>
    </div>
    <slot name="extra" />
  </div>
</template>

<style scoped>
.ub-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-white);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-light);
}
.ub-avatar {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-light);
  color: var(--primary);
  flex-shrink: 0;
  overflow: hidden;
}
.ub-warn .ub-avatar { background: var(--warning-light); color: #b45309; }
.ub-avatar img { width: 100%; height: 100%; object-fit: cover; }
.ub-info { flex: 1; min-width: 0; }
.ub-name { font-weight: 600; color: var(--text-primary); }
.ub-meta { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

.ub-sm { gap: 8px; }
.ub-sm .ub-info { padding: 0; }
.ub-sm .ub-name { font-size: 13px; }
.ub-sm .ub-meta { font-size: 11px; }
.ub-sm .ub-avatar { width: 36px; height: 36px; }
.ub-sm { padding: 8px; }

.ub-md .ub-name { font-size: 14px; }
.ub-md .ub-meta { font-size: 12px; }

.ub-lg .ub-name { font-size: 15px; }
.ub-lg .ub-meta { font-size: 12px; }
.ub-lg { padding: 12px; }
</style>
