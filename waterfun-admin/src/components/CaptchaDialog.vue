<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import axios from 'axios';
import { ElMessage } from 'element-plus';
import { throttle } from '@waterfun/web-core/src/triggerControl'
import BaseDialog from '~/components/BaseDialog.vue';

interface Props { modelValue: boolean; }
interface Emits { (e: 'update:modelValue', value: boolean): void; (e: 'confirm', captcha: string, callback: (success: boolean) => void): void; (e: 'cancel'): void; }

const props = withDefaults(defineProps<Props>(), { modelValue: false });
const emit = defineEmits<Emits>();

const dialogVisible = ref(props.modelValue);
const captchaUrl = ref<string>('');
const inputCode = ref<string>('');
const loading = ref<boolean>(false);

watch(() => props.modelValue, (v) => { dialogVisible.value = v; if (v) { inputCode.value = ''; refreshCaptcha(); } });
watch(dialogVisible, (v) => { if (!v) emit('update:modelValue', false); });

const refreshCaptcha = throttle(async () => {
  try {
    loading.value = true;
    const response = await axios.get(import.meta.env.VITE_API_BASE + '/auth/captcha', { responseType: 'arraybuffer', withCredentials: true });
    const uint8Array = new Uint8Array(response.data);
    let binary = '';
    for (let i = 0; i < uint8Array.byteLength; i++) binary += String.fromCharCode(uint8Array[i]);
    captchaUrl.value = `data:image/jpeg;base64,${btoa(binary)}`;
  } catch (error) { console.error('获取验证码失败:', error); }
  finally { loading.value = false; }
}, 1000);

const confirmCaptcha = () => {
  if (!inputCode.value.trim()) { ElMessage.warning('请输入验证码'); return; }
  loading.value = true;
  emit('confirm', inputCode.value.trim(), (success) => {
    if (success && dialogVisible.value) dialogVisible.value = false;
    loading.value = false;
  });
};

const cancelCaptcha = () => { emit('cancel'); dialogVisible.value = false; };

onMounted(() => { if (props.modelValue) refreshCaptcha(); });
</script>

<template>
  <BaseDialog v-model="dialogVisible" title="请输入验证码" width="340px">
    <div class="captcha-content">
      <div class="captcha-image-container">
        <img :src="captchaUrl" alt="验证码" class="captcha-image" style="cursor: pointer;" @click="refreshCaptcha" />
        <i class="fa-solid fa-rotate refresh-icon" @click="refreshCaptcha"></i>
      </div>
      <input v-model="inputCode" class="form-input" placeholder="请输入验证码" maxlength="4" style="margin-top: 15px;" @keyup.enter="confirmCaptcha" />
    </div>
    <template #footer>
      <button class="btn" @click="cancelCaptcha">取消</button>
      <button class="btn btn-primary" :disabled="loading" @click="confirmCaptcha">
        <i v-if="loading" class="fa-solid fa-spinner fa-spin"></i> 确定
      </button>
    </template>
  </BaseDialog>
</template>

<style scoped>
.captcha-content { text-align: center; }
.captcha-image-container { display: flex; justify-content: center; align-items: center; gap: 10px; }
.captcha-image { width: 120px; height: 30px; border-radius: 4px; }
.refresh-icon { cursor: pointer; color: var(--primary); transition: transform 0.2s; }
.refresh-icon:hover { transform: rotate(90deg); }
</style>
