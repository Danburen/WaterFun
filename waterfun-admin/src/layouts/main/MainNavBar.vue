<script setup lang="ts">
import { ref } from 'vue'
import type {BreadNavItemType} from "@/layouts/types.js";
// @ts-ignore
import {Expand, Fold} from "@element-plus/icons-vue";
import {useBreadcrumbs} from "@/composables/useBreadcrumbs.js";
import UserMenu from "@/layouts/main/UserMenu.vue";
const menuCollapse = ref(false)
const emit = defineEmits(['collapse'])
const collapseButtonClick = () =>{
  menuCollapse.value = !menuCollapse.value
  emit('collapse')
}

const bcStore = useBreadcrumbs();

</script>
<template>
  <div class="header-container">
    <el-header class="app-header items-center">
      <div class="header-left">
        <div class="align-center logo">
          <el-button size="large" style="width: 40px;height: 40px;" link  @click="collapseButtonClick">
            <div class="collapse-icon" v-if="!menuCollapse"><Fold /></div>
            <div class="collapse-icon" v-else><Expand /></div>
          </el-button>
          <img src="../../assets/logo.svg" width="35px" height="35px" alt="WaterFun">
          <span style="margin-left: 0.5em; font-size: 1em">WaterFun</span>
        </div>
        <el-breadcrumb class="bread-nav" separator="/">
          <el-breadcrumb-item v-for="item in bcStore.breadcrumbs.value" :to="item.to">{{ $t(item.locale) }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-right float-right">
        <UserMenu />
      </div>
    </el-header>
  </div>
</template>

<style scoped>
.header-container {
  width: 100%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  z-index: 1;
}
.app-header {
  justify-content: space-between;
  height: 60px;
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