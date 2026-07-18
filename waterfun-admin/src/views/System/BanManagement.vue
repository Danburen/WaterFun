<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listBans, banUser, liftPenalty, liftAllPenalties, type BanUserResponse, type PenaltyType } from '~/api/bans'
import { ElMessage } from 'element-plus'

const bans = ref<BanUserResponse[]>([])
const totalElements = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const searchUid = ref<number | undefined>()
const searchNickname = ref('')

const showBanDialog = ref(false)
const banUid = ref<number | undefined>()
const banNickname = ref('')
const selectedPenalty = ref<PenaltyType>('BAN_LOGIN')
const banHours = ref<number>(0)
const banReason = ref('')

const penaltyOptions: { value: PenaltyType; label: string }[] = [
  { value: 'BAN_LOGIN', label: '封号' },
  { value: 'BAN_POST', label: '禁止发帖' },
  { value: 'BAN_COMMENT', label: '禁止评论' },
  { value: 'BAN_UPLOAD', label: '禁止上传' },
  { value: 'BAN_CHAT', label: '禁止聊天' },
  { value: 'BAN_CREATE', label: '禁止创建内容' },
]

const penaltyLabel = (code: string): string => {
  const map: Record<string, string> = {
    'ban:login': '封号',
    'ban:post': '禁止发帖',
    'ban:comment': '禁止评论',
    'ban:upload': '禁止上传',
    'ban:chat': '禁止聊天',
    'ban:create': '禁止创建内容',
  }
  return map[code] || code
}

const permissionCodeToPenaltyType = (code: string): PenaltyType | undefined => {
  const map: Record<string, PenaltyType> = {
    'ban:login': 'BAN_LOGIN',
    'ban:post': 'BAN_POST',
    'ban:comment': 'BAN_COMMENT',
    'ban:upload': 'BAN_UPLOAD',
    'ban:chat': 'BAN_CHAT',
    'ban:create': 'BAN_CREATE',
  }
  return map[code]
}

const formatTime = (timeStr?: string): string => {
  if (!timeStr) return '永久'
  return new Date(timeStr).toLocaleString('zh-CN', { hour12: false })
}

const fetchBans = async () => {
  loading.value = true
  try {
    const params: any = { page: currentPage.value - 1, size: pageSize.value }
    if (searchUid.value) params.userUid = searchUid.value
    if (searchNickname.value.trim()) params.nickname = searchNickname.value.trim()
    const res = await listBans(params)
    bans.value = res.data?.content || []
    totalElements.value = res.data?.totalElements ?? res.data?.page?.totalElements ?? 0
  } catch {
    ElMessage.error('获取封禁列表失败')
  } finally {
    loading.value = false
  }
}

const search = () => { currentPage.value = 1; fetchBans() }

const openBanDialog = (uid?: number, nickname?: string) => {
  banUid.value = uid
  banNickname.value = nickname || ''
  selectedPenalty.value = 'BAN_LOGIN'
  banHours.value = 0
  banReason.value = ''
  showBanDialog.value = true
}

const confirmBan = async () => {
  if (!banUid.value) {
    ElMessage.warning('请输入用户UID')
    return
  }
  try {
    await banUser({
      userUid: banUid.value,
      penaltyType: selectedPenalty.value,
      penaltyDurationHours: banHours.value > 0 ? banHours.value : undefined,
      reasonText: banReason.value || undefined,
    })
    ElMessage.success('封禁已执行')
    showBanDialog.value = false
    await fetchBans()
  } catch {
    ElMessage.error('封禁失败')
  }
}

const handleLift = async (item: BanUserResponse) => {
  try {
    const penaltyType = permissionCodeToPenaltyType(item.permissionCode)
    await liftPenalty(item.userUid, { userUid: item.userUid, penaltyType })
    ElMessage.success('已解除该限制')
    await fetchBans()
  } catch {
    ElMessage.error('解封失败')
  }
}

const handleLiftAll = async (uid: number) => {
  if (!confirm('确定解除该用户所有限制？')) return
  try {
    await liftAllPenalties(uid)
    ElMessage.success('已解除所有限制')
    await fetchBans()
  } catch {
    ElMessage.error('操作失败')
  }
}

const formatExpiry = (expiresAt?: string): string => {
  if (!expiresAt) return '永久'
  return new Date(expiresAt).toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => fetchBans())
</script>

<template>
  <div>
    <div class="page-header">
      <h2><i class="fa-solid fa-gavel"></i> 封禁管理</h2>
      <button class="btn btn-danger" @click="openBanDialog()">
        <i class="fa-solid fa-ban"></i> 封禁用户
      </button>
    </div>

    <div class="search-section">
      <div class="search-form">
        <div class="search-field">
          <label>用户UID</label>
          <input v-model.number="searchUid" type="number" placeholder="输入UID搜索" @keyup.enter="search" />
        </div>
        <div class="search-field">
          <label>昵称</label>
          <input v-model="searchNickname" placeholder="输入昵称搜索" @keyup.enter="search" />
        </div>
        <div class="search-actions">
          <button class="btn btn-primary" @click="search"><i class="fa-solid fa-search"></i> 搜索</button>
          <button class="btn btn-default" @click="searchUid = undefined; searchNickname = ''; search()"><i class="fa-solid fa-undo"></i> 重置</button>
        </div>
      </div>
    </div>

    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>UID</th>
            <th>用户</th>
            <th>限制类型</th>
            <th>封禁时间</th>
            <th>到期时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="bans.length === 0 && !loading">
            <td colspan="6" class="empty-cell">暂无封禁记录</td>
          </tr>
          <tr v-for="item in bans" :key="`${item.userUid}-${item.permissionCode}`">
            <td class="cell-mono">{{ item.userUid }}</td>
            <td>
              <div class="user-info">
                <span class="user-nickname">{{ item.displayName || item.nickname || '用户' }}</span>
              </div>
            </td>
            <td><span class="penalty-badge">{{ penaltyLabel(item.permissionCode) }}</span></td>
            <td class="cell-time">{{ formatTime(item.createdAt) }}</td>
            <td class="cell-time">{{ formatExpiry(item.expiresAt) }}</td>
            <td class="cell-actions">
              <button class="action-btn action-btn-sm action-btn-reject" @click="handleLift(item)">
                <i class="fa-solid fa-unlock"></i> 解封
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination-bar" v-if="totalElements > pageSize">
      <button class="btn" :disabled="currentPage <= 1" @click="currentPage--; fetchBans()">上一页</button>
      <span class="page-info">{{ currentPage }} / {{ Math.ceil(totalElements / pageSize) }} (共 {{ totalElements }})</span>
      <button class="btn" :disabled="currentPage >= Math.ceil(totalElements / pageSize)" @click="currentPage++; fetchBans()">下一页</button>
    </div>

    <div v-if="loading" class="loading-wrap"><i class="fa-solid fa-spinner fa-spin"></i> 加载中...</div>

    <Teleport to="body">
      <div v-if="showBanDialog" class="modal-overlay" @click.self="showBanDialog = false">
        <div class="modal">
          <div class="modal-header">
            <h3><i class="fa-solid fa-gavel" style="color: var(--danger);"></i> 封禁用户</h3>
          </div>
          <div class="modal-body">
            <div class="form-field">
              <label>用户UID <span class="required">*</span></label>
              <input v-model.number="banUid" type="number" placeholder="输入用户UID" />
            </div>
            <div class="form-field">
              <label>处罚类型 <span class="required">*</span></label>
              <select v-model="selectedPenalty">
                <option v-for="opt in penaltyOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
            <div class="form-field">
              <label>有效期（小时，0 = 永久）</label>
              <input v-model.number="banHours" type="number" min="0" placeholder="0 表示永久" />
            </div>
            <div class="form-field">
              <label>处罚原因</label>
              <textarea v-model="banReason" placeholder="可选：填写处罚原因" rows="3"></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="action-btn action-btn-skip" @click="showBanDialog = false">取消</button>
            <button class="action-btn action-btn-reject" @click="confirmBan">确认封禁</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { font-size: 18px; font-weight: 700; display: flex; align-items: center; gap: 8px; }

.table-wrap { background: var(--bg-white); border-radius: var(--radius); border: 1px solid var(--border); overflow: hidden; }
.data-table tr:last-child td { border-bottom: none; }

.cell-mono { font-family: 'SF Mono', monospace; font-size: 13px; color: var(--text-secondary); }
.cell-time { font-size: 13px; color: var(--text-secondary); white-space: nowrap; }
.cell-actions { white-space: nowrap; }
.empty-cell { text-align: center; padding: 40px !important; color: var(--text-muted); }
.user-info { display: flex; align-items: center; gap: 8px; }
.user-nickname { font-weight: 500; }
.penalty-badge { display: inline-block; padding: 2px 8px; background: var(--danger-light); color: #dc2626; border-radius: 4px; font-size: 12px; font-weight: 500; }

.pagination-bar { display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 16px; }
.page-info { font-size: 13px; color: var(--text-muted); }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 200; display: flex; align-items: center; justify-content: center; }
.modal { background: var(--bg-white); border-radius: var(--radius); width: 480px; max-width: 90%; box-shadow: 0 20px 60px rgba(0,0,0,0.2); overflow: hidden; }
.modal-header { padding: 20px 24px; border-bottom: 1px solid var(--border-light); }
.modal-header h3 { font-size: 16px; font-weight: 700; display: flex; align-items: center; gap: 8px; }
.modal-body { padding: 20px 24px; display: flex; flex-direction: column; gap: 16px; }
.modal-footer { padding: 16px 24px; border-top: 1px solid var(--border-light); display: flex; justify-content: flex-end; gap: 8px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-field label { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.form-field .required { color: var(--danger); }
.form-field input, .form-field select, .form-field textarea { padding: 8px 12px; border: 1px solid var(--border); border-radius: 6px; font-size: 14px; font-family: inherit; }
.form-field textarea { resize: vertical; min-height: 60px; }
</style>
