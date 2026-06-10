import { defineStore, acceptHMRUpdate } from 'pinia'
import * as postApi from '~/api/postApi'
import type {
  PostCardResp,
  PostAuthorCardResp,
  PostDetailResp,
  PostAuthorDetailResp,
  PostDraftResp,
  PostSaveReq,
  OptionVOLong,
  TagResponse,
  PostListParams,
  MyPostListParams,
  PageResult
} from '~/api/postApi'

interface PostState {
  posts: PostCardResp[]
  myPosts: PostAuthorCardResp[]
  currentPost: PostDetailResp | null
  currentMyPost: PostAuthorDetailResp | null
  editDraft: PostDraftResp | null
  categories: OptionVOLong[]
  tags: TagResponse[]
  pagination: {
    number: number
    size: number
    totalPages: number
    totalElements: number
  }
  myPagination: {
    number: number
    size: number
    totalPages: number
    totalElements: number
  }
  loading: boolean
  myLoading: boolean
}

export const usePostStore = defineStore('post', {
  state: (): PostState => ({
    posts: [],
    myPosts: [],
    currentPost: null,
    currentMyPost: null,
    editDraft: null,
    categories: [],
    tags: [],
    pagination: {
      number: 0,
      size: 10,
      totalPages: 0,
      totalElements: 0
    },
    myPagination: {
      number: 0,
      size: 10,
      totalPages: 0,
      totalElements: 0
    },
    loading: false,
    myLoading: false
  }),

  getters: {
    postList: (state) => state.posts,
    myPostList: (state) => state.myPosts,
    categoryList: (state) => state.categories,
    tagList: (state) => state.tags,
  },

  actions: {
    async fetchPostList(params: PostListParams = {}): Promise<void> {
      this.loading = true
      try {
        const res = await postApi.fetchPostList({
          page: 1,
          size: 10,
          ...params
        })
        const data = res.data as unknown as PageResult<PostCardResp>
        this.posts = data.content || []
        this.pagination = {
          number: data.number || 0,
          size: data.size || 10,
          totalPages: data.totalPages || 0,
          totalElements: data.totalElements || 0
        }
      } catch (err) {
        console.error('获取帖子列表失败:', err)
        throw err
      } finally {
        this.loading = false
      }
    },

    async fetchPostDetail(id: number): Promise<void> {
      try {
        const res = await postApi.fetchPostDetail(id)
        this.currentPost = res.data as unknown as PostDetailResp
      } catch (err) {
        console.error('获取帖子详情失败:', err)
        throw err
      }
    },

    async deletePost(id: number): Promise<void> {
      try {
        await postApi.deletePost(id)
      } catch (err) {
        console.error('删除帖子失败:', err)
        throw err
      }
    },

    async fetchMyPostList(params: MyPostListParams = {}): Promise<void> {
      this.myLoading = true
      try {
        const res = await postApi.fetchMyPostList({
          page: 1,
          size: 10,
          ...params
        })
        const data = res.data as unknown as PageResult<PostAuthorCardResp>
        this.myPosts = data.content || []
        this.myPagination = {
          number: data.number || 0,
          size: data.size || 10,
          totalPages: data.totalPages || 0,
          totalElements: data.totalElements || 0
        }
      } catch (err) {
        console.error('获取我的帖子列表失败:', err)
        throw err
      } finally {
        this.myLoading = false
      }
    },

    async fetchMyPostDetail(id: number): Promise<void> {
      try {
        const res = await postApi.fetchMyPostDetail(id)
        this.currentMyPost = res.data as unknown as PostAuthorDetailResp
      } catch (err) {
        console.error('获取我的帖子详情失败:', err)
        throw err
      }
    },

    async createDraft(): Promise<number> {
      try {
        const res = await postApi.createDraft()
        return res.data as unknown as number
      } catch (err) {
        console.error('创建草稿失败:', err)
        throw err
      }
    },

    async fetchEditDraft(id: number): Promise<void> {
      try {
        const res = await postApi.fetchEditDraft(id)
        this.editDraft = res.data as unknown as PostDraftResp
      } catch (err) {
        console.error('获取编辑草稿失败:', err)
        throw err
      }
    },

    async publishPost(id: number, data: PostSaveReq): Promise<void> {
      try {
        await postApi.publishPost(id, data)
      } catch (err) {
        console.error('发布帖子失败:', err)
        throw err
      }
    },

    async tempSavePost(id: number, data: PostSaveReq): Promise<void> {
      try {
        await postApi.tempSavePost(id, data)
      } catch (err) {
        console.error('保存草稿失败:', err)
        throw err
      }
    },

    async fetchCategories(): Promise<void> {
      try {
        const res = await postApi.fetchCategories()
        this.categories = res.data as unknown as OptionVOLong[]
      } catch (err) {
        console.error('获取分类列表失败:', err)
        throw err
      }
    },

    async fetchTags(): Promise<void> {
      try {
        const res = await postApi.fetchTags()
        this.tags = res.data as unknown as TagResponse[]
      } catch (err) {
        console.error('获取标签列表失败:', err)
        throw err
      }
    },

  }
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(usePostStore, import.meta.hot))
}
