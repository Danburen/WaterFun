<template>
    <el-dialog
        :model-value="visible"
        @update:model-value="(v) => emit('update:visible', v)"
        :title="title"
        width="420px"
        :close-on-click-modal="false"
        destroy-on-close
        @closed="handleClosed"
    >
        <div class="single-image-uploader">
            <div class="preview" @click="!disabled && triggerPick()">
                <img v-if="localPreviewUrl" class="preview-img" :src="localPreviewUrl" alt="" />
                <div v-else class="preview-placeholder">
                    <div>选择图片</div>
                    <div class="preview-sub">支持常见图片格式</div>
                </div>
            </div>

            <div class="actions">
                <input
                    ref="fileInputRef"
                    class="file-input"
                    type="file"
                    accept="image/*"
                    :disabled="disabled || uploading"
                    @change="handlePicked"
                />
                <el-button :disabled="disabled || uploading" @click="triggerPick">
                    {{ localPreviewUrl ? '重新选择' : '选择图片' }}
                </el-button>
                <el-button v-if="localPreviewUrl" :disabled="disabled || uploading" @click="clearSelected">
                    清除
                </el-button>
            </div>

            <div v-if="uploading" class="upload-tip">Uploading...</div>
        </div>

        <template #footer>
            <el-button @click="emit('update:visible', false)" :disabled="uploading">取消</el-button>
            <el-button type="primary" :loading="uploading" @click="handleConfirm">
                确定
            </el-button>
        </template>
    </el-dialog>
</template>

<script lang="ts" setup>
import { ElMessage } from 'element-plus'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
export type PresignedUploadResp = {
    key: string
    url: string
    method: HttpMethod
    token: string
}

const props = withDefaults(
    defineProps<{
        visible: boolean
        title?: string
        disabled?: boolean
        maxSizeMB?: number
        resourceKey?: string
        uploadToken?: string
        previewUrl?: string
        getUploadPolicy: (suffix: string) => Promise<{ data: PresignedUploadResp }> | Promise<PresignedUploadResp>
        onUploaded?: (payload: { key: string; token: string; previewUrl: string }) => void | Promise<void>
    }>(),
    {
        title: '上传图片',
        disabled: false,
        maxSizeMB: 5,
        resourceKey: '',
        uploadToken: '',
        previewUrl: '',
    }
)

const emit = defineEmits<{
    'update:visible': [value: boolean]
    'update:resourceKey': [value: string]
    'update:uploadToken': [value: string]
    'update:previewUrl': [value: string]
    submitted: [payload: { key: string; token: string; previewUrl: string }]
}>()

const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const uploading = ref(false)

const localPreviewUrl = ref(props.previewUrl || '')
let ownedPendingObjectUrl: string | null = null
let lastEmittedObjectUrl: string | null = null

watch(
    () => props.previewUrl,
    (v) => {
        if (typeof v === 'string' && v !== localPreviewUrl.value) {
            localPreviewUrl.value = v
        }
    }
)

const triggerPick = () => {
    if (props.disabled || uploading.value) return
    fileInputRef.value?.click()
}

const validateFile = (file: File): boolean => {
    const isImage = file.type?.startsWith('image/')
    if (!isImage) {
        ElMessage.error('只能选择图片文件')
        return false
    }
    const tooLarge = file.size / 1024 / 1024 > props.maxSizeMB
    if (tooLarge) {
        ElMessage.error(`图片大小不能超过 ${props.maxSizeMB}MB`)
        return false
    }
    return true
}

const setPreviewFromFile = (file: File) => {
    if (ownedPendingObjectUrl) {
        URL.revokeObjectURL(ownedPendingObjectUrl)
        ownedPendingObjectUrl = null
    }
    ownedPendingObjectUrl = URL.createObjectURL(file)
    localPreviewUrl.value = ownedPendingObjectUrl
}

const handlePicked = (e: Event) => {
    const input = e.target as HTMLInputElement
    const file = input.files?.[0]
    if (!file) return
    if (!validateFile(file)) {
        input.value = ''
        return
    }

    selectedFile.value = file
    setPreviewFromFile(file)
}

const clearSelected = () => {
    selectedFile.value = null
    if (fileInputRef.value) fileInputRef.value.value = ''
    if (ownedPendingObjectUrl) {
        URL.revokeObjectURL(ownedPendingObjectUrl)
        ownedPendingObjectUrl = null
    }
    if (lastEmittedObjectUrl) {
        URL.revokeObjectURL(lastEmittedObjectUrl)
        lastEmittedObjectUrl = null
    }
    localPreviewUrl.value = ''
    emit('update:previewUrl', '')
    emit('update:resourceKey', '')
    emit('update:uploadToken', '')
}

const uploadToPresignedUrl = async (policy: PresignedUploadResp, file: File) => {
    const res = await fetch(policy.url, {
        method: policy.method,
        headers: {
            'Content-Type': file.type || 'application/octet-stream',
        },
        body: file,
    })
    if (!res.ok) throw new Error(`Upload failed: ${res.status}`)
}

const normalizePolicy = async (suffix: string): Promise<PresignedUploadResp> => {
    const result = await props.getUploadPolicy(suffix)
    const anyRes: any = result as any
    return anyRes?.data ? (anyRes.data as PresignedUploadResp) : (result as PresignedUploadResp)
}

const handleConfirm = async () => {
    if (props.disabled) return
    if (!selectedFile.value || !localPreviewUrl.value) {
        ElMessage.warning('请先选择图片')
        return
    }

    uploading.value = true
    try {
        const suffix = selectedFile.value.name.split('.').pop()?.toLowerCase() || ''
        const policy = await normalizePolicy(suffix)
        await uploadToPresignedUrl(policy, selectedFile.value)

        emit('update:resourceKey', policy.key)
        emit('update:uploadToken', policy.token)

        // 只有上传成功后才把 objectURL 同步给父层，用于“按钮替换成 img”展示。
        if (ownedPendingObjectUrl) {
            if (lastEmittedObjectUrl && lastEmittedObjectUrl !== ownedPendingObjectUrl) {
                URL.revokeObjectURL(lastEmittedObjectUrl)
            }
            lastEmittedObjectUrl = ownedPendingObjectUrl
            ownedPendingObjectUrl = null
            emit('update:previewUrl', lastEmittedObjectUrl)
        }

        const payload = { key: policy.key, token: policy.token, previewUrl: lastEmittedObjectUrl || localPreviewUrl.value }
        emit('submitted', payload)
        await props.onUploaded?.(payload)

        emit('update:visible', false)
    } catch (e) {
        console.error(e)
        ElMessage.error('上传失败')
        emit('update:resourceKey', '')
        emit('update:uploadToken', '')
    } finally {
        uploading.value = false
    }
}

const handleClosed = () => {
    // 关闭弹窗时只清理“待上传文件选择”，不主动 revoke 预览 URL，
    // 因为父层可能需要用该 objectURL 作为已上传后的展示。
    selectedFile.value = null
    if (fileInputRef.value) fileInputRef.value.value = ''

    // 如果用户只是选了文件但没点“确定上传”，需要清理 pending objectURL，
    // 同时把本地预览回退到父层传入的 previewUrl。
    if (ownedPendingObjectUrl) {
        URL.revokeObjectURL(ownedPendingObjectUrl)
        ownedPendingObjectUrl = null
    }
    localPreviewUrl.value = props.previewUrl || ''
}
</script>

<style scoped>
.single-image-uploader {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.preview {
    width: 200px;
    height: 200px;
    border: 1px dashed var(--el-border-color);
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--el-fill-color-lighter);
}

.preview-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.preview-placeholder {
    text-align: center;
    color: var(--el-text-color-secondary);
    font-size: 14px;
    line-height: 20px;
}

.preview-sub {
    margin-top: 4px;
    font-size: 12px;
}

.actions {
    display: flex;
    gap: 8px;
}

.file-input {
    display: none;
}

.upload-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
}
</style>