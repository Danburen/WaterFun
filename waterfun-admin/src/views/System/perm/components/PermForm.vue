<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import type { FormInstance, FormRules } from "element-plus";
import { useI18n } from "vue-i18n";
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

const { t } = useI18n();
const formRef = ref<FormInstance>();

const localModel = computed({
  get: () => props.modelValue,
  set: (value: PermFormModel) => emit("update:modelValue", value),
});

const permTypeOptions: { label: string; value: PermissionType }[] = [
  { label: "permission.type.menu", value: "MENU" },
  { label: "permission.type.button", value: "BUTTON" },
  { label: "permission.type.api", value: "API" },
  { label: "permission.type.data", value: "DATA" },
  { label: "permission.type.other", value: "OTHER" },
];

const rules: FormRules<PermFormModel> = {
  name: [
    { required: true, message: t("permission.validate.name"), trigger: "blur" },
    { min: 2, max: 50, message: t("permission.validate.nameLength"), trigger: "blur" },
  ],
  code: [{ min: 2, max: 50, message: t("permission.validate.codeLength"), trigger: "blur" }],
  resource: [
    { required: true, message: t("permission.validate.resource"), trigger: "blur" },
    { max: 255, message: t("permission.validate.resourceLength"), trigger: "blur" },
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
  <el-form ref="formRef" :model="localModel" :rules="rules" label-width="120px" status-icon>
    <el-form-item prop="name" :label="t('permission.name')">
      <el-input v-model="localModel.name" :readonly="readonly" :placeholder="t('permission.input.name')" />
    </el-form-item>

    <el-form-item prop="code" :label="t('permission.code')">
      <el-input v-model="localModel.code" :readonly="readonly" :placeholder="t('permission.input.code')" />
    </el-form-item>

    <el-form-item prop="type" :label="t('permission.type.title')">
      <el-select v-model="localModel.type" :disabled="readonly" style="width: 100%">
        <el-option v-for="item in permTypeOptions" :key="item.value" :label="t(item.label)" :value="item.value" />
      </el-select>
    </el-form-item>

    <el-form-item prop="resource" :label="t('permission.resource')">
      <el-input
        v-model="localModel.resource"
        :readonly="readonly"
        :placeholder="t('permission.input.resource')"
      />
    </el-form-item>

    <el-form-item prop="parentId" :label="t('permission.parentId')">
      <el-select
        v-model="localModel.parentId"
        clearable
        :disabled="readonly"
        :placeholder="t('permission.input.parentId')"
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

    <el-form-item prop="orderWeight" :label="t('permission.weight')">
      <el-input-number v-model="localModel.orderWeight" :disabled="readonly" :min="0" :max="99999" />
    </el-form-item>

    <el-form-item prop="description" :label="t('permission.description')">
      <el-input
        v-model="localModel.description"
        type="textarea"
        :readonly="readonly"
        :rows="4"
        :placeholder="t('permission.input.description')"
      />
    </el-form-item>

    <el-form-item prop="isSystem" :label="t('permission.isSystem')">
      <el-switch v-model="localModel.isSystem" :disabled="readonly" />
    </el-form-item>
  </el-form>
</template>

