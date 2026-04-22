<script setup lang="ts">
import { formatDate } from "@waterfun/web-core/src/timer";
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import { getCategoryOptions } from "~/api/category";
import { getPostById, type PostResp } from "~/api/post";
import { getTagOptions } from "~/api/tag";
import { getUserOptions } from "~/api/user";
import PostCreateDialog from "~/views/Content/components/PostCreateDialog.vue";

const { t } = useI18n();
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
  PUBLIC: "content.post.visibility.public",
  PRIVATE: "content.post.visibility.private",
  FANS_ONLY: "content.post.visibility.fansOnly",
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
    ElMessage.error(t("error.fetch"));
  }
};

const fetchDetail = async () => {
  if (!/^\d+$/.test(postId.value)) {
    ElMessage.error(t("content.post.error.invalidId"));
    router.back();
    return;
  }

  loading.value = true;
  try {
    const res = await getPostById(postId.value);
    postDetail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("content.post.error.fetchDetail"));
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
  <div class="post-detail" v-loading="loading">
    <CardContainer title="content.post.detail">
      <template #header-right>
        <el-button text @click="router.back()">{{ t("common.action.back") }}</el-button>
        <el-button type="primary" plain @click="editDialogVisible = true">{{ t("common.action.edit") }}</el-button>
      </template>

      <el-descriptions v-if="postDetail" :column="2" border>
        <el-descriptions-item label="ID">{{ postDetail.id }}</el-descriptions-item>
        <el-descriptions-item :label="t('content.post.field.title')">{{ postDetail.title }}</el-descriptions-item>
        <el-descriptions-item :label="t('content.post.field.subtitle')">
          {{ postDetail.subtitle || t('common.none.title') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.post.field.slug')">{{ postDetail.slug || t('common.none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('content.post.field.status')">
          {{ postDetail.status ? t(`content.post.status.${postDetail.status.toLowerCase()}`) : t('common.none.title') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.post.field.visibility')">
          {{ postDetail.visibility ? t(visibilityLabelMap[postDetail.visibility] || 'common.none.title') : t('common.none.title') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.post.field.categoryId')">
          <span v-if="postDetail.categoryId">
            {{ postDetail.categoryId }} ({{ categoryNameMap.get(postDetail.categoryId) || t('common.none.title') }})
          </span>
          <span v-else>{{ t('common.none.title') }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.post.field.authorId')">
          <span v-if="postDetail.authorId">
            {{ postDetail.authorId }} ({{ userNameMap.get(postDetail.authorId) || t('common.none.title') }})
          </span>
          <span v-else>{{ t('common.none.title') }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.post.field.tagIds')">
          <el-space wrap>
            <el-tag v-for="tagId in postDetail.tagIds || []" :key="tagId" size="small">
              {{ tagId }} ({{ tagNameMap.get(tagId) || t('common.none.title') }})
            </el-tag>
            <span v-if="!postDetail.tagIds || postDetail.tagIds.length === 0">{{ t('common.none.title') }}</span>
          </el-space>
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.post.field.summary')">
          {{ postDetail.summary || t('common.none.title') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.post.field.coverImg')">
          {{ postDetail.coverImg || t('common.none.title') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.time.create')">{{ formatDate(postDetail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.time.update')">{{ formatDate(postDetail.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item :label="t('content.post.field.content')" :span="2">
          <pre class="content-block">{{ postDetail.content || t('common.none.title') }}</pre>
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
