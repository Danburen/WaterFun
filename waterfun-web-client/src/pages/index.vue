<script setup lang="ts">
import { usePostStore } from '~/stores/postStore'
import { useAuthStore } from '~/stores/authStore'
import { useUserInfoStore } from '~/stores/userInfoStore'
import { storeToRefs } from 'pinia'

import { useRouter, useRoute } from 'vue-router'
import BannerCarousel from '~/components/BannerCarousel.vue'
//@ts-ignore
import { formatRelativeTime } from '~/utils/date'
import { View, Star, ChatDotSquare, ArrowRight, Edit, User, Bell, Link as IconLink } from '@element-plus/icons-vue'
import { getTagColor } from '@waterfun/web-core/src/tagColor'

const postStore = usePostStore()
const authStore = useAuthStore()
const userInfoStore = useUserInfoStore()
const { posts, pagination, loading, categories } = storeToRefs(postStore)
const { userInfo } = storeToRefs(userInfoStore)
const router = useRouter()
const route = useRoute()

const onlineStats = ref<{ onlineCount: number; todayNewUsers: number; todayPeakOnline: number } | null>(null)
const onlineUsers = ref<any[]>([])
const onlineUsersLoading = ref(false)
const myStats = ref<{ publishedCount: number; followerCount: number; totalLikeCount: number } | null>(null)

const fetchMyStats = async () => {
  try {
    const { default: request } = await import('~/utils/axiosRequest')
    const res = await request.get('/posts/me/stats')
    const d = res.data as any
    myStats.value = { publishedCount: d.publishedCount, followerCount: d.followerCount, totalLikeCount: d.totalLikeCount }
  } catch { /* ignore */ }
}

const fetchOnlineStats = async () => {
  try {
    const { default: request } = await import('~/utils/axiosRequest')
    const res = await request.get('/online-users/stats')
    onlineStats.value = res.data as { onlineCount: number; todayNewUsers: number; todayPeakOnline: number }
  } catch { /* ignore */ }
}

const fetchOnlineUsers = async () => {
  onlineUsersLoading.value = true
  try {
    const { default: request } = await import('~/utils/axiosRequest')
    const res = await request.get('/online-users/list', { params: { page: 0, size: 12 } })
    onlineUsers.value = (res.data.content || []) as any[]
  } catch { /* ignore */ }
  onlineUsersLoading.value = false
}

const hotPosts = ref<any[]>([])
const hotPostsPage = ref(0)
const hotPostsLoading = ref(false)
const fetchHotPosts = async (page: number) => {
  hotPostsLoading.value = true
  try {
    const { default: request } = await import('~/utils/axiosRequest')
    const res = await request.get('/posts/hot', { params: { page, size: 5 } })
    hotPosts.value = res.data.content as any[]
    hotPostsPage.value = page
  } catch { /* ignore */ }
  hotPostsLoading.value = false
}

const announcements = ref<any[]>([])
const announcementsPage = ref(0)
const announcementsLoading = ref(false)
const fetchAnnouncements = async (page: number) => {
  announcementsLoading.value = true
  try {
    const { default: request } = await import('~/utils/axiosRequest')
    const res = await request.get('/announcements', { params: { page, size: 5 } })
    announcements.value = res.data.content as any[]
    announcementsPage.value = page
  } catch { /* ignore */ }
  announcementsLoading.value = false
}

const hotTags = ref<any[]>([])
const hotTagsLoading = ref(false)
const fetchHotTags = async () => {
  hotTagsLoading.value = true
  try {
    const { default: request } = await import('~/utils/axiosRequest')
    const res = await request.get('/post/tags/hot', { params: { page: 0, size: 8 } })
    hotTags.value = res.data.content as any[]
  } catch { /* ignore */ }
  hotTagsLoading.value = false
}

const selectedCategoryId = ref<string | undefined>(
  route.query.category ? String(route.query.category) : undefined
)

const selectCategory = (id: string | undefined) => {
  selectedCategoryId.value = id
  router.replace({ query: { ...route.query, category: id || undefined, page: '1' } })
  postStore.fetchPostList({ categoryId: id, page: 1, size: 12 })
}

const currentPage = computed({
  get: () => pagination.value.number + 1,
  set: (page: number) => {
    router.replace({ query: { ...route.query, page: String(page) } })
    postStore.fetchPostList({ categoryId: selectedCategoryId.value, page, size: 12 })
  }
})

const goToDetail = (id: string) => router.push(`/post/${id}`)
const goToCreate = () => router.push('/post/create')

onMounted(async () => {
  const page = route.query.page ? Math.max(1, parseInt(route.query.page as string)) : 1
  try { await postStore.fetchPostList({ categoryId: selectedCategoryId.value, page, size: 12 }) } catch { /* ignore */ }
  postStore.fetchCategories()
  fetchOnlineStats()
  fetchOnlineUsers()
  fetchHotPosts(0)
  fetchAnnouncements(0)
  fetchHotTags()
  if (authStore.isAccess) fetchMyStats()
})

</script>
<template>
  <div>
    <HeaderNavMenu />
    <BannerCarousel />
    <div style="max-width:1280px;margin:0 auto;padding:24px">
      <el-row :gutter="24">
        <el-col :xs="24" :md="17">
          <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:16px">
            <el-tag
              :type="selectedCategoryId === undefined ? 'primary' : 'info'"
              :effect="selectedCategoryId === undefined ? 'dark' : 'plain'"
              style="cursor:pointer;border-radius:20px;padding:0 16px;height:34px;line-height:34px"
              @click="selectCategory(undefined)"
            >{{ $t('community.all') }}</el-tag>
            <el-tag
              v-for="cat in categories"
              :key="cat.id"
              :type="selectedCategoryId === cat.id ? 'primary' : 'info'"
              :effect="selectedCategoryId === cat.id ? 'dark' : 'plain'"
              style="cursor:pointer;border-radius:20px;padding:0 16px;height:34px;line-height:34px"
              @click="selectCategory(cat.id)"
            >{{ cat.name }}</el-tag>
          </div>

          <el-skeleton :loading="loading" :count="3" animated>
            <template #default>
              <el-empty v-if="posts.length === 0" :description="$t('message.empty.noPosts')">
                <el-button type="primary" @click="goToCreate">{{ $t('community.publish') }}</el-button>
              </el-empty>
              <div v-else style="display:flex;flex-direction:column;gap:10px">
                <div
                  v-for="post in posts"
                  :key="post.id"
                  :class="['post-card-item', { 'is-system-card': !post.userBrief, pinned: post.isPinned }]"
                  style="cursor:pointer;padding:12px 14px"
                  @click="goToDetail(post.id)"
                >
                  <div class="post-title-row">
                    <h3 class="post-title">{{ post.title }}</h3>
                    <div class="post-title-meta">
                      <div v-if="post.tags?.length" class="post-card-tags">
                        <span v-for="tag in post.tags" :key="tag.id" class="post-card-tag"
                          :style="{ backgroundColor: getTagColor(tag.name) }">{{ tag.name }}</span>
                      </div>
                      <span v-if="post.isPinned" class="badge-pinned"><i class="fas fa-thumbtack"></i></span>
                      <span v-if="post.type === 'NOTICE'" class="post-tag tag-notice">公告</span>
                      <span v-if="post.category && selectedCategoryId === undefined" class="post-tag tag-category">{{ post.category.name }}</span>
                    </div>
                  </div>
                  <p v-if="post.subtitle" class="post-subtitle">{{ post.subtitle }}</p>

                  <template v-if="post.coverImage">
                    <div class="post-media-row">
                      <div class="post-cover-wrap">
                        <img :src="post.coverImage.url" class="post-cover-thumb" :alt="post.title" />
                        <img v-if="post.userBrief" :src="post.userBrief.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" class="post-avatar-overlay" :alt="post.userBrief.displayName" />
                      </div>
                      <div class="post-media-content">
                        <p v-if="post.summary" class="post-excerpt">{{ post.summary }}</p>
                        <div class="post-meta-row">
                          <span class="post-meta-name">{{ post.userBrief ? post.userBrief.displayName : '系统' }}</span>
                          <span class="post-meta-dot">·</span>
                          <span class="post-meta-time">{{ post.publishedAt ? formatRelativeTime(post.publishedAt) : '' }}</span>
                        </div>
                      </div>
                    </div>
                  </template>
                  <template v-else>
                    <div class="post-media-row text-only">
                      <p v-if="post.summary" class="post-excerpt">{{ post.summary }}</p>
                      <div class="post-meta-row">
                        <template v-if="post.userBrief">
                          <img :src="post.userBrief.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" class="post-meta-avatar" :alt="post.userBrief.displayName" />
                          <span class="post-meta-name">{{ post.userBrief.displayName }}</span>
                        </template>
                        <template v-else>
                          <span class="post-meta-avatar system-avatar-sm"><i class="fas fa-robot"></i></span>
                          <span class="post-meta-name">系统</span>
                        </template>
                        <span class="post-meta-dot">·</span>
                        <span class="post-meta-time">{{ post.publishedAt ? formatRelativeTime(post.publishedAt) : '' }}</span>
                      </div>
                    </div>
                  </template>

                  <div class="post-footer">
                    <span class="post-stat"><i class="far fa-eye"></i> {{ post.viewCount }}</span>
                    <span class="post-stat"><i class="far fa-heart"></i> {{ post.likeCount }}</span>
                    <span class="post-stat"><i class="far fa-comment"></i> {{ post.commentCount }}</span>
                  </div>
                </div>
              </div>
              <div v-if="pagination.totalPages > 1" style="display:flex;justify-content:center;margin-top:24px">
                <el-pagination
                  v-model:current-page="currentPage"
                  :page-size="pagination.size"
                  :total="pagination.totalElements"
                  layout="prev, pager, next"
                  background
                />
              </div>
            </template>
          </el-skeleton>
        </el-col>

        <el-col :xs="0" :md="7">
          <div style="display:flex;flex-direction:column;gap:20px">
            <el-card shadow="never">
              <ClientOnly>
                <div style="text-align:center">
                  <template v-if="authStore.isAccess && userInfo">
                    <el-avatar :size="64" :src="userInfo.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" style="margin-bottom:10px" />
                    <div style="font-size:16px;font-weight:600;color:#1e293b;margin-bottom:4px">{{ userInfo.nickname || userInfo.username }}</div>
                    <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px;padding:12px 0 8px">
                      <div style="text-align:center">
                        <div style="font-size:18px;font-weight:700;color:#3b82f6">{{ myStats?.publishedCount ?? '-' }}</div>
                        <div style="font-size:11px;color:#94a3b8;margin-top:2px">发布</div>
                      </div>
                      <div style="text-align:center">
                        <div style="font-size:18px;font-weight:700;color:#67c23a">{{ myStats?.followerCount ?? '-' }}</div>
                        <div style="font-size:11px;color:#94a3b8;margin-top:2px">关注者</div>
                      </div>
                      <div style="text-align:center">
                        <div style="font-size:18px;font-weight:700;color:#e6a23c">{{ myStats?.totalLikeCount ?? '-' }}</div>
                        <div style="font-size:11px;color:#94a3b8;margin-top:2px">获赞</div>
                      </div>
                    </div>
                    <el-button type="primary" style="width:100%;margin-top:4px" @click="goToCreate">
                      <el-icon size="14" style="margin-right:4px"><Edit /></el-icon> {{ $t('community.publish') }}
                    </el-button>
                  </template>
                  <template v-else>
                    <div style="font-size:16px;font-weight:600;color:#1e293b;margin-bottom:8px">{{ $t('community.welcome') }}</div>
                    <p style="font-size:13px;color:#64748b;margin-bottom:16px">{{ $t('community.loginPrompt') }}</p>
                    <el-button type="primary" style="width:100%" @click="router.push('/login')">
                      <el-icon size="14" style="margin-right:4px"><User /></el-icon> {{ $t('community.loginNow') }}
                    </el-button>
                  </template>
                </div>
              </ClientOnly>
            </el-card>

            <el-card shadow="never">
              <template #header>
                <span style="font-size:15px;font-weight:600;display:flex;align-items:center;gap:8px"><i class="fas fa-fire" style="color:#f59e0b;font-size:14px"></i> {{ $t('community.hotTags') }}</span>
              </template>
              <el-skeleton :loading="hotTagsLoading" :count="4" animated>
                <div v-if="hotTags.length" style="display:flex;flex-wrap:wrap;gap:8px;padding:12px 20px">
                  <el-tag
                    v-for="tag in hotTags"
                    :key="tag.id"
                    :style="{ backgroundColor: getTagColor(tag.name), color: '#fff', border: 'none', cursor: 'pointer' }"
                    @click="router.push('/search?q=' + encodeURIComponent(tag.name))"
                  >{{ tag.name }}
                    <span v-if="tag.usageCount != null" style="margin-left:4px;opacity:.8;font-size:11px">{{ tag.usageCount }}</span>
                  </el-tag>
                </div>
              </el-skeleton>
            </el-card>

            <el-card shadow="never">
              <template #header>
                <div style="display:flex;align-items:center;justify-content:space-between">
                  <span style="font-size:15px;font-weight:600;display:flex;align-items:center;gap:8px">🔥 {{ $t('community.hotTopics') }}</span>
                  <el-button text size="small" :loading="hotPostsLoading" style="font-size:13px;color:#94a3b8" @click="fetchHotPosts(hotPostsPage + 1)">
                    {{ $t('option.shuffle') }} <el-icon size="12"><ArrowRight /></el-icon>
                  </el-button>
                </div>
              </template>
              <el-skeleton :loading="hotPostsLoading" :count="3" animated>
                <div
                  v-for="(item, i) in hotPosts"
                  :key="item.id"
                  class="hot-topic-item"
                  @click="goToDetail(item.id)"
                >
                  <div :class="['topic-rank', i < 3 ? 'rank-top' : 'rank-normal']">{{ i + 1 }}</div>
                  <div style="flex:1;min-width:0">
                    <div class="topic-title">{{ item.title }}</div>
                    <div class="topic-heat">🔥 {{ item.viewCount + item.likeCount * 2 + item.commentCount * 3 }} 热度</div>
                  </div>
                </div>
                <el-empty v-if="!hotPostsLoading && hotPosts.length === 0" :description="$t('message.empty.noPosts')" />
              </el-skeleton>
            </el-card>

            <el-card shadow="never">
              <template #header>
                <div style="display:flex;align-items:center;justify-content:space-between">
                  <span style="font-size:15px;font-weight:600;display:flex;align-items:center;gap:8px"><el-icon size="16"><Bell /></el-icon> {{ $t('community.announcements') }}</span>
                  <el-button text size="small" :loading="announcementsLoading" style="font-size:13px;color:#94a3b8" @click="fetchAnnouncements(announcementsPage + 1)">
                    {{ $t('option.shuffle') }} <el-icon size="12"><ArrowRight /></el-icon>
                  </el-button>
                </div>
              </template>
              <el-skeleton :loading="announcementsLoading" :count="3" animated>
                <div
                  v-for="notice in announcements"
                  :key="notice.id"
                  class="notice-item"
                  @click="goToDetail(notice.id)"
                >
                  <span class="notice-dot" />
                  <span>{{ notice.title }}</span>
                </div>
                <el-empty v-if="!announcementsLoading && announcements.length === 0" :description="$t('message.empty.noPosts')" />
              </el-skeleton>
            </el-card>

            <el-card shadow="never">
              <template #header>
                <span style="font-size:15px;font-weight:600;display:flex;align-items:center;gap:8px"><el-icon size="16"><User /></el-icon> {{ $t('community.onlineStats') }}</span>
              </template>
              <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;padding:16px 20px 8px">
                <div style="text-align:center">
                  <div style="font-size:20px;font-weight:700;color:#3b82f6">{{ onlineStats?.onlineCount ?? '-' }}</div>
                  <div style="font-size:12px;color:#94a3b8;margin-top:2px">{{ $t('community.onlineCount') }}</div>
                </div>
                <div style="text-align:center">
                  <div style="font-size:20px;font-weight:700;color:#67c23a">{{ onlineStats?.todayNewUsers ?? '-' }}</div>
                  <div style="font-size:12px;color:#94a3b8;margin-top:2px">{{ $t('community.todayNew') }}</div>
                </div>
                <div style="text-align:center">
                  <div style="font-size:20px;font-weight:700;color:#e6a23c">{{ onlineStats?.todayPeakOnline ?? '-' }}</div>
                  <div style="font-size:12px;color:#94a3b8;margin-top:2px">{{ $t('community.todayPeak') }}</div>
                </div>
              </div>
              <el-divider style="margin:4px 0" />
              <div style="padding:8px 20px 16px">
                <div style="font-size:13px;color:#64748b;margin-bottom:10px">{{ $t('community.onlineUsers') }}</div>
                <el-skeleton :loading="onlineUsersLoading" :count="3" animated>
                  <div v-if="onlineUsers.length" style="display:flex;flex-wrap:wrap;gap:10px">
                    <div
                      v-for="user in onlineUsers"
                      :key="user.uid"
                      class="online-user-item"
                      @click="router.push('/user/' + user.uid)"
                    >
                      <el-avatar :size="32" :src="user.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                      <div class="online-user-name">{{ user.displayName || user.nickname || user.username }}</div>
                    </div>
                  </div>
                  <div v-else-if="!onlineUsersLoading" style="font-size:13px;color:#94a3b8;text-align:center;padding:8px 0">{{ $t('general.noData') }}</div>
                </el-skeleton>
              </div>
            </el-card>

            <el-card shadow="never">
              <template #header>
                <span style="font-size:15px;font-weight:600;display:flex;align-items:center;gap:8px"><el-icon size="16"><IconLink /></el-icon> {{ $t('community.friendLinks') }}</span>
              </template>
              <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;padding:16px 20px">
                <a
                  v-for="link in [{ name:'GitHub', url:'https://github.com' }, { name:'Stack Overflow', url:'https://stackoverflow.com' }, { name:'V2EX', url:'https://v2ex.com' }, { name:'掘金', url:'https://juejin.cn' }]"
                  :key="link.name"
                  :href="link.url"
                  target="_blank"
                  class="link-item"
                >{{ link.name }}</a>
              </div>
            </el-card>
          </div>
        </el-col>
      </el-row>
    </div>
    <el-divider style="margin:0" />
    <footer style="padding:32px 24px;text-align:center">
      <div style="display:flex;justify-content:center;gap:24px;margin-bottom:16px">
        <NuxtLink to="/about" style="font-size:14px;color:#64748b;text-decoration:none">关于我们</NuxtLink>
        <a href="#" style="font-size:14px;color:#64748b;text-decoration:none">社区规范</a>
        <a href="#" style="font-size:14px;color:#64748b;text-decoration:none">隐私政策</a>
        <a href="#" style="font-size:14px;color:#64748b;text-decoration:none">联系我们</a>
      </div>
      <div style="font-size:13px;color:#94a3b8">© 2026 WaterFun. All rights reserved.</div>
    </footer>
  </div>
</template>
<style scoped>
.link-item {
  display: block;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 13px;
  color: #64748b;
  text-align: center;
  text-decoration: none;
  transition: all 0.2s;
}
.link-item:hover {
  background: #eff6ff;
  color: #3b82f6;
}
.online-user-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: transform 0.15s;
  width: 48px;
}
.online-user-item:hover {
  transform: translateY(-2px);
}
.online-user-name {
  font-size: 11px;
  color: #64748b;
  max-width: 48px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: center;
}
</style>
