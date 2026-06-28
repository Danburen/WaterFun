<script setup lang="ts">
import { useRoute } from 'vue-router'
import { usePostStore } from '~/stores/postStore'
import { useAuthStore } from '~/stores/authStore'
import { storeToRefs } from 'pinia'
import { fetchUserPublicProfile, fetchFollowers, fetchFollowings, type UserPublicProfileResp } from '~/api/publicUserApi'
import { toggleFollowUser } from '~/api/publicUserApi'
import { useUserInfoStore } from '~/stores/userInfoStore'
import { createTicket } from '~/api/ticketApi'
import ReportDropdown from '~/components/ReportDropdown.vue'
import { getTagColor } from '@waterfun/web-core/src/tagColor'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

definePageMeta({
  ssr: false
})

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const postStore = usePostStore()
const authStore = useAuthStore()
const { posts, loading: postsLoading } = storeToRefs(postStore)

const uid = computed(() => String(route.params.uid))

const profile = ref<UserPublicProfileResp | null>(null)
const profileLoading = ref(true)
const profileError = ref(false)
const followed = ref(false)
const followLoading = ref(false)

const activeTab = ref<'posts' | 'followers' | 'followings' | 'likes'>('posts')
const followers = ref<any[]>([])
const followings = ref<any[]>([])
const relationLoading = ref(false)

const userInfoStore = (() => {
  try { return useUserInfoStore() } catch { return null as any }
})()
const currentUserId = computed(() => userInfoStore?.userInfo?.uid || '')
const isSelf = computed(() => currentUserId.value === uid.value)

const loadProfile = async () => {
  profileLoading.value = true
  profileError.value = false
  try {
    const profileRes = await fetchUserPublicProfile(uid.value)
    profile.value = profileRes.data as unknown as UserPublicProfileResp

        // Check follow status
    if (authStore.isAccess && !isSelf.value) {
      try {
        const cardRes = await import('~/api/publicUserApi').then(m => m.fetchUserPublicCard(uid.value))
        followed.value = (cardRes.data as any)?.isFollowed === true
      } catch { /* ignore */ }
    }
  } catch {
    profileError.value = true
  } finally {
    profileLoading.value = false
  }
}

const loadPosts = async () => {
  try {
    await postStore.fetchPostList({ page: 1, size: 20 })
  } catch { /* ignore */ }
}

const handleFollow = async () => {
  if (!authStore.isAccess) { router.push('/login'); return }
  followLoading.value = true
  try {
    await toggleFollowUser(uid.value)
    followed.value = !followed.value
    if (profile.value) {
      profile.value.followers! += followed.value ? 1 : -1
    }
    ElMessage.success(followed.value ? t('message.success.followSuccess') : t('message.success.unfollowSuccess'))
  } catch { ElMessage.error(t('message.failed.operationFailed')) }
  finally { followLoading.value = false }
}

const loadFollowers = async () => {
  if (activeTab.value !== 'followers') return
  relationLoading.value = true
  try {
    const res = await fetchFollowers(uid.value)
    followers.value = ((res.data as any)?.content || []) as any[]
  } catch { /* ignore */ }
  finally { relationLoading.value = false }
}

const loadFollowings = async () => {
  if (activeTab.value !== 'followings') return
  relationLoading.value = true
  try {
    const res = await fetchFollowings(uid.value)
    followings.value = ((res.data as any)?.content || []) as any[]
  } catch { /* ignore */ }
  finally { relationLoading.value = false }
}

const switchTab = (tab: 'posts' | 'followers' | 'followings' | 'likes') => {
  activeTab.value = tab
  if (tab === 'followers') loadFollowers()
  if (tab === 'followings') loadFollowings()
}

const goToUserPage = (targetUid: string) => {
  router.push(`/User/${targetUid}`)
}

const goToEditProfile = () => {
  router.push('/profile')
}

const formatNumber = (n: number | string | undefined | null): string => {
  if (n == null) return '0'
  const num = typeof n === 'string' ? parseInt(n) : n
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return String(num)
}

const genderLabel = (g: string | undefined): string => {
  if (g === 'MALE') return '♂ 男'
  if (g === 'FEMALE') return '♀ 女'
  if (g === 'SECRET') return '保密'
  return g || ''
}

const handleReportUser = () => {
  if (!authStore.isAccess) { router.push('/login'); return }
  ElMessageBox.prompt('请描述举报原因', '举报用户', {
    confirmButtonText: '提交举报',
    cancelButtonText: '取消',
    inputPlaceholder: '请详细描述违规行为...',
    inputType: 'textarea',
    inputValidator: (val: string) => !!val.trim() || '请填写举报原因',
  }).then(({ value: reason }) => {
    return createTicket({
      ticketType: 'CONTENT_REPORT',
      type: 'OTHER',
      reason,
      targetId: uid.value,
      targetType: 'USER',
    })
  }).then(() => {
    ElMessage.success('举报已提交，我们会尽快处理')
  }).catch(() => { /* cancelled or closed */ })
}

const getAvatarUrl = (avatar: any): string => {
  return avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
}

onMounted(() => {
  loadProfile()
  loadPosts()
})
</script>

<template>
  <div>
    <HeaderNavMenu />
    <!-- Profile Cover -->
    <div class="profile-cover">
      <div class="profile-cover-inner" />
    </div>

    <div v-if="profileLoading" class="wf-state-wrap" style="padding:80px 24px">
      <div class="wf-state-text">{{ $t('general.loading') }}</div>
    </div>

    <div v-else-if="profileError" class="wf-state-wrap" style="padding:80px 24px">
      <div class="wf-state-text wf-state-error">{{ $t('general.userNotExist') }}</div>
      <button class="wf-btn wf-btn-primary" @click="router.push('/')">{{ $t('general.backHome') }}</button>
    </div>

    <template v-else-if="profile">
      <!-- Profile Header Card -->
      <div class="profile-header-wrap">
        <div class="profile-header">
          <div class="profile-header-top">
            <div class="profile-avatar-wrap">
              <img :src="getAvatarUrl(profile.userBrief?.avatar)" alt="avatar" class="profile-avatar">
            </div>
            <div class="profile-info">
              <div class="profile-name-row">
                <span class="profile-name">{{ profile.userBrief?.displayName || $t('general.anonymous') }}</span>
                <span v-if="profile.userBrief?.level" class="profile-badge badge-level">LV.{{ profile.userBrief.level }}</span>
                <span v-if="profile.userBrief?.userType && profile.userBrief.userType !== 'COMMON'" class="profile-badge" :class="profile.userBrief.userType === 'ADMIN' || profile.userBrief.userType === 'MODERATOR' ? 'badge-admin' : 'badge-type'">
                  {{ profile.userBrief.userType === 'ADMIN' ? '管理员' : profile.userBrief.userType === 'MODERATOR' ? '版主' : profile.userBrief.userType === 'VIP' ? 'VIP' : profile.userBrief.userType }}
                </span>
              </div>
              <div class="profile-uid">UID: {{ profile.uid }}</div>
              <div v-if="profile.bio" class="profile-bio">{{ profile.bio }}</div>
              <div v-if="profile.residence" class="profile-residence">
                <i class="fas fa-map-marker-alt"></i> {{ profile.residence }}
              </div>
            </div>
            <div class="profile-actions">
              <template v-if="isSelf">
                <button class="btn btn-outline" @click="goToEditProfile">
                  <i class="fas fa-pen"></i> 编辑资料
                </button>
              </template>
              <template v-else>
                <button
                  :class="['btn', followed ? 'btn-outline' : 'btn-primary']"
                  :disabled="followLoading"
                  @click="handleFollow"
                >
                  <i :class="followed ? 'fas fa-check' : 'fas fa-plus'"></i>
                  {{ followed ? '已关注' : '关注' }}
                </button>
                <button class="btn btn-outline">
                  <i class="fas fa-envelope"></i> 私信
                </button>
                <ReportDropdown
                  target-type="USER"
                  :target-id="uid"
                  placement="bottom-start"
                  @report="handleReportUser"
                />
              </template>
            </div>
          </div>

          <!-- Stats Bar -->
          <div class="profile-stats-bar">
            <div class="profile-stat-item" @click="switchTab('posts')">
              <div class="profile-stat-value">{{ formatNumber(profile.postCount) }}</div>
              <div class="profile-stat-label">帖子</div>
            </div>
            <div class="profile-stat-item" @click="switchTab('followers')">
              <div class="profile-stat-value">{{ formatNumber(profile.followers) }}</div>
              <div class="profile-stat-label">粉丝</div>
            </div>
            <div class="profile-stat-item" @click="switchTab('followings')">
              <div class="profile-stat-value">{{ formatNumber(profile.followings) }}</div>
              <div class="profile-stat-label">关注</div>
            </div>
            <div class="profile-stat-item" @click="switchTab('likes')">
              <div class="profile-stat-value">{{ formatNumber(profile.likeCount) }}</div>
              <div class="profile-stat-label">获赞</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Main Content -->
      <main class="main">
        <div class="left-col">
          <!-- Content Tabs -->
          <div class="content-tabs">
            <button :class="['content-tab', { active: activeTab === 'posts' }]" @click="switchTab('posts')">
              <i class="fas fa-file-alt"></i> 帖子
            </button>
            <button :class="['content-tab', { active: activeTab === 'followers' }]" @click="switchTab('followers')">
              <i class="fas fa-users"></i> 粉丝
            </button>
            <button :class="['content-tab', { active: activeTab === 'followings' }]" @click="switchTab('followings')">
              <i class="fas fa-user-plus"></i> 关注
            </button>
            <button :class="['content-tab', { active: activeTab === 'likes' }]" @click="switchTab('likes')">
              <i class="fas fa-heart"></i> 赞过
            </button>
          </div>

          <!-- Posts Panel -->
          <div v-show="activeTab === 'posts'" class="content-panel">
            <div v-if="postsLoading" class="empty-state">
              <i class="fas fa-spinner fa-pulse"></i>
              <h3>加载中...</h3>
            </div>
            <div v-else-if="posts.length === 0" class="empty-state">
              <i class="far fa-file-alt"></i>
              <h3>暂无帖子</h3>
              <p>还没有发布过帖子</p>
            </div>
            <div v-else class="post-list">
              <div
                v-for="post in posts"
                :key="post.id"
                class="post-card"
                @click="router.push(`/post/${post.id}`)"
              >
                <div class="post-header">
                  <img :src="getAvatarUrl(profile.userBrief?.avatar)" alt="avatar" class="post-author-avatar">
                  <div class="post-meta">
                    <div class="post-author-name">{{ profile.userBrief?.displayName }}</div>
                    <div class="post-time">{{ post.publishedAt ? new Date(post.publishedAt).toLocaleDateString('zh-CN') : '' }}</div>
                  </div>
                  <span v-if="post.category" class="post-tag" :class="post.category?.name === '技术' ? 'tag-tech' : 'tag-life'">{{ post.category.name }}</span>
                </div>
                <div class="post-title-row">
                  <div class="post-title">{{ post.title }}</div>
                  <div v-if="post.tags?.length" class="post-card-tags">
                    <span v-for="tag in post.tags" :key="tag.id" class="post-card-tag"
                      :style="{ backgroundColor: getTagColor(tag.name) }">{{ tag.name }}</span>
                  </div>
                </div>
                <div v-if="post.summary" class="post-excerpt">{{ post.summary }}</div>
                <div class="post-footer">
                  <span class="post-stat"><i class="far fa-comment"></i> {{ post.commentCount || 0 }}</span>
                  <span class="post-stat"><i class="far fa-eye"></i> {{ formatNumber(post.viewCount) }}</span>
                  <span class="post-stat"><i class="far fa-heart"></i> {{ post.likeCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Followers Panel -->
          <div v-show="activeTab === 'followers'" class="content-panel">
            <div v-if="relationLoading" class="empty-state">
              <i class="fas fa-spinner fa-pulse"></i>
              <h3>加载中...</h3>
            </div>
            <div v-else-if="followers.length === 0" class="empty-state">
              <i class="far fa-users"></i>
              <h3>暂无粉丝</h3>
            </div>
            <div v-else class="user-list">
              <div
                v-for="user in followers"
                :key="user.uid"
                class="user-list-item"
                @click="goToUserPage(user.uid)"
              >
                <img :src="getAvatarUrl(user.avatar)" alt="" class="user-list-avatar">
                <div class="user-list-info">
                  <div class="user-list-name">{{ user.displayName }}</div>
                  <div class="user-list-meta" v-if="user.level">LV.{{ user.level }}</div>
                </div>
                <button
                  v-if="!isSelf && user.uid !== currentUserId"
                  class="user-list-action"
                  :class="{ following: user.isFollowed }"
                  @click.stop="toggleFollowUser(user.uid).then(() => user.isFollowed = !user.isFollowed).catch(() => {})"
                >
                  {{ user.isFollowed ? '已关注' : '关注' }}
                </button>
              </div>
            </div>
          </div>

          <!-- Followings Panel -->
          <div v-show="activeTab === 'followings'" class="content-panel">
            <div v-if="relationLoading" class="empty-state">
              <i class="fas fa-spinner fa-pulse"></i>
              <h3>加载中...</h3>
            </div>
            <div v-else-if="followings.length === 0" class="empty-state">
              <i class="far fa-user-plus"></i>
              <h3>暂无关注</h3>
            </div>
            <div v-else class="user-list">
              <div
                v-for="user in followings"
                :key="user.uid"
                class="user-list-item"
                @click="goToUserPage(user.uid)"
              >
                <img :src="getAvatarUrl(user.avatar)" alt="" class="user-list-avatar">
                <div class="user-list-info">
                  <div class="user-list-name">{{ user.displayName }}</div>
                  <div class="user-list-meta" v-if="user.level">LV.{{ user.level }}</div>
                </div>
                <button
                  v-if="!isSelf && user.uid !== currentUserId"
                  class="user-list-action following"
                  @click.stop="toggleFollowUser(user.uid).then(() => user.isFollowed = false).catch(() => {})"
                >
                  已关注
                </button>
              </div>
            </div>
          </div>

          <!-- Likes Panel -->
          <div v-show="activeTab === 'likes'" class="content-panel">
            <div class="empty-state">
              <i class="far fa-heart"></i>
              <h3>暂无赞过的内容</h3>
              <p>去社区逛逛，发现精彩内容</p>
            </div>
          </div>
        </div>

        <!-- Right Column -->
        <div class="right-col">
          <!-- Personal Info Widget -->
          <div class="widget">
            <div class="widget-header">
              <div class="widget-title"><i class="fas fa-user-circle"></i> 个人资料</div>
            </div>
            <div class="widget-body">
              <div class="info-list">
                <div class="info-item">
                  <div class="info-icon"><i class="fas fa-venus-mars"></i></div>
                  <div class="info-content">
                    <div class="info-label">性别</div>
                    <div class="info-value">{{ genderLabel(profile.gender) || '未设置' }}</div>
                  </div>
                </div>
                <div v-if="profile.birthday" class="info-item">
                  <div class="info-icon"><i class="fas fa-birthday-cake"></i></div>
                  <div class="info-content">
                    <div class="info-label">生日</div>
                    <div class="info-value">{{ profile.birthday }}</div>
                  </div>
                </div>
                <div v-if="profile.residence" class="info-item">
                  <div class="info-icon"><i class="fas fa-map-marker-alt"></i></div>
                  <div class="info-content">
                    <div class="info-label">所在地</div>
                    <div class="info-value">{{ profile.residence }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="profile.createdAt" class="join-info">
              <i class="fas fa-calendar-alt"></i>
              <span>于 {{ new Date(profile.createdAt).toLocaleDateString('zh-CN') }} 加入 WaterFun</span>
            </div>
          </div>
        </div>
      </main>
    </template>
  </div>
</template>

<style scoped>
/* ========== Profile Cover ========== */
.profile-cover {
  position: relative;
  height: 280px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  overflow: hidden;
}

.profile-cover-inner {
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.08'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  opacity: 0.6;
}

/* ========== Profile Header Card ========== */
.profile-header-wrap {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
  position: relative;
}

.profile-header {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 0 32px 24px;
  margin-top: -60px;
  position: relative;
  z-index: 10;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
}

.profile-header-top {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding-top: 0;
}

.profile-avatar-wrap {
  position: relative;
  margin-top: -48px;
  flex-shrink: 0;
}

.profile-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #ffffff;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
  background: #ffffff;
}

.profile-info {
  flex: 1;
  padding-bottom: 8px;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.profile-name {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.5px;
}

.profile-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.badge-level {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  color: white;
}

.badge-type {
  background: #eff6ff;
  color: #3b82f6;
}

.badge-admin {
  background: linear-gradient(135deg, #ef4444, #f87171);
  color: white;
}

.profile-uid {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 8px;
}

.profile-bio {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
  max-width: 500px;
}

.profile-residence {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 6px;
}

.profile-residence i {
  margin-right: 4px;
}

.profile-actions {
  display: flex;
  gap: 10px;
  padding-bottom: 8px;
  flex-shrink: 0;
}

/* ========== Buttons ========== */
.btn {
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
  border: none;
  text-decoration: none;
  white-space: nowrap;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline {
  background: #ffffff;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.btn-outline:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background: #eff6ff;
}

/* ========== Profile Stats Bar ========== */
.profile-stats-bar {
  display: flex;
  gap: 0;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.profile-stat-item {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.profile-stat-item:hover {
  background: #f8fafc;
}

.profile-stat-item:not(:last-child) {
  border-right: 1px solid #f1f5f9;
}

.profile-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.profile-stat-label {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 2px;
}

/* ========== Main Layout ========== */
.main {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;
}

/* ========== Left Column ========== */
.left-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.content-tabs {
  display: flex;
  gap: 4px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 4px;
}

.content-tab {
  flex: 1;
  padding: 10px;
  text-align: center;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  background: transparent;
}

.content-tab:hover {
  color: #3b82f6;
  background: #eff6ff;
}

.content-tab.active {
  background: #3b82f6;
  color: white;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.content-panel {
  /* display controlled by v-show */
}

/* ========== Post List ========== */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s ease;
  cursor: pointer;
}

.post-card:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.post-author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.post-meta {
  flex: 1;
}

.post-author-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.post-time {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.post-tag {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.tag-tech { background: #dbeafe; color: #1d4ed8; }
.tag-life { background: #dcfce7; color: #15803d; }

.post-title {
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-excerpt {
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 14px;
}

.post-footer {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.post-stat {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #94a3b8;
}

.post-stat i { font-size: 14px; }

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h3 {
  font-size: 16px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 6px;
}

.empty-state p {
  font-size: 14px;
}

/* ========== Right Column ========== */
.right-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.widget {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.widget-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.widget-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.widget-title i {
  color: #3b82f6;
  font-size: 14px;
}

.widget-body {
  padding: 16px 20px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.info-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #eff6ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  font-size: 14px;
  flex-shrink: 0;
}

.info-content {
  flex: 1;
  min-width: 0;
}

.info-label {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 2px;
}

.info-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

.join-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
  font-size: 13px;
  color: #94a3b8;
}

.join-info i { color: #3b82f6; }

/* ========== User List (followers/followings) ========== */
.user-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.user-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f1f5f9;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  margin-bottom: 8px;
}

.user-list-item:last-child { margin-bottom: 0; }

.user-list-item:hover {
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
  border-color: #cbd5e1;
}

.user-list-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e2e8f0;
}

.user-list-info { flex: 1; min-width: 0; }

.user-list-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.user-list-meta {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.user-list-action {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #64748b;
  transition: all 0.2s ease;
}

.user-list-action:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background: #eff6ff;
}

.user-list-action.following {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

/* ========== Responsive ========== */
@media (max-width: 1024px) {
  .main { grid-template-columns: 1fr; }
  .right-col { display: none; }
  .profile-cover { height: 200px; }
}

@media (max-width: 768px) {
  .profile-header-wrap { padding: 0 16px; }
  .profile-header { padding: 0 16px 20px; }
  .profile-header-top {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  .profile-avatar-wrap { margin-top: -40px; }
  .profile-avatar { width: 100px; height: 100px; }
  .profile-info { padding-bottom: 0; }
  .profile-name-row { justify-content: center; }
  .profile-actions {
    justify-content: center;
    width: 100%;
  }
  .profile-stats-bar { margin-top: 12px; }
  .main { padding: 16px; }
  .profile-cover { height: 160px; }
}
</style>
