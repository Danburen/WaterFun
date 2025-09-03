<script setup lang="ts">
import type {TagNavItemType} from "@/layouts/types.js";
import {ref, computed, onMounted, onUnmounted, watch, type Ref, type ComputedRef} from 'vue'
import router from "@/router/index.js";
const tagNavContainer = ref();
const activeTagName = ref('');
const dragIndex = ref<number | null>(null);
const dragPlaceholderIndex = ref<number | null>(null);
let touchStartX = 0;

const props = defineProps<{
  tagList: TagNavItemType[];
  tagGap?: number;
  modelValue: string;
}>()

const localOrder = ref(props.tagList.map(tag => tag.name))

const emit = defineEmits(['orderUpdated','tagRemoved','tagClick']);

const sortedTags = computed(() => {
  return [...localOrder.value]
      .map(name => props.tagList.find(tag => tag.name === name))
      .filter(Boolean)
})

/*Events*/
const handleClose = (tagName:string) =>{
  emit('tagRemoved',tagName);
}

const handleClick = (tagName:string) => {
  activeTagName.value = tagName;
  router.push({ name: props.tagList.find(tag => tag.name === tagName).to });
  emit('tagClick',tagName);
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
        Math.max(0,Math.min(newIndex, props.tagList.length)) // ensure not below zero or ubound
}

const handleDrop = () => {
  if (dragIndex.value === null || dragPlaceholderIndex.value === null) return;
  let source = dragIndex.value
  let target = dragPlaceholderIndex.value
  if (target > source) {
    target -= 1; // fix ubound
  }
  if (source === target) {
    dragIndex.value = null
    dragPlaceholderIndex.value = null
    return
  }
  const newOrder = [...localOrder.value]
  const [draggedItem] = newOrder.splice(dragIndex.value, 1)
  newOrder.splice(dragPlaceholderIndex.value,0 ,draggedItem)
  localOrder.value = newOrder
  emit('orderUpdated',newOrder)
  dragIndex.value = null
  dragPlaceholderIndex.value = null
}

const setCurrentName = (name?:string) => {
  if(name === null) return
  if(sortedTags.value.some(tag => tag.name === name)){
    activeTagName.value = name
  }
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

watch(
    () => props.tagList,
    (newList) => {
      localOrder.value = newList.map(tag => tag.name);
      activeTagName.value = props.modelValue;
    },
    { immediate: true, deep: true }
);

watch(()=> props.modelValue,
    ()=>activeTagName.value = props.modelValue,
{immediate: true},)
</script>

<template>
    <div class="tag-container no-scroll-bar" :style="{ gap: `${tagGap || 6}px` }" ref="tagNavContainer">
      <template v-for="(tag, index) in sortedTags" :key="index">
        <div
            v-if="dragPlaceholderIndex === index"
            class="tag-placeholder"
        />
        <el-tag
            class="tag"
            :effect="activeTagName  === tag.name ? 'light' : 'plain'"
            :class="{ 'tag-dragging': dragIndex === index,
                      'active': activeTagName === tag.name
                    }"
            :closable="tag.closeable"
            draggable="true"
            @close="handleClose(tag.name)"
            @click="handleClick(tag.name)"
            @dragstart="dragIndex = index"
            @dragover="handleDragOver"
            @dragend="handleDrop"
            @drop="handleDrop"
        >
          {{ $t(tag.locale) }}
        </el-tag>
        <div
            v-if="dragPlaceholderIndex === sortedTags.length && index === sortedTags.length - 1"
            class="tag-placeholder"
        />
      </template>
    </div>
</template>

<style scoped>
.tag-container {
  box-sizing: border-box;
  height: 38px;
  display: flex;
  align-items: center;
  overflow-x: auto;
  overflow-y: hidden; /* 禁止垂直滚动 */
  min-width: 0;
  width: 100%;
  gap: 6px;
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

.tag-dragging {
  opacity: 0.5;
}

.tag.active {
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.1);
}

.tag-placeholder {
  width: 60px;
  height: 28px;
  background-color: var(--light-background);
  border: 2px dashed var(--default-blue);
  border-radius: 4px;
  flex-shrink: 0;
}
</style>