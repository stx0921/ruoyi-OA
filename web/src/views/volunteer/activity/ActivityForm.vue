<template>
  <el-drawer
    v-model="visible"
    size="620px"
    append-to-body
    :before-close="handleBeforeClose"
    class="va-form-drawer"
    @closed="handleClosed"
  >
    <template #header>
      <div class="va-form-head">
        <span class="va-form-title">{{ drawerTitle }}</span>
        <span v-if="derivedStatus" class="oa-pill" :class="'oa-pill--' + derivedStatus.tone">
          {{ derivedStatus.label }}
        </span>
      </div>
    </template>

    <div v-loading="loading" class="va-form-body">
      <!-- 加载失败 -->
      <div v-if="loadError" class="va-form-error">
        <div class="va-form-error__title">活动详情加载失败</div>
        <div class="va-form-error__actions">
          <el-button type="primary" plain @click="loadActivity">重试</el-button>
          <el-button @click="visible = false">关 闭</el-button>
        </div>
      </div>

      <template v-else>
        <!-- 编辑提示：时长影响历史统计 -->
        <div v-if="isEdit" class="va-notice">
          <b>编辑提示：</b>修改志愿时长会影响历史统计口径——统计总时长按活动<b>当前</b>志愿时长实时计算，保存后统计卡片与图表会同步变化。
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="va-form">
          <el-form-item label="活动名称" prop="name">
            <el-input
              v-model="form.name"
              maxlength="100"
              show-word-limit
              placeholder="例如：校运会志愿"
            />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="活动地点" prop="location">
                <el-input v-model="form.location" maxlength="100" placeholder="例如：田径运动场" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="活动人数（参考值，不做硬上限）" prop="capacity">
                <el-input-number
                  v-model="form.capacity"
                  :min="1"
                  :max="9999"
                  :step="1"
                  :precision="0"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="志愿时长（小时，0.5 步进）" prop="durationHours">
                <el-input-number
                  v-model="form.durationHours"
                  :min="0.5"
                  :max="99.9"
                  :step="0.5"
                  :precision="1"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态" prop="status">
                <el-select v-if="isEdit" v-model="form.status" style="width: 100%">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
                <el-input v-else model-value="正常（新增默认正常，可保存后取消）" disabled />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="开始时间" prop="startTime">
                <el-date-picker
                  v-model="form.startTime"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="请选择开始时间"
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
                  placeholder="请选择结束时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="报名截止时间" prop="signupDeadline">
                <el-date-picker
                  v-model="form.signupDeadline"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="请选择报名截止时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="自动推导状态">
                <div class="va-derived">
                  <span v-if="derivedStatus" class="oa-pill" :class="'oa-pill--' + derivedStatus.tone">
                    {{ derivedStatus.label }}
                  </span>
                  <span v-else class="oa-muted">—（请先填写时间）</span>
                  <div class="oa-muted va-derived-tip">由「报名截止 / 开始 / 结束」时间自动推导</div>
                </div>
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="活动详情" prop="content">
                <el-input
                  v-model="form.content"
                  type="textarea"
                  :rows="6"
                  maxlength="5000"
                  show-word-limit
                  placeholder="活动内容、集合方式、注意事项等（10–5000 字）"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </template>
    </div>

    <template #footer>
      <div class="va-form-foot">
        <el-button :disabled="submitting" @click="handleBeforeClose()">取 消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="loading || loadError"
          @click="handleSave"
        >保 存</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup name="VolunteerActivityForm">
import { ElMessageBox } from 'element-plus'
import {
  getActivity,
  addActivity,
  updateActivity,
  ACTIVITY_STATUS_META,
  SHOW_STATUS_META
} from '@/api/oa/volunteer'

/**
 * 志愿活动新增 / 编辑（620px 抽屉，契约 §4.2）
 * - 字段：名称 / 地点 / 人数 / 时长（0.5 步进）/ 开始 / 结束 / 报名截止 / 详情 / 状态
 * - 校验：结束 > 开始、报名截止 ≤ 开始；详情 10–5000 字
 * - 编辑修改「志愿时长」保存前二次确认（影响历史统计口径）
 */
const emit = defineEmits(['success'])
const { proxy } = getCurrentInstance()

const visible = ref(false)
const loading = ref(false)
const loadError = ref(false)
const submitting = ref(false)
const mode = ref('add')
const activityId = ref(undefined)
const formRef = ref()
const detail = ref({})
const originalSnapshot = ref('')
const originalDuration = ref(null)

const isEdit = computed(() => mode.value === 'edit')
const drawerTitle = computed(() => {
  if (!isEdit.value) return '新增活动'
  const name = detail.value && detail.value.name
  return name ? '编辑活动 · ' + name : '编辑活动'
})

const statusOptions = Object.keys(ACTIVITY_STATUS_META).map(value => ({
  value,
  label: ACTIVITY_STATUS_META[value].label
}))

const form = reactive(createForm())

/* -------------------------------------------------------------------------- */
/* 校验                                                                       */
/* -------------------------------------------------------------------------- */
/** yyyy-MM-dd HH:mm:ss → 时间戳（无效返回 0） */
function toTimestamp(value) {
  if (!value) return 0
  const date = new Date(String(value).replace(/-/g, '/'))
  return Number.isNaN(date.getTime()) ? 0 : date.getTime()
}

const rules = {
  name: [
    { required: true, message: '请填写活动名称', trigger: 'blur' },
    { validator: (rule, value, callback) => validateLength(value, 2, 100, '活动名称', callback), trigger: 'blur' }
  ],
  location: [
    { required: true, message: '请填写活动地点', trigger: 'blur' },
    { validator: (rule, value, callback) => validateLength(value, 2, 100, '活动地点', callback), trigger: 'blur' }
  ],
  capacity: [{ validator: validateCapacity, trigger: 'change' }],
  durationHours: [{ validator: validateDuration, trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    { validator: validateEndTime, trigger: 'change' }
  ],
  signupDeadline: [
    { required: true, message: '请选择报名截止时间', trigger: 'change' },
    { validator: validateDeadline, trigger: 'change' }
  ],
  content: [
    { required: true, message: '请填写活动详情', trigger: 'blur' },
    { validator: (rule, value, callback) => validateLength(value, 10, 5000, '活动详情', callback), trigger: 'blur' }
  ]
}

function validateLength(value, min, max, label, callback) {
  const text = String(value || '').trim()
  if (text.length < min) return callback(new Error(label + '至少 ' + min + ' 字（当前 ' + text.length + ' 字）'))
  if (text.length > max) return callback(new Error(label + '最多 ' + max + ' 字'))
  callback()
}

function validateCapacity(rule, value, callback) {
  const num = Number(value)
  if (!Number.isFinite(num) || !Number.isInteger(num) || num < 1 || num > 9999) {
    return callback(new Error('活动人数需为 1–9999 的整数'))
  }
  callback()
}

function validateDuration(rule, value, callback) {
  const num = Number(value)
  if (!Number.isFinite(num) || num < 0.5 || num > 99.9) {
    return callback(new Error('志愿时长需为 0.5–99.9 小时'))
  }
  const doubled = num * 2
  if (Math.abs(doubled - Math.round(doubled)) > 1e-6) {
    return callback(new Error('志愿时长需按 0.5 小时步进'))
  }
  callback()
}

function validateEndTime(rule, value, callback) {
  const start = toTimestamp(form.startTime)
  const end = toTimestamp(value)
  if (!start || !end) return callback()
  if (end <= start) return callback(new Error('结束时间必须晚于开始时间'))
  callback()
}

function validateDeadline(rule, value, callback) {
  const start = toTimestamp(form.startTime)
  const deadline = toTimestamp(value)
  if (!start || !deadline) return callback()
  if (deadline > start) return callback(new Error('报名截止时间不能晚于开始时间'))
  callback()
}

watch(
  () => form.startTime,
  () => {
    if (formRef.value) {
      if (form.endTime) formRef.value.validateField('endTime').catch(() => {})
      if (form.signupDeadline) formRef.value.validateField('signupDeadline').catch(() => {})
    }
  }
)

/* -------------------------------------------------------------------------- */
/* 派生状态                                                                   */
/* -------------------------------------------------------------------------- */
const derivedStatus = computed(() => {
  if (!form.startTime || !form.endTime || !form.signupDeadline) return null
  if (String(form.status) === '1') return SHOW_STATUS_META['3']
  const now = Date.now()
  const start = toTimestamp(form.startTime)
  const end = toTimestamp(form.endTime)
  const deadline = toTimestamp(form.signupDeadline)
  if (!start || !end || !deadline) return null
  if (now > end) return SHOW_STATUS_META['2']
  if (now >= start && now <= end) return SHOW_STATUS_META['1']
  if (now < start && now <= deadline) return SHOW_STATUS_META['0']
  return SHOW_STATUS_META['4']
})

/* -------------------------------------------------------------------------- */
/* 打开 / 关闭                                                                */
/* -------------------------------------------------------------------------- */
function open(options) {
  const opts = options || {}
  mode.value = opts.mode === 'edit' ? 'edit' : 'add'
  resetState()
  visible.value = true
  if (mode.value === 'edit' && opts.activityId) {
    activityId.value = opts.activityId
    loadActivity()
  } else {
    initAdd()
  }
}

function createForm() {
  return {
    name: '',
    location: '',
    capacity: 30,
    durationHours: 2,
    startTime: '',
    endTime: '',
    signupDeadline: '',
    content: '',
    status: '0'
  }
}

function initAdd() {
  const times = defaultTimes()
  form.name = ''
  form.location = ''
  form.capacity = 30
  form.durationHours = 2
  form.startTime = times.start
  form.endTime = times.end
  form.signupDeadline = times.deadline
  form.content = ''
  form.status = '0'
  nextTick(() => {
    originalSnapshot.value = snapshot()
    formRef.value && formRef.value.clearValidate()
  })
}

function defaultTimes() {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 9, 0, 0)
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 12, 0, 0)
  const deadline = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 8, 0, 0)
  return {
    start: formatDateTime(start),
    end: formatDateTime(end),
    deadline: formatDateTime(deadline)
  }
}

function formatDateTime(date) {
  const pad = value => (value < 10 ? '0' + value : String(value))
  return (
    date.getFullYear() +
    '-' +
    pad(date.getMonth() + 1) +
    '-' +
    pad(date.getDate()) +
    ' ' +
    pad(date.getHours()) +
    ':' +
    pad(date.getMinutes()) +
    ':00'
  )
}

async function loadActivity() {
  if (!activityId.value) return
  loading.value = true
  loadError.value = false
  try {
    const response = await getActivity(activityId.value)
    const data = (response && response.data) || {}
    detail.value = data
    form.name = data.name || ''
    form.location = data.location || ''
    form.capacity = data.capacity !== undefined && data.capacity !== null ? Number(data.capacity) : 30
    form.durationHours = data.durationHours !== undefined && data.durationHours !== null ? Number(data.durationHours) : 0.5
    form.startTime = data.startTime || ''
    form.endTime = data.endTime || ''
    form.signupDeadline = data.signupDeadline || ''
    form.content = data.content || ''
    form.status = data.status === undefined || data.status === null ? '0' : String(data.status)
    originalDuration.value = Number(form.durationHours)
    await nextTick()
    originalSnapshot.value = snapshot()
    formRef.value && formRef.value.clearValidate()
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function resetState() {
  form.name = ''
  form.location = ''
  form.capacity = 30
  form.durationHours = 2
  form.startTime = ''
  form.endTime = ''
  form.signupDeadline = ''
  form.content = ''
  form.status = '0'
  detail.value = {}
  activityId.value = undefined
  loading.value = false
  loadError.value = false
  submitting.value = false
  originalDuration.value = null
  originalSnapshot.value = ''
}

function snapshot() {
  return JSON.stringify({
    name: String(form.name || '').trim(),
    location: String(form.location || '').trim(),
    capacity: form.capacity,
    durationHours: form.durationHours,
    startTime: form.startTime,
    endTime: form.endTime,
    signupDeadline: form.signupDeadline,
    content: String(form.content || '').trim(),
    status: form.status
  })
}

const dirty = computed(() => {
  if (!visible.value || loading.value || loadError.value) return false
  return snapshot() !== originalSnapshot.value
})

/** 有未保存修改时二次确认 */
function handleBeforeClose(done) {
  if (submitting.value) return
  const close = typeof done === 'function' ? done : () => (visible.value = false)
  if (!dirty.value) {
    close()
    return
  }
  ElMessageBox.confirm('有未保存的修改，确认放弃？', '提示', {
    confirmButtonText: '放弃修改',
    cancelButtonText: '继 续 编 辑',
    type: 'warning'
  })
    .then(close)
    .catch(() => {})
}

function handleClosed() {
  resetState()
}

/* -------------------------------------------------------------------------- */
/* 保存                                                                       */
/* -------------------------------------------------------------------------- */
async function handleSave() {
  if (submitting.value || loading.value) return
  let valid = false
  try {
    valid = await formRef.value.validate()
  } catch (e) {
    valid = false
  }
  if (!valid) {
    proxy.$modal.msgWarning('还有必填项未填写，请检查红字提示')
    return
  }

  // 编辑修改志愿时长：影响历史统计口径，二次确认
  if (isEdit.value && durationChanged()) {
    try {
      await ElMessageBox.confirm('修改志愿时长会同步影响历史统计口径，确定保存？', '提示', {
        confirmButtonText: '确定保存',
        cancelButtonText: '取 消',
        type: 'warning'
      })
    } catch (e) {
      return
    }
  }

  submitting.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value) {
      await updateActivity(payload)
      proxy.$modal.msgSuccess('已保存活动：' + payload.name + '（统计已按当前志愿时长实时更新）')
    } else {
      const response = await addActivity(payload)
      const activityNo = extractActivityNo(response)
      proxy.$modal.msgSuccess('已新增活动：' + payload.name + (activityNo ? '（' + activityNo + '）' : ''))
    }
    visible.value = false
    emit('success')
  } catch (e) {
    // 失败原因由 request 拦截器统一提示，保留输入
  } finally {
    submitting.value = false
  }
}

function buildPayload() {
  const payload = {
    name: String(form.name || '').trim(),
    location: String(form.location || '').trim(),
    capacity: Number(form.capacity),
    durationHours: Number(form.durationHours),
    startTime: form.startTime,
    endTime: form.endTime,
    signupDeadline: form.signupDeadline,
    content: String(form.content || '').trim(),
    status: String(form.status || '0')
  }
  if (isEdit.value) payload.activityId = activityId.value
  return payload
}

function durationChanged() {
  if (originalDuration.value === null) return false
  return Math.abs(Number(form.durationHours) - Number(originalDuration.value)) > 1e-6
}

function extractActivityNo(response) {
  const data = response && response.data
  if (!data) return ''
  if (typeof data === 'string') return data
  if (typeof data === 'object') return data.activityNo || data.no || ''
  return ''
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.va-form-drawer :deep(.el-drawer__body) {
  padding-top: 16px;
}

.va-form-head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.va-form-title {
  font-size: 16px;
  font-weight: 650;
  color: var(--ink);
}

.va-form-body {
  min-height: 240px;
}

.va-notice {
  margin-bottom: 14px;
  padding: 9px 12px;
  border: 1px solid var(--warn-soft);
  border-radius: var(--r-xs);
  background: var(--warn-soft);
  color: var(--warn-text);
  font-size: 13px;
  line-height: 1.7;

  b {
    color: var(--warn-text);
  }
}

.va-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 6px;
    font-size: 13px;
    color: var(--ink-muted);
  }
}

.va-derived {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-height: 34px;
  width: 100%;
}

.va-derived-tip {
  flex: 1 0 100%;
  font-size: 12px;
  line-height: 1.5;
}

.va-form-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.va-form-error {
  padding: 48px 0;
  text-align: center;
}

.va-form-error__title {
  font-size: 14px;
  color: var(--ink-muted);
}

.va-form-error__actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
}
</style>
