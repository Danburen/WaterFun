<script setup lang="ts">

</script>

<template>
  <div class="user-menu">
    <el-button link class="dropdown-header">
      <el-avatar src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" class="avatar" />
      <span class="display">Admin</span>
    </el-button>
    <div class="dropdown-menu align-center">
      <a class="item">个人中心</a>
      <a class="item">修改密码</a>
      <div class="separator default-border-bottom"></div>
      <a class="item">退出登录</a>
    </div>
  </div>
</template>

<style scoped>
.user-menu {
  position: relative;
  display: inline-block;
}

.avatar {
  height: 35px;
  width: 35px;
  margin-right: 5px;
}

.dropdown-header {
  font-size: 16px;
  cursor: pointer;
  display: flex;
  height: 40px;
  color: var(--default-dark);
}

.dropdown-menu {
  opacity: 0;
  display: flex;
  flex-direction: column;
  transform: translateY(-10px);
  pointer-events: none;
  transition: all 0.3s ease;
  position: absolute;
  right: 0;
  background-color: #f9f9f9;
  min-width: 100px;
  border: 1px solid var(--default-gray);
  box-shadow: 0 8px 16px 0 rgba(0, 0, 0, 0.2);
}

.dropdown-menu a:hover {
  background-color: #f1f1f1;
  cursor: pointer;
}

.dropdown-menu a{
  font-size: 14px;
  text-align: center;
  color: #333;
  text-decoration: none;
  padding: 10px 0;
  width: 100%;
}

.dropdown-menu .separator {
  width: 100%;
  height: 100%;
}

.user-menu:hover .dropdown-menu {
  display: flex;
  flex-direction: column;
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}

</style>