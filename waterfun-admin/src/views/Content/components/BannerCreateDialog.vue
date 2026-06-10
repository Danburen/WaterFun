<script setup lang="ts">
import { createBanner, getBannerById, getBannerUploadPolicy, updateBanner, type BannerPosition, type BannerStatus, type CreateBannerRequest, type UpdateBannerRequest } from "~/api/banner";
import SingleImageUploader from "~/components/SingleImageUploader.vue";
import { ElMessage } from "element-plus";
import BaseDialog from "~/components/BaseDialog.vue";

const props = withDefaults(defineProps<{
  modelValue: boolean; mode?: "create" | "edit"; bannerId?: string; initialResourceKey?: string; initialUploadToken?: string;
}>(), { mode: "create", bannerId: "", initialResourceKey: "", initialUploadToken: "" });

const emit = defineEmits<{ "update:modelValue": [value: boolean]; success: [] }>();

const submitting = ref(false);
const uploaderVisible = ref(false);
const title = ref(''); const subtitle = ref(''); const linkUrl = ref(''); const position = ref<BannerPosition>('HOME'); const sortNo = ref(0); const status = ref<BannerStatus>('SHOW'); const startAt = ref(''); const endAt = ref(''); const imageUuid = ref('');
const uploadToken = ref(''); const resourceKey = ref(''); const coveragePreviewUrl = ref('');

const hasImage = computed(() => !!(coveragePreviewUrl.value || imageUuid.value));
const titleErr = ref(''); const subtitleErr = ref(''); const linkUrlErr = ref('');

const positionOptions = [{ label: "首页", value: "HOME" }, { label: "侧边栏", value: "SIDE" }];
const statusOptions = [{ label: "显示", value: "SHOW" }, { label: "隐藏", value: "HIDE" }];

const dialogTitle = computed(() => props.mode === "edit" ? '编辑轮播图' : '新增轮播图');
const visible = computed({ get: () => props.modelValue, set: v => emit("update:modelValue", v) });

const toDateStr = (value?: string | null): string => { if (!value) return ''; const d = new Date(value); return Number.isNaN(d.getTime()) ? '' : d.toISOString().slice(0, 16); };
const toIsoString = (value: string): string | undefined => value ? new Date(value).toISOString() : undefined;

const resetForm = () => {
  title.value = ''; subtitle.value = ''; linkUrl.value = ''; position.value = 'HOME'; sortNo.value = 0; status.value = 'SHOW'; startAt.value = ''; endAt.value = ''; imageUuid.value = '';
  resourceKey.value = ''; uploadToken.value = ''; coveragePreviewUrl.value = ''; uploaderVisible.value = false; clearErrs();
};
const clearErrs = () => { titleErr.value = ''; subtitleErr.value = ''; linkUrlErr.value = ''; };

const loadDetail = async () => {
  if (props.mode !== "edit" || !props.bannerId) return;
  try {
    const res = await getBannerById(props.bannerId);
    title.value = res.data.title || ''; subtitle.value = res.data.subtitle || ''; linkUrl.value = res.data.linkUrl || '';
    position.value = res.data.position || 'HOME'; sortNo.value = res.data.sortNo || 0; status.value = res.data.status || 'SHOW';
    startAt.value = toDateStr(res.data.startAt); endAt.value = toDateStr(res.data.endAt); imageUuid.value = '';
    if (res.data.coverageUrl?.url) coveragePreviewUrl.value = res.data.coverageUrl.url;
  } catch { ElMessage.error('获取数据失败'); }
};

watch(() => visible.value, (open) => { if (open) { loadDetail(); if (props.mode === "create" && props.initialResourceKey) { imageUuid.value = props.initialResourceKey; resourceKey.value = props.initialResourceKey; uploadToken.value = props.initialUploadToken || ''; } } });

const validate = () => {
  clearErrs(); let ok = true;
  if (!title.value.trim()) { titleErr.value = '请输入标题'; ok = false; }
  if (subtitle.value.length > 128) { subtitleErr.value = 'Max 128'; ok = false; }
  if (linkUrl.value.length > 255) { linkUrlErr.value = 'Max 255'; ok = false; }
  return ok;
};

const handleSave = async () => {
  if (!validate()) return;
  if (!hasImage.value) { ElMessage.error('请先上传图片'); return; }
  submitting.value = true;
  try {
    const common = { title: title.value.trim(), subtitle: subtitle.value.trim() || undefined, linkUrl: linkUrl.value.trim() || undefined, position: position.value, sortNo: sortNo.value, status: status.value, startAt: toIsoString(startAt.value), endAt: toIsoString(endAt.value) };
    if (props.mode === "edit" && props.bannerId) {
      const payload: UpdateBannerRequest = { ...common };
      if (imageUuid.value) { payload.imageUuid = imageUuid.value; payload.resourceKey = resourceKey.value || undefined; }
      await updateBanner(props.bannerId, payload); ElMessage.success('轮播图更新成功');
    } else {
      await createBanner({ ...common, imageUuid: imageUuid.value } as CreateBannerRequest); ElMessage.success('轮播图创建成功');
    }
    visible.value = false; emit("success");
  } catch { ElMessage.error(props.mode === "edit" ? '轮播图保存失败' : '轮播图创建失败'); }
  finally { submitting.value = false; }
};
</script>

<template>
  <BaseDialog v-model="visible" :title="dialogTitle" width="720px">
    <div class="form-block">
      <div class="form-field">
        <label class="form-label">标题</label>
        <div class="form-content"><input v-model="title" class="form-input" placeholder="请输入标题" /><div v-if="titleErr" class="form-error">{{ titleErr }}</div></div>
      </div>
      <div class="form-field">
        <label class="form-label">副标题</label>
        <div class="form-content"><input v-model="subtitle" class="form-input" placeholder="请输入副标题" /><div v-if="subtitleErr" class="form-error">{{ subtitleErr }}</div></div>
      </div>
      <div class="form-field">
        <label class="form-label">轮播图图片</label>
        <div class="form-content">
          <div class="form-inline">
            <img v-if="coveragePreviewUrl" class="banner-coverage-img" :src="coveragePreviewUrl" alt="" @click="uploaderVisible = true" />
            <button v-if="!coveragePreviewUrl" class="btn btn-primary" @click="uploaderVisible = true">选择</button>
            <button v-if="coveragePreviewUrl" class="btn" @click="uploaderVisible = true">更换</button>
          </div>
          <SingleImageUploader v-model:visible="uploaderVisible" v-model:resourceKey="resourceKey" v-model:uploadToken="uploadToken" v-model:previewUrl="coveragePreviewUrl" :get-upload-policy="(suffix: string) => getBannerUploadPolicy([suffix]).then(r => ({ data: r.data[0] })) as any" @submitted="(payload) => { imageUuid = payload.token }" />
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">跳转链接</label>
        <div class="form-content"><input v-model="linkUrl" class="form-input" placeholder="请输入跳转链接" /><div v-if="linkUrlErr" class="form-error">{{ linkUrlErr }}</div></div>
      </div>
      <div class="form-field">
        <label class="form-label">位置</label>
        <div class="form-content">
          <select v-model="position" class="form-select" style="max-width: 220px;">
            <option v-for="item in positionOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">状态</label>
        <div class="form-content">
          <select v-model="status" class="form-select" style="max-width: 220px;">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">排序</label>
        <div class="form-content"><input v-model="sortNo" type="number" class="form-input" min="0" max="99999" style="max-width: 180px;" /></div>
      </div>
      <div class="form-field">
        <label class="form-label">开始时间</label>
        <div class="form-content"><input v-model="startAt" type="datetime-local" class="form-input" style="max-width: 260px;" /></div>
      </div>
      <div class="form-field">
        <label class="form-label">结束时间</label>
        <div class="form-content"><input v-model="endAt" type="datetime-local" class="form-input" style="max-width: 260px;" /></div>
      </div>
    </div>
    <template #footer>
      <button class="btn" @click="visible = false">取消</button>
      <button class="btn btn-primary" :disabled="submitting" @click="handleSave"><i v-if="submitting" class="fa-solid fa-spinner fa-spin"></i> 保存</button>
    </template>
  </BaseDialog>
</template>

<style scoped>
.banner-coverage-img { width: 120px; height: 120px; object-fit: cover; border-radius: 8px; border: 1px solid var(--border); cursor: pointer; }
</style>
