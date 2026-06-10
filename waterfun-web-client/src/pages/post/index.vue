<script setup lang="ts">
import { usePostStore } from '~/stores/postStore'
import { storeToRefs } from 'pinia'
import { useRouter, useRoute } from 'vue-router'

const postStore = usePostStore()
const { posts, pagination, loading, categories } = storeToRefs(postStore)
const router = useRouter()
const route = useRoute()

const selectedTab = ref<number | ''>('')

const searchParams = reactive({
  categoryId: undefined as number | undefined,
  tagIds: undefined as number[] | undefined,
  page: 1,
  size: 10
})

const syncUrl = () => {
  const query: Record<string, string> = {}
  if (searchParams.categoryId) query.category = String(searchParams.categoryId)
  if (searchParams.page > 1) query.page = String(searchParams.page)
  router.replace({ query })
}

const fetchPosts = async () => {
  try {
    await postStore.fetchPostList({ ...searchParams })
    syncUrl()
  } catch { /* ignore */ }
}

const handleTabChange = (tab: number | '') => {
  selectedTab.value = tab
  const cat = categories.value.find(c => c.id === tab)
  searchParams.categoryId = cat ? cat.id : undefined
  searchParams.page = 1
  fetchPosts()
}

const handlePageChange = (page: number) => {
  searchParams.page = page
  fetchPosts()
}

const goToDetail = (id: number) => router.push(`/post/${id}`)
const goToCreate = () => router.push('/post/create')
const goToUser = (uid: number) => router.push(`/User/${uid}`)

onMounted(() => {
  if (route.query.category) {
    const catId = parseInt(route.query.category as string)
    selectedTab.value = isNaN(catId) ? '' : catId
    searchParams.categoryId = isNaN(catId) ? undefined : catId
  }
  if (route.query.page) {
    searchParams.page = Math.max(1, parseInt(route.query.page as string))
  }
  fetchPosts()
  postStore.fetchCategories()
})
</script>

<template>
  <div>
    <HeaderNavMenu />
    <div class="wf-container" style="padding-top:24px">
      <div class="wf-grid-2">
        <div>
          <div class="wf-flex-between wf-mb-md" style="flex-wrap:wrap;gap:12px">
            <div>
              <h1 class="wf-text-primary" style="font-size:22px;font-weight:700;margin:0">{{ $t('community.title') }}</h1>
              <div class="wf-flex-center wf-gap-md wf-text-xs wf-text-muted wf-mt-sm">
                <span>📄 {{ pagination.totalElements }} 帖子</span>
              </div>
            </div>
            <el-button type="primary" @click="goToCreate">{{ $t('community.publish') }}</el-button>
          </div>

          <div class="wf-flex-center wf-gap-sm wf-flex-wrap wf-mb-md">
            <el-tag
              :type="selectedTab === '' ? 'primary' : 'info'"
              :effect="selectedTab === '' ? 'dark' : 'plain'"
              style="cursor:pointer;border-radius:20px"
              @click="handleTabChange('')"
            >{{ $t('community.all') }}</el-tag>
            <el-tag
              v-for="cat in categories"
              :key="cat.id"
              :type="selectedTab === cat.id ? 'primary' : 'info'"
              :effect="selectedTab === cat.id ? 'dark' : 'plain'"
              style="cursor:pointer;border-radius:20px"
              @click="handleTabChange(cat.id)"
            >{{ cat.name }}</el-tag>
          </div>

          <el-skeleton :loading="loading" :count="3" animated>
            <template #default>
              <el-empty v-if="posts.length === 0" :description="$t('message.empty.noPosts')">
                <el-button type="primary" @click="goToCreate">{{ $t('community.publish') }}</el-button>
              </el-empty>
              <div v-else class="wf-flex" style="flex-direction:column;gap:12px">
                <div
                  v-for="post in posts"
                  :key="post.id"
                  class="wf-card wf-card-hover"
                  style="cursor:pointer;padding:18px 20px"
                  @click="goToDetail(post.id)"
                >
                  <div class="wf-flex-center wf-gap-sm wf-mb-sm wf-flex-wrap">
                    <span v-if="post.type === 'NOTICE'" class="wf-tag wf-tag-warning" style="font-size:11px">{{ $t('post.announcement') }}</span>
                    <span v-if="post.isPinned" class="wf-tag wf-tag-danger" style="font-size:11px">{{ $t('post.pinned') }}</span>
                    <span v-if="post.category" class="wf-tag wf-tag-primary">{{ post.category.name }}</span>
                    <span v-if="post.userBrief" class="post-author" @click.stop="goToUser(post.userBrief.uid)">
                      <img :src="post.userBrief.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" class="author-avatar">
                      {{ post.userBrief.displayName }}
                    </span>
                    <span class="wf-text-xs wf-text-muted">{{ post.publishedAt ? new Date(post.publishedAt).toLocaleDateString() : '' }}</span>
                  </div>
                  <h3 class="post-title">{{ post.title }}</h3>
                  <p v-if="post.summary" class="post-excerpt">{{ post.summary }}</p>
                  <div class="wf-divider" style="margin:12px 0" />
                  <div class="wf-flex-center wf-gap-lg wf-text-xs wf-text-muted">
                    <span>👁 {{ post.viewCount }}</span>
                    <span>❤️ {{ post.likeCount }}</span>
                    <span>💬 {{ post.commentCount }}</span>
                    <span v-if="post.tags?.length" class="wf-flex-center wf-gap-sm wf-ml-auto wf-flex-wrap">
                      <el-tag v-for="tag in post.tags" :key="tag.id" size="small" type="primary" effect="plain">{{ tag.name }}</el-tag>
                    </span>
                  </div>
                </div>
              </div>
              <div v-if="pagination.totalPages > 1" class="wf-pagination">
                <el-pagination
                  :current-page="pagination.number + 1"
                  :page-size="pagination.size"
                  :total="pagination.totalElements"
                  layout="prev, pager, next"
                  background
                  @current-change="handlePageChange"
                />
              </div>
            </template>
          </el-skeleton>
        </div>

        <div class="wf-flex" style="flex-direction:column;gap:20px">
          <el-card shadow="never" class="wf-card">
            <div style="padding:20px;text-align:center">
              <div style="font-size:16px;font-weight:600;color:#1e293b;margin-bottom:8px">{{ $t('community.welcome') }}</div>
              <p style="font-size:13px;color:#64748b;margin-bottom:16px">{{ $t('community.welcomeDesc') }}</p>
              <el-button type="primary" style="width:100%" @click="goToCreate">{{ $t('community.publish') }}</el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="wf-card">
            <template #header>
              <span class="wf-flex-center wf-gap-sm" style="font-size:15px;font-weight:600">📂 {{ $t('post.category') }}</span>
            </template>
            <div class="category-list">
              <div
                v-for="cat in categories"
                :key="cat.id"
                class="category-item"
                @click="handleTabChange(cat.id)"
              >
                <div>
                  <div class="category-name">{{ cat.name }}</div>
                  <div class="category-count">{{ (cat as any).usageCount || 0 }} 帖子</div>
                </div>
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.post-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.5;
  margin: 0 0 6px;
}
.post-excerpt {
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 0;
}
.post-author {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #64748b;
}
.post-author:hover { color: #3b82f6; }
.author-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
}
.category-list { padding: 4px 0; }
.category-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; cursor: pointer; border-radius: 8px; transition: background 0.2s;
}
.category-item:hover { background: #f8fafc; }
.category-name { font-size: 14px; font-weight: 500; color: #1e293b; }
.category-count { font-size: 12px; color: #94a3b8; margin-top: 1px; }
</style>
