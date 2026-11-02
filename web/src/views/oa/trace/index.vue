<template>
  <div class="app-container">
    <!-- 未指定请假单：先选一张再看流程（从左侧菜单直接进入时的入口） -->
    <template v-if="!leaveId">
      <div class="oa-page-title">
        流程跟踪
        <small>选择请假单后查看流程图、流程变量与审批记录</small>
      </div>

      <div class="oa-card">
        <div class="oa-card-head">
          <h3>选择一张请假单查看流程</h3>
          <el-button icon="Refresh" @click="loadMyLeaves">刷新</el-button>
        </div>
        <div class="oa-trace__tip oa-muted">
          也可以从「请假申请 / 待办中心」列表点「流程」直接进入本页。
        </div>
        <el-table v-loading="listLoading" :data="myLeaves" empty-text="暂无请假单" border>
          <el-table-column label="单号" width="150">
            <template #default="scope">{{ oaLeaveNo(scope.row) }}</template>
          </el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="scope">{{ leaveTypeText(scope.row.leaveType) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <OaStatusTag :status="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="180">
            <template #default="scope">
              {{ oaDateTime(scope.row.applyTime || scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openTrace(scope.row)">查看流程</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- 流程跟踪详情 -->
    <template v-else>
      <div class="oa-page-title">
        流程跟踪
        <small>请假审批 · 实例 ID：{{ instanceIdText }}</small>
      </div>

      <div v-loading="loading" class="oa-trace__body">
        <el-empty
          v-if="loadError"
          :image-size="90"
          description="未找到该请假单的流程信息，可能已被删除或无权查看"
        >
          <el-button type="primary" @click="backToPicker">返回选择请假单</el-button>
        </el-empty>

        <template v-else-if="leave">
          <!-- 基本信息 -->
          <div class="oa-card">
            <div class="oa-card-head">
              <h3>基本信息</h3>
              <div class="oa-trace__actions">
                <OaStatusTag :status="leave.status" />
                <el-button size="small" icon="Refresh" @click="refresh">刷新</el-button>
                <el-button
                  v-if="canRevoke"
                  v-hasPermi="['oa:leave:submit']"
                  size="small"
                  type="danger"
                  plain
                  icon="RefreshLeft"
                  @click="handleRevoke"
                >撤回</el-button>
              </div>
            </div>
            <table class="oa-desc-table">
              <tbody>
                <tr>
                  <th>流程名称</th>
                  <td>请假审批</td>
                  <th>业务单号</th>
                  <td>{{ oaLeaveNo(leave) }}</td>
                </tr>
                <tr>
                  <th>发起人</th>
                  <td>{{ applicantText }}</td>
                  <th>发起时间</th>
                  <td>{{ oaDateTime(leave.applyTime) }}</td>
                </tr>
                <tr>
                  <th>当前节点</th>
                  <td>{{ currentNodeText }}</td>
                  <th>当前处理人</th>
                  <td>{{ currentHandlerText }}</td>
                </tr>
                <tr>
                  <th>已耗时</th>
                  <td>{{ oaWaitText(elapsedBase) }}</td>
                  <th>流程实例ID</th>
                  <td>{{ instanceIdText }}</td>
                </tr>
                <tr>
                  <th>状态文案</th>
                  <td>{{ leaveStatusText(leave.status) }}</td>
                  <th>请假类型</th>
                  <td>{{ leaveTypeText(leave.leaveType) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="oa-grid-2 oa-trace__grid">
            <!-- 流程图 + 业务参数 -->
            <div class="oa-card">
              <div class="oa-card-head">
                <h3>流程图</h3>
                <span class="oa-muted">绿色为已完成，蓝色为进行中</span>
              </div>
              <OaFlowChart :leave="leave" :records="records" :users="users" />

              <div class="oa-card-head oa-trace__section-head">
                <h3>业务参数</h3>
                <span class="oa-muted">变量取自业务单，与流程启动时传参一致</span>
              </div>
              <el-table :data="flowVariables" border size="small">
                <el-table-column prop="name" label="变量名" width="140" />
                <el-table-column prop="type" label="类型" width="100" />
                <el-table-column prop="value" label="值" />
              </el-table>
            </div>

            <!-- 审批记录 + 抄送人 -->
            <div class="oa-card">
              <div class="oa-card-head">
                <h3>审批记录</h3>
                <span class="oa-muted">最新在前</span>
              </div>
              <OaTimeline :records="records" :users="users" :leave="leave" />

              <div class="oa-card-head oa-trace__section-head">
                <h3>抄送人</h3>
              </div>
              <div class="oa-empty">后端暂未接入抄送（P3）</div>
            </div>
          </div>
        </template>
      </div>
    </template>
  </div>
</template>

<script setup name="Trace">
import useUserStore from '@/store/modules/user'
import { getLeaveTrace, listLeave, revokeLeave } from '@/api/oa/leave'
import { listUser } from '@/api/system/user'
import OaStatusTag from '@/components/oa/OaStatusTag.vue'
import OaFlowChart from '@/components/oa/OaFlowChart.vue'
import OaTimeline from '@/components/oa/OaTimeline.vue'
import {
  leaveStatusText,
  leaveTypeText,
  oaLeaveNo,
  oaDateTime,
  oaWaitText,
  oaUserMap,
  oaUserName,
  oaCurrentNode
} from '@/utils/oa'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 路由 query.leaveId（字符串）；为空表示从菜单直接进入，展示我的请假单选择列表 */
const leaveId = computed(() => {
  const value = route.query.leaveId
  if (value === undefined || value === null || value === '') return ''
  return String(value)
})

const loading = ref(false)
const loadError = ref(false)
// 后端 /oa/leave/trace/{leaveId} 的 data: { leave, processInstanceId, finished, records }
const trace = ref({})
// userId → { nickName, deptName }，用于把 records.assignee 显示成人名
const users = ref({})
const usersLoaded = ref(false)
const listLoading = ref(false)
const myLeaves = ref([])

const leave = computed(() => trace.value.leave || null)
const records = computed(() => trace.value.records || [])
const instanceIdText = computed(() => trace.value.processInstanceId || '未发起')
const currentNode = computed(() => oaCurrentNode(leave.value, records.value))

// 已办结 / 无进行中节点时，当前节点没有意义，按需求显示占位符
const currentNodeText = computed(() => {
  const name = currentNode.value.name
  return name && name !== '已办结' ? name : '—'
})

const currentHandlerText = computed(() => {
  if (!currentNode.value.assignee) return '—'
  return oaUserName(users.value, currentNode.value.assignee, leave.value)
})

const applicantText = computed(() => {
  if (!leave.value) return '—'
  const name = leave.value.applyUserName || '—'
  const dept = leave.value.applyDeptName
  return dept ? `${name}（${dept}）` : name
})

// 已耗时：第一条审批记录的接单时间，未发起时退回申请时间
const elapsedBase = computed(() => {
  const first = records.value[0]
  if (first && first.startTime) return first.startTime
  return leave.value ? leave.value.applyTime : null
})

const canRevoke = computed(() => {
  if (!leave.value || String(leave.value.status) !== '1') return false
  return String(leave.value.applyUserId) === String(userStore.id)
})

function isBlank(value) {
  return value === undefined || value === null || value === ''
}

function displayValue(value) {
  return isBlank(value) ? '—' : value
}

/** 流程变量表格：取值与流程启动时传入的变量保持一致 */
const flowVariables = computed(() => {
  const data = leave.value || {}
  const duration = data.duration
  return [
    { name: 'leaveId', type: 'Long', value: displayValue(data.leaveId) },
    { name: 'applyUserId', type: 'String', value: displayValue(data.applyUserId) },
    { name: 'applyDeptId', type: 'Long', value: displayValue(data.applyDeptId) },
    { name: 'days', type: 'Double', value: isBlank(duration) ? '—' : Number(duration) },
    {
      name: 'leaveType',
      type: 'String',
      value: isBlank(data.leaveType) ? '—' : `${data.leaveType}（${leaveTypeText(data.leaveType)}）`
    }
  ]
})

/** 加载流程跟踪数据 */
async function loadTrace() {
  if (!leaveId.value) return
  loading.value = true
  loadError.value = false
  try {
    const res = await getLeaveTrace(leaveId.value)
    const data = res && res.data
    if (!data || !data.leave) throw new Error('流程数据为空')
    trace.value = data
  } catch (e) {
    // 请假单不存在 / 无权限等：页面降级为 el-empty + 返回
    trace.value = {}
    loadError.value = true
  } finally {
    loading.value = false
  }
}

/** 审批人姓名映射；无权限或接口异常时静默降级（oaUserName 会用申请人姓名兜底） */
async function loadUsers() {
  if (usersLoaded.value) return
  usersLoaded.value = true
  try {
    const res = await listUser({ pageNum: 1, pageSize: 500 })
    users.value = oaUserMap((res && res.rows) || [])
  } catch (e) {
    users.value = {}
  }
}

/** 我的请假单（未带 leaveId 时的入口列表） */
async function loadMyLeaves() {
  listLoading.value = true
  try {
    const res = await listLeave({ applyUserId: userStore.id, pageNum: 1, pageSize: 10 })
    myLeaves.value = (res && res.rows) || []
  } catch (e) {
    myLeaves.value = []
  } finally {
    listLoading.value = false
  }
}

/** 选中一张请假单 → 带 leaveId 重新进入本页 */
function openTrace(row) {
  if (!row || row.leaveId === undefined || row.leaveId === null) return
  router.replace({ path: '/oa/trace', query: { leaveId: row.leaveId } })
}

/** 加载失败时返回选择列表 */
function backToPicker() {
  router.replace({ path: '/oa/trace' })
}

/** 刷新当前流程（同时重试用户姓名映射） */
function refresh() {
  usersLoaded.value = false
  loadTrace()
  loadUsers()
}

/** 撤回：confirm 后调用后端，成功后刷新 */
function handleRevoke() {
  proxy.$modal
    .confirm('撤回后流程将终止且不可恢复，需要重新发起，是否确认撤回？')
    .then(() => revokeLeave(leaveId.value))
    .then(() => {
      proxy.$modal.msgSuccess('撤回成功')
      loadTrace()
    })
    .catch(() => {})
}

watch(
  leaveId,
  id => {
    if (id) {
      loadTrace()
      loadUsers()
    } else {
      loadMyLeaves()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.oa-trace__body {
  min-height: 200px;
}

.oa-trace__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.oa-trace__grid {
  margin-top: 16px;
}

/* 全局 .oa-card + .oa-card 的 16px 间距在 grid 内会错位，靠 gap 控制对齐 */
.oa-trace__grid > .oa-card + .oa-card {
  margin-top: 0;
}

.oa-trace__section-head {
  margin-top: 20px;
}

.oa-trace__tip {
  margin: -4px 0 12px;
}
</style>
