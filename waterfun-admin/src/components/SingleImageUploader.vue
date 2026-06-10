<template>
  <BaseDialog :model-value="visible" @update:model-value="v => emit('update:visible', v)" :title="title" width="420px">
    <div class="single-image-uploader">
      <div class="preview" @click="!disabled && triggerPick()">
        <img v-if="localPreviewUrl" class="preview-img" :src="localPreviewUrl" alt="" />
        <div v-else class="preview-placeholder">
          <div>选择图片</div>
          <div class="preview-sub">支持常见图片格式</div>
        </div>
      </div>
      <div class="actions">
        <input ref="fileInputRef" class="file-input" type="file" accept="image/*" :disabled="disabled || uploading" @change="handlePicked" />
        <button class="btn" :disabled="disabled || uploading" @click="triggerPick">{{ localPreviewUrl ? '重新选择' : '选择图片' }}</button>
        <button v-if="localPreviewUrl" class="btn" :disabled="disabled || uploading" @click="clearSelected">清除</button>
      </div>
      <div v-if="uploading" class="upload-tip"><i class="fa-solid fa-spinner fa-spin"></i> 上传中...</div>
    </div>
    <template #footer>
      <button class="btn" @click="emit('update:visible', false)" :disabled="uploading">取消</button>
      <button class="btn btn-primary" :disabled="uploading" @click="handleConfirm">
        <i v-if="uploading" class="fa-solid fa-spinner fa-spin"></i> 确定
      </button>
    </template>
  </BaseDialog>
</template>

<script lang="ts" setup>
import { ElMessage } from 'element-plus'
import { uploadCallback } from '~/api/resource'
import BaseDialog from '~/components/BaseDialog.vue'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
export type PresignedUploadResp = { url: string; method: HttpMethod; token: string }

const props = withDefaults(defineProps<{
  visible: boolean; title?: string; disabled?: boolean; maxSizeMB?: number;
  resourceKey?: string; uploadToken?: string; previewUrl?: string;
  getUploadPolicy: (suffix: string) => Promise<{ data: PresignedUploadResp }> | Promise<PresignedUploadResp>
  onUploaded?: (payload: { token: string; previewUrl: string }) => void | Promise<void>
}>(), { title: '上传图片', disabled: false, maxSizeMB: 5, resourceKey: '', uploadToken: '', previewUrl: '' })

const emit = defineEmits<{
  'update:visible': [value: boolean]; 'update:resourceKey': [value: string]; 'update:uploadToken': [value: string];
  'update:previewUrl': [value: string]; submitted: [payload: { token: string; previewUrl: string }]
}>()

const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const localPreviewUrl = ref(props.previewUrl || '')
let ownedPendingObjectUrl: string | null = null
let lastEmittedObjectUrl: string | null = null

watch(() => props.previewUrl, (v) => { if (typeof v === 'string' && v !== localPreviewUrl.value) localPreviewUrl.value = v })

const triggerPick = () => { if (!props.disabled && !uploading.value) fileInputRef.value?.click() }

const validateFile = (file: File): boolean => {
  if (!file.type?.startsWith('image/')) { ElMessage.error('只能选择图片文件'); return false }
  if (file.size / 1024 / 1024 > props.maxSizeMB) { ElMessage.error(`图片大小不能超过 ${props.maxSizeMB}MB`); return false }
  return true
}

const setPreviewFromFile = (file: File) => {
  if (ownedPendingObjectUrl) { URL.revokeObjectURL(ownedPendingObjectUrl); ownedPendingObjectUrl = null }
  ownedPendingObjectUrl = URL.createObjectURL(file); localPreviewUrl.value = ownedPendingObjectUrl
}

const handlePicked = (e: Event) => {
  const input = e.target as HTMLInputElement; const file = input.files?.[0]
  if (!file) return; if (!validateFile(file)) { input.value = ''; return }
  selectedFile.value = file; setPreviewFromFile(file)
}

const clearSelected = () => {
  selectedFile.value = null; if (fileInputRef.value) fileInputRef.value.value = ''
  if (ownedPendingObjectUrl) { URL.revokeObjectURL(ownedPendingObjectUrl); ownedPendingObjectUrl = null }
  if (lastEmittedObjectUrl) { URL.revokeObjectURL(lastEmittedObjectUrl); lastEmittedObjectUrl = null }
  localPreviewUrl.value = ''; emit('update:previewUrl', ''); emit('update:resourceKey', ''); emit('update:uploadToken', '')
}

const uploadToPresignedUrl = async (policy: PresignedUploadResp, file: File) => {
  const res = await fetch(policy.url, { method: policy.method, headers: { 'Content-Type': file.type || 'application/octet-stream' }, body: file })
  if (!res.ok) throw new Error(`Upload failed: ${res.status}`)
}

const normalizePolicy = async (suffix: string): Promise<PresignedUploadResp> => {
  const result = await props.getUploadPolicy(suffix); const anyRes: any = result
  return anyRes?.data ? (anyRes.data as PresignedUploadResp) : (result as PresignedUploadResp)
}

const handleConfirm = async () => {
  if (props.disabled) return
  if (!selectedFile.value || !localPreviewUrl.value) { ElMessage.warning('请先选择图片'); return }
  uploading.value = true
  try {
    const suffix = selectedFile.value.name.split('.').pop()?.toLowerCase() || ''
    const policy = await normalizePolicy(suffix); await uploadToPresignedUrl(policy, selectedFile.value)
    await uploadCallback({ token: policy.token })
    emit('update:resourceKey', policy.token); emit('update:uploadToken', policy.token)
    if (ownedPendingObjectUrl) {
      if (lastEmittedObjectUrl && lastEmittedObjectUrl !== ownedPendingObjectUrl) URL.revokeObjectURL(lastEmittedObjectUrl)
      lastEmittedObjectUrl = ownedPendingObjectUrl; ownedPendingObjectUrl = null
      emit('update:previewUrl', lastEmittedObjectUrl)
    }
    const payload = { token: policy.token, previewUrl: lastEmittedObjectUrl || localPreviewUrl.value }
    emit('submitted', payload); await props.onUploaded?.(payload); emit('update:visible', false)
  } catch { ElMessage.error('上传失败'); emit('update:resourceKey', ''); emit('update:uploadToken', '') }
  finally { uploading.value = false }
}
</script>

<style scoped>
.single-image-uploader { display: flex; flex-direction: column; gap: 12px; }
.preview { width: 200px; height: 200px; border: 1px dashed var(--border); border-radius: 8px; overflow: hidden; cursor: pointer; display: flex; align-items: center; justify-content: center; background: var(--bg); }
.preview-img { width: 100%; height: 100%; object-fit: cover; }
.preview-placeholder { text-align: center; color: var(--text-muted); font-size: 14px; line-height: 20px; }
.preview-sub { margin-top: 4px; font-size: 12px; }
.actions { display: flex; gap: 8px; }
.file-input { display: none; }
.upload-tip { font-size: 12px; color: var(--text-muted); }
</style>
