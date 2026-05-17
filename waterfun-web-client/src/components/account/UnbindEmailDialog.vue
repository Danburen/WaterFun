<template>
  <!-- 第一步：输入邮箱地址 -->
  <el-dialog
    v-if="step === 1"
    title="确认解绑邮箱"
    v-model="visible"
    width="400px"
    @close="handleClose"
  >
    <div class="dialog-content">
      <span class="warning-text">
        解绑后，您将无法使用该邮箱登录账号。
      </span>
      <el-form label-width="auto">
        <el-form-item label="邮箱地址">
          <el-input
            v-model="email"
            placeholder="请输入完整的邮箱地址进行验证"
            clearable
          />
          <div class="email-hint">当前绑定邮箱：{{ props.email || '未获取到邮箱信息' }}</div>
        </el-form-item>
      </el-form>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="danger" @click="proceedToPhoneVerification">下一步</el-button>
      </div>
    </template>
  </el-dialog>
  
  <!-- 第二步：手机验证 -->
  <UnifiedVerify
    v-else-if="step === 2"
    :visible="step === 2"
    type="phone"
    scene="unbind"
    :can-switch="false"
    @try-verify="handlePhoneVerify"
    @update:visible="handleVerifyClose"
  />
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { unbindEmail } from '~/api/accountApi'
import UnifiedVerify from '~/components/auth/verify/UnifiedVerify.vue'

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

// 监听props变化
watch(() => props.visible, (newVal) => {
  visible.value = newVal
})

watch(() => props.email, (newVal) => {
  email.value = newVal || ''
})

// 监听内部visible变化
watch(visible, (newVal) => {
  emit('update:visible', newVal)
})

// 验证邮箱格式
const validateEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

// 进入手机验证步骤
const proceedToPhoneVerification = () => {
  if (!email.value) {
    ElMessage.error('请输入邮箱地址')
    return
  }
  
  if (!validateEmail(email.value)) {
    ElMessage.error('请输入有效的邮箱地址')
    return
  }
  step.value = 2
}

// 处理手机验证
const handlePhoneVerify = async (code: string) => {
  try {
    await unbindEmail({ 
      email: email.value, 
      verify: {
        code: code,
        channel: 'sms',
        scene: 'unbind'
      }
    })
    
    ElMessage.success('邮箱解绑成功')
    visible.value = false
    step.value = 1 // 重置步骤
    emit('success')
  } catch (error: any) {
    const message = error.response?.data?.message || '邮箱解绑失败'
    // 不再显示错误消息，让调用方处理
    emit('error', message)
    // 验证失败时不关闭对话框，让用户可以重试
    step.value = 1
  }
}

// 处理关闭
const handleClose = () => {
  step.value = 1
  email.value = ''
}

// 处理取消
const handleCancel = () => {
  visible.value = false
}

// 处理验证关闭
const handleVerifyClose = (val: boolean) => {
  if (!val) {
    visible.value = false
    step.value = 1
  }
}
</script>

<style scoped>
.dialog-content {
  padding: 20px 0;
}

.warning-text {
  display: block;
  color: #e6a23c;
  font-size: 14px;
  margin-bottom: 20px;
  line-height: 1.5;
}

.email-input {
  margin-top: 10px;
}

.email-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.verify-container {
  padding: 0;
}
</style>