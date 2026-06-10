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

const categoryNameMap = computed(() => { const m = new Map<number, string>(); categoryOptions.value.forEach(i => m.set(i.id, i.name)); return m; });
const userNameMap = computed(() => { const m = new Map<string, string>(); userOptions.value.forEach(i => m.set(i.id, i.name)); return m; });

const fetchOptions = async () => {
  try { const [cr, ur] = await Promise.all([getCategoryOptions(), getUserOptions()]); categoryOptions.value = cr.data || []; userOptions.value = ur.data || []; }
  catch { ElMessage.error('获取数据失败'); }
};

const fetchDetail = async () => {
  if (Number.isNaN(categoryId.value)) { ElMessage.error('无效的分类ID'); router.back(); return; }
  loading.value = true;
  try { const res = await getCategory(categoryId.value); categoryDetail.value = res.data; }
  catch { ElMessage.error('获取分类详情失败'); }
  finally { loading.value = false; }
};

const handleEditSuccess = async () => { await fetchDetail() };

onMounted(async () => { await Promise.all([fetchOptions(), fetchDetail()]); });
</script>

<template>
  <CardContainer title="分类详情">
    <template #header-right>
      <button class="btn" @click="router.back()">返回</button>
      <button class="btn btn-primary" @click="editDialogVisible = true">编辑</button>
    </template>

    <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
    <table v-else-if="categoryDetail" class="detail-table">
      <tr>
        <td class="label">ID</td>
        <td class="value">{{ categoryDetail.id }}</td>
        <td class="label">分类名</td>
        <td class="value">{{ categoryDetail.name }}</td>
      </tr>
      <tr>
        <td class="label">唯一标识符</td>
        <td class="value">{{ categoryDetail.slug || '无' }}</td>
        <td class="label">父级</td>
        <td class="value">{{ categoryDetail.parentId != null ? `${categoryDetail.parentId} (${categoryNameMap.get(Number(categoryDetail.parentId)) || '无'})` : '无' }}</td>
      </tr>
      <tr>
        <td class="label">排序</td>
        <td class="value">{{ categoryDetail.sortOrder ?? '无' }}</td>
        <td class="label">是否启用</td>
        <td class="value"><span :class="['badge', categoryDetail.isActive ? 'badge-green' : 'badge-gray']">{{ categoryDetail.isActive ? '是' : '否' }}</span></td>
      </tr>
      <tr>
        <td class="label">创建人</td>
        <td class="value" colspan="3">{{ categoryDetail.creatorId ? `${categoryDetail.creatorId} (${userNameMap.get(categoryDetail.creatorId) || '无'})` : '无' }}</td>
      </tr>
      <tr>
        <td class="label">描述</td>
        <td class="value" colspan="3">{{ categoryDetail.description || '无' }}</td>
      </tr>
      <tr>
        <td class="label">创建时间</td>
        <td class="value">{{ formatDate(categoryDetail.createdAt) }}</td>
        <td class="label">更新时间</td>
        <td class="value">{{ formatDate(categoryDetail.updateAt) }}</td>
      </tr>
    </table>
  </CardContainer>

  <CategoryCreateDialog v-model="editDialogVisible" mode="edit" :category-id="categoryId" @success="handleEditSuccess" />
</template>
