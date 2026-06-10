<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { addPermission, getPermission, updatePermission, type PermissionType } from "~/api/permission";
import PermForm from "./PermForm.vue";
import { ElMessage } from "element-plus";
import type { PermFormExpose, PermFormModel } from "../types";
import BaseDialog from "~/components/BaseDialog.vue";

const props = withDefaults(defineProps<{
  modelValue: boolean; mode: "create" | "edit"; permissionId?: number | null; permOptions?: OptionResItem[]; disabledParentIds?: number[];
}>(), { permissionId: null, permOptions: () => [], disabledParentIds: () => [] });

const emit = defineEmits<{ "update:modelValue": [value: boolean]; success: [] }>();

const permFormRef = ref<PermFormExpose>();
const submitting = ref(false);

const createEmptyFormModel = (): PermFormModel => ({ name: "", code: "", description: "", type: "MENU" as PermissionType, resource: "", parentId: null, orderWeight: 1, isSystem: false });
const permFormModel = ref<PermFormModel>(createEmptyFormModel());

const visible = computed({ get: () => props.modelValue, set: v => emit("update:modelValue", v) });

const loadEditData = async () => {
  if (props.mode !== "edit" || props.permissionId == null) return;
  try {
    const res = await getPermission(props.permissionId);
    permFormModel.value = { name: res.data.name || "", code: res.data.code || "", description: res.data.description || "", type: (res.data.type || "MENU") as PermissionType, resource: res.data.resource || "", parentId: res.data.parentId, orderWeight: res.data.orderWeight ?? null, isSystem: !!res.data.isSystem };
  } catch { ElMessage.error('获取权限信息失败'); visible.value = false; }
};

watch(() => visible.value, async (open) => { if (!open) return; if (props.mode === "create") { permFormModel.value = createEmptyFormModel(); return; } await loadEditData(); });

const handleSave = async () => {
  if (!permFormRef.value) return;
  const valid = await permFormRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    const payload = { name: permFormModel.value.name, code: permFormModel.value.code || undefined, description: permFormModel.value.description || undefined, type: permFormModel.value.type, resource: permFormModel.value.resource, parentId: permFormModel.value.parentId ?? undefined, orderWeight: permFormModel.value.orderWeight ?? undefined, isSystem: permFormModel.value.isSystem };
    if (props.mode === "create") { await addPermission({ ...payload, name: payload.name, resource: payload.resource }); ElMessage.success('权限创建成功'); }
    else if (props.permissionId != null) { await updatePermission(props.permissionId, { ...payload, id: props.permissionId, name: payload.name, resource: payload.resource }); ElMessage.success('权限更新成功'); }
    visible.value = false; emit("success");
  } catch { ElMessage.error('保存权限失败'); }
  finally { submitting.value = false; }
};

const handleClosed = () => { permFormModel.value = createEmptyFormModel(); permFormRef.value?.resetFields(); };
</script>

<template>
  <BaseDialog v-model="visible" :title="mode === 'create' ? '创建权限' : '编辑权限'" width="680px">
    <PermForm ref="permFormRef" v-model="permFormModel" :perm-options="permOptions" :disabled-parent-ids="disabledParentIds" />
    <template #footer>
      <button class="btn" @click="visible = false">取消</button>
      <button class="btn btn-primary" :disabled="submitting" @click="handleSave"><i v-if="submitting" class="fa-solid fa-spinner fa-spin"></i> 保存</button>
    </template>
  </BaseDialog>
</template>
