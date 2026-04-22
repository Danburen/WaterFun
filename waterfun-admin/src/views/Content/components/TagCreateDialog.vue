<script setup lang="ts">
import type { FormInstance, FormRules } from "element-plus";
import { useI18n } from "vue-i18n";
import { createTag, getTag, putTag, type CreateTagRequest } from "~/api/tag";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    mode?: "create" | "edit";
    tagId?: number;
  }>(),
  {
    mode: "create",
    tagId: 0,
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  success: [];
}>();

const { t } = useI18n();
const formRef = ref<FormInstance>();
const submitting = ref(false);

const formModel = ref<CreateTagRequest>({
  name: "",
  slug: "",
  description: "",
});

const rules: FormRules<CreateTagRequest> = {
  name: [
    { required: true, message: t("content.tag.input.name"), trigger: "blur" },
    { max: 30, message: "Max 30", trigger: "blur" },
  ],
  slug: [{ max: 50, message: "Max 50", trigger: "blur" }],
  description: [{ max: 500, message: "Max 500", trigger: "blur" }],
};

const dialogTitle = computed(() =>
  props.mode === "edit" ? t("content.tag.edit") : t("content.tag.create")
);

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});

const loadDetail = async () => {
  if (props.mode !== "edit" || !props.tagId) return;
  try {
    const res = await getTag(props.tagId);
    formModel.value = {
      name: res.data.name || "",
      slug: res.data.slug || "",
      description: res.data.description || "",
    };
  } catch (e) {
    console.error(e);
    ElMessage.error(t("error.fetch"));
  }
};

watch(
  () => visible.value,
  (open) => {
    if (open) {
      loadDetail();
    }
  }
);

const resetForm = () => {
  formModel.value = {
    name: "",
    slug: "",
    description: "",
  };
  formRef.value?.resetFields();
};

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const payload = {
      name: formModel.value.name,
      slug: formModel.value.slug || undefined,
      description: formModel.value.description || undefined,
    };

    if (props.mode === "edit" && props.tagId) {
      await putTag(props.tagId, payload);
      ElMessage.success(t("content.tag.success.update"));
    } else {
      await createTag(payload);
      ElMessage.success(t("content.tag.success.create"));
    }

    visible.value = false;
    emit("success");
  } catch (e) {
    console.error(e);
    ElMessage.error(props.mode === "edit" ? t("content.tag.error.save") : t("content.tag.error.create"));
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="560" destroy-on-close @closed="resetForm">
    <el-form ref="formRef" :model="formModel" :rules="rules" label-width="100px" status-icon>
      <el-form-item prop="name" :label="t('content.tag.field.name')">
        <el-input v-model="formModel.name" :placeholder="t('content.tag.input.name')" />
      </el-form-item>
      <el-form-item prop="slug" :label="t('content.tag.field.slug')">
        <el-input v-model="formModel.slug" :placeholder="t('content.tag.input.slug')" />
      </el-form-item>
      <el-form-item prop="description" :label="t('content.tag.field.description')">
        <el-input v-model="formModel.description" type="textarea" :rows="4" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('common.action.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSave">{{ t('common.action.save') }}</el-button>
    </template>
  </el-dialog>
</template>

