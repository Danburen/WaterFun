<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getCaptcha } from '~/api/authApi';
import { convertArrayBufferToBase64 } from '@waterfun/web-core/src/dataMapper';
import { throttle } from '@waterfun/web-core/src/triggerControl';
import { useI18n } from 'vue-i18n';

const i18n = useI18n();

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  'try-verify': [captcha: string];
}>();

const captcha = ref('');
const captchaImage = ref('');
const captchaLoading = ref(false);
const verifying = ref(false);

const refreshCaptcha = throttle(() => {
  if (!captchaLoading.value) {
    captchaLoading.value = true;
    getCaptcha()
      .then(res => {
        const base64 = convertArrayBufferToBase64(res);
        captchaImage.value = `data:image/jpeg;base64,${base64}`;
      })
      .catch(err => {
        ElMessage.error(i18n.t('message.error.apiError'));
        console.error(err);
      })
      .finally(() => {
        captchaLoading.value = false;
      });
  }
}, 1000, () => {
  ElMessage.error(i18n.t('message.throttled.clickTooFast'));
});

const handleVerify = () => {
  if (!captcha.value.trim()) {
    ElMessage.error('请输入验证码');
    return;
  }
  
  verifying.value = true;
  
  // 验证码验证逻辑可以在这里添加，如果需要后端验证
  // 目前直接返回输入的验证码
  setTimeout(() => {
    emit('try-verify', captcha.value);
    emit('update:visible', false);
    captcha.value = '';
    verifying.value = false;
  }, 500);
};

const handleClose = () => {
  emit('update:visible', false);
  captcha.value = '';
};

// 当组件可见时刷新验证码
if (props.visible) {
  refreshCaptcha();
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="图像验证码验证"
    width="400px"
    @close="handleClose"
    @update:model-value="(val: boolean) => emit('update:visible', val)"
  >
    <div class="captcha-container">
      <div class="captcha-image-wrapper">
        <img :src="captchaImage" @click="refreshCaptcha" alt="验证码" class="captcha-image" />
        <el-button type="text" @click="refreshCaptcha" :loading="captchaLoading" class="refresh-btn">
          刷新
        </el-button>
      </div>
      <el-input
        v-model="captcha"
        placeholder="请输入验证码"
        class="captcha-input"
        @keyup.enter="handleVerify"
      ></el-input>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleVerify" :loading="verifying">
          验证
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<style scoped>
.captcha-container {
  padding: 20px 0;
}

.captcha-image-wrapper {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.captcha-image {
  width: 150px;
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
}

.refresh-btn {
  margin-left: 10px;
}

.captcha-input {
  width: 100%;
}
</style>