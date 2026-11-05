<template>
  <div class="app-container board-page">
    <div class="oa-page-title">
      任务看板
      <small>4 列状态 · 拖动卡片即可流转（失败自动回弹并提示原因）</small>
    </div>

    <div class="oa-card">
      <!-- 顶部筛选 -->
      <div class="board-top">
        <el-select v-model="filters.projectId" placeholder="全部项目" clearable filterable style="width: 200px" @change="loadBoard">
          <el-option v-for="project in projectOptions" :key="project.projectId" :label="project.name" :value="project.projectId" />
        </el-select>
        <el-select
          v-model="filters.assigneeId"
          placeholder="全部负责人"
          clearable
          filterable
          remote
          :remote-method="searchAssignee"
          :loading="assigneeLoading"
          style="width: 160px"
          @change="loadBoard"
        >
          <el-option v-for="item in assigneeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button icon="Refresh" @click="loadBoard">刷新</el-button>
        <span class="oa-muted">拖动卡片到其他列 → 状态即时变更并提示</span>
        <span style="flex: 1" />
        <el-button link type="primary" class="canceled-entry" @click="openCanceled">已取消 {{ canceledCount }} 条</el-button>
      </div>

      <!-- 加载骨架 -->
      <div v-if="loading && !loaded" class="board board-skeleton">
        <div v-for="index in 4" :key="index" class="kb-col-skeleton">
          <el-skeleton :rows="2" animated />
          <el-skeleton :rows="2" animated style="margin-top: 10px" />
        </div>
      </div>

      <!-- 错误态 -->
      <div v-else-if="error" class="state-block">
        <el-icon :size="34"><WarningFilled /></el-icon>
        <p>看板加载失败</p>
        <el-button size="small" type="primary" @click="loadBoard">重新加载</el-button>
      </div>

      <!-- 整板空态 -->
      <div v-else-if="boardEmpty" class="state-block">
        <el-icon :size="34"><FolderOpened /></el-icon>
        <p>当前筛选下没有任务</p>
        <el-button size="small" @click="clearFilters">清空筛选</el-button>
      </div>

      <!-- 看板 -->
      <div v-else class="board">
        <KanbanColumn
          v-for="column in columns"
          :key="column.status"
          :status="column.status"
          :label="column.label"
          :color="column.color"
          :tasks="column.tasks"
          :dragging="dragTask"
          :loading="loading"
          @drop="handleDrop"
          @drop-invalid="handleDropInvalid"
          @open="openDetail"
          @move="handleMove"
          @move-invalid="handleMoveInvalid"
          @delete="handleDelete"
          @dragstart="handleDragStart"
          @dragend="handleDragEnd"
        />
      </div>
    </div>

    <!-- 已取消任务（只读） -->
    <el-dialog v-model="canceledVisible" title="已取消任务" width="760px" append-to-body>
      <el-table v-loading="canceledLoading" :data="canceledList" empty-text="暂无已取消任务" max-height="420">
        <el-table-column label="任务编号" width="140">
          <template #default="scope">
            <span class="mono">{{ scope.row.taskNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="180" show-overflow-tooltip prop="title" />
        <el-table-column label="主责" width="100" prop="assigneeName" />
        <el-table-column label="截止时间" width="150" align="center">
          <template #default="scope">{{ formatDateTime(scope.row.dueTime) }}</template>
        </el-table-column>
        <el-table-column label="取消原因" min-width="160" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.cancelRemark || '—' }}</template>
        </el-table-column>
        <el-table-column label="取消时间" width="150" align="center">
          <template #default="scope">{{ formatDateTime(scope.row.cancelTime) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="canceledVisible = false">关 闭</el-button>
      </template>
    </el-dialog>

    <task-detail-drawer ref="detailDrawerRef" @refresh="loadBoard" />
  </div>
</template>

<script setup name="TaskBoard">
import { ElMessageBox } from 'element-plus'
import KanbanColumn from '../components/KanbanColumn.vue'
import TaskDetailDrawer from '../components/TaskDetailDrawer.vue'
import { auditTask, deleteTask, getTaskBoard, listTask, startTask, submitTask } from '@/api/oa/task'
import { listProject } from '@/api/oa/project'
import {
  BOARD_COLUMNS,
  PRIORITY_ORDER,
  boardDropCheck,
  boardMoveFailText,
  boardMoveSuccessText,
  formatDateTime,
  isOverdue,
  searchMemberOptions,
  toDate
} from '@/utils/oaTask'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const loaded = ref(false)
const error = ref(false)
const deleting = ref(false)
const columns = ref(createColumns())
const canceledCount = ref(0)
const dragTask = ref(null)
const detailDrawerRef = ref()
const projectOptions = ref([])
const assigneeOptions = ref([])
const assigneeLoading = ref(false)

const filters = ref({ projectId: undefined, assigneeId: undefined })

const canceledVisible = ref(false)
const canceledLoading = ref(false)
const canceledList = ref([])

const boardEmpty = computed(() => columns.value.every(column => !column.tasks.length))

function createColumns() {
  return BOARD_COLUMNS.map(column => ({ ...column, tasks: [] }))
}

function loadBoard() {
  loading.value = true
  error.value = false
  getTaskBoard({ projectId: filters.value.projectId, assigneeId: filters.value.assigneeId })
    .then(response => {
      const data = response.data || {}
      const sourceColumns = data.columns || []
      columns.value = BOARD_COLUMNS.map(column => {
        const hit = sourceColumns.find(item => String(item.status) === column.status)
        const tasks = (hit && hit.tasks) || []
        return {
          ...column,
          tasks: sortTasks(tasks),
          count: hit && hit.count !== undefined ? hit.count : tasks.length
        }
      })
      canceledCount.value = Number(data.canceledCount) || 0
      loaded.value = true
    })
    .catch(() => {
      error.value = true
    })
    .finally(() => {
      loading.value = false
    })
}

/** 列内排序：逾期置顶 → 优先级（高→低）→ 截止时间升序 */
function sortTasks(tasks) {
  return tasks.slice().sort((a, b) => {
    const aOverdue = isOverdue(a) ? 0 : 1
    const bOverdue = isOverdue(b) ? 0 : 1
    if (aOverdue !== bOverdue) return aOverdue - bOverdue
    const aPriority = PRIORITY_ORDER[String(a.priority)] !== undefined ? PRIORITY_ORDER[String(a.priority)] : 9
    const bPriority = PRIORITY_ORDER[String(b.priority)] !== undefined ? PRIORITY_ORDER[String(b.priority)] : 9
    if (aPriority !== bPriority) return aPriority - bPriority
    const aDue = toDate(a.dueTime)
    const bDue = toDate(b.dueTime)
    return (aDue ? aDue.getTime() : 0) - (bDue ? bDue.getTime() : 0)
  })
}

function searchAssignee(keyword) {
  assigneeLoading.value = true
  searchMemberOptions(keyword).then(options => {
    assigneeOptions.value = options
    assigneeLoading.value = false
  })
}

function loadProjects() {
  listProject({ pageNum: 1, pageSize: 200 })
    .then(response => {
      projectOptions.value = (response.rows || []).map(project => ({ projectId: project.projectId, name: project.name }))
    })
    .catch(() => {
      projectOptions.value = []
    })
}

function clearFilters() {
  filters.value = { projectId: undefined, assigneeId: undefined }
  loadBoard()
}

/* ------------------------------ 拖拽 / 移动 ------------------------------ */
function handleDragStart(task) {
  dragTask.value = task
}

function handleDragEnd() {
  dragTask.value = null
}

function handleDrop(toStatus) {
  const task = dragTask.value
  dragTask.value = null
  if (!task) return
  moveTask(task, toStatus)
}

function handleDropInvalid(payload) {
  dragTask.value = null
  if (payload && payload.reason) {
    proxy.$modal.msgWarning(payload.reason)
  }
}

function handleMove(payload) {
  if (!payload || !payload.task) return
  moveTask(payload.task, payload.toStatus)
}

function handleMoveInvalid(payload) {
  if (payload && payload.reason) {
    proxy.$modal.msgWarning(payload.reason)
  }
}

/**
 * 乐观更新 + 失败回滚
 * 落位成功：先移动卡片并 toast，再调接口；接口失败（409/403）回滚并提示原因
 */
async function moveTask(task, toStatus) {
  const fromStatus = String(task.status)
  const check = boardDropCheck(fromStatus, toStatus, task)
  if (!check.ok) {
    if (check.reason) proxy.$modal.msgWarning(boardMoveFailText(toStatus, check.reason))
    return
  }

  const snapshot = JSON.parse(JSON.stringify(columns.value))
  const fromColumn = columns.value.find(column => column.status === fromStatus)
  const toColumn = columns.value.find(column => column.status === toStatus)
  if (!fromColumn || !toColumn) return

  fromColumn.tasks = fromColumn.tasks.filter(item => item.taskId !== task.taskId)
  const movedTask = { ...task, status: toStatus }
  toColumn.tasks = sortTasks([...toColumn.tasks, movedTask])
  proxy.$modal.msgSuccess(boardMoveSuccessText(toStatus))

  try {
    if (check.action === 'start') {
      await startTask({ taskId: task.taskId })
    } else if (check.action === 'submit') {
      await submitTask({ taskId: task.taskId, attachmentFileIds: [] })
    } else if (check.action === 'audit') {
      await auditTask({ taskId: task.taskId, pass: true, remark: '' })
    }
    // 成功后静默刷新列计数与数据（保持视图最新）
    refreshSilently()
  } catch (e) {
    // 回滚到拖拽前快照
    columns.value = snapshot
    // 接口错误原因由 request 拦截器统一以红色通知展示
  }
}

/** 刷新数据但不显示整板 loading（保持卡片不闪烁） */
function refreshSilently() {
  getTaskBoard({ projectId: filters.value.projectId, assigneeId: filters.value.assigneeId })
    .then(response => {
      const data = response.data || {}
      const sourceColumns = data.columns || []
      columns.value = BOARD_COLUMNS.map(column => {
        const hit = sourceColumns.find(item => String(item.status) === column.status)
        const tasks = (hit && hit.tasks) || []
        return {
          ...column,
          tasks: sortTasks(tasks),
          count: hit && hit.count !== undefined ? hit.count : tasks.length
        }
      })
      canceledCount.value = Number(data.canceledCount) || 0
    })
    .catch(() => {})
}

/* ------------------------------ 其他 ------------------------------ */
function openDetail(task) {
  if (!task || !task.taskId) return
  detailDrawerRef.value.open(task.taskId, { highlight: true })
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
        loadBoard()
      } catch (e) {
        // 接口异常已由 request 拦截器统一提示
      } finally {
        deleting.value = false
      }
    })
    .catch(() => {})
}

function openCanceled() {
  canceledVisible.value = true
  canceledLoading.value = true
  listTask({ status: '4', pageNum: 1, pageSize: 100, excludeArchived: true })
    .then(response => {
      canceledList.value = response.rows || []
    })
    .catch(() => {
      canceledList.value = []
    })
    .finally(() => {
      canceledLoading.value = false
    })
}

onMounted(() => {
  loadProjects()
  loadBoard()
})
</script>

<style scoped lang="scss">
.board-page {
  .board-top {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 14px;
  }

  .board {
    display: grid;
    grid-template-columns: repeat(4, minmax(280px, 1fr));
    gap: 14px;
    align-items: start;
    overflow-x: auto;
  }

  .board-skeleton {
    .kb-col-skeleton {
      min-height: 420px;
      padding: 12px;
      background: #f5f7fa;
      border: 1px solid #ebeef5;
      border-radius: 6px;
    }
  }

  .canceled-entry {
    font-size: 13px;
  }

  .mono {
    font-family: Consolas, Monaco, monospace;
    font-size: 12px;
  }

  .state-block {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    padding: 60px 0;
    color: #909399;
    font-size: 13px;

    p {
      margin: 0;
    }
  }
}

@media (max-width: 1280px) {
  .board-page .board {
    grid-template-columns: repeat(4, 280px);
  }
}
</style>
