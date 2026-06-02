<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";

import { addRole, getRole, updateRole } from "~/api/role";
import RoleForm from "./RoleForm.vue";
import type { RoleFormExpose, RoleFormModel } from "../types";
import { ElMessage } from "element-plus";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    mode: "create" | "edit";
    roleId?: number | null;
    roleOptions?: OptionResItem[];
    disabledParentIds?: number[];
  }>(),
  {
    roleId: null,
    roleOptions: () => [],
    disabledParentIds: () => [],
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  success: [];
}>();


const roleFormRef = ref<RoleFormExpose>();
const submitting = ref(false);

const createEmptyFormModel = (): RoleFormModel => ({
  name: "",
  code: "",
  description: "",
  parentId: null,
  orderWeight: 1,
  isSystem: false,
});

const roleFormModel = ref<RoleFormModel>(createEmptyFormModel());

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});

const loadEditData = async () => {
  if (props.mode !== "edit" || props.roleId == null) return;
  const res = await getRole(props.roleId);
  roleFormModel.value = {
    name: res.data.name || "",
    code: res.data.code || "",
    description: res.data.description || "",
    parentId: res.data.parentId,
    orderWeight: res.data.orderWeight ?? null,
    isSystem: !!res.data.isSystem,
  };
};

watch(
  () => visible.value,
  async (open) => {
    if (!open) return;
    if (props.mode === "create") {
      roleFormModel.value = createEmptyFormModel();
      return;
    }
    try {
      await loadEditData();
    } catch (e) {
      console.error(e);
      ElMessage.error('获取角色信息失败');
      visible.value = false;
    }
  }
);

const handleSave = async () => {
  if (!roleFormRef.value) return;
  const valid = await roleFormRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const payload = {
      name: roleFormModel.value.name,
      code: roleFormModel.value.code,
      description: roleFormModel.value.description || undefined,
      parentId: roleFormModel.value.parentId ?? undefined,
      orderWeight: roleFormModel.value.orderWeight ?? undefined,
      isSystem: roleFormModel.value.isSystem,
    };

    if (props.mode === "create") {
      await addRole(payload);
      ElMessage.success('角色创建成功');
    } else if (props.roleId != null) {
      await updateRole(props.roleId, payload);
      ElMessage.success('角色更新成功');
    }

    visible.value = false;
    emit("success");
  } catch (e) {
    console.error(e);
    ElMessage.error('保存角色失败');
  } finally {
    submitting.value = false;
  }
};

const handleClosed = () => {
  roleFormModel.value = createEmptyFormModel();
  roleFormRef.value?.resetFields();
};
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="mode === 'create' ? '创建角色' : '编辑角色'"
    width="640"
    destroy-on-close
    @closed="handleClosed"
  >
    <RoleForm
      ref="roleFormRef"
      v-model="roleFormModel"
      :role-options="roleOptions"
      :disabled-parent-ids="disabledParentIds"
    />
    <template #footer>
      <el-button @click="visible = false">
        取消
      </el-button>
      <el-button
        type="primary"
        :loading="submitting"
        @click="handleSave"
      >
        保存
      </el-button>
    </template>
  </el-dialog>
</template>


