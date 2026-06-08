<script setup lang="ts">
import { formatDate } from "@waterfun/web-core/src/timer";
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";

import { useRoute, useRouter } from "vue-router";
import { getCategory, getCategoryOptions, type CategoryResp } from "~/api/category";
import { getUserOptions } from "~/api/user";
import CategoryCreateDialog from "~/views/Content/components/CategoryCreateDialog.vue";
import { ElMessage } from "element-plus";


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
    ElMessage.error('获取数据失败');
  }
};

const fetchDetail = async () => {
  if (Number.isNaN(categoryId.value)) {
    ElMessage.error('无效的分类ID');
    router.back();
    return;
  }

  loading.value = true;
  try {
    const res = await getCategory(categoryId.value);
    categoryDetail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取分类详情失败');
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
    class="category-detail"
  >
    <CardContainer title="分类详情">
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
        v-if="categoryDetail"
        :column="2"
        border
      >
        <el-descriptions-item label="ID">
          {{ categoryDetail.id }}
        </el-descriptions-item>
        <el-descriptions-item label="分类名">
          {{ categoryDetail.name }}
        </el-descriptions-item>
        <el-descriptions-item label="唯一标识符">
          {{ categoryDetail.slug || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="父级ID">
          <span v-if="categoryDetail.parentId != null">
            {{ categoryDetail.parentId }} ({{ categoryNameMap.get(Number(categoryDetail.parentId)) || '无' }})
          </span>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="排序">
          {{ categoryDetail.sortOrder ?? '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建人ID">
          <span v-if="categoryDetail.creatorId">
            {{ categoryDetail.creatorId }} ({{ userNameMap.get(categoryDetail.creatorId) || '无' }})
          </span>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="是否启用">
          <el-tag
            size="small"
            :type="categoryDetail.isActive ? 'success' : 'info'"
          >
            {{ categoryDetail.isActive ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述">
          {{ categoryDetail.description || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDate(categoryDetail.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatDate(categoryDetail.updateAt) }}
        </el-descriptions-item>
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
