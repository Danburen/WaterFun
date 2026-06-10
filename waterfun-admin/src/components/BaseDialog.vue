<script setup lang="ts">
const props = withDefaults(defineProps<{
  modelValue: boolean;
  title: string;
  width?: string;
}>(), {
  width: '640px',
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  close: [];
}>()

const visible = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const handleOverlay = (e: MouseEvent) => { if ((e.target as HTMLElement).classList.contains('dialog-overlay')) visible.value = false }
</script>

<template>
  <Teleport to="body">
    <transition name="dialog-fade">
      <div v-if="visible" class="dialog-overlay" @click="handleOverlay">
        <div class="dialog-panel" :style="{ maxWidth: width }">
          <div class="dialog-header">
            <span class="dialog-title">{{ title }}</span>
            <button class="dialog-close" @click="visible = false">&times;</button>
          </div>
          <div class="dialog-body">
            <slot />
          </div>
          <div v-if="$slots.footer" class="dialog-footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>
