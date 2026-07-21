<script setup lang="ts">
import { useUserAccountStore } from '@/stores/userAccountStore'
import { useUserInfoStore } from '@/stores/userInfoStore'
import { storeToRefs } from 'pinia'
import UnbindEmailDialog from '~/components/account/UnbindEmailDialog.vue'
import ReAuthDialog from '@/components/auth/ReAuthDialog.vue'
import { ElMessage, ElInput, ElButton, ElDialog } from 'element-plus'
import { useRouter } from 'vue-router'
import { changeEmail, changePhone, activateEmail, verifyChangeEmail, verifyChangePhone } from '~/api/accountApi'
import type { VerifyScene } from '~/api/authApi'

const userAccountStore = useUserAccountStore()
const userInfoStore = useUserInfoStore()

const { userAccount } = storeToRefs(userAccountStore)
const { userInfo } = storeToRefs(userInfoStore)

const loading = ref(false)
const error = ref('')
const router = useRouter()

// -- dialog state machine --
// action: 'change_email' | 'bind_email' | 'activate_email' | 'change_phone'
const actionType = ref<string>('')

// ReAuthDialog
const showReAuth = ref(false)
const reAuthScene = ref<VerifyScene>('change_email')

// input target (new email/phone)
const showTargetInput = ref(false)
const targetInput = ref('')
const targetLabel = ref('')

// activation code input
const showActivateCode = ref(false)
const activateCode = ref('')
const activateVerifyKey = ref('')
const activateLabel = ref('')

// internal state
const processing = ref(false)
const reAuthToken = ref('')

const unbindEmailDialog = reactive({ visible: false })

const resetDialogs = () => {
  showReAuth.value = false
  showTargetInput.value = false
  showActivateCode.value = false
  targetInput.value = ''
  activateCode.value = ''
  activateVerifyKey.value = ''
  reAuthToken.value = ''
  actionType.value = ''
  processing.value = false
}

// -- fetch account info --
const fetchAccountInfo = async () => {
  loading.value = true
  error.value = ''
  try {
    await userAccountStore.fetchAccountInfoAndUpdate()
  } catch (err) {
    error.value = '获取账户信息失败'
    console.error('获取账户信息失败:', err)
  } finally {
    loading.value = false
  }
}

const navigateToPassword = () => {
  router.push('/account/password')
}

// -- ReAuth success handler --
const handleReAuthSuccess = (token: string) => {
  reAuthToken.value = token
  showReAuth.value = false

  const action = actionType.value

  if (action === 'activate_email') {
    // Direct activation — no new target needed
    doActivateEmail(token)
  } else if (action === 'change_email' || action === 'bind_email') {
    // Need new email first
    if (action === 'bind_email') {
      // For bind, we already collected the email before re-auth
      doChangeEmail(targetInput.value, token)
    } else {
      // For change, show target input after re-auth
      targetLabel.value = '新邮箱地址'
      showTargetInput.value = true
    }
  } else if (action === 'change_phone') {
    targetLabel.value = '新手机号'
    showTargetInput.value = true
  }
}

// -- handlers --

const handleEmailAction = () => {
  resetDialogs()
  if (userAccount.value.emailMasked) {
    // Already bound — change email
    actionType.value = 'change_email'
    targetInput.value = ''
    reAuthScene.value = 'change_email'
    showReAuth.value = true
  } else {
    // Not bound — bind new email
    actionType.value = 'bind_email'
    targetInput.value = ''
    targetLabel.value = '邮箱地址'
    showTargetInput.value = true
  }
}

const handlePhoneAction = () => {
  if (!userAccount.value.phoneMasked) {
    ElMessage.warning('手机号必须绑定，请联系客服')
    return
  }
  resetDialogs()
  actionType.value = 'change_phone'
  targetInput.value = ''
  reAuthScene.value = 'change_phone'
  showReAuth.value = true
}

const handleEmailActivate = () => {
  resetDialogs()
  actionType.value = 'activate_email'
  reAuthScene.value = 'activate'
  showReAuth.value = true
}

const handleEmailUnbind = () => {
  unbindEmailDialog.visible = true
}

// -- after target input is submitted --
const handleTargetSubmit = async () => {
  if (!targetInput.value.trim()) {
    ElMessage.warning(`请输入${targetLabel.value}`)
    return
  }
  const token = reAuthToken.value
  if (actionType.value === 'change_email') {
    await doChangeEmail(targetInput.value.trim(), token)
  } else if (actionType.value === 'bind_email') {
    // For bind, we need re-auth first, then activate
    reAuthScene.value = 'activate'
    showReAuth.value = true
  } else if (actionType.value === 'change_phone') {
    await doChangePhone(targetInput.value.trim(), token)
  }
}

// -- API calls --

const doChangeEmail = async (newEmail: string, token: string) => {
  processing.value = true
  showTargetInput.value = false
  try {
    const res = await changeEmail(newEmail, token)
    if (res.data?.verifyKey) {
      activateVerifyKey.value = res.data.verifyKey
      activateLabel.value = '新邮箱验证码'
      activateCode.value = ''
      showActivateCode.value = true
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '修改邮箱失败')
    resetDialogs()
  } finally {
    processing.value = false
  }
}

const doChangePhone = async (newPhone: string, token: string) => {
  processing.value = true
  showTargetInput.value = false
  try {
    const res = await changePhone(newPhone, token)
    if (res.data?.verifyKey) {
      activateVerifyKey.value = res.data.verifyKey
      activateLabel.value = '新手机号验证码'
      activateCode.value = ''
      showActivateCode.value = true
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '修改手机号失败')
    resetDialogs()
  } finally {
    processing.value = false
  }
}

const doActivateEmail = async (token: string) => {
  processing.value = true
  try {
    await activateEmail(userAccount.value.emailMasked || '', token)
    ElMessage.success('邮箱激活成功')
    resetDialogs()
    fetchAccountInfo()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '激活邮箱失败')
    resetDialogs()
  } finally {
    processing.value = false
  }
}

const handleActivateCodeSubmit = async () => {
  if (!activateCode.value.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  processing.value = true
  try {
    if (actionType.value === 'change_email') {
      await verifyChangeEmail(activateVerifyKey.value, activateCode.value.trim())
      ElMessage.success('邮箱修改成功')
    } else if (actionType.value === 'change_phone') {
      await verifyChangePhone(activateVerifyKey.value, activateCode.value.trim())
      ElMessage.success('手机号修改成功')
    }
    resetDialogs()
    fetchAccountInfo()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '验证失败')
  } finally {
    processing.value = false
  }
}

const handleBindEmailSubmit = async () => {
  if (!targetInput.value.trim()) {
    ElMessage.warning('请输入邮箱地址')
    return
  }
  reAuthScene.value = 'activate'
  showTargetInput.value = false
  showReAuth.value = true
}

const handleUnbindEmailSuccess = () => {
  fetchAccountInfo()
}

const handleVerifySuccess = () => {
  fetchAccountInfo()
}

// -- account info computed --
const accountInfo = computed(() => ({
  password: {
    label: '密码',
    content: userInfo.value.passwordHash ? '已设置' : '未设置',
    action: {
      name: userInfo.value.passwordHash ? '重置' : '设置',
      handler: navigateToPassword
    },
    secondaryAction: null as { name: string; handler: () => void } | null
  },
  email: {
    label: '电子邮箱',
    content: userAccount.value.emailMasked || '未绑定',
    action: {
      name: userAccount.value.emailMasked ? '修改' : '绑定',
      handler: handleEmailAction
    },
    secondaryAction: userAccount.value.emailMasked
      ? (userAccount.value.emailVerified
        ? { name: '解绑', handler: handleEmailUnbind }
        : { name: '激活', handler: handleEmailActivate }
      ) : null
  },
  phone: {
    label: '手机号',
    content: userAccount.value.phoneMasked || '未绑定',
    action: {
      name: userAccount.value.phoneMasked ? '修改' : '绑定',
      handler: handlePhoneAction
    },
    secondaryAction: null as { name: string; handler: () => void } | null
  },
}))

onMounted(() => {
  fetchAccountInfo()
})
</script>

<template>
  <div class="account-container">
    <div class="g-form">
      <div class="g-form-section">
        <div class="g-section-title">注册账号</div>
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <div class="loading-text">加载中...</div>
        </div>
        <!-- 错误提示 -->
        <div v-else-if="error" class="error-container">
          <div class="error-text">{{ error }}</div>
          <button class="retry-btn" @click="fetchAccountInfo">重新加载</button>
        </div>
        <!-- 账户信息 -->
        <div v-else>
          <div v-for="(item, key) in accountInfo" :key="key" class="g-form-row">
            <div class="form-item">
              <span class="form-label">{{ item.label }}</span>
              <span class="item-content">{{ item.content }}</span>
              <div class="action-buttons">
                <button
                  v-if="item.secondaryAction"
                  class="action-btn"
                  @click="item.secondaryAction.handler"
                >
                  {{ item.secondaryAction.name }}
                </button>
                <button class="action-btn" @click="item.action.handler">{{ item.action.name }}</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ReAuth 身份验证弹窗 -->
    <ReAuthDialog
      v-if="(actionType === 'change_email' || actionType === 'change_phone' || actionType === 'bind_email' || actionType === 'activate_email')"
      :visible="showReAuth"
      :scene="reAuthScene"
      @update:visible="showReAuth = $event; if(!$event) resetDialogs()"
      @success="handleReAuthSuccess"
    />

    <!-- 新目标输入弹窗（修改邮箱/手机号时输入新值） -->
    <el-dialog
      v-model="showTargetInput"
      :title="'输入' + targetLabel"
      width="400px"
      @close="resetDialogs"
    >
      <el-input
        v-model="targetInput"
        :placeholder="'请输入' + targetLabel"
        size="large"
      />
      <template #footer>
        <el-button @click="resetDialogs">取消</el-button>
        <el-button type="primary" @click="actionType === 'bind_email' ? handleBindEmailSubmit() : handleTargetSubmit()" :loading="processing">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 激活码输入弹窗（修改后激活新邮箱/手机号） -->
    <el-dialog
      v-model="showActivateCode"
      :title="'验证' + activateLabel"
      width="400px"
      @close="resetDialogs"
    >
      <p class="activate-hint">验证码已发送至新{{ actionType === 'change_email' ? '邮箱' : '手机号' }}，请输入验证码完成修改</p>
      <el-input
        v-model="activateCode"
        placeholder="请输入验证码"
        maxlength="6"
        size="large"
        @keyup.enter="handleActivateCodeSubmit"
      />
      <template #footer>
        <el-button @click="resetDialogs">取消</el-button>
        <el-button type="primary" @click="handleActivateCodeSubmit" :loading="processing">
          验证
        </el-button>
      </template>
    </el-dialog>

    <!-- 解绑邮箱弹窗（已有独立组件） -->
    <UnbindEmailDialog
      v-model:visible="unbindEmailDialog.visible"
      :email="userAccount.emailMasked ? userAccount.emailMasked.replace('*', '') : ''"
      @success="handleUnbindEmailSuccess"
      @error="(message: string) => ElMessage.error(message)"
    />
  </div>
</template>

<style scoped>
.account-container {
  max-width: 800px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.form-item {
  flex: 1;
  min-width: 250px;
  padding: 10px 0;
  margin-left: 20px;
  display: flex;
  align-items: center;
}

.form-label {
  display: block;
  font-weight: 500;
  color: #606266;
  font-size: 14px;
  line-height: 24px;
  width: 100px;
}

.form-item .item-content {
  flex: 1;
  font-size: 14px;
  color: #303133;
  line-height: 24px;
}

.action-btn {
  padding: 8px 16px;
  font-size: 14px;
  line-height: 24px;
  border: none;
  background-color: transparent;
  cursor: pointer;
  transition-duration: 0.3s;
}
.action-btn:hover {
  color: #409eff;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.loading-container, .error-container {
  padding: 40px 20px;
  text-align: center;
}

.loading-text {
  font-size: 14px;
  color: #606266;
}

.error-text {
  font-size: 14px;
  color: #f56c6c;
  margin-bottom: 16px;
}

.retry-btn {
  padding: 8px 16px;
  font-size: 14px;
  border: 1px solid #409eff;
  background-color: transparent;
  color: #409eff;
  cursor: pointer;
  border-radius: 4px;
  transition-duration: 0.3s;
}

.retry-btn:hover {
  background-color: #409eff;
  color: white;
}

.activate-hint {
  font-size: 14px;
  color: #606266;
  margin-bottom: 16px;
  line-height: 1.5;
}
</style>
