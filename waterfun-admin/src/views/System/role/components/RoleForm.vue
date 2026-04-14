<script setup lang="ts">
import type {OptionResItem} from "@waterfun/web-core/src/types";
import {useI18n} from "vue-i18n";
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

const {t} = useI18n();
const formRef = ref<FormInstance>();

const localModel = computed({
  get: () => props.modelValue,
  set: (value: RoleFormModel) => emit("update:modelValue", value),
});

const rules: FormRules<RoleFormModel> = {
  name: [
    {required: true, message: t("role.validate.name"), trigger: "blur"},
    {min: 2, max: 64, message: t("role.validate.nameLength"), trigger: "blur"},
  ],
  code: [
    {required: false, message: t("role.validate.code"), trigger: "blur"},
    {min: 2, max: 64, message: t("role.validate.codeLength"), trigger: "blur"},
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
  <el-form ref="formRef" :model="localModel" :rules="rules" label-width="110px" status-icon>
    <el-form-item prop="name" :label="t('role.name')">
      <el-input v-model="localModel.name" :readonly="readonly " :placeholder="t('role.input.name')" />
    </el-form-item>
    <el-form-item prop="code" :label="t('role.code')">
      <el-input v-model="localModel.code" :readonly="readonly || localModel.isSystem" :placeholder="t('role.input.code')" />
    </el-form-item>
    <el-form-item prop="parentId" :label="t('role.parentId')">
      <el-select
        v-model="localModel.parentId"
        clearable
        :disabled="readonly"
        :placeholder="t('role.input.parentId')"
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
    <el-form-item prop="orderWeight" :label="t('role.weight')">
      <el-input-number
        v-model="localModel.orderWeight"
        :disabled="readonly"
        :min="0"
        :max="99999"
        style="width: 180px"
      />
    </el-form-item>
    <el-form-item prop="description" :label="t('role.description')">
      <el-input
        v-model="localModel.description"
        type="textarea"
        :readonly="readonly"
        :rows="4"
        :placeholder="t('role.input.description')"
      />
    </el-form-item>
    <el-form-item prop="isSystem" :label="t('role.isSystem')">
      <el-switch v-model="localModel.isSystem" :disabled="readonly" />
    </el-form-item>
  </el-form>
</template>

