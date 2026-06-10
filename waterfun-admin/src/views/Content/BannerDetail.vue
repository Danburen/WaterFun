<script setup lang="ts">
import { formatDate } from "@waterfun/web-core/src/timer";
import { useRoute, useRouter } from "vue-router";
import { getBannerById, type BannerResp } from "~/api/banner";
import { ElMessage } from "element-plus";
import BannerCreateDialog from "~/views/Content/components/BannerCreateDialog.vue";

const route = useRoute();
const router = useRouter();

const bannerId = computed(() => String(route.params.id));
const loading = ref(false);
const bannerDetail = ref<BannerResp | null>(null);
const editDialogVisible = ref(false);

const positionLabel = (pos?: string): string =>
  ({ home: "首页", side: "侧边栏" })[pos?.toLowerCase() ?? ""] || pos || "无";

const statusLabel = (s?: string): string =>
  ({ show: "显示", hide: "隐藏" })[s?.toLowerCase() ?? ""] || s || "无";

const validityLabel = computed(() => {
  const d = bannerDetail.value;
  if (!d) return { text: "未知", type: "badge-blue" as const };
  const now = Date.now();
  const start = d.startAt ? new Date(d.startAt).getTime() : null;
  const end = d.endAt ? new Date(d.endAt).getTime() : null;
  if (start && now < start) return { text: "未生效", type: "badge-yellow" as const };
  if (end && now > end) return { text: "已过期", type: "badge-red" as const };
  return { text: "生效中", type: "badge-green" as const };
});

const statusBadgeType = (s?: string) =>
  s?.toUpperCase() === 'SHOW' ? 'badge-green' : 'badge-gray';

const fetchDetail = async () => {
  if (!bannerId.value) {
    ElMessage.error("无效的横幅ID");
    router.back();
    return;
  }
  loading.value = true;
  try {
    const res = await getBannerById(bannerId.value);
    bannerDetail.value = res.data;
  } catch {
    ElMessage.error("获取横幅详情失败");
  } finally {
    loading.value = false;
  }
};

const handleEditSuccess = () => { fetchDetail() };

onMounted(fetchDetail);
</script>

<template>
  <CardContainer title="横幅详情">
    <template #header-right>
      <button class="btn" @click="router.back()">返回</button>
      <button class="btn btn-primary" @click="editDialogVisible = true">编辑</button>
    </template>

    <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
    <table v-else-if="bannerDetail" class="detail-table">
      <tr>
        <td class="label">ID</td>
        <td class="value">{{ bannerDetail.id }}</td>
        <td class="label">标题</td>
        <td class="value">{{ bannerDetail.title }}</td>
      </tr>
      <tr>
        <td class="label">副标题</td>
        <td class="value">{{ bannerDetail.subtitle || "无" }}</td>
        <td class="label">位置</td>
        <td class="value">{{ positionLabel(bannerDetail.position) }}</td>
      </tr>
      <tr>
        <td class="label">排序</td>
        <td class="value">{{ bannerDetail.sortNo ?? "无" }}</td>
        <td class="label">状态</td>
        <td class="value"><span :class="['badge', statusBadgeType(bannerDetail.status)]">{{ statusLabel(bannerDetail.status) }}</span></td>
      </tr>
      <tr>
        <td class="label">有效期状态</td>
        <td class="value"><span :class="['badge', validityLabel.type]">{{ validityLabel.text }}</span></td>
        <td class="label">启用时间</td>
        <td class="value">{{ formatDate(bannerDetail.startAt) || "立即" }}</td>
      </tr>
      <tr>
        <td class="label">结束时间</td>
        <td class="value">{{ formatDate(bannerDetail.endAt) || "永久" }}</td>
        <td class="label">创建时间</td>
        <td class="value">{{ formatDate(bannerDetail.createdAt) }}</td>
      </tr>
      <tr>
        <td class="label">更新时间</td>
        <td class="value" colspan="3">{{ formatDate(bannerDetail.updatedAt) }}</td>
      </tr>
      <tr>
        <td class="label">图片</td>
        <td class="value" colspan="3">
          <img v-if="bannerDetail.coverageUrl?.url" :src="bannerDetail.coverageUrl.url" style="max-width: 320px; max-height: 180px; border-radius: 6px" />
          <span v-else>无</span>
        </td>
      </tr>
      <tr>
        <td class="label">跳转链接</td>
        <td class="value" colspan="3">
          <a v-if="bannerDetail.linkUrl" :href="bannerDetail.linkUrl" target="_blank" class="link">{{ bannerDetail.linkUrl }}</a>
          <span v-else>无</span>
        </td>
      </tr>
    </table>
  </CardContainer>

  <BannerCreateDialog v-model="editDialogVisible" mode="edit" :banner-id="bannerId" @success="handleEditSuccess" />
</template>
