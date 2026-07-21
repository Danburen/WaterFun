<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { type FormInstance, type FormRules, ElMessage } from 'element-plus';
import { validatePassword } from '~/utils/validator';
import { useRoute, useRouter } from 'vue-router';
import { useUserInfoStore } from '~/stores/userInfoStore';
import { resetPassword, setPassword } from '~/api/accountApi';
import ReAuthDialog from '~/components/auth/ReAuthDialog.vue';
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

const scene = computed(() => isSetMode.value ? 'set_password' as const : 'reset_password' as const);

// re-auth 弹窗
const showReAuth = ref(false);

const passwordFormRef = ref<FormInstance>();
const buttonLoad = ref(false);

const passwordForm = reactive({
  oldPassword: '',
  password: '',
  newPassword: '',
  confirmPassword: '',
});

const passwordRules = computed<FormRules<typeof passwordForm>>(() => ({
  oldPassword: isSetMode.value ? [] : [
    { validator: validatePassword(false), trigger: 'blur' },
  ],
  password: isSetMode.value ? [
    { validator: validatePassword(false), trigger: 'blur' },
  ] : [],
  newPassword: isSetMode.value ? [] : [
    { validator: validatePassword(false), trigger: 'blur' },
  ],
  confirmPassword: [
    { validator: validatePassword(false), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        const pwd = isSetMode.value ? passwordForm.password : passwordForm.newPassword;
        if (value !== pwd) callback(new Error('两次输入的密码不一致'));
        else callback();
      },
      trigger: 'blur',
    },
  ],
}));

const handleReAuthSuccess = async (reAuthToken: string) => {
  buttonLoad.value = true;
  try {
    if (isSetMode.value) {
      await setPassword(passwordForm.password, passwordForm.confirmPassword, reAuthToken);
      ElMessage.success('密码设置成功');
    } else {
      await resetPassword(passwordForm.newPassword, passwordForm.confirmPassword, reAuthToken);
      ElMessage.success('密码重置成功');
    }
    passwordForm.oldPassword = '';
    passwordForm.password = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
    passwordFormRef.value?.resetFields();
    router.push('/profile');
  } catch (error: any) {
    ElMessage.error(`操作失败，请重试(${error.message})`);
  } finally {
    buttonLoad.value = false;
  }
};

const handleSubmit = () => {
  if (!passwordFormRef.value) return;
  passwordFormRef.value.validate((valid) => {
    if (valid) showReAuth.value = true;
  });
};

onMounted(async () => {
  await userInfoStore.fetchAndUpdateUserInfo();
  await userProfileStore.fetchAndUpdateUserProfile();
  const hasPwd = !!userInfoStore.userInfo.passwordHash;
  if (hasPwd && isSetMode.value) router.push('/account/password?mode=reset');
  if (!hasPwd && !isSetMode.value) router.push('/account/password?mode=set');
});
</script>

<template>
  <div class="password-page">
    <div class="password-form-container">
      <h2 class="form-title">{{ isSetMode ? '设置密码' : '重置密码' }}</h2>
      <div class="form-description">
        <template v-if="isSetMode">
          您还没有设置账号密码，请先设置密码。<br>设置密码后，您可以使用密码登录账号。
        </template>
        <template v-else>
          请输入您的旧密码和新密码，确保新密码符合安全要求。
        </template>
      </div>

      <el-form :model="passwordForm" ref="passwordFormRef" :rules="passwordRules" class="password-form">
        <el-form-item v-if="!isSetMode" label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" show-password class="form-input" />
        </el-form-item>
        <el-form-item v-if="isSetMode" label="密码" prop="password">
          <el-input v-model="passwordForm.password" type="password" placeholder="请输入密码" show-password class="form-input" />
        </el-form-item>
        <el-form-item v-else label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password class="form-input" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password class="form-input" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" @click="handleSubmit" :loading="buttonLoad">
            {{ isSetMode ? '设置密码' : '重置密码' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>

  <ReAuthDialog
    :visible="showReAuth"
    :scene="scene"
    @update:visible="showReAuth = $event"
    @success="handleReAuthSuccess"
  />
</template>

<style scoped>
.password-page { display: flex; justify-content: center; align-items: center; padding: 20px; height: 100%; width: 100%; }
.password-form-container { width: 100%; max-width: 300px; margin: 20px; padding: 20px; background-color: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.form-title { font-size: 20px; font-weight: 600; color: #333; margin-bottom: 16px; }
.form-description { color: #606266; margin-bottom: 24px; line-height: 1.5; }
.password-form { width: 100%; }
.form-input { width: 100%; }
@media (max-width: 768px) { .password-page { padding: 12px; align-items: flex-start; } .password-form-container { margin: 12px; padding: 16px; } .form-title { font-size: 17px; } .form-description { font-size: 13px; } }
</style>
