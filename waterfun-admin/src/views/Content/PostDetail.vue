<script setup lang="ts">
import { formatDate } from "@waterfun/web-core/src/timer";
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { useRoute, useRouter } from "vue-router";
import { getCategoryOptions } from "~/api/category";
import { getPostById, type PostResp } from "~/api/post";
import { getTagOptions } from "~/api/tag";
import { getUserOptions } from "~/api/user";
import PostCreateDialog from "~/views/Content/components/PostCreateDialog.vue";
import { ElMessage } from "element-plus";

const route = useRoute();
const router = useRouter();

const postId = computed(() => String(route.params.id ?? ""));
const loading = ref(false);
const postDetail = ref<PostResp | null>(null);
const editDialogVisible = ref(false);

const categoryOptions = ref<OptionResItem<number>[]>([]);
const userOptions = ref<OptionResItem<string>[]>([]);
const tagOptions = ref<OptionResItem<number>[]>([]);

const categoryNameMap = computed(() => { const m = new Map<number, string>(); categoryOptions.value.forEach(i => m.set(i.id, i.name)); return m; });
const userNameMap = computed(() => { const m = new Map<string, string>(); userOptions.value.forEach(i => m.set(i.id, i.name)); return m; });
const tagNameMap = computed(() => { const m = new Map<number, string>(); tagOptions.value.forEach(i => m.set(i.id, i.name)); return m; });

const visibilityLabelMap: Record<string, string> = { PUBLIC: "公开", PRIVATE: "私密", FANS_ONLY: "粉丝可见" };

const fetchOptions = async () => {
  try {
    const [cr, ur, tr] = await Promise.all([getCategoryOptions(), getUserOptions(), getTagOptions()]);
    categoryOptions.value = cr.data || [];
    userOptions.value = ur.data || [];
    tagOptions.value = tr.data || [];
  } catch { ElMessage.error('获取数据失败'); }
};

const fetchDetail = async () => {
  if (!/^\d+$/.test(postId.value)) { ElMessage.error('无效的文章ID'); router.back(); return; }
  loading.value = true;
  try { const res = await getPostById(postId.value); postDetail.value = res.data; }
  catch { ElMessage.error('获取文章详情失败'); }
  finally { loading.value = false; }
};

const handleEditSuccess = async () => { await fetchDetail() };

onMounted(async () => { await Promise.all([fetchOptions(), fetchDetail()]); });
</script>

<template>
  <CardContainer title="文章详情">
    <template #header-right>
      <button class="btn" @click="router.back()">返回</button>
      <button class="btn btn-primary" @click="editDialogVisible = true">编辑</button>
    </template>

    <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
    <table v-else-if="postDetail" class="detail-table">
      <tr>
        <td class="label">ID</td>
        <td class="value">{{ postDetail.id }}</td>
        <td class="label">标题</td>
        <td class="value">{{ postDetail.title }}</td>
      </tr>
      <tr>
        <td class="label">副标题</td>
        <td class="value">{{ postDetail.subtitle || '无' }}</td>
        <td class="label">唯一标识符</td>
        <td class="value">{{ postDetail.slug || '无' }}</td>
      </tr>
      <tr>
        <td class="label">状态</td>
        <td class="value">{{ ({ draft: '草稿', pending: '待审核', published: '已发布', rejected: '已拒绝', archived: '已归档' })[postDetail.status?.toLowerCase()] || '无' }}</td>
        <td class="label">可见性</td>
        <td class="value">{{ postDetail.visibility ? (visibilityLabelMap[postDetail.visibility] || '无') : '无' }}</td>
      </tr>
      <tr>
        <td class="label">帖子类型</td>
        <td class="value">{{ ({ COMMON: '普通帖子', NOTICE: '公告' })[postDetail.type || ''] || '无' }}</td>
        <td class="label">置顶</td>
        <td class="value">{{ postDetail.isPinned ? '是' : '否' }}</td>
      </tr>
      <tr>
        <td class="label">分类</td>
        <td class="value">{{ postDetail.categoryId != null ? `${postDetail.categoryId} (${categoryNameMap.get(Number(postDetail.categoryId)) || '无'})` : '无' }}</td>
        <td class="label">作者</td>
        <td class="value">{{ postDetail.authorId ? `${postDetail.authorId} (${userNameMap.get(String(postDetail.authorId)) || '无'})` : '无' }}</td>
      </tr>
      <tr>
        <td class="label">标签</td>
        <td class="value" colspan="3">
          <span v-if="postDetail.tagIds?.length">
            <span v-for="(tagId, idx) in postDetail.tagIds" :key="tagId" class="badge badge-blue" style="margin-right: 4px;">{{ tagId }} ({{ tagNameMap.get(Number(tagId)) || '无' }})</span>
          </span>
          <span v-else>无</span>
        </td>
      </tr>
      <tr>
        <td class="label">摘要</td>
        <td class="value" colspan="3">{{ postDetail.summary || '无' }}</td>
      </tr>
      <tr>
        <td class="label">封面图</td>
        <td class="value" colspan="3">{{ postDetail.coverImg || '无' }}</td>
      </tr>
      <tr>
        <td class="label">创建时间</td>
        <td class="value">{{ formatDate(postDetail.createdAt) }}</td>
        <td class="label">更新时间</td>
        <td class="value">{{ formatDate(postDetail.updatedAt) }}</td>
      </tr>
      <tr>
        <td class="label">内容</td>
        <td class="value" colspan="3"><pre class="content-block">{{ postDetail.content || '无' }}</pre></td>
      </tr>
    </table>
  </CardContainer>

  <PostCreateDialog v-model="editDialogVisible" mode="edit" :post-id="postId" @success="handleEditSuccess" />
</template>

<style scoped>
.content-block { margin: 0; white-space: pre-wrap; word-break: break-word; }
</style>
