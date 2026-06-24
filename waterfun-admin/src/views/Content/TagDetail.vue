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

const userNameMap = computed(() => { const m = new Map<string, string>(); userOptions.value.forEach(i => m.set(i.id, i.name)); return m; });

const fetchOptions = async () => {
  try { const res = await getUserOptions(); userOptions.value = res.data || []; }
  catch { ElMessage.error('获取数据失败'); }
};

const fetchDetail = async () => {
  if (Number.isNaN(tagId.value)) { ElMessage.error('无效的标签ID'); router.back(); return; }
  loading.value = true;
  try { const res = await getTag(tagId.value); tagDetail.value = res.data; }
  catch { ElMessage.error('获取标签详情失败'); }
  finally { loading.value = false; }
};

const handleEditSuccess = async () => { await fetchDetail() };

onMounted(async () => { await Promise.all([fetchOptions(), fetchDetail()]); });
</script>

<template>
  <CardContainer title="标签详情">
    <template #header-right>
      <button class="btn" @click="router.back()">返回</button>
      <button class="btn btn-primary" @click="editDialogVisible = true">编辑</button>
    </template>

    <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
    <table v-else-if="tagDetail" class="detail-table">
      <tr>
        <td class="label">ID</td>
        <td class="value">{{ tagDetail.id }}</td>
        <td class="label">标签名</td>
        <td class="value">{{ tagDetail.name }}</td>
      </tr>
      <tr>
        <td class="label">唯一标识符</td>
        <td class="value">{{ tagDetail.slug || '无' }}</td>
        <td class="label">使用次数</td>
        <td class="value">{{ tagDetail.usageCount ?? '无' }}</td>
      </tr>
      <tr>
        <td class="label">创建人</td>
        <td class="value" colspan="3">{{ tagDetail.creatorId ? `${tagDetail.creatorId} (${userNameMap.get(String(tagDetail.creatorId)) || '无'})` : '无' }}</td>
      </tr>
      <tr>
        <td class="label">描述</td>
        <td class="value" colspan="3">{{ tagDetail.description || '无' }}</td>
      </tr>
      <tr>
        <td class="label">创建时间</td>
        <td class="value">{{ formatDate(tagDetail.createdAt) }}</td>
        <td class="label">更新时间</td>
        <td class="value">{{ formatDate(tagDetail.updateAt) }}</td>
      </tr>
    </table>
  </CardContainer>

  <TagCreateDialog v-model="editDialogVisible" mode="edit" :tag-id="tagId" @success="handleEditSuccess" />
</template>
