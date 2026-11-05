<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="620px"
    append-to-body
    :close-on-click-modal="false"
    @closed="reset"
  >
    <div v-loading="pageLoading" class="task-form">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px" label-position="top">
        <el-form-item label="任务标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="200"
            show-word-limit
            placeholder="请输入任务标题（2–200 个字符）"
          />
        </el-form-item>

        <template v-if="mode !== 'self'">
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="所属项目" prop="projectId">
                <el-select v-model="form.projectId" placeholder="不挂项目" clearable filterable style="width: 100%">
                  <el-option
                    v-for="project in projectOptions"
                    :key="project.projectId"
                    :label="project.name"
                    :value="project.projectId"
                  />
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
                  <el-option
                    v-for="item in assigneeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="协作人">
            <el-select
              v-model="form.collaboratorIds"
              placeholder="搜索姓名 / 学号，最多 10 人（跨部门需在项目成员中）"
              filterable
              remote
              multiple
              clearable
              :multiple-limit="10"
              :remote-method="searchCollaborator"
              :loading="collaboratorLoading"
              style="width: 100%"
            >
              <el-option
                v-for="item in collaboratorOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </template>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option
                  v-for="item in PRIORITY_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签（最多 3 个）">
              <el-select v-model="form.tags" multiple clearable :multiple-limit="3" style="width: 100%" placeholder="选择标签">
                <el-option
                  v-for="item in tagOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row v-if="mode !== 'self'" :gutter="12">
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
            <el-form-item label="截止时间" prop="dueTime">
              <el-date-picker
                v-model="form.dueTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择截止时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-else label="截止时间" prop="dueTime">
          <el-date-picker
            v-model="form.dueTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择截止时间"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="任务描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="2000"
            show-word-limit
            placeholder="补充背景、交付物、注意事项等（≤2000 字）"
          />
        </el-form-item>

        <el-form-item v-if="mode === 'add'" label="子任务（只拆一层）">
          <div class="child-list">
            <div v-for="(child, index) in form.children" :key="index" class="child-row">
              <el-input v-model="child.title" maxlength="200" placeholder="子任务标题" class="child-title" />
              <el-select
                v-model="child.assigneeId"
                placeholder="主责人"
                filterable
                remote
                clearable
                :remote-method="searchChildAssignee"
                :loading="childLoading"
                class="child-assignee"
              >
                <el-option
                  v-for="item in childAssigneeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-date-picker
                v-model="child.dueTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="截止时间"
                class="child-due"
              />
              <el-button link type="danger" icon="Delete" @click="removeChild(index)" />
            </div>
            <el-button link type="primary" icon="Plus" @click="addChild">添加子任务</el-button>
          </div>
        </el-form-item>

        <el-form-item label="附件（单文件 ≤10MB）">
          <div class="attach-field">
            <div v-if="form.attachments.length" class="attach-list">
              <div v-for="(file, index) in form.attachments" :key="file.fileId || index" class="attach-item">
                <el-icon><Document /></el-icon>
                <span class="attach-name">{{ file.originalName || file.fileName || '附件' }}</span>
                <el-button link type="danger" icon="Close" @click="removeAttachment(index)" />
              </div>
            </div>
            <el-upload
              :show-file-list="false"
              :http-request="handleUpload"
              :before-upload="beforeUpload"
              accept=".jpg,.jpeg,.png,.pdf,.docx,.xlsx"
            >
              <el-button icon="Upload" :loading="uploading">上传附件</el-button>
            </el-upload>
            <div class="oa-muted">支持 jpg / png / pdf / docx / xlsx</div>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-button :disabled="saving" @click="visible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup name="TaskFormDrawer">
import { addTask, addSelfTask, updateTask, uploadTaskFile } from '@/api/oa/task'
import { listProject } from '@/api/oa/project'
import useUserStore from '@/store/modules/user'
import { PRIORITY_OPTIONS, toDate, toDateTimeString, searchMemberOptions, memberBriefOptions, TASK_TAG_FALLBACK } from '@/utils/oaTask'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['success'])
const userStore = useUserStore()
const { oa_task_tag } = useDict('oa_task_tag')

const visible = ref(false)
const saving = ref(false)
const uploading = ref(false)
const pageLoading = ref(false)
const mode = ref('add')
const taskId = ref(undefined)
const formRef = ref()

const projectOptions = ref([])
const assigneeOptions = ref([])
const assigneeLoading = ref(false)
const collaboratorOptions = ref([])
const collaboratorLoading = ref(false)
const childAssigneeOptions = ref([])
const childLoading = ref(false)

/** 标签选项：按设计口径直接使用中文标签（字典 label） */
const tagOptions = computed(() => {
  const dict = oa_task_tag && oa_task_tag.value ? oa_task_tag.value : []
  if (dict.length) return dict.map(item => ({ value: item.label, label: item.label }))
  return Object.keys(TASK_TAG_FALLBACK).map(key => ({ value: TASK_TAG_FALLBACK[key], label: TASK_TAG_FALLBACK[key] }))
})

const drawerTitle = computed(() => {
  if (mode.value === 'self') return '新建自有任务'
  if (mode.value === 'edit') return '编辑任务'
  return '新建任务'
})

const form = ref(createForm())

const rules = {
  title: [
    { required: true, message: '请填写任务标题', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        const text = String(value || '').trim()
        if (!text) return callback(new Error('请填写任务标题'))
        if (text.length < 2 || text.length > 200) return callback(new Error('标题需为 2–200 个字符'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  assigneeId: [{ required: true, message: '请选择主责人', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  dueTime: [
    { required: true, message: '请选择截止时间', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        const start = toDate(form.value.startTime)
        const due = toDate(value)
        if (start && due && due.getTime() < start.getTime()) {
          return callback(new Error('截止时间不能早于开始时间'))
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  startTime: [
    {
      validator: (rule, value, callback) => {
        const start = toDate(value)
        const due = toDate(form.value.dueTime)
        if (start && due && start.getTime() > due.getTime()) {
          return callback(new Error('开始时间不能晚于截止时间'))
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
    assigneeId: undefined,
    collaboratorIds: [],
    priority: '1',
    tags: [],
    startTime: undefined,
    dueTime: undefined,
    children: [],
    attachments: []
  }
}

/** 打开抽屉：mode = add / edit / self */
async function open(options = {}) {
  const opts = options || {}
  mode.value = opts.mode || 'add'
  reset()
  visible.value = true
  pageLoading.value = true
  try {
    await loadProjects()
    if (mode.value === 'edit' && opts.data) {
      fillFromDetail(opts.data)
    } else if (mode.value === 'add') {
      if (opts.projectId) form.value.projectId = opts.projectId
      if (opts.assigneeId) {
        form.value.assigneeId = opts.assigneeId
        assigneeOptions.value = memberBriefOptions([{ userId: opts.assigneeId, userName: opts.assigneeName }])
      }
    }
    // 自有任务：主责人固定为本人
    if (mode.value === 'self') {
      form.value.assigneeId = userStore.id
    }
  } finally {
    pageLoading.value = false
  }
}

async function loadProjects() {
  try {
    const response = await listProject({ pageNum: 1, pageSize: 200, status: '0' })
    projectOptions.value = (response.rows || []).map(project => ({
      projectId: project.projectId,
      name: project.name
    }))
  } catch (e) {
    projectOptions.value = []
  }
}

function fillFromDetail(data) {
  taskId.value = data.taskId
  form.value.title = data.title || ''
  form.value.description = data.description || ''
  form.value.projectId = data.projectId || undefined
  form.value.assigneeId = data.assigneeId || undefined
  form.value.priority = data.priority !== undefined && data.priority !== null ? String(data.priority) : '1'
  form.value.tags = Array.isArray(data.tags) ? data.tags.slice() : []
  form.value.startTime = toDateTimeString(data.startTime)
  form.value.dueTime = toDateTimeString(data.dueTime)
  form.value.collaboratorIds = (data.collaborators || []).map(item => item.userId)
  form.value.attachments = (data.files || []).map(file => ({ ...file }))
  assigneeOptions.value = memberBriefOptions([
    { userId: data.assigneeId, userName: data.assigneeName, deptName: data.assigneeDeptName }
  ])
  collaboratorOptions.value = memberBriefOptions(data.collaborators || [])
  if (data.projectId && !projectOptions.value.some(item => item.projectId === data.projectId)) {
    projectOptions.value.push({ projectId: data.projectId, name: data.projectName || '项目' })
  }
}

function searchAssignee(keyword) {
  assigneeLoading.value = true
  searchMemberOptions(keyword).then(options => {
    assigneeOptions.value = options
    assigneeLoading.value = false
  })
}

function searchCollaborator(keyword) {
  collaboratorLoading.value = true
  searchMemberOptions(keyword).then(options => {
    // 保留已选成员的标签，避免远端搜索后回显丢失
    const map = new Map(collaboratorOptions.value.map(item => [item.value, item]))
    options.forEach(item => map.set(item.value, item))
    collaboratorOptions.value = [...map.values()]
    collaboratorLoading.value = false
  })
}

function searchChildAssignee(keyword) {
  childLoading.value = true
  searchMemberOptions(keyword).then(options => {
    childAssigneeOptions.value = options
    childLoading.value = false
  })
}

function addChild() {
  form.value.children.push({ title: '', assigneeId: undefined, dueTime: undefined })
}

function removeChild(index) {
  form.value.children.splice(index, 1)
}

function reset() {
  taskId.value = undefined
  form.value = createForm()
  if (formRef.value) formRef.value.clearValidate()
}

/* ------------------------------ 附件 ------------------------------ */
function beforeUpload(file) {
  const allow = ['jpg', 'jpeg', 'png', 'pdf', 'docx', 'xlsx']
  const ext = String(file.name || '').split('.').pop().toLowerCase()
  if (!allow.includes(ext)) {
    proxy.$modal.msgError('仅支持 jpg / png / pdf / docx / xlsx')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    proxy.$modal.msgError('单个文件不能超过 10MB')
    return false
  }
  return true
}

async function handleUpload(options) {
  uploading.value = true
  try {
    const response = await uploadTaskFile(options.file)
    const data = response.data || {}
    form.value.attachments.push({
      fileId: data.fileId,
      fileName: options.file.name,
      originalName: options.file.name,
      fileUrl: data.fileUrl,
      fileSize: options.file.size
    })
    proxy.$modal.msgSuccess('上传成功')
  } catch (e) {
    // 拦截器已提示
  } finally {
    uploading.value = false
  }
}

function removeAttachment(index) {
  form.value.attachments.splice(index, 1)
}

/* ------------------------------ 保存 ------------------------------ */
function validateChildren() {
  for (const child of form.value.children) {
    if (!String(child.title || '').trim()) {
      proxy.$modal.msgWarning('请填写子任务标题')
      return false
    }
    if (String(child.title).trim().length < 2) {
      proxy.$modal.msgWarning('子任务标题需为 2–200 个字符')
      return false
    }
    if (!child.assigneeId) {
      proxy.$modal.msgWarning('请选择子任务主责人')
      return false
    }
    if (!child.dueTime) {
      proxy.$modal.msgWarning('请选择子任务截止时间')
      return false
    }
  }
  return true
}

function buildPayload() {
  const payload = {
    title: String(form.value.title || '').trim(),
    description: form.value.description || '',
    priority: form.value.priority,
    tags: form.value.tags,
    dueTime: form.value.dueTime,
    attachmentFileIds: form.value.attachments.map(file => file.fileId).filter(id => id !== undefined && id !== null)
  }
  if (mode.value === 'self') return payload
  payload.projectId = form.value.projectId || null
  payload.assigneeId = form.value.assigneeId
  payload.collaboratorIds = form.value.collaboratorIds || []
  payload.startTime = form.value.startTime || null
  if (mode.value === 'add') {
    payload.children = form.value.children.map(child => ({
      title: String(child.title || '').trim(),
      assigneeId: child.assigneeId,
      dueTime: child.dueTime
    }))
  }
  return payload
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
  if (mode.value === 'add' && !validateChildren()) return

  saving.value = true
  try {
    if (mode.value === 'self') {
      const response = await addSelfTask(buildPayload())
      const data = response.data || {}
      proxy.$modal.msgSuccess('任务已创建' + (data.taskNo ? '（' + data.taskNo + '）' : ''))
    } else if (mode.value === 'edit') {
      await updateTask({ taskId: taskId.value, ...buildPayload() })
      proxy.$modal.msgSuccess('任务已更新')
    } else {
      const response = await addTask(buildPayload())
      const data = response.data || {}
      proxy.$modal.msgSuccess('任务已创建' + (data.taskNo ? '（' + data.taskNo + '），已通知主责人' : '，已通知主责人'))
    }
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
.task-form {
  padding-bottom: 8px;

  :deep(.el-form-item) {
    margin-bottom: 16px;
  }
}

.child-list {
  width: 100%;
}

.child-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;

  .child-title {
    flex: 1.4;
    min-width: 0;
  }

  .child-assignee {
    flex: 1;
    min-width: 0;
  }

  .child-due {
    flex: 1;
    min-width: 0;
  }
}

.attach-field {
  width: 100%;
}

.attach-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 8px;
}

.attach-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f7f8fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;

  .attach-name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
