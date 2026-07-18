<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/authStore'
import { REGEX } from '@waterfun/web-core/src/regex'
import { generateFingerprint } from '@waterfun/web-core/src/fingerprint';
import { APIError } from '@waterfun/web-core/src/errors/APIError';
import CaptchaDialog from '@/components/CaptchaDialog.vue'

const router = useRouter()
const authStore = useAuthStore()

const loginForm = ref({ username: '', password: '', captcha: '' })
const usernameErr = ref('')
const passwordErr = ref('')
const captchaDialogVisible = ref(false)
const loading = ref(false)

const validate = () => {
  usernameErr.value = ''; passwordErr.value = ''; let ok = true;
  const u = loginForm.value.username;
  if (!u) { usernameErr.value = '登录名不能为空'; ok = false; }
  else if (u.length < 4 || u.length > 20) { usernameErr.value = '用户名长度超出限制(4-20字符)'; ok = false; }
  else if (!REGEX.username.test(u)) { usernameErr.value = '用户名格式有误'; ok = false; }
  const p = loginForm.value.password;
  if (!p) { passwordErr.value = '密码不能为空'; ok = false; }
  else if (p.length < 8) { passwordErr.value = '密码长度必须大于8个字符'; ok = false; }
  else if (!/[a-z]/.test(p) || !/[A-Z]/.test(p) || !/[0-9]/.test(p)) { passwordErr.value = '密码必须包含大小写字母和数字'; ok = false; }
  return ok;
};

const handleCaptchaConfirm = (code: string, callback: (success: boolean) => void) => {
  loginForm.value.captcha = code;
  loading.value = true;
  generateFingerprint().then(deviceFp => {
    authStore.tryLogin({ ...loginForm.value, deviceFp }).then(_ => {
      ElMessage.success('登录成功'); router.push({ name: 'dashboard' })
    }).catch((err: APIError) => {
      ElMessage.error(err.message || '登录失败')
    }).finally(() => { loading.value = false })
  });
  callback(true);
};

const submitLoginForm = () => {
  if (validate()) captchaDialogVisible.value = true;
}
</script>

<template>
  <div class="login-page full-page-container items-center justify-center">
    <div class="login-card">
      <div class="login-brand">
        <div class="brand-logo">
          <img src="../assets/logo.svg" alt="WaterFun" />
        </div>
        <h1 class="brand-title">WaterFun</h1>
        <p class="brand-desc">内容管理平台</p>
        <div class="brand-decoration">
          <div class="deco-circle deco-circle-1"></div>
          <div class="deco-circle deco-circle-2"></div>
          <div class="deco-circle deco-circle-3"></div>
        </div>
      </div>
      <div class="login-form-wrapper">
        <div class="login-form-inner">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">请登录您的账户以继续</p>
          <div class="form-group">
            <label class="form-label-style">用户名</label>
            <input v-model="loginForm.username" class="form-input" :class="{ 'input-error': usernameErr }" placeholder="请输入用户名" maxlength="20" />
            <p v-if="usernameErr" class="form-error">{{ usernameErr }}</p>
          </div>
          <div class="form-group">
            <label class="form-label-style">密码</label>
            <input v-model="loginForm.password" type="password" class="form-input" :class="{ 'input-error': passwordErr }" placeholder="请输入密码" maxlength="20" @keyup.enter="submitLoginForm" />
            <p v-if="passwordErr" class="form-error">{{ passwordErr }}</p>
          </div>
          <button class="btn btn-primary login-btn" :class="{ loading }" :disabled="loading" @click="submitLoginForm">
            <span v-if="loading" class="btn-spinner"></span>
            <span v-else>登 录</span>
          </button>
        </div>
      </div>
    </div>
  </div>
  <CaptchaDialog v-model="captchaDialogVisible" @confirm="handleCaptchaConfirm" @cancel="captchaDialogVisible = false" />
</template>

<style scoped>
.login-page {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  overflow: hidden;
}

.login-page::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at 20% 50%, rgba(59, 130, 246, 0.08) 0%, transparent 50%),
              radial-gradient(ellipse at 80% 50%, rgba(139, 92, 246, 0.08) 0%, transparent 50%);
  animation: bgShift 20s ease-in-out infinite alternate;
}

@keyframes bgShift {
  0% { transform: translate(0, 0) rotate(0deg); }
  100% { transform: translate(2%, 2%) rotate(3deg); }
}

.login-card {
  display: flex;
  width: 960px;
  min-height: 540px;
  background: var(--bg-white);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
  animation: cardEnter 0.5s ease-out;
}

@keyframes cardEnter {
  from { opacity: 0; transform: translateY(30px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.login-brand {
  width: 380px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 40%, #3b82f6 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 40px 20px;
}

.brand-logo img {
  width: 72px;
  height: 72px;
  margin-bottom: 16px;
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2px;
  margin-bottom: 8px;
  position: relative;
  z-index: 2;
}

.brand-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 4px;
  position: relative;
  z-index: 2;
}

.brand-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.08);
}

.deco-circle-1 {
  width: 280px;
  height: 280px;
  top: -80px;
  right: -70px;
  animation: floatSlow 8s ease-in-out infinite alternate;
}

.deco-circle-2 {
  width: 180px;
  height: 180px;
  bottom: -40px;
  left: -40px;
  animation: floatSlow 10s ease-in-out infinite alternate-reverse;
}

.deco-circle-3 {
  width: 100px;
  height: 100px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-color: rgba(255, 255, 255, 0.04);
  animation: pulseGlow 4s ease-in-out infinite alternate;
}

@keyframes floatSlow {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(20px, -20px) scale(1.05); }
}

@keyframes pulseGlow {
  from { transform: translate(-50%, -50%) scale(1); opacity: 0.4; }
  to { transform: translate(-50%, -50%) scale(1.3); opacity: 0.8; }
}

.login-form-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36px 40px;
}

.login-form-inner {
  width: 100%;
  max-width: 360px;
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 28px;
}

.form-group {
  margin-bottom: 18px;
}

.form-label-style {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.form-input.input-error {
  border-color: var(--danger);
  background: #fef2f2;
}

.form-input.input-error:focus {
  border-color: var(--danger);
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12);
}

.login-btn {
  width: 100%;
  padding: 10px 0;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  border: none;
  border-radius: 8px;
  margin-top: 4px;
  justify-content: center;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.login-btn.loading {
  background: var(--primary);
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2.5px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
