<script setup lang="ts">
import type {TagItem} from "@/types/TagItemsType";
import { ref,onMounted,onUnmounted } from 'vue'
const tagNavContainer = ref();
const dragIndex = ref<number | null>(null);
const dragPlaceholderIndex = ref<number | null>(null);

let touchStartX = 0;
const props = defineProps<{
  modelValue: TagItem[];
  tagGap?: number;
}>()

const emit = defineEmits(['update:modelValue'])

const handleClose = (tagInd:number) =>{
  const newList = [...props.modelValue]
  newList.splice(tagInd, 1);
  emit('update:modelValue',newList);
}

const handleClick = (to:string) => {
  console.log(to);
}

/*Control*/
const handleWheel = (e) => {
  if(! tagNavContainer.value) return
  const scroll = tagNavContainer.value
  scroll.scrollLeft = scroll.scrollLeft + e.deltaY
}

const handleTouchStart = (e) => {
  if (!tagNavContainer.value) return
  touchStartX = e.touches[0].clientX
}

const handleTouchMove = (e) => {
  if (!tagNavContainer.value) return
  e.preventDefault()
  const touchX = e.touches[0].clientX
  const diffX = touchStartX - touchX
  tagNavContainer.value.scrollLeft += diffX * 1.5
  touchStartX = touchX
}

/*Drag*/
const handleDragOver = (e: DragEvent) => {
  e.preventDefault()
  if (! dragIndex) return
  const container = tagNavContainer.value
  const containerRect = container.getBoundingClientRect()
  const x = e.clientX - containerRect.left + container.scrollLeft
  let newIndex = 0
  let accumulatedWidth = 0
  const tags = container.querySelectorAll('.tag')
  tags.forEach((tag:Element,index:number) => {
    const tagRect = tag.getBoundingClientRect()
    const tagWidthWithGap = tagRect.width + (props.tagGap || 6)
    if(x > accumulatedWidth + tagWidthWithGap /2) newIndex = index + 1
    accumulatedWidth += tagWidthWithGap
  })
  dragPlaceholderIndex.value =
      Math.max(0,Math.min(newIndex, props.modelValue.length)) // ensure not below zero or ubound
}

const handleDrop = () => {
  if (dragIndex.value === null || dragPlaceholderIndex.value === null) return;
  if (dragIndex.value === dragPlaceholderIndex.value) {
    dragIndex.value = null
    dragPlaceholderIndex.value = null
    return
  }
  const newList = [...props.modelValue]
  const [draggedItem] = newList.splice(dragIndex.value, 1)
  newList.splice(dragPlaceholderIndex.value,0 ,draggedItem)
  emit('update:modelValue',newList)
  dragIndex.value = null
  dragPlaceholderIndex.value = null
}

onMounted(() => {
  const container = tagNavContainer.value
  if (container) {
    container.addEventListener('wheel', handleWheel, { passive: false })
    container.addEventListener('touchstart', handleTouchStart, { passive: false })
    container.addEventListener('touchmove', handleTouchMove, { passive: false })
  }
})

onUnmounted(() => {
  const container = tagNavContainer.value
  if (container) {
    container.removeEventListener('wheel', handleWheel)
    container.removeEventListener('touchstart', handleTouchStart)
    container.removeEventListener('touchmove', handleTouchMove)
  }
})
</script>

<template>
  <div class="tag-nav">
    <div class="tag-container no-scroll-bar" :style="{ gap: `${tagGap || 6}px` }" ref="tagNavContainer">
      <template v-for="(tag, index) in modelValue" :key="index">
        <div
            v-if="dragPlaceholderIndex === index"
            class="tag-placeholder"
        />
        <el-tag
            class="tag"
            :class="{ 'tag-dragging': dragIndex === index }"
            closable
            draggable="true"
            @close="handleClose(index)"
            @click="handleClick(tag.to)"
            @dragstart="dragIndex = index"
            @dragover="handleDragOver"
            @dragend="handleDrop"
            @drop="handleDrop"
        >
          {{ $t(tag.locale) }}
        </el-tag>
      </template>
    </div>
  </div>
</template>

<style scoped>
.tag-container {
  box-sizing: border-box;
  height: 36px;
  display: flex;
  align-items: center;
  overflow-x: auto;
  overflow-y: hidden; /* 禁止垂直滚动 */
  min-width: 0;
  width: 100%;
  gap: 6px;
  transition: all 1s ease;
}

.tag {
  height: 28px;
  user-select: none;
}

.tag-nav {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
}
.tag:active {
  cursor: grabbing;
}

.tag-dragging {
  opacity: 0.5;
}

.tag-placeholder {
  width: 60px;
  height: 28px;
  background-color: #f0f7ff;
  border: 2px dashed #409eff;
  border-radius: 4px;
  flex-shrink: 0;
}
</style>