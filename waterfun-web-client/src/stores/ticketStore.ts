import { defineStore, acceptHMRUpdate } from 'pinia'
import * as ticketApi from '~/api/ticketApi'
import type { UserTicketListResponse, UserTicketDetailResponse, TicketStatsResponse, TicketType, TicketStatus, CreateUserReportReq, PageResult } from '~/api/ticketApi'

interface TicketState {
  tickets: UserTicketListResponse[]
  currentTicket: UserTicketDetailResponse | null
  stats: TicketStatsResponse | null
  pagination: {
    number: number
    size: number
    totalPages: number
    totalElements: number
  }
  loading: boolean
  detailLoading: boolean
  filters: {
    ticketType: TicketType | ''
    status: TicketStatus | ''
  }
}

export const useTicketStore = defineStore('ticket', {
  state: (): TicketState => ({
    tickets: [],
    currentTicket: null,
    stats: null,
    pagination: {
      number: 0,
      size: 10,
      totalPages: 0,
      totalElements: 0
    },
    loading: false,
    detailLoading: false,
    filters: {
      ticketType: '',
      status: ''
    }
  }),

  getters: {
    ticketList: (state) => state.tickets,
    ticketStats: (state) => state.stats,
  },

  actions: {
    async fetchTickets(page: number = 1) {
      this.loading = true
      try {
        const params: any = { page, size: this.pagination.size }
        if (this.filters.ticketType) params.ticketType = this.filters.ticketType
        if (this.filters.status) params.status = this.filters.status
        const res = await ticketApi.fetchTicketList(params)
        this.tickets = res.data.content
        this.pagination = {
          number: res.data.number,
          size: res.data.size,
          totalPages: res.data.totalPages,
          totalElements: res.data.totalElements
        }
      } catch {
        this.tickets = []
      } finally {
        this.loading = false
      }
    },

    async fetchTicketDetail(id: string) {
      this.detailLoading = true
      try {
        const res = await ticketApi.fetchTicketDetail(id)
        this.currentTicket = res.data
      } catch {
        this.currentTicket = null
      } finally {
        this.detailLoading = false
      }
    },

    async createTicket(data: CreateUserReportReq) {
      const res = await ticketApi.createTicket(data)
      return res.data
    },

    async cancelTicket(id: string) {
      await ticketApi.cancelTicket(id)
      // reload current ticket if viewing it
      if (this.currentTicket?.ticketId === id) {
        this.currentTicket = null
      }
    },

    async fetchStats() {
      try {
        const res = await ticketApi.fetchTicketStats()
        this.stats = res.data
      } catch {
        this.stats = null
      }
    },

    setFilters(filters: { ticketType?: TicketType; status?: TicketStatus }) {
      if (filters.ticketType !== undefined) this.filters.ticketType = filters.ticketType
      if (filters.status !== undefined) this.filters.status = filters.status
    },

    clearCurrentTicket() {
      this.currentTicket = null
    }
  }
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useTicketStore, import.meta.hot))
}
