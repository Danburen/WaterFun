<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { changeEmail, changePhone } from '~/api/accountApi'
import type { VerifyScene } from '~/api/authApi'
import UnifiedVerify from './UnifiedVerify.vue'
import ActivateVerify from './ActivateVerify.vue'
import { sendAuthenticationCode } from '~/api/authApi'
import { generateFingerprint } from '@waterfun/web-core/src/fingerprint'
import { useUserAccountStore } from '@/stores/userAccountStore'

const props = defineProps<{
  visible: boolean
  type: 'email' | 'phone'
  scene: VerifyScene
  target?: string
  isNewBinding?: boolean // 是否是新绑定（未绑定过）
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'success': []
}>()

const step = ref(1)
const verifyCodeKey = ref('')
const newTarget = ref('') // 新邮箱地址
const isProcessing = ref(false)

const userAccountStore = useUserAccountStore()

const isChangeScene = computed(() => props.scene.includes('change'))
const isEmail = computed(() => props.type === 'email')

// 修改邮箱时 Step2 使用手机号(SMS)验证身份
const step2Type = computed(() => {
  if (props.scene === 'change_email') return 'phone' as const
  return props.type
})

// 修改邮箱时 Step2 展示手机号作为验证目标
const step2Target = computed(() => {
  if (props.scene === 'change_email') {
    return userAccountStore.userAccount.phoneMasked || ''
  }
  return props.target || ''
})

// 步骤标题
const stepTitle = computed(() => {
  const typeText = isEmail.value ? '邮箱' : '手机号'
  if (step.value === 1) return `填写${typeText}`
  if (step.value === 2) return `验证身份 - ${typeText}`
  return isChangeScene.value ? `修改${typeText}` : `激活${typeText}`
})

// 第一步：处理表单提交（新邮箱/手机号）
const handleFormSubmit = () => {
  if (!newTarget.value.trim()) {
    ElMessage.warning(`请输入${isEmail.value ? '邮箱' : '手机号'}`)
    return
  }
  // 如果是新绑定且未验证过，直接跳到第三步（激活）
  if (props.isNewBinding && !props.target) {
    step.value = 3
  } else {
    step.value = 2
  }
}

// 第二步：身份验证成功
const handleVerifySuccess = async (code: string) => {
    const deviceFp = await generateFingerprint()
    verifyCodeKey.value = code
    // 修改邮箱用 SMS(手机号)验证身份；其他场景按类型对应的通道
    const channel = props.scene === 'change_email' ? 'sms' : (isEmail.value ? 'email' : 'sms')
    const scene = props.scene as VerifyScene
    if (props.target && !props.isNewBinding) {
        isProcessing.value = true
        try {
            if (props.scene === 'change_email') {
                await changeEmail({
                    email: newTarget.value,
                    verify: { code, channel: 'sms', scene, deviceFp }
                })
            } else {
                await changePhone({
                    phone: newTarget.value,
                    verify: { code, channel, scene, deviceFp }
                })
            }
            // change请求成功后，后端会自动发送激活验证码
            step.value = 3
        } catch (error: any) {
            ElMessage.error(error.response?.data?.message || '修改失败')
            return
        } finally {
            isProcessing.value = false
        }
    } else {
        // 绑定场景，发送激活验证码
        isProcessing.value = true
        try {
            await sendAuthenticationCode({ channel, scene, deviceFp })
            step.value = 3
        } catch (error: any) {
            ElMessage.error(error.response?.data?.message || '发送验证码失败')
            return
        } finally {
            isProcessing.value = false
        }
    }
}

// 第三步：激活成功
const handleActivateSuccess = () => {
  emit('success')
  handleClose()
}

const handleClose = () => {
  emit('update:visible', false)
  step.value = 1
  verifyCodeKey.value = ''
  newTarget.value = ''
  isProcessing.value = false
}

// 跳过第一步（已绑定场景）
const skipToStep2 = () => {
  step.value = 2
}

onMounted(() => {
  // 如果是纯激活场景（未绑定且新绑定），跳过第一步
  if (!props.target && props.isNewBinding) {
    step.value = 2
  }
})
</script>

<template>
  <div>
    <!-- 第一步：填写新邮箱/手机号 -->
    <div v-if="step === 1 && !(!props.target && props.isNewBinding)" class="step-container">
      <el-dialog
        :model-value="props.visible"
        :title="stepTitle"
        width="400px"
        @close="handleClose"
      >
        <div class="form-content">
          <el-input
            v-model="newTarget"
            :placeholder="`请输入新的${isEmail ? '邮箱地址' : '手机号'}`"
            size="large"
          />
          <span class="tip-text" v-if="isEmail">稍后我们会像您的新邮箱发送验证码，请注意修改邮箱会解绑您的旧邮箱。</span>
          <span class="tip-text" v-else>稍后我们会像您的新手机号发送验证码，请注意修改手机号会解绑您的旧手机号。</span>
        </div>
        
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="handleClose">取消</el-button>
            <el-button type="primary" @click="handleFormSubmit" :loading="isProcessing">
              下一步
            </el-button>
          </div>
        </template>
      </el-dialog>
    </div>

    <!-- 第二步：身份验证 -->
    <UnifiedVerify
      v-if="step === 2"
      :visible="props.visible"
      :type="step2Type"
      :scene="props.scene"
      :target="step2Target"
      :can-switch="false"
      @update:visible="$emit('update:visible', $event)"
      @try-verify="handleVerifySuccess"
    />

    <!-- 第三步：激活验证 -->
    <ActivateVerify
      v-if="step === 3"
      :visible="props.visible"
      :type="props.type"
      :target="newTarget || props.target || ''"
      :already-sent="true"
      @update:visible="handleClose"
      @activate-success="handleActivateSuccess"
    />
  </div>
</template>

<style scoped>
.step-container {
  position: relative;
}

.form-content {
  padding: 20px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.tip-text {
  display: block;
  margin-top: 10px;
  font-size: 12px;
  color: #606266;
}
</style>