<script setup lang="ts">
export interface LegalDocPropsType {
  title: string,
  content: string,
  lastUpdate: Date
}

defineProps<LegalDocPropsType>()

const emit = defineEmits(['confirm'])

const handleClick = () =>{
  emit('confirm')
}
</script>

<template>
  <div class="header-container">
    <div class="title" style="font-size: 30px">{{ title }}</div>
    <div class="content">{{$t('info.lastUpdateTime',
        { year:lastUpdate.getFullYear(),
          month: lastUpdate.getMonth() + 1,
          date: lastUpdate.getDate()  })}}</div>
  </div>
  <div class="content-container">
    <div class="content-text">
      {{content}}
    </div>
    <div class="button-container">
      <el-button style="width: 80%" type="primary" @click="handleClick" size="large">{{$t('confirm.confirmReadLicences')}}</el-button>
    </div>
  </div>
  <div class="footer-container">
    <div class="footer">
      CopyRight © WaterFun.top All Rights Reserved.
    </div>
  </div>
</template>

<style scoped>
.header-container {
  background: linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%);
  justify-content: center;
  display: flex;
  padding: 30px 0;
  color: white;
  align-items: center;
  flex-direction: column;
  gap: 10px;
}

.footer-container {
  background: linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%);
  padding: 20px 0;
  text-align: center;
  color: white;
  font-size: 14px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1); /* 顶部轻微阴影增强层次感 */
}

.content-container {
  background: white;
  padding: 30px;
  min-height: 300px; /* 确保内容区域有最小高度 */
  color: #333; /* 深灰色文字提升可读性 */
  box-shadow: 0 0 15px rgba(173, 216, 230, 0.2); /* 淡蓝色阴影柔和过渡 */
  margin: 10px auto;
  max-width: 1200px; /* 限制内容宽度避免过宽 */
  white-space: pre-wrap; /* 换行 */

  display: flex;
  flex-direction: column;
}

.content-text {
  flex: 1; /* 这将使内容区域占据剩余空间 */
}

.button-container {
  display: flex;
  justify-content: center; /* 水平居中 */
  margin-top: 20px; /* 添加一些顶部间距 */
}
</style>