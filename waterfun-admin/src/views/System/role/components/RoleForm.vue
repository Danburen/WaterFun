<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import type { RoleFormExpose, RoleFormModel } from "../types";

const props = withDefaults(defineProps<{
  modelValue: RoleFormModel;
  roleOptions?: OptionResItem[];
  disabledParentIds?: number[];
  readonly?: boolean;
}>(), { roleOptions: () => [], disabledParentIds: () => [], readonly: false });

const emit = defineEmits<{ "update:modelValue": [value: RoleFormModel] }>();

const localModel = computed({ get: () => props.modelValue, set: v => emit("update:modelValue", v) });

const nameErr = ref(''); const codeErr = ref('');

const optionDisabled = (item: OptionResItem) => item.disabled || props.disabledParentIds.includes(item.id);

const validate = async (): Promise<boolean> => {
  nameErr.value = ''; codeErr.value = ''; let ok = true;
  if (!localModel.value.name?.trim()) { nameErr.value = '请输入角色名称'; ok = false; }
  else if ((localModel.value.name?.length ?? 0) < 2 || (localModel.value.name?.length ?? 0) > 64) { nameErr.value = '角色名称长度需在 2-64 个字符'; ok = false; }
  if (localModel.value.code && ((localModel.value.code?.length ?? 0) < 2 || (localModel.value.code?.length ?? 0) > 64)) { codeErr.value = '角色编码长度需在 2-64 个字符'; ok = false; }
  return ok;
};

const resetFields = () => { nameErr.value = ''; codeErr.value = ''; };

defineExpose<RoleFormExpose>({ validate, resetFields });
</script>

<template>
  <div class="form-block">
    <div class="form-field">
      <label class="form-label">角色名称</label>
      <div class="form-content">
        <input v-model="localModel.name" :readonly="readonly" class="form-input" placeholder="请输入角色名称" />
        <div v-if="nameErr" class="form-error">{{ nameErr }}</div>
      </div>
    </div>
    <div class="form-field">
      <label class="form-label">角色编码</label>
      <div class="form-content">
        <input v-model="localModel.code" :readonly="readonly || localModel.isSystem" class="form-input" placeholder="请输入角色编码" />
        <div v-if="codeErr" class="form-error">{{ codeErr }}</div>
      </div>
    </div>
    <div class="form-field">
      <label class="form-label">父级角色ID</label>
      <div class="form-content">
        <select v-model="localModel.parentId" :disabled="readonly" class="form-select">
          <option :value="null">无</option>
          <option v-for="item in roleOptions" :key="item.id" :value="item.id" :disabled="optionDisabled(item)">{{ item.name }} ({{ item.code }})</option>
        </select>
      </div>
    </div>
    <div class="form-field">
      <label class="form-label">排序权重</label>
      <div class="form-content"><input v-model="localModel.orderWeight" type="number" :disabled="readonly" class="form-input" min="0" max="99999" style="max-width: 180px;" /></div>
    </div>
    <div class="form-field">
      <label class="form-label">角色描述</label>
      <div class="form-content"><textarea v-model="localModel.description" :readonly="readonly" class="form-textarea" rows="4" placeholder="请输入角色描述"></textarea></div>
    </div>
    <div class="form-field">
      <label class="form-label">系统角色</label>
      <div class="form-content">
        <label class="switch">
          <input v-model="localModel.isSystem" type="checkbox" :disabled="readonly" />
          <span class="switch-slider"></span>
        </label>
      </div>
    </div>
  </div>
</template>
