<script setup lang="ts">
import { usePostStore } from '~/stores/postStore'
import { useAuthStore } from '~/stores/authStore'
import { useUserInfoStore } from '~/stores/userInfoStore'
import { storeToRefs } from 'pinia'
import { useRouter, useRoute } from 'vue-router'
import BannerCarousel from '~/components/BannerCarousel.vue'
//@ts-ignore
import { View, Star, ChatDotSquare, ArrowRight, Edit, User, Bell, Link as IconLink } from '@element-plus/icons-vue'

const postStore = usePostStore()
const authStore = useAuthStore()
const userInfoStore = useUserInfoStore()
const { posts, pagination, loading, categories } = storeToRefs(postStore)
const { userInfo } = storeToRefs(userInfoStore)
const router = useRouter()
const route = useRoute()

const onlineStats = ref<{ onlineCount: number; todayNewUsers: number; todayPeakOnline: number } | null>(null)

const fetchOnlineStats = async () => {
  try {
    const { default: request } = await import('~/utils/axiosRequest')
    const res = await request.get('/online-users/stats')
    onlineStats.value = res.data as { onlineCount: number; todayNewUsers: number; todayPeakOnline: number }
  } catch { /* ignore */ }
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
              <div v-else style="display:flex;flex-direction:column;gap:12px">
                <el-card
                  v-for="post in posts"
                  :key="post.id"
                  shadow="hover"
                  style="cursor:pointer"
                  @click="goToDetail(post.id)"
                >
                  <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px;flex-wrap:wrap">
                    <span style="font-size:14px;font-weight:600;color:#1e293b">{{ post.category?.name || '未分类' }}</span>
                    <span style="font-size:12px;color:#94a3b8">{{ post.publishedAt ? new Date(post.publishedAt).toLocaleDateString() : '' }}</span>
                    <el-tag v-for="tag in post.tags" :key="tag.id" size="small" type="primary" effect="plain" style="margin-left:4px">{{ tag.name }}</el-tag>
                  </div>
                  <h3 style="font-size:16px;font-weight:600;color:#1e293b;line-height:1.5;margin:0 0 8px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden">{{ post.title }}</h3>
                  <p v-if="post.summary" style="font-size:14px;color:#64748b;line-height:1.7;margin:0 0 12px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden">{{ post.summary }}</p>
                  <el-divider style="margin:12px 0" />
                  <div style="display:flex;align-items:center;gap:20px;color:#94a3b8;font-size:13px">
                    <span style="display:flex;align-items:center;gap:5px"><el-icon size="14"><View /></el-icon> {{ post.viewCount }}</span>
                    <span style="display:flex;align-items:center;gap:5px"><el-icon size="14"><Star /></el-icon> {{ post.likeCount }}</span>
                    <span style="display:flex;align-items:center;gap:5px"><el-icon size="14"><ChatDotSquare /></el-icon> {{ post.commentCount }}</span>
                  </div>
                </el-card>
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
                    <el-button type="primary" style="width:100%;margin-top:16px" @click="goToCreate">
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
                <div style="display:flex;align-items:center;justify-content:space-between">
                  <span style="font-size:15px;font-weight:600;display:flex;align-items:center;gap:8px">🔥 {{ $t('community.hotTopics') }}</span>
                  <NuxtLink to="/post" style="font-size:13px;color:#94a3b8;text-decoration:none">{{ $t('option.more') }} <el-icon size="12"><ArrowRight /></el-icon></NuxtLink>
                </div>
              </template>
              <div
                v-for="(item, i) in [
                  { title: 'AI 辅助编程工具大盘点', heat: '12.5k' },
                  { title: '远程工作三年，我的一些心得', heat: '9.8k' },
                  { title: '2024 年前端面试题汇总', heat: '8.2k' },
                  { title: 'Mac 开发环境配置指南', heat: '6.1k' },
                  { title: '独立开发者月入过万经验分享', heat: '5.4k' },
                ]"
                :key="i"
                class="hot-topic-item"
              >
                <div :class="['topic-rank', i < 3 ? 'rank-top' : 'rank-normal']">{{ i + 1 }}</div>
                <div style="flex:1;min-width:0">
                  <div class="topic-title">{{ item.title }}</div>
                  <div class="topic-heat">🔥 {{ item.heat }} 热度</div>
                </div>
              </div>
            </el-card>

            <el-card shadow="never">
              <template #header>
                <span style="font-size:15px;font-weight:600;display:flex;align-items:center;gap:8px"><el-icon size="16"><Bell /></el-icon> {{ $t('community.announcements') }}</span>
              </template>
              <div
                v-for="(notice, i) in ['社区规范更新，请查阅', '新版编辑器已上线', '端午节活动即将开始']"
                :key="i"
                class="notice-item"
              >
                <span class="notice-dot" />
                <span>{{ notice }}</span>
              </div>
            </el-card>

            <el-card shadow="never" v-if="onlineStats">
              <template #header>
                <span style="font-size:15px;font-weight:600;display:flex;align-items:center;gap:8px"><el-icon size="16"><User /></el-icon> {{ $t('community.onlineStats') }}</span>
              </template>
              <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;padding:16px 20px">
                <div style="text-align:center">
                  <div style="font-size:20px;font-weight:700;color:#3b82f6">{{ onlineStats.onlineCount }}</div>
                  <div style="font-size:12px;color:#94a3b8;margin-top:2px">{{ $t('community.onlineCount') }}</div>
                </div>
                <div style="text-align:center">
                  <div style="font-size:20px;font-weight:700;color:#67c23a">{{ onlineStats.todayNewUsers }}</div>
                  <div style="font-size:12px;color:#94a3b8;margin-top:2px">{{ $t('community.todayNew') }}</div>
                </div>
                <div style="text-align:center">
                  <div style="font-size:20px;font-weight:700;color:#e6a23c">{{ onlineStats.todayPeakOnline }}</div>
                  <div style="font-size:12px;color:#94a3b8;margin-top:2px">{{ $t('community.todayPeak') }}</div>
                </div>
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
.hot-topic-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  cursor: pointer;
  transition: background 0.2s;
}
.hot-topic-item:hover {
  background: #f8fafc;
}
.topic-rank {
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
.rank-top {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  color: white;
}
.rank-normal {
  background: #f1f5f9;
  color: #94a3b8;
}
.topic-title {
  font-size: 14px;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.topic-heat {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}
.notice-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px solid #f1f5f9;
}
.notice-item:last-child {
  border-bottom: none;
}
.notice-item:hover {
  background: #f8fafc;
  color: #3b82f6;
}
.notice-dot {
  width: 6px;
  height: 6px;
  background: #3b82f6;
  border-radius: 50%;
  flex-shrink: 0;
}
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
</style>
