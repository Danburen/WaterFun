<script setup lang="ts">
import { ref, onUnmounted, onBeforeMount, watch } from 'vue';
import {ElMessage} from "element-plus";
import {useI18n} from 'vue-i18n';
import { Refresh } from '@element-plus/icons-vue';
import { sendCode, getCaptcha } from "~/api/authApi";
import type { SendCodeType, VerifyScene } from "~/api/authApi";
import { throttle } from "@waterfun/web-core/src/triggerControl";
import { generateFingerprint } from "@waterfun/web-core/src/fingerprint";
import { convertArrayBufferToBase64 } from '@waterfun/web-core/src/dataMapper';

const i18n = useI18n();

const props = defineProps<{
  username: string
  getType: 'sms' | 'email'
  scene: VerifyScene
}>();

const countDown = ref(0);
let timer = null as NodeJS.Timeout | null;

const showCaptcha = ref(false);
const captchaImage = ref('');
const captchaInput = ref('');
const captchaLoading = ref(false);
const sending = ref(false);

const getVerifyingCode = () => {
  if (countDown.value > 0) return;
  showCaptcha.value = true;
}

watch(showCaptcha, (val) => {
  if (val) refreshCaptcha();
});

const refreshCaptcha = throttle(async () => {
  captchaLoading.value = true;
  try {
    const res = await getCaptcha();
    captchaImage.value = `data:image/jpeg;base64,${convertArrayBufferToBase64(res)}`;
  } catch {
    ElMessage.error(i18n.t('message.error.apiError'));
  } finally {
    captchaLoading.value = false;
  }
}, 1000);

const confirmCaptcha = async () => {
  if (!captchaInput.value.trim()) return;
  sending.value = true;
  try {
    const requestData: SendCodeType = {
      target: props.username,
      channel: props.getType,
      scene: props.scene,
      deviceFp: await generateFingerprint(),
      captcha: captchaInput.value,
    }
    await sendCode(requestData);
    ElMessage.success(i18n.t('message.success.verificationCodeSent'));
    showCaptcha.value = false;
    captchaInput.value = '';
    countDown.value = 5;
    setUpIntervalTimer();
  } catch {
    captchaInput.value = '';
    refreshCaptcha();
  } finally {
    sending.value = false;
  }
}

const cancelCaptcha = () => {
  showCaptcha.value = false;
  captchaInput.value = '';
}

const setUpIntervalTimer = () =>{
  if(timer == null) {
    timer = setInterval(()=>{
      if(countDown.value > 0){
        countDown.value -=1;
        localStorage.setItem("verifyingCode:countDown", String(countDown.value));
      }else{
        if (timer != null) {
          clearInterval(timer);
        }
        localStorage.removeItem("verifyingCode:countDown");
      }
    },1000)
  }
}

onUnmounted(()=>{
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
})

onBeforeMount(()=>{
  if(localStorage.getItem("verifyingCode:countDown") == null){
    countDown.value = 0;
  }else{
    countDown.value = Number(localStorage.getItem("verifyingCode:countDown"));
    setUpIntervalTimer();
  }
})
</script>

<template>
  <el-button type="primary"
             underline="false"
             @click.prevent="getVerifyingCode"
             :disabled="countDown > 0">
    {{ countDown > 0 ? $t('auth.countDown',{countDown})  : $t('auth.btn.getVerificationCode') }}
  </el-button>

  <el-dialog v-model="showCaptcha"
             :title="$t('auth.verifyCode')"
             width="360px"
             :close-on-click-modal="false"
             destroy-on-close
             class="captcha-dialog">
    <div class="captcha-body">
      <div class="captcha-image-row">
        <img :src="captchaImage"
             class="captcha-img"
             :class="{ loading: captchaLoading }"
             @click="refreshCaptcha"
             :alt="$t('auth.verifyCode')" />
        <el-icon class="refresh-icon" @click="refreshCaptcha" :class="{ spinning: captchaLoading }">
          <Refresh />
        </el-icon>
      </div>
      <el-input v-model="captchaInput"
                :placeholder="$t('auth.placeholder.verifyCode')"
                maxlength="4"
                class="captcha-input"
                @keyup.enter="confirmCaptcha" />
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button class="btn-cancel" @click="cancelCaptcha">{{ $t('auth.btn.cancel') }}</el-button>
        <el-button type="primary" class="btn-confirm" @click="confirmCaptcha" :loading="sending">
          {{ $t('auth.btn.verify') }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<style scoped>
.captcha-dialog :deep(.el-dialog__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  margin: 0;
}
.captcha-dialog :deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}
.captcha-dialog :deep(.el-dialog__body) {
  padding: 20px;
}
.captcha-dialog :deep(.el-dialog__footer) {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
}

.captcha-body {
  text-align: center;
}

.captcha-image-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.captcha-img {
  width: 120px;
  height: 30px;
  border-radius: 4px;
  cursor: pointer;
  transition: opacity 0.2s;
}
.captcha-img.loading {
  opacity: 0.5;
}

.refresh-icon {
  cursor: pointer;
  font-size: 16px;
  color: #3b82f6;
  transition: transform 0.3s ease;
}
.refresh-icon:hover {
  transform: rotate(90deg);
}
.refresh-icon.spinning {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.captcha-input {
  margin-top: 16px;
  width: 100%;
}
.captcha-input :deep(.el-input__wrapper) {
  padding: 4px 12px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}
.captcha-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}
.captcha-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #3b82f6 inset, 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.captcha-input :deep(.el-input__inner) {
  font-size: 14px;
  color: #1e293b;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-cancel {
  padding: 7px 16px;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #64748b;
}
.btn-cancel:hover {
  color: #3b82f6;
  border-color: #3b82f6;
}

.btn-confirm {
  padding: 7px 16px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
}
</style>