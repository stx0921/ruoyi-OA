<template>
  <div class="app-container">
    <div class="oa-page-title">
      操作日志 <small>字段级变更留痕 · 只读</small>
    </div>

    <!-- 无权限态 -->
    <div v-if="!canView" class="oa-card">
      <div class="oa-load-error">
        <el-icon><Lock /></el-icon>
        <p>没有访问操作日志的权限，请联系管理员</p>
      </div>
    </div>

    <template v-else>
      <div class="oa-card">
        <!-- 部长本部门提示条 -->
        <el-alert
          v-if="isMinister"
          class="oa-log__alert"
          type="info"
          show-icon
          :closable="false"
          title="仅显示本部门日志"
        />

        <!-- 筛选：时间范围（默认近 7 天 / 最大 90 天）、操作人、动作、对象关键字 -->
        <el-form :model="queryParams" :inline="true" class="oa-filter-bar" @submit.prevent>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 340px"
              :clearable="false"
              unlink-panels
              :shortcuts="dateShortcuts"
            />
          </el-form-item>
          <el-form-item label="操作人">
            <el-input
              v-model="queryParams.operatorName"
              placeholder="姓名 / 账号"
              clearable
              style="width: 160px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="动作">
            <el-select v-model="queryParams.action" placeholder="全部" clearable style="width: 160px">
              <el-option
                v-for="item in ACTION_FILTER_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="对象">
            <el-input
              v-model="queryParams.keyword"
              placeholder="成员姓名 / 学号"
              clearable
              style="width: 200px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="oa-log__toolbar">
          <span class="oa-muted">共 {{ total }} 条日志 · 日志只读，不提供编辑 / 删除入口</span>
        </div>

        <!-- 错误态 -->
        <div v-if="listError" class="oa-load-error">
          <el-icon><WarningFilled /></el-icon>
          <p>日志加载失败</p>
          <el-button type="primary" plain @click="getList">重新加载</el-button>
        </div>

        <!-- 列表 -->
        <el-table v-else v-loading="loading" :data="logList" class="oa-log-table">
          <el-table-column label="时间" width="160" align="center">
            <template #default="{ row }">{{ oaDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作人" width="120" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ row.operatorName || '系统' }}</template>
          </el-table-column>
          <el-table-column label="动作" width="120" align="center">
            <template #default="{ row }">
              <span class="oa-pill" :class="'oa-pill--' + labelTone(row.action)">{{ labelText(row.action) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="对象" width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <template v-if="row.memberName">
                <span>{{ row.memberName }}</span>
                <span v-if="row.studentNo" class="oa-muted">（{{ row.studentNo }}）</span>
              </template>
              <span v-else-if="row.applyNo">{{ row.applyNo }}</span>
              <span v-else class="oa-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="变更摘要" min-width="320">
            <template #default="{ row }">
              <el-tooltip :content="summaryOf(row)" placement="top" :show-after="300">
                <div class="oa-log__summary">{{ summaryOf(row) }}</div>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="IP" width="130" align="center">
            <template #default="{ row }">
              <span class="oa-muted">{{ row.ip || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDiff(row)">对比</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <div class="oa-log-empty">
              <p class="oa-log-empty__title">暂无操作日志</p>
              <p class="oa-log-empty__desc">成员的新增、编辑、状态变更都会记录在这里</p>
            </div>
          </template>
        </el-table>

        <pagination
          v-show="!listError && total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          :page-sizes="[20, 50, 100]"
          @pagination="getList"
        />
      </div>
    </template>

    <!-- 变更前后对比 -->
    <member-log-diff-dialog v-model="diffVisible" :log="currentLog" />
  </div>
</template>

<script setup name="OaLog">
import { listMemberLog } from '@/api/oa/log'
import {
  ACTION_OPTIONS,
  actionLabel,
  actionTone,
  memberLogSummary,
  oaDateTime
} from '@/utils/oaMember'
import MemberLogDiffDialog from './MemberLogDiffDialog.vue'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const route = useRoute()
const userStore = useUserStore()

const canView = computed(() => proxy.$auth.hasPermi('oa:log:list'))
const isMinister = computed(() => (userStore.roles || []).indexOf('oa_minister') > -1)

/**
 * 动作筛选项：后端 oa_member_log.action 实际写入值为
 * 新增 / 编辑 / 状态变更 / 恢复 / 调部门 / 升职 / 导入（导出、账号操作等为设计稿预留项）。
 * 这里在 oaMember 的 ACTION_OPTIONS 基础上收敛为实际可命中的值，并补「恢复」。
 */
const ACTION_FILTER_OPTIONS = ACTION_OPTIONS.filter(
  item => ['新增', '编辑', '状态变更', '调部门', '升职', '审批', '导入', '导出', '账号操作'].indexOf(item.value) > -1
).concat([{ value: '恢复', label: '恢复在籍' }])

const loading = ref(false)
const listError = ref(false)
const logList = ref([])
const total = ref(0)

const diffVisible = ref(false)
const currentLog = ref({})

const queryParams = ref({
  pageNum: 1,
  pageSize: 20,
  memberId: undefined,
  operatorName: undefined,
  action: undefined,
  keyword: undefined
})

const dateRange = ref(defaultRange())

const dateShortcuts = [
  { text: '近 7 天', value: () => rangeOf(7) },
  { text: '近 30 天', value: () => rangeOf(30) },
  { text: '近 90 天', value: () => rangeOf(90) }
]

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/** 近 N 天（含今天） */
function rangeOf(days) {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - (days - 1))
  return [formatDate(start), formatDate(end)]
}

function defaultRange() {
  return rangeOf(7)
}

/** 统一成 yyyy-MM-dd（快捷选项可能给出 Date 对象） */
function normalizeDate(value) {
  if (!value) return ''
  if (typeof value === 'string') return value.slice(0, 10)
  const date = new Date(value)
  return isNaN(date.getTime()) ? '' : formatDate(date)
}

/** 起止之间的天数（含首尾） */
function daySpan(start, end) {
  const startDate = new Date(String(start).replace(/-/g, '/'))
  const endDate = new Date(String(end).replace(/-/g, '/'))
  if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) return 0
  return Math.round((endDate.getTime() - startDate.getTime()) / 86400000) + 1
}

function summaryOf(row) {
  return memberLogSummary(row)
}

/** 后端实际写入的「恢复」不在 ACTION_MAP 里，这里补文案与配色 */
function labelText(action) {
  if (action === '恢复') return '恢复在籍'
  return actionLabel(action)
}

function labelTone(action) {
  if (action === '恢复') return 'green'
  return actionTone(action)
}

function buildQuery() {
  const query = {
    pageNum: queryParams.value.pageNum,
    pageSize: queryParams.value.pageSize
  }
  if (queryParams.value.memberId) query.memberId = queryParams.value.memberId
  if (queryParams.value.operatorName) query.operatorName = String(queryParams.value.operatorName).trim()
  if (queryParams.value.action) query.action = queryParams.value.action
  if (queryParams.value.keyword) query.keyword = String(queryParams.value.keyword).trim()
  if (Array.isArray(dateRange.value) && dateRange.value.length === 2) {
    query.params = {
      beginTime: normalizeDate(dateRange.value[0]),
      endTime: normalizeDate(dateRange.value[1])
    }
  }
  return query
}

function getList() {
  if (!canView.value) return
  loading.value = true
  listError.value = false
  listMemberLog(buildQuery())
    .then(res => {
      logList.value = res.rows || []
      total.value = res.total || 0
    })
    .catch(() => {
      logList.value = []
      total.value = 0
      listError.value = true
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  // 关键字长度
  if (String(queryParams.value.keyword || '').trim().length > 50) {
    proxy.$modal.msgWarning('关键字最多 50 个字符')
    return
  }
  // 起止颠倒：提示并自动纠正
  if (Array.isArray(dateRange.value) && dateRange.value.length === 2) {
    const start = normalizeDate(dateRange.value[0])
    const end = normalizeDate(dateRange.value[1])
    dateRange.value = [start, end]
    if (start && end && start > end) {
      dateRange.value = [end, start]
      proxy.$modal.msgWarning('开始时间不能晚于结束时间')
    }
  }
  // 最大跨度 90 天
  if (Array.isArray(dateRange.value) && dateRange.value.length === 2) {
    if (daySpan(dateRange.value[0], dateRange.value[1]) > 90) {
      proxy.$modal.msgWarning('最多查询 90 天，请缩小时间范围')
      return
    }
  }
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.value.memberId = undefined
  queryParams.value.operatorName = undefined
  queryParams.value.action = undefined
  queryParams.value.keyword = undefined
  queryParams.value.pageNum = 1
  dateRange.value = defaultRange()
  getList()
}

function openDiff(row) {
  currentLog.value = row || {}
  diffVisible.value = true
}

/** PC-02「查看全部日志」按对象预填（memberId / keyword 均可） */
function applyRouteQuery() {
  const query = route.query || {}
  const memberId = query.memberId || query.objectId
  if (memberId) queryParams.value.memberId = Number(memberId)
  if (query.keyword) queryParams.value.keyword = String(query.keyword)
  if (query.studentNo) queryParams.value.keyword = String(query.studentNo)
}

onMounted(() => {
  if (!canView.value) return
  applyRouteQuery()
  getList()
})
</script>

<style scoped lang="scss">
.oa-log__alert {
  margin-bottom: 12px;
}

.oa-log__toolbar {
  margin-bottom: 8px;
}

.oa-log-table {
  :deep(.el-table__header th) {
    background: #fafafa;
    color: #606266;
    font-weight: 600;
  }

  :deep(.el-table__row) {
    height: 48px;
  }
}

.oa-log__summary {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
  word-break: break-all;
}

.oa-log-empty {
  padding: 40px 0;
  text-align: center;
}

.oa-log-empty__title {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.oa-log-empty__desc {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
}

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
</style>
