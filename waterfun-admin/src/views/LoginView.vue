<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/authStore'
import { REGEX } from '@waterfun/web-core/src/regex'
import { generateFingerprint } from '@waterfun/web-core/src/fingerprint';
import { APIError } from '@waterfun/web-core/src/errors/APIError';

type ValidatorRule = unknown;
type ValidatorCallback = (error?: Error | string) => void;

const validateAuthname = (loginType: string) => {
  return (rule: ValidatorRule, value: string, callback: ValidatorCallback) => {
    if (value === '') {
      callback(new Error('登录名不能为空'));
      return;
    }
    switch (loginType) {
      case 'email':
        if (!REGEX.email.test(value)) {
          callback(new Error('邮箱格式有误'));
        }
        break;
      case 'sms':
        if (!REGEX.phone.test(value)) {
          callback(new Error('手机号有误'));
        }
        break;
      case 'password':
        if (value.length < 4 || value.length > 20) {
          callback(new Error('用户名长度超出限制(4-20字符)'));
        } else if (!REGEX.username.test(value)) {
          callback(new Error('用户名格式有误'));
        }
        break;
    }
    callback();
  }
}

const validatePassword = (allowEmpty?: boolean) => {
  return (_rule: ValidatorRule, value: string, callback: ValidatorCallback) => {
    if (!value && !allowEmpty) {
      callback(new Error('密码不能为空'));
      return;
    }

    if (allowEmpty && !value) {
      callback();
      return;
    }

    if (value.length < 8) {
      callback(new Error('密码长度必须大于8个字符'));
      return;
    }

    if (value.length > 20) {
      callback(new Error('密码长度不能超过20个字符'));
      return;
    }

    if (!/[a-z]/.test(value) || !/[A-Z]/.test(value) || !/[0-9]/.test(value)) {
      callback(new Error('密码必须包含大小写字母和数字!'));
      return;
    }

    callback();
  };
}

const validateVerifyCode = (allowEmpty: boolean) => {
  return (rule: ValidatorRule, value: string, callback: ValidatorCallback) => {
    if (!value && !allowEmpty) {
      callback(new Error('验证码不能为空'));
    } else {
      callback();
    }
  }
}

const router = useRouter()
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
        }).then(_ => {
          ElMessage.success('登录成功');
          router.push({ name: 'dashboard' })
        }).catch((err: APIError) => {

          ElMessage.error(err.message || '登录失败');
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
          <img
            src="../assets/logo.svg"
            style="height: 65px;width: 65px;padding: 10px"
            alt=""
          >
          <span>WaterFun</span>
        </div>
      </div>
      <div class="form-container items-center">
        <el-form 
          ref="loginFormRef" 
          class="form"
          label-width="auto"
          :model="loginForm"
          :rules="validRules"
        >
          <el-form-item
            prop="username"
            label="用户名"
          >
            <el-input
              v-model="loginForm.username"
              placeholder="用户名"
            />
          </el-form-item>
          <el-form-item
            prop="password"
            label="密码"
          >
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              class="login-btn"
              type="primary" 
              @click="submitLoginForm"
            >
              登录
            </el-button>
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
