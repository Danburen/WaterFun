<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { getCategoryOptions } from "~/api/category";
import { createPost, getPostById, putPost, replacePostTags, type PostResp, type PostStatus, type PostVisibility, type PostType } from "~/api/post";
import { getTagOptions } from "~/api/tag";
import { getUserOptions } from "~/api/user";
import { uploadImage, type AdminUploadBizType } from "~/api/upload";
import { ElMessage } from "element-plus";
import BaseDialog from "~/components/BaseDialog.vue";

const props = withDefaults(defineProps<{
  modelValue: boolean; mode?: "create" | "edit"; postId?: string;
}>(), { mode: "create", postId: "" });

const emit = defineEmits<{ "update:modelValue": [value: boolean]; success: [] }>();

const submitting = ref(false);
const loadingOptions = ref(false);
const uploadingCover = ref(false);
const categoryOptions = ref<OptionResItem<number>[]>([]);
const tagOptions = ref<OptionResItem<number>[]>([]);
const userOptions = ref<OptionResItem<string>[]>([]);

const title = ref('');
const content = ref('');
const subtitle = ref('');
const summary = ref('');
const coverageUuid = ref('');
const slug = ref('');
const status = ref<PostStatus>('DRAFT');
const visibility = ref<PostVisibility>('PUBLIC');
const authorId = ref<string | null>(null);
const categoryId = ref<number | null>(null);
const tagIds = ref<number[]>([]);
const postType = ref<PostType>('COMMON');
const isPinned = ref(false);

const titleErr = ref('');
const contentErr = ref('');
const authorIdErr = ref('');
const categoryIdErr = ref('');

const statusOptions = [{ label: "草稿", value: "DRAFT" }, { label: "待审核", value: "PENDING" }, { label: "已发布", value: "PUBLISHED" }, { label: "已拒绝", value: "REJECTED" }, { label: "已归档", value: "ARCHIVED" }];
const visibilityOptions = [{ label: "公开", value: "PUBLIC" }, { label: "私密", value: "PRIVATE" }, { label: "粉丝可见", value: "FANS_ONLY" }];
const postTypeOptions = [{ label: "普通帖子", value: "COMMON" }, { label: "公告", value: "NOTICE" }];

const dialogTitle = computed(() => props.mode === "edit" ? '编辑文章' : '新增文章');
const visible = computed({ get: () => props.modelValue, set: v => emit("update:modelValue", v) });

const resetForm = () => {
  title.value = '';
  content.value = '';
  subtitle.value = '';
  summary.value = '';
  coverageUuid.value = '';
  slug.value = '';
  status.value = 'DRAFT';
  visibility.value = 'PUBLIC';
  authorId.value = null;
  categoryId.value = null;
  tagIds.value = [];
  postType.value = 'COMMON';
  isPinned.value = false;
  clearErrs();
};
const clearErrs = () => { titleErr.value = ''; contentErr.value = ''; authorIdErr.value = ''; categoryIdErr.value = ''; };

const loadOptions = async () => {
  loadingOptions.value = true;
  try { const [cr, tr, ur] = await Promise.all([getCategoryOptions(), getTagOptions(), getUserOptions()]); categoryOptions.value = cr.data || []; tagOptions.value = tr.data || []; userOptions.value = ur.data || []; }
  catch { ElMessage.error('获取数据失败'); }
  finally { loadingOptions.value = false; }
};

const fillForm = (data: PostResp) => {
  title.value = data.title || '';
  content.value = data.content || '';
  subtitle.value = data.subtitle || '';
  summary.value = data.summary || '';
  /* coverImg in response, coverageUuid in request — initialize from response */
  coverageUuid.value = data.coverImg || '';
  slug.value = data.slug || '';
  status.value = data.status || 'DRAFT';
  visibility.value = data.visibility || 'PUBLIC';
  authorId.value = String(data.authorId ?? '');
  categoryId.value = data.categoryId ? Number(data.categoryId) : null;
  tagIds.value = (data.tagIds || []).map(id => Number(id));
  postType.value = data.type || 'COMMON';
  isPinned.value = data.isPinned ?? false;
};

const loadDetail = async () => {
  if (props.mode !== "edit" || !props.postId) return;
  try { const res = await getPostById(props.postId); fillForm(res.data); }
  catch { ElMessage.error('获取数据失败'); }
};

watch(() => visible.value, async (open) => { if (open) { resetForm(); await loadOptions(); await loadDetail(); } });

const toggleTag = (id: number) => { const idx = tagIds.value.indexOf(id); if (idx >= 0) tagIds.value.splice(idx, 1); else tagIds.value.push(id); };

const validate = () => {
  clearErrs(); let ok = true;
  if (!title.value.trim()) { titleErr.value = '请输入标题'; ok = false; }
  if (!content.value.trim()) { contentErr.value = '请输入内容'; ok = false; }
  if (!authorId.value) { authorIdErr.value = '请选择作者'; ok = false; }
  if (categoryId.value == null) { categoryIdErr.value = '请选择分类'; ok = false; }
  return ok;
};

const handleCoverUpload = async () => {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.onchange = async () => {
    const file = input.files?.[0];
    if (!file) return;
    uploadingCover.value = true;
    const uuid = await uploadImage('POST_COVERAGE_IMAGE', file);
    if (uuid) {
      coverageUuid.value = uuid;
      ElMessage.success('封面图上传成功');
    } else {
      ElMessage.error('封面图上传失败');
    }
    uploadingCover.value = false;
  };
  input.click();
};

const handleSave = async () => {
  if (!validate()) return;
  submitting.value = true;
  try {
    const payload = {
      title: title.value,
      content: content.value,
      subtitle: subtitle.value || undefined,
      summary: summary.value || undefined,
      coverageUuid: coverageUuid.value || undefined,
      slug: slug.value || undefined,
      status: status.value as PostStatus,
      visibility: visibility.value as PostVisibility,
      authorId: authorId.value ?? undefined,
      categoryId: categoryId.value != null ? String(categoryId.value) : undefined,
      tagIds: tagIds.value.length ? tagIds.value.map(id => String(id)) : undefined,
      type: postType.value as PostType,
      isPinned: isPinned.value || undefined,
    };
    if (props.mode === "edit" && props.postId) {
      await putPost(props.postId, payload);
      if (tagIds.value.length) {
        await replacePostTags(props.postId, { tagIds: tagIds.value.map(id => String(id)) });
      }
      ElMessage.success('文章更新成功');
    } else {
      await createPost(payload);
      ElMessage.success('文章创建成功');
    }
    visible.value = false;
    emit("success");
  } catch {
    ElMessage.error(props.mode === "edit" ? '文章保存失败' : '文章创建失败');
  }
  finally { submitting.value = false; }
};
</script>

<template>
  <BaseDialog v-model="visible" :title="dialogTitle" width="820px">
    <div v-if="loadingOptions" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
    <div v-else class="form-block">
      <div class="form-field">
        <label class="form-label">标题</label>
        <div class="form-content"><input v-model="title" class="form-input" placeholder="请输入标题" maxlength="32" /><div v-if="titleErr" class="form-error">{{ titleErr }}</div></div>
      </div>
      <div class="form-field">
        <label class="form-label">副标题</label>
        <div class="form-content"><input v-model="subtitle" class="form-input" placeholder="请输入副标题" maxlength="64" /></div>
      </div>
      <div class="form-field">
        <label class="form-label">帖子类型</label>
        <div class="form-content">
          <select v-model="postType" class="form-select" style="max-width: 220px;">
            <option v-for="item in postTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">置顶</label>
        <div class="form-content">
          <label style="display: flex; align-items: center; gap: 6px; cursor: pointer; font-size: 13px;">
            <input type="checkbox" v-model="isPinned" />
            置顶该帖子
          </label>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">内容</label>
        <div class="form-content"><textarea v-model="content" class="form-textarea" rows="6" placeholder="请输入内容"></textarea><div v-if="contentErr" class="form-error">{{ contentErr }}</div></div>
      </div>
      <div class="form-field">
        <label class="form-label">摘要</label>
        <div class="form-content"><textarea v-model="summary" class="form-textarea" rows="3" placeholder="请输入摘要" maxlength="500"></textarea></div>
      </div>
      <div class="form-field">
        <label class="form-label">封面图</label>
        <div class="form-content">
          <div style="display: flex; align-items: center; gap: 8px;">
            <input v-model="coverageUuid" class="form-input" placeholder="封面图UUID，或点击上传" readonly />
            <button class="btn btn-default" :disabled="uploadingCover" @click="handleCoverUpload">
              <i v-if="uploadingCover" class="fa-solid fa-spinner fa-spin"></i>
              <i v-else class="fa-solid fa-upload"></i>
              {{ coverageUuid ? '重新上传' : '上传封面' }}
            </button>
          </div>
          <div v-if="coverageUuid" style="margin-top: 6px; font-size: 12px; color: #666;">已上传: {{ coverageUuid }}</div>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">唯一标识符</label>
        <div class="form-content"><input v-model="slug" class="form-input" placeholder="请输入唯一标识符" maxlength="200" /></div>
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
        <label class="form-label">可见性</label>
        <div class="form-content">
          <select v-model="visibility" class="form-select" style="max-width: 220px;">
            <option v-for="item in visibilityOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">作者ID</label>
        <div class="form-content">
            <select v-model="authorId" class="form-select">
              <option :value="null">请选择</option>
            <option v-for="item in userOptions" :key="item.id" :value="item.id" :disabled="item.disabled">{{ item.id }} ({{ item.name }}{{ item.code ? ` / ${item.code}` : '' }})</option>
          </select>
          <div v-if="authorIdErr" class="form-error">{{ authorIdErr }}</div>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">分类ID</label>
        <div class="form-content">
            <select v-model="categoryId" class="form-select">
              <option :value="null">请选择</option>
            <option v-for="item in categoryOptions" :key="item.id" :value="item.id" :disabled="item.disabled">{{ item.id }} ({{ item.name }})</option>
          </select>
          <div v-if="categoryIdErr" class="form-error">{{ categoryIdErr }}</div>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">标签</label>
        <div class="form-content">
          <div class="option-grid" style="max-height: 150px; overflow-y: auto; padding: 4px 0;">
            <label v-for="item in tagOptions" :key="item.id" style="display: flex; align-items: center; gap: 6px; font-size: 13px; cursor: pointer;">
              <input type="checkbox" :checked="tagIds.includes(item.id)" @change="toggleTag(item.id)" />
              {{ item.id }} ({{ item.name }})
            </label>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <button class="btn" @click="visible = false">取消</button>
      <button class="btn btn-primary" :disabled="submitting" @click="handleSave"><i v-if="submitting" class="fa-solid fa-spinner fa-spin"></i> 保存</button>
    </template>
  </BaseDialog>
</template>
