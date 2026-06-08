<script setup lang="ts">
import type { FormInstance, FormRules } from "element-plus";

import type { OptionResItem } from "@waterfun/web-core/src/types";
import { createCategory, getCategory, getCategoryOptions, putCategory, type CreateCategoryRequest } from "~/api/category";
import { ElMessage } from "element-plus";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    mode?: "create" | "edit";
    categoryId?: number | string;
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
    { required: true, message: '请输入分类名', trigger: "blur" },
    { max: 50, message: "Max 50", trigger: "blur" },
  ],
  slug: [{ max: 50, message: "Max 50", trigger: "blur" }],
  description: [{ max: 500, message: "Max 500", trigger: "blur" }],
};

const dialogTitle = computed(() =>
  props.mode === "edit" ? '编辑分类' : '新增分类'
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
    ElMessage.error('获取数据失败');
  } finally {
    loadingOptions.value = false;
  }
};

const loadDetail = async () => {
  if (props.mode !== "edit" || !props.categoryId) return;
  try {
    const res = await getCategory(Number(props.categoryId));
    formModel.value = {
      name: res.data.name || "",
      slug: res.data.slug || "",
      description: res.data.description || "",
      parentId: res.data.parentId != null ? String(res.data.parentId) : undefined,
      sortOrder: res.data.sortOrder || 0,
      isActive: res.data.isActive !== false,
    };
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

    if (props.mode === "edit" && props.categoryId != null && props.categoryId !== "") {
      await putCategory(Number(props.categoryId), payload);
      ElMessage.success('分类更新成功');
    } else {
      await createCategory(payload);
      ElMessage.success('分类创建成功');
    }

    visible.value = false;
    emit("success");
  } catch (e) {
    console.error(e);
    ElMessage.error(props.mode === "edit" ? '分类保存失败' : '分类创建失败');
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="620" destroy-on-close @closed="resetForm">
    <el-form ref="formRef" v-loading="loadingOptions" :model="formModel" :rules="rules" label-width="110px" status-icon>
      <el-form-item prop="name" label="分类名">
        <el-input v-model="formModel.name" placeholder="请输入分类名"/>
      </el-form-item>
      <el-form-item prop="slug" label="唯一标识符">
        <el-input v-model="formModel.slug" placeholder="请输入唯一标识符"/>
      </el-form-item>
      <el-form-item prop="parentId" label="父级ID">
        <el-select v-model="formModel.parentId" clearable filterable style="width: 280px">
          <el-option v-for="item in categoryOptions" :key="item.id" :label="`${item.id} (${item.name})`" :value="item.id" :disabled="item.disabled || false"/>
        </el-select>
      </el-form-item>
      <el-form-item prop="sortOrder" label="排序">
        <el-input-number v-model="formModel.sortOrder" :min="0" :max="99999"/>
      </el-form-item>
      <el-form-item prop="isActive" label="是否启用">
        <el-switch v-model="formModel.isActive"/>
      </el-form-item>
      <el-form-item prop="description" label="描述">
        <el-input v-model="formModel.description" type="textarea" :rows="4"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

