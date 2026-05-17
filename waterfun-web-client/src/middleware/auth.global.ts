import { useAuthStore } from "~/stores/authStore";
import { navigateTo } from "nuxt/app";

export default defineNuxtRouteMiddleware((to, from) => {
  const whiteList = ['/login', '/register', '/']
   const inWhiteList = whiteList.some(p =>
    typeof p === 'string' ? p === to.path : (p as RegExp).test(to.path)
  )
  if (inWhiteList) {
    return
  }

  if (import.meta.server) return
  const authStore = useAuthStore()
  if (!authStore.isAccess) {
    return navigateTo(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }
})