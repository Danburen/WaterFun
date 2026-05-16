<script setup lang="ts">
import { formatDate } from "@waterfun/web-core/src/timer";
import { useI18n } from "vue-i18n";
import SearchContainer from "~/components/SearchContainer.vue";
import TableContainer from "~/components/TableContainer.vue";
import { listBanners, getBannerCoverageUpload, type BannerPosition, type BannerResp, type BannerStatus } from "~/api/banner";
import type { PageOptions } from "~/types/api";
import BannerCreateDialog from "~/views/Content/components/BannerCreateDialog.vue";
import { ElMessage } from "element-plus";

const { t } = useI18n();

const loading = ref(false);
const bannerList = ref<BannerResp[]>([]);
const createDialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const currentBannerId = ref<number>(0);

const searchForm = ref<{
  title: string;
  subtitle: string;
  position: BannerPosition | "";
  status: BannerStatus | "";
}>({
  title: "",
  subtitle: "",
  position: "",
  status: "",
});

const pageOpts = ref<PageOptions>({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const positionOptions: { label: string; value: BannerPosition }[] = [
  { label: "content.banner.position.home", value: "HOME" },
  { label: "content.banner.position.side", value: "SIDE" },
];

const statusOptions: { label: string; value: BannerStatus }[] = [
  { label: "content.banner.status.show", value: "SHOW" },
  { label: "content.banner.status.hide", value: "HIDE" },
];

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listBanners({
      page: (pageOpts.value.currentPage || 1) - 1,
      size: pageOpts.value.pageSize,
      title: searchForm.value.title || undefined,
      subtitle: searchForm.value.subtitle || undefined,
      position: searchForm.value.position || undefined,
      status: searchForm.value.status || undefined,
    });
    bannerList.value = res.data.content || [];
    pageOpts.value.total = res.data.page?.totalElements || 0;
  } catch (e) {
    console.error(e);
    ElMessage.error(t("error.fetch"));
  } finally {
    loading.value = false;
  }
};

const uploaderVisible = ref(false);
const uploading = ref(false);
const selectedFile = ref<File | null>(null);
const uploadSuffix = ref("");
const uploadedResourceKey = ref("");
const uploadedUploadToken = ref("");

watch(
  () => createDialogVisible.value,
  (open) => {
    if (!open) {
      // clear transient upload state when dialog closed
      uploadedResourceKey.value = "";
      uploadedUploadToken.value = "";
      selectedFile.value = null;
      uploadSuffix.value = "";
      uploaderVisible.value = false;
    }
  }
);

const handleSearch = () => {
  pageOpts.value.currentPage = 1;
  fetchData();
};

const handleReset = () => {
  pageOpts.value.currentPage = 1;
  searchForm.value = {
    title: "",
    subtitle: "",
    position: "",
    status: "",
  };
  fetchData();
};

const handleAdd = () => {
  dialogMode.value = "create";
  currentBannerId.value = 0;
  createDialogVisible.value = true;
};

const handleEdit = (id: number) => {
  dialogMode.value = "edit";
  currentBannerId.value = id;
  createDialogVisible.value = true;
};

const handleCreateSuccess = () => {
  fetchData();
};

onMounted(() => {
  fetchData();
});
</script>

<template>
  <div class="list-layout">
    <SearchContainer>
      <el-form
        inline
        class="search-form"
        :model="searchForm"
      >
        <el-form-item :label="t('content.banner.field.title')">
          <el-input
            v-model="searchForm.title"
            :placeholder="t('content.banner.input.title')"
          />
        </el-form-item>
        <el-form-item :label="t('content.banner.field.subtitle')">
          <el-input
            v-model="searchForm.subtitle"
            :placeholder="t('content.banner.input.subtitle')"
          />
        </el-form-item>
        <el-form-item :label="t('content.banner.field.position')">
          <el-select
            v-model="searchForm.position"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in positionOptions"
              :key="item.value"
              :label="t(item.label)"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('content.banner.field.status')">
          <el-select
            v-model="searchForm.status"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="t(item.label)"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            {{ t("common.query.title") }}
          </el-button>
          <el-button @click="handleReset">
            {{ t("common.reset.title") }}
          </el-button>
        </el-form-item>
      </el-form>
    </SearchContainer>

    <TableContainer
      v-model:page-size="pageOpts.pageSize"
      v-model:current-page="pageOpts.currentPage"
      title="content.banner.title"
      show-add-btn
      :show-remove-btn="false"
      :total="pageOpts.total"
      @add="handleAdd"
      @change="fetchData"
    >
      <el-table
        v-loading="loading"
        :data="bannerList"
        border
        fit
        highlight-current-row
        style="width: 100%"
      >
        <el-table-column
          prop="id"
          label="ID"
          width="90"
        />
        <el-table-column
          prop="title"
          :label="t('content.banner.field.title')"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="subtitle"
          :label="t('content.banner.field.subtitle')"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="position"
          :label="t('content.banner.field.position')"
          width="110"
        >
          <template #default="{ row }">
            {{ row.position ? t(`content.banner.position.${row.position.toLowerCase()}`) : t('common.none.title') }}
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          :label="t('content.banner.field.status')"
          width="110"
        >
          <template #default="{ row }">
            {{ row.status ? t(`content.banner.status.${row.status.toLowerCase()}`) : t('common.none.title') }}
          </template>
        </el-table-column>
        <el-table-column
          prop="sortNo"
          :label="t('content.banner.field.sortNo')"
          width="110"
        />
        <el-table-column
          prop="linkUrl"
          :label="t('content.banner.field.linkUrl')"
          min-width="220"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-link
              v-if="row.linkUrl"
              :href="row.linkUrl"
              target="_blank"
              type="primary"
            >
              {{ row.linkUrl }}
            </el-link>
            <span v-else>{{ t('common.none.title') }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="startAt"
          :label="t('content.banner.field.startAt')"
          min-width="170"
        >
          <template #default="{ row }">
            {{ formatDate(row.startAt) || t('common.none.title') }}
          </template>
        </el-table-column>
        <el-table-column
          prop="endAt"
          :label="t('content.banner.field.endAt')"
          min-width="170"
        >
          <template #default="{ row }">
            {{ formatDate(row.endAt) || t('common.none.title') }}
          </template>
        </el-table-column>
        <el-table-column
          prop="createdAt"
          :label="t('common.time.create')"
          min-width="170"
        >
          <template #default="{ row }">
            {{ formatDate(row.createdAt) || t('common.none.title') }}
          </template>
        </el-table-column>
        <el-table-column
          prop="updatedAt"
          :label="t('common.time.update')"
          min-width="170"
        >
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) || t('common.none.title') }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('common.operation.title')"
          width="140"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              @click="handleEdit(row.id)"
            >
              {{ t('common.action.edit') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </TableContainer>

    <BannerCreateDialog
      v-model="createDialogVisible"
      :mode="dialogMode"
      :banner-id="currentBannerId"
      :initial-resource-key="uploadedResourceKey"
      :initial-upload-token="uploadedUploadToken"
      @success="handleCreateSuccess"
    />
  </div>
</template>
