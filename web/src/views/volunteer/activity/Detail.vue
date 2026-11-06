<template>
  <el-drawer
    v-model="visible"
    size="900px"
    append-to-body
    class="va-detail-drawer"
    @closed="handleClosed"
  >
    <template #header>
      <div class="va-detail-head">
        <span class="va-detail-name">{{ detail.name || '活动详情' }}</span>
        <span v-if="detail.showStatus !== undefined" class="oa-pill" :class="'oa-pill--' + showStatus.tone">
          {{ showStatus.label }}
        </span>
        <span v-if="detail.activityNo" class="va-detail-no">{{ detail.activityNo }}</span>
      </div>
    </template>

    <div v-loading="loading" class="va-detail-body">
      <!-- 错误态 -->
      <div v-if="loadError" class="va-detail-error">
        <div class="va-detail-error__title">活动加载失败</div>
        <div class="oa-muted">活动可能已被删除，请刷新列表后重试。</div>
        <div class="va-detail-error__actions">
          <el-button type="primary" plain @click="loadDetail">重试</el-button>
          <el-button @click="visible = false">关 闭</el-button>
        </div>
      </div>

      <template v-else-if="!loading">
        <!-- 顶部：基本信息 + 二维码 -->
        <div class="va-detail-top">
          <div class="va-info-card">
            <div class="va-section-title">基本信息</div>
            <div class="va-info-grid">
              <div class="va-info-item">
                <div class="va-info-item__k">活动地点</div>
                <div class="va-info-item__v">{{ detail.location || '—' }}</div>
              </div>
              <div class="va-info-item">
                <div class="va-info-item__k">活动人数（参考）</div>
                <div class="va-info-item__v">{{ capacityText }}</div>
              </div>
              <div class="va-info-item">
                <div class="va-info-item__k">志愿时长</div>
                <div class="va-info-item__v">{{ formatHours(detail.durationHours) }} 小时</div>
              </div>
              <div class="va-info-item">
                <div class="va-info-item__k">报名截止</div>
                <div class="va-info-item__v">{{ formatActivityTime(detail.signupDeadline) }}</div>
              </div>
              <div class="va-info-item is-full">
                <div class="va-info-item__k">起止时间</div>
                <div class="va-info-item__v">
                  {{ formatActivityTime(detail.startTime) }} ~ {{ formatActivityTime(detail.endTime) }}
                </div>
              </div>
              <div class="va-info-item is-full">
                <div class="va-info-item__k">活动详情</div>
                <div class="va-info-item__v va-pre">{{ detail.content || '—' }}</div>
              </div>
            </div>
          </div>

          <div class="va-qr-card">
            <div class="va-qr-title">报名二维码</div>
            <div class="va-qr-box">
              <img v-if="qrcodeImage" :src="qrcodeImage" alt="报名二维码" class="va-qr-img" />
              <div v-else class="oa-muted">二维码加载失败</div>
            </div>
            <div class="va-qr-tip">扫码进入 H5 报名页<br />（仅展示，不提供下载 / 复制）</div>
            <div class="va-qr-link">{{ qrcodeLink || '—' }}</div>
          </div>
        </div>

        <!-- 报名名单 -->
        <div class="va-signup-card">
          <div class="va-signup-head">
            <div class="va-section-title">报名名单</div>
            <el-button
              v-hasPermi="['oa:volunteer:export']"
              size="small"
              icon="Download"
              :loading="exporting"
              @click="handleExportSignup"
            >导出报名名单</el-button>
          </div>

          <!-- 报名统计小卡（待审核 / 通过 / 驳回） -->
          <div class="va-mini-stats">
            <div class="va-mini-stat is-wait">
              <span class="va-mini-stat__label">待审核</span>
              <b class="va-mini-stat__num">{{ pendingCount }}</b>
            </div>
            <div class="va-mini-stat is-pass">
              <span class="va-mini-stat__label">通过</span>
              <b class="va-mini-stat__num">{{ passCount }}</b>
            </div>
            <div class="va-mini-stat is-rej">
              <span class="va-mini-stat__label">驳回</span>
              <b class="va-mini-stat__num">{{ rejectCount }}</b>
            </div>
          </div>

          <!-- 报名筛选 -->
          <el-form
            :model="signupFilters"
            :inline="true"
            class="oa-filter-bar va-signup-filter"
            @submit.prevent="handleSignupQuery"
          >
            <el-form-item label="审核状态">
              <el-select v-model="signupFilters.auditStatus" placeholder="全部状态" clearable style="width: 130px">
                <el-option v-for="item in auditStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键词">
              <el-input
                v-model="signupFilters.keyword"
                placeholder="姓名 / 学号"
                clearable
                style="width: 200px"
                @keyup.enter="handleSignupQuery"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" icon="Search" :loading="signupLoading" @click="handleSignupQuery">查询</el-button>
              <el-button size="small" icon="Refresh" @click="resetSignupQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 批量审核条 -->
          <div v-if="selectedRows.length" class="va-batch-bar">
            <span class="va-batch-count">已选 <b>{{ selectedRows.length }}</b> 人</span>
            <span class="oa-muted">仅对待审核报名生效</span>
            <span class="va-batch-grow"></span>
            <el-button
              v-hasPermi="['oa:volunteer:audit']"
              size="small"
              type="success"
              plain
              :loading="auditing"
              @click="handleBatchPass"
            >批量通过</el-button>
            <el-button
              v-hasPermi="['oa:volunteer:audit']"
              size="small"
              type="danger"
              plain
              :loading="auditing"
              @click="handleBatchReject"
            >批量驳回</el-button>
            <el-button size="small" @click="clearSelection(false)">清空选择</el-button>
          </div>

          <!-- 报名表格 -->
          <el-table
            ref="signupTableRef"
            v-loading="signupLoading"
            :data="signupList"
            row-key="signupId"
            class="va-signup-table"
            empty-text="该活动暂无报名记录"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="48" align="center" :selectable="isPendingRow" />
            <el-table-column label="姓名" width="100" show-overflow-tooltip>
              <template #default="scope">
                <span class="va-name">{{ scope.row.name || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="学号" width="130">
              <template #default="scope">
                <span class="va-mono">{{ scope.row.studentNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="学院" width="180" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.college || '—' }}</template>
            </el-table-column>
            <el-table-column label="专业" width="160" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.major || '—' }}</template>
            </el-table-column>
            <el-table-column label="报名时间" width="150" align="center">
              <template #default="scope">{{ formatActivityTime(scope.row.signupTime) }}</template>
            </el-table-column>
            <el-table-column label="审核状态" width="190">
              <template #default="scope">
                <span class="oa-pill" :class="'oa-pill--' + rowAuditMeta(scope.row).tone">
                  {{ rowAuditMeta(scope.row).label }}
                </span>
                <div v-if="isRejectedRow(scope.row) && scope.row.auditRemark" class="va-reason">
                  原因：{{ scope.row.auditRemark }}
                </div>
                <div v-else-if="!isPendingRow(scope.row) && scope.row.auditTime" class="oa-muted va-audit-time">
                  {{ formatActivityTime(scope.row.auditTime) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right" align="center" class-name="small-padding fixed-width">
              <template #default="scope">
                <template v-if="isPendingRow(scope.row)">
                  <el-button
                    v-hasPermi="['oa:volunteer:audit']"
                    link
                    type="primary"
                    :loading="auditing"
                    @click.stop="handlePass(scope.row)"
                  >通过</el-button>
                  <el-button
                    v-hasPermi="['oa:volunteer:audit']"
                    link
                    type="danger"
                    @click.stop="openReject([scope.row])"
                  >驳回</el-button>
                </template>
                <span v-else class="oa-muted">—</span>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-show="signupTotal > 0"
            :total="signupTotal"
            v-model:page="signupPage.pageNum"
            v-model:limit="signupPage.pageSize"
            :page-sizes="[10, 20, 50]"
            @pagination="handleSignupPage"
          />
        </div>
      </template>
    </div>

    <template #footer>
      <div class="va-detail-foot">
        <el-button @click="visible = false">关 闭</el-button>
        <el-button
          v-hasPermi="['oa:volunteer:edit']"
          type="primary"
          @click="handleEditActivity"
        >编辑活动</el-button>
      </div>
    </template>

    <!-- 驳回弹窗（420px，原因选填 ≤200） -->
    <el-dialog
      v-model="rejectVisible"
      title="驳回报名"
      width="420px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="va-reject-target">
        驳回对象：{{ rejectNames }}（共 {{ rejectRows.length }} 人）
      </div>
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        maxlength="200"
        show-word-limit
        placeholder="请填写驳回原因（选填，≤200 字）"
      />
      <template #footer>
        <el-button :disabled="rejectSubmitting" @click="rejectVisible = false">取 消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup name="VolunteerActivityDetail">
import { saveAs } from 'file-saver'
import {
  getActivity,
  getActivityQrcode,
  listSignup,
  auditSignup,
  exportSignup,
  AUDIT_STATUS_OPTIONS,
  showStatusMeta,
  auditStatusMeta,
  formatHours,
  formatActivityTime,
  qrcodeSrc,
  qrcodeContentOf
} from '@/api/oa/volunteer'
import { blobValidate, parseTime } from '@/utils/ruoyi'

/**
 * 志愿活动详情（抽屉，契约 §4.2）
 * - 基本信息 + 二维码（仅展示，链接文本可选中，不提供下载/复制）
 * - 报名统计 3 小卡 + 报名名单（筛选/勾选/批量通过/批量驳回/单条/导出/分页）
 * - 驳回 420px 弹窗，原因选填 ≤200
 */
const { proxy } = getCurrentInstance()
const emit = defineEmits(['success', 'edit'])

const visible = ref(false)
const loading = ref(false)
const loadError = ref(false)
const activityId = ref(undefined)
const detail = ref({})
const qrcode = ref(null)

const exporting = ref(false)
const auditing = ref(false)

const signupTableRef = ref()
const signupLoading = ref(false)
const signupList = ref([])
const signupTotal = ref(0)
const signupPage = reactive({ pageNum: 1, pageSize: 10 })
const signupFilters = reactive({ auditStatus: undefined, keyword: '' })
const selectedRows = ref([])

const rejectVisible = ref(false)
const rejectSubmitting = ref(false)
const rejectRows = ref([])
const rejectReason = ref('')

const auditStatusOptions = AUDIT_STATUS_OPTIONS

/* -------------------------------------------------------------------------- */
/* 派生数据                                                                   */
/* -------------------------------------------------------------------------- */
const showStatus = computed(() => showStatusMeta(detail.value.showStatus, detail.value.showStatusLabel))

const capacityText = computed(() => {
  const value = detail.value.capacity
  if (value === undefined || value === null || value === '') return '—'
  return value + ' 人（仅参考，不做硬上限）'
})

const pendingCount = computed(() => firstNumber(detail.value.pendingCount, detail.value.waitCount, 0))
const passCount = computed(() => firstNumber(detail.value.passCount, detail.value.approvedCount, 0))
const rejectCount = computed(() => firstNumber(detail.value.rejectCount, detail.value.rejectedCount, 0))

const qrcodeImage = computed(() => qrcodeSrc(qrcode.value || detail.value.qrcode))
const qrcodeLink = computed(() => qrcodeContentOf(qrcode.value) || detail.value.qrcodeContent || '')

const rejectNames = computed(() => {
  const list = rejectRows.value || []
  if (list.length <= 3) return list.map(row => row.name || '').join('、')
  return list.slice(0, 3).map(row => row.name || '').join('、') + ' 等'
})

function firstNumber() {
  for (let index = 0; index < arguments.length; index++) {
    const value = arguments[index]
    if (value === undefined || value === null) continue
    const num = Number(value)
    if (Number.isFinite(num)) return num
  }
  return 0
}

function rowAuditMeta(row) {
  return auditStatusMeta(row && row.auditStatus, row && row.auditStatusLabel)
}

function isPendingRow(row) {
  return String((row && row.auditStatus) ?? '') === '0'
}

function isRejectedRow(row) {
  return String((row && row.auditStatus) ?? '') === '2'
}

/* -------------------------------------------------------------------------- */
/* 打开 / 加载                                                                */
/* -------------------------------------------------------------------------- */
function open(options) {
  const opts = options || {}
  activityId.value = opts.activityId
  resetState()
  visible.value = true
  loadDetail()
  loadQrcode()
  loadSignups()
}

function resetState() {
  detail.value = {}
  qrcode.value = null
  loading.value = false
  loadError.value = false
  exporting.value = false
  auditing.value = false
  signupList.value = []
  signupTotal.value = 0
  signupPage.pageNum = 1
  signupPage.pageSize = 10
  signupFilters.auditStatus = undefined
  signupFilters.keyword = ''
  selectedRows.value = []
  rejectVisible.value = false
  rejectRows.value = []
  rejectReason.value = ''
}

async function loadDetail() {
  if (!activityId.value) {
    loadError.value = true
    return
  }
  loading.value = true
  loadError.value = false
  try {
    const response = await getActivity(activityId.value)
    const data = (response && response.data) || {}
    if (!data || (!data.activityId && !data.name)) {
      loadError.value = true
      detail.value = {}
    } else {
      detail.value = data
    }
  } catch (e) {
    loadError.value = true
    detail.value = {}
  } finally {
    loading.value = false
  }
}

function loadQrcode() {
  if (!activityId.value) return
  getActivityQrcode(activityId.value)
    .then(response => {
      qrcode.value = (response && response.data) || null
    })
    .catch(() => {
      // 详情里若已带 qrcode 字段，则由 qrcodeImage 计算属性兜底
      qrcode.value = null
    })
}

function loadSignups() {
  if (!activityId.value) return
  signupLoading.value = true
  const params = {
    activityId: activityId.value,
    pageNum: signupPage.pageNum,
    pageSize: signupPage.pageSize
  }
  if (signupFilters.auditStatus !== undefined && signupFilters.auditStatus !== null && signupFilters.auditStatus !== '') {
    params.auditStatus = signupFilters.auditStatus
  }
  const keyword = String(signupFilters.keyword || '').trim()
  if (keyword) params.keyword = keyword
  listSignup(params)
    .then(response => {
      signupList.value = (response && response.rows) || []
      signupTotal.value = (response && response.total) || 0
      clearSelection(false)
    })
    .catch(() => {
      signupList.value = []
      signupTotal.value = 0
    })
    .finally(() => {
      signupLoading.value = false
    })
}

/* -------------------------------------------------------------------------- */
/* 报名筛选 / 分页 / 选择                                                      */
/* -------------------------------------------------------------------------- */
function handleSignupQuery() {
  signupPage.pageNum = 1
  clearSelection(false)
  loadSignups()
}

function resetSignupQuery() {
  signupFilters.auditStatus = undefined
  signupFilters.keyword = ''
  signupPage.pageNum = 1
  clearSelection(false)
  loadSignups()
}

function handleSignupPage() {
  clearSelection(false)
  loadSignups()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows || []
}

function clearSelection(notify) {
  if (notify && selectedRows.value.length) proxy.$modal.msg('已清空选择')
  selectedRows.value = []
  if (signupTableRef.value) signupTableRef.value.clearSelection()
}

/* -------------------------------------------------------------------------- */
/* 审核                                                                       */
/* -------------------------------------------------------------------------- */
async function requestAudit(ids, auditStatus, auditRemark) {
  // 后端 SignupAuditDTO.auditStatus 是 Integer（1 通过 / 2 驳回）
  return auditSignup({
    ids: ids,
    auditStatus: Number(auditStatus),
    auditRemark: auditRemark || ''
  })
}

async function refreshAfterAudit() {
  clearSelection(false)
  await loadDetail()
  loadSignups()
  emit('success')
}

async function handlePass(row) {
  if (auditing.value) return
  auditing.value = true
  try {
    await requestAudit([row.signupId], '1', '')
    proxy.$modal.msgSuccess('已通过 1 人，统计数字已同步')
    await refreshAfterAudit()
  } catch (e) {
    // 失败原因由 request 拦截器统一提示
  } finally {
    auditing.value = false
  }
}

async function handleBatchPass() {
  const rows = selectedRows.value.slice()
  if (!rows.length) {
    proxy.$modal.msgWarning('请勾选待审核的报名')
    return
  }
  if (auditing.value) return
  auditing.value = true
  try {
    await requestAudit(rows.map(row => row.signupId), '1', '')
    proxy.$modal.msgSuccess('已通过 ' + rows.length + ' 人，统计数字已同步')
    await refreshAfterAudit()
  } catch (e) {
    // 失败原因由 request 拦截器统一提示
  } finally {
    auditing.value = false
  }
}

function openReject(rows) {
  const list = (rows || []).filter(row => isPendingRow(row))
  if (!list.length) return
  rejectRows.value = list
  rejectReason.value = ''
  rejectVisible.value = true
}

function handleBatchReject() {
  const rows = selectedRows.value.slice()
  if (!rows.length) {
    proxy.$modal.msgWarning('请勾选待审核的报名')
    return
  }
  openReject(rows)
}

async function submitReject() {
  if (rejectSubmitting.value) return
  const reason = String(rejectReason.value || '').trim()
  if (reason.length > 200) {
    proxy.$modal.msgWarning('驳回原因最多 200 字')
    return
  }
  const ids = rejectRows.value.map(row => row.signupId)
  rejectSubmitting.value = true
  try {
    await requestAudit(ids, '2', reason)
    proxy.$modal.msgSuccess('已驳回 ' + ids.length + ' 人')
    rejectVisible.value = false
    await refreshAfterAudit()
  } catch (e) {
    // 失败原因由 request 拦截器统一提示
  } finally {
    rejectSubmitting.value = false
  }
}

/* -------------------------------------------------------------------------- */
/* 导出 / 编辑                                                                */
/* -------------------------------------------------------------------------- */
async function handleExportSignup() {
  if (exporting.value || !activityId.value) return
  exporting.value = true
  try {
    const query = { activityId: activityId.value }
    if (signupFilters.auditStatus !== undefined && signupFilters.auditStatus !== null && signupFilters.auditStatus !== '') {
      query.auditStatus = signupFilters.auditStatus
    }
    const keyword = String(signupFilters.keyword || '').trim()
    if (keyword) query.keyword = keyword
    const data = await exportSignup(query)
    if (!blobValidate(data)) {
      let message = '导出失败，请稍后重试'
      try {
        const resText = await data.text()
        const rspObj = JSON.parse(resText)
        message = rspObj.msg || message
      } catch (e) {
        // 保留默认提示
      }
      proxy.$modal.msgError(message)
      return
    }
    const filename =
      '报名名单_' +
      (detail.value.name || '活动') +
      '_' +
      parseTime(new Date(), '{y}{m}{d}{h}{i}{s}') +
      '.xlsx'
    saveAs(new Blob([data]), filename)
    proxy.$modal.msgSuccess('报名名单已导出')
  } catch (e) {
    proxy.$modal.msgError('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

function handleEditActivity() {
  if (!activityId.value) return
  visible.value = false
  emit('edit', { activityId: activityId.value })
}

function handleClosed() {
  resetState()
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.va-detail-drawer :deep(.el-drawer__body) {
  padding-top: 16px;
}

.va-detail-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}

.va-detail-name {
  font-size: 16px;
  font-weight: 650;
  color: var(--ink);
}

.va-detail-no {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--ink-soft);
}

.va-detail-body {
  min-height: 240px;
}

.va-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 650;
  color: var(--ink);

  &::before {
    width: 3px;
    height: 13px;
    border-radius: 2px;
    background: var(--blue);
    content: '';
  }
}

/* ---------- 顶部：信息 + 二维码 ---------- */
.va-detail-top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  align-items: start;
  margin-bottom: 16px;
}

@media (max-width: 900px) {
  .va-detail-top {
    grid-template-columns: minmax(0, 1fr);
  }
}

.va-info-card {
  padding: 16px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-md);
  background: var(--surface);
  box-shadow: var(--edge), var(--e1);
}

.va-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
  font-size: 13px;
}

.va-info-item {
  min-width: 0;

  &.is-full {
    grid-column: 1 / -1;
  }
}

.va-info-item__k {
  font-size: 12px;
  color: var(--ink-soft);
}

.va-info-item__v {
  margin-top: 3px;
  color: var(--ink);
  line-height: 1.6;
  word-break: break-all;
}

.va-pre {
  white-space: pre-wrap;
}

/* ---------- 二维码卡片 ---------- */
.va-qr-card {
  padding: 16px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-md);
  background: var(--surface);
  box-shadow: var(--edge), var(--e1);
  text-align: center;
}

.va-qr-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 650;
  color: var(--ink);
}

.va-qr-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 200px;
  height: 200px;
  margin: 0 auto;
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
  background: #fff;
  overflow: hidden;
}

.va-qr-img {
  display: block;
  width: 100%;
  height: 100%;
}

.va-qr-tip {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink-soft);
}

.va-qr-link {
  margin-top: 10px;
  font-family: var(--font-mono);
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--blue);
  word-break: break-all;
  user-select: text;
}

/* ---------- 报名名单 ---------- */
.va-signup-card {
  padding: 16px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-md);
  background: var(--surface);
  box-shadow: var(--edge), var(--e1);
}

.va-signup-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;

  .va-section-title {
    margin-bottom: 0;
  }
}

.va-signup-head__right {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}

/* ---------- 报名统计小卡 ---------- */
.va-mini-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.va-mini-stat {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
  background: var(--hover-bg);
}

.va-mini-stat__label {
  font-size: 12.5px;
  color: var(--ink-muted);
}

.va-mini-stat__num {
  font-size: 20px;
  font-weight: 650;
  line-height: 1.2;
}

.va-mini-stat.is-wait {
  border-color: var(--warn-soft);
  background: var(--warn-soft);

  .va-mini-stat__num {
    color: var(--warn-text);
  }
}

.va-mini-stat.is-pass {
  border-color: var(--lime-soft);
  background: var(--lime-soft);

  .va-mini-stat__num {
    color: var(--lime-text);
  }
}

.va-mini-stat.is-rej {
  border-color: var(--red-soft);
  background: var(--red-soft);

  .va-mini-stat__num {
    color: var(--red-text);
  }
}

.va-detail-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--ink-muted);
}

.va-stat b {
  margin-left: 5px;
  font-size: 16px;
}

.va-stat.is-wait b {
  color: var(--warn-text);
}

.va-stat.is-pass b {
  color: var(--lime-text);
}

.va-stat.is-rej b {
  color: var(--red-text);
}

.va-signup-filter {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 0 0 12px;
  margin-bottom: 6px;
}

.va-batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  padding: 8px 12px;
  border: 1px solid var(--blue-soft);
  border-radius: var(--r-xs);
  background: var(--blue-soft);
  font-size: 13px;
  color: var(--blue);
}

.va-batch-count b {
  margin: 0 2px;
  font-size: 15px;
}

.va-batch-grow {
  flex: 1;
}

.va-name {
  color: var(--ink);
  font-weight: 600;
}

.va-mono {
  font-family: var(--font-mono);
  font-size: 12.5px;
  color: var(--ink-muted);
}

.va-reason {
  margin-top: 3px;
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--red-text);
  word-break: break-all;
}

.va-audit-time {
  margin-top: 2px;
  font-size: 11.5px;
}

/* ---------- 驳回弹窗 ---------- */
.va-reject-target {
  margin-bottom: 10px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-muted);
}

/* ---------- 底部 / 错误态 ---------- */
.va-detail-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.va-detail-error {
  padding: 56px 0;
  text-align: center;
}

.va-detail-error__title {
  margin-bottom: 8px;
  font-size: 15px;
  color: var(--ink-muted);
}

.va-detail-error__actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
}
</style>
