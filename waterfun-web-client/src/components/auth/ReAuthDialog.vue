<template>
  <el-dialog
    title="验证身份"
    v-model="dialogVisible"
    width="380px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="reauth-body">
      <!-- Step 1: 显示掩码手机 + 发送验证码 -->
      <template v-if="reauth.state.step === 1">
        <p class="reauth-hint">
          为了安全起见，请完成身份验证。
          <br>验证码将发送到您的绑定手机：
        </p>
        <p class="reauth-phone">{{ reauth.state.maskedPhone || '正在获取...' }}</p>
        <div class="reauth-action">
          <el-button
            type="primary"
            :loading="reauth.state.sendingCode"
            :disabled="!reauth.state.maskedPhone"
            @click="handleSendCode"
          >
            {{ reauth.state.codeSent ? '重新发送验证码' : '发送验证码' }}
          </el-button>
        </div>
        <p v-if="reauth.state.codeSent" class="reauth-tip">验证码已发送，请查看手机短信</p>
      </template>

      <!-- Step 2: 输入验证码 -->
      <template v-if="reauth.state.step === 2">
        <p class="reauth-hint">请输入手机收到的验证码</p>
        <el-input
          v-model="reauth.code"
          placeholder="请输入验证码"
          maxlength="6"
          class="reauth-input"
          @keyup.enter="handleVerifyCode"
        />
        <div class="reauth-action">
          <el-button
            type="primary"
            :loading="reauth.state.verifying"
            :disabled="reauth.code.length < 6"
            @click="handleVerifyCode"
          >
            验证
          </el-button>
        </div>
        <el-button link size="small" @click="reauth.resetToStep1()">
          重新发送验证码
        </el-button>
      </template>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useReAuth } from '~/composables/useReAuth'
import type { VerifyScene } from '~/api/authApi'

const props = defineProps<{
  visible: boolean
  scene: VerifyScene
}>()

const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'success', reAuthToken: string): void
}>()

const dialogVisible = ref(props.visible)
watch(() => props.visible, v => { dialogVisible.value = v })

const reauth = useReAuth(props.scene)

// 打开时获取掩码手机号
watch(dialogVisible, async (v) => {
  if (v) {
    reauth.reset()
    await reauth.fetchInfo()
  }
})

const handleSendCode = async () => {
  await reauth.sendCode()
}

const handleVerifyCode = async () => {
  const token = await reauth.verifyCode()
  if (token) {
    emit('success', token)
    dialogVisible.value = false
  }
}

const handleClose = () => {
  reauth.reset()
  emit('update:visible', false)
}
</script>

<style scoped>
.reauth-body {
  text-align: center;
  padding: 12px 0;
}
.reauth-hint {
  font-size: 14px;
  color: #606266;
  margin-bottom: 16px;
  line-height: 1.6;
}
.reauth-phone {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
  letter-spacing: 1px;
}
.reauth-action {
  margin: 16px 0;
}
.reauth-input {
  width: 200px;
  margin: 0 auto 12px;
}
.reauth-tip {
  font-size: 12px;
  color: #67c23a;
}
</style>
