<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { type FormInstance, type FormRules, ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { REGEX } from '@waterfun/web-core/src/regex';
import { validatePassword } from '~/utils/validator';
import { forgotPasswordReset, type ForgotPasswordRequest } from '~/api/authApi';
import VerifyingCodeButton from '~/components/auth/VerifyingCodeButton.vue';

const router = useRouter();

const formRef = ref<FormInstance>();
const buttonLoad = ref(false);

const form = reactive({
  target: '',
  code: '',
  newPwd: '',
  confirmPwd: '',
});

const channelType = computed(() => form.target.includes('@') ? 'email' : 'sms');

const validateTarget = () => {
  return (_: any, value: any, callback: any) => {
    if (!value) {
      callback(new Error('请输入手机号或邮箱'));
      return;
    }
    if (value.includes('@')) {
      if (!REGEX.email.test(value)) {
        callback(new Error('邮箱格式不正确'));
        return;
      }
    } else {
      if (!REGEX.phone.test(value)) {
        callback(new Error('手机号格式不正确'));
        return;
      }
    }
    callback();
  };
};

const confirmPasswordValidator = (_: any, value: any, callback: any) => {
  if (value !== form.newPwd) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const rules = reactive<FormRules<typeof form>>({
  target: [{ validator: validateTarget(), trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPwd: [{ validator: validatePassword(false), trigger: 'blur' }],
  confirmPwd: [
    { validator: validatePassword(false), trigger: 'blur' },
    { validator: confirmPasswordValidator, trigger: 'blur' },
  ],
});

const handleSubmit = () => {
  if (!formRef.value) return;

  formRef.value.validate(async (valid) => {
    if (!valid) return;
    buttonLoad.value = true;
    try {
      const req: ForgotPasswordRequest = {
        channel: form.target.includes('@') ? 'email' : 'sms',
        target: form.target,
        code: form.code,
        newPwd: form.newPwd,
        confirmPwd: form.confirmPwd,
      };
      await forgotPasswordReset(req);
      ElMessage.success('密码重置成功，请使用新密码登录');
      router.push('/login');
    } catch (error: any) {
      ElMessage.error(error.message || '密码重置失败，请重试');
    } finally {
      buttonLoad.value = false;
    }
  });
};
</script>

<template>
  <auth-box title="忘记密码" subtitle="验证身份后重置您的密码">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      class="auth-form"
      label-position="top"
      size="large"
    >
      <el-form-item :label="channelType === 'email' ? '邮箱' : '手机号'" prop="target">
        <el-input
          v-model="form.target"
          :placeholder="channelType === 'email' ? '请输入邮箱地址' : '请输入手机号'"
          class="login-input"
        />
      </el-form-item>

      <el-form-item label="验证码" prop="code">
        <el-input
          v-model="form.code"
          placeholder="请输入验证码"
          class="login-input"
        >
          <template #append>
            <VerifyingCodeButton
              :username="form.target"
              :getType="channelType"
              scene="forgot_password"
            />
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="新密码" prop="newPwd">
        <el-input
          v-model="form.newPwd"
          type="password"
          placeholder="请输入新密码（至少8位，含大小写字母和数字）"
          show-password
          class="login-input"
        />
      </el-form-item>

      <el-form-item label="确认新密码" prop="confirmPwd">
        <el-input
          v-model="form.confirmPwd"
          type="password"
          placeholder="请再次输入新密码"
          show-password
          class="login-input"
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          class="login-btn"
          :loading="buttonLoad"
          @click="handleSubmit"
        >
          重置密码
        </el-button>
      </el-form-item>

      <div class="form-footer" style="justify-content: center; margin-top: -4px;">
        <el-button size="small" link @click.prevent="router.push('/login')">
          返回登录
        </el-button>
      </div>
    </el-form>
  </auth-box>
</template>

<style scoped>
/* Responsive handled by global.css .auth-form rules */
</style>
