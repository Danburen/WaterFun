<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { type FormInstance, type FormRules, ElMessage } from 'element-plus';
import { validatePassword } from '~/utils/validator';
import { useRoute, useRouter } from 'vue-router';
import { useUserInfoStore } from '~/stores/userInfoStore';
import { resetPassword, setPassword } from '~/api/accountApi';
import UnifiedVerify from '~/components/auth/verify/UnifiedVerify.vue';
import type { SecurityVerifyCodeDto } from '~/api/authApi';
import { useUserProfileStore } from '~/stores/userProfileStore';
import { useUserAccountStore } from '~/stores/userAccountStore';

const route = useRoute();
const router = useRouter();
const userInfoStore = useUserInfoStore();
const userProfileStore = useUserProfileStore();
const userAccountStore = useUserAccountStore();



const isSetMode = computed(() => {
  const queryMode = route.query.mode as string;
  if (queryMode === 'set' || queryMode === 'reset') {
    return queryMode === 'set';
  }
  return false;
});

// 验证码弹窗状态
const showVerifyDialog = ref(false);
const verifyType = ref<'email' | 'phone'>('email');
const verifyScene = computed(() => isSetMode.value ? 'set_password' : 'reset_password');

// 表单数据
const passwordFormRef = ref<FormInstance>();
const buttonLoad = ref(false);

const passwordForm = reactive({
  oldPassword: '',
  password: '',
  newPassword: '',
  confirmPassword: '',
});

// 表单验证规则 - 使用 computed 确保响应式
const passwordRules = computed<FormRules<typeof passwordForm>>(() => ({
  // 只有在重置模式下才验证旧密码
  oldPassword: isSetMode.value ? [] : [
    { validator: validatePassword(false), trigger: 'blur' },
  ],
  // 设置模式下的密码验证
  password: isSetMode.value ? [
    { validator: validatePassword(false), trigger: 'blur' },
  ] : [],
  // 重置模式下的新密码验证
  newPassword: isSetMode.value ? [] : [
    { validator: validatePassword(false), trigger: 'blur' },
  ],
  // 确认密码验证
  confirmPassword: [
    { validator: validatePassword(false), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        const passwordField = isSetMode.value ? passwordForm.password : passwordForm.newPassword;
        if (value !== passwordField) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
}));

// 处理验证码
const handleVerify = async (code: string) => {
  buttonLoad.value = true;
  try {
    const verifyDto: SecurityVerifyCodeDto = {
      code,
      channel: verifyType.value === 'phone' ? 'sms' : 'email',
      scene: verifyScene.value,
    };
    if (isSetMode.value) {
      await setPassword({
        newPwd: passwordForm.password,
        confirmPwd: passwordForm.confirmPassword,
        verify: verifyDto,
      });
      ElMessage.success('密码设置成功');
    } else {
      await resetPassword({
        oldPwd: passwordForm.oldPassword,
        newPwd: passwordForm.newPassword,
        confirmPwd: passwordForm.confirmPassword,
        verify: verifyDto,
      });
      ElMessage.success('密码重置成功');
    }

    // 重置表单
    passwordForm.oldPassword = '';
    passwordForm.password = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
    passwordFormRef.value?.resetFields();

    // 更新用户信息并跳转到个人中心
    router.push('/profile');
  } catch (error: any) {
    ElMessage.error(`操作失败，请重试(${error.message})`);
  } finally {
    buttonLoad.value = false;
  }
};

// 处理提交
const handleSubmit = () => {
  if (!passwordFormRef.value) return;
  
  passwordFormRef.value.validate((valid) => {
    if (valid) {
      // 显示验证码弹窗
      showVerifyDialog.value = true;
      verifyType.value = 'email'; // 默认使用邮箱验证
    }
  });
};

// 切换验证方式
const handleSwitchVerifyType = () => {
  verifyType.value = verifyType.value === 'email' ? 'phone' : 'email';
};

// 关闭验证码弹窗
const handleCloseVerify = () => {
  showVerifyDialog.value = false;
};

onMounted(async () => {
  await userInfoStore.fetchAndUpdateUserInfo();
  await userProfileStore.fetchAndUpdateUserProfile();
  const isPasswordSet = !!userInfoStore.userInfo.passwordHash;
  if (isPasswordSet && isSetMode.value) {
    router.push('/account/password?mode=reset');
  }
  if (!isPasswordSet && !isSetMode.value) {
    router.push('/account/password?mode=set');
  }
});
</script>

<template>
  <div class="password-page">
    <div class="password-form-container">
        <h2 class="form-title">{{ isSetMode ? '设置密码' : '重置密码' }}</h2>
        <div class="form-description">
          <template v-if="isSetMode">
            您还没有设置账号密码，请先设置密码。<br>
            设置密码后，您可以使用密码登录账号。
          </template>
          <template v-else>
            请输入您的旧密码和新密码，确保新密码符合安全要求。
            为了安全起见，您需要验证身份后才能重置密码。
          </template>
        </div>
        
        <el-form
          :model="passwordForm"
          ref="passwordFormRef"
          :rules="passwordRules"
          class="password-form"
        >
          <!-- 只有在重置模式下才显示旧密码字段 -->
          <el-form-item v-if="!isSetMode" label="旧密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入旧密码"
              show-password
              class="form-input"
            ></el-input>
          </el-form-item>
          
          <!-- 设置模式下显示密码字段 -->
          <el-form-item v-if="isSetMode" label="密码" prop="password">
            <el-input
              v-model="passwordForm.password"
              type="password"
              placeholder="请输入密码"
              show-password
              class="form-input"
            ></el-input>
          </el-form-item>
          
          <!-- 重置模式下显示新密码字段 -->
          <el-form-item v-else label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              show-password
              class="form-input"
            ></el-input>
          </el-form-item>
          
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              show-password
              class="form-input"
            ></el-input>
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              @click="handleSubmit"
              :loading="buttonLoad"
            >
              {{ isSetMode ? '设置密码' : '重置密码' }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 验证码弹窗 -->
    <UnifiedVerify
      :visible="showVerifyDialog"
      :scene="verifyScene"
      :type="verifyType"
      :can-switch="true"
      @update:visible="handleCloseVerify"
      @try-verify="handleVerify"
      @switch-type="handleSwitchVerifyType"
    />
  </template>

<style scoped>

.password-page {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  height: 100%;
  width: 100%;
}

.password-form-container {
  width: 100%;
  max-width: 300px;
  margin: 20px;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}


.form-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.form-description {
  color: #606266;
  margin-bottom: 24px;
  line-height: 1.5;
}

.password-form {
  width: 100%;
}

.form-input {
  width: 100%;
}

/* Base .submit-btn now in global.css (.password-page .submit-btn) */

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .password-page {
    padding: 12px;
    align-items: flex-start;
  }
  .password-form-container {
    margin: 12px;
    padding: 16px;
  }
  .form-title {
    font-size: 17px;
    margin-bottom: 12px;
  }
  .form-description {
    font-size: 13px;
    margin-bottom: 18px;
  }
  .password-form :deep(.el-form-item) {
    margin-bottom: 14px;
  }
  .password-form :deep(.el-form-item__label) {
    font-size: 13px;
    padding-bottom: 2px;
  }
}
</style>