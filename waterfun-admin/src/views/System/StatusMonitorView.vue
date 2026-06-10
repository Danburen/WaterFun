<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, reactive, nextTick } from 'vue'
import { getSystemMonitor, type SystemInfoVO } from '~/api/monitor'
import { ElMessage } from 'element-plus'
import { init, graphic } from 'echarts'

const monitorData = ref<SystemInfoVO | null>(null)
const loading = ref(false)
const polling = ref<ReturnType<typeof setInterval> | null>(null)
const refreshing = ref(false)
const chartRef = ref<HTMLElement | null>(null)
let chartInstance: ReturnType<typeof init> | null = null

const toPercent = (v?: number) => (v ?? 0) * 100

const cpuUsage = computed(() => toPercent(monitorData.value?.cpu?.usage))
const cpuStatus = computed(() => {
  const u = cpuUsage.value
  return u >= 90 ? 'danger' : u >= 70 ? 'warning' : 'healthy'
})
const cpuStatusText = computed(() => ({ healthy: '正常', warning: '偏高', danger: '危险' })[cpuStatus.value])

const memUsage = computed(() => toPercent(monitorData.value?.memory?.usage))
const memStatus = computed(() => {
  const u = memUsage.value
  return u >= 90 ? 'danger' : u >= 80 ? 'warning' : 'healthy'
})
const memStatusText = computed(() => ({ healthy: '正常', warning: '偏高', danger: '危险' })[memStatus.value])

const barClass = (status: string) => {
  if (status === 'danger') return 'bar-red'
  if (status === 'warning') return 'bar-yellow'
  return 'bar-blue'
}

const barHeight = (value: number) => Math.min(value, 100)

const formatUptime = (seconds?: number): string => {
  if (seconds == null) return '未知'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const mins = Math.floor((seconds % 3600) / 60)
  if (days > 0) return `${days}d ${hours}h ${mins}m`
  if (hours > 0) return `${hours}h ${mins}m`
  return `${mins}m`
}

const formatBytes = (bytes?: number, decimals = 2): string => {
  if (bytes == null) return '未知'
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1)
  return parseFloat((bytes / Math.pow(k, i)).toFixed(decimals)) + ' ' + sizes[i]
}

const formatBytesPerSec = (bytes?: number): string => formatBytes(bytes, 1) + '/s'

const memTotalFormatted = computed(() => formatBytes(monitorData.value?.memory?.total))
const memAvailableFormatted = computed(() => formatBytes(monitorData.value?.memory?.available))

const adminJvm = computed(() => monitorData.value?.jvms?.admin)
const userJvm = computed(() => monitorData.value?.jvms?.user)

const adminHeapUsed = computed(() => adminJvm.value?.heap?.used)
const adminHeapMax = computed(() => adminJvm.value?.heap?.max)
const adminHeapPct = computed(() => {
  if (!adminHeapUsed.value || !adminHeapMax.value || adminHeapMax.value === 0) return 0
  return adminHeapUsed.value / adminHeapMax.value * 100
})
const adminThreadCount = computed(() => adminJvm.value?.thread?.count)

const userHeapUsed = computed(() => userJvm.value?.heap?.used)
const userHeapMax = computed(() => userJvm.value?.heap?.max)
const userHeapPct = computed(() => {
  if (!userHeapUsed.value || !userHeapMax.value || userHeapMax.value === 0) return 0
  return userHeapUsed.value / userHeapMax.value * 100
})
const userThreadCount = computed(() => userJvm.value?.thread?.count)

const jvmTotalHeapUsed = computed(() => (adminHeapUsed.value ?? 0) + (userHeapUsed.value ?? 0))
const jvmTotalHeapMax = computed(() => (adminHeapMax.value ?? 0) + (userHeapMax.value ?? 0))
const jvmTotalPct = computed(() => {
  if (jvmTotalHeapMax.value === 0) return 0
  return jvmTotalHeapUsed.value / jvmTotalHeapMax.value * 100
})
const jvmTotalStatus = computed(() => {
  const u = jvmTotalPct.value
  return u >= 90 ? 'danger' : u >= 75 ? 'warning' : 'healthy'
})
const jvmTotalStatusText = computed(() => ({ healthy: '正常', warning: '偏高', danger: '危险' })[jvmTotalStatus.value])

// Stacked memory bar
const memStacked = computed(() => {
  const total = monitorData.value?.memory?.total ?? 1
  const available = monitorData.value?.memory?.available ?? 0
  const memUsed = monitorData.value?.memory?.used ?? 0
  const adminHeap = adminHeapUsed.value ?? 0
  const userHeap = userHeapUsed.value ?? 0
  const otherProcs = Math.max(0, memUsed - adminHeap - userHeap)
  return {
    adminPct: Math.min(100, adminHeap / total * 100),
    userPct: Math.min(100, userHeap / total * 100),
    otherPct: Math.min(100, otherProcs / total * 100),
    freePct: Math.min(100, available / total * 100),
    adminLabel: `管理端 JVM: ${formatBytes(adminHeap)}`,
    userLabel: `用户端 JVM: ${formatBytes(userHeap)}`,
    otherLabel: `其他进程: ${formatBytes(otherProcs)}`,
    freeLabel: `空闲: ${formatBytes(available)}`,
  }
})

const hasUserJvm = computed(() => userJvm.value != null)

// Network
const prevNetwork = ref<{ sent: number; received: number; ts: number } | null>(null)
const rxSpeed = ref('0 B/s')
const txSpeed = ref('0 B/s')

const updateNetworkSpeed = () => {
  const net = monitorData.value?.network
  if (!net || net.sent == null || net.received == null) return
  const ts = net.timestamp ?? Date.now()
  if (prevNetwork.value) {
    const dt = (ts - prevNetwork.value.ts) / 1000
    if (dt > 0) {
      rxSpeed.value = formatBytesPerSec(Math.max(0, (net.received - prevNetwork.value.received) / dt))
      txSpeed.value = formatBytesPerSec(Math.max(0, (net.sent - prevNetwork.value.sent) / dt))
    }
  }
  prevNetwork.value = { sent: net.sent, received: net.received, ts }
}

// Disk
const disks = computed(() => monitorData.value?.disks ?? [])
const diskBarClass = (usage: number) => {
  if (usage >= 90) return 'bar-red'
  if (usage >= 75) return 'bar-yellow'
  return 'bar-blue'
}

// ECharts trend data (cached in memory, not cleared on type switch)
const MAX_HISTORY = 30
const trendHistory = reactive<Record<string, { admin: number[]; user: number[]; labels: string[] }>>({
  cpu: { admin: [], user: [], labels: [] },
  memory: { admin: [], user: [], labels: [] },
  jvm: { admin: [], user: [], labels: [] },
})

const currentTrendType = ref<'cpu' | 'memory' | 'jvm'>('cpu')

const pushTrendPoint = (data: SystemInfoVO) => {
  const now = new Date()
  const label = now.getHours().toString().padStart(2, '0') + ':' + now.getMinutes().toString().padStart(2, '0')

  const cpu = toPercent(data.cpu?.usage)
  const memGB = data.memory ? data.memory.used / (1024 * 1024 * 1024) : 0
  const adminHeapMB = data.jvms?.admin?.heap?.used ? data.jvms.admin.heap.used / (1024 * 1024) : 0
  const userHeapMB = data.jvms?.user?.heap?.used ? data.jvms.user.heap.used / (1024 * 1024) : 0

  const push = (key: string, adminVal: number, userVal: number) => {
    const h = trendHistory[key]
    h.labels.push(label)
    h.admin.push(adminVal)
    h.user.push(userVal)
    if (h.labels.length > MAX_HISTORY) {
      h.labels.shift()
      h.admin.shift()
      h.user.shift()
    }
  }

  push('cpu', cpu, cpu)
  push('memory', memGB, memGB)
  push('jvm', adminHeapMB, userHeapMB)
}

const switchTrend = (type: 'cpu' | 'memory' | 'jvm') => {
  currentTrendType.value = type
  updateChart()
}

const seriesConfig: Record<string, { unit: string; maxVal?: number; color: string[] }> = {
  cpu: { unit: '%', maxVal: 100, color: ['#3b82f6', '#10b981'] },
  memory: { unit: 'GB', color: ['#3b82f6', '#10b981'] },
  jvm: { unit: 'MB', color: ['#f59e0b', '#06b6d4'] },
}

const buildOption = () => {
  const key = currentTrendType.value
  const h = trendHistory[key]
  const cfg = seriesConfig[key]

  const maxVal = cfg.maxVal
  const dataMax = Math.max(...h.admin, ...h.user, 0)
  const finalMax = maxVal ?? Math.max(100, Math.ceil(dataMax * 1.2))

  return {
    tooltip: {
      trigger: 'axis' as const,
      backgroundColor: 'rgba(30, 41, 59, 0.95)',
      borderColor: '#334155',
      textStyle: { color: '#e2e8f0', fontSize: 12 },
      formatter: (params: any[]) => {
        if (!params || params.length === 0) return ''
        let html = `<div style="font-weight:600;margin-bottom:6px;">${params[0].axisValue}</div>`
        params.forEach((p: any) => {
          html += `<div style="display:flex;align-items:center;gap:6px;margin:3px 0;">` +
            `<span style="width:8px;height:8px;border-radius:50%;background:${p.color};"></span>` +
            `<span>${p.seriesName}: <b>${typeof p.value === 'number' ? p.value.toFixed(1) : p.value} ${cfg.unit}</b></span></div>`
        })
        return html
      },
    },
    legend: {
      data: ['管理端', '用户端'],
      right: 0,
      top: 0,
      textStyle: { color: '#64748b', fontSize: 12 },
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 16,
    },
    grid: {
      left: 10,
      right: 10,
      top: 40,
      bottom: 20,
      containLabel: true,
    },
    xAxis: {
      type: 'category' as const,
      boundaryGap: false,
      data: [...h.labels],
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#94a3b8', fontSize: 11, interval: 3 },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value' as const,
      max: finalMax,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 11, formatter: `{value} ${cfg.unit}` },
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' as const } },
    },
    series: [
      {
        name: '管理端',
        type: 'line' as const,
        smooth: true,
        symbol: 'circle' as const,
        symbolSize: 4,
        showSymbol: false,
        lineStyle: { width: 2, color: cfg.color[0] },
        itemStyle: { color: cfg.color[0] },
        areaStyle: {
          color: new graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: cfg.color[0] + '33' },
            { offset: 1, color: cfg.color[0] + '05' },
          ]),
        },
        data: [...h.admin],
      },
      {
        name: '用户端',
        type: 'line' as const,
        smooth: true,
        symbol: 'circle' as const,
        symbolSize: 4,
        showSymbol: false,
        lineStyle: { width: 2, color: cfg.color[1] },
        itemStyle: { color: cfg.color[1] },
        areaStyle: {
          color: new graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: cfg.color[1] + '33' },
            { offset: 1, color: cfg.color[1] + '05' },
          ]),
        },
        data: [...h.user],
      },
    ],
  }
}

const updateChart = () => {
  if (chartInstance) {
    chartInstance.setOption(buildOption(), true)
  }
}

const initChart = () => {
  if (!chartRef.value) return
  if (chartInstance) return
  chartInstance = init(chartRef.value)
  updateChart()
}

const handleResize = () => {
  chartInstance?.resize()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSystemMonitor()
    if (res.data) {
      monitorData.value = res.data
      updateNetworkSpeed()
      pushTrendPoint(res.data)
      nextTick(() => {
        if (!chartInstance) initChart()
        else updateChart()
      })
    }
  } catch {
    ElMessage.error('获取系统状态失败')
  } finally {
    loading.value = false
  }
}

const handleRefresh = async () => {
  refreshing.value = true
  await fetchData()
  setTimeout(() => { refreshing.value = false }, 600)
}

onMounted(() => {
  fetchData()
  polling.value = setInterval(fetchData, 4000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (polling.value) {
    clearInterval(polling.value)
    polling.value = null
  }
  chartInstance?.dispose()
  chartInstance = null
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="monitor-page">
    <div class="page-header">
      <h1 class="page-title"><i class="fa-solid fa-heartbeat"></i> 系统状态</h1>
      <div class="page-actions">
        <span class="polling-indicator" v-if="!loading">
          <span class="dot"></span> 实时更新
        </span>
        <button class="btn" :class="{ refreshing }" @click="handleRefresh">
          <i class="fa-solid fa-sync-alt"></i> 刷新
        </button>
      </div>
    </div>

    <div v-if="loading && !monitorData" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>

    <template v-else-if="monitorData">
      <div class="monitor-grid">
        <!-- CPU -->
        <div class="card monitor-card">
          <div class="monitor-card-header">
            <div class="monitor-card-title"><i class="fa-solid fa-microchip" style="color: var(--primary);"></i> CPU 使用率</div>
            <span :class="['monitor-status', `status-${cpuStatus}`]">{{ cpuStatusText }}</span>
          </div>
          <div class="monitor-value">{{ cpuUsage.toFixed(1) }}<span class="monitor-unit">%</span></div>
          <div class="monitor-bar">
            <div :class="['monitor-bar-fill', barClass(cpuStatus)]" :style="{ width: barHeight(cpuUsage) + '%' }"></div>
          </div>
          <div class="monitor-detail">
            <span>物理核心: {{ monitorData.cpu?.physicalCores ?? '?' }}</span>
            <span>逻辑核心: {{ monitorData.cpu?.logicalCores ?? '?' }}</span>
          </div>
        </div>

        <!-- Memory (Stacked with JVM instances) -->
        <div class="card monitor-card">
          <div class="monitor-card-header">
            <div class="monitor-card-title"><i class="fa-solid fa-memory" style="color: var(--success);"></i> 内存使用</div>
            <span :class="['monitor-status', `status-${memStatus}`]">{{ memStatusText }}</span>
          </div>
          <div class="monitor-value">{{ formatBytes(monitorData.memory?.used) }}<span class="monitor-unit"> / {{ memTotalFormatted }}</span></div>
          <div class="stacked-bar">
            <div class="stacked-segment bar-blue" :style="{ width: memStacked.adminPct + '%' }" :data-label="memStacked.adminLabel"></div>
            <div class="stacked-segment bar-green" :style="{ width: memStacked.userPct + '%' }" :data-label="memStacked.userLabel"></div>
            <div class="stacked-segment bar-yellow" :style="{ width: memStacked.otherPct + '%' }" :data-label="memStacked.otherLabel"></div>
            <div class="stacked-segment bar-cyan" :style="{ width: memStacked.freePct + '%' }" :data-label="memStacked.freeLabel"></div>
          </div>
          <div class="stacked-legend">
            <div class="stacked-legend-item">
              <div class="stacked-legend-dot bar-blue-bg"></div>
              <span>管理端 JVM</span>
            </div>
            <div class="stacked-legend-item">
              <div class="stacked-legend-dot bar-green-bg"></div>
              <span>用户端 JVM</span>
            </div>
            <div class="stacked-legend-item">
              <div class="stacked-legend-dot bar-yellow-bg"></div>
              <span>其他进程</span>
            </div>
            <div class="stacked-legend-item">
              <div class="stacked-legend-dot bar-cyan-bg"></div>
              <span>空闲</span>
            </div>
          </div>
          <div class="monitor-detail" style="margin-top: 8px;">
            <span>JVM 合计: {{ formatBytes((adminHeapUsed ?? 0) + (userHeapUsed ?? 0)) }}</span>
            <span>可用: {{ memAvailableFormatted }}</span>
          </div>
        </div>

        <!-- JVM Heap Overview -->
        <div class="card monitor-card">
          <div class="monitor-card-header">
            <div class="monitor-card-title"><i class="fa-solid fa-coffee" style="color: var(--warning);"></i> JVM 堆内存总览</div>
            <span :class="['monitor-status', `status-${jvmTotalStatus}`]">{{ jvmTotalStatusText }}</span>
          </div>
          <div class="monitor-value">{{ formatBytes(jvmTotalHeapUsed) }}<span class="monitor-unit"> / {{ formatBytes(jvmTotalHeapMax) }}</span></div>
          <div class="monitor-bar">
            <div :class="['monitor-bar-fill', barClass(jvmTotalStatus)]" :style="{ width: barHeight(jvmTotalPct) + '%' }"></div>
          </div>
          <div class="monitor-detail">
            <span>管理端: {{ formatBytes(adminHeapUsed) }}</span>
            <span>用户端: {{ formatBytes(userHeapUsed) }}</span>
          </div>
        </div>
      </div>

      <!-- JVM Instance Cards -->
      <div class="jvm-grid">
        <div class="card jvm-card">
          <div class="jvm-card-header">
            <div class="jvm-card-title">
              <i class="fa-solid fa-server" style="color: var(--primary);"></i>
              管理端 JVM
            </div>
            <span class="jvm-instance-tag tag-admin">{{ adminJvm?.source || 'Admin Service' }}</span>
          </div>
          <div class="jvm-metrics">
            <div class="jvm-metric">
              <div class="jvm-metric-value">{{ formatBytes(adminHeapUsed) }}</div>
              <div class="jvm-metric-label">堆内存</div>
            </div>
            <div class="jvm-metric">
              <div class="jvm-metric-value">{{ adminThreadCount ?? '?' }}</div>
              <div class="jvm-metric-label">活跃线程</div>
            </div>
            <div class="jvm-metric">
              <div class="jvm-metric-value">{{ formatUptime(adminJvm?.jvm?.uptime) }}</div>
              <div class="jvm-metric-label">运行时间</div>
            </div>
          </div>
          <div class="monitor-bar">
            <div :class="['monitor-bar-fill', barClass(adminHeapPct >= 75 ? 'warning' : 'healthy')]" :style="{ width: barHeight(adminHeapPct) + '%' }"></div>
          </div>
          <div class="monitor-detail">
            <span>已用: {{ formatBytes(adminHeapUsed) }} / {{ formatBytes(adminHeapMax) }}</span>
            <span>堆使用率: {{ adminHeapPct.toFixed(1) }}%</span>
          </div>
        </div>

        <div class="card jvm-card">
          <div class="jvm-card-header">
            <div class="jvm-card-title">
              <i class="fa-solid fa-users" style="color: var(--success);"></i>
              用户端 JVM
            </div>
            <span class="jvm-instance-tag tag-user">{{ userJvm?.source || 'User Service' }}</span>
          </div>
          <div v-if="!hasUserJvm" class="jvm-error">用户端 JVM 不可用</div>
          <template v-else>
            <div class="jvm-metrics">
              <div class="jvm-metric">
                <div class="jvm-metric-value">{{ formatBytes(userHeapUsed) }}</div>
                <div class="jvm-metric-label">堆内存</div>
              </div>
              <div class="jvm-metric">
                <div class="jvm-metric-value">{{ userThreadCount ?? '?' }}</div>
                <div class="jvm-metric-label">活跃线程</div>
              </div>
              <div class="jvm-metric">
                <div class="jvm-metric-value">{{ formatUptime(userJvm?.jvm?.uptime) }}</div>
                <div class="jvm-metric-label">运行时间</div>
              </div>
            </div>
            <div class="monitor-bar">
              <div :class="['monitor-bar-fill', barClass(userHeapPct >= 75 ? 'warning' : 'healthy')]" :style="{ width: barHeight(userHeapPct) + '%' }"></div>
            </div>
            <div class="monitor-detail">
              <span>已用: {{ formatBytes(userHeapUsed) }} / {{ formatBytes(userHeapMax) }}</span>
              <span>堆使用率: {{ userHeapPct.toFixed(1) }}%</span>
            </div>
          </template>
        </div>
      </div>

      <!-- Charts & Info -->
      <div class="section-grid">
        <div class="card">
          <div class="card-header">
            <div class="card-title"><i class="fa-solid fa-chart-area"></i> 资源使用趋势</div>
            <div class="card-actions">
              <button :class="['action-btn', { active: currentTrendType === 'cpu' }]" @click="switchTrend('cpu')">CPU</button>
              <button :class="['action-btn', { active: currentTrendType === 'memory' }]" @click="switchTrend('memory')">内存</button>
              <button :class="['action-btn', { active: currentTrendType === 'jvm' }]" @click="switchTrend('jvm')">JVM</button>
            </div>
          </div>
          <div class="card-body">
            <div ref="chartRef" class="echarts-container"></div>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-title"><i class="fa-solid fa-info-circle"></i> 系统详情</div>
          </div>
          <div class="card-body">
            <div class="info-list">
              <div class="info-item">
                <span class="info-label"><i class="fa-solid fa-server"></i> 主机名</span>
                <span class="info-value">{{ monitorData.system?.hostname || '未知' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label"><i class="fa-solid fa-desktop"></i> 操作系统</span>
                <span class="info-value">{{ monitorData.system?.os || '未知' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label"><i class="fa-solid fa-clock"></i> 系统运行时间</span>
                <span class="info-value">{{ formatUptime(monitorData.system?.uptime) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label"><i class="fa-solid fa-code-branch"></i> Java 版本</span>
                <span class="info-value">{{ adminJvm?.jvm?.version || '未知' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label"><i class="fa-solid fa-layer-group"></i> 总活跃线程</span>
                <span class="info-value">{{ (adminThreadCount ?? 0) + (userThreadCount ?? 0) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Disk & Network -->
      <div class="section-grid section-grid-2col">
        <div class="card">
          <div class="card-header">
            <div class="card-title"><i class="fa-solid fa-hdd"></i> 磁盘使用</div>
          </div>
          <div class="card-body">
            <div class="disk-grid">
              <div v-for="(disk, idx) in disks" :key="idx" class="disk-item">
                <div class="disk-header">
                  <span class="disk-name"><i class="fa-solid fa-hdd" style="color: var(--primary); margin-right: 6px;"></i>{{ disk.mount || '未知' }}</span>
                  <span class="disk-size">{{ formatBytes(disk.used) }} / {{ formatBytes(disk.total) }}</span>
                </div>
                <div class="disk-bar">
                  <div :class="['disk-bar-fill', diskBarClass(toPercent(disk.usage))]" :style="{ width: barHeight(toPercent(disk.usage)) + '%' }"></div>
                </div>
                <div class="disk-detail">
                  <span>已用 {{ toPercent(disk.usage).toFixed(1) }}%</span>
                  <span>可用 {{ formatBytes(disk.free) }}</span>
                </div>
              </div>
              <div v-if="disks.length === 0" class="disk-empty">暂无数据</div>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-title"><i class="fa-solid fa-network-wired"></i> 网络 IO</div>
          </div>
          <div class="card-body">
            <div class="info-list">
              <div class="info-item">
                <span class="info-label"><i class="fa-solid fa-arrow-down"></i> 接收速率</span>
                <span class="info-value">{{ rxSpeed }}</span>
              </div>
              <div class="info-item">
                <span class="info-label"><i class="fa-solid fa-arrow-up"></i> 发送速率</span>
                <span class="info-value">{{ txSpeed }}</span>
              </div>
              <div class="info-item">
                <span class="info-label"><i class="fa-solid fa-download"></i> 总接收</span>
                <span class="info-value">{{ formatBytes(monitorData.network?.received) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label"><i class="fa-solid fa-upload"></i> 总发送</span>
                <span class="info-value">{{ formatBytes(monitorData.network?.sent) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.monitor-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title i {
  color: var(--primary);
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.polling-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.dot {
  width: 8px;
  height: 8px;
  background: var(--success);
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.refreshing i {
  animation: spin 0.6s linear;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.monitor-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.monitor-card {
  padding: 20px;
  transition: all 0.2s ease;
}

.monitor-card:hover {
  box-shadow: var(--shadow-md);
}

.monitor-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.monitor-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.monitor-card-title i {
  font-size: 14px;
}

.monitor-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.status-healthy {
  background: #dcfce7;
  color: #15803d;
}

.status-healthy::before {
  content: '';
  width: 6px;
  height: 6px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.status-warning {
  background: #fef3c7;
  color: #b45309;
}

.status-warning::before {
  content: '';
  width: 6px;
  height: 6px;
  background: #f59e0b;
  border-radius: 50%;
}

.status-danger {
  background: #fee2e2;
  color: #dc2626;
}

.status-danger::before {
  content: '';
  width: 6px;
  height: 6px;
  background: #ef4444;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.monitor-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.monitor-unit {
  font-size: 14px;
  color: var(--text-muted);
  font-weight: 400;
}

.monitor-bar {
  height: 6px;
  background: var(--border-light);
  border-radius: 3px;
  overflow: hidden;
  margin-top: 12px;
}

.monitor-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

.bar-blue { background: linear-gradient(90deg, #3b82f6, #60a5fa); }
.bar-green { background: linear-gradient(90deg, #10b981, #34d399); }
.bar-yellow { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.bar-red { background: linear-gradient(90deg, #ef4444, #f87171); }

.monitor-detail {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

/* Stacked Memory Bar */
.stacked-bar {
  height: 8px;
  background: var(--border-light);
  border-radius: 4px;
  overflow: hidden;
  margin-top: 12px;
  display: flex;
}

.stacked-segment {
  height: 100%;
  transition: width 0.5s ease;
  position: relative;
}

.stacked-segment:hover::after {
  content: attr(data-label);
  position: absolute;
  top: -28px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--text-primary);
  color: white;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 11px;
  white-space: nowrap;
  z-index: 10;
}

.stacked-legend {
  display: flex;
  gap: 16px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.stacked-legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.stacked-legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
}

.bar-blue-bg { background: linear-gradient(90deg, #3b82f6, #60a5fa); }
.bar-green-bg { background: linear-gradient(90deg, #10b981, #34d399); }
.bar-yellow-bg { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.bar-cyan-bg { background: linear-gradient(90deg, #06b6d4, #22d3ee); }

/* JVM Instance Cards */
.jvm-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.jvm-card {
  padding: 20px;
  transition: all 0.2s ease;
}

.jvm-card:hover {
  box-shadow: var(--shadow-md);
}

.jvm-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.jvm-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.jvm-instance-tag {
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.tag-admin { background: var(--primary-light); color: var(--primary); }
.tag-user { background: #f0fdf4; color: var(--success); }

.jvm-error {
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
  padding: 30px 0;
}

.jvm-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.jvm-metric {
  text-align: center;
}

.jvm-metric-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.jvm-metric-label {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.section-grid-2col {
  grid-template-columns: 1fr 1fr;
}

.echarts-container {
  height: 280px;
  width: 100%;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: var(--bg);
  border-radius: var(--radius-sm);
}

.info-label {
  font-size: 13px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-label i { color: var(--primary); font-size: 13px; }

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.disk-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.disk-item {
  padding: 14px;
  background: var(--bg);
  border-radius: var(--radius-sm);
}

.disk-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.disk-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.disk-size {
  font-size: 12px;
  color: var(--text-muted);
}

.disk-bar {
  height: 8px;
  background: var(--border-light);
  border-radius: 4px;
  overflow: hidden;
}

.disk-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.disk-detail {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.disk-empty {
  grid-column: 1 / -1;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
  padding: 20px;
}

@media (max-width: 1200px) {
  .monitor-grid { grid-template-columns: repeat(2, 1fr); }
  .jvm-grid { grid-template-columns: 1fr; }
  .section-grid-2col { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .monitor-grid { grid-template-columns: 1fr; }
  .jvm-grid { grid-template-columns: 1fr; }
  .disk-grid { grid-template-columns: 1fr; }
}
</style>
