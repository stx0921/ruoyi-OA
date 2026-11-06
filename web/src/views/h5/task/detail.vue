<template>
  <H5TaskShell>
    <!-- ================= 顶部导航 ================= -->
    <header class="ht-nav">
      <button type="button" class="ht-nav__back" aria-label="返回" @click="goBack">‹</button>
      <span class="ht-nav__title">任务详情</span>
      <button type="button" class="ht-bell" aria-label="站内信" @click="openNotify">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8.5a6 6 0 1 0-12 0c0 5.6-2 6.8-2 6.8h16s-2-1.2-2-6.8" />
          <path d="M10.3 20a2 2 0 0 0 3.4 0" />
        </svg>
        <i v-if="unread > 0" class="ht-bell__badge">{{ unread > 99 ? '99+' : unread }}</i>
      </button>
    </header>

    <!-- 归档只读提示 -->
    <div v-if="!loading && task && readOnly" class="readonly-tip">项目已结束，任务只读</div>

    <div ref="scrollRef" class="ht-scroll">
      <!-- loading 骨架：头部 + 信息卡 -->
      <template v-if="loading">
        <div class="ht-skeleton">
          <div class="ht-skel-line ht-skel-line--short"></div>
          <div class="ht-skel-line ht-skel-line--wide"></div>
          <div class="ht-skel-line"></div>
        </div>
        <div class="ht-skeleton">
          <div class="ht-skel-line ht-skel-line--short"></div>
          <div class="ht-skel-line ht-skel-line--tall"></div>
        </div>
      </template>

      <!-- 错误 / 无权限态 -->
      <div v-else-if="errorType" class="ht-empty">
        <div class="ht-empty__ico">!</div>
        <div class="ht-empty__title">任务加载失败</div>
        <div class="ht-empty__sub">任务不存在，或没有访问该任务的权限（越权访问会被拦截）</div>
        <div class="empty-actions">
          <button type="button" class="ht-btn ht-btn--primary ht-btn--sm" @click="loadDetail">重试</button>
          <button type="button" class="ht-btn ht-btn--ghost-2 ht-btn--sm" @click="goBack">返回任务列表</button>
        </div>
      </div>

      <!-- ================= 正文 ================= -->
      <template v-else-if="task">
        <!-- 标题 + 状态 + 编号 -->
        <section class="ht-card detail-head">
          <div class="detail-head__no">{{ task.taskNo || '—' }}</div>
          <h1 class="detail-head__title">{{ task.title || '未命名任务' }}</h1>
          <div class="detail-head__tags">
            <span v-if="task.projectName" class="ht-proj">{{ task.projectName }}</span>
            <span
              class="ht-pill"
              :class="{ 'ht-pill--strike': status === '4' }"
              :style="statusStyle"
            >
              {{ statusMeta(task.status).label }}
            </span>
            <span class="ht-priority" :style="{ color: priorityMeta(task.priority).color }">
              <i :style="{ background: priorityMeta(task.priority).color }"></i>{{ priorityMeta(task.priority).label }}优先级
            </span>
            <span v-if="overdue" class="ht-overdue-badge">已逾期 {{ overdueDayCount }} 天</span>
            <span v-if="task.source === '1'" class="ht-pill ht-pill--gray ht-pill--sm">周期生成</span>
          </div>
        </section>

        <!-- 基本信息 -->
        <section class="ht-card">
          <div class="ht-sec">基本信息</div>
          <div class="info-grid">
            <div class="info-item">
              <span>项目</span>
              <b>{{ task.projectName || '未关联项目' }}</b>
            </div>
            <div class="info-item">
              <span>主责人</span>
              <b>{{ task.assigneeName || '—' }}<em v-if="task.assigneeDeptName"> · {{ task.assigneeDeptName }}</em></b>
            </div>
            <div class="info-item">
              <span>截止时间</span>
              <b :class="{ 'is-red': overdue }">{{ formatDateTime(task.dueTime) }}</b>
            </div>
            <div class="info-item">
              <span>优先级</span>
              <b :style="{ color: priorityMeta(task.priority).color }">{{ priorityMeta(task.priority).label }}</b>
            </div>
            <div class="info-item">
              <span>验收人</span>
              <b>{{ task.auditorName || '—' }}<em v-if="rejectCount > 0"> · 打回 {{ rejectCount }} 次</em></b>
            </div>
            <div class="info-item">
              <span>标签</span>
              <b>{{ tagsText }}</b>
            </div>
          </div>
          <div class="info-line"><span>派发人</span><b>{{ task.creatorName || '—' }}</b></div>
          <div v-if="collaborators.length" class="info-line"><span>协作人</span><b>{{ collaboratorNames }}</b></div>
          <div v-if="task.submitTime && status === '2'" class="info-line">
            <span>提交时间</span><b>{{ formatDateTime(task.submitTime) }}</b>
          </div>
          <div v-if="status === '3' && task.finishTime" class="info-line">
            <span>完成时间</span><b>{{ formatDateTime(task.finishTime) }}</b>
          </div>
        </section>

        <!-- 任务描述（>6 行折叠） -->
        <section v-if="descText" class="ht-card">
          <div class="ht-sec">
            任务描述
            <button v-if="descLong" type="button" class="ht-sec__link" @click="descExpanded = !descExpanded">
              {{ descExpanded ? '收起' : '展开' }}
            </button>
          </div>
          <div class="desc" :class="{ 'is-clamp': descLong && !descExpanded }">{{ descText }}</div>
        </section>

        <!-- 子任务（无子任务不渲染） -->
        <section v-if="children.length" ref="subtaskRef" class="ht-card">
          <div class="ht-sec">
            子任务
            <span class="ht-sec__tip ht-sec__tip--auto">{{ childDone }}/{{ children.length }} 已完成</span>
          </div>
          <div class="subtask-list">
            <label
              v-for="child in children"
              :key="child.taskId"
              class="subtask"
              :class="{ 'is-done': String(child.status) === '3', 'is-locked': !canCheckChild(child) }"
            >
              <input
                class="subtask__check"
                type="checkbox"
                :checked="String(child.status) === '3'"
                :disabled="!canCheckChild(child)"
                @change="onChildCheck(child, $event)"
              />
              <span class="subtask__main">
                <span class="subtask__title">{{ child.title || '未命名子任务' }}</span>
                <span class="subtask__meta">
                  {{ child.assigneeName || '—' }}
                  <template v-if="child.dueTime"> · {{ formatDueTime(child.dueTime) }}</template>
                </span>
              </span>
              <span
                class="ht-pill ht-pill--sm"
                :class="{ 'ht-pill--strike': String(child.status) === '4' }"
                :style="statusStyleOf(child.status)"
              >
                {{ statusMeta(child.status).label }}
              </span>
            </label>
          </div>
          <div class="ht-sec__hint">
            {{ childAllDone
              ? '子任务已全部完成，可以提交完成'
              : `还有 ${children.length - childDone} 个子任务未完成，全部验收后才能提交完成` }}
          </div>
        </section>

        <!-- 附件：拍照 / 相册 / 文件 -->
        <section class="ht-card">
          <div class="ht-sec">
            附件
            <span class="ht-sec__tip ht-sec__tip--auto">{{ canUpload ? '单个文件 ≤10MB' : '只读' }}</span>
          </div>

          <div v-if="fileList.length" class="file-list">
            <div v-for="file in fileList" :key="file.key" class="file-item" :class="{ 'is-uploading': file.uploading }">
              <span class="file-item__ico" :class="{ 'is-image': file.isImage }">{{ file.isImage ? '图' : '文' }}</span>
              <span class="file-item__main" @click="openFile(file)">
                <span class="file-item__name">{{ file.name }}</span>
                <span class="file-item__meta">{{ file.uploading ? '上传中…' : file.sizeText }}</span>
              </span>
              <button
                v-if="!file.server && !file.uploading"
                type="button"
                class="file-item__del"
                aria-label="移除附件"
                @click.stop="removeUpload(file)"
              >
                ×
              </button>
              <span v-if="file.uploading" class="file-item__bar"><i :style="{ width: file.progress + '%' }"></i></span>
            </div>
          </div>
          <div v-else class="ht-hint">未上传附件</div>

          <div v-if="canUpload" class="upload-row">
            <button type="button" class="upload-entry" @click="pick('camera')">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 8h3l1.4-2h7.2L17 8h3v11H4z" />
                <circle cx="12" cy="13" r="3.2" />
              </svg>
              <span>拍照</span>
            </button>
            <button type="button" class="upload-entry" @click="pick('album')">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                <rect x="4" y="5" width="16" height="14" rx="2" />
                <path d="M4.5 16l4.2-4.2 3.3 3.3 2.4-2.4 5.1 5.1" />
                <circle cx="9" cy="9.2" r="1.3" />
              </svg>
              <span>相册</span>
            </button>
            <button type="button" class="upload-entry" @click="pick('doc')">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                <path d="M13.5 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8.5z" />
                <path d="M13.5 3v5.5H19" />
              </svg>
              <span>文件</span>
            </button>
          </div>
          <input ref="cameraInput" class="ht-file-input" type="file" accept="image/*" capture="environment" @change="onPicked" />
          <input ref="albumInput" class="ht-file-input" type="file" accept="image/*" @change="onPicked" />
          <input
            ref="docInput"
            class="ht-file-input"
            type="file"
            accept=".jpg,.jpeg,.png,.pdf,.docx,.xlsx"
            @change="onPicked"
          />
          <div v-if="canUpload" class="ht-hint">支持 jpg / png / pdf / docx / xlsx；上传后在「提交完成 / 验收通过」时一并提交</div>
        </section>

        <!-- 流转记录：默认折叠 -->
        <section class="ht-card">
          <button type="button" class="ht-sec ht-sec--toggle" @click="logsExpanded = !logsExpanded">
            {{ logsExpanded ? '流转记录' : '查看流转记录' }}（{{ logs.length }}）
            <span class="ht-caret" :class="{ 'is-open': logsExpanded }">›</span>
          </button>
          <template v-if="logsExpanded">
            <div v-if="logs.length" class="timeline">
              <div v-for="log in logs" :key="log.logId" class="timeline__item">
                <i class="timeline__dot" :class="logDotClass(log)"></i>
                <div class="timeline__main">
                  <div class="timeline__title">
                    <b>{{ log.operatorName || '系统' }}</b>
                    <span>{{ logActionText(log) }}</span>
                  </div>
                  <div v-if="log.remark" class="timeline__remark">{{ log.remark }}</div>
                  <div class="timeline__time">{{ formatDateTime(log.createTime) }}</div>
                </div>
              </div>
            </div>
            <div v-else class="ht-hint">暂无流转记录</div>
          </template>
        </section>

        <div class="detail-foot"></div>
      </template>
    </div>

    <!-- ================= 底部固定操作条 ================= -->
    <template v-if="!loading && task && hasBar">
      <div v-if="!readOnly && canSubmitNow && !childAllDone" class="ht-hint-bar">
        子任务未全部完成（{{ childDone }}/{{ children.length }}），「提交完成」暂不可用
      </div>
      <div class="ht-action-bar">
        <template v-if="readOnly">
          <span class="ht-action-bar__readonly">项目已结束，任务只读</span>
        </template>
        <template v-else-if="showAuditActions">
          <button type="button" class="ht-btn ht-btn--danger-ghost" :disabled="!!busy" @click="openReject">打回</button>
          <button type="button" class="ht-btn ht-btn--primary" :disabled="!!busy" @click="doPass">
            {{ busy === 'pass' ? '处理中…' : '验收通过' }}
          </button>
        </template>
        <template v-else-if="showOwnerActions">
          <button v-if="canStartNow" type="button" class="ht-btn ht-btn--primary" :disabled="!!busy" @click="doStart">
            {{ busy === 'start' ? '处理中…' : '开始' }}
          </button>
          <button v-if="canSubmitNow" type="button" class="ht-btn ht-btn--primary" :disabled="!!busy" @click="doSubmit">
            {{ busy === 'submit' ? '提交中…' : '提交完成' }}
          </button>
        </template>
        <template v-else>
          <span class="ht-action-bar__readonly">{{ finishedHint }}</span>
        </template>
      </div>
    </template>

    <!-- ================= 打回底部弹层（6 折） ================= -->
    <div v-if="rejectVisible" class="ht-sheet-mask" @click.self="closeReject">
      <section class="ht-sheet-panel ht-sheet-panel--half">
        <header class="ht-sheet-head">
          <span class="ht-sheet-head__title">打回任务</span>
          <button type="button" class="ht-sheet-head__close" aria-label="关闭" @click="closeReject">×</button>
        </header>
        <div class="ht-sheet-body">
          <label class="ht-label"><span class="ht-req">*</span>打回理由<small>{{ rejectLength }}/200</small></label>
          <textarea
            v-model="rejectReason"
            class="ht-textarea"
            maxlength="200"
            placeholder="例如：排班表未覆盖志愿者分组，请补齐 A / B / C 组后重新提交"
            @input="rejectError = ''"
          ></textarea>
          <div class="ht-chips">
            <button v-for="reason in REJECT_REASONS" :key="reason" type="button" class="ht-chip" @click="appendReason(reason)">
              {{ reason }}
            </button>
          </div>
          <div class="ht-hint">理由将同步通知主责人；打回后任务回到「进行中」，打回次数 +1</div>
          <div v-if="rejectError" class="ht-err">{{ rejectError }}</div>
        </div>
        <div class="ht-sheet-foot">
          <button type="button" class="ht-btn ht-btn--ghost-2" :disabled="busy === 'reject'" @click="closeReject">取消</button>
          <button
            type="button"
            class="ht-btn ht-btn--danger"
            :disabled="busy === 'reject' || !rejectValid"
            @click="doReject"
          >
            {{ busy === 'reject' ? '处理中…' : '确认打回' }}
          </button>
        </div>
      </section>
    </div>

    <!-- ================= 图片附件预览 ================= -->
    <div v-if="previewUrl" class="preview-mask" @click="closePreview">
      <img class="preview-mask__img" :src="previewUrl" :alt="previewName || '附件预览'" />
      <span class="preview-mask__name">{{ previewName }}</span>
      <span class="preview-mask__tip">点击任意位置关闭</span>
    </div>

    <!-- ================= 站内信半屏弹层 ================= -->
    <H5NotifySheet ref="notifyRef" @unread="onUnread" />
  </H5TaskShell>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTask, startTask, submitTask, auditTask, uploadTaskFile, getTaskFileBlob } from '@/api/oa/task'
import {
  REJECT_REASONS,
  canAudit,
  canStart,
  canSubmit,
  fileName as fileDisplayName,
  formatDateTime,
  formatDueTime,
  formatFileSize,
  isOverdue,
  isReadOnly,
  normalizeTags,
  overdueDays,
  priorityMeta,
  statusMeta,
  toDate
} from '@/utils/oaTask'
import useUserStore from '@/store/modules/user'
import H5TaskShell from './components/H5TaskShell.vue'
import H5NotifySheet from './components/H5NotifySheet.vue'

defineOptions({ name: 'H5TaskDetail' })

const ALLOWED_EXT = ['jpg', 'jpeg', 'png', 'pdf', 'docx', 'xlsx']
const MAX_FILE_SIZE = 10 * 1024 * 1024
const LOG_ACTION_LABEL = {
  create: '创建任务',
  update: '编辑任务',
  start: '开始执行',
  submit: '提交完成',
  audit_pass: '验收通过',
  audit_reject: '打回',
  cancel: '取消任务',
  reassign: '改派',
  recur_generate: '周期生成',
  child_create: '创建子任务',
  retire_cancel: '成员退会自动取消'
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const taskId = computed(() => {
  const raw = route.params ? route.params.id : ''
  const num = Number(raw)
  return isNaN(num) || num <= 0 ? 0 : num
})

const task = ref(null)
const loading = ref(true)
const errorType = ref('')
const busy = ref('')
const unread = ref(0)
const descExpanded = ref(false)
const logsExpanded = ref(false)
const scrollRef = ref(null)
const subtaskRef = ref(null)
const notifyRef = ref(null)

/* 打回弹层 */
const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectError = ref('')

/* 附件上传 */
const cameraInput = ref(null)
const albumInput = ref(null)
const docInput = ref(null)
const uploads = ref([])

/* 图片预览 */
const previewUrl = ref('')
const previewName = ref('')

const status = computed(() => String((task.value && task.value.status) || ''))
const readOnly = computed(() => isReadOnly(task.value))
const children = computed(() => (Array.isArray(task.value && task.value.children) ? task.value.children : []))
const collaborators = computed(() =>
  Array.isArray(task.value && task.value.collaborators) ? task.value.collaborators : []
)
const logs = computed(() => {
  const list = Array.isArray(task.value && task.value.logs) ? [...task.value.logs] : []
  return list.sort((a, b) => {
    const left = toDate(a && a.createTime)
    const right = toDate(b && b.createTime)
    return (right ? right.getTime() : 0) - (left ? left.getTime() : 0)
  })
})
const tagsText = computed(() => {
  const list = normalizeTags(task.value && task.value.tags)
  return list.length ? list.join('、') : '—'
})
const collaboratorNames = computed(() =>
  collaborators.value
    .map(item => item.userName || item.nickName || '')
    .filter(Boolean)
    .join('、')
)
const childDone = computed(() => children.value.filter(child => String(child.status) === '3').length)
const childAllDone = computed(() => !children.value.length || childDone.value === children.value.length)
const overdue = computed(() => isOverdue(task.value))
const overdueDayCount = computed(() => overdueDays(task.value))
const rejectCount = computed(() => Number(task.value && task.value.rejectCount) || 0)
const statusStyle = computed(() => statusStyleOf(task.value && task.value.status))
const canStartNow = computed(() => !readOnly.value && status.value === '0' && canStart(task.value))
const canSubmitNow = computed(() => !readOnly.value && status.value === '1' && canSubmit(task.value))
const canAuditNow = computed(() => !readOnly.value && status.value === '2' && canAudit(task.value))
const canUpload = computed(
  () => !readOnly.value && (canStartNow.value || canSubmitNow.value || canAuditNow.value)
)
const showAuditActions = computed(() => canAuditNow.value)
const showOwnerActions = computed(() => !readOnly.value && (canStartNow.value || canSubmitNow.value))
const hasBar = computed(
  () =>
    readOnly.value ||
    showAuditActions.value ||
    showOwnerActions.value ||
    status.value === '3' ||
    status.value === '4'
)
const finishedHint = computed(() => {
  if (status.value === '3') {
    return task.value && task.value.finishTime
      ? `任务已完成（完成时间 ${formatDateTime(task.value.finishTime)}）`
      : '任务已完成'
  }
  if (status.value === '4') return '任务已取消'
  return ''
})
const descText = computed(() => String((task.value && task.value.description) || '').trim())
const descLong = computed(() => descText.value.length > 120)
const rejectLength = computed(() => rejectReason.value.trim().length)
const rejectValid = computed(() => rejectLength.value >= 2 && rejectLength.value <= 200)
const pendingFileIds = computed(() => uploads.value.filter(item => item.fileId).map(item => item.fileId))
const fileList = computed(() => {
  const serverFiles = (Array.isArray(task.value && task.value.files) ? task.value.files : []).map(file => ({
    key: `s-${file.fileId}`,
    fileId: file.fileId,
    name: fileDisplayName(file),
    sizeText: formatFileSize(file.fileSize),
    isImage: isImageName(fileDisplayName(file)),
    uploading: false,
    progress: 100,
    server: true
  }))
  const localFiles = uploads.value.map(item => ({
    key: item.key,
    fileId: item.fileId,
    name: item.fileName,
    sizeText: formatFileSize(item.fileSize),
    isImage: isImageName(item.fileName),
    uploading: item.uploading,
    progress: item.progress,
    server: false
  }))
  return serverFiles.concat(localFiles)
})

onMounted(() => {
  loadDetail()
})

onBeforeUnmount(() => {
  closePreview()
})

/* -------------------------------------------------------------------------- */
/* 详情加载                                                                    */
/* -------------------------------------------------------------------------- */
async function loadDetail() {
  if (!taskId.value) {
    loading.value = false
    errorType.value = 'notfound'
    return
  }
  loading.value = true
  errorType.value = ''
  try {
    const res = await getTask(taskId.value)
    const data = res && res.data !== undefined && res.data !== null ? res.data : res
    if (!data || (!data.taskId && !data.title)) throw new Error('empty task')
    task.value = data
  } catch (e) {
    task.value = null
    errorType.value = 'load'
  } finally {
    loading.value = false
  }
}

/* -------------------------------------------------------------------------- */
/* 状态与权限                                                                  */
/* -------------------------------------------------------------------------- */
function statusStyleOf(value) {
  const meta = statusMeta(value) || {}
  return {
    color: meta.color || '#909399',
    background: meta.bg || '#F4F4F5',
    borderColor: meta.border || '#D3D4D6'
  }
}

function canCheckChild(child) {
  if (!child || readOnly.value) return false
  if (String(child.status) !== '2') return false
  if (typeof child.canAudit === 'boolean') return child.canAudit
  const userId = userStore.id
  if (!userId) return false
  return child.auditorId !== undefined && child.auditorId !== null && String(child.auditorId) === String(userId)
}

/* -------------------------------------------------------------------------- */
/* 操作：开始 / 提交 / 验收通过 / 打回                                          */
/* -------------------------------------------------------------------------- */
async function doStart() {
  if (busy.value) return
  busy.value = 'start'
  try {
    await startTask({ taskId: taskId.value })
    await loadDetail()
    ElMessage.success('已开始任务')
  } catch (e) {
    // 错误提示由 utils/request.js 统一处理
  } finally {
    busy.value = ''
  }
}

async function doSubmit() {
  if (busy.value || !task.value) return
  if (!childAllDone.value) {
    ElMessage.warning(`还有 ${children.value.length - childDone.value} 个子任务未完成`)
    scrollToSubtasks()
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认提交完成？提交后由 ${task.value.auditorName || '验收人'} 验收`,
      '提交完成',
      {
        confirmButtonText: '确认提交',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'ht-message-box'
      }
    )
  } catch (e) {
    return
  }
  busy.value = 'submit'
  try {
    await submitTask({ taskId: taskId.value, attachmentFileIds: pendingFileIds.value })
    uploads.value = []
    await loadDetail()
    ElMessage.success(`已提交，等待 ${(task.value && task.value.auditorName) || '验收人'} 验收`)
  } catch (e) {
    // 错误提示由 utils/request.js 统一处理，保持当前页
  } finally {
    busy.value = ''
  }
}

async function doPass() {
  if (busy.value || !task.value) return
  try {
    await ElMessageBox.confirm('确认验收通过？将写入实际完成时间并通知主责人', '验收通过', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'ht-message-box'
    })
  } catch (e) {
    return
  }
  busy.value = 'pass'
  try {
    await auditTask({ taskId: taskId.value, pass: true, finishFileIds: pendingFileIds.value })
    uploads.value = []
    await loadDetail()
    const finishText = task.value && task.value.finishTime ? formatDateTime(task.value.finishTime) : ''
    ElMessage.success(finishText ? `已验收通过，完成时间 ${finishText}` : '已验收通过')
  } catch (e) {
    // 错误提示由 utils/request.js 统一处理
  } finally {
    busy.value = ''
  }
}

function openReject() {
  if (busy.value) return
  rejectReason.value = ''
  rejectError.value = ''
  rejectVisible.value = true
}

function closeReject() {
  if (busy.value === 'reject') return
  rejectVisible.value = false
}

function appendReason(text) {
  const current = rejectReason.value.trim()
  if (!current) {
    rejectReason.value = text
  } else if (!current.includes(text)) {
    rejectReason.value = `${current}，${text}`
  }
  rejectError.value = ''
}

async function doReject() {
  if (busy.value === 'reject') return
  const reason = rejectReason.value.trim()
  if (!reason) {
    rejectError.value = '请填写打回理由'
    return
  }
  if (reason.length < 2 || reason.length > 200) {
    rejectError.value = '打回理由需为 2–200 字'
    return
  }
  busy.value = 'reject'
  try {
    await auditTask({ taskId: taskId.value, pass: false, remark: reason })
    rejectVisible.value = false
    uploads.value = []
    await loadDetail()
    ElMessage.success('已打回，主责人将收到通知')
  } catch (e) {
    // 错误提示由 utils/request.js 统一处理，保留理由
  } finally {
    busy.value = ''
  }
}

/* 子任务快捷验收（仅子任务验收人 + 待验收时可用，规则同 PC-04） */
async function onChildCheck(child, event) {
  const target = event && event.target
  const reset = () => {
    if (target) target.checked = String(child.status) === '3'
  }
  if (!canCheckChild(child)) {
    reset()
    return
  }
  const title = child.title || '该子任务'
  try {
    await ElMessageBox.confirm(`确认验收通过子任务「${title}」？`, '子任务验收', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'ht-message-box'
    })
  } catch (e) {
    reset()
    return
  }
  try {
    await auditTask({ taskId: child.taskId, pass: true })
    ElMessage.success('已验收通过子任务')
    await loadDetail()
  } catch (e) {
    reset()
  }
}

function scrollToSubtasks() {
  nextTick(() => {
    if (subtaskRef.value && typeof subtaskRef.value.scrollIntoView === 'function') {
      subtaskRef.value.scrollIntoView({ block: 'start', behavior: 'smooth' })
    }
  })
}

/* -------------------------------------------------------------------------- */
/* 附件上传 / 预览                                                             */
/* -------------------------------------------------------------------------- */
function pick(which) {
  if (!canUpload.value) {
    ElMessage.warning('当前任务状态不可上传附件')
    return
  }
  const map = { camera: cameraInput, album: albumInput, doc: docInput }
  const input = map[which]
  if (input && input.value) input.value.click()
}

function onPicked(event) {
  const input = event && event.target
  const file = input && input.files && input.files[0]
  if (input) input.value = ''
  if (!file) return
  const ext = String(file.name || '').split('.').pop().toLowerCase()
  if (!ALLOWED_EXT.includes(ext)) {
    ElMessage.error('仅支持 jpg / png / pdf / docx / xlsx')
    return
  }
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.error('单个文件不能超过 10MB')
    return
  }
  const item = {
    key: `u-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    fileName: file.name,
    fileSize: file.size,
    fileId: null,
    uploading: true,
    progress: 20
  }
  uploads.value = uploads.value.concat([item])
  const tick = setInterval(() => {
    const hit = uploads.value.find(upload => upload.key === item.key)
    if (!hit || !hit.uploading) return
    hit.progress = Math.min(92, hit.progress + 9)
  }, 320)
  uploadTaskFile(file)
    .then(res => {
      const data = res && res.data !== undefined ? res.data : res
      const fileId = data && data.fileId
      if (!fileId) throw new Error('upload failed')
      const hit = uploads.value.find(upload => upload.key === item.key)
      if (hit) {
        hit.fileId = fileId
        hit.uploading = false
        hit.progress = 100
      }
      ElMessage.success('附件已上传')
    })
    .catch(() => {
      uploads.value = uploads.value.filter(upload => upload.key !== item.key)
    })
    .finally(() => clearInterval(tick))
}

function removeUpload(file) {
  if (!file || file.server || file.uploading) return
  uploads.value = uploads.value.filter(item => item.key !== file.key)
}

async function openFile(file) {
  if (!file || file.uploading) return
  try {
    const blob = await getTaskFileBlob(file.fileId)
    if (!(blob instanceof Blob) || (blob.type && blob.type.indexOf('json') >= 0)) {
      throw new Error('bad blob')
    }
    const url = URL.createObjectURL(blob)
    if (file.isImage) {
      closePreview()
      previewUrl.value = url
      previewName.value = file.name
      return
    }
    const link = document.createElement('a')
    link.href = url
    link.download = file.name || '附件'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    setTimeout(() => URL.revokeObjectURL(url), 5000)
  } catch (e) {
    ElMessage.error('附件下载失败，请稍后重试')
  }
}

function closePreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  previewName.value = ''
}

/* -------------------------------------------------------------------------- */
/* 展示辅助                                                                    */
/* -------------------------------------------------------------------------- */
function isImageName(name) {
  const ext = String(name || '').split('.').pop().toLowerCase()
  return ['jpg', 'jpeg', 'png'].includes(ext)
}

function logActionText(log) {
  if (!log) return '—'
  if (log.actionLabel) return log.actionLabel
  const key = String(log.action || '')
  return LOG_ACTION_LABEL[key] || key || '—'
}

function logDotClass(log) {
  const key = String((log && log.action) || '')
  if (/reject|cancel|打回|取消/.test(key)) return 'is-red'
  if (/pass|finish|验收通过|完成/.test(key)) return 'is-green'
  return ''
}

/* -------------------------------------------------------------------------- */
/* 导航 / 站内信                                                               */
/* -------------------------------------------------------------------------- */
function goBack() {
  const historyState = window.history.state
  if (historyState && historyState.back) {
    router.back()
    return
  }
  router.replace('/h5/task')
}

function openNotify() {
  if (notifyRef.value && typeof notifyRef.value.open === 'function') notifyRef.value.open()
}

function onUnread(value) {
  unread.value = Number(value) || 0
}
</script>

<style lang="scss" scoped>
/* ---------- 归档只读提示 ---------- */
.readonly-tip {
  flex: 0 0 auto;
  padding: 8px 14px;
  border-bottom: 1px solid #ebeef5;
  background: #f4f4f5;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
  text-align: center;
}

/* ---------- 标题卡 ---------- */
.detail-head {
  margin-top: 10px;

  &__no {
    margin-bottom: 6px;
    color: #909399;
    font-family: Consolas, Menlo, monospace;
    font-size: 11.5px;
    letter-spacing: 0.3px;
  }

  &__title {
    display: -webkit-box;
    overflow: hidden;
    margin: 0 0 10px;
    color: #303133;
    font-size: 17px;
    font-weight: 600;
    line-height: 1.5;
    word-break: break-all;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    line-clamp: 2;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
  }
}

/* ---------- 基本信息 ---------- */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.info-item {
  min-width: 0;
  padding: 8px 10px;
  border-radius: 6px;
  background: #f7f9fc;
  font-size: 12.5px;
  line-height: 1.5;

  span {
    display: block;
    margin-bottom: 3px;
    color: #909399;
    font-size: 11px;
  }

  b {
    display: block;
    overflow: hidden;
    color: #303133;
    font-weight: 500;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  b.is-red {
    color: #f56c6c;
    font-weight: 600;
  }

  em {
    color: #909399;
    font-style: normal;
    font-size: 11px;
  }
}

.info-line {
  display: flex;
  gap: 10px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #f0f2f5;
  font-size: 12.5px;
  line-height: 1.6;

  span {
    flex: 0 0 52px;
    color: #909399;
  }

  b {
    flex: 1;
    min-width: 0;
    color: #303133;
    font-weight: 400;
    word-break: break-all;
  }
}

/* ---------- 描述折叠 ---------- */
.desc {
  color: #606266;
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-all;

  &.is-clamp {
    display: -webkit-box;
    overflow: hidden;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 6;
    line-clamp: 6;
  }
}

/* ---------- 子任务 ---------- */
.subtask-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.subtask {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 11px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;

  &__check {
    flex: 0 0 18px;
    width: 18px;
    height: 18px;
    margin: 0;
    accent-color: #236de9;
  }

  &__main {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__title {
    color: #303133;
    line-height: 1.5;
    word-break: break-all;
  }

  &__meta {
    color: #909399;
    font-size: 11px;
    line-height: 1.5;
  }

  &.is-done {
    border-color: #e1f3d8;
    background: #f7fbf3;

    .subtask__title {
      color: #7fae5e;
      text-decoration: line-through;
    }
  }

  &.is-locked {
    opacity: 0.9;
  }
}

/* ---------- 附件 ---------- */
.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.file-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 11px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafbfd;
  overflow: hidden;

  &__ico {
    display: flex;
    flex: 0 0 30px;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border-radius: 6px;
    background: #edf3fe;
    color: #236de9;
    font-size: 11.5px;
    font-weight: 600;

    &.is-image {
      background: #f0f9eb;
      color: #67c23a;
    }
  }

  &__main {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    cursor: pointer;
  }

  &__name {
    overflow: hidden;
    color: #303133;
    font-size: 12.5px;
    line-height: 1.5;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    color: #909399;
    font-size: 11px;
    line-height: 1.4;
  }

  &__del {
    flex: 0 0 22px;
    width: 22px;
    height: 22px;
    padding: 0;
    border: none;
    border-radius: 50%;
    background: #f4f4f5;
    color: #909399;
    font-size: 14px;
    line-height: 1;
    cursor: pointer;
  }

  &__bar {
    position: absolute;
    right: 0;
    bottom: 0;
    left: 0;
    height: 2px;
    background: #ebeef5;

    i {
      display: block;
      height: 100%;
      background: #236de9;
      transition: width 0.2s;
    }
  }
}

.upload-row {
  display: flex;
  gap: 10px;
}

.upload-entry {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  min-height: 68px;
  padding: 8px;
  border: 1px dashed #c6d3e2;
  border-radius: 8px;
  background: #fafbfd;
  color: #909399;
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;

  svg {
    color: #a8bdd6;
  }

  &:active {
    border-color: #236de9;
    color: #236de9;

    svg {
      color: #236de9;
    }
  }
}

.ht-file-input {
  display: none;
}

/* ---------- 流转时间轴 ---------- */
.timeline {
  margin-top: 12px;

  &__item {
    position: relative;
    display: flex;
    gap: 10px;
    padding-bottom: 14px;

    &:last-child {
      padding-bottom: 0;
    }

    &::before {
      content: '';
      position: absolute;
      top: 12px;
      bottom: 0;
      left: 4px;
      width: 1px;
      background: #ebeef5;
    }

    &:last-child::before {
      display: none;
    }
  }

  &__dot {
    position: relative;
    z-index: 1;
    flex: 0 0 9px;
    width: 9px;
    height: 9px;
    margin-top: 4px;
    border-radius: 50%;
    background: #236de9;

    &.is-green {
      background: #67c23a;
    }

    &.is-red {
      background: #f56c6c;
    }
  }

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__title {
    color: #303133;
    font-size: 12.5px;
    line-height: 1.6;

    b {
      margin-right: 6px;
      font-weight: 600;
    }
  }

  &__remark {
    margin-top: 3px;
    padding: 6px 8px;
    border-left: 2px solid #fbc4c4;
    border-radius: 3px;
    background: #fef0f0;
    color: #606266;
    font-size: 12px;
    line-height: 1.6;
    word-break: break-all;
  }

  &__time {
    margin-top: 3px;
    color: #c0c4cc;
    font-size: 11px;
    line-height: 1.4;
  }
}

/* ---------- 错误态按钮组 ---------- */
.empty-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

/* ---------- 图片预览 ---------- */
.preview-mask {
  position: absolute;
  inset: 0;
  z-index: 95;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px;
  background: rgba(0, 0, 0, 0.86);

  &__img {
    max-width: 100%;
    max-height: 72%;
    border-radius: 6px;
    object-fit: contain;
  }

  &__name {
    color: #fff;
    font-size: 13px;
    line-height: 1.6;
    text-align: center;
    word-break: break-all;
  }

  &__tip {
    color: rgba(255, 255, 255, 0.6);
    font-size: 11.5px;
  }
}

/* ---------- 底部留白 ---------- */
.detail-foot {
  height: calc(10px + env(safe-area-inset-bottom));
}
</style>
