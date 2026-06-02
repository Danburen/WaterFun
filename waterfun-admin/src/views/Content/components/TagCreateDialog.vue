<script setup lang="ts">
import type { FormInstance, FormRules } from "element-plus";

import { createTag, getTag, putTag, type CreateTagRequest } from "~/api/tag";
import { ElMessage } from "element-plus";

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


const formRef = ref<FormInstance>();
const submitting = ref(false);

const formModel = ref<CreateTagRequest>({
  name: "",
  slug: "",
  description: "",
});

const rules: FormRules<CreateTagRequest> = {
  name: [
    { required: true, message: '请输入标签名', trigger: "blur" },
    { max: 30, message: "Max 30", trigger: "blur" },
  ],
  slug: [{ max: 50, message: "Max 50", trigger: "blur" }],
  description: [{ max: 500, message: "Max 500", trigger: "blur" }],
};

const dialogTitle = computed(() =>
  props.mode === "edit" ? '编辑标签' : '新增标签'
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
    ElMessage.error('获取数据失败');
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
      ElMessage.success('标签更新成功');
    } else {
      await createTag(payload);
      ElMessage.success('标签创建成功');
    }

    visible.value = false;
    emit("success");
  } catch (e) {
    console.error(e);
    ElMessage.error(props.mode === "edit" ? '标签保存失败' : '标签创建失败');
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="560" destroy-on-close @closed="resetForm">
    <el-form ref="formRef" :model="formModel" :rules="rules" label-width="100px" status-icon>
      <el-form-item prop="name" label="标签名">
        <el-input v-model="formModel.name" placeholder="请输入标签名" />
      </el-form-item>
      <el-form-item prop="slug" label="唯一标识符">
        <el-input v-model="formModel.slug" placeholder="请输入唯一标识符" />
      </el-form-item>
      <el-form-item prop="description" label="描述">
        <el-input v-model="formModel.description" type="textarea" :rows="4" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

