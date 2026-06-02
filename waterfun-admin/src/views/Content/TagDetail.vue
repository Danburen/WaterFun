<script setup lang="ts">
import { formatDate } from "@waterfun/web-core/src/timer";
import type { OptionResItem } from "@waterfun/web-core/src/types/api/response";

import { useRoute, useRouter } from "vue-router";
import { getTag, type TagResp } from "~/api/tag";
import { getUserOptions } from "~/api/user";
import TagCreateDialog from "~/views/Content/components/TagCreateDialog.vue";
import { ElMessage } from "element-plus";


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
    ElMessage.error('获取数据失败');
  }
};

const fetchDetail = async () => {
  if (Number.isNaN(tagId.value)) {
    ElMessage.error('无效的标签ID');
    router.back();
    return;
  }

  loading.value = true;
  try {
    const res = await getTag(tagId.value);
    tagDetail.value = res.data;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取标签详情失败');
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
    class="tag-detail"
  >
    <CardContainer title="标签详情">
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
        v-if="tagDetail"
        :column="2"
        border
      >
        <el-descriptions-item label="ID">
          {{ tagDetail.id }}
        </el-descriptions-item>
        <el-descriptions-item label="标签名">
          {{ tagDetail.name }}
        </el-descriptions-item>
        <el-descriptions-item label="唯一标识符">
          {{ tagDetail.slug || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="使用次数">
          {{ tagDetail.usageCount ?? '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建人ID">
          <span v-if="tagDetail.creatorId">
            {{ tagDetail.creatorId }} ({{ userNameMap.get(tagDetail.creatorId) || '无' }})
          </span>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="描述">
          {{ tagDetail.description || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDate(tagDetail.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatDate(tagDetail.updateAt) }}
        </el-descriptions-item>
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
