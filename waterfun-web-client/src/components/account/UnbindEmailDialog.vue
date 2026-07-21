<template>
  <el-dialog
    v-if="step === 1"
    title="确认解绑邮箱"
    v-model="visible"
    width="400px"
    @close="handleClose"
  >
    <div class="dialog-content">
      <span class="warning-text">解绑后，您将无法使用该邮箱登录账号。</span>
      <el-form label-width="auto">
        <el-form-item label="邮箱地址">
          <el-input v-model="email" placeholder="请输入完整的邮箱地址进行验证" clearable />
          <div class="email-hint">当前绑定邮箱：{{ props.email || '未获取到邮箱信息' }}</div>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="danger" @click="proceedToVerify">下一步</el-button>
      </div>
    </template>
  </el-dialog>

  <ReAuthDialog
    v-if="step === 2"
    :visible="step === 2"
    scene="unbind"
    @update:visible="handleVerifyClose"
    @success="handleReAuthSuccess"
  />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { unbindEmail } from '~/api/accountApi'
import ReAuthDialog from '~/components/auth/ReAuthDialog.vue'

interface Props {
  visible: boolean
  email?: string
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
  (e: 'error', message: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = ref(props.visible)
const email = ref('')
const step = ref(1 as 1 | 2)

watch(() => props.visible, v => { visible.value = v })
watch(() => props.email, v => { email.value = v || '' })
watch(visible, v => { emit('update:visible', v) })

const validateEmail = (e: string): boolean => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e)

const proceedToVerify = () => {
  if (!email.value || !validateEmail(email.value)) {
    ElMessage.error('请输入有效的邮箱地址')
    return
  }
  step.value = 2
}

const handleReAuthSuccess = async (reAuthToken: string) => {
  try {
    await unbindEmail(email.value, reAuthToken)
    ElMessage.success('邮箱解绑成功')
    visible.value = false
    step.value = 1
    emit('success')
  } catch (error: any) {
    emit('error', error.response?.data?.message || '邮箱解绑失败')
    step.value = 1
  }
}

const handleClose = () => { step.value = 1; email.value = '' }
const handleCancel = () => { visible.value = false }
const handleVerifyClose = (val: boolean) => { if (!val) { visible.value = false; step.value = 1 } }
</script>

<style scoped>
.dialog-content { padding: 20px 0; }
.warning-text { display: block; color: #e6a23c; font-size: 14px; margin-bottom: 20px; line-height: 1.5; }
.email-hint { font-size: 12px; color: #909399; margin-top: 5px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; }
</style>
