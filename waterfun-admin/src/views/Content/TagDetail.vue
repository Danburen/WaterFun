<script setup lang="ts">
import { formatDate } from "@waterfun/web-core/src/timer";
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import { getTag, type TagResp } from "~/api/tag";
import { getUserOptions } from "~/api/user";
import TagCreateDialog from "~/views/Content/components/TagCreateDialog.vue";

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const tagId = computed(() => Number(route.params.id));
const loading = ref(false);
const tagDetail = ref<TagResp | null>(null);
const editDialogVisible = ref(false);
const userOptions = ref<OptionResItem<string>[]>([]);

const userNameMap = computed(() => {
  const map = new Map<string, string>();
  userOptions.value.forEach((item) => map.set(item.id, item.name));
  return map;
});

const fetchOptions = async () => {
  try {
    const res = await getUserOptions();
    userOptions.value = res.data || [];
  } catch (e) {
    console.error(e);
    ElMessage.error(t("error.fetch"));
  }
};

const fetchDetail = async () => {
  if (Number.isNaN(tagId.value)) {
    ElMessage.error(t("content.tag.error.invalidId"));
    router.back();
    return;
  }

  loading.value = true;
  try {
    const res = await getTag(tagId.value);
    tagDetail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("content.tag.error.fetchDetail"));
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
  <div class="tag-detail" v-loading="loading">
    <CardContainer title="content.tag.detail">
      <template #header-right>
        <el-button text @click="router.back()">{{ t("common.action.back") }}</el-button>
        <el-button type="primary" plain @click="editDialogVisible = true">{{ t("common.action.edit") }}</el-button>
      </template>

      <el-descriptions v-if="tagDetail" :column="2" border>
        <el-descriptions-item label="ID">{{ tagDetail.id }}</el-descriptions-item>
        <el-descriptions-item :label="t('content.tag.field.name')">{{ tagDetail.name }}</el-descriptions-item>
        <el-descriptions-item :label="t('content.tag.field.slug')">{{ tagDetail.slug || t('common.none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('content.tag.field.usageCount')">
          {{ tagDetail.usageCount ?? t('common.none.title') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.tag.field.creatorId')">
          <span v-if="tagDetail.creatorId">
            {{ tagDetail.creatorId }} ({{ userNameMap.get(tagDetail.creatorId) || t('common.none.title') }})
          </span>
          <span v-else>{{ t('common.none.title') }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.tag.field.description')">
          {{ tagDetail.description || t('common.none.title') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.time.create')">{{ formatDate(tagDetail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.time.update')">{{ formatDate(tagDetail.updateAt) }}</el-descriptions-item>
      </el-descriptions>
    </CardContainer>

    <TagCreateDialog
      v-model="editDialogVisible"
      mode="edit"
      :tag-id="tagId"
      @success="handleEditSuccess"
    />
  </div>
</template>
