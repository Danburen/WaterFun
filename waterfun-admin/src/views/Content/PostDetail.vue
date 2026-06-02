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

const categoryNameMap = computed(() => {
  const map = new Map<number, string>();
  categoryOptions.value.forEach((item) => map.set(item.id, item.name));
  return map;
});

const userNameMap = computed(() => {
  const map = new Map<string, string>();
  userOptions.value.forEach((item) => map.set(item.id, item.name));
  return map;
});

const tagNameMap = computed(() => {
  const map = new Map<number, string>();
  tagOptions.value.forEach((item) => map.set(item.id, item.name));
  return map;
});

const visibilityLabelMap: Record<string, string> = {
  PUBLIC: "公开",
  PRIVATE: "私密",
  FANS_ONLY: "粉丝可见",
};

const fetchOptions = async () => {
  try {
    const [categoryRes, userRes, tagRes] = await Promise.all([
      getCategoryOptions(),
      getUserOptions(),
      getTagOptions(),
    ]);
    categoryOptions.value = categoryRes.data || [];
    userOptions.value = userRes.data || [];
    tagOptions.value = tagRes.data || [];
  } catch (e) {
    console.error(e);
    ElMessage.error('获取数据失败');
  }
};

const fetchDetail = async () => {
  if (!/^\d+$/.test(postId.value)) {
    ElMessage.error('无效的文章ID');
    router.back();
    return;
  }

  loading.value = true;
  try {
    const res = await getPostById(postId.value);
    postDetail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取文章详情失败');
  } finally {
    loading.value = false;
  }
};

const handleEditSuccess = async () => {
  await fetchDetail();
};

onMounted(async () => {
  await Promise.all([fetchOptions(), fetchDetail()]);
});
</script>

<template>
  <div
    v-loading="loading"
    class="post-detail"
  >
    <CardContainer title="文章详情">
      <template #header-right>
        <el-button
          text
          @click="router.back()"
        >
          返回
        </el-button>
        <el-button
          type="primary"
          plain
          @click="editDialogVisible = true"
        >
          编辑
        </el-button>
      </template>

      <el-descriptions
        v-if="postDetail"
        :column="2"
        border
      >
        <el-descriptions-item label="ID">
          {{ postDetail.id }}
        </el-descriptions-item>
        <el-descriptions-item label="标题">
          {{ postDetail.title }}
        </el-descriptions-item>
        <el-descriptions-item label="副标题">
          {{ postDetail.subtitle || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="唯一标识符">
          {{ postDetail.slug || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ ({ draft: '草稿', pending: '待审核', published: '已发布', rejected: '已拒绝', archived: '已归档' })[postDetail.status?.toLowerCase()] || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="可见性">
          {{ postDetail.visibility ? (visibilityLabelMap[postDetail.visibility] || '无') : '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="分类ID">
          <span v-if="postDetail.categoryId">
            {{ postDetail.categoryId }} ({{ categoryNameMap.get(postDetail.categoryId) || '无' }})
          </span>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="作者ID">
          <span v-if="postDetail.authorId">
            {{ postDetail.authorId }} ({{ userNameMap.get(postDetail.authorId) || '无' }})
          </span>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-space wrap>
            <el-tag
              v-for="tagId in postDetail.tagIds || []"
              :key="tagId"
              size="small"
            >
              {{ tagId }} ({{ tagNameMap.get(tagId) || '无' }})
            </el-tag>
            <span v-if="!postDetail.tagIds || postDetail.tagIds.length === 0">无</span>
          </el-space>
        </el-descriptions-item>
        <el-descriptions-item label="摘要">
          {{ postDetail.summary || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="封面图">
          {{ postDetail.coverImg || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDate(postDetail.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatDate(postDetail.updatedAt) }}
        </el-descriptions-item>
        <el-descriptions-item
          label="内容"
          :span="2"
        >
          <pre class="content-block">{{ postDetail.content || '无' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </CardContainer>

    <PostCreateDialog
      v-model="editDialogVisible"
      mode="edit"
      :post-id="postId"
      @success="handleEditSuccess"
    />
  </div>
</template>

<style scoped>
.content-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
