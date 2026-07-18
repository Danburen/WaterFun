<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessage, ElMessageBox } from "element-plus";
import ListPage from "~/components/ListPage.vue";
import {
  listIpBans, getIpBan, banIp, unbanIp, deleteIpBan,
  type IpBanResponse, type ListIpBanParams, type BanIpRequest,
} from "~/api/security";
import type { PageOptions } from "~/types";

const loading = ref(false);
const banList = ref<IpBanResponse[]>([]);
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 20, total: 0 });

const searchForm = ref<ListIpBanParams>({
  ip: undefined, reason: undefined,
  bannedAtStart: undefined, bannedAtEnd: undefined,
  expiresStart: undefined, expiresEnd: undefined,
});

// Ban dialog
const showBanDialog = ref(false);
const banForm = ref<BanIpRequest>({ ip: '', reason: '', expiresAt: undefined });
const banHours = ref(0);
const isSubmitting = ref(false);

// Detail dialog
const showDetailDialog = ref(false);
const detailData = ref<IpBanResponse | null>(null);
const detailLoading = ref(false);

const fetchData = async () => {
  loading.value = true;
  try {
    const params: ListIpBanParams = {
      page: pageOpts.value.currentPage || 1,
      size: pageOpts.value.pageSize,
    };
    if (searchForm.value.ip) params.ip = searchForm.value.ip;
    if (searchForm.value.reason) params.reason = searchForm.value.reason;
    if (searchForm.value.bannedAtStart) params.bannedAtStart = searchForm.value.bannedAtStart;
    if (searchForm.value.bannedAtEnd) params.bannedAtEnd = searchForm.value.bannedAtEnd;
    if (searchForm.value.expiresStart) params.expiresStart = searchForm.value.expiresStart;
    if (searchForm.value.expiresEnd) params.expiresEnd = searchForm.value.expiresEnd;

    const res = await listIpBans(params);
    banList.value = res.data.content || [];
    pageOpts.value.total = res.data.page?.totalElements ?? res.data.totalElements ?? 0;
  } catch {
    ElMessage.error('获取IP封禁列表失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleReset = () => {
  pageOpts.value.currentPage = 1;
  searchForm.value = {
    ip: undefined, reason: undefined,
    bannedAtStart: undefined, bannedAtEnd: undefined,
    expiresStart: undefined, expiresEnd: undefined,
  };
  fetchData();
};

const openBanDialog = () => {
  banForm.value = { ip: '', reason: '', expiresAt: undefined };
  banHours.value = 0;
  showBanDialog.value = true;
};

const confirmBan = async () => {
  if (!banForm.value.ip.trim()) {
    ElMessage.warning('请输入IP地址');
    return;
  }
  isSubmitting.value = true;
  try {
    const data: BanIpRequest = { ip: banForm.value.ip.trim() };
    if (banForm.value.reason?.trim()) data.reason = banForm.value.reason.trim();
    if (banHours.value > 0) {
      data.expiresAt = new Date(Date.now() + banHours.value * 3600000).toISOString();
    }
    await banIp(data);
    ElMessage.success('IP封禁已执行');
    showBanDialog.value = false;
    fetchData();
  } catch {
    ElMessage.error('封禁IP失败');
  } finally {
    isSubmitting.value = false;
  }
};

const handleUnban = async (ip: string) => {
  try {
    await ElMessageBox.confirm(`确定解封 IP ${ip} 吗？`, '解封确认', { type: 'warning' });
    await unbanIp({ ip });
    ElMessage.success('IP已解封');
    fetchData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('解封失败');
  }
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该封禁记录吗？', '删除确认', { type: 'warning' });
    await deleteIpBan(id);
    ElMessage.success('封禁记录已删除');
    fetchData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败');
  }
};

const showDetail = async (id: number) => {
  detailLoading.value = true;
  showDetailDialog.value = true;
  try {
    const res = await getIpBan(id);
    detailData.value = res.data;
  } catch {
    ElMessage.error('获取详情失败');
    showDetailDialog.value = false;
  } finally {
    detailLoading.value = false;
  }
};

const formatTime = (t?: string | null): string => {
  if (!t) return '永久';
  return formatISOData(t);
};

const isExpired = (expiresAt?: string | null): boolean => {
  if (!expiresAt) return false;
  return new Date(expiresAt).getTime() < Date.now();
};

onMounted(fetchData);
</script>

<template>
  <ListPage
    title="IP封禁管理"
    :loading="loading"
    :total="pageOpts.total"
    v-model:page="pageOpts.currentPage"
    v-model:pageSize="pageOpts.pageSize"
    @change="fetchData"
  >
    <template #search>
      <div class="search-form">
        <div class="search-field">
          <label>IP地址</label>
          <input v-model="searchForm.ip" placeholder="精确匹配" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>封禁原因</label>
          <input v-model="searchForm.reason" placeholder="模糊匹配" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>封禁开始</label>
          <input v-model="searchForm.bannedAtStart" type="datetime-local" />
        </div>
        <div class="search-field">
          <label>封禁结束</label>
          <input v-model="searchForm.bannedAtEnd" type="datetime-local" />
        </div>
        <div class="search-field">
          <label>到期开始</label>
          <input v-model="searchForm.expiresStart" type="datetime-local" />
        </div>
        <div class="search-field">
          <label>到期结束</label>
          <input v-model="searchForm.expiresEnd" type="datetime-local" />
        </div>
        <div class="search-actions">
          <button class="btn btn-primary" @click="handleSearch">查询</button>
          <button class="btn btn-default" @click="handleReset">重置</button>
        </div>
      </div>
    </template>
    <template #header>
      <button class="btn btn-danger" @click="openBanDialog"><i class="fa-solid fa-ban"></i> 封禁IP</button>
    </template>

    <table class="data-table">
      <thead>
        <tr>
          <th>IP</th>
          <th>原因</th>
          <th>封禁时间</th>
          <th>到期时间</th>
          <th>状态</th>
          <th style="width:220px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in banList" :key="row.id">
          <td><code class="ip-text">{{ row.ip }}</code></td>
          <td>{{ row.reason || '-' }}</td>
          <td class="time-cell">{{ formatTime(row.bannedAt) }}</td>
          <td class="time-cell">{{ formatTime(row.expiresAt) }}</td>
          <td>
            <span :class="['badge', isExpired(row.expiresAt) ? 'badge-gray' : 'badge-red']">
              {{ isExpired(row.expiresAt) ? '已过期' : '生效中' }}
            </span>
          </td>
          <td>
            <div class="table-actions">
              <button class="action-btn action-btn-sm action-btn-skip" @click="showDetail(Number(row.id))">详情</button>
              <button class="action-btn action-btn-sm action-btn-pass" :disabled="isExpired(row.expiresAt)" @click="handleUnban(row.ip)">解封</button>
              <button class="action-btn action-btn-sm action-btn-reject" @click="handleDelete(Number(row.id))">删除</button>
            </div>
          </td>
        </tr>
        <tr v-if="banList.length === 0 && !loading">
          <td colspan="6" class="empty-cell">暂无IP封禁记录</td>
        </tr>
      </tbody>
    </table>
  </ListPage>

  <!-- Ban Dialog -->
  <Teleport to="body">
    <div v-if="showBanDialog" class="modal-overlay" @click.self="showBanDialog = false">
      <div class="modal">
        <div class="modal-header">
          <h3><i class="fa-solid fa-ban" style="color: var(--danger);"></i> 封禁IP</h3>
        </div>
        <div class="modal-body">
          <div class="form-field">
            <label>IP地址 <span class="required">*</span></label>
            <input v-model="banForm.ip" placeholder="如 192.168.1.1" />
          </div>
          <div class="form-field">
            <label>封禁时长（小时，0 = 永久）</label>
            <input v-model.number="banHours" type="number" min="0" placeholder="0 表示永久封禁" />
          </div>
          <div class="form-field">
            <label>封禁原因</label>
            <textarea v-model="banForm.reason" placeholder="可选" rows="3"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="action-btn action-btn-skip" @click="showBanDialog = false">取消</button>
          <button class="action-btn action-btn-reject" :disabled="isSubmitting" @click="confirmBan">
            {{ isSubmitting ? '处理中...' : '确认封禁' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- Detail Dialog -->
  <Teleport to="body">
    <div v-if="showDetailDialog" class="modal-overlay" @click.self="showDetailDialog = false">
      <div class="modal">
        <div class="modal-header">
          <h3><i class="fa-solid fa-info-circle"></i> IP封禁详情</h3>
        </div>
        <div class="modal-body">
          <div v-if="detailLoading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>
          <div v-else-if="detailData" class="detail-grid">
            <div class="detail-row">
              <span class="detail-label">ID</span>
              <span class="detail-value">{{ detailData.id }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">IP</span>
              <span class="detail-value"><code>{{ detailData.ip }}</code></span>
            </div>
            <div class="detail-row">
              <span class="detail-label">原因</span>
              <span class="detail-value">{{ detailData.reason || '无' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">封禁时间</span>
              <span class="detail-value">{{ formatTime(detailData.bannedAt) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">到期时间</span>
              <span class="detail-value">{{ formatTime(detailData.expiresAt) }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="action-btn action-btn-skip" @click="showDetailDialog = false">关闭</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}
.search-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.search-field label {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}
.search-field input,
.search-field select {
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 13px;
  min-width: 140px;
  background: var(--bg-white);
}
.search-actions {
  display: flex;
  gap: 8px;
}
/* Modal */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal {
  background: var(--bg-white);
  border-radius: var(--radius);
  width: 480px;
  max-width: 90%;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
  overflow: hidden;
}
.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-light);
}
.modal-header h3 {
  font-size: 16px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}
.modal-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 60vh;
  overflow-y: auto;
}
.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--border-light);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.form-field .required { color: var(--danger); }
.form-field input,
.form-field select,
.form-field textarea {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
}
.form-field textarea {
  resize: vertical;
  min-height: 60px;
}
.loading-wrap {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}
.detail-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.detail-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.detail-label {
  font-size: 12px;
  color: var(--text-muted);
  min-width: 70px;
  font-weight: 500;
}
.detail-value {
  font-size: 14px;
  color: var(--text-primary);
}
.detail-value code {
  font-family: 'SF Mono', monospace;
  font-size: 13px;
  background: var(--bg);
  padding: 2px 6px;
  border-radius: 3px;
}
</style>
