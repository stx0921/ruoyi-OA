<template>
  <H5TaskShell>
    <!-- ================= 顶部：日期 + 统计 + 铃铛 ================= -->
    <header class="home-head">
      <div class="home-head__row">
        <div class="home-head__date">{{ todayLabel }}</div>
        <button
          type="button"
          class="home-head__refresh"
          aria-label="刷新"
          :disabled="loading || refreshing"
          @click="doRefresh"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 11a8 8 0 1 0-2.3 5.7" />
            <path d="M20 4.5V11h-6" />
          </svg>
        </button>
        <button type="button" class="ht-bell home-head__bell" aria-label="站内信" @click="openNotify">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8.5a6 6 0 1 0-12 0c0 5.6-2 6.8-2 6.8h16s-2-1.2-2-6.8" />
            <path d="M10.3 20a2 2 0 0 0 3.4 0" />
          </svg>
          <i v-if="unread > 0" class="ht-bell__badge">{{ unread > 99 ? '99+' : unread }}</i>
        </button>
      </div>
      <div class="home-head__stat">
        <span v-if="statLoading" class="home-head__stat-skel"></span>
        <template v-else>
          今日 <b>{{ counts.today }}</b> · 逾期 <b :class="{ 'is-alert': counts.overdue > 0 }">{{ counts.overdue }}</b>
        </template>
      </div>
    </header>

    <!-- ================= Tab：今天 / 本周 / 全部 ================= -->
    <nav class="home-tabs">
      <button
        v-for="item in TASK_TABS"
        :key="item.value"
        type="button"
        class="home-tab"
        :class="{ 'is-active': tab === item.value }"
        @click="switchTab(item.value)"
      >
        {{ item.label }}
      </button>
    </nav>

    <!-- ================= 任务列表 ================= -->
    <div
      ref="scrollRef"
      class="ht-scroll"
      @scroll.passive="onScroll"
      @touchstart.passive="onTouchStart"
      @touchmove.passive="onTouchMove"
      @touchend.passive="onTouchEnd"
      @touchcancel.passive="onTouchEnd"
    >
      <!-- 下拉刷新指示 -->
      <div class="pull" :style="{ height: pullHeight + 'px' }">
        <span v-if="pullHeight > 0" class="pull__text">
          {{ refreshing ? '刷新中…' : pull >= PULL_THRESHOLD ? '松开刷新' : '下拉刷新' }}
        </span>
      </div>

      <!-- loading 骨架：任务卡 3 张 -->
      <template v-if="loading && !tasks.length">
        <div v-for="n in 3" :key="'sk-' + n" class="ht-skeleton">
          <div class="ht-skel-line ht-skel-line--wide"></div>
          <div class="ht-skel-line"></div>
          <div class="ht-skel-line ht-skel-line--short"></div>
        </div>
      </template>

      <!-- 错误态 -->
      <div v-else-if="error" class="ht-empty">
        <div class="ht-empty__ico">!</div>
        <div class="ht-empty__title">任务列表加载失败</div>
        <div class="ht-empty__sub">下拉可重新加载；若提示无权限，请联系管理员确认账号已关联成员档案</div>
        <button type="button" class="ht-btn ht-btn--primary ht-empty__action" @click="reload">重新加载</button>
      </div>

      <!-- 空态 -->
      <div v-else-if="!tasks.length" class="ht-empty">
        <div class="ht-empty__ico">
          <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <rect x="5" y="4" width="14" height="17" rx="2" />
            <path d="M9 3.5h6v3H9z" />
            <path d="M9 11h6M9 15h4" />
          </svg>
        </div>
        <div class="ht-empty__title">{{ tab === 'today' ? '今天没有任务' : '暂无任务' }}</div>
        <div class="ht-empty__sub">
          {{ tab === 'today' ? '休息一下，新的任务会出现在这里' : '切换「本周 / 全部」看看其他任务' }}
        </div>
      </div>

      <!-- 任务卡列表 -->
      <template v-else>
        <article
          v-for="row in tasks"
          :key="row.taskId"
          class="task-card"
          :class="{ 'is-overdue': isOverdue(row), 'is-done': String(row.status) === '3' }"
        >
          <i class="task-card__bar" :style="{ background: isOverdue(row) ? '#F56C6C' : priorityBarColor(row.priority) }"></i>
          <div class="task-card__main" @click="goDetail(row)">
            <h3 class="task-card__title">{{ row.title || '未命名任务' }}</h3>
            <div class="task-card__meta">
              <span v-if="row.projectName" class="ht-proj">{{ row.projectName }}</span>
              <span v-if="isOverdue(row)" class="ht-overdue-badge">{{ overdueText(row) }}</span>
              <span class="task-card__due" :class="{ 'is-red': isOverdue(row) }">{{ dueTextOf(row) }}</span>
              <span
                class="ht-pill task-card__status"
                :class="{ 'ht-pill--strike': String(row.status) === '4', 'ht-pill--sm': true }"
                :style="statusStyle(row.status)"
              >
                {{ statusMeta(row.status).label }}
              </span>
            </div>
          </div>
          <button
            v-if="quickOf(row)"
            type="button"
            class="task-card__btn"
            :class="'is-' + quickOf(row).key"
            :disabled="busyId === row.taskId"
            @click.stop="doQuick(row)"
          >
            {{ busyId === row.taskId ? '处理中' : quickOf(row).label }}
          </button>
        </article>

        <div v-if="tasks.length < total" class="load-more">
          <button type="button" class="ht-btn ht-btn--ghost-2 ht-btn--sm" :disabled="loadingMore" @click="loadMore">
            {{ loadingMore ? '加载中…' : '加载更多' }}
          </button>
        </div>

        <p class="list-foot">逾期任务仍可继续完成；本模块不提供「申请延期」，完成时间按实际记录。</p>
      </template>
    </div>

    <!-- ================= 底部 Tab：任务 / 我的 ================= -->
    <nav class="home-tabbar">
      <button type="button" class="home-tabbar__item is-active">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 6.5h16M4 12h16M4 17.5h10" />
        </svg>
        <span>任务</span>
      </button>
      <button type="button" class="home-tabbar__item" @click="goMine">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="8" r="3.4" />
          <path d="M4.5 19.5c0-3.4 3.2-5.8 7.5-5.8s7.5 2.4 7.5 5.8" />
        </svg>
        <span>我的</span>
      </button>
    </nav>

    <!-- ================= 站内信半屏弹层 ================= -->
    <H5NotifySheet ref="notifyRef" @unread="onUnread" />
  </H5TaskShell>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMyTask, startTask, submitTask } from '@/api/oa/task'
import {
  canStart,
  canSubmit,
  isOverdue,
  overdueText,
  priorityBarColor,
  statusMeta,
  formatDateTime,
  formatDueTime,
  toDate
} from '@/utils/oaTask'
import H5TaskShell from './components/H5TaskShell.vue'
import H5NotifySheet from './components/H5NotifySheet.vue'

defineOptions({ name: 'H5TaskList' })

const TASK_TABS = [
  { value: 'today', label: '今天' },
  { value: 'week', label: '本周' },
  { value: 'all', label: '全部' }
]

const PAGE_SIZE = 20
const PULL_THRESHOLD = 42
const STATE_KEY = 'oa:h5:task:list-state'

const router = useRouter()
const scrollRef = ref(null)
const notifyRef = ref(null)

const tab = ref('today')
const tasks = ref([])
const total = ref(0)
const pageNum = ref(1)
const counts = ref({ today: 0, week: 0, overdue: 0, toAudit: 0, created: 0, total: 0 })
const loading = ref(false)
const loadingMore = ref(false)
const refreshing = ref(false)
const error = ref('')
const busyId = ref(null)
const unread = ref(0)
const pull = ref(0)
const todayLabel = ref(headDateLabel())

let touchStartY = 0
let pulling = false
let savedScrollTop = 0
const pullHeight = computed(() => {
  if (refreshing.value) return 34
  return Math.round(pull.value)
})
const statLoading = computed(() => loading.value && !tasks.value.length && !error.value)

onMounted(async () => {
  const restore = readState()
  if (restore && restore.tab && TASK_TABS.some(item => item.value === restore.tab)) {
    tab.value = restore.tab
  }
  await fetchList(true)
  if (restore && Number(restore.scrollTop) > 0) {
    await nextTick()
    if (scrollRef.value) {
      scrollRef.value.scrollTop = Number(restore.scrollTop)
      savedScrollTop = Number(restore.scrollTop)
    }
  }
})

onBeforeUnmount(saveState)

/* -------------------------------------------------------------------------- */
/* 数据加载                                                                    */
/* -------------------------------------------------------------------------- */
function normalizeCounts(raw) {
  const data = raw || {}
  const num = value => Number(value) || 0
  return {
    today: num(data.today),
    week: num(data.week),
    overdue: num(data.overdue),
    toAudit: num(data.toAudit),
    created: num(data.created),
    total: num(data.total)
  }
}

async function fetchList(reset = true) {
  if (reset) {
    loading.value = true
    error.value = ''
    pageNum.value = 1
  } else {
    loadingMore.value = true
  }
  try {
    const res = await listMyTask({ tab: tab.value, pageNum: pageNum.value, pageSize: PAGE_SIZE })
    const rows = Array.isArray(res && res.rows) ? res.rows : []
    tasks.value = reset ? rows : tasks.value.concat(rows)
    total.value = Number(res && res.total) || tasks.value.length
    counts.value = normalizeCounts(res && res.counts)
  } catch (e) {
    if (reset) {
      tasks.value = []
      total.value = 0
      error.value = '加载失败'
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function reload() {
  fetchList(true)
}

function loadMore() {
  if (loading.value || loadingMore.value) return
  if (tasks.value.length >= total.value) return
  pageNum.value += 1
  fetchList(false)
}

function switchTab(value) {
  if (tab.value === value) return
  tab.value = value
  tasks.value = []
  total.value = 0
  error.value = ''
  if (scrollRef.value) scrollRef.value.scrollTop = 0
  fetchList(true)
}

/* -------------------------------------------------------------------------- */
/* 下拉刷新                                                                    */
/* -------------------------------------------------------------------------- */
function onScroll() {
  savedScrollTop = scrollRef.value ? scrollRef.value.scrollTop : 0
  if (savedScrollTop > 0 && pull.value !== 0) pull.value = 0
}

function onTouchStart(event) {
  if (refreshing.value || loading.value) return
  const target = scrollRef.value
  if (!target || target.scrollTop > 0) {
    pulling = false
    return
  }
  touchStartY = event.touches && event.touches.length ? event.touches[0].clientY : 0
  pulling = !!touchStartY
}

function onTouchMove(event) {
  if (!pulling || refreshing.value) return
  const y = event.touches && event.touches.length ? event.touches[0].clientY : 0
  const delta = y - touchStartY
  pull.value = delta > 0 ? Math.min(56, delta * 0.5) : 0
}

function onTouchEnd() {
  if (!pulling) return
  pulling = false
  if (pull.value >= PULL_THRESHOLD) {
    doRefresh()
  } else {
    pull.value = 0
  }
}

async function doRefresh() {
  if (refreshing.value || loading.value) return
  refreshing.value = true
  pull.value = 0
  try {
    await fetchList(true)
    if (notifyRef.value && typeof notifyRef.value.refreshUnread === 'function') notifyRef.value.refreshUnread()
  } finally {
    refreshing.value = false
  }
}

/* -------------------------------------------------------------------------- */
/* 快捷操作：开始 / 提交完成                                                    */
/* -------------------------------------------------------------------------- */
function quickOf(row) {
  if (!row || row.readOnly) return null
  const status = String(row.status)
  if (status === '0' && canStart(row)) return { key: 'start', label: '开始' }
  if (status === '1' && canSubmit(row)) return { key: 'submit', label: '提交完成' }
  return null
}

async function doQuick(row) {
  const action = quickOf(row)
  if (!action || busyId.value) return
  if (action.key === 'start') {
    await doStart(row)
    return
  }
  await doSubmit(row)
}

async function doStart(row) {
  busyId.value = row.taskId
  try {
    await startTask({ taskId: row.taskId })
    ElMessage.success('已开始任务')
    await fetchList(true)
  } catch (e) {
    // 错误提示由 utils/request.js 统一处理，保持原列表状态
  } finally {
    busyId.value = null
  }
}

async function doSubmit(row) {
  try {
    await ElMessageBox.confirm(
      `确认提交完成？提交后由 ${row.auditorName || '验收人'} 验收`,
      '提交完成',
      {
        confirmButtonText: '确认提交',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'ht-message-box'
      }
    )
  } catch (e) {
    return
  }
  busyId.value = row.taskId
  try {
    await submitTask({ taskId: row.taskId })
    ElMessage.success(`已提交，等待 ${row.auditorName || '验收人'} 验收`)
    await fetchList(true)
  } catch (e) {
    // 409 子任务未完成 / 状态不允许：给出「查看子任务」入口
    ElMessageBox.confirm(
      '提交未成功，可能还有子任务未完成。可到任务详情查看子任务，全部完成后重新提交。',
      '无法提交',
      {
        confirmButtonText: '查看子任务',
        cancelButtonText: '知道了',
        type: 'warning',
        customClass: 'ht-message-box'
      }
    )
      .then(() => goDetail(row))
      .catch(() => {})
  } finally {
    busyId.value = null
  }
}

/* -------------------------------------------------------------------------- */
/* 展示辅助                                                                    */
/* -------------------------------------------------------------------------- */
const pad2 = value => (value < 10 ? `0${value}` : `${value}`)

function headDateLabel() {
  const WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const now = new Date()
  return `${now.getMonth() + 1} 月 ${now.getDate()} 日 ${WEEK[now.getDay()]}`
}

function statusStyle(status) {
  const meta = statusMeta(status) || {}
  return {
    color: meta.color || '#909399',
    background: meta.bg || '#F4F4F5',
    borderColor: meta.border || '#D3D4D6'
  }
}

function dueTextOf(row) {
  if (!row) return '—'
  const status = String(row.status)
  if (status === '3') return row.finishTime ? `${formatDateTime(row.finishTime)} 完成` : '已完成'
  if (status === '4') return '已取消'
  const due = toDate(row.dueTime)
  if (!due) return '—'
  if (isOverdue(row)) return formatDateTime(row.dueTime)
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const dueStart = new Date(due.getFullYear(), due.getMonth(), due.getDate())
  const diffDays = Math.round((dueStart - todayStart) / 86400000)
  const clock = `${pad2(due.getHours())}:${pad2(due.getMinutes())}`
  if (diffDays === 0) return `今天 ${clock}`
  if (diffDays === 1) return `明天 ${clock}`
  return formatDueTime(row.dueTime)
}

/* -------------------------------------------------------------------------- */
/* 跳转 / 站内信 / 状态保持                                                    */
/* -------------------------------------------------------------------------- */
function goDetail(row) {
  if (!row || !row.taskId) return
  router.push(`/h5/task/detail/${row.taskId}`)
}

function goMine() {
  router.push('/h5/portal')
}

function openNotify() {
  if (notifyRef.value && typeof notifyRef.value.open === 'function') notifyRef.value.open()
}

function onUnread(value) {
  unread.value = Number(value) || 0
}

function readState() {
  try {
    const raw = sessionStorage.getItem(STATE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

function saveState() {
  try {
    sessionStorage.setItem(STATE_KEY, JSON.stringify({ tab: tab.value, scrollTop: savedScrollTop }))
  } catch (e) {
    // 忽略隐私模式下的存储异常
  }
}
</script>

<style lang="scss" scoped>
/* ---------- 顶部：日期 + 统计 ---------- */
.home-head {
  flex: 0 0 auto;
  padding: 10px 16px 10px;
  background: linear-gradient(135deg, #409eff, #79bbff 70%, #9fcaff);
  color: #fff;
}

.home-head__row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.home-head__date {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  opacity: 0.94;
}

.home-head__bell {
  color: #fff;
}

.home-head__refresh {
  display: flex;
  flex: 0 0 34px;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  cursor: pointer;

  svg {
    width: 17px;
    height: 17px;
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

.home-head__bell .ht-bell__badge {
  border: 1px solid #fff;
}

.home-head__stat {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.4;
  opacity: 0.96;

  b {
    margin: 0 1px;
    font-size: 20px;
    font-weight: 600;
    vertical-align: -2px;
  }

  b.is-alert {
    color: #ffd8d8;
  }
}

.home-head__stat-skel {
  display: inline-block;
  width: 108px;
  height: 14px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.35);
}

/* ---------- Tab ---------- */
.home-tabs {
  display: flex;
  flex: 0 0 auto;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
}

.home-tab {
  position: relative;
  flex: 1;
  height: 40px;
  padding: 0;
  border: none;
  background: none;
  color: #909399;
  font-family: inherit;
  font-size: 13.5px;
  cursor: pointer;

  &::after {
    content: '';
    position: absolute;
    right: 0;
    bottom: 0;
    left: 0;
    height: 2px;
    background: transparent;
    transition: background 0.15s;
  }

  &.is-active {
    color: #409eff;
    font-weight: 600;

    &::after {
      background: #409eff;
    }
  }
}

/* ---------- 下拉刷新 ---------- */
.pull {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  overflow: hidden;
  transition: height 0.12s;
}

.pull__text {
  padding-bottom: 6px;
  color: #909399;
  font-size: 11.5px;
  line-height: 1.4;
}

/* ---------- 任务卡 ---------- */
.task-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 88px;
  margin: 10px 12px;
  padding: 12px 12px 12px 16px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

  &__bar {
    position: absolute;
    top: 12px;
    bottom: 12px;
    left: 0;
    width: 4px;
    border-radius: 2px;
    background: #c0c4cc;
  }

  &.is-overdue {
    background: #fef0f0;
  }

  &.is-done .task-card__title {
    color: #909399;
  }

  &__main {
    flex: 1;
    min-width: 0;
    cursor: pointer;
  }

  &__title {
    display: -webkit-box;
    overflow: hidden;
    margin: 0 0 6px;
    color: #303133;
    font-size: 14.5px;
    font-weight: 500;
    line-height: 1.5;
    word-break: break-all;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    line-clamp: 2;
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 4px 6px;
    min-width: 0;
    color: #909399;
    font-size: 12px;
    line-height: 1.5;
  }

  &__due {
    flex: 0 0 auto;
    white-space: nowrap;

    &.is-red {
      color: #f56c6c;
      font-weight: 600;
    }
  }

  &__meta .ht-proj {
    flex: 0 1 auto;
    min-width: 0;
  }

  &__meta .ht-overdue-badge {
    flex: 0 0 auto;
  }

  &__status {
    flex: 0 0 auto;
    margin-left: auto;
  }

  &__btn {
    display: inline-flex;
    flex: 0 0 64px;
    align-items: center;
    justify-content: center;
    width: 64px;
    height: 32px;
    padding: 0 4px;
    border: 1px solid #dcdfe6;
    border-radius: 16px;
    background: #fff;
    color: #606266;
    font-family: inherit;
    font-size: 11.5px;
    line-height: 1;
    white-space: nowrap;
    cursor: pointer;

    &.is-submit {
      border-color: transparent;
      background: linear-gradient(90deg, #409eff, #66b1ff);
      color: #fff;
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
}

/* ---------- 加载更多 / 底部说明 ---------- */
.load-more {
  display: flex;
  justify-content: center;
  padding: 12px 14px 0;
}

.list-foot {
  margin: 0;
  padding: 14px 16px 18px;
  color: #909399;
  font-size: 11.5px;
  line-height: 1.7;
  text-align: center;
}

/* ---------- 底部 Tab 栏 ---------- */
.home-tabbar {
  display: flex;
  flex: 0 0 auto;
  padding-bottom: env(safe-area-inset-bottom);
  border-top: 1px solid #ebeef5;
  background: #fff;
}

.home-tabbar__item {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 56px;
  padding: 0;
  border: none;
  background: none;
  color: #909399;
  font-family: inherit;
  font-size: 11px;
  cursor: pointer;

  &.is-active {
    color: #409eff;
  }
}
</style>
