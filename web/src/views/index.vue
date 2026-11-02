<template>
  <div class="app-container workbench">
    <!-- 顶部问候 -->
    <div class="oa-page-title wb-head">
      <span>{{ greeting }}，{{ userStore.nickName }}</span>
      <small>今天有 {{ todoTotal }} 条待办需要你处理</small>
      <span class="wb-head__date">{{ todayLabel }}</span>
    </div>

    <!-- 4 个统计卡 -->
    <div class="wb-stats">
      <OaStatCard label="待我审批" :value="todoTotal" tone="blue" icon="Tickets" />
      <OaStatCard label="我发起的" :value="myLeaveTotal" tone="orange" icon="Document" />
      <OaStatCard label="本月已办" :value="monthDoneCount" tone="green" icon="CircleCheck" />
      <OaStatCard label="平均审批时长" :value="avgDoneText" tone="red" icon="Timer" />
    </div>

    <div class="oa-grid-2 wb-grid">
      <!-- 我的待办 -->
      <div v-loading="loading.todo" class="oa-card">
        <div class="oa-card-head">
          <h3>我的待办</h3>
          <span class="oa-link" @click="go('/oa/todo')">查看全部</span>
        </div>
        <template v-if="todoList.length">
          <div v-for="item in todoList" :key="item.taskId" class="oa-list-row">
            <span class="oa-list-row__dot"></span>
            <div class="oa-list-row__main">
              <div class="oa-list-row__title">{{ todoTitle(item) }}</div>
              <div class="oa-list-row__sub">{{ todoSub(item) }}</div>
            </div>
            <span class="oa-pill oa-pill--orange">待审批</span>
            <el-button size="small" @click="go('/oa/todo')">去处理</el-button>
          </div>
        </template>
        <div v-else class="oa-empty">暂无待办</div>
      </div>

      <!-- 快捷入口 + 系统公告 -->
      <div class="oa-card">
        <div class="oa-card-head">
          <h3>快捷入口</h3>
        </div>
        <div class="wb-quick">
          <div
            v-for="entry in quickEntries"
            :key="entry.title"
            class="wb-quick__item"
            @click="go(entry.path, entry.query)"
          >
            <div class="wb-quick__ico" :class="'is-' + entry.tone">{{ entry.text }}</div>
            <span class="wb-quick__txt">{{ entry.title }}</span>
          </div>
        </div>

        <div class="oa-card-head wb-notice-head">
          <h3>系统公告</h3>
        </div>
        <div v-loading="loading.notice" class="wb-notice">
          <template v-if="noticeList.length">
            <div v-for="notice in noticeList" :key="notice.noticeId" class="oa-list-row">
              <span class="oa-list-row__dot" :style="{ background: noticeDot(notice) }"></span>
              <div class="oa-list-row__main">
                <div class="oa-list-row__title">{{ notice.noticeTitle }}</div>
                <div class="oa-list-row__sub">{{ oaDate(notice.createTime) }} · {{ notice.createBy || '系统' }}</div>
              </div>
            </div>
          </template>
          <div v-else class="oa-empty">暂无公告</div>
        </div>
      </div>
    </div>

    <div class="oa-grid-2 wb-grid">
      <!-- 近 7 日审批量 -->
      <div class="oa-card">
        <div class="oa-card-head">
          <h3>近 7 日审批量</h3>
          <span class="oa-muted">单位：单</span>
        </div>
        <OaChart :option="trendOption" height="260px" />
      </div>

      <!-- 请假类型分布 -->
      <div class="oa-card">
        <div class="oa-card-head">
          <h3>请假类型分布</h3>
          <span class="oa-muted">近 200 条请假单</span>
        </div>
        <OaChart v-if="leaveTypeStats.length" :option="typeOption" height="260px" />
        <div v-else class="oa-empty">暂无请假数据</div>
      </div>
    </div>
  </div>
</template>

<script setup name="Workbench">
import { computed, getCurrentInstance, onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'
import { useDict } from '@/utils/dict'
import { listLeave } from '@/api/oa/leave'
import { listDoneTask, listTodoTask } from '@/api/oa/task'
import { listNotice } from '@/api/system/notice'
import OaStatCard from '@/components/oa/OaStatCard.vue'
import OaChart from '@/components/oa/OaChart.vue'
import {
  leaveTypeText,
  oaAvgMinutes,
  oaDate,
  oaDateTime,
  oaGroupCount,
  oaHumanSpan,
  oaTimestamp
} from '@/utils/oa'

const WEEK_LABELS = ['日', '一', '二', '三', '四', '五', '六']
const PIE_COLORS = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#9b7bfa']

const router = useRouter()
const userStore = useUserStore()
const { proxy } = getCurrentInstance()
const { oa_leave_type } = useDict('oa_leave_type')

const todoList = ref([])
const todoTotal = ref(0)
const myLeaveTotal = ref(0)
const doneRows = ref([])
const leaveRows = ref([])
const noticeList = ref([])

const loading = reactive({ todo: false, notice: false })

const dictTypes = computed(() => oa_leave_type.value || [])

const quickEntries = [
  { title: '请假申请', text: '假', tone: 'blue', path: '/oa/leave' },
  { title: '待办中心', text: '审', tone: 'orange', path: '/oa/todo' },
  { title: '流程跟踪', text: '踪', tone: 'green', path: '/oa/trace' },
  { title: '员工档案', text: '档', tone: 'blue', path: '/oa/employee' },
  { title: '统计看板', text: '表', tone: 'green', path: '/oa/report' },
  { title: '我的已办', text: '办', tone: 'red', path: '/oa/todo', query: { tab: 'done' } }
]

function pad(num) {
  return String(num).padStart(2, '0')
}

function fmtDate(date) {
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate())
}

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const todayLabel = computed(() => {
  const now = new Date()
  return fmtDate(now) + ' 周' + WEEK_LABELS[now.getDay()]
})

/** 本月已办：已办任务按 endTime 落在当前自然月 */
const monthDoneCount = computed(() => {
  const now = new Date()
  const prefix = now.getFullYear() + '-' + pad(now.getMonth() + 1)
  return doneRows.value.filter(row => oaDate(row.endTime).indexOf(prefix) === 0).length
})

/** 平均审批时长：同批已办记录的 createTime → endTime */
const avgDoneText = computed(() => oaHumanSpan(oaAvgMinutes(doneRows.value)))

/** 请假类型分布（近 200 条） */
const leaveTypeStats = computed(() =>
  oaGroupCount(leaveRows.value, row => leaveTypeText(row.leaveType, dictTypes.value))
)

/** 最近 7 天（含今天），标签形如「周一\n10-24」 */
const recentDays = computed(() => {
  const days = []
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  for (let i = 6; i >= 0; i--) {
    const day = new Date(today)
    day.setDate(day.getDate() - i)
    days.push({
      label: '周' + WEEK_LABELS[day.getDay()] + '\n' + pad(day.getMonth() + 1) + '-' + pad(day.getDate()),
      start: day.getTime(),
      end: day.getTime() + 24 * 60 * 60 * 1000
    })
  }
  return days
})

/** 近 7 日审批量：按已办任务 endTime 分天统计 */
const trendOption = computed(() => {
  const days = recentDays.value
  const counts = days.map(day => {
    return doneRows.value.filter(row => {
      const time = oaTimestamp(row.endTime)
      return time >= day.start && time < day.end
    }).length
  })
  return {
    grid: { left: 4, right: 12, top: 18, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: days.map(day => day.label),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399', fontSize: 12, lineHeight: 16 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f0f2f5' } },
      axisLabel: { color: '#909399', fontSize: 12 }
    },
    series: [
      {
        name: '审批量',
        type: 'bar',
        data: counts,
        barWidth: '46%',
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] }
      }
    ]
  }
})

/** 请假类型环形图，中心显示近 200 条总数 */
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
    text: String(leaveRows.value.length),
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
      data: leaveTypeStats.value.map(item => ({ name: item.name, value: item.value }))
    }
  ]
}))

function todoTitle(row) {
  const type = leaveTypeText(row.leaveType, dictTypes.value)
  const duration = row.duration === null || row.duration === undefined || row.duration === '' ? '—' : row.duration
  return (row.applyUserName || '—') + '的请假申请（' + type + ' ' + duration + ' 天）'
}

function todoSub(row) {
  return oaDateTime(row.createTime) + ' · 流程：请假审批'
}

function noticeDot(notice) {
  if (String(notice.noticeType) === '1') return '#409eff'
  if (String(notice.noticeType) === '2') return '#67c23a'
  return '#909399'
}

function canViewNotice() {
  try {
    return proxy.$auth.hasPermi('system:notice:list')
  } catch (e) {
    return false
  }
}

async function fetchTodo() {
  loading.todo = true
  try {
    const res = await listTodoTask({ pageNum: 1, pageSize: 5 })
    todoTotal.value = Number(res.total) || 0
    todoList.value = res.rows || []
  } catch (e) {
    console.warn('[工作台] 待办加载失败', e)
    todoTotal.value = 0
    todoList.value = []
  } finally {
    loading.todo = false
  }
}

async function fetchMine() {
  if (!userStore.id) {
    myLeaveTotal.value = 0
    return
  }
  try {
    const res = await listLeave({ applyUserId: userStore.id, pageNum: 1, pageSize: 1 })
    myLeaveTotal.value = Number(res.total) || 0
  } catch (e) {
    console.warn('[工作台] 我发起的加载失败', e)
    myLeaveTotal.value = 0
  }
}

async function fetchDone() {
  try {
    const res = await listDoneTask({ pageNum: 1, pageSize: 200 })
    doneRows.value = res.rows || []
  } catch (e) {
    console.warn('[工作台] 已办加载失败', e)
    doneRows.value = []
  }
}

async function fetchLeaveRows() {
  try {
    const res = await listLeave({ pageNum: 1, pageSize: 200 })
    leaveRows.value = res.rows || []
  } catch (e) {
    console.warn('[工作台] 请假单加载失败', e)
    leaveRows.value = []
  }
}

async function fetchNotice() {
  noticeList.value = []
  // 无公告权限时直接展示空态，避免触发一次注定失败的请求（以及全局错误提示）
  if (!canViewNotice()) return
  loading.notice = true
  try {
    const res = await listNotice({ pageNum: 1, pageSize: 5 })
    noticeList.value = res.rows || []
  } catch (e) {
    console.warn('[工作台] 公告加载失败', e)
    noticeList.value = []
  } finally {
    loading.notice = false
  }
}

function refresh() {
  fetchTodo()
  fetchMine()
  fetchDone()
  fetchLeaveRows()
  fetchNotice()
}

function go(path, query) {
  const target = query ? { path, query } : path
  const result = router.push(target)
  if (result && typeof result.catch === 'function') result.catch(() => {})
}

let activatedOnce = false

onMounted(() => {
  refresh()
})

// 首页默认被 keep-alive 缓存，回到工作台时刷新一次以免数据过期
onActivated(() => {
  if (activatedOnce) refresh()
  activatedOnce = true
})
</script>

<style lang="scss" scoped>
.workbench {
  .wb-head {
    align-items: baseline;

    &__date {
      margin-left: auto;
      font-size: 13px;
      font-weight: 400;
      color: #909399;
    }
  }

  .wb-stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
    margin-bottom: 16px;
  }

  .wb-grid {
    margin-top: 16px;

    > .oa-card {
      margin-top: 0;
    }
  }

  .wb-quick {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;

    &__item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 14px 6px;
      border: 1px solid transparent;
      border-radius: 8px;
      background: #fafbfc;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: #c6e2ff;
        background: #ecf5ff;
        transform: translateY(-1px);
      }
    }

    &__ico {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 38px;
      height: 38px;
      border-radius: 10px;
      font-size: 16px;
      font-weight: 600;

      &.is-blue {
        background: #ecf5ff;
        color: #409eff;
      }

      &.is-green {
        background: #f0f9eb;
        color: #67c23a;
      }

      &.is-orange {
        background: #fdf6ec;
        color: #e6a23c;
      }

      &.is-red {
        background: #fef0f0;
        color: #f56c6c;
      }
    }

    &__txt {
      font-size: 12px;
      color: #606266;
    }
  }

  .wb-notice-head {
    margin-top: 18px;
  }

  .wb-notice {
    min-height: 96px;
  }

  @media (max-width: 1200px) {
    .wb-stats {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 640px) {
    .wb-stats {
      grid-template-columns: minmax(0, 1fr);
    }
  }
}
</style>
