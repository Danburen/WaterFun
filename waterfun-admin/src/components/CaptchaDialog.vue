<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import axios from 'axios';
import { ElMessage } from 'element-plus';
import { throttle } from '@waterfun/web-core/src/triggerControl'

// 定义组件的 props
interface Props {
  modelValue: boolean; // 控制对话框显示隐藏
}

// 定义组件的 emits
interface Emits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'confirm', captcha: string, callback: (success: boolean) => void): void;
  (e: 'cancel'): void;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false
});
const emit = defineEmits<Emits>();

// 响应式数据
const dialogVisible = ref(props.modelValue);
const captchaUrl = ref<string>('');
const inputCode = ref<string>('');
const loading = ref<boolean>(false);

// 监听 modelValue 变化
watch(() => props.modelValue, (newVal) => {
  dialogVisible.value = newVal;
  if (newVal) {
    inputCode.value = '';
    refreshCaptcha();
  }
});

// 监听对话框关闭
watch(dialogVisible, (newVal) => {
  if (!newVal) {
    emit('update:modelValue', false);
  }
});

// 刷新验证码
const refreshCaptcha = throttle(async () => {
  try {
    loading.value = true;
    const response = await axios.get(import.meta.env.VITE_API_BASE + '/auth/captcha', {
      responseType: 'arraybuffer',
      withCredentials: true
    });
    
    const uint8Array = new Uint8Array(response.data);
    let binary = '';
    for (let i = 0; i < uint8Array.byteLength; i++) {
      binary += String.fromCharCode(uint8Array[i]);
    }
    const base64 = btoa(binary);
    captchaUrl.value = `data:image/jpeg;base64,${base64}`;
    loading.value = false;
  } catch (error) {
    console.error('获取验证码失败:', error);
    loading.value = false;
  }
}, 1000);

const confirmCaptcha = () => {
  if (!inputCode.value.trim()) {
    ElMessage.warning('请输入验证码');
    return;
  }
  loading.value = true;
  emit('confirm', inputCode.value.trim(), (success) => {
    console.log('confirmCaptcha success:', success);
    if (success && dialogVisible.value) {
      dialogVisible.value = false;
    }
    loading.value = false;
  })
};

const cancelCaptcha = () => {
  emit('cancel');
  dialogVisible.value = false;
};

onMounted(() => {
  if (props.modelValue) {
    refreshCaptcha();
  }
});
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="请输入验证码"
    width="300px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    center
  >
    <div class="captcha-content">
      <div class="captcha-image-container">
        <img
          :src="captchaUrl"
          alt="验证码"
          class="captcha-image"
          @click="refreshCaptcha"
          style="cursor: pointer;"
        />
        <el-icon
          :size="20"
          class="refresh-icon"
          @click="refreshCaptcha"
          style="cursor: pointer;"
        >
          <Refresh />
        </el-icon>
      </div>
      <el-input
        v-model="inputCode"
        placeholder="请输入验证码"
        maxlength="4"
        @keyup.enter="confirmCaptcha"
        style="margin-top: 15px;"
      >
      </el-input>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="cancelCaptcha">取消</el-button>
        <el-button type="primary" @click="confirmCaptcha" :loading="loading">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>



<style scoped>
.captcha-content {
  text-align: center;
}

.captcha-image-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.captcha-image {
  width: 120px;
  height: 30px;
  border-radius: 4px;
}

.refresh-icon {
  margin-left: 10px;
  color: #409eff;
  transition: transform 0.2s;
}

.refresh-icon:hover {
  transform: rotate(90deg);
  color: #66b1ff;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
}
</style>