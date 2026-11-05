<template>
  <div class="app-container workbench">
    <!-- 顶部问候 -->
    <div class="oa-page-title wb-head">
      <span>{{ greeting }}，{{ userStore.nickName }}</span>
      <small>{{ headHint }}</small>
      <span class="wb-head__date">{{ todayLabel }}</span>
    </div>

    <!-- 4 个统计卡（学生组织口径） -->
    <div class="wb-stats">
      <OaStatCard label="在册成员" :value="stats.total" tone="blue" icon="User" />
      <OaStatCard label="待审批入会" :value="stats.pendingApply" tone="orange" icon="Tickets" />
      <OaStatCard label="本月新增" :value="stats.monthJoin" tone="green" icon="CirclePlus" />
      <OaStatCard label="本月退会/毕业" :value="stats.monthLeave" tone="red" icon="Remove" />
    </div>

    <div class="oa-grid-2 wb-grid">
      <!-- 我的任务 -->
      <div v-loading="loading.task" class="oa-card">
        <div class="oa-card-head">
          <h3>我的任务</h3>
          <span class="oa-link" @click="go('/task/my')">查看全部</span>
        </div>
        <template v-if="myTasks.length">
          <div v-for="task in myTasks" :key="task.taskId" class="oa-list-row">
            <span class="oa-list-row__dot" :style="{ background: statusMeta(task.status).color }"></span>
            <div class="oa-list-row__main">
              <div class="oa-list-row__title">{{ task.title }}</div>
              <div class="oa-list-row__sub">{{ taskSub(task) }}</div>
            </div>
            <span class="oa-pill" :style="pillStyle(task.status)">
              {{ task.statusLabel || statusMeta(task.status).label }}
            </span>
          </div>
        </template>
        <div v-else class="oa-empty">{{ canTask ? '暂无待处理任务' : '当前账号无任务模块权限' }}</div>
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
          <div v-if="!quickEntries.length" class="oa-empty wb-quick__empty">当前账号暂无可用入口</div>
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
          <div v-else class="oa-empty">{{ canNotice ? '暂无公告' : '当前账号无公告查看权限' }}</div>
        </div>
      </div>
    </div>

    <div class="oa-grid-2 wb-grid">
      <!-- 任务状态分布 -->
      <div class="oa-card">
        <div class="oa-card-head">
          <h3>任务状态分布</h3>
          <span class="oa-muted">单位：个</span>
        </div>
        <OaChart v-if="taskTotal" :option="taskOption" height="260px" />
        <div v-else class="oa-empty">{{ canTask ? '暂无任务数据' : '当前账号无任务模块权限' }}</div>
      </div>

      <!-- 入会申请状态分布 -->
      <div class="oa-card">
        <div class="oa-card-head">
          <h3>入会申请状态</h3>
          <span class="oa-muted">近 200 条</span>
        </div>
        <OaChart v-if="applyRows.length" :option="applyOption" height="260px" />
        <div v-else class="oa-empty">{{ canApply ? '暂无申请数据' : '当前账号无审批权限' }}</div>
      </div>
    </div>
  </div>
</template>

<script setup name="Workbench">
import { computed, getCurrentInstance, onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'
import { getMemberStats } from '@/api/oa/member'
import { listApply } from '@/api/oa/apply'
import { getTaskBoard, listMyTask } from '@/api/oa/task'
import { listNotice } from '@/api/system/notice'
import OaStatCard from '@/components/oa/OaStatCard.vue'
import OaChart from '@/components/oa/OaChart.vue'
import { oaDate } from '@/utils/oa'
import { TASK_STATUS_MAP } from '@/utils/oaTask'

const WEEK_LABELS = ['日', '一', '二', '三', '四', '五', '六']
const PIE_COLORS = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#9b7bfa']

const router = useRouter()
const userStore = useUserStore()
const { proxy } = getCurrentInstance()

/**
 * 工作台的每个数据块后端都有 @PreAuthorize，缺权限时直接跳过请求，
 * 否则未授权角色打开工作台会被 403 提示刷屏、页面一片空白。
 */
function can(perm) {
  try {
    return proxy.$auth.hasPermi(perm)
  } catch (e) {
    return false
  }
}

const canMember = computed(() => can('oa:member:list'))
const canTask = computed(() => can('oa:task:list'))
const canApply = computed(() => can('oa:apply:list'))
const canNotice = computed(() => can('system:notice:list'))

const stats = reactive({ total: 0, pendingApply: 0, monthJoin: 0, monthLeave: 0 })
const myTasks = ref([])
const boardColumns = ref([])
const canceledCount = ref(0)
const applyRows = ref([])
const noticeList = ref([])

const loading = reactive({ task: false, notice: false })

const headHint = computed(() => {
  if (!canMember.value) return '当前账号暂无组织人事模块权限'
  return '共 ' + stats.total + ' 名在册成员，' + stats.pendingApply + ' 条入会申请待审批'
})

/** 快捷入口按当前账号实际拥有权限的菜单生成，避免点进去 404 / 403 */
const quickEntries = computed(() =>
  [
    { title: '成员管理', text: '员', tone: 'blue', path: '/hr/member', perm: 'oa:member:list' },
    { title: '入会审批', text: '审', tone: 'orange', path: '/hr/apply', perm: 'oa:apply:list' },
    { title: '批量导入', text: '导', tone: 'green', path: '/hr/import', perm: 'oa:member:import' },
    { title: '任务看板', text: '板', tone: 'blue', path: '/task/board', perm: 'oa:task:list' },
    { title: '我的任务', text: '任', tone: 'green', path: '/task/my', perm: 'oa:task:list' },
    { title: '项目管理', text: '项', tone: 'orange', path: '/task/project', perm: 'oa:project:list' },
    { title: '周期任务', text: '周', tone: 'red', path: '/task/recur', perm: 'oa:recur:list' },
    { title: '部门与负责人', text: '部', tone: 'blue', path: '/hr/dept', perm: 'oa:dept:list' },
    { title: '操作日志', text: '志', tone: 'green', path: '/hr/log', perm: 'oa:log:list' }
  ].filter(entry => can(entry.perm))
)

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

function statusMeta(status) {
  return TASK_STATUS_MAP[String(status)] || { label: '—', color: '#909399', bg: '#f4f4f5' }
}

function pillStyle(status) {
  const meta = statusMeta(status)
  return { background: meta.bg, color: meta.color }
}

function taskSub(task) {
  const prefix = task.projectName ? task.projectName + ' · ' : ''
  const overdue = String(task.overdueFlag) === '1' ? ' · 已逾期' : ''
  return prefix + '截止 ' + oaDate(task.dueTime) + overdue
}

function noticeDot(notice) {
  if (String(notice.noticeType) === '1') return '#409eff'
  if (String(notice.noticeType) === '2') return '#67c23a'
  return '#909399'
}

/** 任务总数（4 列合计 + 已取消） */
const taskTotal = computed(() => {
  const sum = boardColumns.value.reduce((acc, column) => acc + (Number(column.count) || 0), 0)
  return sum + (Number(canceledCount.value) || 0)
})

/** 任务状态条形图 */
const taskOption = computed(() => {
  const labels = boardColumns.value.map(column => column.statusLabel)
  const values = boardColumns.value.map(column => Number(column.count) || 0)
  if (Number(canceledCount.value) > 0) {
    labels.push('已取消')
    values.push(Number(canceledCount.value))
  }
  return {
    grid: { left: 4, right: 12, top: 18, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: labels,
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
        name: '任务数',
        type: 'bar',
        data: values,
        barWidth: '46%',
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        label: { show: true, position: 'top', color: '#909399', fontSize: 12 }
      }
    ]
  }
})

/** 入会申请按状态分组 */
const applyStats = computed(() => {
  const map = new Map()
  applyRows.value.forEach(row => {
    const name = row.statusLabel || '未知'
    map.set(name, (map.get(name) || 0) + 1)
  })
  return Array.from(map, ([name, value]) => ({ name, value }))
})

/** 入会申请环形图 */
const applyOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c} 条（{d}%）' },
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
    text: String(applyRows.value.length),
    subtext: '申请',
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
      data: applyStats.value
    }
  ]
}))

async function fetchStats() {
  if (!canMember.value) return
  try {
    const res = await getMemberStats()
    const data = res.data || {}
    stats.total = Number(data.total) || 0
    stats.pendingApply = Number(data.pendingApply) || 0
    stats.monthJoin = Number(data.monthJoin) || 0
    stats.monthLeave = Number(data.monthLeave) || 0
  } catch (e) {
    console.warn('[工作台] 成员统计加载失败', e)
  }
}

async function fetchMyTasks() {
  if (!canTask.value) {
    myTasks.value = []
    return
  }
  loading.task = true
  try {
    const res = await listMyTask({ pageNum: 1, pageSize: 5 })
    myTasks.value = res.rows || []
  } catch (e) {
    console.warn('[工作台] 我的任务加载失败', e)
    myTasks.value = []
  } finally {
    loading.task = false
  }
}

async function fetchBoard() {
  if (!canTask.value) {
    boardColumns.value = []
    canceledCount.value = 0
    return
  }
  try {
    const res = await getTaskBoard()
    const data = res.data || {}
    boardColumns.value = data.columns || []
    canceledCount.value = Number(data.canceledCount) || 0
  } catch (e) {
    console.warn('[工作台] 任务看板加载失败', e)
    boardColumns.value = []
    canceledCount.value = 0
  }
}

async function fetchApply() {
  if (!canApply.value) {
    applyRows.value = []
    return
  }
  try {
    const res = await listApply({ pageNum: 1, pageSize: 200 })
    applyRows.value = res.rows || []
  } catch (e) {
    console.warn('[工作台] 入会申请加载失败', e)
    applyRows.value = []
  }
}

async function fetchNotice() {
  noticeList.value = []
  // 无公告权限时直接展示空态，避免触发一次注定失败的请求（以及全局错误提示）
  if (!canNotice.value) return
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
  fetchStats()
  fetchMyTasks()
  fetchBoard()
  fetchApply()
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

    &__empty {
      grid-column: 1 / -1;
      padding: 12px 0;
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
