<script setup lang="ts">
import { ref, computed } from 'vue'
const props = withDefaults(defineProps<{
    modelValue: boolean;
    title: string;
    width?: string;
    destroyOnClose?: boolean;
    confirmButtonText?: string;
    cancelButtonText?: string;
}>(), {
    modelValue: false,
    title: '提示',
    width: '30%',
    destroyOnClose: true,
    confirmButtonText: '确定',
    cancelButtonText: '取消',
})

const visible = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)    
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  confirm: [];
  cancel: [];
  close: [];
}>();
</script>
<template>
    <el-dialog
        v-model="visible"
        :title="title"
        :width="width"
        :destroy-on-close="destroyOnClose"
        :confirm-button-text="confirmButtonText"
        :cancel-button-text="cancelButtonText"
        @close="emit('close')"
    >
    <slot></slot>
    <template #footer>
        <el-button type="primary" @click="emit('confirm')">{{ confirmButtonText }}</el-button>
        <el-button @click="emit('cancel')">{{ cancelButtonText }}</el-button>
    </template>
    </el-dialog>
</template>
<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>