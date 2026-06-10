<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { getUserList, type AccountStatus, type UserAdminDto } from "~/api/user";
import type { PageOptions } from "~/types";
import { ElMessage } from "element-plus";
import BaseDialog from "~/components/BaseDialog.vue";

const props = withDefaults(defineProps<{ modelValue: boolean; disabledUserUids?: string[] }>(), { disabledUserUids: () => [] });
const emit = defineEmits<{ "update:modelValue": [value: boolean]; confirm: [ids: string[]] }>();

const visible = computed({ get: () => props.modelValue, set: v => emit("update:modelValue", v) });
const loading = ref(false); const tableData = ref<UserAdminDto[]>([]); const selectedIds = ref<Set<string>>(new Set());
const searchUsername = ref(''); const searchNickname = ref(''); const searchAccountStatus = ref<AccountStatus | ''>('');
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 10, total: 0 });

const statusOpts: { label: string; value: AccountStatus }[] = [{ label: '正常', value: 'ACTIVE' }, { label: '已停用', value: 'SUSPENDED' }, { label: '已注销', value: 'DEACTIVATED' }, { label: '已删除', value: 'DELETED' }];
const selectable = (row: UserAdminDto) => !props.disabledUserUids.includes(row.uid);
const toggleRow = (row: UserAdminDto) => { if (!selectable(row)) return; const id = row.uid; if (selectedIds.value.has(id)) selectedIds.value.delete(id); else selectedIds.value.add(id); };
const allSelected = computed(() => tableData.value.length > 0 && tableData.value.filter(selectable).every(r => selectedIds.value.has(r.uid)));
const toggleAll = () => { if (allSelected.value) { tableData.value.filter(selectable).forEach(r => selectedIds.value.delete(r.uid)); } else { tableData.value.filter(selectable).forEach(r => selectedIds.value.add(r.uid)); } };

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getUserList({ page: (pageOpts.value.currentPage || 1) - 1, size: pageOpts.value.pageSize, username: searchUsername.value || undefined, nickname: searchNickname.value || undefined, accountStatus: searchAccountStatus.value || undefined });
    tableData.value = res.data.content || []; pageOpts.value.total = res.data.totalElements ?? res.data.page?.totalElements ?? 0;
  } catch { ElMessage.error('获取用户列表失败'); }
  finally { loading.value = false; }
};

const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleReset = () => { pageOpts.value.currentPage = 1; searchUsername.value = ''; searchNickname.value = ''; searchAccountStatus.value = ''; fetchData(); };
const handleConfirm = () => { emit("confirm", [...selectedIds.value]); visible.value = false; };

watch(() => visible.value, (open) => { if (!open) return; selectedIds.value = new Set(); pageOpts.value.currentPage = 1; fetchData(); });
</script>

<template>
  <BaseDialog v-model="visible" title="选择用户" width="960px">
    <div class="list-layout">
      <div class="search-form">
        <div class="search-field"><label>用户名</label><input v-model="searchUsername" class="form-input" placeholder="请输入用户名" @keyup.enter="handleSearch" /></div>
        <div class="search-field"><label>昵称</label><input v-model="searchNickname" class="form-input" placeholder="请输入昵称" @keyup.enter="handleSearch" /></div>
        <div class="search-field"><label>用户状态</label><select v-model="searchAccountStatus" class="form-select"><option value="">全部</option><option v-for="item in statusOpts" :key="item.value" :value="item.value">{{ item.label }}</option></select></div>
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
              <th style="width: 100px;">UID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th style="width: 100px;">用户状态</th>
              <th style="width: 160px;">创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="6" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</td></tr>
            <tr v-for="row in tableData" :key="row.uid" :class="{ 'row-disabled': !selectable(row) }" @click="toggleRow(row)">
              <td><input type="checkbox" :checked="selectedIds.has(row.uid)" :disabled="!selectable(row)" @click.stop="toggleRow(row)" /></td>
              <td>{{ row.uid }}</td>
              <td>{{ row.username }}</td>
              <td>{{ row.nickname || '无' }}</td>
              <td><span :class="['badge', { 'badge-green': row.accountStatus === 'ACTIVE', 'badge-yellow': row.accountStatus === 'SUSPENDED', 'badge-red': row.accountStatus === 'DEACTIVATED', 'badge-gray': row.accountStatus === 'DELETED' }]">{{ ({ ACTIVE: '正常', SUSPENDED: '已停用', DEACTIVATED: '已注销', DELETED: '已删除' })[row.accountStatus] }}</span></td>
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
