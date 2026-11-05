<template>
  <div class="app-container task-list-page">
    <div class="oa-page-title">
      任务列表
      <small>本部门 + 我参与的任务（管理员 / 主席团可见全部）</small>
    </div>

    <div class="oa-card">
      <!-- 筛选栏 -->
      <el-form ref="queryRef" :model="queryParams" :inline="true" class="oa-filter-bar">
        <el-form-item label="项目" prop="projectId">
          <el-select v-model="queryParams.projectId" placeholder="全部项目" clearable filterable style="width: 180px">
            <el-option v-for="project in projectOptions" :key="project.projectId" :label="project.name" :value="project.projectId" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="assigneeId">
          <el-select
            v-model="queryParams.assigneeId"
            placeholder="全部负责人"
            clearable
            filterable
            remote
            :remote-method="searchAssignee"
            :loading="assigneeLoading"
            style="width: 160px"
          >
            <el-option v-for="item in assigneeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in TASK_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="queryParams.priority" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in PRIORITY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="tag">
          <el-select v-model="queryParams.tag" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in tagOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 320px"
            unlink-panels
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="queryParams.keyword"
            placeholder="任务标题 / 编号"
            clearable
            style="width: 180px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作条 -->
      <div class="oa-toolbar">
        <div class="oa-toolbar__left">
          <el-button v-if="canAdd" v-hasPermi="['oa:task:add']" type="primary" plain icon="Plus" @click="handleAdd">
            新建任务
          </el-button>
          <el-button v-else type="primary" plain icon="Plus" @click="handleAddSelf">新建自有任务</el-button>
        </div>
        <div class="oa-toolbar__right">
          <span class="oa-muted">逾期置顶 · 共 {{ total }} 条</span>
        </div>
      </div>

      <!-- 批量条 -->
      <div v-if="selectedRows.length" class="batch-bar">
        <span class="sel-tag">已选 {{ selectedRows.length }} 项</span>
        <el-button v-hasPermi="['oa:task:assign']" size="small" type="primary" plain @click="openBatchReassign">批量改派</el-button>
        <el-button v-hasPermi="['oa:task:edit']" size="small" type="danger" plain @click="openBatchCancel">批量取消</el-button>
        <el-button size="small" link @click="clearSelection">清空选择</el-button>
      </div>

      <!-- 加载骨架 -->
      <div v-if="loading && !taskList.length" class="skeleton-table">
        <div v-for="index in 8" :key="index" class="skeleton-row">
          <el-skeleton :rows="1" animated />
        </div>
      </div>

      <!-- 错误态 -->
      <div v-else-if="error" class="state-block">
        <el-icon :size="34"><WarningFilled /></el-icon>
        <p>任务列表加载失败</p>
        <el-button size="small" type="primary" @click="getList">重新加载</el-button>
      </div>

      <template v-else>
        <el-table
          ref="tableRef"
          v-loading="loading"
          :data="taskList"
          :row-class-name="rowClassName"
          row-key="taskId"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" align="center" :reserve-selection="false" />
          <el-table-column label="任务编号" width="140">
            <template #default="scope">
              <span class="mono oa-link" @click="openDetail(scope.row)">{{ scope.row.taskNo || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="标题" width="260">
            <template #default="scope">
              <el-tooltip :content="scope.row.title" placement="top" :show-after="300">
                <div class="title-cell" @click="openDetail(scope.row)">{{ scope.row.title }}</div>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="项目" width="140">
            <template #default="scope">
              <template v-if="scope.row.projectName">
                <span class="proj-tag">{{ scope.row.projectName }}</span>
                <span v-if="scope.row.projectStatus === '1'" class="archived-tag">已结束</span>
              </template>
              <span v-else class="oa-muted">未挂项目</span>
            </template>
          </el-table-column>
          <el-table-column label="主责" width="110" prop="assigneeName" show-overflow-tooltip />
          <el-table-column label="协作人" width="90">
            <template #default="scope">
              <template v-if="collaboratorsOf(scope.row).length">
                <span class="avatar-row">
                  <span
                    v-for="(member, index) in collaboratorsOf(scope.row).slice(0, 3)"
                    :key="index"
                    class="avatar-xs"
                    :style="{ background: avatarColor(member.userId) }"
                    :title="member.userName"
                  >{{ nameInitial(member.userName) }}</span>
                </span>
                <span v-if="collaboratorsOf(scope.row).length > 3" class="oa-muted">+{{ collaboratorsOf(scope.row).length - 3 }}</span>
              </template>
              <span v-else class="oa-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="截止时间" width="160" align="center">
            <template #default="scope">
              <span :class="{ 'overdue-text': isOverdue(scope.row) }" :title="overdueText(scope.row)">
                {{ formatDateTime(scope.row.dueTime) }}
              </span>
              <div v-if="isOverdue(scope.row)" class="overdue-badge">{{ overdueText(scope.row) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="90" align="center">
            <template #default="scope">
              <span class="prio">
                <i :style="{ background: priorityMeta(scope.row.priority).color }" />
                {{ priorityLabel(scope.row.priority) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="scope">
              <OaTaskStatusTag :status="scope.row.status" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right" align="center">
            <template #default="scope">
              <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
              <el-button
                v-for="action in rowActionList(scope.row).actions"
                :key="action.key"
                link
                :type="action.type"
                @click="handleRowAction(action.key, scope.row)"
              >{{ action.label }}</el-button>
              <el-dropdown
                v-if="rowActionList(scope.row).more.length || canRemove"
                trigger="click"
                @command="command => handleRowAction(command, scope.row)"
              >
                <el-button link type="primary">更多<el-icon><ArrowDown /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="action in rowActionList(scope.row).more" :key="action.key" :command="action.key">
                      {{ action.label }}
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-if="canRemove"
                      command="__delete__"
                      divided
                      style="color: #f56c6c"
                    >删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
          <template #empty>
            <div class="state-block">
              <el-icon :size="30"><FolderOpened /></el-icon>
              <p>{{ emptyText }}</p>
              <div class="empty-actions">
                <el-button v-if="hasFilter" size="small" @click="resetQuery">重置筛选</el-button>
                <el-button v-if="canAdd" size="small" type="primary" @click="handleAdd">新建任务</el-button>
              </div>
            </div>
          </template>
        </el-table>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          @pagination="handlePageChange"
        />
      </template>
    </div>

    <!-- 新建 / 编辑任务抽屉 -->
    <task-form-drawer ref="taskFormRef" @success="getList" />
    <!-- 任务详情抽屉 -->
    <task-detail-drawer ref="detailDrawerRef" @refresh="getList" />

    <!-- 批量改派 -->
    <el-dialog v-model="batchReassignVisible" title="批量改派" width="760px" append-to-body :close-on-click-modal="false">
      <div class="dialog-tip">将对选中的 {{ selectedRows.length }} 条任务改派主责人，任务状态不变。</div>
      <el-form label-width="90px">
        <el-form-item label="新主责人" required>
          <el-select
            v-model="batchReassignAssignee"
            placeholder="搜索姓名 / 学号"
            filterable
            remote
            clearable
            :remote-method="searchBatchMember"
            :loading="batchMemberLoading"
            style="width: 100%"
          >
            <el-option v-for="item in batchMemberOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchReassignVisible = false">取 消</el-button>
        <el-button type="primary" :loading="batchSubmitting" :disabled="!batchReassignAssignee" @click="submitBatchReassign">
          确认改派
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量取消 -->
    <el-dialog v-model="batchCancelVisible" title="批量取消" width="420px" append-to-body :close-on-click-modal="false">
      <div class="dialog-tip">将取消 {{ batchCancelRows.length }} 条任务，取消后不可恢复，确认？</div>
      <el-form label-width="82px">
        <el-form-item label="取消原因" required>
          <el-input v-model="batchCancelRemark" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请填写取消原因（2–200 字）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchCancelVisible = false">取 消</el-button>
        <el-button type="danger" :loading="batchSubmitting" :disabled="!batchCancelValid" @click="submitBatchCancel">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TaskList">
import { ElMessageBox } from 'element-plus'
import OaTaskStatusTag from '@/components/oa/OaTaskStatusTag.vue'
import TaskFormDrawer from '../components/TaskFormDrawer.vue'
import TaskDetailDrawer from '../components/TaskDetailDrawer.vue'
import { cancelTask, deleteTask, getTask, listTask, reassignTask, startTask, submitTask } from '@/api/oa/task'
import { listProject } from '@/api/oa/project'
import {
  PRIORITY_OPTIONS,
  TASK_STATUS_OPTIONS,
  TASK_TAG_FALLBACK,
  avatarColor,
  formatDateTime,
  isOverdue,
  nameInitial,
  overdueText,
  priorityLabel,
  priorityMeta,
  rowActions,
  searchMemberOptions
} from '@/utils/oaTask'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()
const route = useRoute()
const { oa_task_tag } = useDict('oa_task_tag')

const loading = ref(false)
const error = ref(false)
const taskList = ref([])
const total = ref(0)
const selectedRows = ref([])
const tableRef = ref()
const queryRef = ref()
const projectOptions = ref([])
const assigneeOptions = ref([])
const assigneeLoading = ref(false)
const dateRange = ref(defaultDateRange())

const taskFormRef = ref()
const detailDrawerRef = ref()

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  projectId: undefined,
  assigneeId: undefined,
  status: undefined,
  priority: undefined,
  tag: undefined,
  keyword: undefined,
  params: {}
})

const canAdd = computed(() => proxy.$auth.hasPermi('oa:task:add'))
const canRemove = computed(() => proxy.$auth.hasPermi('oa:task:remove'))
const deleting = ref(false)

const tagOptions = computed(() => {
  const dict = oa_task_tag && oa_task_tag.value ? oa_task_tag.value : []
  if (dict.length) return dict.map(item => ({ value: item.label, label: item.label }))
  return Object.keys(TASK_TAG_FALLBACK).map(key => ({ value: TASK_TAG_FALLBACK[key], label: TASK_TAG_FALLBACK[key] }))
})

const hasFilter = computed(() => {
  const q = queryParams.value
  return !!(q.projectId || q.assigneeId || q.status || q.priority || q.tag || q.keyword)
})

const emptyText = computed(() => (hasFilter.value ? '没有匹配的任务' : '暂无任务'))

/* ------------------------------ 查询 ------------------------------ */
function defaultDateRange() {
  const end = new Date()
  const begin = new Date()
  begin.setDate(begin.getDate() - 30)
  return [parseTime(begin, '{y}-{m}-{d}'), parseTime(end, '{y}-{m}-{d}')]
}

/** 时间范围写入 params[beginDue|endDue] */
function syncDateRange() {
  if (dateRange.value && dateRange.value.length === 2) {
    queryParams.value.params = { beginDue: dateRange.value[0], endDue: dateRange.value[1] }
  } else {
    queryParams.value.params = {}
  }
}

function getList() {
  syncDateRange()
  loading.value = true
  error.value = false
  listTask(queryParams.value)
    .then(response => {
      taskList.value = response.rows || []
      total.value = response.total || 0
    })
    .catch(() => {
      error.value = true
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  clearSelection(true)
  getList()
}

function resetQuery() {
  if (queryRef.value) queryRef.value.resetFields()
  queryParams.value.projectId = undefined
  queryParams.value.assigneeId = undefined
  queryParams.value.status = undefined
  queryParams.value.priority = undefined
  queryParams.value.tag = undefined
  queryParams.value.keyword = undefined
  dateRange.value = defaultDateRange()
  queryParams.value.pageNum = 1
  clearSelection(true)
  getList()
}

function handlePageChange() {
  clearSelection(true)
  getList()
}

/** 加载项目下拉（无项目权限时静默降级） */
function loadProjects() {
  listProject({ pageNum: 1, pageSize: 200 })
    .then(response => {
      projectOptions.value = (response.rows || []).map(project => ({ projectId: project.projectId, name: project.name }))
    })
    .catch(() => {
      projectOptions.value = []
    })
}

function searchAssignee(keyword) {
  assigneeLoading.value = true
  searchMemberOptions(keyword).then(options => {
    assigneeOptions.value = options
    assigneeLoading.value = false
  })
}

/* ------------------------------ 表格渲染 ------------------------------ */
function rowClassName({ row }) {
  if (isOverdue(row)) return 'row-overdue'
  if (row.readOnly) return 'row-archived'
  return ''
}

function rowActionList(row) {
  const result = rowActions(row)
  const perm = {
    start: 'oa:task:edit',
    submit: 'oa:task:submit',
    audit: 'oa:task:audit',
    reject: 'oa:task:audit',
    edit: 'oa:task:edit',
    reassign: 'oa:task:assign',
    cancel: 'oa:task:edit'
  }
  const filter = list => list.filter(action => proxy.$auth.hasPermi(perm[action.key]))
  return { actions: filter(result.actions), more: filter(result.more) }
}

function collaboratorsOf(row) {
  if (Array.isArray(row.collaborators)) return row.collaborators
  if (Array.isArray(row.collaboratorNames)) {
    return row.collaboratorNames.map((name, index) => ({ userId: index + 1, userName: name }))
  }
  return []
}

function openDetail(row) {
  if (!row || !row.taskId) return
  detailDrawerRef.value.open(row.taskId, { highlight: true })
}

/* ------------------------------ 行操作 ------------------------------ */
function handleRowAction(key, row) {
  switch (key) {
    case 'start':
      startRow(row)
      break
    case 'submit':
      submitRow(row)
      break
    case '__delete__':
      confirmDeleteTask(row)
      break
    case 'audit':
    case 'reject':
    case 'reassign':
    case 'cancel':
      detailDrawerRef.value.open(row.taskId, { highlight: true })
      break
    case 'edit':
      getTask(row.taskId).then(response => taskFormRef.value.open({ mode: 'edit', data: response.data || {} })).catch(() => {})
      break
    default:
      break
  }
}

/** 行内开始：status=0 → 进行中 */
async function startRow(row) {
  try {
    await startTask({ taskId: row.taskId })
    proxy.$modal.msgSuccess('已开始任务')
    getList()
  } catch (e) {
    // 拦截器已提示
  }
}

/** 行内提交：先查子任务，再二次确认 */
function submitRow(row) {
  getTask(row.taskId)
    .then(response => {
      const detail = response.data || {}
      const unfinished = (detail.children || []).filter(child => String(child.status) !== '3')
      if (unfinished.length) {
        const names = unfinished.slice(0, 5).map(child => child.title).join('、')
        ElMessageBox.alert(
          '还有 ' + unfinished.length + ' 个子任务未完成：' + names + (unfinished.length > 5 ? ' 等' : ''),
          '暂不可提交',
          { type: 'warning', confirmButtonText: '知道了' }
        ).catch(() => {})
        return
      }
      confirmRowSubmit(row, detail.auditorName)
    })
    .catch(() => confirmRowSubmit(row, row.auditorName))
}

function confirmRowSubmit(row, auditorName) {
  ElMessageBox.confirm('确认提交完成？提交后由 ' + (auditorName || '验收人') + ' 验收', '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => submitTask({ taskId: row.taskId, attachmentFileIds: [] }))
    .then(() => {
      proxy.$modal.msgSuccess('已提交，等待 ' + (auditorName || '验收人') + ' 验收')
      getList()
    })
    .catch(() => {})
}

/** 删除任务（契约追加需求 §9，硬删除不可恢复） */
function confirmDeleteTask(row) {
  if (!row || !row.taskId || deleting.value) return
  ElMessageBox.confirm(
    '删除任务「' + (row.title || '') + '」？删除后不可恢复，其子任务、协作人、流转日志、任务附件记录与站内信将一并删除。',
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
        await deleteTask(row.taskId)
        proxy.$modal.msgSuccess('删除成功')
        getList()
      } catch (e) {
        // 接口异常已由 request 拦截器统一提示
      } finally {
        deleting.value = false
      }
    })
    .catch(() => {})
}

/* ------------------------------ 新建 / 编辑 ------------------------------ */
function handleAdd() {
  taskFormRef.value.open({ mode: 'add' })
}

function handleAddSelf() {
  taskFormRef.value.open({ mode: 'self' })
}

/* ------------------------------ 勾选 / 批量 ------------------------------ */
function handleSelectionChange(selection) {
  if (selection.length > 50) {
    proxy.$modal.msgWarning('单次最多选择 50 条，请分批操作')
  }
  selectedRows.value = selection
}

function clearSelection(silent = false) {
  if (selectedRows.value.length && !silent) {
    proxy.$modal.msg('已清空选择')
  }
  if (tableRef.value) tableRef.value.clearSelection()
  selectedRows.value = []
}

const batchReassignVisible = ref(false)
const batchReassignAssignee = ref(undefined)
const batchMemberOptions = ref([])
const batchMemberLoading = ref(false)
const batchCancelVisible = ref(false)
const batchCancelRows = ref([])
const batchCancelRemark = ref('')
const batchSubmitting = ref(false)

const batchCancelValid = computed(() => {
  const text = String(batchCancelRemark.value || '').trim()
  return text.length >= 2 && text.length <= 200
})

function searchBatchMember(keyword) {
  batchMemberLoading.value = true
  searchMemberOptions(keyword).then(options => {
    batchMemberOptions.value = options
    batchMemberLoading.value = false
  })
}

function openBatchReassign() {
  if (selectedRows.value.length > 50) {
    proxy.$modal.msgWarning('单次最多选择 50 条，请分批操作')
    return
  }
  batchReassignAssignee.value = undefined
  batchMemberOptions.value = []
  batchReassignVisible.value = true
  searchBatchMember('')
}

async function submitBatchReassign() {
  if (!batchReassignAssignee.value) return
  batchSubmitting.value = true
  let success = 0
  let failed = 0
  for (const row of selectedRows.value) {
    try {
      let collaboratorIds = []
      try {
        const response = await getTask(row.taskId)
        const detail = response.data || {}
        collaboratorIds = (detail.collaborators || []).map(item => item.userId)
      } catch (e) {
        collaboratorIds = []
      }
      await reassignTask({ taskId: row.taskId, assigneeId: batchReassignAssignee.value, collaboratorIds })
      success++
    } catch (e) {
      failed++
    }
  }
  batchSubmitting.value = false
  batchReassignVisible.value = false
  proxy.$modal.msgSuccess('成功 ' + success + ' 条，失败 ' + failed + ' 条')
  clearSelection(true)
  getList()
}

function openBatchCancel() {
  if (selectedRows.value.length > 50) {
    proxy.$modal.msgWarning('单次最多选择 50 条，请分批操作')
    return
  }
  const rows = selectedRows.value.filter(row => ['0', '1'].includes(String(row.status)) && !row.readOnly)
  if (!rows.length) {
    proxy.$modal.msgWarning('选中的任务当前状态不可取消（已完成 / 已取消 / 归档项目）')
    return
  }
  batchCancelRows.value = rows
  batchCancelRemark.value = ''
  batchCancelVisible.value = true
}

async function submitBatchCancel() {
  if (!batchCancelValid.value) return
  const remark = batchCancelRemark.value.trim()
  batchSubmitting.value = true
  let success = 0
  let failed = 0
  for (const row of batchCancelRows.value) {
    try {
      await cancelTask({ taskId: row.taskId, remark })
      success++
    } catch (e) {
      failed++
    }
  }
  batchSubmitting.value = false
  batchCancelVisible.value = false
  proxy.$modal.msgSuccess('成功 ' + success + ' 条，失败 ' + failed + ' 条')
  clearSelection(true)
  getList()
}

/* ------------------------------ 路由联动 ------------------------------ */
function applyRouteQuery() {
  let changed = false
  if (route.query.projectId) {
    queryParams.value.projectId = Number(route.query.projectId) || route.query.projectId
    changed = true
  }
  if (route.query.status) {
    queryParams.value.status = String(route.query.status)
    changed = true
  }
  if (changed) {
    queryParams.value.pageNum = 1
    getList()
  }
  if (route.query.taskId) {
    detailDrawerRef.value.open(route.query.taskId, { highlight: true })
  }
}

watch(
  () => route.query,
  () => applyRouteQuery(),
  { deep: true }
)

onMounted(() => {
  loadProjects()
  getList()
  nextTick(applyRouteQuery)
})
</script>

<style scoped lang="scss">
.task-list-page {
  .oa-filter-bar {
    :deep(.el-form-item) {
      margin-bottom: 0;
    }
  }

  .batch-bar {
    display: flex;
    align-items: center;
    gap: 10px;
    height: 48px;
    padding: 0 14px;
    margin-bottom: 12px;
    background: #ecf5ff;
    border-radius: 4px;

    .sel-tag {
      padding: 1px 8px;
      border: 1px solid #b3d8ff;
      border-radius: 3px;
      background: #fff;
      color: #337ecc;
      font-size: 12px;
    }
  }

  .mono {
    font-family: Consolas, Monaco, monospace;
    font-size: 12px;
  }

  .title-cell {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 13px;
    color: #303133;
    line-height: 1.45;
    cursor: pointer;

    &:hover {
      color: #409eff;
    }
  }

  .proj-tag {
    display: inline-block;
    padding: 1px 6px;
    border: 1px solid #d9ecff;
    border-radius: 3px;
    background: #ecf5ff;
    color: #337ecc;
    font-size: 11px;
    white-space: nowrap;
  }

  .archived-tag {
    margin-left: 4px;
    padding: 0 4px;
    border-radius: 2px;
    background: #f4f4f5;
    color: #909399;
    font-size: 11px;
  }

  .overdue-text {
    color: #f56c6c;
    font-weight: 600;
  }

  .overdue-badge {
    display: inline-flex;
    align-items: center;
    height: 16px;
    padding: 0 5px;
    margin-top: 2px;
    border: 1px solid #fbc4c4;
    border-radius: 8px;
    background: #fef0f0;
    color: #f56c6c;
    font-size: 11px;
    line-height: 1;
  }

  .prio {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 12px;
    color: #606266;

    i {
      width: 8px;
      height: 8px;
      border-radius: 2px;
      display: block;
      flex: 0 0 8px;
    }
  }

  .avatar-row {
    display: inline-flex;
    align-items: center;

    .avatar-xs {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 20px;
      height: 20px;
      margin-left: -6px;
      border: 2px solid #fff;
      border-radius: 50%;
      color: #fff;
      font-size: 10px;
      flex: 0 0 20px;

      &:first-child {
        margin-left: 0;
      }
    }
  }

  .skeleton-table {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 8px 0;
  }

  .skeleton-row {
    padding: 8px 12px;
    border-bottom: 1px solid #f5f7fa;
  }

  .state-block {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    padding: 40px 0;
    color: #909399;
    font-size: 13px;

    p {
      margin: 0;
    }
  }

  .empty-actions {
    display: flex;
    gap: 8px;
  }

  :deep(.el-table .row-overdue) {
    background: rgba(245, 108, 108, 0.08);

    &:hover > td {
      background: rgba(245, 108, 108, 0.14) !important;
    }
  }

  :deep(.el-table .row-archived) {
    background: #fafafa;
    color: #909399;
  }
}

.dialog-tip {
  margin-bottom: 14px;
  padding: 8px 12px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  font-size: 12px;
  color: #b96a1e;
  line-height: 1.6;
}
</style>
