import { defineStore, acceptHMRUpdate } from 'pinia'
import * as notificationApi from '~/api/notificationApi'
import type { InboxNotificationRes, NotificationType, NotificationGroup, CursorPageLong } from '~/api/notificationApi'
import { useAuthStore } from '~/stores/authStore'
import { useUserInfoStore } from '~/stores/userInfoStore'
import { SseClient } from '~/utils/sseClient'

let sseClient: SseClient | null = null

interface NotificationState {
  notifications: Record<string, InboxNotificationRes[]>
  cursors: Record<string, number | null>
  hasNext: Record<string, boolean>
  loading: Record<string, boolean>
  unreadCount: number
  sseConnected: boolean
}

const SYSTEM = 'system'
const REPLY = 'reply'
const MENTION = 'mention'
const SUBSCRIBE = 'subscribe'

export const useNotificationStore = defineStore('notification', {
  state: (): NotificationState => ({
    notifications: {
      [SYSTEM]: [],
      [REPLY]: [],
      [MENTION]: [],
      [SUBSCRIBE]: [],
    },
    cursors: {
      [SYSTEM]: null,
      [REPLY]: null,
      [MENTION]: null,
      [SUBSCRIBE]: null,
    },
    hasNext: {
      [SYSTEM]: true,
      [REPLY]: true,
      [MENTION]: true,
      [SUBSCRIBE]: true,
    },
    loading: {
      [SYSTEM]: false,
      [REPLY]: false,
      [MENTION]: false,
      [SUBSCRIBE]: false,
    },
    unreadCount: 0,
    sseConnected: false,
  }),

  getters: {
    getNotifications:
      (state) =>
      (tab: string): InboxNotificationRes[] =>
        state.notifications[tab] || [],
    isLoading:
      (state) =>
      (tab: string): boolean =>
        state.loading[tab] || false,
    hasMore:
      (state) =>
      (tab: string): boolean =>
        state.hasNext[tab] ?? true,
  },

  actions: {
    getQueryParams(tab: string): { type?: NotificationType; group?: NotificationGroup } {
      switch (tab) {
        case SYSTEM:
          return { group: 'SYSTEM' }
        case REPLY:
          return { type: 'REPLY' }
        case MENTION:
          return { type: 'MENTION' }
        case SUBSCRIBE:
          return { group: 'INTERACTION' }
        default:
          return {}
      }
    },

    async fetchNotifications(tab: string, reset = false): Promise<void> {
      if (this.loading[tab]) return
      if (!this.hasNext[tab] && !reset) return

      this.loading[tab] = true
      try {
        const params = this.getQueryParams(tab)
        const res = await notificationApi.listNotifications({
          cursor: reset ? undefined : this.cursors[tab] ?? undefined,
          limit: 10,
          ...params,
        })
        const page = res.data as unknown as CursorPageLong<InboxNotificationRes>
        const list = page?.list || []

        this.notifications[tab] = reset ? list : [...this.notifications[tab], ...list]
        this.cursors[tab] = page?.nextCursor ?? null
        this.hasNext[tab] = Boolean(page?.hasNext)
      } catch (err) {
        console.error(`fetch ${tab} notifications failed:`, err)
        throw err
      } finally {
        this.loading[tab] = false
      }
    },

    async fetchUnreadCount(): Promise<void> {
      try {
        const res = await notificationApi.getUnreadCount()
        this.unreadCount = (res.data as unknown as number) || 0
      } catch (err) {
        console.error('fetch unread count failed:', err)
      }
    },

    async markAsRead(id: string): Promise<void> {
      try {
        await notificationApi.markNotificationRead(id)
        for (const key of Object.keys(this.notifications)) {
          const idx = this.notifications[key].findIndex((n) => n.id === id)
          if (idx !== -1) {
            this.notifications[key][idx].isRead = true
            break
          }
        }
        if (this.unreadCount > 0) this.unreadCount--
      } catch (err) {
        console.error('mark notification read failed:', err)
      }
    },

    async markAllRead(): Promise<void> {
      try {
        await notificationApi.markAllNotificationsRead()
        for (const key of Object.keys(this.notifications)) {
          this.notifications[key].forEach((n) => (n.isRead = true))
        }
        this.unreadCount = 0
      } catch (err) {
        console.error('mark all notifications read failed:', err)
      }
    },

    connectSSE(): void {
      if (sseClient || this.sseConnected || !process.client) return
      const authStore = useAuthStore()
      const token = authStore.accessData.token
      if (!token) return
      const uid = useUserInfoStore().userInfo.uid
      if (!uid) return

      const base = import.meta.env.VITE_API_BASE || '//localhost:8080/api'
      const url = `${base}/notifications/sse`

      this.sseConnected = true
      sseClient = new SseClient(url, {
        'Authorization': `Bearer ${token}`,
      })

      sseClient.addEventListener('notification', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data) as InboxNotificationRes
          this.unreadCount++
          const tab = data.noticeType === 1 ? 'system' : data.noticeType === 2 ? 'reply' : data.noticeType === 3 ? 'mention' : 'subscribe'
          if (this.notifications[tab]) {
            this.notifications[tab].unshift(data)
          }
        } catch { /* ignore parse errors */ }
      })

      sseClient.addEventListener('heartbeat', () => {})

      sseClient.connect()
    },

    disconnectSSE(): void {
      this.sseConnected = false
      if (sseClient) {
        sseClient.close()
        sseClient = null
      }
    },

    async deleteNotification(id: string): Promise<void> {
      try {
        await notificationApi.deleteNotification(id)
        for (const key of Object.keys(this.notifications)) {
          this.notifications[key] = this.notifications[key].filter((n) => n.id !== id)
        }
      } catch (err) {
        console.error('delete notification failed:', err)
      }
    },
  },
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useNotificationStore, import.meta.hot))
}
