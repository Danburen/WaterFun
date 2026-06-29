import { defineStore } from "pinia"
import { ref, computed } from "vue"

export interface PoolAccount {
  uid: string
  username: string
  nickname: string
  avatarUrl: string
  avatarExpireAt: string | number
  token: string
  expire: number
}

export const useAccountPoolStore = defineStore('accountPool', () => {
  const accounts = ref<PoolAccount[]>([])
  const activeUid = ref<string>('')

  const activeAccount = computed<PoolAccount | undefined>(() =>
    accounts.value.find(a => a.uid === activeUid.value)
  )

  const otherAccounts = computed<PoolAccount[]>(() =>
    accounts.value.filter(a => a.uid !== activeUid.value)
  )

  const isTokenValid = (uid: string): boolean => {
    const acc = accounts.value.find(a => a.uid === uid)
    return acc ? Date.now() < acc.expire : false
  }

  function saveCurrentAccount(info: PoolAccount) {
    const idx = accounts.value.findIndex(a => a.uid === info.uid)
    if (idx >= 0) {
      accounts.value[idx] = info
    } else {
      accounts.value.push(info)
    }
    activeUid.value = info.uid
  }

  function updateToken(uid: string, token: string, expire: number) {
    const acc = accounts.value.find(a => a.uid === uid)
    if (acc) {
      acc.token = token
      acc.expire = expire
    }
  }

  function removeAccount(uid: string) {
    accounts.value = accounts.value.filter(a => a.uid !== uid)
    if (activeUid.value === uid) {
      activeUid.value = accounts.value[0]?.uid || ''
    }
  }

  function switchTo(uid: string): PoolAccount | undefined {
    const target = accounts.value.find(a => a.uid === uid)
    if (!target) return undefined
    activeUid.value = uid
    return target
  }

  function hasAccount(uid: string): boolean {
    return accounts.value.some(a => a.uid === uid)
  }

  function clearPool() {
    accounts.value = []
    activeUid.value = ''
  }

  return {
    accounts, activeUid, activeAccount, otherAccounts,
    saveCurrentAccount, updateToken, removeAccount, switchTo,
    hasAccount, isTokenValid, clearPool,
  }
}, {
  persist: process.client ? { storage: localStorage } : false
})
