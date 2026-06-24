<script setup lang="ts">
const emit = defineEmits<{
  select: [emoji: string]
}>()

const emojis = [
  '😀', '😂', '🤣', '😊', '😍', '🥰', '😘', '😜',
  '😎', '🤩', '🥳', '😢', '😭', '😤', '😡', '🤯',
  '👍', '👎', '👏', '🙌', '💪', '🤝', '❤️', '💔',
  '🔥', '⭐', '💯', '✅', '❌', '🎉', '🎊', '🙏',
]

const visible = ref(false)
const pickerRef = ref<HTMLElement | null>(null)

const toggle = () => {
  visible.value = !visible.value
}

const selectEmoji = (emoji: string) => {
  emit('select', emoji)
  visible.value = false
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

const handleClickOutside = (e: MouseEvent) => {
  if (pickerRef.value && !pickerRef.value.contains(e.target as Node)) {
    visible.value = false
  }
}
</script>

<template>
  <div ref="pickerRef" class="emoji-picker-wrap">
    <button class="reply-tool-btn" title="表情" @click="toggle">
      <i class="far fa-smile"></i>
    </button>
    <div v-if="visible" class="emoji-dropdown">
      <button
        v-for="emoji in emojis"
        :key="emoji"
        class="emoji-item"
        @click="selectEmoji(emoji)"
      >
        {{ emoji }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.emoji-picker-wrap {
  position: relative;
  display: inline-block;
}

.reply-tool-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--wf-text-muted);
  border-radius: 6px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: var(--wf-transition);
}

.reply-tool-btn:hover {
  color: var(--wf-primary);
  background: #eff6ff;
}

.emoji-dropdown {
  position: absolute;
  bottom: 100%;
  left: 0;
  margin-bottom: 4px;
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 2px;
  padding: 8px;
  background: var(--wf-bg-white);
  border: 1px solid var(--wf-border);
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  z-index: 100;
  width: 280px;
}

.emoji-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
  padding: 0;
  transition: background 0.15s;
}

.emoji-item:hover {
  background: #f0f0f0;
}
</style>
