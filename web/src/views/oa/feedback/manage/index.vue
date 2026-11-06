<template>
  <div class="app-container feedback-manage-page">
    <div class="oa-page-title">
      工单管理
      <small>管理员可见全部工单（含匿名提交人的真实身份），支持单次官方回复、批量标记已回复、批量删除；删除为逻辑删除，不提供回收站页面。</small>
    </div>

    <!-- 4 统计卡：待处理 / 已回复 / 今日新增 / 本周新增 -->
    <div class="fb-stat-grid">
      <div
        v-for="card in statCards"
        :key="card.key"
        class="fb-stat is-clickable"
        :class="'is-' + card.tone"
        @click="handleStatClick(card.key)"
      >
        <div class="fb-stat__ico">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="fb-stat__body">
          <div class="fb-stat__num">{{ statsUnavailable ? '--' : statValue(card.key) }}</div>
          <div class="fb-stat__label">{{ card.label }}</div>
        </div>
      </div>
    </div>
    <div class="fb-stat-note" :class="{ 'is-error': statsError }" @click="statsError && getStats()">
      {{ statsError ? '统计加载失败，点击重试' : '统计口径：待处理 / 已回复按工单状态统计，今日新增 / 本周新增按提交时间统计。' }}
    </div>

    <div class="oa-card">
      <!-- 筛选：类型 / 状态 / 提交人 / 关键词 / 时间范围 -->
      <el-form :model="queryParams" :inline="true" class="oa-filter-bar" @submit.prevent>
        <el-form-item label="类型">
          <el-select v-model="queryParams.type" placeholder="全部类型" clearable style="width: 140px">
            <el-option
              v-for="item in FEEDBACK_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option
              v-for="item in FEEDBACK_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="提交人">
          <el-select
            v-model="queryParams.submitterId"
            placeholder="全部提交人"
            clearable
            filterable
            style="width: 160px"
          >
            <el-option
              v-for="item in submitterOptions"
              :key="'submitter-' + item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="编号 / 标题 / 描述"
            clearable
            style="width: 220px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 300px"
            unlink-panels
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 批量操作条 -->
      <div v-if="selectedRows.length" class="fb-batch-bar">
        <span class="fb-batch-bar__count">已选 <b>{{ selectedRows.length }}</b> 条</span>
        <span class="fb-batch-bar__hint">勾选后可批量处理</span>
        <span class="fb-batch-bar__spacer"></span>
        <el-button
          type="success"
          size="small"
          :loading="batchRepliedLoading"
          @click="handleBatchReplied"
          v-hasPermi="['oa:feedback:reply']"
        >批量标记已回复</el-button>
        <el-button
          type="danger"
          plain
          size="small"
          :loading="batchDeleteLoading"
          @click="handleBatchDelete"
          v-hasPermi="['oa:feedback:remove']"
        >批量删除</el-button>
        <el-button size="small" @click="clearSelection">清空选择</el-button>
      </div>

      <!-- 错误态 -->
      <div v-if="listError" class="fb-state">
        <el-icon :size="34"><WarningFilled /></el-icon>
        <p>工单列表加载失败</p>
        <el-button size="small" type="primary" @click="getList">重新加载</el-button>
      </div>

      <!-- 列表 -->
      <el-table
        v-else
        ref="tableRef"
        v-loading="loading"
        :data="manageList"
        row-key="feedbackId"
        class="fb-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column label="工单编号" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="fb-mono">{{ row.ticketNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <span class="oa-pill" :class="'oa-pill--' + typeMeta(row).tone">
              <span class="fb-type-icon">{{ typeMeta(row).icon }}</span>{{ typeMeta(row).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="fb-title" @click="openDrawer(row)">{{ row.title || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="影响模块" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ moduleText(row) }}</template>
        </el-table-column>
        <el-table-column label="提交人" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.submitterName || '—' }}</span>
            <span v-if="isAnonymous(row)" class="fb-anon">匿名</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="oa-pill" :class="'oa-pill--' + statusMeta(row).tone">
              {{ statusMeta(row).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160" align="center">
          <template #default="{ row }">
            <span class="fb-time">{{ oaDateTime(submitTime(row)) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDrawer(row)" v-hasPermi="['oa:feedback:reply']">处理</el-button>
            <el-button link type="danger" @click="handleDelete(row)" v-hasPermi="['oa:feedback:remove']">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <div class="fb-empty">
            <el-icon :size="34"><ChatDotRound /></el-icon>
            <p>{{ emptyText }}</p>
          </div>
        </template>
      </el-table>

      <pagination
        v-show="!listError && total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @pagination="getList"
      />
    </div>

    <!-- 处理抽屉 -->
    <reply-drawer
      v-model="drawerVisible"
      :feedback-id="currentFeedbackId"
      :row="currentRow"
      @saved="handleDrawerChanged"
      @deleted="handleDrawerChanged"
    />
  </div>
</template>

<script setup name="FeedbackManage">
import {
  batchReplyFeedback,
  deleteFeedback,
  getFeedbackStats,
  getManageList,
  FEEDBACK_STATUS_OPTIONS,
  FEEDBACK_TYPE_OPTIONS,
  feedbackStatusMeta,
  feedbackTypeMeta
} from '@/api/oa/feedback'
import { oaDateTime } from '@/utils/oa'
import ReplyDrawer from './ReplyDrawer.vue'

const { proxy } = getCurrentInstance()

/** 统计卡结构 */
const statCards = [
  { key: 'pending', label: '待处理', icon: 'Timer', tone: 'orange' },
  { key: 'replied', label: '已回复', icon: 'CircleCheck', tone: 'green' },
  { key: 'today', label: '今日新增', icon: 'Calendar', tone: 'blue' },
  { key: 'week', label: '本周新增', icon: 'TrendCharts', tone: 'red' }
]

const tableRef = ref()
const loading = ref(false)
const listError = ref(false)
const manageList = ref([])
const total = ref(0)
const selectedRows = ref([])

const stats = ref({})
const statsLoading = ref(false)
const statsError = ref(false)
const statsUnavailable = computed(() => statsLoading.value || statsError.value)

const dateRange = ref([])
const submitterOptions = ref([])

const drawerVisible = ref(false)
const currentFeedbackId = ref(null)
const currentRow = ref({})

const batchRepliedLoading = ref(false)
const batchDeleteLoading = ref(false)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  type: undefined,
  status: undefined,
  submitterId: undefined,
  keyword: ''
})

const emptyText = computed(() => {
  const hasFilter =
    queryParams.value.type ||
    queryParams.value.status ||
    queryParams.value.submitterId ||
    queryParams.value.keyword ||
    (dateRange.value && dateRange.value.length)
  return hasFilter ? '没有符合条件的工单，试试调整筛选条件' : '暂无工单'
})

/* -------------------------------------------------------------------------- */
/* 字段兜底                                                                   */
/* -------------------------------------------------------------------------- */
function rowId(row) {
  return row ? row.feedbackId ?? row.id : null
}

function submitTime(row) {
  return row ? row.submitTime || row.createTime : ''
}

function moduleText(row) {
  if (!row) return '—'
  const text = [row.moduleName, row.pageName].filter(Boolean).join(' / ')
  return text || '—'
}

function isAnonymous(row) {
  if (!row) return false
  const value = row.isAnonymous
  return value === true || value === 1 || String(value) === '1'
}

function typeMeta(row) {
  const meta = feedbackTypeMeta(row ? row.type : '')
  return { ...meta, label: (row && row.typeLabel) || meta.label }
}

function statusMeta(row) {
  const meta = feedbackStatusMeta(row ? row.status : '')
  return { ...meta, label: (row && row.statusLabel) || meta.label }
}

function statValue(key) {
  const data = stats.value || {}
  const map = {
    pending: data.pendingCount,
    replied: data.repliedCount,
    today: data.todayCount,
    week: data.weekCount
  }
  const value = map[key]
  return value === null || value === undefined ? 0 : value
}

/* -------------------------------------------------------------------------- */
/* 统计卡                                                                     */
/* -------------------------------------------------------------------------- */
function getStats() {
  statsLoading.value = true
  statsError.value = false
  getFeedbackStats()
    .then(res => {
      stats.value = (res && res.data) || {}
    })
    .catch(() => {
      stats.value = {}
      statsError.value = true
    })
    .finally(() => {
      statsLoading.value = false
    })
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function todayRange() {
  const today = formatDate(new Date())
  return [today, today]
}

/** 本周一 00:00 起 */
function weekRange() {
  const now = new Date()
  const monday = new Date(now)
  const offset = (now.getDay() + 6) % 7
  monday.setDate(now.getDate() - offset)
  return [formatDate(monday), formatDate(now)]
}

function handleStatClick(key) {
  // 统计卡是独立视图：先清空其它筛选，避免卡片数字与列表 total 不一致
  queryParams.value.type = undefined
  queryParams.value.submitterId = undefined
  queryParams.value.keyword = ''
  if (key === 'pending') {
    queryParams.value.status = '0'
    dateRange.value = []
  } else if (key === 'replied') {
    queryParams.value.status = '1'
    dateRange.value = []
  } else if (key === 'today') {
    queryParams.value.status = undefined
    dateRange.value = todayRange()
  } else {
    queryParams.value.status = undefined
    dateRange.value = weekRange()
  }
  queryParams.value.pageNum = 1
  clearSelection()
  getList()
}

/* -------------------------------------------------------------------------- */
/* 列表                                                                       */
/* -------------------------------------------------------------------------- */
function buildQuery() {
  const query = {
    pageNum: queryParams.value.pageNum,
    pageSize: queryParams.value.pageSize
  }
  if (queryParams.value.type) query.type = queryParams.value.type
  if (queryParams.value.status) query.status = queryParams.value.status
  if (queryParams.value.submitterId) query.submitterId = queryParams.value.submitterId
  const keyword = (queryParams.value.keyword || '').trim()
  if (keyword) query.keyword = keyword
  if (dateRange.value && dateRange.value.length === 2) {
    query.beginTime = dateRange.value[0]
    query.endTime = dateRange.value[1]
  }
  return query
}

/** 提交人下拉：后端无独立接口，从已加载列表累积（管理端接口返回真实提交人） */
function mergeSubmitterOptions(rows) {
  const map = new Map(submitterOptions.value.map(item => [String(item.value), item]))
  ;(rows || []).forEach(row => {
    const id = row.submitterId
    if (id === null || id === undefined || id === '') return
    const key = String(id)
    if (!map.has(key)) {
      map.set(key, { value: id, label: row.submitterName || '用户' + id })
    }
  })
  submitterOptions.value = [...map.values()]
}

function clearSelection() {
  selectedRows.value = []
  if (tableRef.value) tableRef.value.clearSelection()
}

function getList() {
  loading.value = true
  listError.value = false
  clearSelection()
  getManageList(buildQuery())
    .then(res => {
      manageList.value = (res && res.rows) || []
      total.value = (res && res.total) || 0
      mergeSubmitterOptions(manageList.value)
    })
    .catch(() => {
      manageList.value = []
      total.value = 0
      listError.value = true
    })
    .finally(() => {
      loading.value = false
    })
}

function handleSelectionChange(rows) {
  selectedRows.value = rows || []
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.value.type = undefined
  queryParams.value.status = undefined
  queryParams.value.submitterId = undefined
  queryParams.value.keyword = ''
  dateRange.value = []
  queryParams.value.pageNum = 1
  getList()
}

function refreshAll() {
  getList()
  getStats()
}

/* -------------------------------------------------------------------------- */
/* 删除 / 批量                                                                */
/* -------------------------------------------------------------------------- */
function handleDelete(row) {
  const id = rowId(row)
  if (!id) return
  proxy.$modal
    .confirm('删除后不可在列表查看，确定删除？')
    .then(() => deleteFeedback([id]))
    .then(() => {
      proxy.$modal.msgSuccess(`已删除工单 ${row.ticketNo || ''}`.trim())
      refreshAll()
    })
    .catch(() => {})
}

function handleBatchReplied() {
  const rows = selectedRows.value
  if (!rows.length) return
  proxy.$modal
    .confirm(`将把已选的 ${rows.length} 条工单中「待处理」的标记为已回复；已回复的不会被覆盖，确定继续吗？`)
    .then(() => {
      batchRepliedLoading.value = true
      return batchReplyFeedback({ ids: rows.map(row => rowId(row)).filter(Boolean) })
    })
    .then(response => {
      const done = Number(response && response.data)
      const count = Number.isFinite(done) && done >= 0 ? done : rows.length
      proxy.$modal.msgSuccess(`已批量标记 ${count} 条工单为已回复（已回复的工单自动跳过）`)
      refreshAll()
    })
    .catch(() => {})
    .finally(() => {
      batchRepliedLoading.value = false
    })
}

function handleBatchDelete() {
  const rows = selectedRows.value
  if (!rows.length) return
  if (rows.length > 200) {
    proxy.$modal.msgWarning('单次最多删除 200 条，请分批操作')
    return
  }
  proxy.$modal
    .confirm(`删除后不可在列表查看（逻辑删除，无回收站），确定删除已选的 ${rows.length} 条工单吗？`)
    .then(() => {
      batchDeleteLoading.value = true
      return deleteFeedback(rows.map(row => rowId(row)).filter(Boolean))
    })
    .then(() => {
      proxy.$modal.msgSuccess(`已删除 ${rows.length} 条工单`)
      refreshAll()
    })
    .catch(() => {})
    .finally(() => {
      batchDeleteLoading.value = false
    })
}

/* -------------------------------------------------------------------------- */
/* 抽屉                                                                       */
/* -------------------------------------------------------------------------- */
function openDrawer(row) {
  currentRow.value = row || {}
  currentFeedbackId.value = rowId(row)
  drawerVisible.value = true
}

function handleDrawerChanged() {
  refreshAll()
}

getStats()
getList()
</script>

<style scoped lang="scss">
.feedback-manage-page {
  .oa-page-title small {
    margin-left: 10px;
    font-size: 12px;
    font-weight: 400;
    color: var(--ink-soft);
  }
}

/* ---------- 统计卡 ---------- */
.fb-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 10px;
}

.fb-stat {
  display: flex;
  align-items: center;
  gap: 14px;
  height: 96px;
  padding: 0 20px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-md);
  background: var(--surface);
  box-shadow: var(--edge), var(--e1);
  transition: box-shadow 0.2s ease, transform 0.2s ease;

  &.is-clickable {
    cursor: pointer;

    &:hover {
      transform: translateY(-1px);
      box-shadow: var(--edge), var(--e2);
    }
  }
}

.fb-stat__ico {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  border-radius: var(--r-sm);
  background: var(--neutral-soft);
  color: var(--neutral-text);

  .is-orange & {
    background: var(--warn-soft);
    color: var(--warn-text);
  }

  .is-green & {
    background: var(--lime-soft);
    color: var(--lime-text);
  }

  .is-blue & {
    background: var(--blue-soft);
    color: var(--blue);
  }

  .is-red & {
    background: var(--red-soft);
    color: var(--red-text);
  }
}

.fb-stat__num {
  font-family: var(--font-mono);
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--ink);
}

.fb-stat__label {
  margin-top: 3px;
  font-size: 13px;
  color: var(--ink-soft);
  white-space: nowrap;
}

.fb-stat-note {
  margin: 0 0 16px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink-faint);

  &.is-error {
    color: var(--red-text);
    cursor: pointer;
  }
}

/* ---------- 批量操作条 ---------- */
.fb-batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 48px;
  padding: 8px 14px;
  margin-bottom: 12px;
  border: 1px solid var(--glass-line);
  border-radius: var(--r-sm);
  background: var(--blue-soft);
  box-shadow: var(--edge), var(--e1);
  font-size: 13px;
  color: var(--ink-muted);
}

.fb-batch-bar__count {
  font-weight: 600;

  b {
    margin: 0 2px;
    font-family: var(--font-mono);
    font-size: 15px;
    color: var(--blue);
  }
}

.fb-batch-bar__hint {
  font-size: 12px;
  color: var(--ink-soft);
}

.fb-batch-bar__spacer {
  flex: 1;
}

/* ---------- 表格 ---------- */
.fb-table {
  :deep(.el-table__row) {
    height: 48px;
  }
}

.fb-mono {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--ink-muted);
}

.fb-type-icon {
  margin-right: 4px;
  font-size: 12px;
}

.fb-title {
  color: var(--ink);
  font-weight: 500;
  cursor: pointer;

  &:hover {
    color: var(--blue);
  }
}

.fb-time {
  font-size: 12px;
  color: var(--ink-soft);
}

.fb-anon {
  display: inline-block;
  margin-left: 6px;
  padding: 0 5px;
  border: 1px solid var(--line-strong);
  border-radius: 999px;
  background: var(--neutral-soft);
  color: var(--neutral-text);
  font-size: 11px;
  line-height: 16px;
  vertical-align: middle;
}

/* ---------- 空态 / 错误态 ---------- */
.fb-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 38px 0;
  color: var(--ink-faint);
  font-size: 13px;

  p {
    margin: 0;
  }
}

.fb-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 48px 0;
  color: var(--ink-soft);
  font-size: 13px;

  p {
    margin: 0;
  }
}

@media (max-width: 1200px) {
  .fb-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
