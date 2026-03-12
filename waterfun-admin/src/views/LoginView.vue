<script setup lang="ts">
import { ref, reactive, onMounted} from 'vue'
import router from '~/router/index.js'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/authStore'
import {validateAuthname, validatePassword, validateVerifyCode} from "@/utils/validator";
import { useI18n } from 'vue-i18n'
import { throttle } from '@waterfun/web-core/src/triggerControl'
import { convertArrayBufferToBase64 } from "@waterfun/web-core/src/dataMapper";
import { generateFingerprint } from '@waterfun/web-core/src/fingerprint';

const { t } = useI18n()
const authStore = useAuthStore()
const loginFormRef = ref(null)
const loginForm = ref({
  username: '',
  password: '',
  captcha: ''
})

const captchaDialogVisible = ref(false);

const validRules = reactive({
  username:[{validator: validateAuthname('password'),trigger: "blur" }],
  password:[{validator: validatePassword(false),trigger: "blur"}],
  captcha:[{validator: validateVerifyCode(false), trigger: "blur"}],
})

const handleCaptchaCancel = () => {
  captchaDialogVisible.value = false;
}

const handleCaptchaConfirm = (code: string, callback: (success: boolean) => void) => {
  loginForm.value.captcha = code;
   generateFingerprint().then(deviceFp => {
        authStore.tryLogin({
          ...loginForm.value,
          deviceFp: deviceFp,
        }).then(res => {
          ElMessage.success(t('message.success.loginSuccess'));
          router.push({ name: 'dashboard' })
        }).catch((err) => {
          ElMessage.error(t('message.error.apiError'));
          console.log(err);
        })
      })
  callback(true);
}

const submitLoginForm = () => {
  loginFormRef.value?.validate((valid) => {
    if (valid) {
      captchaDialogVisible.value = true;
    } 
  });
}
</script>

<template>
  <div class="container full-page-container items-center">
    <div class="main">
      <div class="left items-center">
        <div class="logo items-center">
          <img src="../assets/logo.svg" style="height: 65px;width: 65px;padding: 10px" alt="">
          <span>WaterFun</span>
        </div>
      </div>
      <div class="form-container items-center">
        <el-form 
          class="form" 
          label-width="auto"
          ref="loginFormRef"
          :model="loginForm"
          :rules="validRules"
        >
  <el-form-item prop="username" :label="$t('auth.username')">
    <el-input v-model="loginForm.username" :placeholder="$t('auth.username')"></el-input>
  </el-form-item>
  <el-form-item prop="password" :label="$t('auth.password')">
    <el-input v-model="loginForm.password" type="password" :placeholder="$t('auth.password')"></el-input>
  </el-form-item>
        <el-form-item>
            <el-button class="login-btn" type="primary" 
            @click="submitLoginForm">{{ $t('auth.btn.login') }}</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
  <CaptchaDialog
    v-model="captchaDialogVisible"
    @confirm="handleCaptchaConfirm"
    @cancel="handleCaptchaCancel"
  />
</template>

<style scoped>
.container {
  background: linear-gradient(135deg, #3da5f3 0%, #dbeaff 100%);
}
.main {
  width: 800px;
  height: 494px;
  display: flex;
  flex-direction: row;
  background-color: var(--default-white);
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.left {
  width: 494px;
  height: 100%;
  background: linear-gradient(15deg, #7d00ff 0%, #33c2ff 100%);
  background-size: cover;
  display: flex;
  flex-direction: column;
}

.logo {
  font-size: 30px;
  color: var(--default-white);
  padding: 10px 0;
}

.form-container {
  background-color: var(--default-white);
  margin: 25px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.form {
  max-width: 230px;
}

.login-btn {
  width: 100%;
}

.captcha-container {
  display: flex;
  align-items: center;
  width: 100%;
}
.captcha-image {
  padding: 0px 5px;
  width: 120px;
  height: 25px;
  cursor: pointer;
}
.verify-code-input {
  flex: 1;
}
</style>