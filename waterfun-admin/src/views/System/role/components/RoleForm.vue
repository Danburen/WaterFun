<script setup lang="ts">
import type {OptionResItem} from "@waterfun/web-core/src/types";

import type {FormInstance, FormRules} from "element-plus";
import type {RoleFormExpose, RoleFormModel} from "../types";

const props = withDefaults(defineProps<{
  modelValue: RoleFormModel;
  roleOptions?: OptionResItem[];
  disabledParentIds?: number[];
  readonly?: boolean;
}>(), {
  roleOptions: () => [],
  disabledParentIds: () => [],
  readonly: false,
});

const emit = defineEmits<{
  "update:modelValue": [value: RoleFormModel];
}>();


const formRef = ref<FormInstance>();

const localModel = computed({
  get: () => props.modelValue,
  set: (value: RoleFormModel) => emit("update:modelValue", value),
});

const rules: FormRules<RoleFormModel> = {
  name: [
    {required: true, message: '请输入角色名称', trigger: "blur"},
    {min: 2, max: 64, message: '角色名称长度需在 2-64 个字符', trigger: "blur"},
  ],
  code: [
    {required: false, message: '请输入角色编码', trigger: "blur"},
    {min: 2, max: 64, message: '角色编码长度需在 2-64 个字符', trigger: "blur"},
  ],
};

const optionDisabled = (item: OptionResItem) => {
  if (item.disabled) return true;
  return props.disabledParentIds.includes(item.id);
};

defineExpose<RoleFormExpose>({
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
    label-width="110px"
    status-icon
  >
    <el-form-item
      prop="name"
      label="角色名称"
    >
      <el-input
        v-model="localModel.name"
        :readonly="readonly "
        placeholder="请输入角色名称"
      />
    </el-form-item>
    <el-form-item
      prop="code"
      label="角色编码"
    >
      <el-input
        v-model="localModel.code"
        :readonly="readonly || localModel.isSystem"
        placeholder="请输入角色编码"
      />
    </el-form-item>
    <el-form-item
      prop="parentId"
      label="父级角色ID"
    >
      <el-select
        v-model="localModel.parentId"
        clearable
        :disabled="readonly"
        placeholder="请选择父级角色"
        style="width: 100%"
      >
        <el-option
          v-for="item in roleOptions"
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
        style="width: 180px"
      />
    </el-form-item>
    <el-form-item
      prop="description"
      label="角色描述"
    >
      <el-input
        v-model="localModel.description"
        type="textarea"
        :readonly="readonly"
        :rows="4"
        placeholder="请输入角色描述"
      />
    </el-form-item>
    <el-form-item
      prop="isSystem"
      label="系统角色"
    >
      <el-switch
        v-model="localModel.isSystem"
        :disabled="readonly"
      />
    </el-form-item>
  </el-form>
</template>

