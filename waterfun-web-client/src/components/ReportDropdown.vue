<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'

const props = withDefaults(defineProps<{
  targetType: 'POST' | 'COMMENT' | 'USER'
  targetId: string
  placement?: 'bottom-end' | 'bottom-start'
}>(), {
  placement: 'bottom-end',
})

const emit = defineEmits<{
  report: [payload: { targetType: 'POST' | 'COMMENT' | 'USER'; targetId: string }]
}>()

const open = ref(false)
const containerRef = ref<HTMLElement | null>(null)
const menuRef = ref<HTMLElement | null>(null)
const triggerRef = ref<HTMLElement | null>(null)

const toggle = () => {
  open.value = !open.value
}

const handleReport = () => {
  open.value = false
  emit('report', { targetType: props.targetType, targetId: props.targetId })
}

const handleClickOutside = (e: MouseEvent) => {
  if (!open.value) return
  const target = e.target as Node
  if (
    containerRef.value?.contains(target)
  ) return
  open.value = false
}

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

watch(open, (val) => {
  if (val) {
    document.addEventListener('click', handleClickOutside)
  } else {
    document.removeEventListener('click', handleClickOutside)
  }
})
</script>

<template>
  <div class="report-dropdown" ref="containerRef">
    <button
      ref="triggerRef"
      class="report-trigger"
      title="更多"
      @click.stop="toggle"
    >
      <i class="fas fa-ellipsis-v"></i>
    </button>
    <div
      v-if="open"
      ref="menuRef"
      :class="['report-menu', placement === 'bottom-start' ? 'menu-bottom-start' : 'menu-bottom-end']"
      @click.stop
    >
      <button class="report-menu-item report-action" @click="handleReport">
        <i class="fas fa-flag"></i>
        <span>举报</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.report-dropdown {
  position: relative;
  display: inline-flex;
}

.report-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--wf-text-muted, #94a3b8);
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.report-trigger:hover {
  background: var(--wf-border-light, #f1f5f9);
  color: var(--wf-text-secondary, #64748b);
}

.report-menu {
  position: absolute;
  z-index: 2000;
  min-width: 100px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  padding: 4px;
  animation: menu-fade-in 0.15s ease;
}

.menu-bottom-end {
  top: 100%;
  right: 0;
  margin-top: 4px;
}

.menu-bottom-start {
  top: 100%;
  left: 0;
  margin-top: 4px;
}

.report-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.report-menu-item:hover {
  background: #fef2f2;
  color: #ef4444;
}

.report-menu-item i {
  font-size: 12px;
  width: 16px;
  text-align: center;
}

@keyframes menu-fade-in {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
