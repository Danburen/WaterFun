<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="(v) => emit('update:modelValue', v)"
    :title="title"
    width="80%"
    destroy-on-close
  >
    <div class="preview-wrap">
      <img v-if="url" class="preview-img" :src="url" alt="Preview" />
      <div v-else class="preview-empty">No image</div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    url?: string;
    title?: string;
  }>(),
  {
    url: '',
    title: 'Preview',
  }
);

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
}>();

const { modelValue, url, title } = toRefs(props);
</script>

<style scoped>
.preview-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  min-height: 200px;
  overflow: auto;
}

.preview-img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.preview-empty {
  color: var(--el-text-color-secondary);
}
</style>
