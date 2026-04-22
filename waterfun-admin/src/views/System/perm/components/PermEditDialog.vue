<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { useI18n } from "vue-i18n";
import { addPermission, getPermission, updatePermission, type PermissionType } from "~/api/permission";
import PermForm from "./PermForm.vue";
import type { PermFormExpose, PermFormModel } from "../types";

const props = withDefaults(
  defineProps<{
	modelValue: boolean;
	mode: "create" | "edit";
	permissionId?: number | null;
	permOptions?: OptionResItem[];
	disabledParentIds?: number[];
  }>(),
  {
	permissionId: null,
	permOptions: () => [],
	disabledParentIds: () => [],
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  success: [];
}>();

const { t } = useI18n();
const permFormRef = ref<PermFormExpose>();
const submitting = ref(false);

const createEmptyFormModel = (): PermFormModel => ({
  name: "",
  code: "",
  description: "",
  type: "MENU",
  resource: "",
  parentId: null,
  orderWeight: 1,
  isSystem: false,
});

const permFormModel = ref<PermFormModel>(createEmptyFormModel());

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});

const loadEditData = async () => {
  if (props.mode !== "edit" || props.permissionId == null) return;
  const res = await getPermission(props.permissionId);
  permFormModel.value = {
	name: res.data.name || "",
	code: res.data.code || "",
	description: res.data.description || "",
	type: (res.data.type || "MENU") as PermissionType,
	resource: res.data.resource || "",
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
	  permFormModel.value = createEmptyFormModel();
	  return;
	}
	try {
	  await loadEditData();
	} catch (e) {
	  console.error(e);
	  ElMessage.error(t("permission.error.fetch"));
	  visible.value = false;
	}
  }
);

const handleSave = async () => {
  if (!permFormRef.value) return;
  const valid = await permFormRef.value.validate().catch(() => false);
  if (!valid) return;

  const payload = {
	id: props.permissionId ?? undefined,
	name: permFormModel.value.name,
	code: permFormModel.value.code || undefined,
	description: permFormModel.value.description || undefined,
	type: permFormModel.value.type,
	resource: permFormModel.value.resource,
	parentId: permFormModel.value.parentId ?? undefined,
	orderWeight: permFormModel.value.orderWeight ?? undefined,
	isSystem: permFormModel.value.isSystem,
  };

  submitting.value = true;
  try {
	if (props.mode === "create") {
	  await addPermission({ ...payload, name: payload.name, resource: payload.resource });
	  ElMessage.success(t("permission.success.create"));
	} else if (props.permissionId != null) {
	  await updatePermission(props.permissionId, {
		...payload,
		id: props.permissionId,
		name: payload.name,
		resource: payload.resource,
	  });
	  ElMessage.success(t("permission.success.update"));
	}
	visible.value = false;
	emit("success");
  } catch (e) {
	console.error(e);
	ElMessage.error(t("permission.error.save"));
  } finally {
	submitting.value = false;
  }
};

const handleClosed = () => {
  permFormModel.value = createEmptyFormModel();
  permFormRef.value?.resetFields();
};
</script>

<template>
  <el-dialog
	v-model="visible"
	:title="mode === 'create' ? t('permission.create') : t('permission.edit')"
	width="680"
	destroy-on-close
	@closed="handleClosed"
  >
	<PermForm
	  ref="permFormRef"
	  v-model="permFormModel"
	  :perm-options="permOptions"
	  :disabled-parent-ids="disabledParentIds"
	/>
	<template #footer>
	  <el-button @click="visible = false">{{ t('common.action.cancel') }}</el-button>
	  <el-button type="primary" :loading="submitting" @click="handleSave">{{ t('common.action.save') }}</el-button>
	</template>
  </el-dialog>
</template>


