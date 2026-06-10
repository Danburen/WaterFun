<script setup lang="ts">
import { usePostStore } from '~/stores/postStore'
import { useCommentStore } from '~/stores/commentStore'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { likePost, collectPost } from '~/api/postApi'
import { likeComment } from '~/api/commentApi'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '~/stores/authStore'
import { useI18n } from 'vue-i18n'

const postStore = usePostStore()
const commentStore = useCommentStore()
const { currentPost, loading } = storeToRefs(postStore)
const allComments = computed(() => commentStore.comments)
const hasNextComments = computed(() => commentStore.hasNext)
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()

const postId = computed(() => Number(route.params.id))
const liked = ref(false)
const collected = ref(false)
const likeCount = ref(0)
const collectCount = ref(0)

const commentText = ref('')
const submitting = ref(false)

const replyTarget = ref<{ commentId: number; displayName: string } | null>(null)
const replyText = ref('')
const replySubmitting = ref(false)
const expandedReplies = ref<Set<number>>(new Set())

const fetchDetail = async () => {
  try {
    await postStore.fetchPostDetail(postId.value)
    if (currentPost.value) {
      likeCount.value = currentPost.value.likeCount
      collectCount.value = currentPost.value.collectCount
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
    if (currentPost.value) currentPost.value.commentCount++
    ElMessage.success('评论成功')
  } catch { ElMessage.error('评论失败') }
  finally { submitting.value = false }
}

const startReply = (commentId: number, displayName: string) => {
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
    if (currentPost.value) currentPost.value.commentCount++
    ElMessage.success('回复成功')
  } catch { ElMessage.error('回复失败') }
  finally { replySubmitting.value = false }
}

const handleLikeComment = async (commentId: number) => {
  if (!authStore.isAccess) { router.push('/login'); return }
  try {
    await likeComment(commentId)
  } catch { /* ignore */ }
}

const toggleReplies = async (rootId: number) => {
  const key = String(rootId)
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

const goBack = () => router.push('/post')

onMounted(() => {
  fetchDetail()
  fetchComments(true)
})
</script>

<template>
  <div>
    <HeaderNavMenu />
    <div class="post-detail-container">
      <div v-if="currentPost" class="wf-card">
        <div class="wf-card-body">
          <button class="wf-btn wf-btn-text wf-mb-md" @click="goBack">← {{ $t('post.backToList') }}</button>

          <div v-if="currentPost.coverImage" class="detail-cover">
            <img :src="currentPost.coverImage.url" :alt="currentPost.title">
          </div>

          <div class="wf-flex-center wf-gap-sm wf-mb-sm">
            <span v-if="currentPost.type === 'NOTICE'" class="wf-tag wf-tag-warning">{{ $t('post.announcement') }}</span>
            <span v-if="currentPost.isPinned" class="wf-tag wf-tag-danger">{{ $t('post.pinned') }}</span>
          </div>

          <h1 class="detail-title">{{ currentPost.title }}</h1>
          <p v-if="currentPost.subtitle" class="detail-subtitle">{{ currentPost.subtitle }}</p>

          <div class="wf-flex-center wf-gap-sm wf-flex-wrap wf-mb-md">
            <span v-if="currentPost.category" class="wf-tag wf-tag-primary">{{ currentPost.category.name }}</span>
            <span v-for="tag in currentPost.tags" :key="tag.id" class="wf-tag wf-tag-success">{{ tag.name }}</span>
          </div>

          <div class="wf-flex-center wf-gap-md wf-mb-md">
            <button :class="['wf-btn', liked && 'wf-btn-primary']" @click="handleLike">
              {{ liked ? '❤️' : '🤍' }} {{ likeCount }}
            </button>
            <button :class="['wf-btn', collected && 'wf-btn-primary']" @click="handleCollect">
              {{ collected ? '📁' : '📂' }} {{ collectCount }}
            </button>
          </div>

          <div class="wf-stats wf-mb-lg">
            <span class="wf-stat">👁 {{ currentPost.viewCount }}</span>
            <span class="wf-stat">❤️ {{ likeCount }}</span>
            <span class="wf-stat">💬 {{ currentPost.commentCount }}</span>
            <span class="wf-stat">📁 {{ collectCount }}</span>
          </div>

          <div v-if="currentPost.summary" class="detail-summary">{{ currentPost.summary }}</div>
          <div class="detail-content"><pre>{{ currentPost.content }}</pre></div>
        </div>

        <div class="wf-divider" />

        <div class="wf-card-body">
          <h3 class="wf-section-title">{{ $t('message.commentTitle', { count: currentPost.commentCount }) }}</h3>

          <div class="wf-mb-lg">
            <el-input v-model="commentText" type="textarea" :rows="3" :placeholder="$t('comment.writeComment')" maxlength="1000" show-word-limit />
            <div class="wf-flex wf-mt-sm" style="justify-content:flex-end">
              <el-button type="primary" :loading="submitting" :disabled="!commentText.trim()" @click="submitComment">
                {{ $t('comment.submitComment') }}
              </el-button>
            </div>
          </div>

          <div v-for="comment in allComments" :key="comment.id" class="comment-item">
            <div class="wf-flex-center wf-gap-sm wf-mb-sm">
              <div class="wf-avatar-wrap wf-avatar-sm">
                <img :src="comment.author?.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" alt="avatar">
              </div>
              <span class="comment-author">{{ comment.author?.displayName || $t('general.anonymous') }}</span>
              <span class="wf-text-xs wf-text-muted">{{ new Date(comment.createdAt).toLocaleString() }}</span>
            </div>
            <div class="comment-body">{{ comment.content }}</div>
            <div class="wf-flex-center wf-gap-md wf-mt-sm">
              <button class="wf-btn wf-btn-sm wf-btn-text" @click="handleLikeComment(comment.id)">❤️ {{ comment.likeCount }}</button>
              <button class="wf-btn wf-btn-sm wf-btn-text" @click="startReply(comment.id, comment.author?.displayName || $t('general.anonymous'))">{{ $t('comment.reply') }}</button>
              <button v-if="comment.replyCount > 0" class="wf-btn wf-btn-sm wf-btn-text" @click="toggleReplies(comment.id)">
                {{ expandedReplies.has(comment.id) ? $t('comment.collapse') : $t('comment.expand') }} ({{ comment.replyCount }})
              </button>
            </div>

            <div v-if="expandedReplies.has(comment.id)" class="replies-wrap">
              <div v-for="reply in getReplies(comment.id)" :key="reply.id" class="reply-item">
                <div class="wf-flex-center wf-gap-sm wf-mb-sm">
                  <div class="wf-avatar-wrap wf-avatar-sm">
                    <img :src="reply.author?.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" alt="avatar">
                  </div>
                  <span class="comment-author">{{ reply.author?.displayName || $t('general.anonymous') }}</span>
                  <span v-if="reply.replyToDisplayName" class="wf-text-xs wf-text-muted">{{ $t('comment.replyTo', { name: '@' + reply.replyToDisplayName }) }}</span>
                  <span class="wf-text-xs wf-text-muted">{{ new Date(reply.createdAt).toLocaleString() }}</span>
                </div>
                <div class="reply-body">{{ reply.content }}</div>
                <div class="wf-flex-center wf-gap-sm wf-mt-sm">
                  <button class="wf-btn wf-btn-sm wf-btn-text" @click="handleLikeComment(reply.id)">❤️ {{ reply.likeCount }}</button>
                  <button class="wf-btn wf-btn-sm wf-btn-text" @click="startReply(comment.id, reply.author?.displayName || $t('general.anonymous'))">{{ $t('comment.reply') }}</button>
                </div>
              </div>
              <div v-if="replyHasNext(comment.id)" class="wf-flex-center" style="justify-content:center;padding:8px">
                <el-button text type="primary" size="small" @click="loadMoreReplies(comment.id)">{{ $t('message.loadMoreReplies') }}</el-button>
              </div>
              <el-empty v-if="getReplies(comment.id).length === 0" :description="$t('message.empty.noReplies')" :image-size="60" />
            </div>

            <div v-if="replyTarget?.commentId === comment.id" class="reply-input-wrap">
              <div class="wf-flex-center wf-gap-sm wf-mb-sm">
                <span class="wf-text-sm wf-text-secondary">{{ $t('comment.replyTo', { name: '@' + replyTarget.displayName }) }}</span>
                <button class="wf-btn wf-btn-sm wf-btn-text" @click="cancelReply">{{ $t('comment.cancel') }}</button>
              </div>
              <el-input v-model="replyText" type="textarea" :rows="2" :placeholder="$t('comment.writeReply')" maxlength="1000" />
              <div class="wf-flex wf-mt-sm" style="justify-content:flex-end">
                <el-button size="small" type="primary" :loading="replySubmitting" :disabled="!replyText.trim()" @click="submitReply">
                  {{ $t('comment.submitReply') }}
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="hasNextComments" class="wf-mt-md wf-flex-center" style="justify-content:center">
            <el-button text type="primary" @click="fetchComments(false)">{{ $t('message.loadMoreComments') }}</el-button>
          </div>
          <el-empty v-if="!hasNextComments && allComments.length === 0" :description="$t('message.empty.noComments')" :image-size="80" />
        </div>
      </div>

      <div v-else-if="loading" class="wf-state-wrap">
        <div class="wf-state-text">{{ $t('general.loading') }}</div>
      </div>
      <div v-else class="wf-state-wrap">
        <div class="wf-state-text wf-state-error">{{ $t('post.notFound') }}</div>
        <button class="wf-btn wf-btn-primary" @click="goBack">{{ $t('post.backToList') }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.post-detail-container { max-width: 800px; margin: 0 auto; position: relative; z-index: 1; padding: 24px; }
.detail-cover { width: 100%; max-height: 400px; overflow: hidden; border-radius: 8px; margin-bottom: 24px; }
.detail-cover img { width: 100%; height: auto; object-fit: cover; }
.detail-title { font-size: 28px; color: #303133; margin: 0 0 12px; line-height: 1.4; }
.detail-subtitle { font-size: 16px; color: #909399; margin: 0 0 16px; }
.detail-summary { padding: 16px; background: #f5f7fa; border-radius: 4px; margin-bottom: 24px; font-size: 14px; color: #606266; }
.detail-content { font-size: 15px; color: #303133; line-height: 1.8; }
.detail-content pre { white-space: pre-wrap; word-wrap: break-word; font-family: inherit; }

.comment-item { padding: 16px 0; border-bottom: 1px solid #f1f5f9; }
.comment-author { font-weight: 600; color: #1e293b; font-size: 14px; }
.comment-body { font-size: 14px; color: #374151; line-height: 1.6; }

.replies-wrap { margin: 12px 0 4px 36px; padding: 12px; background: #f8fafc; border-radius: 8px; }
.reply-item { padding: 10px 0; }
.reply-item + .reply-item { border-top: 1px solid #f1f5f9; }
.reply-body { font-size: 13px; color: #374151; line-height: 1.6; }

.reply-input-wrap { margin: 12px 0 4px; padding: 12px; background: #f8fafc; border-radius: 8px; }

@media (max-width: 768px) { .post-detail-container { padding: 16px; } }
</style>
