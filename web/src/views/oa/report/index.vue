<template>
  <div class="app-container report-page">
    <div class="oa-page-title">
      <span>统计看板</span>
      <small>数据截至 {{ todayText }}</small>
    </div>

    <div v-if="leaveTotal > PAGE_SIZE" class="oa-muted report-hint">
      图表按最近 {{ PAGE_SIZE }} 条请假单统计
    </div>

    <!-- 筛选 -->
    <div class="oa-card report-filter">
      <el-form :inline="true" :model="filters" class="report-filter__form" @submit.prevent="handleQuery">
        <el-form-item label="统计周期">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="~"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :clearable="false"
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="filters.deptId"
            :data="deptOptions"
            :props="{ value: 'id', label: 'label', children: 'children' }"
            value-key="id"
            placeholder="全部部门"
            clearable
            check-strictly
            style="width: 190px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" :loading="loading" @click="handleQuery">查询</el-button>
          <el-button icon="Download" @click="handleExport">导出报表</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-loading="loading" class="report-body">
      <!-- 统计卡 -->
      <div class="report-stats">
        <OaStatCard label="流程总量" :value="totalCount" tone="blue" icon="Document" />
        <OaStatCard label="审批通过率" :value="passRate" suffix="%" tone="green" icon="CircleCheck" />
        <OaStatCard label="平均审批时长" :value="avgDoneText" tone="orange" icon="Timer" />
        <OaStatCard label="超时未处理" :value="overdueCount" tone="red" icon="Warning" />
      </div>

      <div class="oa-grid-2 report-grid">
        <!-- 审批趋势 -->
        <div class="oa-card">
          <div class="oa-card-head">
            <h3>审批趋势</h3>
            <span class="oa-muted">按周 · 单位：单</span>
          </div>
          <OaChart v-if="filteredLeaves.length" :option="trendOption" height="280px" />
          <div v-else class="oa-empty">所选周期内暂无请假单</div>
        </div>

        <!-- 请假类型分布 -->
        <div class="oa-card">
          <div class="oa-card-head">
            <h3>请假类型分布</h3>
            <span class="oa-muted">共 {{ filteredLeaves.length }} 单</span>
          </div>
          <OaChart v-if="typeStats.length" :option="typeOption" height="280px" />
          <div v-else class="oa-empty">暂无数据</div>
        </div>
      </div>

      <div class="oa-grid-2 report-grid">
        <!-- 各类型平均时长 -->
        <div class="oa-card">
          <div class="oa-card-head">
            <h3>各类型平均时长</h3>
            <span class="oa-muted">按请假天数</span>
          </div>
          <div v-if="durationStats.length" class="report-bars">
            <div v-for="item in durationStats" :key="item.name" class="report-bar">
              <div class="report-bar__head">
                <span>{{ item.name }}</span>
                <span class="oa-muted">{{ item.count }} 单 · {{ oaDurationText(item.avgDays) }}</span>
              </div>
              <el-progress :percentage="item.percent" :show-text="false" :stroke-width="10" :color="item.color" />
            </div>
          </div>
          <div v-else class="oa-empty">暂无数据</div>
        </div>

        <!-- 审批人效率 TOP5 -->
        <div class="oa-card">
          <div class="oa-card-head">
            <h3>审批人效率 TOP5</h3>
            <span class="oa-muted">按已办数排序</span>
          </div>
          <el-table :data="approverStats" size="small" empty-text="所选周期内暂无已办记录">
            <el-table-column label="审批人" min-width="90">
              <template #default="scope">
                <span class="oa-strong">{{ scope.row.name }}</span>
              </template>
            </el-table-column>
            <el-table-column label="部门" prop="dept" min-width="100" show-overflow-tooltip />
            <el-table-column label="已办" prop="count" width="70" align="center" />
            <el-table-column label="平均耗时" width="112" align="center">
              <template #default="scope">
                <span class="oa-pill" :class="'oa-pill--' + scope.row.tone">{{ scope.row.avgText }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="Report">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import { listLeave } from '@/api/oa/leave'
import { listDoneTask, listTodoTask } from '@/api/oa/leaveTask'
import { deptTreeSelect, listUser } from '@/api/system/user'
import OaStatCard from '@/components/oa/OaStatCard.vue'
import OaChart from '@/components/oa/OaChart.vue'
import { useDict } from '@/utils/dict'
import {
  leaveTypeText,
  oaAvgMinutes,
  oaDate,
  oaDurationText,
  oaGroupCount,
  oaHumanSpan,
  oaPercent,
  oaSpanMinutes,
  oaTimestamp,
  oaUserMap,
  oaUserName
} from '@/utils/oa'

const PAGE_SIZE = 500
const PIE_COLORS = ['#236de9', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#9b7bfa']

const { oa_leave_type } = useDict('oa_leave_type')

const deptOptions = ref([])
const leaveRows = ref([])
const leaveTotal = ref(0)
const doneRows = ref([])
const todoRows = ref([])
const users = ref({})
const loading = ref(false)

const filters = reactive({
  dateRange: defaultRange(),
  deptId: undefined
})

/** 已生效的统计周期：点击「查询」后才把筛选周期应用给统计逻辑 */
const appliedRange = ref(defaultRange())

const dictTypes = computed(() => oa_leave_type.value || [])

function pad(num) {
  return String(num).padStart(2, '0')
}

function fmtDate(date) {
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate())
}

function defaultRange() {
  const now = new Date()
  return [fmtDate(new Date(now.getFullYear(), now.getMonth(), 1)), fmtDate(now)]
}

function normalizeRange(range) {
  if (Array.isArray(range) && range.length === 2 && range[0] && range[1]) return range
  return defaultRange()
}

/** 'YYYY-MM-DD' → Date（本地时区，避免浏览器把短日期按 UTC 解析） */
function parseDay(text, endOfDay) {
  if (!text) return null
  const parts = String(text).split('-').map(Number)
  if (parts.length !== 3 || parts.some(num => isNaN(num))) return null
  return endOfDay
    ? new Date(parts[0], parts[1] - 1, parts[2], 23, 59, 59, 999)
    : new Date(parts[0], parts[1] - 1, parts[2], 0, 0, 0, 0)
}

const todayText = computed(() => oaDate(new Date()))
const periodRange = computed(() => normalizeRange(appliedRange.value))
const periodStart = computed(() => parseDay(periodRange.value[0]))
const periodEnd = computed(() => parseDay(periodRange.value[1], true))

/** 部门树节点查找 */
function findDeptNode(nodes, id) {
  for (const node of nodes || []) {
    if (String(node.id) === String(id)) return node
    const hit = findDeptNode(node.children, id)
    if (hit) return hit
  }
  return null
}

/** 选中部门及其全部下级部门的 id */
function collectDeptIds(node) {
  const ids = new Set()
  const walk = current => {
    ids.add(String(current.id))
    ;(current.children || []).forEach(walk)
  }
  walk(node)
  return ids
}

const selectedDeptIds = computed(() => {
  const id = filters.deptId
  if (id === undefined || id === null || id === '') return null
  const node = findDeptNode(deptOptions.value, id)
  return node ? collectDeptIds(node) : null
})

const filteredLeaves = computed(() => {
  const ids = selectedDeptIds.value
  if (!ids) return leaveRows.value
  return leaveRows.value.filter(row => ids.has(String(row.applyDeptId)))
})

/** 有部门筛选时用前端过滤后数量，否则用后端 total */
const totalCount = computed(() => (selectedDeptIds.value ? filteredLeaves.value.length : leaveTotal.value))

const passRate = computed(() => {
  const rows = filteredLeaves.value
  const passed = rows.filter(row => String(row.status) === '2').length
  return oaPercent(passed, rows.length)
})

/** 周期内的已办任务（按 endTime 落在统计周期内） */
const doneInPeriod = computed(() => {
  const start = periodStart.value
  const end = periodEnd.value
  if (!start || !end) return doneRows.value
  return doneRows.value.filter(row => {
    const time = oaTimestamp(row.endTime)
    return time >= start.getTime() && time <= end.getTime()
  })
})

const avgDoneText = computed(() => oaHumanSpan(oaAvgMinutes(doneInPeriod.value)))

/** 超时未处理：待办已等待超过 24 小时 */
const overdueCount = computed(() => {
  const now = new Date()
  return todoRows.value.filter(row => {
    const minutes = oaSpanMinutes(row.createTime, now)
    return minutes !== null && minutes > 24 * 60
  }).length
})

/** 周期内按自然周分组（首周从周期开始日算起） */
const trendWeeks = computed(() => {
  const start = periodStart.value
  const end = periodEnd.value
  if (!start || !end || start.getTime() > end.getTime()) return []
  const weeks = []
  let cursor = new Date(start)
  let index = 1
  while (cursor.getTime() <= end.getTime()) {
    const weekStart = new Date(cursor)
    const weekEnd = new Date(cursor)
    weekEnd.setDate(weekEnd.getDate() + 6)
    weekEnd.setHours(23, 59, 59, 999)
    if (weekEnd.getTime() > end.getTime()) weekEnd.setTime(end.getTime())
    weeks.push({
      label: '第' + index + '周',
      range: fmtDate(weekStart) + ' ~ ' + fmtDate(weekEnd),
      start: weekStart.getTime(),
      end: weekEnd.getTime(),
      count: 0
    })
    cursor = new Date(cursor)
    cursor.setDate(cursor.getDate() + 7)
    index++
  }
  filteredLeaves.value.forEach(row => {
    const time = oaTimestamp(row.applyTime || row.createTime)
    if (!time) return
    const hit = weeks.find(week => time >= week.start && time <= week.end)
    if (hit) hit.count++
  })
  return weeks
})

const trendOption = computed(() => {
  const weeks = trendWeeks.value
  return {
    grid: { left: 4, right: 12, top: 20, bottom: 4, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const item = weeks[params[0].dataIndex]
        if (!item) return ''
        return item.label + '<br/>' + item.range + '<br/>提交量：' + item.count + ' 单'
      }
    },
    xAxis: {
      type: 'category',
      data: weeks.map(week => week.label),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f0f2f5' } },
      axisLabel: { color: '#909399', fontSize: 12 }
    },
    series: [
      {
        name: '提交量',
        type: 'bar',
        data: weeks.map(week => week.count),
        barWidth: '46%',
        itemStyle: { color: '#236de9', borderRadius: [4, 4, 0, 0] }
      }
    ]
  }
})

const typeStats = computed(() =>
  oaGroupCount(filteredLeaves.value, row => leaveTypeText(row.leaveType, dictTypes.value))
)

const typeOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c} 单（{d}%）' },
  legend: {
    bottom: 0,
    left: 'center',
    icon: 'circle',
    itemWidth: 8,
    itemHeight: 8,
    textStyle: { color: '#606266', fontSize: 12 }
  },
  color: PIE_COLORS,
  title: {
    text: String(filteredLeaves.value.length),
    subtext: '请假单',
    left: 'center',
    top: '38%',
    itemGap: 4,
    textStyle: { fontSize: 22, fontWeight: 600, color: '#303133', lineHeight: 26 },
    subtextStyle: { fontSize: 12, color: '#909399' }
  },
  series: [
    {
      type: 'pie',
      radius: ['50%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      emphasis: { scale: false, label: { show: true, fontSize: 13, fontWeight: 600, formatter: '{b}\n{d}%' } },
      data: typeStats.value.map(item => ({ name: item.name, value: item.value }))
    }
  ]
}))

/** 各请假类型的平均天数（进度条按最大值换算比例） */
const durationStats = computed(() => {
  const map = new Map()
  filteredLeaves.value.forEach(row => {
    const name = leaveTypeText(row.leaveType, dictTypes.value)
    if (row.duration === null || row.duration === undefined || row.duration === '') return
    const days = Number(row.duration)
    if (isNaN(days)) return
    const item = map.get(name) || { name, sum: 0, count: 0 }
    item.sum += days
    item.count += 1
    map.set(name, item)
  })
  const list = [...map.values()].map(item => ({ name: item.name, count: item.count, avgDays: item.sum / item.count }))
  const max = list.reduce((value, item) => Math.max(value, item.avgDays), 0)
  return list
    .sort((a, b) => b.avgDays - a.avgDays)
    .map((item, index) => ({
      ...item,
      percent: max ? Math.max(1, Math.round((item.avgDays / max) * 100)) : 0,
      color: PIE_COLORS[index % PIE_COLORS.length]
    }))
})

/** 平均耗时色调：4 小时内绿色，1 天内蓝色，3 天内橙色，其余红色 */
function spanTone(minutes) {
  if (minutes === null || minutes === undefined) return 'gray'
  if (minutes < 60 * 4) return 'green'
  if (minutes < 60 * 24) return 'blue'
  if (minutes < 60 * 24 * 3) return 'orange'
  return 'red'
}

/** 审批人效率：周期内已办按 assignee 分组，取已办数 TOP5 */
const approverStats = computed(() => {
  const groups = new Map()
  doneInPeriod.value.forEach(row => {
    if (row.assignee === undefined || row.assignee === null || row.assignee === '') return
    const key = String(row.assignee)
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(row)
  })
  return [...groups.entries()]
    .map(([assignee, rows]) => {
      const avgMinutes = oaAvgMinutes(rows)
      return {
        name: oaUserName(users.value, assignee),
        dept: (users.value[assignee] || {}).deptName || '—',
        count: rows.length,
        avgMinutes,
        avgText: avgMinutes === null ? '—' : oaHumanSpan(avgMinutes),
        tone: spanTone(avgMinutes)
      }
    })
    .sort((a, b) => {
      if (b.count !== a.count) return b.count - a.count
      return (a.avgMinutes === null ? Number.MAX_SAFE_INTEGER : a.avgMinutes) -
        (b.avgMinutes === null ? Number.MAX_SAFE_INTEGER : b.avgMinutes)
    })
    .slice(0, 5)
})

const deptLabel = computed(() => {
  if (filters.deptId === undefined || filters.deptId === null || filters.deptId === '') return '全部部门'
  const node = findDeptNode(deptOptions.value, filters.deptId)
  return node ? node.label : String(filters.deptId)
})

async function loadDeptOptions() {
  try {
    const res = await deptTreeSelect()
    deptOptions.value = res.data || []
  } catch (e) {
    console.warn('[统计看板] 部门树加载失败', e)
    deptOptions.value = []
  }
}

async function loadLeaves() {
  const [beginTime, endTime] = periodRange.value
  try {
    const res = await listLeave({ pageNum: 1, pageSize: PAGE_SIZE, params: { beginTime, endTime } })
    leaveRows.value = res.rows || []
    leaveTotal.value = Number(res.total) || 0
  } catch (e) {
    console.warn('[统计看板] 请假单加载失败', e)
    leaveRows.value = []
    leaveTotal.value = 0
  }
}

async function loadDone() {
  try {
    const res = await listDoneTask({ pageNum: 1, pageSize: PAGE_SIZE })
    doneRows.value = res.rows || []
  } catch (e) {
    console.warn('[统计看板] 已办任务加载失败', e)
    doneRows.value = []
  }
  await loadUsers()
}

async function loadUsers() {
  if (Object.keys(users.value).length) return
  if (!doneRows.value.length) return
  try {
    const res = await listUser({ pageNum: 1, pageSize: PAGE_SIZE })
    users.value = oaUserMap(res.rows || [])
  } catch (e) {
    console.warn('[统计看板] 用户信息加载失败', e)
    users.value = {}
  }
}

async function loadTodo() {
  try {
    const res = await listTodoTask({ pageNum: 1, pageSize: PAGE_SIZE })
    todoRows.value = res.rows || []
  } catch (e) {
    console.warn('[统计看板] 待办加载失败', e)
    todoRows.value = []
  }
}

async function handleQuery() {
  appliedRange.value = normalizeRange(filters.dateRange)
  loading.value = true
  try {
    await Promise.all([loadLeaves(), loadDone(), loadTodo()])
  } finally {
    loading.value = false
  }
}

/** CSV 单元格：统一加引号，内部引号双写 */
function csvCell(value) {
  const text = value === null || value === undefined ? '' : String(value)
  return '"' + text.replace(/"/g, '""') + '"'
}

/** 前端生成 CSV 汇总（不依赖后端导出接口） */
function handleExport() {
  const [beginTime, endTime] = periodRange.value
  const lines = []
  const push = (...cells) => lines.push(cells.map(csvCell).join(','))

  push('统计看板')
  push('统计周期', beginTime + ' ~ ' + endTime)
  push('部门', deptLabel.value)
  push('数据截至', todayText.value)
  push('')
  push('指标', '数值')
  push('流程总量', totalCount.value)
  push('审批通过率', passRate.value + '%')
  push('平均审批时长', avgDoneText.value)
  push('超时未处理', overdueCount.value)
  push('')
  push('请假类型分布')
  push('类型', '数量', '占比')
  typeStats.value.forEach(item => {
    push(item.name, item.value, oaPercent(item.value, filteredLeaves.value.length) + '%')
  })
  push('')
  push('各类型平均时长')
  push('类型', '数量', '平均天数')
  durationStats.value.forEach(item => push(item.name, item.count, item.avgDays.toFixed(1)))
  push('')
  push('审批人效率 TOP5')
  push('审批人', '部门', '已办数', '平均耗时')
  approverStats.value.forEach(item => push(item.name, item.dept, item.count, item.avgText))

  const csv = '\ufeff' + lines.join('\r\n')
  saveAs(new Blob([csv], { type: 'text/csv;charset=utf-8;' }), '统计看板_' + beginTime + '_' + endTime + '.csv')
  ElMessage.success('报表已导出')
}

onMounted(() => {
  loadDeptOptions()
  handleQuery()
})
</script>

<style lang="scss" scoped>
.report-page {
  .report-hint {
    margin: -6px 0 12px;
  }

  .report-filter {
    padding: 16px 18px 2px;

    &__form {
      :deep(.el-form-item) {
        margin-bottom: 14px;
      }
    }
  }

  .report-body {
    min-height: 240px;
  }

  .report-stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
    margin-top: 16px;
    margin-bottom: 16px;
  }

  .report-grid {
    margin-top: 16px;

    > .oa-card {
      margin-top: 0;
    }
  }

  .report-bars {
    display: flex;
    flex-direction: column;
    gap: 18px;
    padding: 6px 0;
  }

  .report-bar {
    &__head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 8px;
      font-size: 13px;
      color: #606266;
    }
  }

  @media (max-width: 1200px) {
    .report-stats {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 640px) {
    .report-stats {
      grid-template-columns: minmax(0, 1fr);
    }
  }
}
</style>
