<script setup lang="ts">
import { usePostStore } from '~/stores/postStore'
import { useCommentStore } from '~/stores/commentStore'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { likePost, collectPost, fetchPostList } from '~/api/postApi'
import { likeComment } from '~/api/commentApi'
import MarkdownIt from 'markdown-it'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '~/stores/authStore'
import type { PostCardResp } from '~/api/postApi'

const postStore = usePostStore()
const commentStore = useCommentStore()
const { currentPost, loading } = storeToRefs(postStore)
const allComments = computed(() => commentStore.comments)
const hasNextComments = computed(() => commentStore.hasNext)
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const md = new MarkdownIt()
const renderedContent = computed(() => currentPost.value ? md.render(currentPost.value.content) : '')

const postId = computed(() => Number(route.params.id))
const liked = ref(false)
const collected = ref(false)
const likeCount = ref(0)
const collectCount = ref(0)
const commentCount = ref(0)

const commentText = ref('')
const submitting = ref(false)

const replyTarget = ref<{ commentId: number; displayName: string } | null>(null)
const replyText = ref('')
const replySubmitting = ref(false)
const expandedReplies = ref<Set<number>>(new Set())

const relatedPosts = ref<PostCardResp[]>([])

const fetchDetail = async () => {
  try {
    await postStore.fetchPostDetail(postId.value)
    if (currentPost.value) {
      likeCount.value = currentPost.value.likeCount
      collectCount.value = currentPost.value.collectCount
      commentCount.value = currentPost.value.commentCount
      if (currentPost.value.category?.id) {
        const res = await fetchPostList({ categoryId: currentPost.value.category.id, page: 1, size: 5 })
        const data = res.data as any
        relatedPosts.value = (data?.content || []).filter((p: PostCardResp) => p.id !== postId.value).slice(0, 5)
      }
    }
  } catch { /* ignore */ }
}

const fetchComments = (reset = false) => {
  commentStore.fetchComments(postId.value, reset)
}

const handleLike = async () => {
  if (!authStore.isAccess) { router.push('/login'); return }
  try {
    await likePost(postId.value)
    liked.value = !liked.value
    likeCount.value += liked.value ? 1 : -1
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

const startReply = (commentId: number, displayName: string) => {
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

const handleLikeComment = async (commentId: number) => {
  if (!authStore.isAccess) { router.push('/login'); return }
  try {
    await likeComment(commentId)
  } catch { /* ignore */ }
}

const toggleReplies = async (rootId: number) => {
  if (expandedReplies.value.has(rootId)) {
    expandedReplies.value.delete(rootId)
    return
  }
  expandedReplies.value.add(rootId)
  await commentStore.fetchReplies(rootId, true)
}

const getReplies = (rootId: number) => commentStore.replies[String(rootId)] || []
const replyHasNext = (rootId: number) => commentStore.replyHasNext[String(rootId)] ?? false

const loadMoreReplies = async (rootId: number) => {
  await commentStore.fetchReplies(rootId, false)
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

const goBack = () => router.push('/post')

onMounted(() => {
  fetchDetail()
  fetchComments(true)
})
</script>

<template>
  <div>
    <HeaderNavMenu />
    <div class="main">
      <div v-if="currentPost">
        <div class="breadcrumb">
          <a href="/"><i class="fas fa-home"></i> 首页</a>
          <i class="fas fa-chevron-right"></i>
          <a href="/post">社区</a>
          <i class="fas fa-chevron-right"></i>
          <a v-if="currentPost.category" href="javascript:void(0)">{{ currentPost.category.name }}</a>
          <i v-if="currentPost.category" class="fas fa-chevron-right"></i>
          <span>帖子详情</span>
        </div>

        <article class="post-detail">
          <div class="post-detail-header">
            <div class="post-detail-meta">
              <img
                src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png"
                alt="avatar"
                class="post-detail-avatar"
              >
              <div class="post-detail-author-info">
                <div class="post-detail-author-name">
                  <span v-if="currentPost.type === 'NOTICE'" class="wf-tag wf-tag-warning" style="margin-right:6px;font-size:12px">公告</span>
                  <span v-if="currentPost.isPinned" class="wf-tag wf-tag-danger" style="margin-right:6px;font-size:12px">置顶</span>
                </div>
                <div class="post-detail-time">
                  发布于 {{ formatDateTime(currentPost.publishedAt) }}
                  <template v-if="currentPost.updatedAt"> · 最后编辑于 {{ formatDateTime(currentPost.updatedAt) }}</template>
                </div>
              </div>
              <span v-if="currentPost.category" class="post-detail-tag">{{ currentPost.category.name }}</span>
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

          <div class="post-actions-bar">
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
            </div>
          </div>
        </article>

        <div class="reply-section">
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
                  v-model="commentText"
                  class="wf-input"
                  placeholder="写下你的评论...&#10;支持 Markdown 语法"
                ></textarea>
                <div class="reply-input-footer">
                  <div class="reply-input-tools">
                    <button class="reply-tool-btn" title="表情"><i class="far fa-smile"></i></button>
                    <button class="reply-tool-btn" title="图片"><i class="far fa-image"></i></button>
                    <button class="reply-tool-btn" title="代码"><i class="fas fa-code"></i></button>
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
                  <span class="reply-time">{{ timeAgo(comment.createdAt) }}</span>
                </div>
                <div class="reply-body">{{ comment.content }}</div>
                <div class="reply-actions">
                  <button class="reply-action-btn" @click="handleLikeComment(comment.id)">
                    <i class="far fa-thumbs-up"></i> {{ formatCount(comment.likeCount) }}
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
                        <span class="nested-reply-to" v-if="reply.replyToDisplayName">
                          回复 <span>{{ reply.replyToDisplayName }}</span>
                        </span>
                        <span class="nested-time">{{ timeAgo(reply.createdAt) }}</span>
                      </div>
                      <div class="nested-body">{{ reply.content }}</div>
                      <div class="reply-actions" style="margin-top:6px">
                        <button class="reply-action-btn" @click="handleLikeComment(reply.id)">
                          <i class="far fa-thumbs-up"></i> {{ formatCount(reply.likeCount) }}
                        </button>
                        <button class="reply-action-btn" @click="startReply(comment.id, reply.author?.displayName || '匿名用户')">
                          <i class="far fa-comment"></i> 回复
                        </button>
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
                    v-model="replyText"
                    class="wf-input"
                    :placeholder="'回复 ' + replyTarget.displayName + '...'"
                  ></textarea>
                  <div class="reply-form-actions">
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
                  {{ post.userBrief?.displayName || '未知' }}
                  · {{ formatCount(post.viewCount) }} 阅读
                  · {{ post.commentCount }} 评论
                </div>
              </div>
            </a>
          </div>
        </div>
      </div>

      <div v-else-if="loading" class="wf-state-wrap">
        <div class="wf-state-text"><i class="fas fa-spinner fa-pulse"></i> 加载中...</div>
      </div>
      <div v-else class="wf-state-wrap">
        <div class="wf-state-text wf-state-error">帖子不存在</div>
        <button class="wf-btn wf-btn-primary" @click="goBack">返回社区</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.main {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}

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
  overflow: hidden;
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

.post-detail-author-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--wf-text-primary);
}

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

.reply-section {
  background: var(--wf-bg-white);
  border: 1px solid var(--wf-border);
  border-radius: 12px;
  overflow: hidden;
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
  justify-content: flex-end;
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

@media (max-width: 768px) {
  .main { padding: 16px; }
  .post-detail-header { padding: 20px 20px 0; }
  .post-detail-content { padding: 0 20px 20px; }
  .post-detail-cover { padding: 0 20px; }
  .post-tags-bar { padding: 0 20px 16px; }
  .post-actions-bar { padding: 12px 20px; flex-wrap: wrap; gap: 8px; }
  .reply-header { padding: 14px 20px; }
  .reply-input-area { padding: 16px 20px; }
  .reply-list { padding: 0 20px; }
  .post-detail-title { font-size: 20px; }
}
</style>