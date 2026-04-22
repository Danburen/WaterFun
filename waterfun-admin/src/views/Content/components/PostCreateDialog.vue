<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import type { FormInstance, FormRules } from "element-plus";
import { useI18n } from "vue-i18n";
import { getCategoryOptions } from "~/api/category";
import {
  createPost,
  getPostById,
  putPost,
  replacePostTags,
  type CreatePostRequest,
  type PostResp,
  type PostStatus,
  type PostVisibility,
  type UpdatePostRequest,
} from "~/api/post";
import { getTagOptions } from "~/api/tag";
import { getUserOptions } from "~/api/user";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    mode?: "create" | "edit";
    postId?: string;
  }>(),
  {
    mode: "create",
    postId: "",
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  success: [];
}>();

const { t } = useI18n();
const formRef = ref<FormInstance>();
const submitting = ref(false);
const loadingOptions = ref(false);

const categoryOptions = ref<OptionResItem<number>[]>([]);
const tagOptions = ref<OptionResItem<number>[]>([]);
const userOptions = ref<OptionResItem<string>[]>([]);

const formModel = ref<CreatePostRequest>({
  title: "",
  content: "",
  subtitle: "",
  summary: "",
  coverImg: "",
  status: "DRAFT",
  visibility: "PUBLIC",
  authorId: "",
  categoryId: 0,
  slug: "",
  tagIds: [],
});

const statusOptions: { label: string; value: PostStatus }[] = [
  { label: "content.post.status.draft", value: "DRAFT" },
  { label: "content.post.status.pending", value: "PENDING" },
  { label: "content.post.status.published", value: "PUBLISHED" },
  { label: "content.post.status.rejected", value: "REJECTED" },
  { label: "content.post.status.archived", value: "ARCHIVED" },
];

const visibilityOptions: { label: string; value: PostVisibility }[] = [
  { label: "content.post.visibility.public", value: "PUBLIC" },
  { label: "content.post.visibility.private", value: "PRIVATE" },
  { label: "content.post.visibility.fansOnly", value: "FANS_ONLY" },
];

const rules: FormRules<CreatePostRequest> = {
  title: [{ required: true, message: t("content.post.input.title"), trigger: "blur" }],
  content: [{ required: true, message: t("content.post.input.content"), trigger: "blur" }],
  authorId: [{ required: true, message: t("content.post.input.authorId"), trigger: "change" }],
  categoryId: [{ required: true, message: t("content.post.input.categoryId"), trigger: "change" }],
};

const dialogTitle = computed(() =>
  props.mode === "edit" ? t("content.post.edit") : t("content.post.create")
);

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});

const resetForm = () => {
  formModel.value = {
    title: "",
    content: "",
    subtitle: "",
    summary: "",
    coverImg: "",
    status: "DRAFT",
    visibility: "PUBLIC",
    authorId: "",
    categoryId: 0,
    slug: "",
    tagIds: [],
  };
  formRef.value?.resetFields();
};

const loadOptions = async () => {
  loadingOptions.value = true;
  try {
    const [categoryRes, tagRes, userRes] = await Promise.all([getCategoryOptions(), getTagOptions(), getUserOptions()]);
    categoryOptions.value = categoryRes.data || [];
    tagOptions.value = tagRes.data || [];
    userOptions.value = userRes.data || [];
  } catch (e) {
    console.error(e);
    ElMessage.error(t("error.fetch"));
  } finally {
    loadingOptions.value = false;
  }
};

const fillFormFromDetail = (data: PostResp) => {
  formModel.value = {
    title: data.title || "",
    content: data.content || "",
    subtitle: data.subtitle || "",
    summary: data.summary || "",
    coverImg: data.coverImg || "",
    status: data.status || "DRAFT",
    visibility: data.visibility || "PUBLIC",
    authorId: data.authorId || "",
    categoryId: data.categoryId || 0,
    slug: data.slug || "",
    tagIds: data.tagIds || [],
  };
};

const loadDetail = async () => {
  if (props.mode !== "edit" || !props.postId) return;
  try {
    const res = await getPostById(props.postId);
    fillFormFromDetail(res.data);
  } catch (e) {
    console.error(e);
    ElMessage.error(t("error.fetch"));
  }
};

watch(
  () => visible.value,
  async (open) => {
    if (open) {
      await loadOptions();
      await loadDetail();
    }
  }
);

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const payload: UpdatePostRequest = {
      ...formModel.value,
      subtitle: formModel.value.subtitle || undefined,
      summary: formModel.value.summary || undefined,
      coverImg: formModel.value.coverImg || undefined,
      slug: formModel.value.slug || undefined,
    };

    if (props.mode === "edit" && props.postId) {
      await putPost(props.postId, payload);
      await replacePostTags(props.postId, { tagIds: formModel.value.tagIds || [] });
      ElMessage.success(t("content.post.success.update"));
    } else {
      await createPost({
        ...(payload as CreatePostRequest),
        tagIds: formModel.value.tagIds?.length ? formModel.value.tagIds : undefined,
      });
      ElMessage.success(t("content.post.success.create"));
    }

    visible.value = false;
    emit("success");
  } catch (e) {
    console.error(e);
    ElMessage.error(props.mode === "edit" ? t("content.post.error.save") : t("content.post.error.create"));
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="760" destroy-on-close @closed="resetForm">
    <el-form ref="formRef" :model="formModel" :rules="rules" label-width="110px" status-icon v-loading="loadingOptions">
      <el-form-item prop="title" :label="t('content.post.field.title')">
        <el-input v-model="formModel.title" :placeholder="t('content.post.input.title')" />
      </el-form-item>
      <el-form-item prop="subtitle" :label="t('content.post.field.subtitle')">
        <el-input v-model="formModel.subtitle" :placeholder="t('content.post.input.subtitle')" />
      </el-form-item>
      <el-form-item prop="content" :label="t('content.post.field.content')">
        <el-input v-model="formModel.content" type="textarea" :rows="6" :placeholder="t('content.post.input.content')" />
      </el-form-item>
      <el-form-item prop="summary" :label="t('content.post.field.summary')">
        <el-input v-model="formModel.summary" type="textarea" :rows="3" :placeholder="t('content.post.input.summary')" />
      </el-form-item>
      <el-form-item prop="coverImg" :label="t('content.post.field.coverImg')">
        <el-input v-model="formModel.coverImg" :placeholder="t('content.post.input.coverImg')" />
      </el-form-item>
      <el-form-item prop="slug" :label="t('content.post.field.slug')">
        <el-input v-model="formModel.slug" :placeholder="t('content.post.input.slug')" />
      </el-form-item>
      <el-form-item prop="status" :label="t('content.post.field.status')">
        <el-select v-model="formModel.status" style="width: 220px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="t(item.label)" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item prop="visibility" :label="t('content.post.field.visibility')">
        <el-select v-model="formModel.visibility" style="width: 220px">
          <el-option v-for="item in visibilityOptions" :key="item.value" :label="t(item.label)" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item prop="authorId" :label="t('content.post.field.authorId')">
        <el-select v-model="formModel.authorId" style="width: 300px" filterable>
          <el-option
            v-for="item in userOptions"
            :key="item.id"
            :label="`${item.id} (${item.name}${item.code ? ` / ${item.code}` : ''})`"
            :value="item.id"
            :disabled="item.disabled || false"
          />
        </el-select>
      </el-form-item>
      <el-form-item prop="categoryId" :label="t('content.post.field.categoryId')">
        <el-select v-model="formModel.categoryId" style="width: 300px" filterable>
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="`${item.id} (${item.name})`"
            :value="item.id"
            :disabled="item.disabled || false"
          />
        </el-select>
      </el-form-item>
      <el-form-item prop="tagIds" :label="t('content.post.field.tagIds')">
        <el-select v-model="formModel.tagIds" multiple style="width: 100%" filterable>
          <el-option
            v-for="item in tagOptions"
            :key="item.id"
            :label="`${item.id} (${item.name})`"
            :value="item.id"
            :disabled="item.disabled || false"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('common.action.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSave">{{ t('common.action.save') }}</el-button>
    </template>
  </el-dialog>
</template>

