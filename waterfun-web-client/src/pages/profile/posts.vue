<script setup lang="ts">
import { usePostStore } from '~/stores/postStore'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'


const postStore = usePostStore()
const { myPosts, myPagination, myLoading, myPostStats } = storeToRefs(postStore)
const router = useRouter()
const { t } = useI18n()

const activeTab = ref<'all' | 'PUBLISHED' | 'DRAFT' | 'PENDING' | 'REJECTED'>('all')
const keyword = ref('')
const sortOrder = ref('newest')
const selectedIds = ref<Set<string>>(new Set())

const tabConfig = [
  { key: 'all' as const, label: '全部' },
  { key: 'PUBLISHED' as const, label: '已发布' },
  { key: 'DRAFT' as const, label: '草稿箱' },
  { key: 'PENDING' as const, label: '审核中' },
  { key: 'REJECTED' as const, label: '未通过' },
]

const loadMyPosts = async (page: number = 1) => {
  const params: Record<string, any> = { page, size: 10 }
  if (activeTab.value !== 'all') {
    params.status = activeTab.value
  }
  if (keyword.value.trim()) {
    params.keyword = keyword.value.trim()
  }
  try {
    await postStore.fetchMyPostList(params as any)
  } catch { /* ignore */ }
}

const switchTab = (tab: typeof activeTab.value) => {
  activeTab.value = tab
  selectedIds.value = new Set()
  loadMyPosts(1)
}

const searchPosts = () => {
  loadMyPosts(1)
}

const handlePageChange = (page: number) => {
  loadMyPosts(page)
}

const toggleSelect = (id: string) => {
  const next = new Set(selectedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selectedIds.value = next
}

const toggleSelectAll = () => {
  const visible = myPosts.value
  if (selectedIds.value.size === visible.length) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(visible.map(p => p.id))
  }
}

const batchDelete = async () => {
  if (!selectedIds.value.size) return
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.size} 个帖子吗？此操作不可恢复。`, '确认删除', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await postStore.batchDeletePosts(Array.from(selectedIds.value))
    ElMessage.success(`已删除 ${selectedIds.value.size} 个帖子`)
    selectedIds.value = new Set()
    loadMyPosts(myPagination.value.number + 1)
    postStore.fetchMyPostStats()
  } catch { /* cancel or error */ }
}

const publishPostById = async (id: string) => {
  await postStore.fetchMyPostDetail(id)
  const data = postStore.currentMyPost
  if (!data) throw new Error('获取帖子详情失败')
  await postStore.fetchEditDraft(id)
  const draft = postStore.editDraft
  await postStore.publishPost(id, {
    title: data.title || '',
    content: data.content || '',
    categoryId: data.category?.id || undefined,
    subtitle: data.subtitle,
    summary: data.summary,
    tagIds: draft?.editedTagIds?.length ? draft.editedTagIds.map(t => t.id) : undefined,
    newTags: draft?.editedNewTagIds?.length ? [...draft.editedNewTagIds] : undefined,
  })
}

const publishSingleDraft = async (id: string) => {
  try {
    await publishPostById(id)
    ElMessage.success('帖子已提交审核')
    loadMyPosts(myPagination.value.number + 1)
    postStore.fetchMyPostStats()
  } catch { /* ignore */ }
}

const batchPublish = async () => {
  const draftIds = myPosts.value.filter(p => p.status === 'DRAFT' && selectedIds.value.has(p.id)).map(p => p.id)
  if (!draftIds.length) {
    ElMessage.warning('请选择草稿状态的帖子')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要提交 ${draftIds.length} 个草稿进行审核吗？`, '确认提交', {
      confirmButtonText: '提交', cancelButtonText: '取消', type: 'info'
    })
    await postStore.batchPublishPosts(draftIds)
    ElMessage.success(`已提交 ${draftIds.length} 个帖子进行审核`)
    selectedIds.value = new Set()
    loadMyPosts(myPagination.value.number + 1)
    postStore.fetchMyPostStats()
  } catch { /* cancel or error */ }
}

const deleteSingle = async (id: string) => {
  try {
    await ElMessageBox.confirm('确定要删除这个帖子吗？此操作不可恢复。', '确认删除', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await postStore.deletePost(id)
    ElMessage.success('帖子已删除')
    loadMyPosts(myPagination.value.number + 1)
    postStore.fetchMyPostStats()
  } catch { /* cancel */ }
}

const editPost = (id: string) => router.push(`/post/create?id=${id}`)

const statusInfo: Record<string, { label: string; icon: string; cls: string }> = {
  PUBLISHED: { label: '已发布', icon: 'fas fa-check-circle', cls: 'published' },
  DRAFT: { label: '草稿', icon: 'fas fa-pen', cls: 'draft' },
  PENDING: { label: '审核中', icon: 'fas fa-hourglass-half', cls: 'pending' },
  REJECTED: { label: '未通过', icon: 'fas fa-times-circle', cls: 'rejected' },
  ARCHIVED: { label: '已归档', icon: 'fas fa-archive', cls: 'archived' },
}

const formatDateTime = (dateStr: string | null) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const formatCount = (n: number) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

onMounted(async () => {
  await Promise.all([loadMyPosts(), postStore.fetchMyPostStats()])
})
</script>

<template>
  <div class="my-posts">
    <div class="page-header">
      <h1 class="page-title">我的帖子</h1>
    </div>

    <div class="stats-bar">
      <div class="stat-card">
        <div class="stat-icon blue"><i class="fas fa-file-alt"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ myPostStats?.totalCount ?? '-' }}</div>
          <div class="stat-label">全部帖子</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="fas fa-check-circle"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ myPostStats?.publishedCount ?? '-' }}</div>
          <div class="stat-label">已发布</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><i class="fas fa-edit"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ myPostStats?.draftCount ?? '-' }}</div>
          <div class="stat-label">草稿箱</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red"><i class="fas fa-heart"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ myPostStats ? formatCount(myPostStats.totalLikeCount) : '-' }}</div>
          <div class="stat-label">获赞总数</div>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="tabs-bar">
        <button
          v-for="tab in tabConfig"
          :key="tab.key"
          :class="['tab-item', { active: activeTab === tab.key }]"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
          <span class="tab-count">{{
            tab.key === 'all' ? (myPostStats?.totalCount ?? 0)
            : tab.key === 'PUBLISHED' ? (myPostStats?.publishedCount ?? 0)
            : tab.key === 'DRAFT' ? (myPostStats?.draftCount ?? 0)
            : tab.key === 'PENDING' ? (myPostStats?.pendingCount ?? 0)
            : (myPostStats?.rejectedCount ?? 0)
          }}</span>
        </button>
      </div>

      <div class="toolbar">
        <div class="toolbar-left">
          <div class="search-posts">
            <input v-model="keyword" type="text" placeholder="搜索帖子标题..." @keyup.enter="searchPosts">
            <i class="fas fa-search" @click="searchPosts"></i>
          </div>
          <select v-model="sortOrder" class="filter-select">
            <option value="newest">最新发布</option>
            <option value="oldest">最早发布</option>
            <option value="most-viewed">最多浏览</option>
            <option value="most-liked">最多点赞</option>
          </select>
        </div>
        <div class="toolbar-right batch-actions">
          <button class="btn-sm" @click="toggleSelectAll">
            <i :class="selectedIds.size === myPosts.length && myPosts.length ? 'far fa-check-square' : 'far fa-square'"></i>
            {{ selectedIds.size === myPosts.length && myPosts.length ? '取消全选' : '全选' }}
          </button>
          <button v-if="selectedIds.size" class="btn-sm" @click="batchPublish">
            <i class="fas fa-upload"></i> 批量发布
          </button>
          <button v-if="selectedIds.size" class="btn-sm danger" @click="batchDelete">
            <i class="far fa-trash-alt"></i> 批量删除
          </button>
        </div>
      </div>

      <div v-if="myLoading && !myPosts.length" class="empty-state">
        <div class="empty-icon"><i class="fas fa-spinner fa-pulse"></i></div>
        <div class="empty-title">加载中...</div>
      </div>

      <div v-else-if="!myPosts.length" class="empty-state">
        <div class="empty-icon"><i class="fas fa-file-alt"></i></div>
        <div class="empty-title">暂无帖子</div>
        <div class="empty-desc">开始发布你的第一个帖子吧</div>
      </div>

      <div v-else class="post-list">
        <div
          v-for="post in myPosts"
          :key="post.id"
          class="post-item"
          :data-status="post.status?.toLowerCase()"
        >
          <input
            type="checkbox"
            class="post-checkbox"
            :checked="selectedIds.has(post.id)"
            @change="toggleSelect(post.id)"
          >
          <div class="post-content">
            <div class="post-title-row">
              <a class="post-title" @click.prevent="router.push(`/post/${post.id}`)">{{ post.title || post.editedTitle || '无标题' }}</a>
              <span v-if="post.editedTitle && post.editedTitle !== post.title" class="post-status edited" title="有未发布的编辑内容">
                <i class="fas fa-pen"></i> 已编辑
              </span>
              <span v-if="post.status" :class="['post-status', statusInfo[post.status]?.cls || '']">
                <i :class="statusInfo[post.status]?.icon || ''"></i>
                {{ statusInfo[post.status]?.label || post.status }}
              </span>
            </div>
            <div v-if="post.summary || post.editedSummary" class="post-excerpt">{{ post.summary || post.editedSummary }}</div>
            <div class="post-meta-row">
              <span class="post-meta-item">
                <i class="far fa-folder"></i>
                <span class="category-tag">{{ post.category?.name || '全局公告' }}</span>
              </span>
              <span class="post-meta-item">
                <i class="far fa-clock"></i>
                {{ formatDateTime(post.publishedAt || post.updatedAt) }}
              </span>
              <span v-if="post.updatedAt" class="post-meta-item">
                <i class="far fa-edit"></i>
                最后编辑于 {{ formatDateTime(post.updatedAt) }}
              </span>
            </div>
          </div>
          <div v-if="post.status === 'PUBLISHED'" class="post-stats">
            <span class="post-stat"><i class="far fa-eye"></i> {{ formatCount(post.viewCount) }}</span>
            <span class="post-stat"><i class="far fa-thumbs-up"></i> {{ formatCount(post.likeCount) }}</span>
            <span class="post-stat"><i class="far fa-comment"></i> {{ formatCount(post.commentCount) }}</span>
          </div>
          <div v-else class="post-stats">
            <span class="post-stat">—</span>
            <span class="post-stat">—</span>
            <span class="post-stat">—</span>
          </div>
          <div class="post-actions">
            <button class="post-action-btn" title="编辑" @click="editPost(post.id)">
              <i class="fas fa-pen"></i>
            </button>
            <button
              v-if="post.status === 'DRAFT'"
              class="post-action-btn" title="发布"
              @click="publishSingleDraft(post.id)"
            >
              <i class="fas fa-upload"></i>
            </button>
            <button class="post-action-btn danger" title="删除" @click="deleteSingle(post.id)">
              <i class="far fa-trash-alt"></i>
            </button>
          </div>
        </div>
      </div>

      <div v-if="myPagination.totalPages > 1" class="pagination">
        <button
          :class="['page-btn', { disabled: myPagination.number === 0 }]"
          :disabled="myPagination.number === 0"
          @click="handlePageChange(myPagination.number)"
        >
          <i class="fas fa-chevron-left"></i>
        </button>
        <button
          v-for="p in myPagination.totalPages"
          :key="p"
          :class="['page-btn', { active: p === myPagination.number + 1 }]"
          @click="handlePageChange(p)"
          v-show="Math.abs(p - (myPagination.number + 1)) <= 2 || p === 1 || p === myPagination.totalPages"
        >{{ p }}</button>
        <button
          :class="['page-btn', { disabled: myPagination.number + 1 >= myPagination.totalPages }]"
          :disabled="myPagination.number + 1 >= myPagination.totalPages"
          @click="handlePageChange(myPagination.number + 2)"
        >
          <i class="fas fa-chevron-right"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.my-posts {
  max-width: 900px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  letter-spacing: -0.3px;
}

.stats-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.stat-card:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;
}

.stat-icon.blue { background: #eff6ff; color: #3b82f6; }
.stat-icon.green { background: #ecfdf5; color: #10b981; }
.stat-icon.orange { background: #fffbeb; color: #f59e0b; }
.stat-icon.red { background: #fef2f2; color: #ef4444; }

.stat-info { flex: 1; }

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 2px;
}

.content-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
}

.tabs-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 20px 0;
  border-bottom: 1px solid #f1f5f9;
  overflow-x: auto;
}

.tab-item {
  padding: 10px 14px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  border: none;
  background: transparent;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  white-space: nowrap;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-item:hover { color: #3b82f6; }

.tab-item.active {
  color: #3b82f6;
  border-bottom-color: #3b82f6;
}

.tab-count {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 10px;
  background: #f1f5f9;
  color: #94a3b8;
  transition: all 0.2s;
}

.tab-item.active .tab-count {
  background: #eff6ff;
  color: #3b82f6;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid #f1f5f9;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-posts {
  position: relative;
}

.search-posts input {
  width: 220px;
  padding: 7px 34px 7px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  background: #f8fafc;
  color: #1e293b;
  outline: none;
  transition: all 0.2s ease;
}

.search-posts input:focus {
  border-color: #3b82f6;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-posts input::placeholder { color: #94a3b8; }

.search-posts i {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 13px;
  cursor: pointer;
}

.filter-select {
  padding: 7px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  color: #64748b;
  background: #ffffff;
  outline: none;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-select:focus { border-color: #3b82f6; }

.batch-actions .btn-sm {
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.batch-actions .btn-sm:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background: #eff6ff;
}

.batch-actions .btn-sm.danger:hover {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
}

.post-list {
  padding: 0;
}

.post-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.2s ease;
  position: relative;
}

.post-item:last-child { border-bottom: none; }

.post-item:hover { background: #f8fafc; }

.post-checkbox {
  width: 16px;
  height: 16px;
  border: 2px solid #e2e8f0;
  border-radius: 3px;
  cursor: pointer;
  flex-shrink: 0;
  margin-top: 3px;
  accent-color: #3b82f6;
}

.post-content { flex: 1; min-width: 0; }

.post-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.post-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.2s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.post-title:hover { color: #3b82f6; }

.post-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.post-status.published { background: #ecfdf5; color: #10b981; }
.post-status.draft { background: #fffbeb; color: #f59e0b; }
.post-status.pending { background: #eff6ff; color: #3b82f6; }
.post-status.rejected { background: #fef2f2; color: #ef4444; }
.post-status.archived { background: #f1f5f9; color: #64748b; }
.post-status.edited { background: #fefce8; color: #ca8a04; }

.post-status i { font-size: 10px; }

.post-excerpt {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-meta-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.post-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.post-meta-item i { font-size: 11px; }

.post-meta-item .category-tag {
  padding: 1px 7px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 500;
  background: #f1f5f9;
  color: #64748b;
}

.post-stats {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.post-stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #94a3b8;
}

.post-stat i { font-size: 13px; }

.post-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.post-item:hover .post-actions { opacity: 1; }

.post-action-btn {
  width: 30px;
  height: 30px;
  border: none;
  background: transparent;
  color: #94a3b8;
  border-radius: 5px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  transition: all 0.2s;
}

.post-action-btn:hover {
  color: #3b82f6;
  background: #eff6ff;
}

.post-action-btn.danger:hover {
  color: #ef4444;
  background: #fef2f2;
}

.empty-state {
  padding: 50px 20px;
  text-align: center;
}

.empty-icon {
  width: 70px;
  height: 70px;
  background: #f1f5f9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
  font-size: 28px;
  color: #94a3b8;
}

.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}

.empty-desc {
  font-size: 14px;
  color: #94a3b8;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  padding: 16px 20px;
  border-top: 1px solid #f1f5f9;
}

.page-btn {
  min-width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 6px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.page-btn.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.page-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>