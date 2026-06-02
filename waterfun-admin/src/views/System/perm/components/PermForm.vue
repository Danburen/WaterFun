<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import type { FormInstance, FormRules } from "element-plus";
import type { PermissionType } from "~/api/permission";
import type { PermFormExpose, PermFormModel } from "../types";

const props = withDefaults(
  defineProps<{
    modelValue: PermFormModel;
    permOptions?: OptionResItem[];
    disabledParentIds?: number[];
    readonly?: boolean;
  }>(),
  {
    permOptions: () => [],
    disabledParentIds: () => [],
    readonly: false,
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: PermFormModel];
}>();

const formRef = ref<FormInstance>();

const localModel = computed({
  get: () => props.modelValue,
  set: (value: PermFormModel) => emit("update:modelValue", value),
});

const permTypeOptions: { label: string; value: PermissionType }[] = [
  { label: "菜单", value: "MENU" },
  { label: "按钮", value: "BUTTON" },
  { label: "接口", value: "API" },
  { label: "数据", value: "DATA" },
  { label: "其他", value: "OTHER" },
];

const rules: FormRules<PermFormModel> = {
  name: [
    { required: true, message: '请输入权限名称', trigger: "blur" },
    { min: 2, max: 50, message: '权限名称长度需在 2-50 个字符', trigger: "blur" },
  ],
  code: [{ min: 2, max: 50, message: '权限编码长度需在 2-50 个字符', trigger: "blur" }],
  resource: [
    { required: true, message: '请输入资源标识', trigger: "blur" },
    { max: 255, message: '资源标识长度不能超过 255 个字符', trigger: "blur" },
  ],
};

const optionDisabled = (item: OptionResItem) => {
  if (item.disabled) return true;
  return props.disabledParentIds.includes(item.id);
};

defineExpose<PermFormExpose>({
  validate: async () => {
    if (!formRef.value) return false;
    return formRef.value.validate().catch(() => false);
  },
  resetFields: () => {
    formRef.value?.resetFields();
  },
});
</script>

<template>
  <el-form
    ref="formRef"
    :model="localModel"
    :rules="rules"
    label-width="120px"
    status-icon
  >
    <el-form-item
      prop="name"
      label="权限名称"
    >
      <el-input
        v-model="localModel.name"
        :readonly="readonly"
        placeholder="请输入权限名称"
      />
    </el-form-item>

    <el-form-item
      prop="code"
      label="权限编码"
    >
      <el-input
        v-model="localModel.code"
        :readonly="readonly"
        placeholder="请输入权限编码"
      />
    </el-form-item>

    <el-form-item
      prop="type"
      label="权限类型"
    >
      <el-select
        v-model="localModel.type"
        :disabled="readonly"
        style="width: 100%"
      >
        <el-option
          v-for="item in permTypeOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </el-form-item>

    <el-form-item
      prop="resource"
      label="资源标识"
    >
      <el-input
        v-model="localModel.resource"
        :readonly="readonly"
        placeholder="请输入资源标识"
      />
    </el-form-item>

    <el-form-item
      prop="parentId"
      label="父级权限ID"
    >
      <el-select
        v-model="localModel.parentId"
        clearable
        :disabled="readonly"
        placeholder="请选择父级权限"
        style="width: 100%"
      >
        <el-option
          v-for="item in permOptions"
          :key="item.id"
          :label="`${item.name} (${item.code})`"
          :value="item.id"
          :disabled="optionDisabled(item)"
        />
      </el-select>
    </el-form-item>

    <el-form-item
      prop="orderWeight"
      label="排序权重"
    >
      <el-input-number
        v-model="localModel.orderWeight"
        :disabled="readonly"
        :min="0"
        :max="99999"
      />
    </el-form-item>

    <el-form-item
      prop="description"
      label="权限描述"
    >
      <el-input
        v-model="localModel.description"
        type="textarea"
        :readonly="readonly"
        :rows="4"
        placeholder="请输入权限描述"
      />
    </el-form-item>

    <el-form-item
      prop="isSystem"
      label="系统权限"
    >
      <el-switch
        v-model="localModel.isSystem"
        :disabled="readonly"
      />
    </el-form-item>
  </el-form>
</template>

