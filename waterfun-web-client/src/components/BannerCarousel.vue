<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ArrowRight, ArrowLeft } from '@element-plus/icons-vue'
import { getBannersByPosition, type BannerResponse } from '~/api/bannerApi'

const banners = ref<BannerResponse[]>([])
const loading = ref(true)
const current = ref(0)
let autoPlayTimer: ReturnType<typeof setInterval> | null = null
let progressTimer: ReturnType<typeof setInterval> | null = null
const progressVal = ref(0)
const interval = 5000

const total = computed(() => banners.value.length)

const fetchBanners = async () => {
  try {
    const res = await getBannersByPosition('HOME')
    banners.value = (res.data as BannerResponse[]).sort((a, b) => a.sortNo - b.sortNo)
    if (banners.value.length > 0) startAutoPlay()
  } catch {
    banners.value = []
  } finally {
    loading.value = false
  }
}

const goTo = (index: number) => {
  current.value = index
  resetProgress()
}

const next = () => {
  if (total.value === 0) return
  goTo((current.value + 1) % total.value)
}

const prev = () => {
  if (total.value === 0) return
  goTo((current.value - 1 + total.value) % total.value)
}

const resetProgress = () => {
  progressVal.value = 0
  stopProgress()
  progressTimer = setInterval(() => {
    progressVal.value = Math.min(progressVal.value + 100 / (interval / 50), 100)
  }, 50)
}

const stopProgress = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

const startAutoPlay = () => {
  stopAutoPlay()
  autoPlayTimer = setInterval(() => next(), interval)
  resetProgress()
}

const stopAutoPlay = () => {
  if (autoPlayTimer) {
    clearInterval(autoPlayTimer)
    autoPlayTimer = null
  }
  stopProgress()
}

const handlePrev = () => { prev(); startAutoPlay() }
const handleNext = () => { next(); startAutoPlay() }
const handleDot = (i: number) => { goTo(i); startAutoPlay() }
const handleMouseEnter = () => stopAutoPlay()
const handleMouseLeave = () => startAutoPlay()

// Touch support
let touchStartX = 0
const handleTouchStart = (e: TouchEvent) => {
  const touch = e.touches[0]
  if (touch) touchStartX = touch.clientX
}
const handleTouchEnd = (e: TouchEvent) => {
  const touch = e.changedTouches[0]
  if (!touch) return
  const diff = touchStartX - touch.clientX
  if (Math.abs(diff) > 50) {
    diff > 0 ? handleNext() : handlePrev()
  }
}

onMounted(() => {
  fetchBanners()
})

onUnmounted(() => {
  stopAutoPlay()
})
</script>

<template>
  <div v-loading="loading" class="carousel-section">
    <div
      v-if="banners.length > 0"
      class="carousel"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
      @touchstart.passive="handleTouchStart"
      @touchend.passive="handleTouchEnd"
    >
      <div class="carousel-track" :style="{ transform: `translateX(-${current * 100}%)` }">
        <a
          v-for="banner in banners"
          :key="banner.id"
          :href="banner.linkUrl || '#'"
          :target="banner.linkUrl ? '_blank' : '_self'"
          class="carousel-slide"
        >
          <div class="carousel-slide-bg">
            <img v-if="banner.presignedUrl?.url" :src="banner.presignedUrl.url" :alt="banner.title">
            <div v-else class="carousel-placeholder" />
          </div>
          <div class="carousel-slide-overlay" />
          <div class="carousel-content">
            <div class="carousel-tag">{{ banner.subtitle || '推荐' }}</div>
            <div class="carousel-title">{{ banner.title }}</div>
            <span class="carousel-btn">
              了解更多 <el-icon size="12"><ArrowRight /></el-icon>
            </span>
          </div>
        </a>
      </div>

      <button class="carousel-arrow prev" @click="handlePrev">
        <el-icon size="16"><ArrowLeft /></el-icon>
      </button>
      <button class="carousel-arrow next" @click="handleNext">
        <el-icon size="16"><ArrowRight /></el-icon>
      </button>

      <div class="carousel-indicators">
        <button
          v-for="(_, i) in banners"
          :key="i"
          :class="['carousel-dot', { active: i === current }]"
          @click="handleDot(i)"
        />
      </div>

      <div class="carousel-progress" :style="{ width: progressVal + '%' }" />
    </div>
  </div>
</template>

<style scoped>
.carousel-section {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 0;
}

.carousel {
  position: relative;
  width: 100%;
  height: 320px;
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px 0 rgba(0,0,0,0.05), 0 1px 2px -1px rgba(0,0,0,0.05);
}

.carousel-track {
  display: flex;
  height: 100%;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.carousel-slide {
  min-width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  padding: 0 60px;
  text-decoration: none;
}

.carousel-slide-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.carousel-slide-bg img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
}

.carousel-slide-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(0,0,0,0.6) 0%, rgba(0,0,0,0.3) 50%, rgba(0,0,0,0.1) 100%);
  z-index: 1;
}

.carousel-content {
  position: relative;
  z-index: 2;
  max-width: 500px;
  color: white;
}

.carousel-tag {
  display: inline-block;
  padding: 4px 14px;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 16px;
  border: 1px solid rgba(255,255,255,0.15);
}

.carousel-title {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.3;
  margin-bottom: 12px;
  text-shadow: 0 2px 10px rgba(0,0,0,0.3);
}

.carousel-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: white;
  color: #1e293b;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
}

.carousel-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.2);
}

.carousel-btn i {
  font-size: 12px;
  transition: transform 0.2s;
}

.carousel-btn:hover i {
  transform: translateX(3px);
}

.carousel-indicators {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 3;
}

.carousel-dot {
  width: 8px;
  height: 8px;
  border-radius: 4px;
  background: rgba(255,255,255,0.4);
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 0;
}

.carousel-dot.active {
  width: 28px;
  background: white;
}

.carousel-dot:hover {
  background: rgba(255,255,255,0.7);
}

.carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.2);
  color: white;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3;
  transition: all 0.2s ease;
}

.carousel-arrow:hover {
  background: rgba(255,255,255,0.3);
  transform: translateY(-50%) scale(1.05);
}

.carousel-arrow.prev { left: 16px; }
.carousel-arrow.next { right: 16px; }

.carousel-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 3px;
  background: #3b82f6;
  z-index: 3;
  transition: width 0.1s linear;
  border-radius: 0 0 0 12px;
}

@media (max-width: 1024px) {
  .carousel {
    height: 260px;
  }
  .carousel-title {
    font-size: 22px;
  }
}

@media (max-width: 768px) {
  .carousel-section {
    padding: 16px;
  }
  .carousel {
    height: 220px;
  }
  .carousel-slide {
    padding: 0 30px;
  }
  .carousel-title {
    font-size: 18px;
  }
  .carousel-arrow {
    width: 36px;
    height: 36px;
  }
}
</style>
