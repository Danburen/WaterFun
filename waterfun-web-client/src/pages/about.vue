<template>
  <div>
    <HeaderNavMenu />
    <div class="placeholder-page">
      <h1>关于我们</h1>
      <p>关于页面正在建设中...</p>
      <NuxtLink to="/">返回首页</NuxtLink>
    </div>
  </div>
</template>
<style scoped>
.placeholder-page { max-width: 1280px; margin: 60px auto; text-align: center; padding: 40px; }
.placeholder-page h1 { font-size: 28px; color: #1e293b; margin-bottom: 12px; }
.placeholder-page p { color: #64748b; margin-bottom: 20px; }
.placeholder-page a { color: #3b82f6; text-decoration: none; }
</style>
