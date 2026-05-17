<template>
  <div class="error-container">
    <div class="error-content">
      <div class="error-number">
         <span
             v-for="(digit, index) in displayStatusCode"
             :key="index"
             :class="`digit digit-${index + 1}`">{{ digit }}</span>
      </div>
      <h2 class="error-title">页面迷路了</h2>
      <p class="error-message">它可能去太空旅行了，或者只是躲起来了</p>
      <router-link to="/" class="home-link">带我回家</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {NuxtError} from "nuxt/app";

const props = defineProps({
  error: Object as () => NuxtError
})
const statusCode = props.error?.statusCode || 404
const displayStatusCode = String(statusCode).split('')
</script>

<style scoped>
.error-container {
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f2027, #203a43, #2c5364);
  color: white;
  text-align: center;
  padding: 2rem;
}

.error-content {
  max-width: 600px;
  position: relative;
}

.error-number {
  font-size: 8rem;
  font-weight: 700;
  margin-bottom: 1rem;
  display: flex;
  justify-content: center;
}

.digit {
  display: inline-block;
  position: relative;
}

.digit-1 {
  animation: float 3s ease-in-out infinite;
}

.digit-2 {
  animation: float 3s ease-in-out infinite 0.2s;
}

.digit-3 {
  animation: float 3s ease-in-out infinite 0.4s;
}

.error-title {
  font-size: 2rem;
  margin-bottom: 1rem;
  animation: fadeIn 1s ease-out;
}

.error-message {
  font-size: 1.2rem;
  margin-bottom: 2rem;
  opacity: 0.8;
}

.satellite {
  position: relative;
  width: 100px;
  height: 60px;
  margin: 2rem auto;
  animation: orbit 12s linear infinite;
}

.home-link {
  display: inline-block;
  margin-top: 1rem;
  padding: 0.6rem 1.5rem;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  text-decoration: none;
  border-radius: 50px;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(5px);
}

.home-link:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-3px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
}

/* 动画效果 */
@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-15px);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>