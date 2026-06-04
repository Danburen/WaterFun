<script setup lang="ts">
import type { FormInstance, FormRules } from "element-plus";
import {
  createBanner,
  getBannerById,
  getBannerCoverageUpload,
  updateBanner,
  type BannerPosition,
  type BannerStatus,
  type CreateBannerRequest,
  type Instant,
  type UpdateBannerRequest,
} from "~/api/banner";
import SingleImageUploader from "~/components/SingleImageUploader.vue";
import { ElMessage } from "element-plus";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    mode?: "create" | "edit";
    bannerId?: number;
    initialResourceKey?: string;
    initialUploadToken?: string;
  }>(),
  {
    mode: "create",
    bannerId: 0,
    initialResourceKey: "",
    initialUploadToken: "",
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  success: [];
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);
const uploaderVisible = ref(false);

const formModel = ref<{
  title: string;
  subtitle: string;
  linkUrl: string;
  position: BannerPosition;
  sortNo: number;
  status: BannerStatus;
  startAt: Date | null;
  endAt: Date | null;
  imageUuid: string;
}>(
  {
    title: "",
    subtitle: "",
    linkUrl: "",
    position: "HOME",
    sortNo: 0,
    status: "SHOW",
    startAt: null,
    endAt: null,
    imageUuid: "",
  }
);

const uploadToken = ref("");
const resourceKey = ref("");
const coveragePreviewUrl = ref("");

const hasImage = computed(() => !!(coveragePreviewUrl.value || formModel.value.imageUuid));

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: "blur" }],
  subtitle: [{ max: 128, message: "Max 128", trigger: "blur" }],
  linkUrl: [{ max: 255, message: "Max 255", trigger: "blur" }],
};

const positionOptions: { label: string; value: BannerPosition }[] = [
  { label: "首页", value: "HOME" },
  { label: "侧边栏", value: "SIDE" },
];

const statusOptions: { label: string; value: BannerStatus }[] = [
  { label: "显示", value: "SHOW" },
  { label: "隐藏", value: "HIDE" },
];

const dialogTitle = computed(() =>
  props.mode === "edit" ? '编辑轮播图' : '新增轮播图'
);

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});

const toDate = (value?: Instant | string | null): Date | null => {
  if (!value) return null;
  if (typeof value === "string") {
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }
  const seconds = Number(value.seconds || 0);
  const nanos = Number(value.nanos || 0);
  if (!seconds && !nanos) return null;
  const ms = seconds * 1000 + Math.floor(nanos / 1_000_000);
  return new Date(ms);
};

const toInstant = (value?: Date | null): Instant | undefined => {
  if (!value) return undefined;
  const ms = value.getTime();
  return {
    seconds: Math.floor(ms / 1000),
    nanos: (ms % 1000) * 1_000_000,
  };
};

const resetForm = () => {
  formModel.value = {
    title: "",
    subtitle: "",
    linkUrl: "",
    position: "HOME",
    sortNo: 0,
    status: "SHOW",
    startAt: null,
    endAt: null,
    imageUuid: "",
  };
  resourceKey.value = "";
  uploadToken.value = "";
  coveragePreviewUrl.value = "";
  uploaderVisible.value = false;
  formRef.value?.resetFields();
};

const loadDetail = async () => {
  if (props.mode !== "edit" || !props.bannerId) return;
  try {
    const res = await getBannerById(props.bannerId);
    formModel.value = {
      title: res.data.title || "",
      subtitle: res.data.subtitle || "",
      linkUrl: res.data.linkUrl || "",
      position: res.data.position || "HOME",
      sortNo: res.data.sortNo || 0,
      status: res.data.status || "SHOW",
      startAt: toDate(res.data.startAt),
      endAt: toDate(res.data.endAt),
      imageUuid: "",
    };
    if (res.data.coverageUrl?.url) {
      coveragePreviewUrl.value = res.data.coverageUrl.url;
    }
  } catch (e) {
    console.error(e);
    ElMessage.error('获取数据失败');
  }
};

watch(
  () => visible.value,
  (open) => {
    if (open) {
      loadDetail();
      if (props.mode === "create" && props.initialResourceKey) {
        formModel.value.imageUuid = props.initialResourceKey;
        resourceKey.value = props.initialResourceKey;
        uploadToken.value = props.initialUploadToken || "";
      }
    }
  }
);

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  if (!hasImage.value) {
    ElMessage.error('请先上传图片');
    return;
  }

  submitting.value = true;
  try {
    const startAt = toInstant(formModel.value.startAt);
    const endAt = toInstant(formModel.value.endAt);

    if (props.mode === "edit" && props.bannerId) {
      const payload: UpdateBannerRequest = {
        title: formModel.value.title,
        subtitle: formModel.value.subtitle || undefined,
        linkUrl: formModel.value.linkUrl || undefined,
        position: formModel.value.position,
        sortNo: formModel.value.sortNo,
        status: formModel.value.status,
        startAt,
        endAt,
      };
      if (formModel.value.imageUuid) {
        payload.imageUuid = formModel.value.imageUuid;
        payload.resourceKey = resourceKey.value || undefined;
      }
      await updateBanner(props.bannerId, payload);
      ElMessage.success('轮播图更新成功');
    } else {
      const payload: CreateBannerRequest = {
        title: formModel.value.title,
        subtitle: formModel.value.subtitle || undefined,
        linkUrl: formModel.value.linkUrl || undefined,
        position: formModel.value.position,
        sortNo: formModel.value.sortNo,
        status: formModel.value.status,
        startAt,
        endAt,
        imageUuid: formModel.value.imageUuid,
      };
      await createBanner(payload);
      ElMessage.success('轮播图创建成功');
    }

    visible.value = false;
    emit("success");
  } catch (e) {
    console.error(e);
    ElMessage.error(props.mode === "edit" ? '轮播图保存失败' : '轮播图创建失败');
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="720" destroy-on-close @closed="resetForm">
    <el-form ref="formRef" :model="formModel" :rules="rules" label-width="110px" status-icon>
      <el-form-item prop="title" label="标题">
        <el-input v-model="formModel.title" placeholder="请输入标题"/>
      </el-form-item>
      <el-form-item prop="subtitle" label="副标题">
        <el-input v-model="formModel.subtitle" placeholder="请输入副标题"/>
      </el-form-item>
      <el-form-item prop="imageUuid" label="轮播图图片">
        <div class="banner-coverage">
          <img v-if="coveragePreviewUrl" class="banner-coverage-img" :src="coveragePreviewUrl" alt="" @click="uploaderVisible = true"/>
          <el-button v-else type="primary" plain @click="uploaderVisible = true">选择</el-button>
          <el-button v-if="coveragePreviewUrl" @click="uploaderVisible = true">更换</el-button>
        </div>
        <SingleImageUploader
          v-model:visible="uploaderVisible"
          v-model:resourceKey="resourceKey"
          v-model:uploadToken="uploadToken"
          v-model:previewUrl="coveragePreviewUrl"
          :get-upload-policy="getBannerCoverageUpload"
          @submitted="(payload) => { formModel.imageUuid = payload.key }"
        />
      </el-form-item>
      <el-form-item prop="linkUrl" label="跳转链接">
        <el-input v-model="formModel.linkUrl" placeholder="请输入跳转链接"/>
      </el-form-item>
      <el-form-item prop="position" label="位置">
        <el-select v-model="formModel.position" style="width: 220px">
          <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value"/>
        </el-select>
      </el-form-item>
      <el-form-item prop="status" label="状态">
        <el-select v-model="formModel.status" style="width: 220px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"/>
        </el-select>
      </el-form-item>
      <el-form-item prop="sortNo" label="排序">
        <el-input-number v-model="formModel.sortNo" :min="0" :max="99999"/>
      </el-form-item>
      <el-form-item prop="startAt" label="开始时间">
        <el-date-picker v-model="formModel.startAt" type="datetime" clearable style="width: 220px"/>
      </el-form-item>
      <el-form-item prop="endAt" label="结束时间">
        <el-date-picker v-model="formModel.endAt" type="datetime" clearable style="width: 220px"/>
      </el-form-item>
      
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.banner-coverage {
  display: flex;
  align-items: center;
  gap: 10px;
}

.banner-coverage-img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
  cursor: pointer;
}
</style>
