<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { validateVerifyCode } from '~/utils/validator'
import { activateEmail, activatePhone } from '~/api/accountApi'
import { sendAuthenticationCode } from '~/api/authApi'
import type { SecurityVerifyCodeDto, SecurityVerifyScene } from '~/api/authApi'

const props = defineProps<{
  visible: boolean
  type: 'email' | 'phone'
  target: string
  alreadySent: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'activate-success': []
}>()

const formRef = ref<FormInstance>()
const activating = ref(false)
const sendingCode = ref(false)

const label = computed(() => isEmail.value ? '邮箱' : '手机号')

const form = reactive({
  verifyCode: '',
})

const isEmail = computed(() => props.type === 'email')
const title = computed(() => `激活${isEmail.value ? '邮箱' : '手机号'}`)

const rules = reactive<FormRules<typeof form>>({
  verifyCode: [{ validator: validateVerifyCode(false), trigger: 'blur' }],
})

const sendCode = async () => {
  sendingCode.value = true
  try {
    const { generateFingerprint } = await import('@waterfun/web-core/src/fingerprint')
    
    const channel = isEmail.value ? 'email' : 'sms'
    const scene =  'activate'
    await sendAuthenticationCode({
      channel: channel,
      scene: scene as SecurityVerifyScene,
      deviceFp: await generateFingerprint(),
      [isEmail.value ? 'email' : 'phoneNumber']: props.target
    })
    ElMessage.success('验证码发送成功')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '验证码发送失败')
  } finally {
    sendingCode.value = false
  }
}

const handleActivate = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    
    activating.value = true
    const channel = isEmail.value ? 'email' : 'sms'
    const scene = 'activate'
    const verify = { 
      code: form.verifyCode, 
      channel: channel, 
      scene: scene as SecurityVerifyScene 
    } as SecurityVerifyCodeDto
    try {
      if (isEmail.value) {
          await activateEmail({
            email: props.target,
            verify: verify
          })
        } else {
          await activatePhone({
            phone: props.target,
            verify: verify
          })
        }
      ElMessage.success('激活成功')
      emit('activate-success')
      handleClose()
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '激活失败')
    } finally {
      activating.value = false
    }
  })
}

const handleClose = () => {
  emit('update:visible', false)
  form.verifyCode = ''
}
</script>

<template>
  <el-dialog
    :model-value="props.visible"
    :title="title"
    width="400px"
    @close="handleClose"
  > 
    <div v-if="props.alreadySent" class="activate-message">
      我们已向 {{ props.target }} 发送了验证码，请输入验证码
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="60px"
      class="activate-form"
    >
      <el-form-item v-if="!props.alreadySent" :label="label" prop="verifyCode">
        <div class="verify-info">{{ props.target || label }}</div>
      </el-form-item>
      <el-form-item label="验证码" prop="verifyCode">
        <div class="code-input-wrapper">
          <el-input
            v-model="form.verifyCode"
            placeholder="请输入验证码"
            class="code-input"
            @keyup.enter="handleActivate"
          />
          <el-button
            type="primary"
            @click="sendCode"
            :loading="sendingCode"
          >
            {{ sendingCode ? '发送中...' : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleActivate" :loading="activating">
          {{ '激活' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.activate-message {
  padding: 5px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
}

.activate-form {
  padding: 10px 0;
}

.code-input-wrapper {
  display: flex;
  gap: 10px;
}

.code-input {
  flex: 1;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>