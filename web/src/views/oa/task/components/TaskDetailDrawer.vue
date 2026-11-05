<template>
  <el-drawer
    v-model="visible"
    :size="620"
    :with-header="true"
    append-to-body
    :close-on-click-modal="false"
    class="task-detail-drawer"
    @closed="handleClosed"
  >
    <template #header>
      <div class="drawer-head">
        <div class="drawer-head-main">
          <span class="task-no">{{ detail.taskNo || '—' }}</span>
          <span class="task-title" :title="detail.title">{{ detail.title || '任务详情' }}</span>
          <OaTaskStatusTag v-if="detail.status !== undefined" :status="detail.status" size="small" />
          <span v-if="overdueLabel" class="badge-overdue">{{ overdueLabel }}</span>
        </div>
      </div>
    </template>

    <!-- 归档只读 banner -->
    <div v-if="!loading && !error && readOnly" class="readonly-banner">
      <el-icon><Lock /></el-icon>
      项目已结束，任务只读
    </div>

    <!-- 加载态 -->
    <div v-if="loading" class="detail-skeleton">
      <el-skeleton :rows="3" animated />
      <el-skeleton :rows="6" animated style="margin-top: 20px" />
      <el-skeleton :rows="3" animated style="margin-top: 20px" />
    </div>

    <!-- 错误态 -->
    <div v-else-if="error" class="detail-error">
      <el-icon :size="34"><WarningFilled /></el-icon>
      <p>{{ errorText }}</p>
      <div>
        <el-button size="small" @click="visible = false">关 闭</el-button>
        <el-button size="small" type="primary" @click="loadDetail">重新加载</el-button>
      </div>
    </div>

    <!-- 详情内容 -->
    <div v-else class="detail-body">
      <!-- 属性区（双列） -->
      <div class="detail-section">
        <div class="section-title">属性</div>
        <div class="attr-grid">
          <div class="attr-item">
            <span class="attr-label">项目</span>
            <span class="attr-value">
              {{ detail.projectName || '未挂项目' }}
              <span v-if="detail.projectStatus === '1'" class="archived-tag">已结束</span>
            </span>
          </div>
          <div class="attr-item">
            <span class="attr-label">主责人</span>
            <span class="attr-value">
              <span class="avatar-xs" :style="{ background: avatarColor(detail.assigneeId) }">{{ nameInitial(detail.assigneeName) }}</span>
              {{ detail.assigneeName || '—' }}
              <span v-if="detail.assigneeDeptName" class="oa-muted">（{{ detail.assigneeDeptName }}）</span>
            </span>
          </div>
          <div class="attr-item">
            <span class="attr-label">协作人</span>
            <span class="attr-value">{{ collaboratorNames || '暂无协作人' }}</span>
          </div>
          <div class="attr-item">
            <span class="attr-label">验收人</span>
            <span class="attr-value">{{ detail.auditorName || '—' }}</span>
          </div>
          <div class="attr-item">
            <span class="attr-label">优先级</span>
            <span class="attr-value"><OaTaskStatusTag type="priority" :priority="detail.priority" size="small" /></span>
          </div>
          <div class="attr-item">
            <span class="attr-label">标签</span>
            <span class="attr-value">
              <template v-if="tagList.length">
                <span v-for="tag in tagList" :key="tag" class="tag-chip">{{ tag }}</span>
              </template>
              <span v-else class="oa-muted">暂无标签</span>
            </span>
          </div>
          <div class="attr-item">
            <span class="attr-label">开始时间</span>
            <span class="attr-value">{{ formatDateTime(detail.startTime) }}</span>
          </div>
          <div class="attr-item">
            <span class="attr-label">截止时间</span>
            <span class="attr-value" :class="{ 'is-red': overdueLabel }">{{ formatDateTime(detail.dueTime) }}</span>
          </div>
          <div class="attr-item">
            <span class="attr-label">提交时间</span>
            <span class="attr-value">{{ formatDateTime(detail.submitTime) }}</span>
          </div>
          <div class="attr-item">
            <span class="attr-label">实际完成时间</span>
            <span class="attr-value">{{ formatDateTime(detail.finishTime) }}</span>
          </div>
          <div class="attr-item">
            <span class="attr-label">来源</span>
            <span class="attr-value">{{ sourceLabel(detail.source) }}</span>
          </div>
          <div class="attr-item">
            <span class="attr-label">周期规则</span>
            <span class="attr-value">{{ detail.recurId ? '周期生成任务（#' + detail.recurId + '）' : '—' }}</span>
          </div>
        </div>
      </div>

      <!-- 描述 -->
      <div class="detail-section">
        <div class="section-title">描述</div>
        <div v-if="detail.description" class="desc-text" :class="{ 'is-collapsed': descCollapsed }">
          {{ detail.description }}
        </div>
        <div v-else class="oa-muted">暂无描述</div>
        <el-button v-if="descExpandable" link type="primary" size="small" @click="descCollapsed = !descCollapsed">
          {{ descCollapsed ? '展开' : '收起' }}
        </el-button>
      </div>

      <!-- 子任务 -->
      <div class="detail-section">
        <div class="section-title">
          子任务清单
          <small>进度 {{ childDoneCount }}/{{ childList.length }} · 只拆一层</small>
        </div>
        <div v-if="childList.length" class="subtask-list">
          <div
            v-for="child in childList"
            :key="child.taskId"
            class="subtask-row"
            :class="{ 'is-done': child.status === '3' }"
          >
            <el-tooltip :content="childCheckTip(child)" :disabled="childCheckable(child)" placement="top">
              <el-checkbox
                :model-value="child.status === '3'"
                :disabled="!childCheckable(child)"
                @change="handleChildAudit(child)"
              />
            </el-tooltip>
            <span class="st-title" :title="child.title">{{ child.title }}</span>
            <span class="st-assignee">{{ child.assigneeName || '—' }}</span>
            <OaTaskStatusTag :status="child.status" size="small" />
            <span class="st-due" :class="{ 'is-red': isOverdue(child) }">{{ formatDueTime(child.dueTime) }}</span>
          </div>
        </div>
        <div v-else class="oa-muted">暂无子任务</div>
      </div>

      <!-- 协作人 -->
      <div class="detail-section">
        <div class="section-title">
          协作人
          <small>可查看、可上传附件，不改变任务状态</small>
        </div>
        <div v-if="collaborators.length" class="collaborator-chips">
          <span v-for="member in collaborators" :key="member.userId" class="member-chip">
            <span class="avatar-xs" :style="{ background: avatarColor(member.userId) }">{{ nameInitial(member.userName) }}</span>
            {{ member.userName }}
            <span class="oa-muted">{{ member.deptName || '' }}</span>
          </span>
        </div>
        <div v-else class="oa-muted">暂无协作人</div>
      </div>

      <!-- 附件 -->
      <div class="detail-section">
        <div class="section-title">
          附件
          <small>含完成凭证；下载走鉴权接口</small>
        </div>
        <div class="attach-list">
          <div v-for="file in allFiles" :key="'file-' + file.fileId" class="attach-item">
            <div class="attach-icon">
              <el-icon :size="18"><Document /></el-icon>
            </div>
            <div class="attach-main">
              <div class="attach-name" :title="fileName(file)">{{ fileName(file) }}</div>
              <div class="oa-muted">
                {{ formatFileSize(file.fileSize) }}
                <span v-if="file.pending" class="pending-tag">待提交</span>
              </div>
            </div>
            <el-button link type="primary" @click="handleDownload(file)">下载</el-button>
          </div>
          <div v-if="!allFiles.length" class="oa-muted">未上传附件</div>
        </div>
        <div v-if="!readOnly && canUpload" class="upload-box">
          <el-upload
            :show-file-list="false"
            :http-request="handleUpload"
            :before-upload="beforeUpload"
            accept=".jpg,.jpeg,.png,.pdf,.docx,.xlsx"
          >
            <el-button icon="Upload" size="small" :loading="uploading">上传附件</el-button>
          </el-upload>
          <span class="oa-muted">支持 jpg / png / pdf / docx / xlsx，单文件 ≤10MB</span>
        </div>
      </div>

      <!-- 流转时间轴 -->
      <div class="detail-section">
        <div class="section-title">流转记录 <small>最新在上</small></div>
        <div v-if="logList.length" ref="timelineRef" class="timeline">
          <div
            v-for="(log, index) in logList"
            :key="log.logId || index"
            class="tl-item"
            :class="{ 'is-highlight': highlightLogId === log.logId || (highlightLogId === null && index === 0 && highlightEnabled) }"
          >
            <div class="tl-title">{{ log.action || '操作' }}</div>
            <div class="tl-meta">{{ formatDateTime(log.createTime) }} · {{ log.operatorName || '系统' }}</div>
            <div v-if="log.remark" class="tl-comment">{{ log.remark }}</div>
          </div>
        </div>
        <div v-else class="oa-muted">暂无流转记录</div>
      </div>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <span v-if="readOnly" class="oa-muted">项目已结束，任务只读</span>
        <span v-else-if="subtaskHint" class="footer-hint">{{ subtaskHint }}</span>
        <span style="flex: 1" />
        <template v-if="!readOnly && !error">
          <el-button v-if="showStart" v-hasPermi="['oa:task:edit']" size="small" type="primary" @click="handleStart">开始</el-button>
          <el-button v-if="showSubmit" v-hasPermi="['oa:task:submit']" size="small" type="primary" @click="handleSubmit">提交完成</el-button>
          <el-button v-if="showAudit" v-hasPermi="['oa:task:audit']" size="small" type="success" @click="handlePass">验收通过</el-button>
          <el-button v-if="showAudit" v-hasPermi="['oa:task:audit']" size="small" type="danger" plain @click="openReject">打回</el-button>
          <el-button v-if="showCancel" v-hasPermi="['oa:task:edit']" size="small" type="danger" plain @click="openCancel">取消</el-button>
          <el-button v-if="showReassign" v-hasPermi="['oa:task:assign']" size="small" @click="openReassign">改派</el-button>
          <el-button v-if="showEdit" v-hasPermi="['oa:task:edit']" size="small" @click="openEdit">编辑</el-button>
        </template>
        <span v-if="canRemove" class="footer-divider" />
        <el-button
          v-if="canRemove"
          v-hasPermi="['oa:task:remove']"
          size="small"
          type="danger"
          plain
          :loading="deleting"
          @click="handleDelete"
        >删除任务</el-button>
        <el-button size="small" @click="visible = false">关 闭</el-button>
      </div>
    </template>
  </el-drawer>

  <!-- 打回弹窗（760px，理由 2–200 必填） -->
  <el-dialog v-model="rejectVisible" title="打回任务" width="760px" append-to-body :close-on-click-modal="false">
    <el-form ref="rejectFormRef" :model="rejectForm" :rules="remarkRules" label-width="88px">
      <div class="dialog-tip">打回后任务回到「进行中」，打回次数 +1，理由将通知主责人。</div>
      <el-form-item label="任务">
        <el-input :model-value="(detail.taskNo || '') + ' · ' + (detail.title || '')" disabled />
      </el-form-item>
      <el-form-item label="快速理由">
        <el-button
          v-for="reason in REJECT_REASONS"
          :key="reason"
          size="small"
          :type="rejectForm.remark === reason ? 'primary' : 'default'"
          @click="rejectForm.remark = reason"
        >{{ reason }}</el-button>
      </el-form-item>
      <el-form-item label="打回理由" prop="remark">
        <el-input
          v-model="rejectForm.remark"
          type="textarea"
          :rows="4"
          maxlength="200"
          show-word-limit
          placeholder="请填写打回理由（2–200 字）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rejectVisible = false">取 消</el-button>
      <el-button type="danger" :loading="submitting" :disabled="!remarkValid" @click="submitReject">确认打回</el-button>
    </template>
  </el-dialog>

  <!-- 取消弹窗（420px，原因必填） -->
  <el-dialog v-model="cancelVisible" title="取消任务" width="420px" append-to-body :close-on-click-modal="false">
    <el-form ref="cancelFormRef" :model="cancelForm" :rules="remarkRules" label-width="82px">
      <div class="dialog-tip">确认取消任务？取消后不可恢复，原因将通知主责人与协作人。</div>
      <el-form-item label="取消原因" prop="remark">
        <el-input
          v-model="cancelForm.remark"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="请填写取消原因（2–200 字）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="cancelVisible = false">取 消</el-button>
      <el-button type="danger" :loading="submitting" :disabled="!cancelValid" @click="submitCancel">确认取消</el-button>
    </template>
  </el-dialog>

  <!-- 改派弹窗（760px） -->
  <el-dialog v-model="reassignVisible" title="改派主责人" width="760px" append-to-body :close-on-click-modal="false">
    <el-form ref="reassignFormRef" :model="reassignForm" label-width="88px">
      <div class="dialog-tip">改派后任务状态不变并写入流转日志，新主责人会收到通知。</div>
      <el-form-item label="当前主责人">
        <el-input :model-value="detail.assigneeName || '—'" disabled />
      </el-form-item>
      <el-form-item label="新主责人" required>
        <el-select
          v-model="reassignForm.assigneeId"
          placeholder="搜索姓名 / 学号"
          filterable
          remote
          clearable
          :remote-method="searchReassignMember"
          :loading="reassignLoading"
          style="width: 100%"
        >
          <el-option
            v-for="item in reassignOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="reassignForm.remark" type="textarea" :rows="2" maxlength="200" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reassignVisible = false">取 消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!reassignForm.assigneeId" @click="submitReassign">确认改派</el-button>
    </template>
  </el-dialog>

  <!-- 编辑任务（复用新建抽屉） -->
  <task-form-drawer ref="taskFormRef" @success="handleEditSuccess" />
</template>

<script setup name="TaskDetailDrawer">
import { ElMessageBox } from 'element-plus'
import OaTaskStatusTag from '@/components/oa/OaTaskStatusTag.vue'
import TaskFormDrawer from './TaskFormDrawer.vue'
import {
  REJECT_REASONS,
  TASK_STATUS,
  avatarColor,
  canAudit,
  canCancel,
  canEdit,
  canReassign,
  canStart,
  canSubmit,
  fileName,
  formatDateTime,
  formatDueTime,
  formatFileSize,
  isOverdue,
  nameInitial,
  normalizeTags,
  overdueText,
  sourceLabel,
  toDate,
  toDateTimeString
} from '@/utils/oaTask'
import {
  auditTask,
  cancelTask,
  deleteTask,
  downloadTaskFile,
  getTask,
  reassignTask,
  startTask,
  submitTask,
  updateTask,
  uploadTaskFile
} from '@/api/oa/task'
import { searchMemberOptions } from '@/utils/oaTask'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['refresh', 'closed'])
const { oa_task_tag } = useDict('oa_task_tag')

const visible = ref(false)
const loading = ref(false)
const error = ref(false)
const errorText = ref('任务详情加载失败')
const submitting = ref(false)
const uploading = ref(false)
const deleting = ref(false)
const detail = ref({})
const taskId = ref(undefined)
const pendingFiles = ref([])
const descCollapsed = ref(true)
const highlightEnabled = ref(false)
const timelineRef = ref(null)

const rejectVisible = ref(false)
const cancelVisible = ref(false)
const reassignVisible = ref(false)
const rejectFormRef = ref()
const cancelFormRef = ref()
const reassignFormRef = ref()
const taskFormRef = ref()

const rejectForm = ref({ remark: '' })
const cancelForm = ref({ remark: '' })
const reassignForm = ref({ assigneeId: undefined, remark: '' })
const reassignOptions = ref([])
const reassignLoading = ref(false)

const remarkRules = {
  remark: [
    {
      validator: (rule, value, callback) => {
        const text = String(value || '').trim()
        if (!text) return callback(new Error('请填写理由'))
        if (text.length < 2 || text.length > 200) return callback(new Error('理由需为 2–200 个字符'))
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const readOnly = computed(() => !!detail.value.readOnly)
/** 是否具备删除任务权限（契约追加需求 §9） */
const canRemove = computed(() => proxy.$auth.hasPermi('oa:task:remove'))
const overdueLabel = computed(() => overdueText(detail.value))
const collaborators = computed(() => detail.value.collaborators || [])
const childList = computed(() => detail.value.children || [])
const childDoneCount = computed(() => childList.value.filter(child => String(child.status) === TASK_STATUS.DONE).length)
const tagList = computed(() => normalizeTags(detail.value.tags, oa_task_tag.value))
const collaboratorNames = computed(() => collaborators.value.map(item => item.userName).filter(Boolean).join('、'))

const allFiles = computed(() => {
  const existing = (detail.value.files || []).map(file => ({ ...file, pending: false }))
  const pending = pendingFiles.value.map(file => ({ ...file, pending: true }))
  return [...existing, ...pending]
})

const descExpandable = computed(() => String(detail.value.description || '').length > 120)

const subtaskHint = computed(() => {
  if (!childList.value.length) return ''
  if (childDoneCount.value === childList.value.length) return ''
  return '子任务 ' + childDoneCount.value + '/' + childList.value.length + ' 未完成'
})

/* ---------------------------- 按钮可见性 ---------------------------- */
const showStart = computed(() => String(detail.value.status) === TASK_STATUS.TODO && canStart(detail.value))
const showSubmit = computed(() => String(detail.value.status) === TASK_STATUS.DOING && canSubmit(detail.value))
const showAudit = computed(() => String(detail.value.status) === TASK_STATUS.AUDIT && canAudit(detail.value))
const showCancel = computed(() => ['0', '1'].includes(String(detail.value.status)) && canCancel(detail.value))
const showReassign = computed(() => ['0', '1', '2'].includes(String(detail.value.status)) && canReassign(detail.value))
const showEdit = computed(() => ['0', '1'].includes(String(detail.value.status)) && canEdit(detail.value))
const canUpload = computed(() => ['0', '1', '2'].includes(String(detail.value.status)))

const remarkValid = computed(() => {
  const text = String(rejectForm.value.remark || '').trim()
  return text.length >= 2 && text.length <= 200
})
const cancelValid = computed(() => {
  const text = String(cancelForm.value.remark || '').trim()
  return text.length >= 2 && text.length <= 200
})

const logList = computed(() => {
  const logs = (detail.value.logs || []).slice()
  return logs.sort((a, b) => {
    const left = toDate(a.createTime)
    const right = toDate(b.createTime)
    return (right ? right.getTime() : 0) - (left ? left.getTime() : 0)
  })
})
const highlightLogId = computed(() => {
  if (!highlightEnabled.value) return undefined
  const hit = logList.value.find(log => /提交|打回/.test(String(log.action || '')))
  return hit ? hit.logId : undefined
})

/* ---------------------------- 打开 / 加载 ---------------------------- */
function open(id, options = {}) {
  if (!id) return
  taskId.value = id
  detail.value = {}
  pendingFiles.value = []
  descCollapsed.value = true
  highlightEnabled.value = !!(options && options.highlight)
  visible.value = true
  loadDetail()
}

function loadDetail() {
  if (!taskId.value) return
  loading.value = true
  error.value = false
  getTask(taskId.value)
    .then(response => {
      detail.value = response.data || {}
      error.value = false
      nextTick(scrollToHighlight)
    })
    .catch(() => {
      error.value = true
      errorText.value = '任务详情加载失败或没有访问该任务的权限'
    })
    .finally(() => {
      loading.value = false
    })
}

function scrollToHighlight() {
  if (!highlightEnabled.value) return
  const el = timelineRef.value
  if (el && typeof el.scrollIntoView === 'function') {
    el.scrollIntoView({ block: 'nearest' })
  }
}

function handleClosed() {
  emit('closed')
}

function handleEditSuccess() {
  loadDetail()
  emit('refresh')
}

/* ---------------------------- 子任务快捷验收 ---------------------------- */
function childCheckable(child) {
  return String(child.status) === TASK_STATUS.AUDIT && child.canAudit !== false && !readOnly.value
}

function childCheckTip(child) {
  if (readOnly.value) return '项目已结束，任务只读'
  if (child.canAudit === false) return '只有验收人可以验收'
  if (String(child.status) !== TASK_STATUS.AUDIT) return '子任务需由主责人提交后再验收'
  return '勾选 = 验收通过该子任务'
}

async function handleChildAudit(child) {
  if (!childCheckable(child)) return
  try {
    await ElMessageBox.confirm('确认验收通过子任务「' + child.title + '」？', '系统提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await auditTask({ taskId: child.taskId, pass: true, remark: '子任务验收通过' })
    proxy.$modal.msgSuccess('子任务已验收通过')
    loadDetail()
    emit('refresh')
  } catch (e) {
    // 拦截器已提示
  }
}

/* ---------------------------- 流转操作 ---------------------------- */
async function handleStart() {
  try {
    await startTask({ taskId: detail.value.taskId })
    proxy.$modal.msgSuccess('已开始任务')
    loadDetail()
    emit('refresh')
  } catch (e) {}
}

/** 删除任务（契约追加需求 §9，硬删除不可恢复） */
function handleDelete() {
  if (deleting.value) return
  ElMessageBox.confirm(
    '删除任务「' + (detail.value.title || '') + '」？删除后不可恢复，其子任务、协作人、流转日志、任务附件记录与站内信将一并删除。',
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
        await deleteTask(detail.value.taskId)
        proxy.$modal.msgSuccess('删除成功')
        visible.value = false
        emit('refresh')
      } catch (e) {
        // 接口异常已由 request 拦截器统一提示
      } finally {
        deleting.value = false
      }
    })
    .catch(() => {})
}

function handleSubmit() {
  const unfinished = childList.value.filter(child => String(child.status) !== TASK_STATUS.DONE)
  if (unfinished.length) {
    const names = unfinished.slice(0, 5).map(child => child.title).join('、')
    ElMessageBox.alert(
      '还有 ' + unfinished.length + ' 个子任务未完成：' + names + (unfinished.length > 5 ? ' 等' : ''),
      '暂不可提交',
      { type: 'warning', confirmButtonText: '知道了' }
    ).catch(() => {})
    return
  }
  ElMessageBox.confirm(
    '确认提交完成？提交后由 ' + (detail.value.auditorName || '验收人') + ' 验收',
    '系统提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
    .then(async () => {
      submitting.value = true
      try {
        await submitTask({
          taskId: detail.value.taskId,
          remark: '',
          attachmentFileIds: pendingFiles.value.map(file => file.fileId)
        })
        pendingFiles.value = []
        proxy.$modal.msgSuccess('已提交，等待 ' + (detail.value.auditorName || '验收人') + ' 验收')
        loadDetail()
        emit('refresh')
      } catch (e) {
        // 拦截器已提示
      } finally {
        submitting.value = false
      }
    })
    .catch(() => {})
}

function handlePass() {
  ElMessageBox.confirm('确认验收通过？将写入实际完成时间并通知主责人', '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      submitting.value = true
      try {
        await auditTask({
          taskId: detail.value.taskId,
          pass: true,
          remark: '',
          finishFileIds: pendingFiles.value.map(file => file.fileId)
        })
        pendingFiles.value = []
        const finishText = parseTime(new Date(), '{y}-{m}-{d} {h}:{i}')
        proxy.$modal.msgSuccess('已验收通过，完成时间 ' + finishText)
        loadDetail()
        emit('refresh')
      } catch (e) {
        // 拦截器已提示
      } finally {
        submitting.value = false
      }
    })
    .catch(() => {})
}

function openReject() {
  rejectForm.value = { remark: '' }
  rejectVisible.value = true
  nextTick(() => rejectFormRef.value && rejectFormRef.value.clearValidate())
}

async function submitReject() {
  try {
    await rejectFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await auditTask({ taskId: detail.value.taskId, pass: false, remark: rejectForm.value.remark.trim() })
    rejectVisible.value = false
    proxy.$modal.msgSuccess('已打回，主责人将收到通知')
    loadDetail()
    emit('refresh')
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function openCancel() {
  cancelForm.value = { remark: '' }
  cancelVisible.value = true
  nextTick(() => cancelFormRef.value && cancelFormRef.value.clearValidate())
}

async function submitCancel() {
  try {
    await cancelFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await cancelTask({ taskId: detail.value.taskId, remark: cancelForm.value.remark.trim() })
    cancelVisible.value = false
    proxy.$modal.msgSuccess('任务已取消')
    loadDetail()
    emit('refresh')
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function openReassign() {
  reassignForm.value = { assigneeId: undefined, remark: '' }
  reassignOptions.value = []
  reassignLoading.value = true
  searchMemberOptions('').then(options => {
    reassignOptions.value = options
    reassignLoading.value = false
  })
  reassignVisible.value = true
}

function searchReassignMember(keyword) {
  reassignLoading.value = true
  searchMemberOptions(keyword).then(options => {
    reassignOptions.value = options
    reassignLoading.value = false
  })
}

async function submitReassign() {
  if (!reassignForm.value.assigneeId) return
  submitting.value = true
  try {
    await reassignTask({
      taskId: detail.value.taskId,
      assigneeId: reassignForm.value.assigneeId,
      collaboratorIds: collaborators.value.map(item => item.userId),
      remark: reassignForm.value.remark || ''
    })
    reassignVisible.value = false
    const target = reassignOptions.value.find(item => item.value === reassignForm.value.assigneeId)
    proxy.$modal.msgSuccess('已改派给 ' + (target ? target.name : '新主责人') + '，原主责人已收到通知')
    loadDetail()
    emit('refresh')
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function openEdit() {
  taskFormRef.value.open({ mode: 'edit', data: detail.value })
}

/* ---------------------------- 附件 ---------------------------- */
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
    pendingFiles.value.push({
      fileId: data.fileId,
      fileName: options.file.name,
      originalName: options.file.name,
      fileSize: options.file.size,
      fileUrl: data.fileUrl
    })
    // 有编辑权限时立即绑定到任务；否则保留待提交（提交 / 验收时随 DTO 绑定）
    const persisted = await persistPendingFiles()
    proxy.$modal.msgSuccess(persisted ? '附件已上传' : '上传成功，提交 / 验收时一并绑定')
    if (persisted) {
      loadDetail()
      emit('refresh')
    }
  } catch (e) {
    // 拦截器已提示
  } finally {
    uploading.value = false
  }
}

/**
 * 把待绑定附件写入任务（调用编辑接口回填 attachmentFileIds）
 * 仅「未完成且本人可编辑」时直接持久化；其余场景由提交 / 验收接口绑定。
 * @returns {Promise<boolean>} 是否已立即持久化
 */
async function persistPendingFiles() {
  const current = detail.value
  if (!pendingFiles.value.length) return false
  if (readOnly.value || !canEdit(current)) return false
  if (!['0', '1'].includes(String(current.status))) return false
  const existingIds = (current.files || []).map(file => file.fileId).filter(Boolean)
  const pendingIds = pendingFiles.value.map(file => file.fileId).filter(Boolean)
  try {
    await updateTask({
      taskId: current.taskId,
      title: current.title,
      description: current.description || '',
      projectId: current.projectId || null,
      assigneeId: current.assigneeId,
      collaboratorIds: collaborators.value.map(item => item.userId),
      priority: current.priority,
      tags: Array.isArray(current.tags) ? current.tags : [],
      startTime: toDateTimeString(current.startTime) || null,
      dueTime: toDateTimeString(current.dueTime),
      attachmentFileIds: [...existingIds, ...pendingIds]
    })
    pendingFiles.value = []
    return true
  } catch (e) {
    return false
  }
}

function handleDownload(file) {
  if (!file.fileId) {
    proxy.$modal.msgWarning('附件信息不完整，无法下载')
    return
  }
  downloadTaskFile(file.fileId, fileName(file)).catch(() => {
    proxy.$modal.msgError('附件下载失败')
  })
}

defineExpose({ open, loadDetail })
</script>

<style scoped lang="scss">
.drawer-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-right: 8px;
}

.drawer-head-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;

  .task-no {
    font-size: 13px;
    color: #909399;
    font-family: Consolas, Monaco, monospace;
    flex: 0 0 auto;
  }

  .task-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.readonly-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  margin: -20px -20px 16px;
  background: #f4f4f5;
  color: #909399;
  font-size: 13px;
  border-bottom: 1px solid #ebeef5;
}

.detail-skeleton {
  padding: 4px 0;
}

.detail-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 0;
  color: #909399;

  p {
    margin: 0;
    font-size: 14px;
  }
}

.detail-section {
  margin-bottom: 22px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f2f5;
  font-size: 14px;
  font-weight: 600;
  color: #303133;

  &::before {
    content: '';
    width: 3px;
    height: 13px;
    border-radius: 2px;
    background: #409eff;
  }

  small {
    font-size: 12px;
    font-weight: 400;
    color: #909399;
  }
}

.attr-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.attr-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;

  .attr-label {
    font-size: 12px;
    color: #909399;
  }

  .attr-value {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
    font-size: 13px;
    color: #303133;
    word-break: break-all;

    &.is-red {
      color: #f56c6c;
      font-weight: 600;
    }
  }
}

.archived-tag {
  padding: 0 5px;
  border-radius: 2px;
  background: #f4f4f5;
  color: #909399;
  font-size: 11px;
}

.desc-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;

  &.is-collapsed {
    display: -webkit-box;
    -webkit-line-clamp: 6;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.subtask-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.subtask-row {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 40px;
  padding: 6px 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;

  &.is-done {
    background: #f7fbf3;
    border-color: #e1f3d8;

    .st-title {
      color: #7fae5e;
      text-decoration: line-through;
    }
  }

  .st-title {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .st-assignee {
    flex: 0 0 auto;
    color: #909399;
    font-size: 12px;
  }

  .st-due {
    flex: 0 0 auto;
    width: 100px;
    text-align: right;
    color: #909399;
    font-size: 12px;

    &.is-red {
      color: #f56c6c;
      font-weight: 600;
    }
  }
}

.collaborator-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.member-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #f7f8fa;
  border-radius: 14px;
  font-size: 13px;
  color: #303133;
}

.attach-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}

.attach-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;

  .attach-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 4px;
    background: #ecf5ff;
    color: #409eff;
    flex: 0 0 32px;
  }

  .attach-main {
    flex: 1;
    min-width: 0;
  }

  .attach-name {
    font-size: 13px;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.pending-tag {
  margin-left: 6px;
  padding: 0 5px;
  border-radius: 2px;
  background: #fdf6ec;
  color: #e6a23c;
  font-size: 11px;
}

.upload-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

.timeline {
  max-height: 240px;
  overflow-y: auto;
  padding-left: 4px;
}

.tl-item {
  position: relative;
  padding: 0 0 18px 22px;
  border-left: 2px solid #ebeef5;

  &:last-child {
    border-left-color: transparent;
    padding-bottom: 0;
  }

  &::before {
    content: '';
    position: absolute;
    left: -7px;
    top: 2px;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #fff;
    border: 2px solid #dcdfe6;
  }

  &.is-highlight {
    &::before {
      border-color: #409eff;
      background: #409eff;
      box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.18);
    }

    .tl-title {
      color: #409eff;
    }
  }

  .tl-title {
    font-size: 13px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 3px;
  }

  .tl-meta {
    font-size: 12px;
    color: #909399;
    margin-bottom: 5px;
  }

  .tl-comment {
    padding: 7px 10px;
    border-radius: 4px;
    background: #f7f8fa;
    font-size: 12px;
    color: #606266;
    line-height: 1.6;
    word-break: break-word;
  }
}

.drawer-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 4px;

  .footer-hint {
    font-size: 12px;
    color: #f56c6c;
  }

  .footer-divider {
    width: 1px;
    height: 20px;
    margin: 0 4px;
    background: #dcdfe6;
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

.tag-chip {
  display: inline-block;
  padding: 1px 6px;
  border: 1px solid #e9e9eb;
  border-radius: 3px;
  background: #f4f4f5;
  color: #606266;
  font-size: 11px;
}

.avatar-xs {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  color: #fff;
  font-size: 10px;
  flex: 0 0 20px;
}

.badge-overdue {
  display: inline-flex;
  align-items: center;
  height: 18px;
  padding: 0 6px;
  border: 1px solid #fbc4c4;
  border-radius: 9px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 11px;
  white-space: nowrap;
}
</style>
