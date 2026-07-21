<script setup lang="ts">
import { ref, reactive } from 'vue';
import { type FormInstance, type FormRules, ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { validatePassword } from '~/utils/validator';
import { forgotPasswordVerifyReAuth, forgotPasswordReset } from '~/api/authApi';
import VerifyingCodeButton from '~/components/auth/VerifyingCodeButton.vue';

const router = useRouter();

const formRef = ref<FormInstance>();
const buttonLoad = ref(false);
const reAuthKey = ref('');

const form = reactive({
  identifier: '',
  code: '',
  newPwd: '',
  confirmPwd: '',
});

const rules = reactive<FormRules<typeof form>>({
  identifier: [{ required: true, message: '请输入绑定的手机号/邮箱/用户名', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPwd: [{ validator: validatePassword(false), trigger: 'blur' }],
  confirmPwd: [
    { validator: validatePassword(false), trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== form.newPwd) callback(new Error('两次输入的密码不一致'));
        else callback();
      },
      trigger: 'blur',
    },
  ],
});

const handleCodeSent = (data: { reAuthKey?: string }) => {
  if (data.reAuthKey) {
    reAuthKey.value = data.reAuthKey;
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    if (!reAuthKey.value) {
      ElMessage.error('请先获取验证码');
      return;
    }
    buttonLoad.value = true;
    try {
      // Step 1: Verify code → get reAuthToken
      const verifyRes = await forgotPasswordVerifyReAuth(reAuthKey.value, form.code);
      const token = verifyRes.data?.reAuthToken;
      if (!token) {
        ElMessage.error('验证失败，请重试');
        return;
      }
      // Step 2: Reset password with reAuthToken
      await forgotPasswordReset(token, form.newPwd, form.confirmPwd);
      ElMessage.success('密码重置成功，请使用新密码登录');
      router.push('/login');
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || e.message || '密码重置失败');
    } finally {
      buttonLoad.value = false;
    }
  });
};
</script>

<template>
  <auth-box title="忘记密码" subtitle="验证身份后重置您的密码">
    <el-form ref="formRef" :model="form" :rules="rules">
      <el-form-item label="手机号/邮箱/用户名" prop="identifier">
        <el-input v-model="form.identifier" placeholder="请输入绑定的手机号、邮箱或用户名" class="login-input" />
      </el-form-item>

      <el-form-item label="验证码" prop="code">
        <el-input v-model="form.code" placeholder="请输入验证码" class="login-input">
          <template #append>
            <VerifyingCodeButton
              :username="form.identifier"
              get-type="sms"
              scene="forgot_password"
              @sent="handleCodeSent"
            />
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="新密码" prop="newPwd">
        <el-input v-model="form.newPwd" type="password" placeholder="至少8位，含大小写字母和数字" show-password class="login-input" />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPwd">
        <el-input v-model="form.confirmPwd" type="password" placeholder="请再次输入新密码" show-password class="login-input" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" class="login-btn" :loading="buttonLoad" @click="handleSubmit">
          重置密码
        </el-button>
      </el-form-item>

      <div class="form-footer" style="justify-content: center; margin-top: -4px;">
        <el-button size="small" link @click.prevent="router.push('/login')">返回登录</el-button>
      </div>
    </el-form>
  </auth-box>
</template>
