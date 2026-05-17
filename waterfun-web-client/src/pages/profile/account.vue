<script setup lang="ts">
import { useUserAccountStore } from '@/stores/userAccountStore'
import { useUserInfoStore } from '@/stores/userInfoStore'
import { storeToRefs } from 'pinia'
import ThreeStepVerify from '@/components/auth/verify/ThreeStepVerify.vue'
import ActivateVerify from '~/components/auth/verify/ActivateVerify.vue'
import UnbindEmailDialog from '~/components/account/UnbindEmailDialog.vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import * as accountApi from '~/api/accountApi'

const userAccountStore = useUserAccountStore()
const userInfoStore = useUserInfoStore()

const { userAccount } = storeToRefs(userAccountStore)
const { userInfo } = storeToRefs(userInfoStore)

const loading = ref(false)
const error = ref('')
const router = useRouter()

// 验证对话框状态
const verifyDialog = reactive({
  visible: false,
  type: 'email' as 'email' | 'phone',
  scene: 'change_email' as any,
  target: '' as string,
  isNewBinding: false as boolean
})

const activateDialog = reactive({
  visible: false,
  type: 'email' as 'email' | 'phone',
  target: '' as string,
  alreadySent: false as boolean
})

const unbindEmailDialog = reactive({
  visible: false
})

// 获取账户信息
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

// 跳转到密码设置/重置页面
const navigateToPassword = () => {
  router.push('/account/password')
}

// 处理邮箱操作
const handleEmailAction = () => {
  if (userAccount.value.emailMasked) {
    // 已绑定邮箱，修改邮箱（三步流程）
    verifyDialog.scene = 'change_email'
    verifyDialog.target = userAccount.value.emailMasked
    verifyDialog.isNewBinding = false
  } else {
    // 未绑定邮箱，绑定新邮箱（三步流程）
    verifyDialog.scene = 'activate'
    verifyDialog.target = ''
    verifyDialog.isNewBinding = true
  }
  verifyDialog.type = 'email'
  verifyDialog.visible = true
}

// 处理手机操作（手机号强绑定，只能修改不能新绑定）
const handlePhoneAction = () => {
  if (userAccount.value.phoneMasked) {
    verifyDialog.type = 'phone'
    verifyDialog.scene = 'change_phone'
    verifyDialog.target = userAccount.value.phoneMasked
    verifyDialog.isNewBinding = false
    verifyDialog.visible = true
  } else {
    ElMessage.warning('手机号必须绑定，请联系客服')
  }
}

// 处理邮箱激活
const handleEmailActivate = () => {
  activateDialog.type = 'email'
  activateDialog.target = userAccount.value.emailMasked
  activateDialog.visible = true
}

// 处理邮箱解绑
const handleEmailUnbind = () => {
  unbindEmailDialog.visible = true
}

// 解绑成功处理
const handleUnbindEmailSuccess = () => {
  fetchAccountInfo()
}
// 验证成功处理
const handleVerifySuccess = () => {
  fetchAccountInfo()
  verifyDialog.visible = false
}

const handleActivateSuccess = () => {
  fetchAccountInfo()
  activateDialog.visible = false
}

const accountInfo = computed(() => ({
  password: { 
    label: '密码', 
    content: userInfo.value.passwordHash ? '已设置' : '未设置', 
    action: { 
      name: userInfo.value.passwordHash ? '重置' : '设置', 
      handler: navigateToPassword 
    },
    secondaryAction: null
  },
  email: { 
    label: '电子邮箱', 
    content: userAccount.value.emailMasked || '未绑定', 
    action: { 
      name: userAccount.value.emailMasked ? '修改' : '绑定', 
      handler: handleEmailAction 
    },
    // 激活/解绑功能按钮
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
    secondaryAction: null
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
    </div><!-- 三步验证对话框 -->
    <ThreeStepVerify
      v-if="verifyDialog.type === 'email'"
      v-model:visible="verifyDialog.visible"
      :type="verifyDialog.type"
      :scene="verifyDialog.scene"
      :target="verifyDialog.target"
      :is-new-binding="verifyDialog.isNewBinding"
      @success="handleVerifySuccess"
    />
    <ThreeStepVerify
      v-if="verifyDialog.type === 'phone'"
      v-model:visible="verifyDialog.visible"
      :type="verifyDialog.type"
      :scene="verifyDialog.scene"
      :target="verifyDialog.target"
      :is-new-binding="verifyDialog.isNewBinding"
      @success="handleVerifySuccess"
    />
    <!-- 激活邮箱对话框 -->
    <ActivateVerify
      v-model:visible="activateDialog.visible"
      :type="activateDialog.type"
      :target="activateDialog.target"
      :already-sent="activateDialog.alreadySent"
      @activate-success="handleActivateSuccess"
    />

    <!-- 解绑邮箱对话框 -->
    <UnbindEmailDialog
      v-model:visible="unbindEmailDialog.visible"
      :email="userAccount.emailMasked ? userAccount.emailMasked.replace('*', '') : ''"
      @success="handleUnbindEmailSuccess"
      @error="(message) => ElMessage.error(message)"
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
.secondary-action-btn:hover {
  border-color: #409eff;
  color: #409eff;
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

</style>
