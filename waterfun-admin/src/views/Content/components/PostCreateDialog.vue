<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { getCategoryOptions } from "~/api/category";
import { createPost, getPostById, putPost, replacePostTags, type CreatePostRequest, type PostResp, type PostStatus, type PostVisibility } from "~/api/post";
import { getTagOptions } from "~/api/tag";
import { getUserOptions } from "~/api/user";
import { ElMessage } from "element-plus";
import BaseDialog from "~/components/BaseDialog.vue";

const props = withDefaults(defineProps<{
  modelValue: boolean; mode?: "create" | "edit"; postId?: string;
}>(), { mode: "create", postId: "" });

const emit = defineEmits<{ "update:modelValue": [value: boolean]; success: [] }>();

const submitting = ref(false);
const loadingOptions = ref(false);
const categoryOptions = ref<OptionResItem<number>[]>([]);
const tagOptions = ref<OptionResItem<number>[]>([]);
const userOptions = ref<OptionResItem<string>[]>([]);

const title = ref(''); const content = ref(''); const subtitle = ref(''); const summary = ref(''); const coverImg = ref(''); const slug = ref('');
const status = ref<PostStatus>('DRAFT'); const visibility = ref<PostVisibility>('PUBLIC');
const authorId = ref(''); const categoryId = ref(''); const tagIds = ref<number[]>([]);

const titleErr = ref(''); const contentErr = ref(''); const authorIdErr = ref(''); const categoryIdErr = ref('');

const statusOptions = [{ label: "草稿", value: "DRAFT" }, { label: "待审核", value: "PENDING" }, { label: "已发布", value: "PUBLISHED" }, { label: "已拒绝", value: "REJECTED" }, { label: "已归档", value: "ARCHIVED" }];
const visibilityOptions = [{ label: "公开", value: "PUBLIC" }, { label: "私密", value: "PRIVATE" }, { label: "粉丝可见", value: "FANS_ONLY" }];

const dialogTitle = computed(() => props.mode === "edit" ? '编辑文章' : '新增文章');
const visible = computed({ get: () => props.modelValue, set: v => emit("update:modelValue", v) });

const resetForm = () => {
  title.value = ''; content.value = ''; subtitle.value = ''; summary.value = ''; coverImg.value = ''; slug.value = '';
  status.value = 'DRAFT'; visibility.value = 'PUBLIC'; authorId.value = ''; categoryId.value = ''; tagIds.value = [];
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
  title.value = data.title || ''; content.value = data.content || ''; subtitle.value = data.subtitle || ''; summary.value = data.summary || '';
  coverImg.value = data.coverImg || ''; slug.value = data.slug || ''; status.value = data.status || 'DRAFT'; visibility.value = data.visibility || 'PUBLIC';
  authorId.value = data.authorId || ''; categoryId.value = data.categoryId || ''; tagIds.value = data.tagIds || [];
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
  if (!categoryId.value) { categoryIdErr.value = '请选择分类'; ok = false; }
  return ok;
};

const handleSave = async () => {
  if (!validate()) return;
  submitting.value = true;
  try {
    const payload = { title: title.value, content: content.value, subtitle: subtitle.value || undefined, summary: summary.value || undefined, coverImg: coverImg.value || undefined, slug: slug.value || undefined, status: status.value as PostStatus, visibility: visibility.value as PostVisibility, authorId: authorId.value, categoryId: categoryId.value, tagIds: tagIds.value };
    if (props.mode === "edit" && props.postId) { await putPost(props.postId, payload); await replacePostTags(props.postId, { tagIds: tagIds.value }); ElMessage.success('文章更新成功'); }
    else { const created = await createPost({ ...payload, tagIds: tagIds.value.length ? tagIds.value : undefined } as CreatePostRequest); ElMessage.success('文章创建成功'); }
    visible.value = false; emit("success");
  } catch { ElMessage.error(props.mode === "edit" ? '文章保存失败' : '文章创建失败'); }
  finally { submitting.value = false; }
};
</script>

<template>
  <BaseDialog v-model="visible" :title="dialogTitle" width="760px">
    <div v-if="loadingOptions" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
    <div v-else class="form-block">
      <div class="form-field">
        <label class="form-label">标题</label>
        <div class="form-content"><input v-model="title" class="form-input" placeholder="请输入标题" /><div v-if="titleErr" class="form-error">{{ titleErr }}</div></div>
      </div>
      <div class="form-field">
        <label class="form-label">副标题</label>
        <div class="form-content"><input v-model="subtitle" class="form-input" placeholder="请输入副标题" /></div>
      </div>
      <div class="form-field">
        <label class="form-label">内容</label>
        <div class="form-content"><textarea v-model="content" class="form-textarea" rows="6" placeholder="请输入内容"></textarea><div v-if="contentErr" class="form-error">{{ contentErr }}</div></div>
      </div>
      <div class="form-field">
        <label class="form-label">摘要</label>
        <div class="form-content"><textarea v-model="summary" class="form-textarea" rows="3" placeholder="请输入摘要"></textarea></div>
      </div>
      <div class="form-field">
        <label class="form-label">封面图</label>
        <div class="form-content"><input v-model="coverImg" class="form-input" placeholder="请输入封面图URL" /></div>
      </div>
      <div class="form-field">
        <label class="form-label">唯一标识符</label>
        <div class="form-content"><input v-model="slug" class="form-input" placeholder="请输入唯一标识符" /></div>
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
            <option value="">请选择</option>
            <option v-for="item in userOptions" :key="item.id" :value="item.id" :disabled="item.disabled">{{ item.id }} ({{ item.name }}{{ item.code ? ` / ${item.code}` : '' }})</option>
          </select>
          <div v-if="authorIdErr" class="form-error">{{ authorIdErr }}</div>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">分类ID</label>
        <div class="form-content">
          <select v-model="categoryId" class="form-select">
            <option value="">请选择</option>
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
