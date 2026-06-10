<script setup lang="ts">
import { useRoute } from 'vue-router'
import { usePostStore } from '~/stores/postStore'
import { useAuthStore } from '~/stores/authStore'
import { storeToRefs } from 'pinia'
import { fetchUserPublicProfile, fetchUserPublicCard, toggleFollowUser, fetchFollowers, fetchFollowings } from '~/api/publicUserApi'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { UserPublicProfileResp, UserPublicCardResp, PageUserBrief } from '~/api/publicUserApi'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const postStore = usePostStore()
const authStore = useAuthStore()
const { posts, loading: postsLoading } = storeToRefs(postStore)

const uid = computed(() => Number(route.params.uid))

const profile = ref<UserPublicProfileResp | null>(null)
const card = ref<UserPublicCardResp | null>(null)
const profileLoading = ref(true)
const profileError = ref(false)
const followed = ref(false)
const followLoading = ref(false)

const activeTab = ref<'posts' | 'followers' | 'followings'>('posts')
const followers = ref<PageUserBrief | null>(null)
const followings = ref<PageUserBrief | null>(null)
const relationLoading = ref(false)

const currentUserId = computed(() => {
  try { return useUserInfoStore().userInfo.uid } catch { return 0 }
})
const isSelf = computed(() => currentUserId.value === uid.value)

const loadProfile = async () => {
  profileLoading.value = true
  profileError.value = false
  try {
    const [profileRes, cardRes] = await Promise.all([
      fetchUserPublicProfile(uid.value),
      fetchUserPublicCard(uid.value),
    ])
    profile.value = profileRes.data as unknown as UserPublicProfileResp
    card.value = cardRes.data as unknown as UserPublicCardResp
  } catch {
    profileError.value = true
  } finally {
    profileLoading.value = false
  }
}

const loadPosts = async () => {
  try {
    await postStore.fetchPostList({ page: 1, size: 20, authorId: uid.value })
  } catch { /* ignore */ }
}

const handleFollow = async () => {
  if (!authStore.isAccess) { router.push('/login'); return }
  followLoading.value = true
  try {
    await toggleFollowUser(uid.value)
    followed.value = !followed.value
    if (card.value) {
      card.value.followers += followed.value ? 1 : -1
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
    followers.value = res.data as unknown as PageUserBrief
  } catch { /* ignore */ }
  finally { relationLoading.value = false }
}

const loadFollowings = async () => {
  if (activeTab.value !== 'followings') return
  relationLoading.value = true
  try {
    const res = await fetchFollowings(uid.value)
    followings.value = res.data as unknown as PageUserBrief
  } catch { /* ignore */ }
  finally { relationLoading.value = false }
}

const switchTab = (tab: 'posts' | 'followers' | 'followings') => {
  activeTab.value = tab
  if (tab === 'followers') loadFollowers()
  if (tab === 'followings') loadFollowings()
}

const goToUserPage = (targetUid: number) => {
  router.push(`/User/${targetUid}`)
}

onMounted(() => {
  loadProfile()
  loadPosts()
})

import { useUserInfoStore } from '~/stores/userInfoStore'
</script>

<template>
  <div>
    <HeaderNavMenu />
    <div class="profile-page">
      <div v-if="profileLoading" class="wf-state-wrap">
        <div class="wf-state-text">{{ $t('general.loading') }}</div>
      </div>
      <div v-else-if="profileError" class="wf-state-wrap">
        <div class="wf-state-text wf-state-error">{{ $t('general.userNotExist') }}</div>
        <button class="wf-btn wf-btn-primary" @click="router.push('/')">{{ $t('general.backHome') }}</button>
      </div>
      <template v-else-if="profile">
        <div class="wf-card profile-header">
          <div class="wf-flex-center wf-gap-lg" style="flex-wrap:wrap">
            <div class="wf-avatar-wrap wf-avatar-lg">
              <img :src="profile.userBrief?.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" alt="avatar">
            </div>
            <div style="flex:1;min-width:200px">
              <h1 class="profile-name">{{ profile.userBrief?.displayName || $t('general.anonymous') }}</h1>
              <div class="wf-flex-center wf-gap-sm wf-flex-wrap wf-mt-sm">
                <span v-if="profile.gender" class="wf-tag wf-tag-info">{{ profile.gender === 'MALE' ? '♂ 男' : profile.gender === 'FEMALE' ? '♀ 女' : profile.gender }}</span>
                <span v-if="profile.bio" class="wf-text-secondary wf-text-sm">{{ profile.bio }}</span>
                <span v-if="profile.residence" class="wf-text-muted wf-text-xs">📍 {{ profile.residence }}</span>
              </div>
              <div class="wf-flex-center wf-gap-lg wf-mt-sm">
                <div class="profile-stat" @click="switchTab('followers')">
                  <strong>{{ profile.followers }}</strong>
                  <span>{{ $t('user.profile.followers') }}</span>
                </div>
                <div class="profile-stat" @click="switchTab('followings')">
                  <strong>{{ profile.followings }}</strong>
                  <span>{{ $t('user.profile.followings') }}</span>
                </div>
                <div class="profile-stat">
                  <strong>{{ profile.postCount }}</strong>
                  <span>{{ $t('user.profile.posts') }}</span>
                </div>
                <div class="profile-stat">
                  <strong>{{ profile.likeCount }}</strong>
                  <span>{{ $t('user.profile.likes') }}</span>
                </div>
              </div>
            </div>
            <div v-if="!isSelf">
              <button
                :class="['wf-btn', followed ? 'wf-btn-text' : 'wf-btn-primary']"
                :loading="followLoading"
                @click="handleFollow"
              >
                {{ followed ? $t('user.profile.following') : $t('user.profile.follow') }}
              </button>
            </div>
          </div>
        </div>

        <div class="wf-card wf-mt-md">
          <div class="wf-card-header">
            <div class="wf-flex-center wf-gap-md">
              <button
                v-for="tab in [{ key: 'posts', label: $t('user.profile.posts') }, { key: 'followers', label: $t('user.profile.followers') }, { key: 'followings', label: $t('user.profile.followings') }]"
                :key="tab.key"
                :class="['wf-btn wf-btn-sm', activeTab === tab.key ? 'wf-btn-primary' : 'wf-btn-text']"
                @click="switchTab(tab.key as any)"
              >
                {{ tab.label }}
              </button>
            </div>
          </div>

          <div class="wf-card-body">
            <div v-if="activeTab === 'posts'">
              <div v-if="postsLoading" class="wf-state-wrap"><div class="wf-state-text">{{ $t('general.loading') }}</div></div>
              <div v-else-if="posts.length === 0"><el-empty :description="$t('message.empty.noPosts')" :image-size="80" /></div>
              <div v-else class="post-list">
                <div
                  v-for="post in posts"
                  :key="post.id"
                  class="post-card"
                  @click="router.push(`/post/${post.id}`)"
                >
                  <h3 class="post-title">{{ post.title }}</h3>
                  <p v-if="post.summary" class="post-excerpt">{{ post.summary }}</p>
                  <div class="wf-flex-center wf-gap-md wf-text-xs wf-text-muted wf-mt-sm">
                    <span>👁 {{ post.viewCount }}</span>
                    <span>❤️ {{ post.likeCount }}</span>
                    <span>💬 {{ post.commentCount }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="activeTab === 'followers'">
              <div v-if="relationLoading" class="wf-state-wrap"><div class="wf-state-text">{{ $t('general.loading') }}</div></div>
              <div v-else-if="!followers?.content?.length"><el-empty :description="$t('message.empty.noFollowers')" :image-size="80" /></div>
              <div v-else class="relation-list">
                <div v-for="user in followers!.content" :key="user.uid" class="relation-item" @click="goToUserPage(user.uid)">
                  <div class="wf-avatar-wrap wf-avatar-sm">
                    <img :src="user.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" alt="avatar">
                  </div>
                  <span class="relation-name">{{ user.displayName }}</span>
                </div>
              </div>
            </div>

            <div v-if="activeTab === 'followings'">
              <div v-if="relationLoading" class="wf-state-wrap"><div class="wf-state-text">{{ $t('general.loading') }}</div></div>
              <div v-else-if="!followings?.content?.length"><el-empty :description="$t('message.empty.noFollowings')" :image-size="80" /></div>
              <div v-else class="relation-list">
                <div v-for="user in followings!.content" :key="user.uid" class="relation-item" @click="goToUserPage(user.uid)">
                  <div class="wf-avatar-wrap wf-avatar-sm">
                    <img :src="user.avatar?.url || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" alt="avatar">
                  </div>
                  <span class="relation-name">{{ user.displayName }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.profile-page { max-width: 800px; margin: 0 auto; padding: 24px; }
.profile-header { padding: 28px 32px; }
.profile-name { font-size: 22px; font-weight: 700; color: #1e293b; margin: 0; }
.profile-stat {
  display: flex; align-items: center; gap: 4px; cursor: pointer;
  font-size: 13px; color: #64748b;
}
.profile-stat strong { font-size: 16px; color: #1e293b; }
.profile-stat:hover strong { color: #3b82f6; }

.post-list { display: flex; flex-direction: column; gap: 12px; }
.post-card {
  padding: 16px; border: 1px solid #f1f5f9; border-radius: 8px; cursor: pointer; transition: all 0.2s;
}
.post-card:hover { background: #f8fafc; }
.post-title { font-size: 15px; font-weight: 600; color: #1e293b; margin: 0 0 4px; }
.post-excerpt { font-size: 13px; color: #64748b; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.relation-list { display: flex; flex-direction: column; }
.relation-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; cursor: pointer; border-radius: 8px; transition: background 0.2s;
}
.relation-item:hover { background: #f8fafc; }
.relation-name { font-size: 14px; font-weight: 500; color: #1e293b; }

@media (max-width: 768px) { .profile-page { padding: 16px; } .profile-header { padding: 20px; } }
</style>
