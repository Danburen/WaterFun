<script setup lang="ts">

</script>

<template>
  <div class="container">
    <el-card class="auth-box" shadow="always">
      <div class="logo">
        <img src="@/assets/logo.svg" width="40px" height="40px" alt="WaterFun">
        <span style="margin-left: 1em">WaterFun</span>
      </div>
      <div class="slot-container">
        <slot></slot>
      </div>
    </el-card>
  </div>
</template>

<style scoped>

/*container style*/
.container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  padding: 40px 16px;
}

.auth-box {
  width: 400px;
  border-radius: 8px;
}

.slot-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.logo{
  display: flex;
  align-items: center;
  padding: 15px;
}
</style>