<template>
  <div class="space-error-container">
    <div class="stars"></div>
    <div class="error-content">
      <div class="astronaut">
        <component :is="randomSvg" class="floating" />
      </div>
      <div class="error-message">
        <h1>404</h1>
        <h2>迷失在太空中</h2>
        <p>我们似乎找不到您要寻找的页面</p>
        <router-link to="/" class="home-link">返回地球</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, onMounted } from 'vue';
import Astronaut1 from '@/assets/svg/astronaut-1.svg';
import Astronaut2 from '@/assets/svg/astronaut-2.svg';

export default defineComponent({
  name: 'Space404',
  setup() {
    const svgList = [Astronaut1, Astronaut2];
    const randomSvg = ref(null);

    onMounted(() => {
      const randomIndex = Math.floor(Math.random() * svgList.length);
      randomSvg.value = svgList[randomIndex];
    });

    return { randomSvg };
  }
});
</script>

<style scoped>
.space-error-container {
  min-height: 100vh;
  background: #0f0c29;
  color: white;
  overflow: hidden;
  position: relative;
}

.stars {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('@/assets/svg/stars.svg') repeat;
  animation: twinkle 5s infinite alternate;
}

.error-content {
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  position: relative;
  z-index: 1;
}

.astronaut {
  flex: 1;
  text-align: center;
}

.error-message {
  flex: 1;
  padding: 2rem;
}

.floating {
  animation: float 6s ease-in-out infinite;
}

.home-link {
  display: inline-block;
  margin-top: 1rem;
  padding: 0.75rem 1.5rem;
  background: transparent;
  color: #42b983;
  border: 2px solid #42b983;
  text-decoration: none;
  border-radius: 50px;
  transition: all 0.3s ease;
  font-weight: bold;
}

.home-link:hover {
  background: #42b983;
  color: #0f0c29;
  transform: translateY(-3px);
  box-shadow: 0 10px 20px rgba(66, 185, 131, 0.3);
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-30px) rotate(5deg);
  }
}

@keyframes twinkle {
  from { opacity: 0.3; }
  to { opacity: 1; }
}
</style>