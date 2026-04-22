<script setup lang="ts">
import type { FormInstance, FormRules } from "element-plus";
import { useI18n } from "vue-i18n";
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { createCategory, getCategory, getCategoryOptions, putCategory, type CreateCategoryRequest } from "~/api/category";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    mode?: "create" | "edit";
    categoryId?: number;
  }>(),
  {
    mode: "create",
    categoryId: 0,
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

const formModel = ref<CreateCategoryRequest>({
  name: "",
  slug: "",
  description: "",
  parentId: undefined,
  sortOrder: 0,
  isActive: true,
});

const rules: FormRules<CreateCategoryRequest> = {
  name: [
    { required: true, message: t("content.category.input.name"), trigger: "blur" },
    { max: 50, message: "Max 50", trigger: "blur" },
  ],
  slug: [{ max: 50, message: "Max 50", trigger: "blur" }],
  description: [{ max: 500, message: "Max 500", trigger: "blur" }],
};

const dialogTitle = computed(() =>
  props.mode === "edit" ? t("content.category.edit") : t("content.category.create")
);

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});

const loadOptions = async () => {
  loadingOptions.value = true;
  try {
    const res = await getCategoryOptions();
    categoryOptions.value = res.data || [];
  } catch (e) {
    console.error(e);
    ElMessage.error(t("error.fetch"));
  } finally {
    loadingOptions.value = false;
  }
};

const loadDetail = async () => {
  if (props.mode !== "edit" || !props.categoryId) return;
  try {
    const res = await getCategory(props.categoryId);
    formModel.value = {
      name: res.data.name || "",
      slug: res.data.slug || "",
      description: res.data.description || "",
      parentId: res.data.parentId,
      sortOrder: res.data.sortOrder || 0,
      isActive: res.data.isActive !== false,
    };
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

const resetForm = () => {
  formModel.value = {
    name: "",
    slug: "",
    description: "",
    parentId: undefined,
    sortOrder: 0,
    isActive: true,
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
      parentId: formModel.value.parentId,
      sortOrder: formModel.value.sortOrder,
      isActive: formModel.value.isActive,
    };

    if (props.mode === "edit" && props.categoryId) {
      await putCategory(props.categoryId, payload);
      ElMessage.success(t("content.category.success.update"));
    } else {
      await createCategory(payload);
      ElMessage.success(t("content.category.success.create"));
    }

    visible.value = false;
    emit("success");
  } catch (e) {
    console.error(e);
    ElMessage.error(props.mode === "edit" ? t("content.category.error.save") : t("content.category.error.create"));
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="620" destroy-on-close @closed="resetForm">
    <el-form ref="formRef" :model="formModel" :rules="rules" label-width="110px" status-icon v-loading="loadingOptions">
      <el-form-item prop="name" :label="t('content.category.field.name')">
        <el-input v-model="formModel.name" :placeholder="t('content.category.input.name')" />
      </el-form-item>
      <el-form-item prop="slug" :label="t('content.category.field.slug')">
        <el-input v-model="formModel.slug" :placeholder="t('content.category.input.slug')" />
      </el-form-item>
      <el-form-item prop="parentId" :label="t('content.category.field.parentId')">
        <el-select v-model="formModel.parentId" clearable filterable style="width: 280px">
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="`${item.id} (${item.name})`"
            :value="item.id"
            :disabled="item.disabled || false"
          />
        </el-select>
      </el-form-item>
      <el-form-item prop="sortOrder" :label="t('content.category.field.sortOrder')">
        <el-input-number v-model="formModel.sortOrder" :min="0" :max="99999" />
      </el-form-item>
      <el-form-item prop="isActive" :label="t('content.category.field.isActive')">
        <el-switch v-model="formModel.isActive" />
      </el-form-item>
      <el-form-item prop="description" :label="t('content.category.field.description')">
        <el-input v-model="formModel.description" type="textarea" :rows="4" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('common.action.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSave">{{ t('common.action.save') }}</el-button>
    </template>
  </el-dialog>
</template>

