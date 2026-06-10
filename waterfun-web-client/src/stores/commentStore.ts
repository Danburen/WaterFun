import { defineStore, acceptHMRUpdate } from 'pinia'
import * as commentApi from '~/api/commentApi'
import type { CommentResponse, CursorPageComment, CreateCommentReq } from '~/api/commentApi'

interface CommentState {
  comments: CommentResponse[]
  cursor: string | null
  hasNext: boolean
  loading: boolean
  replies: Record<string, CommentResponse[]>
  replyCursors: Record<string, number | null>
  replyHasNext: Record<string, boolean>
}

export const useCommentStore = defineStore('comment', {
  state: (): CommentState => ({
    comments: [],
    cursor: null,
    hasNext: true,
    loading: false,
    replies: {},
    replyCursors: {},
    replyHasNext: {},
  }),

  actions: {
    async fetchComments(postId: number, reset = false): Promise<void> {
      if (this.loading) return
      if (!this.hasNext && !reset) return

      this.loading = true
      try {
        const res = await commentApi.listComments({
          postId,
          cursor: reset ? undefined : this.cursor ?? undefined,
          limit: 10,
        })
        const page = res.data as unknown as CursorPageComment<CommentResponse, string>
        const list = page?.list || []

        this.comments = reset ? list : [...this.comments, ...list]
        this.cursor = page?.nextCursor ?? null
        this.hasNext = Boolean(page?.hasNext)
      } catch (err) {
        console.error('fetch comments failed:', err)
        throw err
      } finally {
        this.loading = false
      }
    },

    async fetchReplies(rootId: number, reset = false): Promise<void> {
      const key = String(rootId)
      if (this.replyCursors[key] === null && !reset) return

      try {
        const res = await commentApi.listReplies(rootId, {
          cursor: reset ? undefined : this.replyCursors[key] ?? undefined,
          limit: 10,
        })
        const page = res.data as unknown as CursorPageComment<CommentResponse, number>
        const list = page?.list || []

        this.replies[key] = reset ? list : [...(this.replies[key] || []), ...list]
        this.replyCursors[key] = page?.nextCursor ?? null
        this.replyHasNext[key] = Boolean(page?.hasNext)
      } catch (err) {
        console.error('fetch replies failed:', err)
        throw err
      }
    },

    async addComment(data: CreateCommentReq): Promise<void> {
      try {
        await commentApi.postComment(data)
        await this.fetchComments(data.postId, true)
      } catch (err) {
        console.error('add comment failed:', err)
        throw err
      }
    },

    async likeComment(commentId: number): Promise<void> {
      try {
        await commentApi.likeComment(commentId)
      } catch (err) {
        console.error('like comment failed:', err)
        throw err
      }
    },

    async removeComment(commentId: number): Promise<void> {
      try {
        await commentApi.deleteComment(commentId)
        this.comments = this.comments.filter((c) => c.id !== commentId)
      } catch (err) {
        console.error('delete comment failed:', err)
        throw err
      }
    },

    clearComments(): void {
      this.comments = []
      this.cursor = null
      this.hasNext = true
    },
  },
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useCommentStore, import.meta.hot))
}
