<template>
  <div class="app-container">
    <div class="oa-page-title">
      入会审批 <small>审批通过后自动建立成员档案并开通账号</small>
    </div>

    <div class="oa-card">
      <!-- Tab：待我审批（数量角标）/ 我已审批 / 全部 -->
      <div class="oa-tabs">
        <div
          v-for="tab in TABS"
          :key="tab.value"
          class="oa-tabs__item"
          :class="{ 'is-active': scope === tab.value }"
          @click="handleTabChange(tab.value)"
        >
          {{ tab.label }}
          <span class="oa-tabs__num">{{ countText(tab.value) }}</span>
        </div>
      </div>

      <!-- 筛选栏：条件变化不自动请求，点查询 / 回车才发起 -->
      <el-form ref="queryRef" :model="queryParams" :inline="true" class="oa-filter-bar" @submit.prevent>
        <el-form-item label="关键字" prop="keyword">
          <el-input
            v-model="queryParams.keyword"
            placeholder="姓名 / 学号"
            clearable
            style="width: 240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="意愿部门" prop="deptId">
          <el-select v-model="queryParams.deptId" placeholder="全部" clearable filterable style="width: 160px">
            <el-option
              v-for="dept in deptOptions"
              :key="dept.deptId"
              :label="dept.deptName"
              :value="dept.deptId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option
              v-for="item in APPLY_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-select v-model="queryParams.grade" placeholder="全部" clearable style="width: 120px">
            <el-option
              v-for="item in GRADE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="提交时间">
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

      <!-- 无部长提示：该部门未设置部长时由主席团 / 管理员代审 -->
      <el-alert
        v-if="showNoLeaderTip"
        class="oa-apply__alert"
        type="warning"
        show-icon
        :closable="true"
        title="该部门未设置部长，申请由主席团代审"
      />

      <!-- 批量操作条：选中 ≥1 行出现 -->
      <div v-if="selectedRows.length" class="oa-batch-bar">
        <span class="oa-batch-bar__count">已选 <b>{{ selectedRows.length }}</b> 项</span>
        <span class="oa-batch-bar__hint">同意后自动入册并开通账号</span>
        <span class="oa-batch-bar__spacer"></span>
        <el-button link @click="clearSelection(false)">清空选择</el-button>
        <el-tooltip
          :disabled="selectedRows.length <= 50"
          content="单次最多同意 50 条，请分批操作"
          placement="top"
        >
          <span>
            <el-button
              type="success"
              :loading="auditSubmitting"
              :disabled="selectedRows.length > 50"
              @click="handleBatchApprove"
              v-hasPermi="['oa:apply:audit']"
            >批量同意</el-button>
          </span>
        </el-tooltip>
      </div>

      <!-- 错误态 -->
      <div v-if="listError" class="oa-load-error">
        <el-icon><WarningFilled /></el-icon>
        <p>审批列表加载失败</p>
        <el-button type="primary" plain @click="getList">重新加载</el-button>
      </div>

      <!-- 列表 -->
      <el-table
        v-else
        v-loading="loading"
        :data="applyList"
        row-key="id"
        class="oa-apply-table"
      >
        <!-- 勾选：仅待审批行可选中；表头全选只选当前页可勾选行 -->
        <el-table-column width="48" align="center">
          <template #header>
            <el-checkbox
              :model-value="allSelected"
              :indeterminate="someSelected"
              :disabled="!selectableRows.length"
              @change="toggleAll"
            />
          </template>
          <template #default="{ row }">
            <el-tooltip
              :disabled="isSelectable(row)"
              content="该申请已处理"
              placement="top"
            >
              <span class="oa-apply__check">
                <el-checkbox
                  :model-value="isSelected(row)"
                  :disabled="!isSelectable(row)"
                  @change="toggleRow(row)"
                />
              </span>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column label="申请编号" width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="oa-link" @click="openDetail(row)">{{ row.applyNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="姓名" width="90" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="oa-strong">{{ row.name || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学号" width="110" align="center" prop="studentNo" show-overflow-tooltip />
        <el-table-column label="年级" width="80" align="center">
          <template #default="{ row }">{{ row.gradeLabel || gradeText(row.grade) }}</template>
        </el-table-column>
        <el-table-column label="意愿部门" width="110" align="center" prop="deptName" show-overflow-tooltip />
        <el-table-column label="提交时间" width="160" align="center">
          <template #default="{ row }">{{ oaDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="oa-pill" :class="'oa-pill--' + applyStatusTone(row.status)">
              {{ statusTextOf(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="审批人" width="100" align="center">
          <template #default="{ row }">
            <template v-if="row.auditUserName">
              <span>{{ auditName(row) }}</span>
              <span v-if="isProxy(row)" class="oa-pill oa-pill--orange oa-proxy">代审</span>
            </template>
            <span v-else class="oa-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <template v-if="String(row.status) === '0'">
              <el-tooltip
                :disabled="row.canAudit !== false"
                content="该申请当前不可审批"
                placement="top"
              >
                <span>
                  <el-button
                    link
                    type="success"
                    :disabled="auditSubmitting || row.canAudit === false"
                    @click="handleApprove(row)"
                    v-hasPermi="['oa:apply:audit']"
                  >同意</el-button>
                </span>
              </el-tooltip>
              <el-tooltip
                :disabled="row.canAudit !== false"
                content="该申请当前不可审批"
                placement="top"
              >
                <span>
                  <el-button
                    link
                    type="danger"
                    :disabled="auditSubmitting || row.canAudit === false"
                    @click="handleReject(row)"
                    v-hasPermi="['oa:apply:audit']"
                  >驳回</el-button>
                </span>
              </el-tooltip>
            </template>
          </template>
        </el-table-column>

        <template #empty>
          <div class="oa-apply-empty">
            <template v-if="scope === 'todo'">
              <p class="oa-apply-empty__title">当前没有需要你审批的申请</p>
              <p class="oa-apply-empty__desc">新申请会出现在这里，通过后自动入册并开通账号</p>
            </template>
            <template v-else-if="scope === 'done'">
              <p class="oa-apply-empty__title">暂无审批记录</p>
            </template>
            <template v-else>
              <p class="oa-apply-empty__title">暂无申请</p>
            </template>
          </div>
        </template>
      </el-table>

      <pagination
        v-show="!listError && total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :page-sizes="[10, 20, 50]"
        @pagination="getList"
      />
    </div>

    <!-- 审批详情抽屉 -->
    <apply-detail-drawer
      v-model="detailVisible"
      :apply-row="currentRow"
      @approve="handleApprove"
      @reject="handleReject"
    />

    <!-- 驳回理由弹窗（理由必填 2–200 字 + 快捷理由 + 字数计数） -->
    <el-dialog
      v-model="rejectVisible"
      title="驳回入会申请"
      width="760px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="oa-reject__notice">
        驳回原因会在 H5「报名进度」同步展示给学生；学生可修改后重提，历史记录保留。
      </div>
      <div class="oa-reject__info">
        <span>申请人：<b class="oa-strong">{{ rejectRow.name || '—' }}</b></span>
        <span>申请编号：{{ rejectRow.applyNo || '—' }}</span>
      </div>
      <el-form :model="rejectForm" label-width="88px">
        <el-form-item label="驳回理由" required>
          <el-input
            v-model="rejectForm.remark"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请填写具体原因，例如：手机号格式有误，请核对后重新提交"
          />
          <div class="oa-reject__quick">
            <span class="oa-muted">快捷理由：</span>
            <el-button
              v-for="reason in QUICK_REASONS"
              :key="reason"
              size="small"
              :type="rejectForm.remark === reason ? 'primary' : 'default'"
              plain
              @click="applyQuickReason(reason)"
            >{{ reason }}</el-button>
          </div>
          <p v-if="rejectHint" class="oa-reject__hint">{{ rejectHint }}</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取 消</el-button>
        <el-button type="danger" :disabled="!rejectValid" :loading="rejectSubmitting" @click="submitReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量审批结果（部分失败时逐条列出） -->
    <el-dialog v-model="resultVisible" title="审批结果" width="760px" append-to-body>
      <div class="oa-result__summary">
        <span>成功 <b class="oa-result__ok">{{ auditResult.successCount || 0 }}</b> 条</span>
        <span>失败 <b class="oa-result__fail">{{ auditResult.failCount || 0 }}</b> 条</span>
      </div>
      <el-table :data="auditResult.items || []" max-height="360" size="small">
        <el-table-column label="申请编号" width="150" prop="applyNo" />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <span class="oa-pill" :class="itemSuccess(row) ? 'oa-pill--green' : 'oa-pill--red'">
              {{ itemSuccess(row) ? '成功' : '失败' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.message || (itemSuccess(row) ? '已通过，成员已入册' : '处理失败') }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button type="primary" @click="resultVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="OaApply">
import { listApply, auditApply } from '@/api/oa/apply'
import { listDept } from '@/api/oa/dept'
import {
  APPLY_STATUS_OPTIONS,
  GRADE_OPTIONS,
  applyStatusText,
  applyStatusTone,
  gradeText,
  oaDateTime
} from '@/utils/oaMember'
import ApplyDetailDrawer from './ApplyDetailDrawer.vue'

const { proxy } = getCurrentInstance()

const queryRef = ref()

const TABS = [
  { value: 'todo', label: '待我审批' },
  { value: 'done', label: '我已审批' },
  { value: 'all', label: '全部' }
]
const QUICK_REASONS = ['照片不清晰', '信息不完整', '非招新对象', '重复报名', '其他']

const scope = ref('todo')
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  deptId: undefined,
  grade: undefined,
  status: undefined
})
const dateRange = ref([])

const loading = ref(false)
const listError = ref(false)
const applyList = ref([])
const total = ref(0)
const counts = ref({ todo: null, done: null, all: null })

const selectedIds = ref([])
const deptOptions = ref([])
const leaderlessDeptIds = ref([])

const detailVisible = ref(false)
const currentRow = ref({})

const rejectVisible = ref(false)
const rejectRow = ref({})
const rejectForm = ref({ remark: '' })
const rejectSubmitting = ref(false)

const auditSubmitting = ref(false)

const resultVisible = ref(false)
const auditResult = ref({ successCount: 0, failCount: 0, items: [] })

/** 可勾选：仅待审批行，且当前登录人有审批权 */
function isSelectable(row) {
  return String(row.status) === '0' && row.canAudit !== false
}

const selectableRows = computed(() => applyList.value.filter(row => isSelectable(row)))
const selectedRows = computed(() => applyList.value.filter(row => selectedIds.value.includes(row.id)))
const allSelected = computed(
  () => selectableRows.value.length > 0 && selectedIds.value.length === selectableRows.value.length
)
const someSelected = computed(
  () => selectedIds.value.length > 0 && selectedIds.value.length < selectableRows.value.length
)

function isSelected(row) {
  return selectedIds.value.includes(row.id)
}

function toggleRow(row) {
  if (!isSelectable(row)) return
  const index = selectedIds.value.indexOf(row.id)
  if (index >= 0) selectedIds.value.splice(index, 1)
  else selectedIds.value.push(row.id)
}

function toggleAll(checked) {
  selectedIds.value = checked ? selectableRows.value.map(row => row.id) : []
}

/** @param {boolean} silent 翻页 / 切换 Tab 时静默清空 */
function clearSelection(silent) {
  const had = selectedIds.value.length > 0
  selectedIds.value = []
  if (had && !silent) proxy.$modal.msg('已清空选择')
}

/** 批量结果里「代审」标记：后端若在姓名后缀「代审」或给出标记位都能识别 */
function isProxy(row) {
  if (!row) return false
  if (row.proxy === true || row.proxyAudit === true) return true
  if (String(row.proxyFlag) === '1' || String(row.proxyFlag) === 'true') return true
  return /代审/.test(String(row.auditUserName || ''))
}

function auditName(row) {
  return String((row && row.auditUserName) || '').replace(/[（(]?代审[）)]?/g, '').trim() || '—'
}

/** 申请状态文案：已关闭带 ⊘ 前缀；未知状态回退后端 statusLabel */
function statusTextOf(row) {
  if (!row) return '—'
  const text = applyStatusText(row.status)
  if (text && text !== '-') return text
  return row.statusLabel || '—'
}

function countText(value) {
  const count = counts.value[value]
  if (count === null || count === undefined) return '…'
  return count === '' ? '-' : count
}

const showNoLeaderTip = computed(() => {
  if (scope.value !== 'todo') return false
  if (!leaderlessDeptIds.value.length) return false
  return applyList.value.some(
    row => String(row.status) === '0' && leaderlessDeptIds.value.includes(row.deptId)
  )
})

/** 待审批 / 已审批 / 全部 三个角标：各发一次 pageSize=1 查询取 total */
function loadCounts() {
  counts.value = { todo: null, done: null, all: null }
  TABS.forEach(tab => {
    listApply({ pageNum: 1, pageSize: 1, scope: tab.value })
      .then(res => {
        counts.value[tab.value] = res.total || 0
      })
      .catch(() => {
        counts.value[tab.value] = ''
      })
  })
}

/** 部门筛选项：有部门列表权限时取全量，否则从列表行兜底（部长只有本部门） */
function loadDeptOptions() {
  if (!proxy.$auth.hasPermi('oa:dept:list')) return
  listDept()
    .then(res => {
      const list = res.data || []
      deptOptions.value = list.map(item => ({ deptId: item.deptId, deptName: item.deptName }))
      leaderlessDeptIds.value = list.filter(item => !item.leaderUserId).map(item => item.deptId)
    })
    .catch(() => {})
}

function mergeDeptOptions(rows) {
  if (proxy.$auth.hasPermi('oa:dept:list')) return
  const map = new Map(deptOptions.value.map(item => [item.deptId, item]))
  ;(rows || []).forEach(row => {
    if (row.deptId && row.deptName && !map.has(row.deptId)) {
      map.set(row.deptId, { deptId: row.deptId, deptName: row.deptName })
    }
  })
  deptOptions.value = [...map.values()]
}

function buildQuery() {
  const query = {
    pageNum: queryParams.value.pageNum,
    pageSize: queryParams.value.pageSize,
    scope: scope.value
  }
  const keyword = (queryParams.value.keyword || '').trim()
  if (keyword) {
    // 纯数字按学号查，其余按姓名查（契约分别给了 name / studentNo 参数）
    if (/^\d+$/.test(keyword)) query.studentNo = keyword
    else query.name = keyword
  }
  if (queryParams.value.deptId) query.deptId = queryParams.value.deptId
  if (queryParams.value.grade) query.grade = queryParams.value.grade
  if (queryParams.value.status) query.status = queryParams.value.status
  if (dateRange.value && dateRange.value.length === 2) {
    // OaMemberApplyMapper 直接按 datetime 比较，需补齐时分秒，否则结束日当天数据会被漏掉
    query.params = {
      beginTime: dateRange.value[0] + ' 00:00:00',
      endTime: dateRange.value[1] + ' 23:59:59'
    }
  }
  return query
}

function getList() {
  loading.value = true
  listError.value = false
  // 翻页 / 改筛选 / 切 Tab 后清空勾选（selection 只对当前页有效）
  selectedIds.value = []
  listApply(buildQuery())
    .then(res => {
      applyList.value = res.rows || []
      total.value = res.total || 0
      mergeDeptOptions(applyList.value)
    })
    .catch(() => {
      applyList.value = []
      total.value = 0
      listError.value = true
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  const had = selectedIds.value.length > 0
  queryParams.value.pageNum = 1
  getList()
  if (had) proxy.$modal.msg('已清空选择')
}

function resetQuery() {
  if (queryRef.value) queryRef.value.resetFields()
  queryParams.value.keyword = ''
  queryParams.value.deptId = undefined
  queryParams.value.grade = undefined
  queryParams.value.status = undefined
  dateRange.value = []
  queryParams.value.pageNum = 1
  getList()
}

function handleTabChange(value) {
  if (scope.value === value) return
  const had = selectedIds.value.length > 0
  scope.value = value
  queryParams.value.pageNum = 1
  getList()
  if (had) proxy.$modal.msg('已清空选择')
}

function openDetail(row) {
  currentRow.value = row || {}
  detailVisible.value = true
}

const rejectLen = computed(() => (rejectForm.value.remark || '').trim().length)
const rejectValid = computed(() => rejectLen.value >= 2 && rejectLen.value <= 200)
const rejectHint = computed(() => {
  if (!rejectLen.value) return '请填写驳回原因，学生将在报名进度中看到'
  if (rejectLen.value < 2) return '驳回原因至少 2 个字'
  return ''
})

/**
 * 同意：单条 / 批量共用
 * @param {Array} rows 申请行（批量来自勾选，单条来自行按钮 / 详情抽屉）
 */
function doApprove(rows) {
  const list = (rows || []).filter(row => row && row.id)
  if (!list.length) return
  if (auditSubmitting.value) return
  if (list.length > 50) {
    proxy.$modal.msgWarning('单次最多同意 50 条，请分批操作')
    return
  }
  const tip =
    list.length === 1
      ? `将同意「${list[0].name || list[0].applyNo}」的入会申请，通过后自动入册并开通账号，确认？`
      : `将同意 ${list.length} 条申请，通过后自动入册并开通账号，确认？`
  proxy.$modal
    .confirm(tip)
    .then(() => {
      auditSubmitting.value = true
      return auditApply({ ids: list.map(row => row.id), pass: true, remark: '' })
    })
    .then(res => afterAudit((res && res.data) || {}, true))
    .catch(() => {})
    .finally(() => {
      auditSubmitting.value = false
    })
}

function handleApprove(row) {
  doApprove([row])
}

function handleBatchApprove() {
  if (!selectedRows.value.length) return
  doApprove(selectedRows.value)
}

function openReject(row) {
  rejectRow.value = row || {}
  rejectForm.value = { remark: '' }
  rejectVisible.value = true
}

function handleReject(row) {
  openReject(row)
}

function applyQuickReason(reason) {
  rejectForm.value.remark = reason
}

function submitReject() {
  if (!rejectValid.value) {
    proxy.$modal.msgWarning('请填写驳回原因（2–200 字）')
    return
  }
  rejectSubmitting.value = true
  auditApply({ ids: [rejectRow.value.id], pass: false, remark: rejectForm.value.remark.trim() })
    .then(res => {
      rejectVisible.value = false
      afterAudit((res && res.data) || {}, false)
    })
    .catch(() => {})
    .finally(() => {
      rejectSubmitting.value = false
    })
}

/** 批量结果条目是否成功（兼容 success 标记位与 status=1 两种返回） */
function itemSuccess(item) {
  if (!item) return false
  if (item.success === true) return true
  if (item.success === false) return false
  return String(item.status) === '1'
}

function afterAudit(data, pass) {
  detailVisible.value = false
  const items = Array.isArray(data.items) ? data.items : []
  const successCount =
    data.successCount === undefined || data.successCount === null
      ? items.filter(item => itemSuccess(item)).length
      : Number(data.successCount)
  const failCount =
    data.failCount === undefined || data.failCount === null
      ? items.filter(item => !itemSuccess(item)).length
      : Number(data.failCount)

  if (failCount > 0) {
    // 部分失败：成功条目已生效不回滚，逐条列出失败原因
    auditResult.value = { ...data, successCount, failCount }
    resultVisible.value = true
  } else if (!pass) {
    proxy.$modal.msgSuccess('已驳回，学生可在报名进度中查看原因')
  } else if (successCount > 1) {
    proxy.$modal.msgSuccess(`已同意 ${successCount} 条申请`)
  } else {
    proxy.$modal.msgSuccess('已同意 1 条申请，成员已入册并开通账号')
  }

  getList()
  loadCounts()
}

onMounted(() => {
  loadCounts()
  loadDeptOptions()
  getList()
})
</script>

<style scoped lang="scss">
/* ---------- Tab ---------- */
.oa-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 14px;
  border-bottom: 1px solid #ebeef5;
}

.oa-tabs__item {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 16px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: color 0.2s;

  &:hover {
    color: #236de9;
  }

  &.is-active {
    color: #236de9;
    font-weight: 600;
    border-bottom-color: #236de9;
  }
}

.oa-tabs__num {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #f0f2f5;
  color: #909399;
  font-size: 12px;
  font-weight: 400;
  line-height: 18px;
  text-align: center;

  .is-active & {
    background: #edf3fe;
    color: #236de9;
  }
}

/* ---------- 批量操作条 ---------- */
.oa-batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 48px;
  padding: 0 14px;
  margin-bottom: 12px;
  background: #edf3fe;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  font-size: 13px;
  color: #303133;
}

.oa-batch-bar__count b {
  color: #236de9;
  font-weight: 600;
}

.oa-batch-bar__hint {
  font-size: 12px;
  color: #909399;
}

.oa-batch-bar__spacer {
  flex: 1;
}

/* ---------- 表格 ---------- */
.oa-apply-table {
  :deep(.el-table__header th) {
    background: #fafafa;
    color: #606266;
    font-weight: 600;
  }

  :deep(.el-table__row) {
    height: 48px;
  }
}

.oa-apply__check {
  display: inline-flex;
  vertical-align: middle;
}

.oa-proxy {
  margin-left: 4px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  font-size: 11px;
}

.oa-apply__alert {
  margin-bottom: 12px;
}

/* ---------- 空态 ---------- */
.oa-apply-empty {
  padding: 40px 0;
  text-align: center;
}

.oa-apply-empty__title {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.oa-apply-empty__desc {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
}

/* ---------- 错误态 ---------- */
.oa-load-error {
  padding: 48px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;

  .el-icon {
    font-size: 32px;
    color: #e6a23c;
  }

  p {
    margin: 10px 0 16px;
  }
}

/* ---------- 驳回弹窗 ---------- */
.oa-reject__notice {
  padding: 8px 12px;
  margin-bottom: 14px;
  border-radius: 4px;
  background: #f4f8ff;
  border: 1px solid #dbe7fc;
  font-size: 12px;
  line-height: 1.7;
  color: #606266;
}

.oa-reject__info {
  display: flex;
  gap: 28px;
  margin-bottom: 14px;
  font-size: 13px;
  color: #606266;
}

.oa-reject__quick {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.oa-reject__hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #f56c6c;
  line-height: 1.6;
}

/* ---------- 批量结果 ---------- */
.oa-result__summary {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
}

.oa-result__ok {
  color: #67c23a;
  font-size: 15px;
}

.oa-result__fail {
  color: #f56c6c;
  font-size: 15px;
}
</style>
