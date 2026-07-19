<script setup lang="ts">
import { usePostStore } from '~/stores/postStore'
import { useAuthStore } from '~/stores/authStore'
import { useUserInfoStore } from '~/stores/userInfoStore'
import { storeToRefs } from 'pinia'
import { useRouter, useRoute } from 'vue-router'
import { computed, ref, onMounted } from 'vue'
import { Edit, User, ArrowRight } from '@element-plus/icons-vue'
import { getTagColor } from '@waterfun/web-core/src/tagColor'
import { formatRelativeTime } from '~/utils/date'

const postStore = usePostStore()
const authStore = useAuthStore()
const userInfoStore = useUserInfoStore()
const { posts, pagination, loading, categories } = storeToRefs(postStore)
const { userInfo } = storeToRefs(userInfoStore)
const router = useRouter()
const route = useRoute()

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
})
</script>

<template>
  <div>
    <HeaderNavMenu />
    <div class="page-container">
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


          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>


