<script setup lang="ts">
import { createTag, getTag, putTag } from "~/api/tag";
import { ElMessage } from "element-plus";
import BaseDialog from "~/components/BaseDialog.vue";

const props = withDefaults(defineProps<{
  modelValue: boolean;
  mode?: "create" | "edit";
  tagId?: number;
}>(), { mode: "create", tagId: 0 });

const emit = defineEmits<{ "update:modelValue": [value: boolean]; success: [] }>();

const submitting = ref(false);
const name = ref(''); const slug = ref(''); const description = ref('');
const nameErr = ref(''); const slugErr = ref(''); const descErr = ref('');

const dialogTitle = computed(() => props.mode === "edit" ? '编辑标签' : '新增标签');

const visible = computed({ get: () => props.modelValue, set: v => emit("update:modelValue", v) });

const loadDetail = async () => {
  if (props.mode !== "edit" || !props.tagId) return;
  try { const res = await getTag(props.tagId); name.value = res.data.name || ''; slug.value = res.data.slug || ''; description.value = res.data.description || ''; }
  catch { ElMessage.error('获取数据失败'); }
};

watch(() => visible.value, (open) => { if (open) { resetForm(); loadDetail(); } });

const resetForm = () => { name.value = ''; slug.value = ''; description.value = ''; clearErrs(); };
const clearErrs = () => { nameErr.value = ''; slugErr.value = ''; descErr.value = ''; };

const validate = () => {
  clearErrs(); let ok = true;
  if (!name.value.trim()) { nameErr.value = '请输入标签名'; ok = false; }
  else if (name.value.length > 30) { nameErr.value = 'Max 30'; ok = false; }
  if (slug.value.length > 50) { slugErr.value = 'Max 50'; ok = false; }
  if (description.value.length > 500) { descErr.value = 'Max 500'; ok = false; }
  return ok;
};

const handleSave = async () => {
  if (!validate()) return;
  submitting.value = true;
  try {
    const payload = { name: name.value.trim(), slug: slug.value.trim() || undefined, description: description.value.trim() || undefined };
    if (props.mode === "edit" && props.tagId) { await putTag(props.tagId, payload); ElMessage.success('标签更新成功'); }
    else { await createTag(payload); ElMessage.success('标签创建成功'); }
    visible.value = false; emit("success");
  } catch { ElMessage.error(props.mode === "edit" ? '标签保存失败' : '标签创建失败'); }
  finally { submitting.value = false; }
};
</script>

<template>
  <BaseDialog v-model="visible" :title="dialogTitle" width="560px">
    <div class="form-block">
      <div class="form-field">
        <label class="form-label">标签名</label>
        <div class="form-content">
          <input v-model="name" class="form-input" placeholder="请输入标签名" />
          <div v-if="nameErr" class="form-error">{{ nameErr }}</div>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">唯一标识符</label>
        <div class="form-content">
          <input v-model="slug" class="form-input" placeholder="请输入唯一标识符" />
          <div v-if="slugErr" class="form-error">{{ slugErr }}</div>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">描述</label>
        <div class="form-content">
          <textarea v-model="description" class="form-textarea" rows="4"></textarea>
          <div v-if="descErr" class="form-error">{{ descErr }}</div>
        </div>
      </div>
    </div>
    <template #footer>
      <button class="btn" @click="visible = false">取消</button>
      <button class="btn btn-primary" :disabled="submitting" @click="handleSave">
        <i v-if="submitting" class="fa-solid fa-spinner fa-spin"></i> 保存
      </button>
    </template>
  </BaseDialog>
</template>
