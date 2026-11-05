<template>
  <div class="app-container task-my-page">
    <div class="oa-page-title">
      我的任务
      <small>只展示与我相关（主责 / 协作 / 派发 / 验收），不受部门数据范围限制</small>
    </div>

    <!-- 统计卡 -->
    <div class="stat-grid">
      <div
        v-for="card in statCards"
        :key="card.key"
        class="stat"
        :class="{ 'is-active': card.active }"
        @click="handleStatClick(card.key)"
      >
        <div class="stat-ico" :class="'is-' + card.tone">
          <el-icon :size="24"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-num">{{ statsUnavailable ? '--' : statValue(card.key) }}</div>
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-desc">{{ card.desc }}</div>
        </div>
      </div>
    </div>

    <div class="oa-card">
      <!-- Tab -->
      <div class="tabs-bar">
        <div
          v-for="tab in TABS"
          :key="tab.value"
          class="tabx"
          :class="{ active: queryParams.tab === tab.value }"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
          <span class="num">{{ tabCount(tab.value) }}</span>
        </div>
      </div>

      <!-- 加载骨架 -->
      <div v-if="loading && !taskList.length" class="skeleton-list">
        <div v-for="index in 3" :key="index" class="skeleton-card">
          <el-skeleton :rows="2" animated />
        </div>
      </div>

      <!-- 错误态 -->
      <div v-else-if="error" class="state-block">
        <el-icon :size="34"><WarningFilled /></el-icon>
        <p>我的任务加载失败</p>
        <el-button size="small" type="primary" @click="loadList(true)">重新加载</el-button>
      </div>

      <!-- 空态 -->
      <div v-else-if="!taskList.length" class="state-block">
        <el-icon :size="34"><FolderOpened /></el-icon>
        <p>{{ emptyText }}</p>
        <el-button v-if="queryParams.tab !== 'today'" size="small" type="primary" plain @click="goTaskList">
          去任务列表看看
        </el-button>
      </div>

      <!-- 任务卡列表 -->
      <div v-else class="task-list">
        <TaskCard
          v-for="task in taskList"
          :key="task.taskId"
          :task="task"
          variant="my"
          @open="openDetail"
          @start="handleStart"
          @submit="handleSubmit"
          @delete="handleDelete"
        />

        <div class="load-more">
          <el-button v-if="hasMore" :loading="loadingMore" size="small" @click="loadMore">加载更多</el-button>
          <span v-else class="oa-muted">没有更多了</span>
        </div>

        <div class="list-foot">
          共 {{ total }} 条与我相关的任务 · 逾期 {{ counts.overdue || 0 }} 条已置顶 · 待我验收 {{ counts.toAudit || 0 }} 条
        </div>
      </div>
    </div>

    <!-- 任务详情抽屉 -->
    <task-detail-drawer ref="detailDrawerRef" @refresh="loadList(true)" />
  </div>
</template>

<script setup name="TaskMy">
import { ElMessageBox } from 'element-plus'
import TaskCard from '../components/TaskCard.vue'
import TaskDetailDrawer from '../components/TaskDetailDrawer.vue'
import { deleteTask, getTask, listMyTask, startTask, submitTask } from '@/api/oa/task'
import { TASK_STATUS } from '@/utils/oaTask'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const TABS = [
  { value: 'today', label: '今天' },
  { value: 'week', label: '本周' },
  { value: 'all', label: '全部' },
  { value: 'created', label: '我发起的' }
]

const loading = ref(false)
const loadingMore = ref(false)
const deleting = ref(false)
const error = ref(false)
const taskList = ref([])
const total = ref(0)
const counts = ref({})
const detailDrawerRef = ref()
const pageNum = ref(1)
const pageSize = 20

const queryParams = ref({
  tab: 'today',
  status: undefined,
  overdueOnly: undefined
})

const hasMore = computed(() => taskList.value.length < total.value)
const statsUnavailable = computed(() => !Object.keys(counts.value).length && (loading.value || error.value))

const statCards = computed(() => [
  {
    key: 'today',
    label: '今日待办',
    desc: '今天截止',
    icon: 'Calendar',
    tone: 'blue',
    active: queryParams.value.tab === 'today' && !queryParams.value.overdueOnly && !queryParams.value.status
  },
  {
    key: 'week',
    label: '本周任务',
    desc: '近 7 天',
    icon: 'Calendar',
    tone: 'green',
    active: queryParams.value.tab === 'week' && !queryParams.value.overdueOnly && !queryParams.value.status
  },
  {
    key: 'overdue',
    label: '已逾期',
    desc: '置顶展示',
    icon: 'Warning',
    tone: 'red',
    active: !!queryParams.value.overdueOnly
  },
  {
    key: 'toAudit',
    label: '待我验收',
    desc: '需及时处理',
    icon: 'CircleCheck',
    tone: 'orange',
    active: String(queryParams.value.status) === TASK_STATUS.AUDIT
  }
])

const emptyText = computed(() => {
  if (queryParams.value.overdueOnly) return '当前没有逾期任务'
  if (String(queryParams.value.status) === TASK_STATUS.AUDIT) return '当前没有待你验收的任务'
  if (queryParams.value.tab === 'today') return '今天没有任务，休息一下'
  if (queryParams.value.tab === 'created') return '你还没有派发过任务'
  return '暂无与你相关的任务'
})

function statValue(key) {
  const value = counts.value[key]
  return value === undefined || value === null ? 0 : value
}

function tabCount(tab) {
  if (tab === 'today') return counts.value.today || 0
  if (tab === 'week') return counts.value.week || 0
  if (tab === 'created') return counts.value.created || 0
  return counts.value.total || 0
}

/** 查询我的任务（首页 + 统计卡） */
function loadList(reset = true) {
  if (reset) {
    pageNum.value = 1
    taskList.value = []
    total.value = 0
  }
  loading.value = reset
  loadingMore.value = !reset
  error.value = false
  const params = {
    tab: queryParams.value.tab,
    status: queryParams.value.status,
    pageNum: pageNum.value,
    pageSize
  }
  if (queryParams.value.overdueOnly) params.overdueOnly = true

  listMyTask(params)
    .then(response => {
      const rows = response.rows || []
      taskList.value = reset ? rows : taskList.value.concat(rows)
      total.value = response.total || 0
      if (response.counts) counts.value = response.counts
    })
    .catch(() => {
      error.value = true
    })
    .finally(() => {
      loading.value = false
      loadingMore.value = false
    })
}

function loadMore() {
  pageNum.value += 1
  loadList(false)
}

function switchTab(tab) {
  if (queryParams.value.tab === tab && !queryParams.value.status && !queryParams.value.overdueOnly) return
  queryParams.value.tab = tab
  queryParams.value.status = undefined
  queryParams.value.overdueOnly = undefined
  loadList(true)
}

function handleStatClick(key) {
  if (key === 'today') {
    queryParams.value.tab = 'today'
    queryParams.value.status = undefined
    queryParams.value.overdueOnly = undefined
  } else if (key === 'week') {
    queryParams.value.tab = 'week'
    queryParams.value.status = undefined
    queryParams.value.overdueOnly = undefined
  } else if (key === 'overdue') {
    queryParams.value.tab = 'all'
    queryParams.value.status = undefined
    queryParams.value.overdueOnly = true
  } else if (key === 'toAudit') {
    queryParams.value.tab = 'all'
    queryParams.value.overdueOnly = undefined
    queryParams.value.status = TASK_STATUS.AUDIT
  }
  loadList(true)
}

function openDetail(task) {
  if (!task || !task.taskId) return
  detailDrawerRef.value.open(task.taskId, { highlight: true })
}

function goTaskList() {
  router.push('/task/list')
}

/** 开始任务（仅主责人可点） */
async function handleStart(task) {
  try {
    await startTask({ taskId: task.taskId })
    proxy.$modal.msgSuccess('已开始任务')
    loadList(true)
  } catch (e) {
    // 拦截器已提示
  }
}

/** 删除任务（契约追加需求 §9，硬删除不可恢复） */
function handleDelete(task) {
  if (!task || !task.taskId || deleting.value) return
  ElMessageBox.confirm(
    '删除任务「' + (task.title || '') + '」？删除后不可恢复，其子任务、协作人、流转日志、任务附件记录与站内信将一并删除。',
    '删除任务',
    {
      type: 'error',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    }
  )
    .then(async () => {
      deleting.value = true
      try {
        await deleteTask(task.taskId)
        proxy.$modal.msgSuccess('删除成功')
        loadList(true)
      } catch (e) {
        // 拦截器已提示
      } finally {
        deleting.value = false
      }
    })
    .catch(() => {})
}

/** 提交完成：先检查子任务，再二次确认 */
function handleSubmit(task) {
  getTask(task.taskId)
    .then(response => {
      const detail = response.data || {}
      const unfinished = (detail.children || []).filter(child => String(child.status) !== TASK_STATUS.DONE)
      if (unfinished.length) {
        const names = unfinished.slice(0, 5).map(child => child.title).join('、')
        ElMessageBox.alert(
          '还有 ' + unfinished.length + ' 个子任务未完成：' + names + (unfinished.length > 5 ? ' 等' : ''),
          '暂不可提交',
          { type: 'warning', confirmButtonText: '知道了' }
        ).catch(() => {})
        return
      }
      confirmSubmit(task, detail.auditorName)
    })
    .catch(() => {
      // 无详情权限时退回直接提交，由后端做最终校验
      confirmSubmit(task, task.auditorName)
    })
}

function confirmSubmit(task, auditorName) {
  ElMessageBox.confirm('确认提交完成？提交后由 ' + (auditorName || '验收人') + ' 验收', '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => submitTask({ taskId: task.taskId, attachmentFileIds: [] }))
    .then(() => {
      proxy.$modal.msgSuccess('已提交，等待 ' + (auditorName || '验收人') + ' 验收')
      loadList(true)
    })
    .catch(() => {})
}

/** 站内信跳转：?taskId= 直接打开详情 */
function applyRouteTask() {
  const taskId = route.query.taskId
  if (taskId) {
    detailDrawerRef.value.open(taskId, { highlight: true })
  }
}

watch(
  () => route.query.taskId,
  () => applyRouteTask()
)

onMounted(() => {
  loadList(true)
  nextTick(applyRouteTask)
})
</script>

<style scoped lang="scss">
.task-my-page {
  .stat-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
    margin-bottom: 16px;
  }

  .stat {
    display: flex;
    align-items: center;
    gap: 14px;
    height: 96px;
    padding: 0 18px;
    background: #fff;
    border: 1px solid transparent;
    border-radius: 4px;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
    cursor: pointer;
    transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;

    &:hover {
      border-color: #b3d8ff;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    &.is-active {
      border-color: #409eff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.12);
    }
  }

  .stat-ico {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    border-radius: 8px;
    flex: 0 0 48px;

    &.is-blue {
      background: #ecf5ff;
      color: #409eff;
    }

    &.is-green {
      background: #f0f9eb;
      color: #67c23a;
    }

    &.is-red {
      background: #fef0f0;
      color: #f56c6c;
    }

    &.is-orange {
      background: #fdf6ec;
      color: #e6a23c;
    }
  }

  .stat-num {
    font-size: 28px;
    font-weight: 600;
    line-height: 1.2;
    color: #303133;
  }

  .stat-label {
    margin-top: 2px;
    font-size: 13px;
    color: #606266;
  }

  .stat-desc {
    font-size: 11px;
    color: #c0c4cc;
  }

  .tabs-bar {
    display: flex;
    gap: 4px;
    margin-bottom: 14px;
    border-bottom: 1px solid #ebeef5;
  }

  .tabx {
    padding: 9px 16px;
    margin-bottom: -1px;
    border-bottom: 2px solid transparent;
    color: #606266;
    font-size: 14px;
    cursor: pointer;
    user-select: none;

    &:hover {
      color: #409eff;
    }

    &.active {
      color: #409eff;
      border-bottom-color: #409eff;
      font-weight: 500;
    }

    .num {
      margin-left: 5px;
      padding: 0 5px;
      border-radius: 8px;
      background: #f0f2f5;
      color: #909399;
      font-size: 12px;
    }

    &.active .num {
      background: #ecf5ff;
      color: #409eff;
    }
  }

  .skeleton-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .skeleton-card {
    padding: 16px;
    border: 1px solid #ebeef5;
    border-radius: 6px;
  }

  .state-block {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    padding: 48px 0;
    color: #909399;
    font-size: 13px;

    p {
      margin: 0;
    }
  }

  .task-list {
    display: flex;
    flex-direction: column;
  }

  .load-more {
    padding: 8px 0 4px;
    text-align: center;
  }

  .list-foot {
    padding-top: 6px;
    font-size: 12px;
    color: #909399;
  }
}

@media (max-width: 1200px) {
  .task-my-page .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
