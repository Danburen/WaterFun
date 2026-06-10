<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import type { OptionResItem } from "@waterfun/web-core/src/types";
import { getRoleAllIds, listRoles, type RoleResp } from "~/api/role";
import type { PageOptions } from "~/types";
import { ElMessage } from "element-plus";
import BaseDialog from "~/components/BaseDialog.vue";

const props = withDefaults(defineProps<{ modelValue: boolean; disabledRoleIds?: number[] }>(), { disabledRoleIds: () => [] });
const emit = defineEmits<{ "update:modelValue": [value: boolean]; confirm: [ids: number[]] }>();

const visible = computed({ get: () => props.modelValue, set: v => emit("update:modelValue", v) });
const loading = ref(false); const tableData = ref<RoleResp[]>([]); const selectedIds = ref<Set<number>>(new Set());
const roleOptions = ref<OptionResItem<number>[]>([]);
const searchName = ref(''); const searchCode = ref(''); const searchParentId = ref<number | null>(null);
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 10, total: 0 });

const selectable = (row: RoleResp) => !props.disabledRoleIds.includes(row.id);
const toggleRow = (row: RoleResp) => { if (!selectable(row)) return; const id = row.id; if (selectedIds.value.has(id)) selectedIds.value.delete(id); else selectedIds.value.add(id); };
const allSelected = computed(() => tableData.value.length > 0 && tableData.value.filter(selectable).every(r => selectedIds.value.has(r.id)));
const toggleAll = () => { if (allSelected.value) { tableData.value.filter(selectable).forEach(r => selectedIds.value.delete(r.id)); } else { tableData.value.filter(selectable).forEach(r => selectedIds.value.add(r.id)); } };

const fetchRoleOptions = async () => { try { const res = await getRoleAllIds(); roleOptions.value = res.data || []; } catch { ElMessage.error('获取角色信息失败'); } };

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await listRoles((pageOpts.value.currentPage || 1) - 1, pageOpts.value.pageSize, searchName.value || undefined, searchCode.value || undefined, searchParentId.value ?? undefined);
    tableData.value = res.data.content || []; pageOpts.value.total = res.data.totalElements ?? res.data.page?.totalElements ?? 0;
  } catch { ElMessage.error('获取角色信息失败'); }
  finally { loading.value = false; }
};

const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleReset = () => { pageOpts.value.currentPage = 1; searchName.value = ''; searchCode.value = ''; searchParentId.value = null; fetchData(); };
const handleConfirm = () => { emit("confirm", [...selectedIds.value]); visible.value = false; };

watch(() => visible.value, async (open) => { if (!open) return; selectedIds.value = new Set(); pageOpts.value.currentPage = 1; await Promise.all([fetchRoleOptions(), fetchData()]); });
</script>

<template>
  <BaseDialog v-model="visible" title="选择角色" width="960px">
    <div class="list-layout">
      <div class="search-form">
        <div class="search-field"><label>角色名称</label><input v-model="searchName" class="form-input" placeholder="请输入角色名称" @keyup.enter="handleSearch" /></div>
        <div class="search-field"><label>角色编码</label><input v-model="searchCode" class="form-input" placeholder="请输入角色编码" @keyup.enter="handleSearch" /></div>
        <div class="search-field"><label>父级角色ID</label><select v-model="searchParentId" class="form-select"><option :value="null">无</option><option v-for="item in roleOptions" :key="item.id" :value="item.id" :disabled="item.disabled">{{ item.id }} ({{ item.name }}{{ item.code ? ` 【${item.code}】` : '' }})</option></select></div>
        <div class="search-actions">
          <button class="btn btn-primary" @click="handleSearch">查询</button>
          <button class="btn" @click="handleReset">重置</button>
        </div>
      </div>

      <div class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 40px;"><input type="checkbox" :checked="allSelected" :indeterminate="!allSelected && selectedIds.size > 0" @change="toggleAll" /></th>
              <th style="width: 80px;">ID</th>
              <th>角色名称</th>
              <th>角色编码</th>
              <th style="width: 120px;">父级角色ID</th>
              <th style="width: 160px;">创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="6" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</td></tr>
            <tr v-for="row in tableData" :key="row.id" :class="{ 'row-disabled': !selectable(row) }" @click="toggleRow(row)">
              <td><input type="checkbox" :checked="selectedIds.has(row.id)" :disabled="!selectable(row)" @click.stop="toggleRow(row)" /></td>
              <td>{{ row.id }}</td>
              <td>{{ row.name }}</td>
              <td>{{ row.code }}</td>
              <td>{{ row.parentId ?? '无' }}</td>
              <td>{{ formatISOData(row.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
        <div class="pagination-wrap">
          <span class="pagination-info">共 {{ pageOpts.total }} 条</span>
          <div class="pagination">
            <button class="page-btn" :disabled="(pageOpts.currentPage || 1) <= 1" @click="pageOpts.currentPage!--; fetchData()"><i class="fa-solid fa-chevron-left"></i></button>
            <button v-for="p in Math.ceil(pageOpts.total / (pageOpts.pageSize || 10))" :key="p" :class="['page-btn', { active: (pageOpts.currentPage || 1) === p }]" @click="pageOpts.currentPage = p; fetchData()">{{ p }}</button>
            <button class="page-btn" :disabled="(pageOpts.currentPage || 1) >= Math.ceil(pageOpts.total / (pageOpts.pageSize || 10))" @click="pageOpts.currentPage!++; fetchData()"><i class="fa-solid fa-chevron-right"></i></button>
          </div>
        </div>
      </div>

      <div class="dialog-extra">
        <slot name="footer-extra" :selected-ids="[...selectedIds]" />
      </div>
    </div>
    <template #footer>
      <button class="btn" @click="visible = false">取消</button>
      <button class="btn btn-primary" :disabled="selectedIds.size === 0" @click="handleConfirm">保存</button>
    </template>
  </BaseDialog>
</template>

<style scoped>
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { text-align: left; padding: 10px 12px; font-size: 12px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; border-bottom: 1px solid var(--border); background: var(--bg); white-space: nowrap; }
.data-table td { padding: 10px 12px; font-size: 13px; color: var(--text-primary); border-bottom: 1px solid var(--border-light); vertical-align: middle; cursor: pointer; }
.data-table tr:hover td { background: var(--bg); }
.data-table tr.row-disabled td { opacity: 0.4; cursor: not-allowed; }
.pagination-wrap { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-top: 1px solid var(--border-light); }
.pagination-info { font-size: 13px; color: var(--text-muted); }
.pagination { display: flex; gap: 4px; }
.page-btn { min-width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border: 1px solid var(--border); background: var(--bg-white); border-radius: 4px; font-size: 13px; color: var(--text-secondary); cursor: pointer; }
.page-btn:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
.page-btn.active { background: var(--primary); color: white; border-color: var(--primary); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.dialog-extra { padding-top: 4px; }
</style>
