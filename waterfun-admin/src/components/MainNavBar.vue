<script setup lang="ts">
import { ref } from 'vue'
import { Expand, Fold, ArrowRight} from "@element-plus/icons-vue";
const menuCollapse = ref(false)
const emit = defineEmits(['collapse'])
const collapseButtonClick = () =>{
  menuCollapse.value = !menuCollapse.value
  emit('collapse')
}
</script>
<template>
  <div class="header-container">
    <el-header class="app-header items-center default-border-bottom">
      <div class="header-left">
        <div class="align-center logo">
          <el-button size="large" style="width: 40px;height: 40px;" link  @click="collapseButtonClick">
            <div class="collapse-icon" v-if="!menuCollapse"><Fold /></div>
            <div class="collapse-icon" v-else><Expand /></div>
          </el-button>
          <img src="@/assets/logo.svg" width="35px" height="35px" alt="WaterFun">
          <span style="margin-left: 0.5em; font-size: 1em">WaterFun</span>
        </div>
        <el-breadcrumb class="bread-nav" separator="/">
          <el-breadcrumb-item :to="{ path: '/'}">{{ $t('nav.home') }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-right">
        <div class="user-center">

        </div>
      </div>
    </el-header>
  </div>
</template>

<style scoped>
.header-container {
  width: 100%;
}
.app-header {
  justify-content: space-between;
  height: 60px;
  background-color: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px; /* 控制子元素间距 */
  flex: 1; /* 占据剩余空间 */
  min-width: 0; /* 防止内容溢出 */
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-right: auto;
}

.collapse-icon {
  color: #303133;
  width: 1.5em;
  height: 1.5em;
}

.bread-nav {
  padding: 0.5em 1em;
}
</style>