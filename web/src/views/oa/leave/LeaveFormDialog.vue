<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="640px"
    append-to-body
    :close-on-click-modal="false"
    @closed="reset"
  >
    <el-form ref="leaveFormRef" :model="form" :rules="rules" label-width="88px" :disabled="readonly">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="申请人">
            <el-input :model-value="form.applyUserName" placeholder="—" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属部门">
            <el-input :model-value="deptName" placeholder="—" disabled />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="请假类型" prop="leaveType">
            <el-select v-model="form.leaveType" placeholder="请选择请假类型" clearable style="width: 100%">
              <el-option
                v-for="dict in oa_leave_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="请假时长">
            <div class="duration-field">
              <el-input :model-value="durationText" disabled />
              <span class="oa-muted">自动计算（按自然日，0.5 天粒度）</span>
            </div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择开始时间"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择结束时间"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="请假事由" prop="reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请填写请假事由，如工作交接说明等"
        />
      </el-form-item>

      <el-form-item label="附件">
        <div class="upload-field">
          <file-upload
            v-model="form.attachmentUrl"
            :limit="1"
            :fileSize="10"
            :fileType="['jpg', 'jpeg', 'png', 'pdf']"
            :disabled="readonly"
          />
          <div class="oa-muted">支持 jpg / png / pdf，单个不超过 10MB；病假等需上传证明材料</div>
        </div>
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" placeholder="选填" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <template v-if="readonly">
          <el-button type="primary" @click="visible = false">关 闭</el-button>
        </template>
        <template v-else>
          <el-button :disabled="saving" @click="visible = false">取 消</el-button>
          <el-button :loading="saving" @click="handleSaveDraft">存草稿</el-button>
          <el-button type="primary" :loading="saving" @click="handleSubmitApproval">提交审批</el-button>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="LeaveFormDialog">
import { addLeave, updateLeave, submitLeave, listLeave } from '@/api/oa/leave'
import { getUser, getUserProfile } from '@/api/system/user'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const { oa_leave_type } = useDict('oa_leave_type')
const emit = defineEmits(['success'])

const visible = ref(false)
const saving = ref(false)
const leaveId = ref(undefined)
// add 新增 / edit 编辑 / view 只读
const mode = ref('add')
const leaveFormRef = ref()
const deptName = ref('—')

const form = ref(createForm())
const readonly = computed(() => mode.value === 'view')
const dialogTitle = computed(() => {
  if (readonly.value) return '请假申请详情'
  return leaveId.value ? '编辑请假申请' : '新增请假申请'
})

/**
 * 自动计算请假时长（按自然日，0.5 天粒度）
 *
 * 口径与原型示例保持一致：2026-10-30 09:00 ~ 2026-10-31 18:00 = 2.0 天
 *  - 同一天 09:00~18:00 = 1.0 天；13:00~18:00 = 0.5 天
 *  - 下午（>=13:00）开始扣 0.5 天；上午（<=12:00）结束扣 0.5 天
 *  - 最小 0.5 天
 */
const durationValue = computed(() => {
  const start = parseDateTime(form.value.startTime)
  const end = parseDateTime(form.value.endTime)
  if (!start || !end || end.getTime() <= start.getTime()) return null
  const startDay = new Date(start.getFullYear(), start.getMonth(), start.getDate())
  const endDay = new Date(end.getFullYear(), end.getMonth(), end.getDate())
  let days = Math.round((endDay.getTime() - startDay.getTime()) / (24 * 60 * 60 * 1000)) + 1
  if (start.getHours() * 60 + start.getMinutes() >= 13 * 60) days -= 0.5
  if (end.getHours() * 60 + end.getMinutes() <= 12 * 60) days -= 0.5
  return Math.max(0.5, Math.round(days * 2) / 2)
})

const durationText = computed(() => {
  return durationValue.value === null ? '—' : durationValue.value.toFixed(1) + ' 天'
})

const rules = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' },
    { validator: validateTime, trigger: 'change' }
  ],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    { validator: validateTime, trigger: 'change' }
  ],
  reason: [{ required: true, message: '请填写请假事由', trigger: 'blur' }]
}

/** 表单默认值 */
function createForm() {
  return {
    applyUserId: userStore.id,
    applyUserName: userStore.nickName || userStore.name || '',
    applyDeptId: undefined,
    leaveType: undefined,
    startTime: undefined,
    endTime: undefined,
    reason: undefined,
    attachmentUrl: '',
    remark: undefined
  }
}

/** 统一解析后端 yyyy-MM-dd HH:mm:ss（兼容部分浏览器解析差异） */
function parseDateTime(value) {
  if (!value) return null
  const date = new Date(String(value).replace(/-/g, '/'))
  return isNaN(date.getTime()) ? null : date
}

/** 结束时间必须晚于开始时间 */
function validateTime(rule, value, callback) {
  const start = parseDateTime(form.value.startTime)
  const end = parseDateTime(form.value.endTime)
  if (!start || !end) {
    return callback()
  }
  if (end.getTime() <= start.getTime()) {
    return callback(new Error('结束时间必须大于开始时间'))
  }
  callback()
}

watch(
  () => form.value.startTime,
  () => {
    if (form.value.endTime && leaveFormRef.value) {
      leaveFormRef.value.validateField('endTime').catch(() => {})
    }
  }
)

/** 打开弹窗：mode = add / edit / view，data 为列表行 */
function open(options = {}) {
  const opts = options || {}
  mode.value = opts.mode || 'add'
  reset()
  const data = opts.data
  if (data) {
    leaveId.value = data.leaveId
    form.value.applyUserId = data.applyUserId || userStore.id
    form.value.applyUserName = data.applyUserName || userStore.nickName || ''
    form.value.applyDeptId = data.applyDeptId
    form.value.leaveType = data.leaveType
    form.value.startTime = data.startTime
    form.value.endTime = data.endTime
    form.value.reason = data.reason
    form.value.attachmentUrl = data.attachmentUrl || ''
    form.value.remark = data.remark
    deptName.value = data.applyDeptName || '—'
  }
  visible.value = true
  nextTick(() => {
    leaveFormRef.value && leaveFormRef.value.clearValidate()
  })
  if (!deptName.value || deptName.value === '—') {
    loadCurrentUserDept()
  }
}

/**
 * 取当前登录人的部门名（只读展示用，失败不影响填单）
 * 优先走 getUser；没有 system:user:query 权限时用个人中心接口兜底
 */
function loadCurrentUserDept() {
  const userId = userStore.id
  if (!userId) return
  const loadProfile = () => {
    getUserProfile()
      .then(response => applyUserInfo(response.data))
      .catch(() => {})
  }
  if (!proxy.$auth.hasPermi('system:user:query')) {
    loadProfile()
    return
  }
  getUser(userId)
    .then(response => applyUserInfo(response.data))
    .catch(() => loadProfile())
}

function applyUserInfo(user) {
  if (!user) return
  const dept = user.dept || {}
  if (dept.deptName) {
    deptName.value = dept.deptName
  }
  const deptId = user.deptId || dept.deptId
  if (deptId && !form.value.applyDeptId) {
    form.value.applyDeptId = deptId
  }
}

/** 重置表单 */
function reset() {
  leaveId.value = undefined
  deptName.value = '—'
  form.value = createForm()
  if (leaveFormRef.value) {
    leaveFormRef.value.clearValidate()
  }
}

/** 组装提交后端的 OaLeave 对象 */
function buildPayload() {
  const payload = {
    applyUserId: form.value.applyUserId || userStore.id,
    applyUserName: form.value.applyUserName || userStore.nickName || '',
    applyDeptId: form.value.applyDeptId,
    leaveType: form.value.leaveType,
    startTime: form.value.startTime,
    endTime: form.value.endTime,
    duration: durationValue.value,
    reason: form.value.reason,
    attachmentUrl: form.value.attachmentUrl || '',
    remark: form.value.remark || ''
  }
  if (leaveId.value) {
    payload.leaveId = leaveId.value
  }
  return payload
}

/** 新增后按"当前用户最新一条"回查 leaveId（add 接口不返回主键） */
function queryLatestLeaveId() {
  return listLeave({ applyUserId: userStore.id, pageNum: 1, pageSize: 1 })
    .then(response => {
      const rows = (response && response.rows) || []
      return rows.length ? rows[0].leaveId : undefined
    })
    .catch(() => undefined)
}

/** 保存草稿 */
function handleSaveDraft() {
  submitForm(false)
}

/** 提交审批（先保存，再走流程） */
function handleSubmitApproval() {
  submitForm(true)
}

async function submitForm(submitAfter) {
  if (readonly.value || saving.value) return

  let valid = false
  try {
    valid = await leaveFormRef.value.validate()
  } catch (e) {
    valid = false
  }
  if (!valid) return

  if (durationValue.value === null) {
    proxy.$modal.msgError('开始时间与结束时间无效，无法计算请假时长')
    return
  }

  saving.value = true
  try {
    const payload = buildPayload()
    if (leaveId.value) {
      await updateLeave(payload)
    } else {
      await addLeave(payload)
      // add 接口不返回主键：回查当前用户最新一条，拿到 id 后提交/重试都复用它，避免重复建单
      const newLeaveId = await queryLatestLeaveId()
      if (newLeaveId) {
        leaveId.value = newLeaveId
      }
    }

    if (submitAfter) {
      const targetId = leaveId.value
      if (!targetId) {
        proxy.$modal.msgError('保存成功，但未获取到请假单号，请到列表中提交审批')
      } else {
        await submitLeave(targetId)
        proxy.$modal.msgSuccess('提交成功')
      }
    } else {
      proxy.$modal.msgSuccess('保存成功')
    }

    visible.value = false
    emit('success')
  } catch (e) {
    // 接口异常已由 request 拦截器统一提示
  } finally {
    saving.value = false
  }
}

defineExpose({ open, reset })
</script>

<style scoped lang="scss">
.duration-field {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;

  .el-input {
    flex: 1;
    min-width: 0;
  }
}

.upload-field {
  width: 100%;
}
</style>
