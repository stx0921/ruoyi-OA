<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="620px"
    append-to-body
    class="oa-audit-drawer"
    @closed="reset"
  >
    <div v-loading="traceLoading" class="oa-audit-drawer__body">
      <template v-if="leave">
        <!-- 流程进度 -->
        <div class="oa-audit-drawer__section">
          <div class="oa-card-head">
            <h3>审批进度</h3>
          </div>
          <OaFlowChart :leave="leave" :records="records" :users="users" />
        </div>

        <!-- 单据信息 -->
        <div class="oa-audit-drawer__section">
          <div class="oa-card-head">
            <h3>单据信息</h3>
          </div>
          <table class="oa-desc-table">
            <tbody>
              <tr>
                <th>申请单号</th>
                <td>{{ oaLeaveNo(leave) }}</td>
                <th>申请人</th>
                <td>{{ leave.applyUserName || '—' }}</td>
              </tr>
              <tr>
                <th>申请部门</th>
                <td>{{ leave.applyDeptName || '—' }}</td>
                <th>请假类型</th>
                <td>{{ leaveTypeText(leave.leaveType, oa_leave_type) }}</td>
              </tr>
              <tr>
                <th>请假时长</th>
                <td>{{ oaDurationText(leave.duration) }}</td>
                <th>当前状态</th>
                <td><OaStatusTag :status="leave.status" /></td>
              </tr>
              <tr>
                <th>开始时间</th>
                <td>{{ oaDateTime(leave.startTime) }}</td>
                <th>结束时间</th>
                <td>{{ oaDateTime(leave.endTime) }}</td>
              </tr>
              <tr>
                <th>请假事由</th>
                <td colspan="3">{{ leave.reason || '—' }}</td>
              </tr>
              <tr>
                <th>附件</th>
                <td colspan="3">
                  <el-link
                    v-if="leave.attachmentUrl"
                    type="primary"
                    :href="leave.attachmentUrl"
                    target="_blank"
                  >{{ attachmentName }}</el-link>
                  <span v-else class="oa-muted">无</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 审批记录 -->
        <div class="oa-audit-drawer__section">
          <div class="oa-card-head">
            <h3>审批记录</h3>
          </div>
          <OaTimeline :records="records" :users="users" :leave="leave" />
        </div>

        <!-- 审批意见（只读查看时不展示） -->
        <div v-if="!readonly" class="oa-audit-drawer__form">
          <div class="oa-audit-drawer__label">
            审批意见
            <span class="oa-audit-drawer__required">*</span>
            <span class="oa-muted">驳回时必填</span>
          </div>
          <el-input
            v-model="comment"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入审批意见（驳回时必填）"
          />
        </div>
      </template>

      <div v-else-if="!traceLoading" class="oa-empty">
        {{ traceError || '暂无单据信息' }}
      </div>
    </div>

    <template #footer>
      <div class="oa-audit-drawer__footer">
        <el-button @click="visible = false">{{ readonly ? '关闭' : '取消' }}</el-button>
        <template v-if="!readonly">
          <el-button
            type="danger"
            plain
            :loading="rejecting"
            :disabled="!canHandle"
            v-hasPermi="['oa:leave:approve']"
            @click="handleReject"
          >驳回</el-button>
          <el-button
            type="primary"
            :loading="approving"
            :disabled="!canHandle"
            v-hasPermi="['oa:leave:approve']"
            @click="handleApprove"
          >同意</el-button>
        </template>
      </div>
    </template>
  </el-drawer>
</template>

<script setup name="AuditDrawer">
import { getLeaveTrace, approveLeave, rejectLeave } from '@/api/oa/leave'
import {
  leaveTypeText,
  oaDateTime,
  oaDurationText,
  oaLeaveNo
} from '@/utils/oa'
import OaStatusTag from '@/components/oa/OaStatusTag.vue'
import OaFlowChart from '@/components/oa/OaFlowChart.vue'
import OaTimeline from '@/components/oa/OaTimeline.vue'

const props = defineProps({
  // 抽屉显隐（v-model）
  modelValue: { type: Boolean, default: false },
  // 当前行：待办任务（含 taskId/leaveId）或请假单（只读查看）
  task: { type: Object, default: null },
  // 只读查看：隐藏审批意见与操作按钮
  readonly: { type: Boolean, default: false },
  // userId → { nickName } 映射
  users: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'refresh'])

const { proxy } = getCurrentInstance()
const { oa_leave_type } = useDict('oa_leave_type')

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const drawerTitle = computed(() => (props.readonly ? '查看 · 请假申请' : '审批 · 请假申请'))

const leaveId = computed(() => (props.task && props.task.leaveId) || null)
// 只有存在真实待办 taskId 时才允许同意 / 驳回
const canHandle = computed(() => !!(props.task && props.task.taskId && leaveId.value))

const traceLeave = ref(null)
const records = ref([])
const traceLoading = ref(false)
const traceError = ref('')
const comment = ref('')
const approving = ref(false)
const rejecting = ref(false)

const leave = computed(() => traceLeave.value)

const attachmentName = computed(() => {
  const url = (traceLeave.value && traceLeave.value.attachmentUrl) || ''
  if (!url) return ''
  const name = url.split('/').pop() || url
  try {
    return decodeURIComponent(name) || '查看附件'
  } catch (e) {
    return name
  }
})

/** 抽屉打开时自取流程轨迹；关闭动画结束后在 @closed 里清理状态 */
watch(
  () => props.modelValue,
  value => {
    if (value) {
      loadTrace()
    }
  }
)

function reset() {
  traceLeave.value = null
  records.value = []
  traceError.value = ''
  comment.value = ''
  approving.value = false
  rejecting.value = false
  traceLoading.value = false
}

async function loadTrace() {
  reset()
  if (!leaveId.value) {
    traceError.value = '该任务未关联请假单（非请假流程），无法加载审批信息'
    return
  }
  traceLoading.value = true
  try {
    const res = await getLeaveTrace(leaveId.value)
    const data = res.data || {}
    traceLeave.value = data.leave || null
    records.value = data.records || []
    if (!traceLeave.value) {
      traceError.value = '未查询到请假单信息'
    }
  } catch (e) {
    traceError.value = '流程轨迹加载失败，请稍后重试'
  } finally {
    traceLoading.value = false
  }
}

/** 同意：审批意见非必填 */
function handleApprove() {
  if (!canHandle.value) return
  approving.value = true
  approveLeave({ taskId: props.task.taskId, comment: comment.value.trim() })
    .then(() => {
      proxy.$modal.msgSuccess('审批通过')
      visible.value = false
      emit('refresh')
    })
    .catch(() => {})
    .finally(() => {
      approving.value = false
    })
}

/** 驳回：审批意见必填（后端同样校验） */
function handleReject() {
  if (!canHandle.value) return
  const text = comment.value.trim()
  if (!text) {
    proxy.$modal.msgWarning('驳回时必须填写审批意见')
    return
  }
  proxy.$modal
    .confirm('确认驳回该请假申请吗？')
    .then(() => {
      rejecting.value = true
      return rejectLeave({ taskId: props.task.taskId, comment: text })
    })
    .then(() => {
      proxy.$modal.msgSuccess('已驳回该请假申请')
      visible.value = false
      emit('refresh')
    })
    .catch(() => {})
    .finally(() => {
      rejecting.value = false
    })
}
</script>

<style scoped>
.oa-audit-drawer__body {
  min-height: 200px;
}

.oa-audit-drawer__section {
  margin-bottom: 22px;
}

.oa-audit-drawer__section :deep(.oa-card-head) {
  margin-bottom: 10px;
}

.oa-audit-drawer__form {
  padding-top: 4px;
}

.oa-audit-drawer__label {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.oa-audit-drawer__required {
  color: #f56c6c;
}

.oa-audit-drawer__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
