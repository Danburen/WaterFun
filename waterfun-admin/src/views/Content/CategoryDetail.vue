<script setup lang="ts">
import { formatDate } from "@waterfun/web-core/src/timer";
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import { getCategory, getCategoryOptions, type CategoryResp } from "~/api/category";
import { getUserOptions } from "~/api/user";
import CategoryCreateDialog from "~/views/Content/components/CategoryCreateDialog.vue";

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const categoryId = computed(() => Number(route.params.id));
const loading = ref(false);
const categoryDetail = ref<CategoryResp | null>(null);
const editDialogVisible = ref(false);

const categoryOptions = ref<OptionResItem<number>[]>([]);
const userOptions = ref<OptionResItem<string>[]>([]);

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

const fetchOptions = async () => {
  try {
    const [categoryRes, userRes] = await Promise.all([getCategoryOptions(), getUserOptions()]);
    categoryOptions.value = categoryRes.data || [];
    userOptions.value = userRes.data || [];
  } catch (e) {
    console.error(e);
    ElMessage.error(t("error.fetch"));
  }
};

const fetchDetail = async () => {
  if (Number.isNaN(categoryId.value)) {
    ElMessage.error(t("content.category.error.invalidId"));
    router.back();
    return;
  }

  loading.value = true;
  try {
    const res = await getCategory(categoryId.value);
    categoryDetail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("content.category.error.fetchDetail"));
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
  <div class="category-detail" v-loading="loading">
    <CardContainer title="content.category.detail">
      <template #header-right>
        <el-button text @click="router.back()">{{ t("common.action.back") }}</el-button>
        <el-button type="primary" plain @click="editDialogVisible = true">{{ t("common.action.edit") }}</el-button>
      </template>

      <el-descriptions v-if="categoryDetail" :column="2" border>
        <el-descriptions-item label="ID">{{ categoryDetail.id }}</el-descriptions-item>
        <el-descriptions-item :label="t('content.category.field.name')">{{ categoryDetail.name }}</el-descriptions-item>
        <el-descriptions-item :label="t('content.category.field.slug')">{{ categoryDetail.slug || t('common.none.title') }}</el-descriptions-item>
        <el-descriptions-item :label="t('content.category.field.parentId')">
          <span v-if="categoryDetail.parentId">
            {{ categoryDetail.parentId }} ({{ categoryNameMap.get(categoryDetail.parentId) || t('common.none.title') }})
          </span>
          <span v-else>{{ t('common.none.title') }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.category.field.sortOrder')">
          {{ categoryDetail.sortOrder ?? t('common.none.title') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.category.field.creatorId')">
          <span v-if="categoryDetail.creatorId">
            {{ categoryDetail.creatorId }} ({{ userNameMap.get(categoryDetail.creatorId) || t('common.none.title') }})
          </span>
          <span v-else>{{ t('common.none.title') }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.category.field.isActive')">
          <el-tag size="small" :type="categoryDetail.isActive ? 'success' : 'info'">
            {{ categoryDetail.isActive ? t('common.boolean.yes') : t('common.boolean.no') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('content.category.field.description')">
          {{ categoryDetail.description || t('common.none.title') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.time.create')">{{ formatDate(categoryDetail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.time.update')">{{ formatDate(categoryDetail.updateAt) }}</el-descriptions-item>
      </el-descriptions>
    </CardContainer>

    <CategoryCreateDialog
      v-model="editDialogVisible"
      mode="edit"
      :category-id="categoryId"
      @success="handleEditSuccess"
    />
  </div>
</template>
