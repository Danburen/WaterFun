<script setup lang="ts">
defineProps<{
  title?: string
  subtitle?: string
}>()
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="brand-logo">
          <img src="@/assets/logo.svg" alt="WaterFun" />
        </div>
        <h1 class="brand-title">WaterFun</h1>
        <p class="brand-desc">{{ subtitle || '内容管理平台' }}</p>
        <div class="brand-decoration">
          <div class="deco-circle deco-circle-1"></div>
          <div class="deco-circle deco-circle-2"></div>
          <div class="deco-circle deco-circle-3"></div>
        </div>
      </div>
      <div class="login-form-wrapper">
        <div class="login-form-inner">
          <h2 class="form-title">{{ title || '欢迎' }}</h2>
          <p class="form-subtitle">{{ subtitle || '请登录您的账户以继续' }}</p>
          <slot />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.login-page::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at 20% 50%, rgba(59, 130, 246, 0.08) 0%, transparent 50%),
              radial-gradient(ellipse at 80% 50%, rgba(139, 92, 246, 0.08) 0%, transparent 50%);
  animation: bgShift 20s ease-in-out infinite alternate;
}

@keyframes bgShift {
  0% { transform: translate(0, 0) rotate(0deg); }
  100% { transform: translate(2%, 2%) rotate(3deg); }
}

.login-card {
  display: flex;
  width: 960px;
  min-height: 540px;
  background: var(--wf-bg-white, #fff);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
  animation: cardEnter 0.5s ease-out;
}

@keyframes cardEnter {
  from { opacity: 0; transform: translateY(30px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.login-brand {
  width: 380px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 40%, #3b82f6 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 40px 20px;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: #fff;
  margin-bottom: 16px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.brand-logo img {
  width: 64px;
  height: 64px;
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2px;
  margin-bottom: 8px;
  position: relative;
  z-index: 2;
}

.brand-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 4px;
  position: relative;
  z-index: 2;
}

.brand-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.08);
}

.deco-circle-1 {
  width: 280px;
  height: 280px;
  top: -80px;
  right: -70px;
  animation: floatSlow 8s ease-in-out infinite alternate;
}

.deco-circle-2 {
  width: 180px;
  height: 180px;
  bottom: -40px;
  left: -40px;
  animation: floatSlow 10s ease-in-out infinite alternate-reverse;
}

.deco-circle-3 {
  width: 100px;
  height: 100px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-color: rgba(255, 255, 255, 0.04);
  animation: pulseGlow 4s ease-in-out infinite alternate;
}

@keyframes floatSlow {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(20px, -20px) scale(1.05); }
}

@keyframes pulseGlow {
  from { transform: translate(-50%, -50%) scale(1); opacity: 0.4; }
  to { transform: translate(-50%, -50%) scale(1.3); opacity: 0.8; }
}

.login-form-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36px 40px;
}

.login-form-inner {
  width: 100%;
  max-width: 360px;
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 4px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--wf-text-muted, #94a3b8);
  margin: 0 0 24px;
}
</style>
