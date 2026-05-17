<script setup lang="ts">
import { ref, reactive, watch, onMounted, computed } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { validateVerifyCode } from '~/utils/validator';
import { useUserAccountStore } from '~/stores/userAccountStore';
import { sendAuthenticationCode } from '~/api/authApi';
import type { VerifyScene } from '~/api/authApi';
import { generateFingerprint } from '@waterfun/web-core/src/fingerprint';

const i18n = useI18n();
const userAccountStore = useUserAccountStore();

const props = defineProps<{
  visible: boolean;
  scene: VerifyScene;
  type: 'email' | 'phone';
  target?: string;
  canSwitch?: boolean;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  'try-verify': [ code: string];
  'switch-type': [];
}>();

const formRef = ref<FormInstance>();
const verifying = ref(false);
const sendingCode = ref(false);

const form = reactive({
  target: '',
  verifyCode: '',
});

const isEmail = computed(() => props.type === 'email');
const title = computed(() => isEmail.value ? '邮箱验证' : '手机验证');
const label = computed(() => isEmail.value ? '邮箱' : '手机号');
const placeholder = computed(() => isEmail.value ? '获取邮箱信息中...' : '获取手机号信息中...');
const switchText = computed(() => isEmail.value ? '使用手机验证' : '使用邮箱验证');
const channel = computed(() => isEmail.value ? 'email' : 'sms');

const updateTarget = () => {
  form.target = props.target || (isEmail.value ? userAccountStore.userAccount.emailMasked : userAccountStore.userAccount.phoneMasked);
};

onMounted(() => updateTarget());

watch(() => props.visible, (newVal) => {
  if (newVal) updateTarget();
});

watch([() => props.target, () => props.type], () => {
  if (props.visible) updateTarget();
});

const rules = reactive<FormRules<typeof form>>({
  verifyCode: [{ validator: validateVerifyCode(false), trigger: 'blur' }],
});

const sendCode = async () => {
  if (!form.target) {
    ElMessage.error(`获取${label.value}信息失败`);
    return;
  }
  
  sendingCode.value = true;
  try {
    await sendAuthenticationCode({
      channel: channel.value,
      scene: props.scene,
      deviceFp: await generateFingerprint(),
    });
    ElMessage.success('验证码发送成功');
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '验证码发送失败，请重试');
  } finally {
    sendingCode.value = false;
  }
};

const handleVerify = () => {
  if (!formRef.value) return;
  formRef.value.validate((valid) => {
    if (valid) {
      verifying.value = true;
      setTimeout(() => {
        emit('try-verify', form.verifyCode);
        form.verifyCode = '';
        verifying.value = false;
      }, 500);
    }
  });
};

const handleClose = () => {
  emit('update:visible', false);
  form.verifyCode = '';
};

const handleSwitchType = () => {
  emit('switch-type');
};
</script>

<template>
  <el-dialog
    :model-value="props.visible"
    :title="title"
    width="400px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="auto"
      class="verify-form"
    >
      <el-form-item :label="label">
        <div class="verify-info">{{ form.target || placeholder }}</div>
      </el-form-item>
      
      <el-form-item label="验证码" prop="verifyCode">
        <div class="code-input-wrapper">
          <el-input
            v-model="form.verifyCode"
            placeholder="请输入验证码"
            class="code-input"
            @keyup.enter="handleVerify"
          />
          <el-button
            type="primary"
            @click="sendCode"
            :loading="sendingCode"
            :disabled="!form.target || sendingCode"
          >
            {{ sendingCode ? '发送中...' : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>
    </el-form>
    
    
    <template #footer>
      <div class="dialog-footer">
        <div v-if="props.canSwitch !== false" class="switch-tip">
          <el-button type="text" @click="handleSwitchType">
            {{ switchText }}
          </el-button>
        </div>
        <div class="buttons">
          <el-button @click="handleClose">取消</el-button>
          <el-button type="primary" @click="handleVerify" :loading="verifying">
            验证
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.verify-form {
  padding: 20px 0;
}

.verify-info {
  height: 100%;
}

.code-input-wrapper {
  display: flex;
  gap: 10px;
}

.code-input {
  flex: 1;
}

.dialog-footer {
  display: flex;
}

.switch-tip {
  margin-left: 10px;
}

.buttons {
  margin-left: auto;
  display: flex;
  flex-direction: row;
}
</style>