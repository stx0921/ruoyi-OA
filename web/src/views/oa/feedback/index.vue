<template>
  <div class="app-container fb-page">
    <div class="fb-page-head">
      <div class="fb-page-head__main">
        <div class="oa-page-title">意见反馈</div>
        <p class="fb-page-desc">
          有问题、建议、新功能都可以提；全部工单对内部成员透明可见，提交人可在「待处理」状态下撤回自己的工单。
        </p>
      </div>
      <el-button
        v-hasPermi="['oa:feedback:add']"
        type="primary"
        icon="Plus"
        @click="handleSubmitTicket"
      >提交工单</el-button>
    </div>

    <div class="oa-card">
      <!-- 全部 / 我的 Tab -->
      <el-tabs v-model="activeTab" class="fb-tabs" @tab-change="handleTabChange">
        <el-tab-pane name="all">
          <template #label>
            全部工单
            <span class="fb-tab-num">{{ scopeTotals.all === null ? '—' : scopeTotals.all }}</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="mine">
          <template #label>
            我的工单
            <span class="fb-tab-num">{{ scopeTotals.mine === null ? '—' : scopeTotals.mine }}</span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 筛选栏 -->
      <el-form :model="filters" :inline="true" class="oa-filter-bar" @submit.prevent="handleQuery">
        <el-form-item label="类型">
          <el-select v-model="filters.type" placeholder="全部类型" clearable style="width: 140px">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="编号 / 标题 / 描述"
            clearable
            style="width: 240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            unlink-panels
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" :loading="loading" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 工单表格 -->
      <el-table
        v-loading="loading"
        :data="list"
        row-key="feedbackId"
        class="fb-table"
        :row-class-name="() => 'fb-row'"
        empty-text="没有符合条件的工单，试试调整筛选条件"
        @row-click="handleRowClick"
      >
        <el-table-column label="工单编号" width="150">
          <template #default="scope">
            <span class="fb-ticket-no">{{ scope.row.ticketNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="116">
          <template #default="scope">
            <span v-if="scope.row.type" class="fb-type-tag" :class="'tone-' + rowTypeMeta(scope.row).tone">
              {{ rowTypeMeta(scope.row).icon }} {{ rowTypeMeta(scope.row).label }}
            </span>
            <span v-else class="oa-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="220" show-overflow-tooltip>
          <template #default="scope">
            <span class="fb-title">{{ scope.row.title || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="影响模块" width="190" show-overflow-tooltip>
          <template #default="scope">{{ rowModuleText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="提交人" width="110" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.submitterName || '—' }}</span>
            <span
              v-if="isAnonymousRow(scope.row) && scope.row.submitterName !== '匿名用户'"
              class="fb-anon-tag"
            >匿名</span>
          </template>
        </el-table-column>
        <el-table-column label="附件" width="96">
          <template #default="scope">{{ rowAttachmentText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96" align="center">
          <template #default="scope">
            <span class="oa-pill" :class="'oa-pill--' + rowStatusMeta(scope.row).tone">
              {{ rowStatusMeta(scope.row).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150" align="center">
          <template #default="scope">{{ rowTimeText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" @click.stop="handleDetail(scope.row)">查看</el-button>
            <el-button
              v-if="canWithdrawRow(scope.row)"
              v-hasPermi="['oa:feedback:withdraw']"
              link
              type="danger"
              @click.stop="handleWithdraw(scope.row)"
            >撤回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="page.pageNum"
        v-model:limit="page.pageSize"
        :page-sizes="[10, 20, 50]"
        @pagination="getList"
      />
    </div>

    <!-- 提交工单（760px） -->
    <feedback-submit ref="submitRef" @success="handleRefresh" />
    <!-- 工单详情（620px） -->
    <feedback-detail ref="detailRef" @success="handleRefresh" />
  </div>
</template>

<script setup name="FeedbackIndex">
import {
  listFeedback,
  withdrawFeedback,
  FEEDBACK_TYPE_OPTIONS,
  FEEDBACK_STATUS_OPTIONS,
  feedbackTypeMeta,
  feedbackStatusMeta,
  feedbackAttachmentSummary,
  formatFeedbackTime
} from '@/api/oa/feedback'
import FeedbackSubmit from './Submit.vue'
import FeedbackDetail from './Detail.vue'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const { oa_feedback_type, oa_feedback_status } = useDict('oa_feedback_type', 'oa_feedback_status')

const submitRef = ref()
const detailRef = ref()

const activeTab = ref('all')
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
// Tab 角标只做辅助展示，接口返回后回填
const scopeTotals = reactive({ all: null, mine: null })

const filters = reactive({
  type: undefined,
  status: undefined,
  keyword: '',
  dateRange: []
})

const typeOptions = computed(() =>
  FEEDBACK_TYPE_OPTIONS.map(item => ({
    value: item.value,
    label: feedbackTypeMeta(item.value, oa_feedback_type.value).label
  }))
)
const statusOptions = computed(() =>
  FEEDBACK_STATUS_OPTIONS.map(item => ({
    value: item.value,
    label: feedbackStatusMeta(item.value, oa_feedback_status.value).label
  }))
)

/* -------------------------------------------------------------------------- */
/* 展示辅助                                                                   */
/* -------------------------------------------------------------------------- */
function rowTimeText(row) {
  if (!row) return '—'
  return formatFeedbackTime(row.submitTime || row.createTime)
}

function rowTypeMeta(row) {
  const meta = feedbackTypeMeta(row && row.type, oa_feedback_type.value)
  return { ...meta, label: (row && row.typeLabel) || meta.label }
}

/** 后端 isAnonymous 是字符串 '0'/'1' */
function isAnonymousRow(row) {
  return String((row && row.isAnonymous) ?? '') === '1'
}

function rowStatusMeta(row) {
  const meta = feedbackStatusMeta(row && row.status, oa_feedback_status.value)
  return { ...meta, label: (row && row.statusLabel) || meta.label }
}

function rowModuleText(row) {
  const moduleName = (row && row.moduleName) || ''
  const pageName = (row && row.pageName) || ''
  if (!moduleName && !pageName) return '—'
  if (!pageName) return moduleName
  if (!moduleName) return pageName
  return moduleName + ' / ' + pageName
}

function rowAttachmentText(row) {
  const summary = feedbackAttachmentSummary(row)
  if (!summary.total) return '—'
  const parts = []
  if (summary.images) parts.push('🖼 ' + summary.images)
  if (summary.words) parts.push('📄 ' + summary.words)
  return parts.length ? parts.join(' ') : '📎 ' + summary.total
}

function canWithdrawRow(row) {
  if (!row) return false
  // 后端若直接下发 canWithdraw / isMine（匿名本人场景只能靠后端判断），优先采用
  if (row.canWithdraw === true || row.isMine === true || row.mine === true) return true
  if (row.canWithdraw === false) return false
  if (String(row.status) !== '0') return false
  if (row.submitterId === undefined || row.submitterId === null) return false
  return String(row.submitterId) === String(userStore.id)
}

/* -------------------------------------------------------------------------- */
/* 列表查询                                                                   */
/* -------------------------------------------------------------------------- */
function buildQuery() {
  const params = {
    scope: activeTab.value,
    pageNum: page.pageNum,
    pageSize: page.pageSize
  }
  if (filters.type) params.type = filters.type
  if (filters.status !== undefined && filters.status !== null && filters.status !== '') {
    params.status = filters.status
  }
  const keyword = String(filters.keyword || '').trim()
  if (keyword) params.keyword = keyword
  if (filters.dateRange && filters.dateRange.length === 2) {
    params.beginTime = filters.dateRange[0]
    params.endTime = filters.dateRange[1]
  }
  return params
}

function getList() {
  loading.value = true
  listFeedback(buildQuery())
    .then(response => {
      list.value = (response && response.rows) || []
      total.value = (response && response.total) || 0
      // Tab 角标保持「全部 / 我的」全量计数，不被筛选结果覆盖（筛选结果看分页 total）
    })
    .catch(() => {
      list.value = []
      total.value = 0
    })
    .finally(() => {
      loading.value = false
    })
}

/** 角标：两个 Tab 的总数各查一次（pageSize=1，代价很小） */
function loadScopeTotals() {
  ;['all', 'mine'].forEach(scope => {
    listFeedback({ scope, pageNum: 1, pageSize: 1 })
      .then(response => {
        scopeTotals[scope] = (response && response.total) || 0
      })
      .catch(() => {})
  })
}

function handleQuery() {
  page.pageNum = 1
  getList()
}

function resetQuery() {
  filters.type = undefined
  filters.status = undefined
  filters.keyword = ''
  filters.dateRange = []
  page.pageNum = 1
  getList()
}

function handleTabChange() {
  page.pageNum = 1
  getList()
}

function handleRefresh() {
  getList()
  loadScopeTotals()
}

/* -------------------------------------------------------------------------- */
/* 行操作                                                                     */
/* -------------------------------------------------------------------------- */
function handleSubmitTicket() {
  submitRef.value && submitRef.value.open()
}

function handleDetail(row) {
  detailRef.value && detailRef.value.open({ feedbackId: row.feedbackId })
}

function handleRowClick(row, column) {
  if (column && column.type === 'selection') return
  handleDetail(row)
}

function handleWithdraw(row) {
  const ticketNo = row.ticketNo || ''
  proxy.$modal
    .confirm('撤回后该工单将从列表移除（仅「待处理」状态可撤回），确定撤回 ' + ticketNo + ' 吗？')
    .then(() => withdrawFeedback(row.feedbackId))
    .then(() => {
      proxy.$modal.msgSuccess('已撤回：工单已从列表移除')
      handleRefresh()
    })
    .catch(() => {})
}

/* -------------------------------------------------------------------------- */
/* 初始化                                                                     */
/* -------------------------------------------------------------------------- */
getList()
loadScopeTotals()
</script>

<style scoped lang="scss">
.fb-page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.fb-page-head__main {
  min-width: 0;
}

.fb-page-desc {
  margin: -8px 0 0;
  max-width: 760px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-soft);
}

.fb-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 14px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
  }
}

.fb-tab-num {
  display: inline-block;
  margin-left: 5px;
  padding: 0 5px;
  border-radius: 8px;
  background: var(--neutral-soft);
  color: var(--ink-soft);
  font-size: 12px;
  line-height: 18px;
}

.fb-table {
  :deep(.fb-row) {
    cursor: pointer;
  }
}

.fb-ticket-no {
  font-family: var(--font-mono);
  font-size: 12.5px;
  color: var(--ink-muted);
}

.fb-title {
  color: var(--ink);
  font-weight: 550;
}

/* ---------- 类型标签 ---------- */
.fb-type-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 8px;
  border: 1px solid var(--line-strong);
  border-radius: 3px;
  font-size: 12px;
  white-space: nowrap;

  &.tone-red {
    border-color: var(--red-soft);
    background: var(--red-soft);
    color: var(--red-text);
  }

  &.tone-blue {
    border-color: var(--blue-soft);
    background: var(--blue-soft);
    color: var(--blue);
  }

  &.tone-green {
    border-color: var(--lime-soft);
    background: var(--lime-soft);
    color: var(--lime-text);
  }

  &.tone-gray {
    border-color: var(--neutral-soft);
    background: var(--neutral-soft);
    color: var(--neutral-text);
  }
}

.fb-anon-tag {
  display: inline-block;
  margin-left: 5px;
  padding: 0 5px;
  border: 1px solid var(--line-strong);
  border-radius: 3px;
  background: var(--neutral-soft);
  color: var(--neutral-text);
  font-size: 11px;
  line-height: 16px;
  vertical-align: middle;
}
</style>
