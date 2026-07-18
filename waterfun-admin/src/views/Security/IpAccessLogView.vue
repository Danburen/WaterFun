<script setup lang="ts">
import { formatISOData } from "@waterfun/web-core/src/timer";
import { ElMessage } from "element-plus";
import ListPage from "~/components/ListPage.vue";
import {
  listIpAccessLogs,
  type IpAccessLogResponse,
  type ListIpAccessLogParams,
} from "~/api/security";
import type { PageOptions } from "~/types";

const loading = ref(false);
const logList = ref<IpAccessLogResponse[]>([]);
const pageOpts = ref<PageOptions>({ currentPage: 1, pageSize: 20, total: 0 });

const searchForm = ref<ListIpAccessLogParams>({
  ip: undefined,
  userUid: undefined,
  requestPath: undefined,
  requestMethod: undefined,
  httpStatus: undefined,
  country: undefined,
  province: undefined,
  city: undefined,
  createdAtStart: undefined,
  createdAtEnd: undefined,
});

const fetchData = async () => {
  loading.value = true;
  try {
    const params: ListIpAccessLogParams = {
      page: pageOpts.value.currentPage || 1,
      size: pageOpts.value.pageSize,
    };
    if (searchForm.value.ip) params.ip = searchForm.value.ip;
    if (searchForm.value.userUid) params.userUid = searchForm.value.userUid;
    if (searchForm.value.requestPath) params.requestPath = searchForm.value.requestPath;
    if (searchForm.value.requestMethod) params.requestMethod = searchForm.value.requestMethod;
    if (searchForm.value.httpStatus) params.httpStatus = searchForm.value.httpStatus;
    if (searchForm.value.country) params.country = searchForm.value.country;
    if (searchForm.value.province) params.province = searchForm.value.province;
    if (searchForm.value.city) params.city = searchForm.value.city;
    if (searchForm.value.createdAtStart) params.createdAtStart = searchForm.value.createdAtStart;
    if (searchForm.value.createdAtEnd) params.createdAtEnd = searchForm.value.createdAtEnd;

    const res = await listIpAccessLogs(params);
    logList.value = res.data.content || [];
    pageOpts.value.total = res.data.page?.totalElements ?? res.data.totalElements ?? 0;
  } catch {
    ElMessage.error('获取IP访问日志失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => { pageOpts.value.currentPage = 1; fetchData(); };
const handleReset = () => {
  pageOpts.value.currentPage = 1;
  searchForm.value = {
    ip: undefined, userUid: undefined,
    requestPath: undefined, requestMethod: undefined, httpStatus: undefined,
    country: undefined, province: undefined, city: undefined,
    createdAtStart: undefined, createdAtEnd: undefined,
  };
  fetchData();
};

onMounted(fetchData);
</script>

<template>
  <ListPage
    title="IP访问日志"
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
          <label>国家</label>
          <input v-model="searchForm.country" placeholder="国家" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>省份</label>
          <input v-model="searchForm.province" placeholder="省份" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>城市</label>
          <input v-model="searchForm.city" placeholder="城市" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>请求路径</label>
          <input v-model="searchForm.requestPath" placeholder="模糊匹配" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>请求方法</label>
          <select v-model="searchForm.requestMethod">
            <option value="">全部</option>
            <option value="GET">GET</option>
            <option value="POST">POST</option>
            <option value="PUT">PUT</option>
            <option value="DELETE">DELETE</option>
          </select>
        </div>
        <div class="search-field">
          <label>HTTP状态码</label>
          <input v-model.number="searchForm.httpStatus" type="number" placeholder="如 200" @keyup.enter="handleSearch" />
        </div>
        <div class="search-field">
          <label>开始时间</label>
          <input v-model="searchForm.createdAtStart" type="datetime-local" />
        </div>
        <div class="search-field">
          <label>结束时间</label>
          <input v-model="searchForm.createdAtEnd" type="datetime-local" />
        </div>
        <div class="search-actions">
          <button class="btn btn-primary" @click="handleSearch">查询</button>
          <button class="btn btn-default" @click="handleReset">重置</button>
        </div>
      </div>
    </template>

    <table class="data-table">
      <thead>
        <tr>
          <th>IP</th>
          <th>请求路径</th>
          <th>方法</th>
          <th>状态码</th>
          <th>地区</th>
          <th>用户</th>
          <th>时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in logList" :key="row.id">
          <td><code class="ip-text">{{ row.ip }}</code></td>
          <td class="path-cell" :title="row.requestPath">{{ row.requestPath }}</td>
          <td><span :class="['method-badge', `method-${row.requestMethod.toLowerCase()}`]">{{ row.requestMethod }}</span></td>
          <td>
            <span :class="['status-badge', row.httpStatus && row.httpStatus >= 400 ? 'status-error' : 'status-ok']">
              {{ row.httpStatus ?? '-' }}
            </span>
          </td>
          <td class="geo-cell">{{ [row.country, row.province, row.city].filter(Boolean).join(' ') || '-' }}</td>
          <td><code class="uid-text">{{ row.userUid ?? '-' }}</code></td>
          <td class="time-cell">{{ formatISOData(row.createdAt) }}</td>
        </tr>
        <tr v-if="logList.length === 0 && !loading">
          <td colspan="7" class="empty-cell">暂无IP访问记录</td>
        </tr>
      </tbody>
    </table>
  </ListPage>
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
  min-width: 130px;
  background: var(--bg-white);
}
.search-actions {
  display: flex;
  gap: 8px;
}
.time-cell {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
}
.empty-cell {
  text-align: center;
  padding: 40px !important;
  color: var(--text-muted);
}
.path-cell {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: var(--text-secondary);
}
.geo-cell {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
}
.uid-text {
  font-family: 'SF Mono', monospace;
  font-size: 12px;
  color: var(--text-muted);
}
.method-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 600;
  font-family: 'SF Mono', monospace;
}
.method-get { background: #dbeafe; color: #2563eb; }
.method-post { background: #d1fae5; color: #059669; }
.method-put { background: #fef3c7; color: #d97706; }
.method-delete { background: #fee2e2; color: #dc2626; }
.status-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 600;
  font-family: 'SF Mono', monospace;
}
.status-ok { background: #d1fae5; color: #059669; }
.status-error { background: #fee2e2; color: #dc2626; }
</style>
