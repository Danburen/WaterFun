<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import type { PermissionType } from "~/api/permission";
import type { PermFormExpose, PermFormModel } from "../types";

const props = withDefaults(defineProps<{
  modelValue: PermFormModel;
  permOptions?: OptionResItem[];
  disabledParentIds?: number[];
  readonly?: boolean;
}>(), { permOptions: () => [], disabledParentIds: () => [], readonly: false });

const emit = defineEmits<{ "update:modelValue": [value: PermFormModel] }>();

const localModel = computed({ get: () => props.modelValue, set: v => emit("update:modelValue", v) });

const permTypeOptions: { label: string; value: PermissionType }[] = [
  { label: "菜单", value: "MENU" }, { label: "按钮", value: "BUTTON" }, { label: "接口", value: "API" }, { label: "数据", value: "DATA" }, { label: "其他", value: "OTHER" },
];

const nameErr = ref(''); const codeErr = ref(''); const resourceErr = ref('');

const optionDisabled = (item: OptionResItem) => item.disabled || props.disabledParentIds.includes(item.id);

const validate = async (): Promise<boolean> => {
  nameErr.value = ''; codeErr.value = ''; resourceErr.value = ''; let ok = true;
  if (!localModel.value.name?.trim()) { nameErr.value = '请输入权限名称'; ok = false; }
  else if ((localModel.value.name?.length ?? 0) < 2 || (localModel.value.name?.length ?? 0) > 50) { nameErr.value = '权限名称长度需在 2-50 个字符'; ok = false; }
  if (localModel.value.code && ((localModel.value.code?.length ?? 0) < 2 || (localModel.value.code?.length ?? 0) > 50)) { codeErr.value = '权限编码长度需在 2-50 个字符'; ok = false; }
  if (!localModel.value.resource?.trim()) { resourceErr.value = '请输入资源标识'; ok = false; }
  else if ((localModel.value.resource?.length ?? 0) > 255) { resourceErr.value = '资源标识长度不能超过 255 个字符'; ok = false; }
  return ok;
};

const resetFields = () => { nameErr.value = ''; codeErr.value = ''; resourceErr.value = ''; };

defineExpose<PermFormExpose>({ validate, resetFields });
</script>

<template>
  <div class="form-block">
    <div class="form-field">
      <label class="form-label">权限名称</label>
      <div class="form-content">
        <input v-model="localModel.name" :readonly="readonly" class="form-input" placeholder="请输入权限名称" />
        <div v-if="nameErr" class="form-error">{{ nameErr }}</div>
      </div>
    </div>
    <div class="form-field">
      <label class="form-label">权限编码</label>
      <div class="form-content">
        <input v-model="localModel.code" :readonly="readonly" class="form-input" placeholder="请输入权限编码" />
        <div v-if="codeErr" class="form-error">{{ codeErr }}</div>
      </div>
    </div>
    <div class="form-field">
      <label class="form-label">权限类型</label>
      <div class="form-content">
        <select v-model="localModel.type" :disabled="readonly" class="form-select">
          <option v-for="item in permTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </div>
    </div>
    <div class="form-field">
      <label class="form-label">资源标识</label>
      <div class="form-content">
        <input v-model="localModel.resource" :readonly="readonly" class="form-input" placeholder="请输入资源标识" />
        <div v-if="resourceErr" class="form-error">{{ resourceErr }}</div>
      </div>
    </div>
    <div class="form-field">
      <label class="form-label">父级权限ID</label>
      <div class="form-content">
        <select v-model="localModel.parentId" :disabled="readonly" class="form-select">
          <option :value="null">无</option>
          <option v-for="item in permOptions" :key="item.id" :value="item.id" :disabled="optionDisabled(item)">{{ item.name }} ({{ item.code }})</option>
        </select>
      </div>
    </div>
    <div class="form-field">
      <label class="form-label">排序权重</label>
      <div class="form-content"><input v-model="localModel.orderWeight" type="number" :disabled="readonly" class="form-input" min="0" max="99999" style="max-width: 180px;" /></div>
    </div>
    <div class="form-field">
      <label class="form-label">权限描述</label>
      <div class="form-content"><textarea v-model="localModel.description" :readonly="readonly" class="form-textarea" rows="4" placeholder="请输入权限描述" /></div>
    </div>
    <div class="form-field">
      <label class="form-label">系统权限</label>
      <div class="form-content">
        <label class="switch">
          <input v-model="localModel.isSystem" type="checkbox" :disabled="readonly" />
          <span class="switch-slider"></span>
        </label>
      </div>
    </div>
  </div>
</template>
