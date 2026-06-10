<script setup lang="ts">
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { createCategory, getCategory, getCategoryOptions, putCategory } from "~/api/category";
import { ElMessage } from "element-plus";
import BaseDialog from "~/components/BaseDialog.vue";

const props = withDefaults(defineProps<{
  modelValue: boolean; mode?: "create" | "edit"; categoryId?: number | string;
}>(), { mode: "create", categoryId: 0 });

const emit = defineEmits<{ "update:modelValue": [value: boolean]; success: [] }>();

const submitting = ref(false);
const loadingOptions = ref(false);
const categoryOptions = ref<OptionResItem<number>[]>([]);
const name = ref(''); const slug = ref(''); const description = ref(''); const parentId = ref<number | undefined>(); const sortOrder = ref(0); const isActive = ref(true);
const nameErr = ref(''); const slugErr = ref(''); const descErr = ref('');

const dialogTitle = computed(() => props.mode === "edit" ? '编辑分类' : '新增分类');
const visible = computed({ get: () => props.modelValue, set: v => emit("update:modelValue", v) });

const loadOptions = async () => {
  loadingOptions.value = true;
  try { const res = await getCategoryOptions(); categoryOptions.value = res.data || []; }
  catch { ElMessage.error('获取数据失败'); }
  finally { loadingOptions.value = false; }
};

const loadDetail = async () => {
  if (props.mode !== "edit" || !props.categoryId) return;
  try {
    const res = await getCategory(Number(props.categoryId));
    name.value = res.data.name || ''; slug.value = res.data.slug || ''; description.value = res.data.description || '';
    parentId.value = res.data.parentId != null ? Number(res.data.parentId) : undefined;
    sortOrder.value = res.data.sortOrder || 0; isActive.value = res.data.isActive !== false;
  } catch { ElMessage.error('获取数据失败'); }
};

watch(() => visible.value, async (open) => { if (open) { resetForm(); await loadOptions(); await loadDetail(); } });

const resetForm = () => { name.value = ''; slug.value = ''; description.value = ''; parentId.value = undefined; sortOrder.value = 0; isActive.value = true; clearErrs(); };
const clearErrs = () => { nameErr.value = ''; slugErr.value = ''; descErr.value = ''; };

const validate = () => {
  clearErrs(); let ok = true;
  if (!name.value.trim()) { nameErr.value = '请输入分类名'; ok = false; } else if (name.value.length > 50) { nameErr.value = 'Max 50'; ok = false; }
  if (slug.value.length > 50) { slugErr.value = 'Max 50'; ok = false; }
  if (description.value.length > 500) { descErr.value = 'Max 500'; ok = false; }
  return ok;
};

const handleSave = async () => {
  if (!validate()) return;
  submitting.value = true;
  try {
    const payload = { name: name.value.trim(), slug: slug.value.trim() || undefined, description: description.value.trim() || undefined, parentId: parentId.value, sortOrder: sortOrder.value, isActive: isActive.value };
    if (props.mode === "edit" && props.categoryId != null && props.categoryId !== "") { await putCategory(Number(props.categoryId), payload); ElMessage.success('分类更新成功'); }
    else { await createCategory(payload); ElMessage.success('分类创建成功'); }
    visible.value = false; emit("success");
  } catch { ElMessage.error(props.mode === "edit" ? '分类保存失败' : '分类创建失败'); }
  finally { submitting.value = false; }
};
</script>

<template>
  <BaseDialog v-model="visible" :title="dialogTitle" width="620px">
    <div v-if="loadingOptions" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
    <div v-else class="form-block">
      <div class="form-field">
        <label class="form-label">分类名</label>
        <div class="form-content"><input v-model="name" class="form-input" placeholder="请输入分类名" /><div v-if="nameErr" class="form-error">{{ nameErr }}</div></div>
      </div>
      <div class="form-field">
        <label class="form-label">唯一标识符</label>
        <div class="form-content"><input v-model="slug" class="form-input" placeholder="请输入唯一标识符" /><div v-if="slugErr" class="form-error">{{ slugErr }}</div></div>
      </div>
      <div class="form-field">
        <label class="form-label">父级ID</label>
        <div class="form-content">
          <select v-model="parentId" class="form-select">
            <option :value="undefined">无</option>
            <option v-for="item in categoryOptions" :key="item.id" :value="item.id" :disabled="item.disabled">{{ item.id }} ({{ item.name }})</option>
          </select>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">排序</label>
        <div class="form-content"><input v-model="sortOrder" type="number" class="form-input" min="0" max="99999" style="width: 180px;" /></div>
      </div>
      <div class="form-field">
        <label class="form-label">是否启用</label>
        <div class="form-content">
          <label class="switch">
            <input v-model="isActive" type="checkbox" />
            <span class="switch-slider"></span>
          </label>
        </div>
      </div>
      <div class="form-field">
        <label class="form-label">描述</label>
        <div class="form-content"><textarea v-model="description" class="form-textarea" rows="4"></textarea><div v-if="descErr" class="form-error">{{ descErr }}</div></div>
      </div>
    </div>
    <template #footer>
      <button class="btn" @click="visible = false">取消</button>
      <button class="btn btn-primary" :disabled="submitting" @click="handleSave"><i v-if="submitting" class="fa-solid fa-spinner fa-spin"></i> 保存</button>
    </template>
  </BaseDialog>
</template>
