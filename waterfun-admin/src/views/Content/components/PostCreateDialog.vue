<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import type { FormInstance, FormRules } from "element-plus";
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
import { ElMessage } from "element-plus";

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
  categoryId: "",
  slug: "",
  tagIds: [],
});

const statusOptions: { label: string; value: PostStatus }[] = [
  { label: "草稿", value: "DRAFT" },
  { label: "待审核", value: "PENDING" },
  { label: "已发布", value: "PUBLISHED" },
  { label: "已拒绝", value: "REJECTED" },
  { label: "已归档", value: "ARCHIVED" },
];

const visibilityOptions: { label: string; value: PostVisibility }[] = [
  { label: "公开", value: "PUBLIC" },
  { label: "私密", value: "PRIVATE" },
  { label: "粉丝可见", value: "FANS_ONLY" },
];

const rules: FormRules<CreatePostRequest> = {
  title: [{ required: true, message: '请输入标题', trigger: "blur" }],
  content: [{ required: true, message: '请输入内容', trigger: "blur" }],
  authorId: [{ required: true, message: '请选择作者', trigger: "change" }],
  categoryId: [{ required: true, message: '请选择分类', trigger: "change" }],
};

const dialogTitle = computed(() =>
  props.mode === "edit" ? '编辑文章' : '新增文章'
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
    categoryId: "",
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
    ElMessage.error('获取数据失败');
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
    categoryId: data.categoryId || "",
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
    ElMessage.error('获取数据失败');
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
      ElMessage.success('文章更新成功');
    } else {
      await createPost({
        ...(payload as CreatePostRequest),
        tagIds: formModel.value.tagIds?.length ? formModel.value.tagIds : undefined,
      });
      ElMessage.success('文章创建成功');
    }

    visible.value = false;
    emit("success");
  } catch (e) {
    console.error(e);
    ElMessage.error(props.mode === "edit" ? '文章保存失败' : '文章创建失败');
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="760" destroy-on-close @closed="resetForm">
    <el-form ref="formRef" v-loading="loadingOptions" :model="formModel" :rules="rules" label-width="110px" status-icon>
      <el-form-item prop="title" label="标题">
        <el-input v-model="formModel.title" placeholder="请输入标题"/>
      </el-form-item>
      <el-form-item prop="subtitle" label="副标题">
        <el-input v-model="formModel.subtitle" placeholder="请输入副标题"/>
      </el-form-item>
      <el-form-item prop="content" label="内容">
        <el-input v-model="formModel.content" type="textarea" :rows="6" placeholder="请输入内容"/>
      </el-form-item>
      <el-form-item prop="summary" label="摘要">
        <el-input v-model="formModel.summary" type="textarea" :rows="3" placeholder="请输入摘要"/>
      </el-form-item>
      <el-form-item prop="coverImg" label="封面图">
        <el-input v-model="formModel.coverImg" placeholder="请输入封面图URL"/>
      </el-form-item>
      <el-form-item prop="slug" label="唯一标识符">
        <el-input v-model="formModel.slug" placeholder="请输入唯一标识符"/>
      </el-form-item>
      <el-form-item prop="status" label="状态">
        <el-select v-model="formModel.status" style="width: 220px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"/>
        </el-select>
      </el-form-item>
      <el-form-item prop="visibility" label="可见性">
        <el-select v-model="formModel.visibility" style="width: 220px">
          <el-option v-for="item in visibilityOptions" :key="item.value" :label="item.label" :value="item.value"/>
        </el-select>
      </el-form-item>
      <el-form-item prop="authorId" label="作者ID">
        <el-select v-model="formModel.authorId" style="width: 300px" filterable>
          <el-option v-for="item in userOptions" :key="item.id" :label="`${item.id} (${item.name}${item.code ? ` / ${item.code}` : ''})`" :value="item.id" :disabled="item.disabled || false"/>
        </el-select>
      </el-form-item>
      <el-form-item prop="categoryId" label="分类ID">
        <el-select v-model="formModel.categoryId" style="width: 300px" filterable>
          <el-option v-for="item in categoryOptions" :key="item.id" :label="`${item.id} (${item.name})`" :value="item.id" :disabled="item.disabled || false"/>
        </el-select>
      </el-form-item>
      <el-form-item prop="tagIds" label="标签">
        <el-select v-model="formModel.tagIds" multiple style="width: 100%" filterable>
          <el-option v-for="item in tagOptions" :key="item.id" :label="`${item.id} (${item.name})`" :value="item.id" :disabled="item.disabled || false"/>
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

