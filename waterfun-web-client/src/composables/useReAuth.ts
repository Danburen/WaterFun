import { ref, readonly } from 'vue'
import { ElMessage } from 'element-plus'
import { getReAuthInfo, sendReAuthCode, verifyReAuthCode } from '~/api/accountApi'
import type { VerifyScene } from '~/api/authApi'

export function useReAuth(scene: VerifyScene) {
  const step = ref(1)             // 1 = info + send, 2 = verify code
  const maskedPhone = ref('')
  const sendingCode = ref(false)
  const codeSent = ref(false)
  const code = ref('')
  const verifying = ref(false)
  const loading = ref(false)

  /** Step 1: fetch masked phone from server */
  async function fetchInfo() {
    loading.value = true
    try {
      const res = await getReAuthInfo()
      maskedPhone.value = res.data.maskedPhone || ''
      return res.data.maskedPhone
    } catch {
      maskedPhone.value = '未绑定手机'
      return null
    } finally {
      loading.value = false
    }
  }

  /** Step 2: send verification code to bound phone */
  async function sendCode() {
    sendingCode.value = true
    try {
      await sendReAuthCode(scene)
      codeSent.value = true
      step.value = 2
      return true
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || e.message || '发送验证码失败')
      return false
    } finally {
      sendingCode.value = false
    }
  }

  /** Step 3: verify code and return reAuthToken */
  async function verifyCode(): Promise<string | null> {
    if (code.value.length < 6) return null
    verifying.value = true
    try {
      const res = await verifyReAuthCode(scene, code.value)
      if (res.data.reAuthToken) {
        return res.data.reAuthToken
      }
      ElMessage.error('验证失败，请重试')
      return null
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || e.message || '验证失败')
      return null
    } finally {
      verifying.value = false
    }
  }

  /** Go back to step 1 (resend), keeping phone info */
  function resetToStep1() {
    step.value = 1
    code.value = ''
    codeSent.value = false
  }

  /** Full reset — back to initial state */
  function reset() {
    resetToStep1()
    maskedPhone.value = ''
    loading.value = false
    sendingCode.value = false
    verifying.value = false
  }

  return {
    state: {
      step: readonly(step),
      maskedPhone: readonly(maskedPhone),
      codeSent: readonly(codeSent),
      loading: readonly(loading),
      sendingCode: readonly(sendingCode),
      verifying: readonly(verifying),
    },
    code,
    fetchInfo,
    sendCode,
    verifyCode,
    resetToStep1,
    reset,
  }
}
