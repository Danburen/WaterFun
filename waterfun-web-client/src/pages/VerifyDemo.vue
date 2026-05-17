<script setup lang="ts">
import { ref } from 'vue';
import CaptchaVerify from '~/components/auth/verify/CaptchaVerify.vue';
import VerifyDialog from '~/components/auth/VerifyDialog.vue';

const showCaptchaVerify = ref(false);
const showVerifyDialog = ref(false);

const handleCaptchaSuccess = (captcha: string) => {
  console.log('图像验证码验证成功:', captcha);
  // 这里可以添加验证成功后的逻辑
};

const handleVerifySuccess = (target: string, code: string, type: 'email' | 'phone') => {
  console.log(`${type === 'email' ? '邮箱' : '手机'}验证成功:`, target, code);
  // 这里可以添加验证成功后的逻辑
};
</script>

<template>
  <div class="verify-demo-container">
    <h1>验证组件演示</h1>
    
    <div class="demo-buttons">
      <el-button type="primary" @click="showCaptchaVerify = true">
        显示图像验证码验证
      </el-button>
      
      <el-button type="primary" @click="showVerifyDialog = true">
        显示邮箱/手机验证
      </el-button>
    </div>
    
    <!-- 图像验证码验证组件 -->
    <captcha-verify
      v-model:visible="showCaptchaVerify"
      @verify-success="handleCaptchaSuccess"
    ></captcha-verify>
    
    <!-- 统一验证组件 -->
    <verify-dialog
      v-model:visible="showVerifyDialog"
      scene="verify"
      type="auto"
      @verify-success="handleVerifySuccess"
    />
  </div>
</template>

<style scoped>
.verify-demo-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}

h1 {
  text-align: center;
  margin-bottom: 40px;
  color: #333;
}

.demo-buttons {
  display: flex;
  gap: 20px;
  justify-content: center;
  flex-wrap: wrap;
}
</style>