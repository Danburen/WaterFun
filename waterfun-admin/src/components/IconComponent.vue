<script setup lang="ts">
// @ts-ignore
import { Edit, VideoCamera } from '@element-plus/icons-vue'
import DashBoard from '@/assets/icons/dashboard.svg'
const props = defineProps<{
  icon: string
}>()
const Icons = { Edit,VideoCamera,DashBoard }

const getIconComponent = (name) =>{
  console.log(name)
  return Icons[name] || null
}

</script>

<template>
  <component v-if="icon.startsWith('el-')" :is="getIconComponent(icon.replace('el-','')) " />
  <img v-else :src="getIconComponent(icon)" alt="icon">
</template>

<style scoped>

</style>