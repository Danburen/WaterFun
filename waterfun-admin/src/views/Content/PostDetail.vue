<script setup lang="ts">
import { formatDate } from "@waterfun/web-core/src/timer";
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { useRoute, useRouter } from "vue-router";
import { getCategoryOptions } from "~/api/category";
import { getPostById, type PostResp } from "~/api/post";
import { getTagOptions } from "~/api/tag";
import { getUserOptions } from "~/api/user";
import { ElMessage } from "element-plus";
import MarkdownIt from 'markdown-it'

const route = useRoute();
const router = useRouter();

const postId = computed(() => String(route.params.id ?? ""));
const loading = ref(false);
const postDetail = ref<PostResp | null>(null);
const activeTab = ref<'published' | 'edited'>('published');

const md = new MarkdownIt({ html: true, breaks: true, linkify: true });
const renderedPublishedHtml = ref('');
const renderedEditedHtml = ref('');

const categoryOptions = ref<OptionResItem<number>[]>([]);
const userOptions = ref<OptionResItem<string>[]>([]);
const tagOptions = ref<OptionResItem<number>[]>([]);

const categoryNameMap = computed(() => { const m = new Map<number, string>(); categoryOptions.value.forEach(i => m.set(i.id, i.name)); return m; });
const userNameMap = computed(() => { const m = new Map<string, string>(); userOptions.value.forEach(i => m.set(i.id, i.name)); return m; });
const tagNameMap = computed(() => { const m = new Map<number, string>(); tagOptions.value.forEach(i => m.set(i.id, i.name)); return m; });

const visibilityLabelMap: Record<string, string> = { PUBLIC: "公开", PRIVATE: "私密", FANS_ONLY: "粉丝可见" };
const statusBadgeCls: Record<string, string> = { DRAFT: "badge-gray", PENDING: "badge-yellow", PUBLISHED: "badge-green", REJECTED: "badge-red", ARCHIVED: "badge-gray" };
const statusLabel: Record<string, string> = { DRAFT: "草稿", PENDING: "待审核", PUBLISHED: "已发布", REJECTED: "已拒绝", ARCHIVED: "已归档" };
const typeLabel: Record<string, string> = { COMMON: "普通帖子", NOTICE: "公告" };

const fetchOptions = async () => {
  try {
    const [cr, ur, tr] = await Promise.all([getCategoryOptions(), getUserOptions(), getTagOptions()]);
    categoryOptions.value = cr.data || [];
    userOptions.value = ur.data || [];
    tagOptions.value = tr.data || [];
  } catch { ElMessage.error('获取数据失败'); }
};

const renderContent = () => {
  const d = postDetail.value;
  if (!d) return;
  if (d.contentHtml) {
    renderedPublishedHtml.value = md.render(d.contentHtml);
  } else if (d.content) {
    renderedPublishedHtml.value = md.render(d.content);
  }
  if (d.editedContentHtml) {
    renderedEditedHtml.value = md.render(d.editedContentHtml);
  } else if (d.editedContent) {
    renderedEditedHtml.value = md.render(d.editedContent);
  }
};

const fetchDetail = async () => {
  if (!/^\d+$/.test(postId.value)) { ElMessage.error('无效的文章ID'); router.back(); return; }
  loading.value = true;
  try {
    const res = await getPostById(postId.value);
    postDetail.value = res.data;
    renderContent();
  }
  catch { ElMessage.error('获取文章详情失败'); }
  finally { loading.value = false; }
};

const handleEdit = () => {
  router.push({ name: "contentPostEditor", query: { id: postId.value } });
};

const hasEditedContent = computed(() => {
  const d = postDetail.value;
  return !!(d?.editedTitle || d?.editedContent || d?.editedSummary);
});

onMounted(async () => { await Promise.all([fetchOptions(), fetchDetail()]); });
</script>

<template>
  <CardContainer title="文章详情">
    <template #header-right>
      <button class="btn" @click="router.back()">返回</button>
      <button class="btn btn-primary" @click="handleEdit">编辑</button>
    </template>

    <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>

    <div v-else-if="postDetail" class="post-detail">
      <!-- Meta header -->
      <div class="post-meta-header">
        <div class="post-meta-left">
          <h1 class="post-title">{{ postDetail.title || '无标题' }}</h1>
          <p v-if="postDetail.subtitle" class="post-subtitle">{{ postDetail.subtitle }}</p>
        </div>
        <div class="post-meta-right">
          <span :class="['badge', statusBadgeCls[postDetail.status || ''] || 'badge-gray']">{{ statusLabel[postDetail.status || ''] || '未知' }}</span>
          <span v-if="postDetail.type === 'NOTICE'" class="badge badge-yellow">公告</span>
          <span v-if="postDetail.isPinned" class="badge badge-blue">置顶</span>
        </div>
      </div>

      <!-- Cover image -->
      <div v-if="postDetail.coverImage?.url" class="post-cover-wrap">
        <img :src="postDetail.coverImage.url" alt="封面图" class="post-cover-img" />
      </div>

      <!-- Info table -->
      <table class="detail-table">
        <tr>
          <td class="label">ID</td>
          <td class="value">{{ postDetail.id }}</td>
          <td class="label">唯一标识符</td>
          <td class="value">{{ postDetail.slug || '无' }}</td>
        </tr>
        <tr>
          <td class="label">可见性</td>
          <td class="value">{{ postDetail.visibility ? (visibilityLabelMap[postDetail.visibility] || '无') : '无' }}</td>
          <td class="label">封面图 UUID</td>
          <td class="value">{{ postDetail.coverImg || postDetail.editedCoverImg || '无' }}</td>
        </tr>
        <tr>
          <td class="label">分类</td>
          <td class="value">{{ postDetail.categoryId != null ? `${postDetail.categoryId} (${categoryNameMap.get(Number(postDetail.categoryId)) || '无'})` : '全局公告' }}</td>
          <td class="label">编辑中分类</td>
          <td class="value">{{ postDetail.editedCategoryId != null ? `${postDetail.editedCategoryId} (${categoryNameMap.get(Number(postDetail.editedCategoryId)) || '无'})` : '无' }}</td>
        </tr>
        <tr>
          <td class="label">作者</td>
          <td class="value">{{ postDetail.authorId ? `${postDetail.authorId} (${userNameMap.get(String(postDetail.authorId)) || '无'})` : '系统' }}</td>
          <td class="label">帖子类型</td>
          <td class="value">{{ typeLabel[postDetail.type || ''] || '无' }}</td>
        </tr>
        <tr>
          <td class="label">标签</td>
          <td class="value" colspan="3">
            <span v-if="postDetail.tagIds?.length">
              <span v-for="(tagId, idx) in postDetail.tagIds" :key="tagId" class="badge badge-blue" style="margin-right: 4px;">{{ tagId }} ({{ tagNameMap.get(Number(tagId)) || '无' }})</span>
            </span>
            <span v-else>无</span>
            <template v-if="postDetail.editedTagIds?.length">
              <span class="label-hint">（编辑中：</span>
              <span v-for="(tagId, idx) in postDetail.editedTagIds" :key="'e'+tagId" class="badge badge-yellow" style="margin-right: 4px;">{{ tagId }} ({{ tagNameMap.get(Number(tagId)) || '无' }})</span>
              <span class="label-hint">）</span>
            </template>
          </td>
        </tr>
        <tr>
          <td class="label">摘要</td>
          <td class="value" colspan="3">{{ postDetail.summary || '无' }}</td>
        </tr>
        <tr v-if="postDetail.editedSummary">
          <td class="label">编辑中摘要</td>
          <td class="value" colspan="3" style="color:#d97706;">{{ postDetail.editedSummary }}</td>
        </tr>
        <tr>
          <td class="label">统计</td>
          <td class="value" colspan="3">
            <span class="stat-item"><i class="fa-regular fa-eye"></i> {{ postDetail.viewCount || 0 }}</span>
            <span class="stat-item"><i class="fa-regular fa-thumbs-up"></i> {{ postDetail.likeCount || 0 }}</span>
            <span class="stat-item"><i class="fa-regular fa-comment"></i> {{ postDetail.commentCount || 0 }}</span>
            <span class="stat-item"><i class="fa-regular fa-star"></i> {{ postDetail.collectCount || 0 }}</span>
          </td>
        </tr>
        <tr>
          <td class="label">创建时间</td>
          <td class="value">{{ formatDate(postDetail.createdAt) }}</td>
          <td class="label">发布时间</td>
          <td class="value">{{ formatDate(postDetail.publishedAt) || '未发布' }}</td>
        </tr>
        <tr>
          <td class="label">更新时间</td>
          <td class="value" colspan="3">{{ formatDate(postDetail.updatedAt) }}</td>
        </tr>
      </table>

      <!-- Content tabs -->
      <div v-if="postDetail.content || postDetail.editedContent" class="content-section">
        <div class="content-tabs" v-if="hasEditedContent">
          <button :class="['tab-btn', { active: activeTab === 'published' }]" @click="activeTab = 'published'">已发布内容</button>
          <button :class="['tab-btn', { active: activeTab === 'edited' }]" @click="activeTab = 'edited'">编辑中内容</button>
        </div>
        <div class="content-body wf-markdown" v-html="activeTab === 'published' ? renderedPublishedHtml : renderedEditedHtml"></div>
        <div v-if="!postDetail.content && !postDetail.editedContent" class="content-empty">暂无内容</div>
      </div>
      <div v-else class="content-empty">暂无内容</div>
    </div>
  </CardContainer>
</template>

<style scoped>
.post-detail {
  padding: 4px 0;
}
.post-meta-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color, #e2e8f0);
}
.post-meta-left {
  flex: 1;
}
.post-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary, #1e293b);
  margin: 0 0 4px;
  line-height: 1.4;
}
.post-subtitle {
  font-size: 14px;
  color: var(--text-muted, #64748b);
  margin: 0;
}
.post-meta-right {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  flex-wrap: wrap;
  align-items: center;
}
.post-cover-wrap {
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  max-height: 300px;
}
.post-cover-img {
  width: 100%;
  height: auto;
  max-height: 300px;
  object-fit: cover;
  display: block;
}
.detail-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
  font-size: 13px;
}
.detail-table td {
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-color, #f1f5f9);
  vertical-align: top;
}
.detail-table .label {
  width: 110px;
  font-weight: 600;
  color: var(--text-secondary, #475569);
  white-space: nowrap;
  background: var(--bg-subtle, #f8fafc);
}
.detail-table .value {
  color: var(--text-primary, #1e293b);
}
.label-hint {
  font-size: 12px;
  color: var(--text-muted, #94a3b8);
}
.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 16px;
  color: var(--text-muted, #64748b);
  font-size: 13px;
}
.content-section {
  border-top: 1px solid var(--border-color, #e2e8f0);
  padding-top: 16px;
}
.content-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 16px;
  border-bottom: 2px solid var(--border-color, #e2e8f0);
}
.tab-btn {
  padding: 8px 20px;
  border: none;
  background: none;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted, #64748b);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}
.tab-btn:hover {
  color: var(--text-primary, #1e293b);
}
.tab-btn.active {
  color: var(--primary, #3b82f6);
  border-bottom-color: var(--primary, #3b82f6);
}
.content-body {
  padding: 16px 0;
  min-height: 100px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-primary, #1e293b);
}
.content-body :deep(img) {
  max-width: 100%;
  border-radius: 6px;
  margin: 8px 0;
}
.content-empty {
  text-align: center;
  padding: 48px 0;
  color: var(--text-muted, #94a3b8);
  font-size: 14px;
}
</style>
