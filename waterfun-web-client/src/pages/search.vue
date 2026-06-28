<script setup lang="ts">
import { usePostStore } from '~/stores/postStore'
import { storeToRefs } from 'pinia'
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import HeaderNavMenu from '~/components/HeaderNavMenu.vue'
import { getTagColor } from '@waterfun/web-core/src/tagColor'
import { formatRelativeTime } from '~/utils/date'

const postStore = usePostStore()
const { posts, pagination, loading } = storeToRefs(postStore)
const router = useRouter()
const route = useRoute()

const keyword = ref(route.query.q as string || '')

const fetchSearch = (page: number = 1) => {
  if (!keyword.value) return
  postStore.fetchPostList({ keyword: keyword.value, page, size: 12 })
}

const currentPage = computed({
  get: () => pagination.value.number + 1,
  set: (page: number) => {
    router.replace({ query: { ...route.query, page: String(page) } })
    fetchSearch(page)
  }
})

const goToDetail = (id: string) => router.push(`/post/${id}`)

onMounted(() => {
  keyword.value = route.query.q as string || ''
  fetchSearch()
})

watch(() => route.query.q, (newQ) => {
  keyword.value = newQ as string || ''
  fetchSearch()
})
</script>

<template>
  <div>
    <HeaderNavMenu />
    <div style="max-width:960px;margin:0 auto;padding:24px">
      <div style="margin-bottom:20px">
        <h2 style="font-size:20px;font-weight:600;color:#1e293b;margin:0 0 4px">
          搜索"{{ keyword }}"
        </h2>
        <span style="font-size:14px;color:#94a3b8">
          共找到 {{ pagination.totalElements }} 条结果
        </span>
      </div>

      <el-skeleton :loading="loading" :count="3" animated>
        <template #default>
          <el-empty v-if="posts.length === 0 && !loading"
            description="没有找到相关结果，试试其他关键词"
          >
            <el-button type="primary" @click="router.push('/')">返回首页</el-button>
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
    </div>
  </div>
</template>

<style scoped>
.post-footer {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px solid var(--wf-border);
}
.post-stat {
  font-size: 13px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
