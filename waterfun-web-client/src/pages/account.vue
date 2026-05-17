<template>

  <div class="container">
    <HeaderNavMenu />
    <main class="account-main">
      <NuxtPage />
    </main>
    <footer class="account-footer">
      <div class="footer-content">
        <p>&copy; {{ new Date().getFullYear() }} WaterFun. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  title: '账户管理',
  ssr: false
})
</script>

<style scoped>
.container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.account-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.account-footer {
  background-color: #f5f7fa;
  border-top: 1px solid #e4e7ed;
  width: 100%;
  padding: 16px 24px;
  margin-top: auto;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
</style>