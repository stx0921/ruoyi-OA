<template>
  <div class="app-container oa-todo-page">
    <div class="oa-page-title">
      待办中心
      <small>按角色聚合你的待处理任务</small>
    </div>

    <div class="oa-card">
      <!-- 四个视角 -->
      <el-tabs v-model="activeTab" class="oa-todo__tabs">
        <el-tab-pane v-for="tab in tabs" :key="tab.key" :name="tab.key">
          <template #label>
            <span class="oa-todo__tab" :class="{ 'is-active': activeTab === tab.key }">
              <span>{{ tab.label }}</span>
              <em v-if="tab.key !== 'cc'" class="oa-todo__badge">{{ badgeText(totals[tab.key]) }}</em>
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 查询区 -->
      <div class="oa-filter-bar">
        <el-form :inline="true" :model="filters" class="oa-todo__filter" @submit.prevent>
          <el-form-item label="流程类型">
            <el-select v-model="filters.flowType" placeholder="全部流程类型" style="width: 180px">
              <el-option label="请假审批" value="leave" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="handleRefresh">刷新</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 待我审批 -->
      <el-table
        v-if="activeTab === 'todo'"
        v-loading="tabLoading.todo"
        :data="lists.todo"
        row-key="taskId"
      >
        <el-table-column label="任务标题" min-width="250" show-overflow-tooltip>
          <template #default="scope">
            <span class="oa-strong">{{ taskTitle(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单号" width="130">
          <template #default="scope">
            <span class="oa-muted">{{ rowLeaveNo(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发起人" prop="applyUserName" width="100" />
        <el-table-column label="请假类型" width="100">
          <template #default="scope">{{ leaveTypeText(scope.row.leaveType, oa_leave_type) }}</template>
        </el-table-column>
        <el-table-column label="时长" width="100">
          <template #default="scope">{{ oaDurationText(scope.row.duration) }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="scope">{{ oaDateTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="已等待" width="110">
          <template #default="scope">
            <span class="oa-pill" :class="'oa-pill--' + waitTone(scope.row.createTime)">
              {{ oaWaitText(scope.row.createTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="当前节点" width="140">
          <template #default="scope">{{ scope.row.taskName || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openAudit(scope.row)">审批</el-button>
            <el-button link type="primary" @click="goTrace(scope.row.leaveId)">流程</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="oa-empty">暂无待审批任务</div>
        </template>
      </el-table>

      <pagination
        v-if="activeTab === 'todo'"
        v-show="totals.todo > 0"
        :total="totals.todo"
        v-model:page="pages.todo.pageNum"
        v-model:limit="pages.todo.pageSize"
        @pagination="getList('todo')"
      />

      <!-- 我已审批 -->
      <el-table
        v-if="activeTab === 'done'"
        v-loading="tabLoading.done"
        :data="lists.done"
        row-key="taskId"
      >
        <el-table-column label="任务标题" min-width="250" show-overflow-tooltip>
          <template #default="scope">
            <span class="oa-strong">{{ taskTitle(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单号" width="130">
          <template #default="scope">
            <span class="oa-muted">{{ rowLeaveNo(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发起人" prop="applyUserName" width="100" />
        <el-table-column label="请假类型" width="100">
          <template #default="scope">{{ leaveTypeText(scope.row.leaveType, oa_leave_type) }}</template>
        </el-table-column>
        <el-table-column label="时长" width="100">
          <template #default="scope">{{ oaDurationText(scope.row.duration) }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="160">
          <template #default="scope">{{ oaDateTime(scope.row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="当前节点" width="140">
          <template #default="scope">
            <span :class="{ 'oa-muted': !doneNodeMap[String(scope.row.leaveId)] }">
              {{ doneNodeText(scope.row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="goTrace(scope.row.leaveId)">流程</el-button>
            <el-button link type="primary" @click="openView(scope.row)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="oa-empty">暂无已审批任务</div>
        </template>
      </el-table>

      <pagination
        v-if="activeTab === 'done'"
        v-show="totals.done > 0"
        :total="totals.done"
        v-model:page="pages.done.pageNum"
        v-model:limit="pages.done.pageSize"
        @pagination="getList('done')"
      />

      <!-- 我发起的 -->
      <el-table
        v-if="activeTab === 'mine'"
        v-loading="tabLoading.mine"
        :data="lists.mine"
        row-key="leaveId"
      >
        <el-table-column label="单号" width="130">
          <template #default="scope">
            <el-button link type="primary" @click="goTrace(scope.row.leaveId)">
              {{ rowLeaveNo(scope.row) }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="请假类型" width="100">
          <template #default="scope">{{ leaveTypeText(scope.row.leaveType, oa_leave_type) }}</template>
        </el-table-column>
        <el-table-column label="时长" width="100">
          <template #default="scope">{{ oaDurationText(scope.row.duration) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <OaStatusTag :status="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="scope">{{ oaDateTime(scope.row.applyTime || scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="goTrace(scope.row.leaveId)">流程</el-button>
            <el-button link type="primary" @click="openView(scope.row)">查看</el-button>
            <el-button
              v-if="String(scope.row.status) === '1'"
              link
              type="warning"
              v-hasPermi="['oa:leave:submit']"
              @click="handleRevoke(scope.row)"
            >撤回</el-button>
            <el-button
              v-if="String(scope.row.status) === '0' && isMine(scope.row)"
              link
              type="success"
              v-hasPermi="['oa:leave:submit']"
              @click="handleSubmit(scope.row)"
            >提交</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="oa-empty">暂无发起的请假申请</div>
        </template>
      </el-table>

      <pagination
        v-if="activeTab === 'mine'"
        v-show="totals.mine > 0"
        :total="totals.mine"
        v-model:page="pages.mine.pageNum"
        v-model:limit="pages.mine.pageSize"
        @pagination="getList('mine')"
      />

      <!-- 抄送我的：后端暂未提供 -->
      <div v-if="activeTab === 'cc'" class="oa-empty">
        后端暂未接入抄送数据，P3 提供
      </div>
    </div>

    <!-- 审批 / 查看抽屉 -->
    <AuditDrawer
      v-model="drawerVisible"
      :task="currentRow"
      :readonly="drawerReadonly"
      :users="users"
      @refresh="handleDrawerRefresh"
    />
  </div>
</template>

<script setup name="Todo">
import { listTodoTask, listDoneTask } from '@/api/oa/leaveTask'
import { listLeave, getLeaveTrace, submitLeave, revokeLeave } from '@/api/oa/leave'
import { listUser } from '@/api/system/user'
import {
  leaveTypeText,
  oaCurrentNode,
  oaDateTime,
  oaDurationText,
  oaLeaveNo,
  oaUserMap,
  oaWaitText
} from '@/utils/oa'
import OaStatusTag from '@/components/oa/OaStatusTag.vue'
import AuditDrawer from './AuditDrawer.vue'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { oa_leave_type } = useDict('oa_leave_type')

/** 四个视角；抄送我的后端未提供，仅保留入口 */
const tabs = [
  { key: 'todo', label: '待我审批' },
  { key: 'done', label: '我已审批' },
  { key: 'mine', label: '我发起的' },
  { key: 'cc', label: '抄送我的' }
]

const activeTab = ref('todo')
// 流程类型：当前仅接入「请假审批」，保留筛选项占位（/oa/task/todo|done 不支持关键词过滤）
const filters = reactive({ flowType: 'leave' })

const lists = reactive({
  todo: [],
  done: [],
  mine: []
})
const totals = reactive({
  todo: 0,
  done: 0,
  mine: 0
})
const tabLoading = reactive({
  todo: false,
  done: false,
  mine: false
})
const pages = reactive({
  todo: { pageNum: 1, pageSize: 10 },
  done: { pageNum: 1, pageSize: 10 },
  mine: { pageNum: 1, pageSize: 10 }
})

// 抽屉：当前行 / 是否只读
const drawerVisible = ref(false)
const drawerReadonly = ref(false)
const currentRow = ref(null)
// userId → { nickName, deptName }，抽屉里的审批记录 / 流程图用
const users = ref({})
let usersLoaded = false
// 「我已审批」的当前节点不在 /oa/task/done 返回里，按 leaveId 懒加载轨迹补齐并缓存
const doneNodeMap = ref({})
const tracedLeaveIds = new Set()

/** Tab 切换时按需拉取该视角的数据 */
watch(activeTab, tab => {
  if (tab === 'cc') return
  getList(tab)
})

/** 支持 /oa/todo?tab=done 直接进入某个视角 */
watch(
  () => route.query.tab,
  tab => {
    const next = resolveTab(tab)
    if (next !== activeTab.value) {
      activeTab.value = next
    }
  }
)

function resolveTab(tab) {
  const key = String(tab || '')
  return tabs.some(item => item.key === key) ? key : 'todo'
}

/** 任务标题：张三的请假申请（事假 2.0 天） */
function taskTitle(row) {
  const type = leaveTypeText(row.leaveType, oa_leave_type.value)
  const duration = oaDurationText(row.duration)
  const detail = duration === '—' ? type : type + ' ' + duration
  return (row.applyUserName || '申请人') + '的请假申请（' + detail + '）'
}

/** 单号：待办 / 已办接口直接给 leaveNo，我发起的用 leaveId 拼 */
function rowLeaveNo(row) {
  return row.leaveNo || oaLeaveNo(row)
}

/** 已等待药丸色调：超过 12 小时标红 */
function waitTone(createTime) {
  const time = new Date(String(createTime || '').replace(/-/g, '/')).getTime()
  if (isNaN(time)) return 'gray'
  const hours = (Date.now() - time) / 3600000
  return hours >= 12 ? 'red' : 'orange'
}

/** 数量 badge 文案 */
function badgeText(value) {
  const num = Number(value) || 0
  return num > 99 ? '99+' : String(num)
}

function isMine(row) {
  return String(row.applyUserId) === String(userStore.id)
}

/** 是否拥有流程跟踪权限（没有就不去补「当前节点」，避免无谓的 403） */
function canQueryTrace() {
  const permissions = userStore.permissions || []
  return permissions.includes('*:*:*') || permissions.includes('oa:leave:query')
}

/**
 * 「我已审批」列表的当前节点只能从流程轨迹推导，这里按页内去重后异步补齐。
 * 拉取失败时保留 taskName（即我处理的节点）兜底，不影响列表渲染。
 */
function enrichDoneNodes(rows) {
  if (!canQueryTrace()) return
  const leaveIds = [...new Set((rows || []).map(row => row.leaveId).filter(id => id !== undefined && id !== null))]
  leaveIds.forEach(leaveId => {
    const key = String(leaveId)
    if (tracedLeaveIds.has(key)) return
    tracedLeaveIds.add(key)
    getLeaveTrace(leaveId)
      .then(res => {
        const data = res.data || {}
        if (!data.leave) return
        const node = oaCurrentNode(data.leave, data.records || [])
        doneNodeMap.value[key] = node.name || (String(data.leave.status) === '2' ? '已办结' : '')
      })
      .catch(() => {})
  })
}

/** 我已审批：优先展示轨迹推导的当前节点，轨迹未返回时用处理节点兜底 */
function doneNodeText(row) {
  const nodeName = doneNodeMap.value[String(row.leaveId)]
  return nodeName || row.taskName || '—'
}

/** 拉取某个视角的列表 */
async function getList(tab = activeTab.value) {
  if (tab === 'cc') return
  const query = { ...pages[tab] }
  if (tab === 'todo') {
    tabLoading.todo = true
    try {
      const res = await listTodoTask(query)
      lists.todo = res.rows || []
      totals.todo = Number(res.total) || 0
    } catch (e) {
      lists.todo = []
      totals.todo = 0
    } finally {
      tabLoading.todo = false
    }
    return
  }
  if (tab === 'done') {
    tabLoading.done = true
    try {
      const res = await listDoneTask(query)
      lists.done = res.rows || []
      totals.done = Number(res.total) || 0
      enrichDoneNodes(lists.done)
    } catch (e) {
      lists.done = []
      totals.done = 0
    } finally {
      tabLoading.done = false
    }
    return
  }
  if (tab === 'mine') {
    if (userStore.id) {
      query.applyUserId = userStore.id
    }
    tabLoading.mine = true
    try {
      const res = await listLeave(query)
      lists.mine = res.rows || []
      totals.mine = Number(res.total) || 0
    } catch (e) {
      lists.mine = []
      totals.mine = 0
    } finally {
      tabLoading.mine = false
    }
  }
}

/** 三个接口的 total 单独取一次（pageSize=1），只用于 Tab badge */
async function loadCounts() {
  const countQuery = { pageNum: 1, pageSize: 1 }
  const mineQuery = userStore.id ? { ...countQuery, applyUserId: userStore.id } : { ...countQuery }
  const [todoRes, doneRes, mineRes] = await Promise.allSettled([
    listTodoTask(countQuery),
    listDoneTask(countQuery),
    listLeave(mineQuery)
  ])
  if (todoRes.status === 'fulfilled') totals.todo = Number(todoRes.value.total) || 0
  if (doneRes.status === 'fulfilled') totals.done = Number(doneRes.value.total) || 0
  if (mineRes.status === 'fulfilled') totals.mine = Number(mineRes.value.total) || 0
}

/** 用户姓名映射：可能无 system:user:list 权限，失败则降级为「用户{id}」 */
async function ensureUsers() {
  if (usersLoaded) return
  usersLoaded = true
  try {
    const res = await listUser({ pageNum: 1, pageSize: 500 })
    users.value = oaUserMap(res.rows || [])
  } catch (e) {
    users.value = {}
  }
}

function handleQuery() {
  if (activeTab.value === 'cc') return
  pages[activeTab.value].pageNum = 1
  getList(activeTab.value)
}

function handleRefresh() {
  handleQuery()
  loadCounts()
}

/** 打开审批抽屉（待我审批） */
function openAudit(row) {
  currentRow.value = row
  drawerReadonly.value = false
  drawerVisible.value = true
  ensureUsers()
}

/** 打开只读抽屉（我已审批 / 我发起的） */
function openView(row) {
  currentRow.value = row
  drawerReadonly.value = true
  drawerVisible.value = true
  ensureUsers()
}

/** 抽屉里审批成功后：刷新当前视角与 badge */
function handleDrawerRefresh() {
  getList(activeTab.value)
  loadCounts()
}

/** 跳转流程跟踪页 */
function goTrace(leaveId) {
  if (!leaveId) {
    proxy.$modal.msgWarning('该记录未关联请假单')
    return
  }
  router.push({ path: '/oa/trace', query: { leaveId: String(leaveId) } }).catch(() => {})
}

/** 撤回（仅审批中） */
function handleRevoke(row) {
  proxy.$modal
    .confirm('确认撤回请假申请「' + rowLeaveNo(row) + '」吗？撤回后不可恢复。')
    .then(() => revokeLeave(row.leaveId))
    .then(() => {
      proxy.$modal.msgSuccess('撤回成功')
      getList('mine')
      loadCounts()
    })
    .catch(() => {})
}

/** 提交草稿（仅本人草稿） */
function handleSubmit(row) {
  proxy.$modal
    .confirm('确认提交请假申请「' + rowLeaveNo(row) + '」进入审批吗？')
    .then(() => submitLeave(row.leaveId))
    .then(() => {
      proxy.$modal.msgSuccess('提交成功')
      getList('mine')
      loadCounts()
    })
    .catch(() => {})
}

onMounted(() => {
  const tab = resolveTab(route.query.tab)
  if (tab !== activeTab.value) {
    // 赋值会触发上面的 watch 去拉数据
    activeTab.value = tab
  } else {
    getList(tab)
  }
  loadCounts()
})
</script>

<style scoped>
.oa-todo__tabs :deep(.el-tabs__header) {
  margin-bottom: 6px;
}

.oa-todo__tabs :deep(.el-tabs__content) {
  display: none;
}

.oa-todo__tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.oa-todo__badge {
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 9px;
  background: #f0f2f5;
  color: #909399;
  font-size: 12px;
  font-style: normal;
  line-height: 18px;
  text-align: center;
}

.oa-todo__tab.is-active .oa-todo__badge {
  background: #236de9;
  color: #fff;
}

.oa-todo__filter :deep(.el-form-item) {
  margin-bottom: 0;
}
</style>
