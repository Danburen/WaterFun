<script setup lang="ts">
import { usePostStore } from '~/stores/postStore'
import { useCommentStore } from '~/stores/commentStore'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { likePost, collectPost, fetchPostList, reportPost, getPostLikedUsers, type LikedUsersResp } from '~/api/postApi'
import { likeComment, reportComment } from '~/api/commentApi'
import type { ReportType } from '~/api/ticketApi'
import UserBadge from '~/components/UserBadge.vue'
import EmojiPicker from '~/components/EmojiPicker.vue'
import ReportDropdown from '~/components/ReportDropdown.vue'
import { useUserInfoStore } from '~/stores/userInfoStore'
import MarkdownIt from 'markdown-it'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '~/stores/authStore'
import type { PostCardResp, UserBrief } from '~/api/postApi'

const postStore = usePostStore()
const commentStore = useCommentStore()
const { currentPost, loading } = storeToRefs(postStore)
const allComments = computed(() => commentStore.comments)
const hasNextComments = computed(() => commentStore.hasNext)
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const userInfoStore = useUserInfoStore()
const { userInfo } = storeToRefs(userInfoStore)

const md = new MarkdownIt()
const renderedContent = computed(() => currentPost.value ? md.render(currentPost.value.content) : '')

const postId = computed(() => String(route.params.id))
const liked = ref(false)
const collected = ref(false)
const likeCount = ref(0)
const collectCount = ref(0)
const commentCount = ref(0)
const likedUsers = ref<LikedUsersResp | null>(null)

const commentText = ref('')
const commentTextareaRef = ref<HTMLTextAreaElement | null>(null)
const submitting = ref(false)

const replyTarget = ref<{ commentId: string; displayName: string } | null>(null)
const replyText = ref('')
const replyTextareaRef = ref<HTMLTextAreaElement | null>(null)
const replySubmitting = ref(false)
const expandedReplies = ref<Set<string>>(new Set())

const insertEmoji = (emoji: string, el: HTMLTextAreaElement | null, text: Ref<string>) => {
  if (!el) {
    text.value += emoji
    return
  }
  const start = el.selectionStart
  const end = el.selectionEnd
  const before = text.value.slice(0, start)
  const after = text.value.slice(end)
  text.value = before + emoji + after
  nextTick(() => {
    const pos = start + emoji.length
    el.setSelectionRange(pos, pos)
    el.focus()
  })
}

const onCommentEmoji = (emoji: string) => insertEmoji(emoji, commentTextareaRef.value, commentText)
const onReplyEmoji = (emoji: string) => insertEmoji(emoji, replyTextareaRef.value, replyText)

const relatedPosts = ref<PostCardResp[]>([])
const pageLoading = ref(true)

const fetchDetail = async () => {
  try {
    await postStore.fetchPostDetail(postId.value)
    if (currentPost.value) {
      if (currentPost.value.category?.id) {
        const res = await fetchPostList({ categoryId: currentPost.value.category.id, page: 1, size: 5 })
        const data = res.data as any
        relatedPosts.value = (data?.content || []).filter((p: PostCardResp) => p.id !== postId.value).slice(0, 5)
      }
    }
    loadLikedUsers()
  } catch { /* ignore */ }
  finally { pageLoading.value = false }
}

const fetchComments = (reset = false) => {
  commentStore.fetchComments(postId.value, reset)
}

const loadLikedUsers = async () => {
  try {
    const res = await getPostLikedUsers(postId.value)
    likedUsers.value = (res.data as LikedUsersResp) || null
  } catch { /* ignore */ }
}

const handleLike = async () => {
  if (!authStore.isAccess) { router.push('/login'); return }
  try {
    await likePost(postId.value)
    liked.value = !liked.value
    likeCount.value += liked.value ? 1 : -1
    loadLikedUsers()
  } catch { /* ignore */ }
}

const handleCollect = async () => {
  if (!authStore.isAccess) { router.push('/login'); return }
  try {
    await collectPost(postId.value)
    collected.value = !collected.value
    collectCount.value += collected.value ? 1 : -1
  } catch { /* ignore */ }
}

const submitComment = async () => {
  if (!commentText.value.trim()) return
  if (!authStore.isAccess) { router.push('/login'); return }
  submitting.value = true
  try {
    await commentStore.addComment({ postId: postId.value, content: commentText.value.trim() })
    commentText.value = ''
    commentCount.value++
    ElMessage.success('评论发布成功！')
  } catch { ElMessage.error('评论发布失败') }
  finally { submitting.value = false }
}

const startReply = (commentId: string, displayName: string) => {
  if (replyTarget.value?.commentId === commentId) {
    replyTarget.value = null
    replyText.value = ''
    return
  }
  replyTarget.value = { commentId, displayName }
  replyText.value = ''
}

const cancelReply = () => {
  replyTarget.value = null
  replyText.value = ''
}

const submitReply = async () => {
  if (!replyText.value.trim() || !replyTarget.value) return
  if (!authStore.isAccess) { router.push('/login'); return }
  replySubmitting.value = true
  try {
    await commentStore.addComment({
      postId: postId.value,
      parentId: replyTarget.value.commentId,
      content: replyText.value.trim(),
    })
    replyText.value = ''
    replyTarget.value = null
    commentCount.value++
    ElMessage.success('回复发布成功！')
  } catch { ElMessage.error('回复发布失败') }
  finally { replySubmitting.value = false }
}

const handleLikeComment = async (commentId: string) => {
  if (!authStore.isAccess) { router.push('/login'); return }
  try {
    await likeComment(commentId)
    const updateItem = (item: { isLiked?: boolean; likeCount: number }) => {
      item.isLiked = !item.isLiked
      item.likeCount += item.isLiked ? 1 : -1
    }
    const root = commentStore.comments.find(c => c.id === commentId)
    if (root) { updateItem(root); return }
    for (const replies of Object.values(commentStore.replies)) {
      const reply = replies.find(r => r.id === commentId)
      if (reply) { updateItem(reply); return }
    }
  } catch { /* ignore */ }
}

const toggleReplies = async (rootId: string) => {
  if (expandedReplies.value.has(rootId)) {
    expandedReplies.value.delete(rootId)
    return
  }
  expandedReplies.value.add(rootId)
  await commentStore.fetchReplies(rootId, true)
}

const getReplies = (rootId: string) => commentStore.replies[rootId] || []
const replyHasNext = (rootId: string) => commentStore.replyHasNext[rootId] ?? false

const loadMoreReplies = async (rootId: string) => {
  await commentStore.fetchReplies(rootId, false)
}

const reportDialog = ref(false)
const reportTarget = ref<{ targetType: 'POST' | 'COMMENT' | 'USER'; targetId: string } | null>(null)
const reportReason = ref('')
const reportType = ref<ReportType>('VIOLATION_OF_GUIDELINES')
const reportSubmitting = ref(false)
const reportReasonExpanded = ref(false)

const reportTypeOptions: { value: ReportType; label: string }[] = [
  { value: 'VIOLATION_OF_GUIDELINES', label: '违反社区准则' },
  { value: 'INAPPROPRIATE_CONTENT', label: '内容不当' },
  { value: 'ADVERTISEMENT', label: '垃圾广告' },
  { value: 'VIOLENCE', label: '暴力内容' },
  { value: 'SENSITIVE', label: '敏感内容' },
  { value: 'CASCADE', label: '刷屏引战' },
  { value: 'OTHER', label: '其他' },
]

const openReportDialog = (target: { targetType: 'POST' | 'COMMENT' | 'USER'; targetId: string }) => {
  if (!authStore.isAccess) { router.push('/login'); return }
  reportTarget.value = target
  reportReason.value = ''
  reportType.value = 'VIOLATION_OF_GUIDELINES'
  reportDialog.value = true
  reportReasonExpanded.value = false
}

const isReportReasonRequired = computed(() => reportType.value === 'OTHER')

const submitReport = async () => {
  if (!reportTarget.value) return
  if (isReportReasonRequired.value && !reportReason.value.trim()) {
    ElMessage.warning('请填写举报原因')
    return
  }
  reportSubmitting.value = true
  try {
    const payload: { type: string; reason?: string } = { type: reportType.value }
    if (reportReason.value.trim()) {
      payload.reason = reportReason.value.trim()
    }
    if (reportTarget.value.targetType === 'POST') {
      await reportPost(reportTarget.value.targetId, payload)
    } else {
      await reportComment(reportTarget.value.targetId, payload)
    }
    ElMessage.success('举报已提交，我们会尽快处理')
    reportDialog.value = false
  } catch (err: any) {
    ElMessage.error(err?.message || '举报提交失败')
  } finally {
    reportSubmitting.value = false
  }
}

const postStatusInfo: Record<string, { label: string; cls: string }> = {
  DRAFT: { label: '草稿', cls: 'status-draft' },
  PENDING: { label: '审核中', cls: 'status-pending' },
  PUBLISHED: { label: '已发布', cls: 'status-published' },
  REJECTED: { label: '未通过', cls: 'status-rejected' },
  ARCHIVED: { label: '已归档', cls: 'status-archived' },
}

const formatDateTime = (dateStr: string | null) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const timeAgo = (dateStr: string) => {
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  return formatDateTime(dateStr)
}

const formatCount = (n: number) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

const commentSectionRef = ref<HTMLElement | null>(null)
let commentsLoaded = false

const goToCreate = () => router.push('/post/create')
const goBack = () => router.push('/post')

onMounted(() => {
  fetchDetail()
})

watch(currentPost, (post) => {
  if (!post) return
  // Sync local UI state from server response (ensures correct default active state)
  liked.value = post.isLiked ?? false
  collected.value = post.isCollected ?? false
  likeCount.value = post.likeCount
  collectCount.value = post.collectCount
  commentCount.value = post.commentCount
  if (commentsLoaded) return
  nextTick(() => {
    const el = commentSectionRef.value
    if (!el) return

    const hash = window.location.hash
    if (hash.startsWith('#comment-')) {
      commentsLoaded = true
      fetchComments(true)
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      return
    }

    const observer = new IntersectionObserver((entries) => {
      if (entries[0]?.isIntersecting && !commentsLoaded) {
        commentsLoaded = true
        fetchComments(true)
        observer.disconnect()
      }
    }, { threshold: 0.1 })
    observer.observe(el)
  })
})
</script>

<template>
  <div>
    <HeaderNavMenu />
    <div style="max-width:1280px;margin:0 auto;padding:24px">
      <el-row :gutter="24">
        <el-col :xs="24" :md="17">
          <div class="main" style="max-width:none;margin:0;padding:0">
            <div v-if="currentPost">
        <div class="breadcrumb">
          <a href="/"><i class="fas fa-home"></i> 首页</a>
          <i class="fas fa-chevron-right"></i>
          <a href="/post">社区</a>
          <i class="fas fa-chevron-right"></i>
          <a v-if="currentPost.category" :href="'/post?category=' + currentPost.category.id">{{ currentPost.category.name }}</a>
          <i v-if="currentPost.category" class="fas fa-chevron-right"></i>
          <span>帖子详情</span>
        </div>

        <div v-if="currentPost.editStatus === 'PENDING'" class="edit-pending-banner">
          <i class="fas fa-clock"></i>
          <span>你的编辑正在审核，审核通过后所有人可见</span>
        </div>

        <article class="post-detail">
          <div class="post-detail-header">
            <div class="post-detail-meta">
              <template v-if="!(currentPost.isPinned && !currentPost.userBrief)">
                <template v-if="currentPost.userBrief">
                  <img
                    :src="currentPost.userBrief?.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                    :alt="currentPost.userBrief?.displayName || '用户'"
                    class="post-detail-avatar"
                  >
                  <div class="post-detail-author-info">
                    <a
                      v-if="currentPost.userBrief?.uid"
                      :href="'/User/' + currentPost.userBrief.uid"
                      class="post-detail-author-name"
                    >{{ currentPost.userBrief?.displayName || '用户' }}</a>
                    <a
                      v-if="userInfoStore.userInfo.uid && userInfoStore.userInfo.uid === currentPost.userBrief?.uid"
                      :href="'/post/create?id=' + postId"
                      class="post-detail-edit-link"
                    ><i class="fas fa-pen"></i> 编辑</a>
                    <div class="post-detail-time">
                      发布于 {{ formatDateTime(currentPost.publishedAt) }}
                      <template v-if="currentPost.updatedAt"> · 最后编辑于 {{ formatDateTime(currentPost.updatedAt) }}</template>
                    </div>
                  </div>
                </template>
                <template v-else>
                  <div class="post-detail-avatar" style="display:flex;align-items:center;justify-content:center;background:#e2e8f0;border-radius:50%;width:40px;height:40px;"><i class="fas fa-robot" style="color:#64748b;"></i></div>
                  <div class="post-detail-author-info">
                    <span class="post-detail-author-name">系统</span>
                    <div class="post-detail-time">
                      发布于 {{ formatDateTime(currentPost.publishedAt) }}
                      <template v-if="currentPost.updatedAt"> · 最后编辑于 {{ formatDateTime(currentPost.updatedAt) }}</template>
                    </div>
                  </div>
                </template>
              </template>
              <div class="post-detail-meta-right">
                <span v-if="currentPost.isPinned" class="post-detail-pinned"><i class="fas fa-thumbtack"></i> 置顶</span>
                <span v-if="currentPost.type === 'NOTICE'" class="post-detail-notice"><i class="fas fa-bullhorn"></i> 公告</span>
                <span
                  v-if="currentPost.status && currentPost.status !== 'PUBLISHED'"
                  :class="['post-detail-status', postStatusInfo[currentPost.status]?.cls]"
                >{{ postStatusInfo[currentPost.status]?.label || currentPost.status }}</span>
                <span v-if="currentPost.category" class="post-detail-tag">{{ currentPost.category.name }}</span>
              </div>
            </div>
            <h1 class="post-detail-title">{{ currentPost.title }}</h1>
            <p v-if="currentPost.subtitle" class="post-detail-subtitle">{{ currentPost.subtitle }}</p>
          </div>

          <div v-if="currentPost.coverImage" class="post-detail-cover">
            <img :src="currentPost.coverImage.url" :alt="currentPost.title">
          </div>

          <div class="post-detail-content">
            <ClientOnly>
              <div class="wf-markdown" v-html="renderedContent"></div>
            </ClientOnly>
          </div>

          <div class="post-tags-bar" v-if="currentPost.tags?.length">
            <i class="fas fa-tags"></i>
            <span v-for="tag in currentPost.tags" :key="tag.id" class="post-tag">{{ tag.name }}</span>
          </div>

          <div v-if="currentPost.status === 'PUBLISHED'" class="post-actions-bar">
            <div class="post-actions-left">
              <button :class="['wf-btn-flat', { active: liked }]" @click="handleLike">
                <i :class="liked ? 'fas fa-thumbs-up' : 'far fa-thumbs-up'"></i>
                {{ formatCount(likeCount) }}
              </button>
              <button class="wf-btn-flat">
                <i class="far fa-comment"></i> {{ formatCount(commentCount) }}
              </button>
              <button :class="['wf-btn-flat', { active: collected }]" @click="handleCollect">
                <i :class="collected ? 'fas fa-star' : 'far fa-star'"></i>
                {{ collected ? '已收藏' : '收藏' }}
              </button>
            </div>
            <div class="post-actions-right">
              <button class="wf-btn-text"><i class="fas fa-share-alt"></i> 分享</button>
              <ReportDropdown
                v-if="currentPost.userBrief && currentPost.type !== 'NOTICE'"
                target-type="POST"
                :target-id="postId"
                @report="openReportDialog"
              />
            </div>
          </div>
          <div v-if="likedUsers?.previewUsers?.length" class="liked-users-bar">
            <div class="liked-avatars">
              <img
                v-for="user in likedUsers.previewUsers"
                :key="user.uid"
                :src="user.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                :title="user.displayName"
                class="liked-avatar"
              />
            </div>
            <span class="liked-text">
              被 <strong>{{ likedUsers.totalCount }}</strong> 人赞过
            </span>
          </div>
        </article>

        <div v-if="currentPost.status === 'PUBLISHED'" ref="commentSectionRef" class="reply-section">
          <div class="reply-header">
            <div class="reply-title">
              <i class="far fa-comments"></i>
              全部回复 <span class="reply-count">{{ commentCount }} 条</span>
            </div>
          </div>

          <div class="reply-input-area">
            <div class="reply-input-wrap">
              <img
                src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png"
                alt="avatar"
                class="reply-input-avatar"
              >
              <div class="reply-input-right">
                <textarea
                  ref="commentTextareaRef"
                  v-model="commentText"
                  class="wf-input"
                  placeholder="写下你的评论..."
                ></textarea>
                <div class="reply-input-footer">
                  <div class="reply-input-tools">
                    <EmojiPicker @select="onCommentEmoji" />
                  </div>
                  <button
                    class="reply-submit-btn"
                    :disabled="submitting || !commentText.trim()"
                    @click="submitComment"
                  >
                    <i class="fas fa-paper-plane"></i> 发布评论
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="allComments.length" class="reply-list">
            <div v-for="comment in allComments" :key="comment.id" class="reply-item">
              <img
                :src="comment.author?.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                alt="avatar"
                class="reply-avatar"
              >
              <div class="reply-content">
                <div class="reply-author-row">
                  <span class="reply-author-name">{{ comment.author?.displayName || '匿名用户' }}</span>
                  <UserBadge
                    :is-post-author="comment.isPostAuthor"
                    :author-uid="comment.author?.uid"
                    :current-uid="userInfoStore.userInfo.uid"
                  />
                  <span class="reply-time">{{ timeAgo(comment.createdAt) }}</span>
                </div>
                <div class="reply-body">{{ comment.content }}</div>
                <div class="reply-actions">
                  <button class="reply-action-btn" :class="{ active: comment.isLiked }" @click="handleLikeComment(comment.id)">
                    <i :class="comment.isLiked ? 'fas fa-thumbs-up' : 'far fa-thumbs-up'"></i> {{ formatCount(comment.likeCount) }}
                  </button>
                  <button class="reply-action-btn" @click="startReply(comment.id, comment.author?.displayName || '匿名用户')">
                    <i class="far fa-comment"></i> 回复
                  </button>
                  <button
                    v-if="comment.replyCount > 0"
                    class="reply-action-btn"
                    @click="toggleReplies(comment.id)"
                  >
                    <i class="far fa-comment-dots"></i>
                    {{ expandedReplies.has(comment.id) ? '收起' : '' }} {{ comment.replyCount }} 条回复
                  </button>
                  <ReportDropdown
                    target-type="COMMENT"
                    :target-id="comment.id"
                    placement="bottom-start"
                    @report="openReportDialog"
                  />
                </div>

                <div v-if="expandedReplies.has(comment.id)" class="nested-replies">
                  <div v-for="reply in getReplies(comment.id)" :key="reply.id" class="nested-reply">
                    <img
                      :src="reply.author?.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                      alt="avatar"
                      class="nested-avatar"
                    >
                    <div class="nested-content">
                      <div class="nested-author-row">
                        <span class="nested-author-name">{{ reply.author?.displayName || '匿名用户' }}</span>
                        <UserBadge
                          :is-post-author="reply.isPostAuthor"
                          :author-uid="reply.author?.uid"
                          :current-uid="userInfoStore.userInfo.uid"
                        />
                        <span class="nested-reply-to" v-if="reply.replyToDisplayName">
                          回复 <span>{{ reply.replyToDisplayName }}</span>
                        </span>
                        <span class="nested-time">{{ timeAgo(reply.createdAt) }}</span>
                      </div>
                      <div class="nested-body">{{ reply.content }}</div>
                      <div class="reply-actions" style="margin-top:6px">
                        <button class="reply-action-btn" :class="{ active: reply.isLiked }" @click="handleLikeComment(reply.id)">
                          <i :class="reply.isLiked ? 'fas fa-thumbs-up' : 'far fa-thumbs-up'"></i> {{ formatCount(reply.likeCount) }}
                        </button>
                        <button class="reply-action-btn" @click="startReply(comment.id, reply.author?.displayName || '匿名用户')">
                          <i class="far fa-comment"></i> 回复
                        </button>
                        <ReportDropdown
                          target-type="COMMENT"
                          :target-id="reply.id"
                          placement="bottom-start"
                          @report="openReportDialog"
                        />
                      </div>
                    </div>
                  </div>
                  <div v-if="replyHasNext(comment.id)" class="load-more-wrap">
                    <button class="wf-btn-flat wf-btn-flat-sm" @click="loadMoreReplies(comment.id)">
                      <i class="fas fa-chevron-down"></i> 加载更多回复
                    </button>
                  </div>
                </div>

                <div v-if="replyTarget?.commentId === comment.id" class="reply-form-inline show">
                  <textarea
                    ref="replyTextareaRef"
                    v-model="replyText"
                    class="wf-input"
                    :placeholder="'回复 ' + replyTarget.displayName + '...'"
                  ></textarea>
                  <div class="reply-form-actions">
                    <div class="reply-input-tools">
                      <EmojiPicker @select="onReplyEmoji" />
                    </div>
                    <div style="display:flex;gap:8px">
                      <button class="wf-btn-flat wf-btn-flat-sm" @click="cancelReply">取消</button>
                      <button
                        class="wf-btn-flat wf-btn-flat-sm"
                        style="background:var(--wf-primary);color:#fff;border-color:var(--wf-primary)"
                        :disabled="replySubmitting || !replyText.trim()"
                        @click="submitReply"
                      >回复</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="hasNextComments" class="load-more-wrap">
              <button class="wf-btn-flat wf-btn-flat-sm" @click="fetchComments(false)">
                <i class="fas fa-chevron-down"></i> 加载更多评论
              </button>
            </div>
          </div>

          <div v-else-if="!commentStore.loading" class="empty-comments">
            <div class="empty-icon"><i class="far fa-comment-dots"></i></div>
            <div class="empty-title">暂无评论</div>
            <div class="empty-desc">快来写下第一条评论吧</div>
          </div>
        </div>

        <div v-if="relatedPosts.length" class="related-section">
          <div class="related-header">
            <i class="fas fa-fire"></i> 相关推荐
          </div>
          <div class="related-list">
            <a
              v-for="(post, index) in relatedPosts"
              :key="post.id"
              class="related-item"
              :href="'/post/' + post.id"
            >
              <div :class="['related-rank', index < 3 ? 'top' : 'normal']">{{ index + 1 }}</div>
              <div class="related-info">
                <div class="related-title">{{ post.title }}</div>
                <div class="related-meta">
                  {{ formatCount(post.viewCount) }} 阅读
                  · {{ post.commentCount }} 评论
                </div>
              </div>
            </a>
          </div>
        </div>
      </div>

      <div v-if="pageLoading" class="skeleton-wrap">
        <div class="skeleton-header">
          <div class="skeleton-line skeleton-line--short"></div>
          <div class="skeleton-line skeleton-line--medium"></div>
        </div>
        <div class="skeleton-body">
          <div class="skeleton-line"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-line skeleton-line--short"></div>
        </div>
      </div>
      <div v-else-if="!currentPost" class="wf-state-wrap">
        <div class="wf-state-text wf-state-error">帖子不存在</div>
        <button class="wf-btn wf-btn-primary" @click="goBack">返回社区</button>
      </div>
          </div>
        </el-col>
        <el-col :xs="0" :md="7">
          <div style="display:flex;flex-direction:column;gap:20px">
            <el-card shadow="never">
              <ClientOnly>
                <div style="text-align:center">
                  <template v-if="authStore.isAccess && userInfo">
                    <el-avatar :size="64" :src="userInfo.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" style="margin-bottom:10px" />
                    <div style="font-size:16px;font-weight:600;color:#1e293b;margin-bottom:4px">{{ userInfo.nickname || userInfo.username }}</div>
                    <el-button type="primary" style="width:100%;margin-top:16px" @click="goToCreate">
                      <el-icon size="14" style="margin-right:4px"><Edit /></el-icon> 发布帖子
                    </el-button>
                  </template>
                  <template v-else>
                    <div style="font-size:16px;font-weight:600;color:#1e293b;margin-bottom:8px">欢迎来到社区</div>
                    <p style="font-size:13px;color:#64748b;margin-bottom:16px">登录后即可发布帖子</p>
                    <el-button type="primary" style="width:100%" @click="router.push('/login')">
                      <el-icon size="14" style="margin-right:4px"><User /></el-icon> 立即登录
                    </el-button>
                  </template>
                </div>
              </ClientOnly>
            </el-card>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- Report Dialog - Teleport to body to avoid any stacking context issue -->
    <Teleport to="body">
      <div v-if="reportDialog" class="wf-dialog-overlay" @click.self="reportDialog = false">
        <div class="wf-dialog-panel">
          <div class="wf-dialog-header">
            <h2>举报{{ reportTarget?.targetType === 'POST' ? '帖子' : '评论' }}</h2>
            <button class="wf-dialog-close" @click="reportDialog = false"><i class="fas fa-times"></i></button>
          </div>
          <div class="wf-dialog-body">
            <div class="wf-form-group">
              <label class="wf-form-label">举报类型 <span class="wf-required">*</span></label>
              <select v-model="reportType" class="wf-form-control">
                <option v-for="opt in reportTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
            <div class="wf-form-group">
              <div class="wf-form-label-wrap" style="display:flex;align-items:center;gap:8px;margin-bottom:8px;">
                <label class="wf-form-label" style="margin-bottom:0;">举报原因 <span v-if="isReportReasonRequired" class="wf-required">*</span></label>
                <button type="button" class="wf-btn-collapse" @click="reportReasonExpanded = !reportReasonExpanded" style="background:none;border:1px solid var(--wf-border,#e2e8f0);border-radius:6px;padding:2px 10px;font-size:12px;color:var(--wf-text-muted,#94a3b8);cursor:pointer;display:inline-flex;align-items:center;gap:4px;">
                  <i :class="reportReasonExpanded ? 'fas fa-chevron-up' : 'fas fa-chevron-down'" style="font-size:10px;"></i>
                  {{ reportReasonExpanded ? '收起' : '展开' }}
                </button>
              </div>
              <div v-show="reportReasonExpanded">
                <textarea
                  v-model="reportReason"
                  class="wf-form-control"
                  rows="4"
                  placeholder="请详细描述违规内容..."
                  maxlength="500"
                ></textarea>
                <span class="wf-char-count">{{ reportReason.length }}/500</span>
              </div>
            </div>
          </div>
          <div class="wf-dialog-footer">
            <button class="wf-btn-cancel" @click="reportDialog = false">取消</button>
            <button class="wf-btn-submit" :disabled="reportSubmitting" @click="submitReport">
              {{ reportSubmitting ? '提交中...' : '提交举报' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--wf-text-muted);
  margin-bottom: 20px;
}

.breadcrumb a {
  color: var(--wf-text-secondary);
  text-decoration: none;
  transition: color 0.2s;
}

.breadcrumb a:hover { color: var(--wf-primary); }

.breadcrumb i { font-size: 10px; }

.post-detail {
  background: var(--wf-bg-white);
  border: 1px solid var(--wf-border);
  border-radius: 12px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.post-detail-header {
  padding: 24px 28px 0;
}

.post-detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.post-detail-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.post-detail-author-info { flex: 1; }

.post-detail-time {
  font-size: 13px;
  color: var(--wf-text-muted);
  margin-top: 2px;
}

.post-detail-tag {
  padding: 3px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  background: #dbeafe;
  color: #1d4ed8;
}

.post-detail-meta-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.post-detail-pinned {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  background: #fef3c7;
  color: #d97706;
}

.post-detail-notice {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  background: #fce4ec;
  color: #c62828;
}

.post-detail-status {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}
.post-detail-status.status-draft {
  background: #f1f5f9;
  color: #64748b;
}
.post-detail-status.status-pending {
  background: #fef3c7;
  color: #d97706;
}
.post-detail-status.status-published {
  background: #dcfce7;
  color: #16a34a;
}
.post-detail-status.status-rejected {
  background: #fce4ec;
  color: #c62828;
}
.post-detail-status.status-archived {
  background: #f1f5f9;
  color: #64748b;
}

.edit-pending-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin-bottom: 16px;
  border-radius: 8px;
  font-size: 14px;
  background: #fef3c7;
  border: 1px solid #fde68a;
  color: #92400e;
}
.edit-pending-banner i {
  font-size: 16px;
}

.post-detail-author-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--wf-text-primary);
  text-decoration: none;
  transition: color 0.2s;
}

.post-detail-author-name:hover {
  color: var(--wf-primary);
}

.post-detail-edit-link {
  display: inline-block;
  margin-left: 8px;
  font-size: 12px;
  color: var(--wf-primary);
  text-decoration: none;
  white-space: nowrap;
}
.post-detail-edit-link:hover {
  text-decoration: underline;
}

.post-detail-title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--wf-text-primary);
  margin: 0 0 8px;
}

.post-detail-subtitle {
  font-size: 15px;
  color: var(--wf-text-secondary);
  margin: 0 0 16px;
}

.post-detail-cover {
  padding: 0 28px;
  margin-bottom: 16px;
}

.post-detail-cover img {
  width: 100%;
  max-height: 400px;
  object-fit: cover;
  border-radius: 8px;
}

.post-detail-content {
  padding: 0 28px 20px;
  font-size: 15px;
  line-height: 1.8;
  color: #475569;
}

.post-tags-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 28px 20px;
  flex-wrap: wrap;
}

.post-tags-bar i {
  color: var(--wf-text-muted);
  font-size: 13px;
}

.post-tag {
  display: inline-block;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 500;
  background: var(--wf-border-light);
  color: var(--wf-text-secondary);
  border-radius: 4px;
}

.post-actions-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 28px;
  border-top: 1px solid var(--wf-border-light);
  background: #f8fafc;
}

.post-actions-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.post-actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.liked-users-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 28px;
  border-top: 1px solid var(--wf-border-light);
  background: #f8fafc;
}

.liked-avatars {
  display: flex;
  align-items: center;
}

.liked-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #fff;
  margin-left: -6px;
  object-fit: cover;
  flex-shrink: 0;
}

.liked-avatar:first-child {
  margin-left: 0;
}

.liked-text {
  font-size: 13px;
  color: var(--wf-text-muted);
}

.liked-text strong {
  color: var(--wf-text-secondary);
}

.reply-section {
  background: var(--wf-bg-white);
  border: 1px solid var(--wf-border);
  border-radius: 12px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.reply-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--wf-border-light);
}

.reply-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--wf-text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.reply-title i { color: var(--wf-primary); font-size: 14px; }

.reply-count {
  font-size: 13px;
  color: var(--wf-text-muted);
  font-weight: 400;
}

.reply-input-area {
  padding: 20px 24px;
  border-bottom: 1px solid var(--wf-border-light);
}

.reply-input-wrap {
  display: flex;
  gap: 12px;
}

.reply-input-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.reply-input-right {
  flex: 1;
}

.reply-input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.reply-input-tools {
  display: flex;
  gap: 4px;
}

.reply-tool-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--wf-text-muted);
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: var(--wf-transition);
}

.reply-tool-btn:hover {
  color: var(--wf-primary);
  background: #eff6ff;
}

.reply-submit-btn {
  padding: 8px 20px;
  background: var(--wf-primary);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--wf-transition);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.reply-submit-btn:hover {
  background: var(--wf-primary-hover);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.reply-submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.reply-list {
  padding: 0 24px;
}

.reply-item {
  display: flex;
  gap: 14px;
  padding: 20px 0;
  border-bottom: 1px solid var(--wf-border-light);
}

.reply-item:last-child { border-bottom: none; }

.reply-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.reply-content { flex: 1; min-width: 0; }

.reply-author-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.reply-author-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--wf-text-primary);
}

.reply-time {
  font-size: 12px;
  color: var(--wf-text-muted);
  margin-left: auto;
}

.reply-body {
  font-size: 14px;
  line-height: 1.8;
  color: #475569;
  margin-bottom: 10px;
}

.reply-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.reply-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--wf-text-muted);
  cursor: pointer;
  border: none;
  background: transparent;
  transition: color 0.2s;
  padding: 2px 0;
}

.reply-action-btn:hover { color: var(--wf-primary); }

.reply-action-btn.active { color: var(--wf-primary); }

.reply-action-btn i { font-size: 13px; }

.nested-replies {
  margin-top: 12px;
  padding-left: 12px;
  border-left: 2px solid var(--wf-border);
}

.nested-reply {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid var(--wf-border-light);
}

.nested-reply:last-child { border-bottom: none; padding-bottom: 0; }
.nested-reply:first-child { padding-top: 0; }

.nested-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.nested-content { flex: 1; }

.nested-author-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.nested-author-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--wf-text-primary);
}

.nested-reply-to {
  font-size: 12px;
  color: var(--wf-text-muted);
}

.nested-reply-to span {
  color: var(--wf-primary);
  font-weight: 500;
}

.nested-time {
  font-size: 12px;
  color: var(--wf-text-muted);
  margin-left: auto;
}

.nested-body {
  font-size: 13px;
  line-height: 1.7;
  color: #475569;
}

.reply-form-inline {
  margin-top: 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.reply-form-inline textarea {
  width: 100%;
  min-height: 80px;
  padding: 10px 14px;
  border: 1px solid var(--wf-border);
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--wf-text-primary);
  background: var(--wf-bg-white);
  outline: none;
  resize: vertical;
  font-family: inherit;
  margin-bottom: 10px;
  transition: var(--wf-transition);
}

.reply-form-inline textarea:focus {
  border-color: var(--wf-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.08);
}

.reply-form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.empty-comments {
  padding: 50px 24px;
  text-align: center;
}

.empty-comments .empty-icon {
  width: 64px;
  height: 64px;
  background: var(--wf-border-light);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  font-size: 26px;
  color: var(--wf-text-muted);
}

.empty-comments .empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--wf-text-primary);
  margin-bottom: 4px;
}

.empty-comments .empty-desc {
  font-size: 13px;
  color: var(--wf-text-muted);
}

.related-section {
  background: var(--wf-bg-white);
  border: 1px solid var(--wf-border);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.related-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--wf-border-light);
  font-size: 16px;
  font-weight: 600;
  color: var(--wf-text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.related-header i { color: var(--wf-primary); font-size: 14px; }

.related-list { padding: 8px 0; }

.related-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  cursor: pointer;
  transition: background 0.2s;
  text-decoration: none;
  color: inherit;
}

.related-item:hover { background: #f8fafc; }

.related-rank {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.related-rank.top {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  color: white;
}

.related-rank.normal {
  background: var(--wf-border-light);
  color: var(--wf-text-muted);
}

.related-info { flex: 1; min-width: 0; }

.related-title {
  font-size: 14px;
  color: var(--wf-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.related-meta {
  font-size: 12px;
  color: var(--wf-text-muted);
}

.skeleton-wrap {
  background: var(--wf-bg-white);
  border: 1px solid var(--wf-border);
  border-radius: 12px;
  padding: 28px;
  margin-bottom: 20px;
}

.skeleton-header {
  margin-bottom: 24px;
}

.skeleton-body {
  margin-bottom: 16px;
}

.skeleton-line {
  height: 16px;
  border-radius: 6px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
  margin-bottom: 12px;
}

.skeleton-line--short {
  width: 60%;
}

.skeleton-line--medium {
  width: 80%;
}

@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (max-width: 768px) {
  .post-detail-header { padding: 20px 20px 0; }
  .post-detail-content { padding: 0 20px 20px; }
  .post-detail-cover { padding: 0 20px; }
  .post-tags-bar { padding: 0 20px 16px; }
  .post-actions-bar { padding: 12px 20px; flex-wrap: wrap; gap: 8px; }
  .reply-header { padding: 14px 20px; }
  .reply-input-area { padding: 16px 20px; }
  .reply-list { padding: 0 20px; }
  .post-detail-title { font-size: 20px; }
}</style>