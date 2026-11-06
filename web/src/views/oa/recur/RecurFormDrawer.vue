<template>
  <el-drawer
    v-model="visible"
    :title="mode === 'edit' ? '编辑周期规则' : '新建周期规则'"
    size="620px"
    append-to-body
    :close-on-click-modal="false"
    @closed="reset"
  >
    <div v-loading="pageLoading" class="recur-form">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px" label-position="top">
        <el-form-item label="规则名称" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="例如：{date} 周报提交" />
          <div class="title-tip">
            支持 <code>{date}</code> 占位，生成时替换为 MM-DD；其他花括号写法将被拒绝
            <div class="title-preview" v-if="titlePreview">示例：{{ titlePreview }}</div>
          </div>
        </el-form-item>

        <el-form-item label="规则描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="2000"
            show-word-limit
            placeholder="描述规则内容与要求（≤2000 字）"
          />
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="所属项目" prop="projectId">
              <el-select v-model="form.projectId" placeholder="不挂项目" clearable filterable style="width: 100%">
                <el-option v-for="project in projectOptions" :key="project.projectId" :label="project.name" :value="project.projectId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主责人" prop="assigneeId">
              <el-select
                v-model="form.assigneeId"
                placeholder="搜索姓名 / 学号"
                filterable
                remote
                clearable
                :remote-method="searchAssignee"
                :loading="assigneeLoading"
                style="width: 100%"
              >
                <el-option v-for="item in assigneeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="每周几" prop="weekDays">
          <div class="weekday-picker">
            <button
              v-for="item in WEEK_DAY_OPTIONS"
              :key="item.value"
              type="button"
              class="weekday-badge"
              :class="{ 'is-active': form.weekDays.includes(item.value) }"
              @click="toggleWeekDay(item.value)"
            >{{ item.short }}</button>
            <span class="oa-muted">至少选择一天</span>
          </div>
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="截止时刻" prop="dueTime">
              <el-time-select
                v-model="form.dueTime"
                start="00:00"
                step="00:15"
                end="23:45"
                placeholder="选择时刻"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生效日期" prop="startDate">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择生效日期"
                :disabled-date="disabledPastDate"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签（最多 3 个）">
              <el-select v-model="form.tags" multiple clearable :multiple-limit="3" style="width: 100%" placeholder="选择标签">
                <el-option v-for="item in tagOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-button :disabled="saving" @click="visible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
      </div>
    </template>

    <recur-scope-dialog ref="scopeDialogRef" @confirm="handleScopeConfirm" />
  </el-drawer>
</template>

<script setup name="RecurFormDrawer">
import RecurScopeDialog from './RecurScopeDialog.vue'
import { addRecur, updateRecur } from '@/api/oa/recur'
import { listProject } from '@/api/oa/project'
import {
  PRIORITY_OPTIONS,
  TASK_TAG_FALLBACK,
  WEEK_DAY_OPTIONS,
  formatDate,
  normalizeWeekDays,
  resolveOptions,
  searchMemberOptions,
  toDateString,
  todayStr,
  tomorrowStr
} from '@/utils/oaTask'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['success'])
const { oa_task_tag, oa_task_priority } = useDict('oa_task_tag', 'oa_task_priority')

const priorityOptions = computed(() => resolveOptions(oa_task_priority.value, PRIORITY_OPTIONS))

const visible = ref(false)
const saving = ref(false)
const pageLoading = ref(false)
const mode = ref('add')
const recurId = ref(undefined)
const formRef = ref()
const scopeDialogRef = ref()

const projectOptions = ref([])
const assigneeOptions = ref([])
const assigneeLoading = ref(false)
const editingData = ref({})

const tagOptions = computed(() => {
  const dict = oa_task_tag && oa_task_tag.value ? oa_task_tag.value : []
  if (dict.length) return dict.map(item => ({ value: item.label, label: item.label }))
  return Object.keys(TASK_TAG_FALLBACK).map(key => ({ value: TASK_TAG_FALLBACK[key], label: TASK_TAG_FALLBACK[key] }))
})

/** 标题预览：{date} → 今天 MM-DD */
const titlePreview = computed(() => {
  const title = String(form.value.title || '')
  if (!title.includes('{date}')) return ''
  return title.replace(/\{date\}/g, parseTime(new Date(), '{m}-{d}'))
})

const form = ref(createForm())

const rules = {
  title: [
    { required: true, message: '请填写规则名称', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        const text = String(value || '').trim()
        if (!text) return callback(new Error('请填写规则名称'))
        if (text.length < 2 || text.length > 200) return callback(new Error('规则名称需为 2–200 个字符'))
        const rest = text.replace(/\{date\}/g, '')
        if (/[{}]/.test(rest)) return callback(new Error('仅支持 {date} 占位，生成时替换为 MM-DD'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  weekDays: [
    {
      validator: (rule, value, callback) => {
        if (!value || !value.length) return callback(new Error('请至少选择一天'))
        callback()
      },
      trigger: 'change'
    }
  ],
  dueTime: [{ required: true, message: '请选择截止时刻', trigger: 'change' }],
  assigneeId: [{ required: true, message: '请选择主责人', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  startDate: [
    { required: true, message: '请选择生效日期', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        // 新建时不得早于今天；编辑既有规则允许保留历史生效日
        if (mode.value === 'add' && value && value < todayStr()) {
          return callback(new Error('生效日期不能早于今天'))
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

function createForm() {
  return {
    title: '',
    description: '',
    projectId: undefined,
    weekDays: [],
    dueTime: '18:00',
    assigneeId: undefined,
    priority: '1',
    tags: [],
    startDate: tomorrowStr()
  }
}

function disabledPastDate(date) {
  if (mode.value === 'edit') return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

function toggleWeekDay(day) {
  const index = form.value.weekDays.indexOf(day)
  if (index >= 0) {
    form.value.weekDays.splice(index, 1)
  } else {
    form.value.weekDays.push(day)
  }
}

function open(options = {}) {
  const opts = options || {}
  mode.value = opts.mode || 'add'
  reset()
  visible.value = true
  loadProjects()
  if (mode.value === 'edit' && opts.data) {
    const data = opts.data
    editingData.value = data
    recurId.value = data.recurId
    form.value.title = data.title || ''
    form.value.description = data.description || ''
    form.value.projectId = data.projectId || undefined
    form.value.weekDays = normalizeWeekDays(data.weekDays)
    form.value.dueTime = data.dueTime ? String(data.dueTime).slice(0, 5) : '18:00'
    form.value.assigneeId = data.assigneeId || undefined
    form.value.priority = data.priority !== undefined && data.priority !== null ? String(data.priority) : '1'
    form.value.tags = Array.isArray(data.tags) ? data.tags.slice() : []
    form.value.startDate = toDateString(data.startDate) || tomorrowStr()
    assigneeOptions.value = [
      { value: data.assigneeId, label: (data.assigneeName || '主责人') + (data.assigneeDeptName ? '（' + data.assigneeDeptName + '）' : '') }
    ]
    if (data.projectId && !projectOptions.value.some(item => item.projectId === data.projectId)) {
      projectOptions.value.push({ projectId: data.projectId, name: data.projectName || '项目' })
    }
  }
  nextTick(() => formRef.value && formRef.value.clearValidate())
}

function loadProjects() {
  // 无项目权限时不请求，避免 403
  if (!proxy.$auth.hasPermi('oa:project:list')) {
    projectOptions.value = []
    return
  }
  listProject({ pageNum: 1, pageSize: 200, status: '0' })
    .then(response => {
      const options = (response.rows || []).map(project => ({ projectId: project.projectId, name: project.name }))
      const existing = projectOptions.value
      projectOptions.value = options.length ? options : existing
    })
    .catch(() => {})
}

function searchAssignee(keyword) {
  assigneeLoading.value = true
  searchMemberOptions(keyword).then(options => {
    assigneeOptions.value = options
    assigneeLoading.value = false
  })
}

function reset() {
  recurId.value = undefined
  editingData.value = {}
  form.value = createForm()
  if (formRef.value) formRef.value.clearValidate()
}

function buildPayload() {
  const dueTime = String(form.value.dueTime || '')
  return {
    title: String(form.value.title || '').trim(),
    description: form.value.description || '',
    projectId: form.value.projectId || null,
    weekDays: normalizeWeekDays(form.value.weekDays),
    dueTime: dueTime.length === 5 ? dueTime + ':00' : dueTime,
    assigneeId: form.value.assigneeId,
    priority: form.value.priority,
    tags: form.value.tags || [],
    startDate: form.value.startDate
  }
}

async function handleSave() {
  if (saving.value) return
  let valid = false
  try {
    valid = await formRef.value.validate()
  } catch (e) {
    valid = false
  }
  if (!valid) return

  if (mode.value === 'edit') {
    // 编辑：先校验表单，再弹修改范围
    scopeDialogRef.value.open({ noInstance: false, defaultScope: 'all' })
    return
  }

  saving.value = true
  try {
    const response = await addRecur(buildPayload())
    const data = response.data || {}
    proxy.$modal.msgSuccess('规则已保存' + (data.nextDate ? '，下次生成日 ' + formatDate(data.nextDate) : ''))
    visible.value = false
    emit('success')
  } catch (e) {
    // 接口异常已由 request 拦截器统一提示
  } finally {
    saving.value = false
  }
}

/** 修改范围选择完成 → 提交编辑 */
async function handleScopeConfirm(scope) {
  saving.value = true
  try {
    const payload = { ...buildPayload(), recurId: recurId.value, applyScope: scope }
    if (scope === 'one' && editingData.value.targetTaskId) {
      payload.targetTaskId = editingData.value.targetTaskId
    }
    await updateRecur(payload)
    proxy.$modal.msgSuccess(scope === 'one' ? '已修改本次任务，规则模板未变更' : '规则已更新，历史任务不变')
    visible.value = false
    emit('success')
  } catch (e) {
    // 接口异常已由 request 拦截器统一提示
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.recur-form {
  padding-bottom: 8px;

  :deep(.el-form-item) {
    margin-bottom: 16px;
  }
}

.title-tip {
  width: 100%;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.7;

  code {
    padding: 1px 5px;
    border-radius: 3px;
    background: #f4f4f5;
    color: #606266;
    font-family: Consolas, Monaco, monospace;
  }

  .title-preview {
    color: #236de9;
  }
}

.weekday-picker {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.weekday-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    border-color: #236de9;
    color: #236de9;
  }

  &.is-active {
    border-color: #236de9;
    background: #236de9;
    color: #fff;
  }
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
