<template>
  <div class="app-container volunteer-stat-page">
    <div class="oa-page-title">
      活动统计
      <small>统计口径：总时长 = Σ(活动志愿时长 × 审核通过人数)，实时取活动当前时长；开展次数 = 已开始且未取消的活动数；参与人次 = 审核通过总人次；软删除活动不计入统计。</small>
    </div>

    <!-- 4 统计卡 -->
    <div class="vt-stat-grid">
      <oa-stat-card label="总志愿时长（人时）" :value="overviewText('totalHours')" tone="blue" icon="Timer" />
      <oa-stat-card label="开展活动次数" :value="overviewText('activityCount')" tone="green" icon="CircleCheck" />
      <oa-stat-card label="参与人次" :value="overviewText('participantCount')" tone="orange" icon="User" />
      <oa-stat-card label="平均每场时长" :value="overviewText('avgHours')" suffix="h" tone="red" icon="TrendCharts" />
    </div>

    <div class="oa-card">
      <!-- 时间范围筛选：默认近 12 个月 -->
      <el-form :model="filterForm" :inline="true" class="oa-filter-bar" @submit.prevent>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 300px"
            :clearable="false"
            unlink-panels
            :shortcuts="dateShortcuts"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" :loading="loading" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
        <el-form-item>
          <span class="oa-muted vt-filter-hint">默认统计近 12 个月，按活动开始时间归月</span>
        </el-form-item>
      </el-form>

      <!-- 错误态 -->
      <div v-if="loadError" class="vt-state">
        <el-icon :size="34"><WarningFilled /></el-icon>
        <p>统计数据加载失败</p>
        <el-button size="small" type="primary" @click="loadStats">重新加载</el-button>
      </div>

      <template v-else>
        <!-- 图表：柱=活动次数（左轴），折线=总时长（右轴） -->
        <div class="vt-chart-head">
          <h3>按月志愿活动统计</h3>
          <span class="oa-muted">柱：活动次数（左轴） · 折线：总时长（右轴，单位人时）</span>
        </div>
        <div ref="chartRef" v-loading="loading" class="vt-chart"></div>

        <!-- 按月数据明细 -->
        <div class="vt-table-head">
          <h3>数据明细</h3>
          <div class="vt-table-head__right">
            <span class="oa-muted">共 {{ monthly.length }} 个月</span>
            <el-button
              type="primary"
              plain
              size="small"
              icon="Download"
              :loading="exporting"
              @click="handleExport"
              v-hasPermi="['oa:volunteer:export']"
            >导出统计</el-button>
          </div>
        </div>

        <el-table
          v-loading="loading"
          :data="monthly"
          :show-summary="monthly.length > 0"
          :summary-method="summaryMethod"
          class="vt-table"
          empty-text="所选时间范围内暂无统计数据"
        >
          <el-table-column label="月份" width="160">
            <template #default="{ row }">{{ formatMonth(row.month) }}</template>
          </el-table-column>
          <el-table-column label="活动数" prop="activityCount" width="140" align="center">
            <template #default="{ row }">{{ numberText(row.activityCount) }} 场</template>
          </el-table-column>
          <el-table-column label="通过人次" prop="passCount" width="140" align="center">
            <template #default="{ row }">{{ numberText(row.passCount) }} 人次</template>
          </el-table-column>
          <el-table-column label="总时长（人时）" prop="totalHours" min-width="160" align="center">
            <template #default="{ row }">{{ formatHours(row.totalHours) }} 人时</template>
          </el-table-column>
        </el-table>
      </template>
    </div>
  </div>
</template>

<script setup name="VolunteerStat">
import * as echarts from 'echarts'
import { saveAs } from 'file-saver'
import OaStatCard from '@/components/oa/OaStatCard.vue'
import { exportStat, getStatMonthly, getStatOverview } from '@/api/oa/volunteer'
import { blobValidate, parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()

const CHART_COLORS = {
  primary: '#236de9',
  primaryLight: '#5b8ff0',
  warn: '#e6a23c',
  inkSoft: '#7e888c',
  gridLine: 'rgba(46, 58, 62, 0.07)',
  axisLine: 'rgba(46, 58, 62, 0.13)',
  empty: '#99a2a5'
}

const filterForm = ref({})
const dateRange = ref(defaultRange())

const loading = ref(false)
const loadError = ref(false)
const exporting = ref(false)
const overview = ref({})
const monthly = ref([])

const chartRef = ref(null)
let chart = null
let resizeObserver = null

/* -------------------------------------------------------------------------- */
/* 时间范围                                                                   */
/* -------------------------------------------------------------------------- */
function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/** 最近 N 个月（含本月） */
function rangeOfMonths(months) {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth() - (months - 1), 1)
  return [formatDate(start), formatDate(now)]
}

/** 默认近 12 个月 */
function defaultRange() {
  return rangeOfMonths(12)
}

const dateShortcuts = [
  { text: '近 3 个月', value: () => rangeOfMonths(3) },
  { text: '近 6 个月', value: () => rangeOfMonths(6) },
  { text: '近 12 个月', value: () => rangeOfMonths(12) },
  {
    text: '本年',
    value: () => {
      const now = new Date()
      return [formatDate(new Date(now.getFullYear(), 0, 1)), formatDate(now)]
    }
  }
]

/** 快捷选项可能回传 Date，统一成 yyyy-MM-dd */
function normalizeDate(value) {
  if (!value) return ''
  if (typeof value === 'string') return value.slice(0, 10)
  const date = new Date(value)
  return isNaN(date.getTime()) ? '' : formatDate(date)
}

/**
 * 统计接口时间参数
 *
 * 契约：GET /oa/volunteer/stat/{overview,monthly,export} 参数 beginTime / endTime，
 * 按活动 start_time 过滤。经与 be-apply 确认：前端传 yyyy-MM-dd，
 * 服务端自动补日界（begin 00:00:00 含、end 23:59:59 含），不要传次日/时分秒。
 */
function buildQuery() {
  const [start, end] = dateRange.value || []
  return {
    beginTime: normalizeDate(start) || undefined,
    endTime: normalizeDate(end) || undefined
  }
}

/* -------------------------------------------------------------------------- */
/* 统计卡                                                                     */
/* -------------------------------------------------------------------------- */
function formatHours(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return '0'
  return Number.isInteger(num) ? String(num) : num.toFixed(1)
}

function numberText(value) {
  const num = Number(value)
  return Number.isFinite(num) ? String(num) : '0'
}

function overviewText(key) {
  if (loading.value || loadError.value) return '--'
  const data = overview.value || {}
  if (key === 'totalHours') return formatHours(data.totalHours)
  if (key === 'avgHours') {
    const num = Number(data.avgHours)
    return Number.isFinite(num) ? num.toFixed(1) : '0.0'
  }
  return numberText(data[key])
}

/* -------------------------------------------------------------------------- */
/* 图表                                                                       */
/* -------------------------------------------------------------------------- */
function formatMonth(value) {
  if (!value) return '—'
  const text = String(value)
  const parts = text.split('-')
  if (parts.length < 2) return text
  return `${parts[0]} 年 ${Number(parts[1])} 月`
}

function formatAxisMonth(value) {
  const text = String(value || '')
  return text.length >= 7 ? text.slice(5) + ' 月' : text
}

function buildChartOption() {
  const rows = monthly.value || []
  const months = rows.map(row => row.month)
  const counts = rows.map(row => Number(row.activityCount) || 0)
  const hours = rows.map(row => Number(row.totalHours) || 0)

  return {
    color: [CHART_COLORS.primary, CHART_COLORS.warn],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.97)',
      borderColor: 'rgba(46, 58, 62, 0.11)',
      textStyle: { color: '#101418', fontSize: 12 },
      formatter: params => {
        const list = Array.isArray(params) ? params : [params]
        if (!list.length) return ''
        const title = formatMonth(list[0].axisValue)
        const lines = list.map(item => {
          const value =
            item.seriesName === '活动次数'
              ? `${numberText(item.value)} 场`
              : `${formatHours(item.value)} 人时`
          return `${item.marker} ${item.seriesName}：${value}`
        })
        return [title].concat(lines).join('<br/>')
      }
    },
    legend: {
      top: 0,
      right: 0,
      itemWidth: 10,
      itemHeight: 10,
      icon: 'roundRect',
      textStyle: { color: '#596368', fontSize: 12 },
      data: ['活动次数', '总时长（人时）']
    },
    grid: {
      left: 16,
      right: 16,
      top: 42,
      bottom: 6,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: months,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: CHART_COLORS.axisLine } },
      axisLabel: {
        color: CHART_COLORS.inkSoft,
        fontSize: 11,
        interval: 0,
        formatter: formatAxisMonth
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '活动次数',
        nameTextStyle: { color: CHART_COLORS.inkSoft, fontSize: 11, padding: [0, 0, 4, 0] },
        minInterval: 1,
        axisLabel: { color: CHART_COLORS.inkSoft, fontSize: 11 },
        splitLine: { lineStyle: { color: CHART_COLORS.gridLine } }
      },
      {
        type: 'value',
        name: '总时长(h)',
        nameTextStyle: { color: CHART_COLORS.inkSoft, fontSize: 11, padding: [0, 0, 4, 0] },
        axisLabel: { color: CHART_COLORS.inkSoft, fontSize: 11 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '活动次数',
        type: 'bar',
        barMaxWidth: 34,
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: CHART_COLORS.primaryLight },
            { offset: 1, color: CHART_COLORS.primary }
          ])
        },
        data: counts
      },
      {
        name: '总时长（人时）',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 2.5, color: CHART_COLORS.warn },
        itemStyle: { color: CHART_COLORS.warn },
        data: hours
      }
    ],
    graphic: months.length
      ? []
      : [
          {
            type: 'text',
            left: 'center',
            top: 'middle',
            style: {
              text: '所选时间范围内暂无统计数据',
              fill: CHART_COLORS.empty,
              fontSize: 13
            }
          }
        ]
  }
}

function ensureChart() {
  if (!chartRef.value) return null
  if (!chart) chart = echarts.init(chartRef.value)
  return chart
}

function bindResizeObserver() {
  if (typeof ResizeObserver === 'undefined') return
  if (!chartRef.value || resizeObserver) return
  resizeObserver = new ResizeObserver(() => resizeChart())
  resizeObserver.observe(chartRef.value)
}

function destroyChart() {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (chart) {
    chart.dispose()
    chart = null
  }
}

function renderChart() {
  const instance = ensureChart()
  if (!instance) return
  bindResizeObserver()
  instance.setOption(buildChartOption(), true)
  instance.resize()
}

function resizeChart() {
  if (chart) chart.resize()
}

/* -------------------------------------------------------------------------- */
/* 数据加载                                                                   */
/* -------------------------------------------------------------------------- */
async function loadStats() {
  loading.value = true
  loadError.value = false
  const query = buildQuery()
  try {
    const [overviewRes, monthlyRes] = await Promise.all([
      getStatOverview(query),
      getStatMonthly(query)
    ])
    overview.value = (overviewRes && overviewRes.data) || {}
    monthly.value = Array.isArray(monthlyRes && monthlyRes.data) ? monthlyRes.data : []
    await nextTick()
    renderChart()
  } catch (e) {
    overview.value = {}
    monthly.value = []
    loadError.value = true
    // 错误态会移除图表容器，这里先释放实例，重试成功后重建
    destroyChart()
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  // 起止颠倒时自动纠正（正常由日期组件保证，兜底数据源）
  if (Array.isArray(dateRange.value) && dateRange.value.length === 2) {
    const start = normalizeDate(dateRange.value[0])
    const end = normalizeDate(dateRange.value[1])
    dateRange.value = [start, end]
    if (start && end && start > end) {
      dateRange.value = [end, start]
      proxy.$modal.msgWarning('开始时间不能晚于结束时间')
    }
  }
  loadStats()
}

function resetQuery() {
  dateRange.value = defaultRange()
  loadStats()
}

/* -------------------------------------------------------------------------- */
/* 导出统计                                                                   */
/* -------------------------------------------------------------------------- */
async function handleExport() {
  if (exporting.value) return
  exporting.value = true
  try {
    const data = await exportStat(buildQuery())
    if (!blobValidate(data)) {
      let message = '导出失败，请稍后重试'
      try {
        const rspObj = JSON.parse(await data.text())
        message = rspObj.msg || message
      } catch (e) {
        // 非 JSON 错误体，保持默认提示
      }
      proxy.$modal.msgError(message)
      return
    }
    const filename = '志愿活动统计_' + (parseTime(new Date(), '{y}{m}{d}{h}{i}{s}') || '') + '.xlsx'
    saveAs(new Blob([data]), filename)
    proxy.$modal.msgSuccess('已导出统计报表')
  } catch (e) {
    proxy.$modal.msgError('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

/* -------------------------------------------------------------------------- */
/* 表格合计                                                                   */
/* -------------------------------------------------------------------------- */
function summaryMethod({ columns, data }) {
  const sumOf = key => (data || []).reduce((sum, row) => sum + (Number(row[key]) || 0), 0)
  return columns.map((column, index) => {
    if (index === 0) return '合计'
    if (column.property === 'activityCount') return `${numberText(sumOf('activityCount'))} 场`
    if (column.property === 'passCount') return `${numberText(sumOf('passCount'))} 人次`
    if (column.property === 'totalHours') return `${formatHours(sumOf('totalHours'))} 人时`
    return ''
  })
}

/* -------------------------------------------------------------------------- */
/* 生命周期                                                                   */
/* -------------------------------------------------------------------------- */
onMounted(async () => {
  await loadStats()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  destroyChart()
})
</script>

<style scoped lang="scss">
.volunteer-stat-page {
  .oa-page-title small {
    margin-left: 10px;
    font-size: 12px;
    font-weight: 400;
    line-height: 1.7;
    color: var(--ink-soft);
  }
}

/* ---------- 统计卡 ---------- */
.vt-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

/* ---------- 筛选 ---------- */
.vt-filter-hint {
  line-height: 32px;
}

/* ---------- 图表 ---------- */
.vt-chart-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin: 6px 0 4px;

  h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 650;
    color: var(--ink);
  }
}

.vt-chart {
  width: 100%;
  height: 320px;
  min-width: 0;
}

/* ---------- 数据明细 ---------- */
.vt-table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin: 18px 0 10px;
  padding-top: 16px;
  border-top: 1px solid var(--line-strong);

  h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 650;
    color: var(--ink);
  }
}

.vt-table-head__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.vt-table {
  :deep(.el-table__row) {
    height: 46px;
  }

  :deep(.el-table__footer-wrapper td) {
    background: var(--groove-bg);
    font-weight: 600;
    color: var(--ink);
  }
}

/* ---------- 错误态 ---------- */
.vt-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 72px 0;
  color: var(--ink-soft);
  font-size: 13px;

  p {
    margin: 0;
  }
}

@media (max-width: 1200px) {
  .vt-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
