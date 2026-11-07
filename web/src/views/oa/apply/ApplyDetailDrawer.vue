<template>
  <el-drawer
    v-model="visible"
    size="620px"
    append-to-body
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <template #header>
      <div class="oa-drawer-head">
        <span class="oa-strong">申请详情 · {{ detail.name || rowApply.name || '—' }}</span>
        <span v-if="statusText" class="oa-pill" :class="'oa-pill--' + statusTone">{{ statusText }}</span>
      </div>
    </template>

    <div v-loading="loading" class="oa-apply-detail">
      <!-- 错误态 -->
      <div v-if="loadError" class="oa-load-error">
        <el-icon><WarningFilled /></el-icon>
        <p>申请详情加载失败</p>
        <div class="oa-load-error__actions">
          <el-button type="primary" plain @click="loadDetail">重试</el-button>
          <el-button @click="visible = false">关闭</el-button>
        </div>
      </div>

      <template v-else>
        <!-- 申请信息 -->
        <section class="oa-section">
          <h4 class="oa-section__title">
            申请信息
            <small>申请编号 {{ detail.applyNo || rowApply.applyNo || '—' }} · 提交于 {{ oaDateTime(detail.createTime || rowApply.createTime) }}</small>
          </h4>
          <div class="oa-kv">
            <div class="oa-kv__item">
              <span class="oa-kv__label">姓名</span>
              <div class="oa-kv__value">{{ detail.name || rowApply.name || '—' }}</div>
            </div>
            <div class="oa-kv__item">
              <span class="oa-kv__label">性别</span>
              <div class="oa-kv__value">{{ genderText(detail.gender) }}</div>
            </div>
            <div class="oa-kv__item">
              <span class="oa-kv__label">学号</span>
              <div class="oa-kv__value">{{ detail.studentNo || rowApply.studentNo || '—' }}</div>
            </div>
            <div class="oa-kv__item">
              <span class="oa-kv__label">年级</span>
              <div class="oa-kv__value">{{ detail.gradeLabel || gradeText(detail.grade) }}</div>
            </div>
            <div class="oa-kv__item">
              <span class="oa-kv__label">学院</span>
              <div class="oa-kv__value">{{ detail.college || '—' }}</div>
            </div>
            <div class="oa-kv__item">
              <span class="oa-kv__label">专业</span>
              <div class="oa-kv__value">{{ detail.major || '—' }}</div>
            </div>
            <div class="oa-kv__item">
              <span class="oa-kv__label">班级</span>
              <div class="oa-kv__value">{{ detail.className || '—' }}</div>
            </div>
            <div class="oa-kv__item">
              <span class="oa-kv__label">意向部门</span>
              <div class="oa-kv__value">{{ detail.deptName || rowApply.deptName || '—' }}</div>
            </div>
            <div class="oa-kv__item">
              <span class="oa-kv__label">是否服从调剂</span>
              <div class="oa-kv__value">{{ adjustText }}</div>
            </div>
            <div class="oa-kv__item">
              <span class="oa-kv__label">手机号</span>
              <div class="oa-kv__value">
                <oa-sensitive-text
                  :value="detail.phone || rowApply.phone"
                  :masked="isMaskedValue(detail.phone || rowApply.phone)"
                  copyable
                />
              </div>
            </div>
            <div class="oa-kv__item">
              <span class="oa-kv__label">重提次数</span>
              <div class="oa-kv__value">{{ detail.retryCount === null || detail.retryCount === undefined ? 0 : detail.retryCount }}</div>
            </div>
            <div class="oa-kv__item is-full">
              <span class="oa-kv__label">特长</span>
              <div class="oa-kv__value oa-kv__value--block">{{ detail.skills || '—' }}</div>
            </div>
            <div class="oa-kv__item is-full">
              <span class="oa-kv__label">自我介绍</span>
              <div class="oa-kv__value oa-kv__value--block">{{ detail.intro || '—' }}</div>
            </div>
          </div>
        </section>

        <!-- 审批结论（已处理时展示） -->
        <section v-if="detail.status !== undefined && String(detail.status) !== '0'" class="oa-section">
          <h4 class="oa-section__title">审批结论</h4>
          <div class="oa-kv">
            <div class="oa-kv__item">
              <span class="oa-kv__label">审批人</span>
              <div class="oa-kv__value">{{ detail.auditUserName || '—' }}</div>
            </div>
            <div class="oa-kv__item">
              <span class="oa-kv__label">审批时间</span>
              <div class="oa-kv__value">{{ oaDateTime(detail.auditTime) }}</div>
            </div>
            <div class="oa-kv__item is-full">
              <span class="oa-kv__label">审批意见</span>
              <div class="oa-kv__value oa-kv__value--block">{{ detail.auditRemark || '—' }}</div>
            </div>
          </div>
        </section>

        <!-- 附件材料 -->
        <section class="oa-section">
          <h4 class="oa-section__title">附件材料 <small>鉴权下载，不返回裸 URL</small></h4>
          <div v-if="attachments.length" class="oa-attach">
            <div v-for="att in attachments" :key="att.key" class="oa-attach__item">
              <div class="oa-attach__thumb" @click="downloadAttachment(att)">
                <img v-if="att.previewUrl" :src="att.previewUrl" :alt="att.label" />
                <div v-else class="oa-attach__ph">
                  <el-icon><Document /></el-icon>
                  <span>{{ att.failed ? '加载失败' : (att.ext || '文件').toUpperCase() }}</span>
                </div>
              </div>
              <div class="oa-attach__meta">
                <span class="oa-attach__name">{{ att.label }}{{ att.ext ? '.' + att.ext : '' }}</span>
                <el-button link type="primary" icon="Download" @click="downloadAttachment(att)">下载附件</el-button>
              </div>
            </div>
          </div>
          <div v-else class="oa-muted">未上传</div>
        </section>

        <!-- 审批时间轴（正序） -->
        <section class="oa-section">
          <h4 class="oa-section__title">审批时间轴</h4>
          <div v-if="timelineLogs.length" class="oa-tl">
            <div
              v-for="(log, index) in timelineLogs"
              :key="log.logId || index"
              class="oa-tl__item"
              :class="'is-' + logTone(log.action)"
            >
              <div class="oa-tl__axis">
                <span class="oa-tl__dot"></span>
                <span v-if="index < timelineLogs.length - 1" class="oa-tl__line"></span>
              </div>
              <div class="oa-tl__body">
                <div class="oa-tl__title">{{ log.action || '操作' }}</div>
                <div class="oa-tl__meta">
                  {{ log.operatorName || '系统' }} · {{ oaDateTime(log.createTime) }}
                </div>
                <div v-if="log.remark" class="oa-tl__comment">{{ log.remark }}</div>
              </div>
            </div>
          </div>
          <div v-else class="oa-empty">暂无审批记录</div>
        </section>
      </template>
    </div>

    <template #footer>
      <div class="oa-drawer-foot">
        <el-button v-if="canViewMember" link type="primary" @click="viewMember">查看成员</el-button>
        <span class="oa-drawer-foot__spacer"></span>
        <el-button @click="visible = false">关 闭</el-button>
        <template v-if="canAuditCurrent">
          <el-button type="danger" plain @click="emit('reject', detail)">驳 回</el-button>
          <el-button type="success" @click="emit('approve', detail)">同 意</el-button>
        </template>
      </div>
    </template>
  </el-drawer>
</template>

<script setup name="ApplyDetailDrawer">
import { saveAs } from 'file-saver'
import { getApply, getApplyFile } from '@/api/oa/apply'
import OaSensitiveText from '@/components/oa/OaSensitiveText.vue'
import {
  applyStatusText,
  applyStatusTone,
  genderText,
  gradeText,
  isMaskedValue,
  oaDateTime,
  oaTimestamp
} from '@/utils/oaMember'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // 列表行数据：先渲染列表字段，详情接口返回后覆盖（避免打开抽屉白屏）
  applyRow: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'approve', 'reject'])

const { proxy } = getCurrentInstance()
const router = useRouter()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const rowApply = computed(() => props.applyRow || {})

const loading = ref(false)
const loadError = ref(false)
const detail = ref({})
const attachments = ref([])

// 快速切换申请时的竞态保护：只有最后一次加载能落数据
let loadSeq = 0

const statusText = computed(() => {
  const status = detail.value.status !== undefined ? detail.value.status : rowApply.value.status
  if (status === undefined || status === null || status === '') return ''
  const text = detail.value.statusLabel || applyStatusText(status)
  // 已关闭统一带 ⊘ 前缀
  return applyStatusText(status) !== '-' ? applyStatusText(status) : text
})

const statusTone = computed(() => {
  const status = detail.value.status !== undefined ? detail.value.status : rowApply.value.status
  return applyStatusTone(status)
})

const adjustText = computed(() => {
  const value = detail.value.obeyAdjust
  if (value === undefined || value === null || value === '') return '—'
  return String(value) === '1' ? '是' : '否'
})

/** 时间轴：PC-04 正序（提交 → 重提 → 驳回 → 同意 → 代审 → 自动关闭） */
const timelineLogs = computed(() => {
  const logs = Array.isArray(detail.value.logs) ? [...detail.value.logs] : []
  return logs.sort((a, b) => oaTimestamp(a.createTime) - oaTimestamp(b.createTime))
})

const canAuditCurrent = computed(() => {
  const status = detail.value.status !== undefined ? detail.value.status : rowApply.value.status
  if (String(status) !== '0') return false
  if (detail.value.canAudit === false) return false
  return proxy.$auth.hasPermi('oa:apply:audit')
})

const canViewMember = computed(() => {
  const status = detail.value.status !== undefined ? detail.value.status : rowApply.value.status
  if (String(status) !== '1') return false
  return proxy.$auth.hasPermi('oa:member:list')
})

/** 审批动作的节点配色 */
function logTone(action) {
  const text = String(action || '')
  if (/同意|通过/.test(text)) return 'green'
  if (/驳回|拒绝/.test(text)) return 'red'
  if (/撤回|关闭/.test(text)) return 'gray'
  if (/重提|代审/.test(text)) return 'orange'
  return 'blue'
}

function clearAttachments() {
  attachments.value.forEach(att => {
    if (att.previewUrl) URL.revokeObjectURL(att.previewUrl)
  })
  attachments.value = []
}

/** 从 photoUrl / cardUrl 里兜底解析 fileId（正常接口都会给 photoFileId） */
function resolveFileId(fileId, fileUrl) {
  if (fileId) return fileId
  if (!fileUrl) return null
  const match = String(fileUrl).match(/\/oa\/file\/(\d+)/)
  return match ? Number(match[1]) : null
}

function extOf(type) {
  const mime = String(type || '')
  if (mime.indexOf('jpeg') >= 0) return 'jpg'
  if (mime.indexOf('png') >= 0) return 'png'
  if (mime.indexOf('pdf') >= 0) return 'pdf'
  return ''
}

/** 附件走 request 带 token 拉 blob，不直接用裸 URL（R12） */
async function loadAttachments(data, seq) {
  clearAttachments()
  const candidates = [
    { key: 'photo', label: '一寸照', fileId: resolveFileId(data.photoFileId, data.photoUrl) },
    { key: 'card', label: '简历', fileId: resolveFileId(data.cardFileId, data.cardUrl) }
  ].filter(item => !!item.fileId)

  for (const item of candidates) {
    try {
      const blob = await getApplyFile(item.fileId)
      if (seq !== loadSeq) return
      const isImage = /^image\//.test(blob.type || '')
      attachments.value.push({
        ...item,
        blob,
        ext: extOf(blob.type),
        previewUrl: isImage ? URL.createObjectURL(blob) : ''
      })
    } catch (e) {
      if (seq !== loadSeq) return
      attachments.value.push({ ...item, blob: null, ext: '', previewUrl: '', failed: true })
    }
  }
}

function downloadAttachment(att) {
  if (!att.blob) {
    proxy.$modal.msgWarning('附件加载失败，请稍后重试')
    return
  }
  const name = att.label + (att.ext ? '.' + att.ext : '')
  saveAs(new Blob([att.blob], { type: att.blob.type || 'application/octet-stream' }), name)
}

/** 加载详情：列表行先占位，详情接口成功后覆盖 */
async function loadDetail() {
  const seq = ++loadSeq
  loadError.value = false
  clearAttachments()
  const row = rowApply.value
  detail.value = { ...row }
  if (!row || !row.id) return
  // 没有详情权限时只展示列表行已有字段，避免无意义的 403
  if (!proxy.$auth.hasPermi('oa:apply:query')) return
  loading.value = true
  try {
    const res = await getApply(row.id)
    if (seq !== loadSeq) return
    detail.value = res.data || { ...row }
    await loadAttachments(detail.value, seq)
  } catch (e) {
    if (seq === loadSeq) loadError.value = true
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

function viewMember() {
  const query = {}
  const studentNo = detail.value.studentNo || rowApply.value.studentNo
  if (studentNo) query.studentNo = studentNo
  if (detail.value.deptId) query.deptId = detail.value.deptId
  // 菜单实际 path 为 hr（SQL），优先 /hr/member，兼容旧文档 /oa/member
  const candidates = ['/hr/member', '/oa/member']
  const target = candidates.find(path => router.resolve(path).matched.length > 0)
  visible.value = false
  router.push({ path: target || candidates[0], query })
}

function handleClosed() {
  loadSeq++
  clearAttachments()
  detail.value = {}
  loadError.value = false
}

watch(
  () => props.modelValue,
  value => {
    if (value) loadDetail()
  }
)

watch(
  () => rowApply.value.id,
  () => {
    if (props.modelValue) loadDetail()
  }
)

// 组件卸载时兜底回收预览用的 object URL，避免内存泄漏
onBeforeUnmount(() => {
  loadSeq++
  clearAttachments()
})
</script>

<style scoped lang="scss">
.oa-drawer-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
}

.oa-apply-detail {
  min-height: 240px;
}

.oa-section {
  margin-bottom: 22px;

  &:last-child {
    margin-bottom: 0;
  }
}

.oa-section__title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;

  small {
    font-size: 12px;
    font-weight: 400;
    color: #909399;
  }
}

.oa-kv {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.oa-kv__item {
  min-width: 0;

  &.is-full {
    grid-column: 1 / -1;
  }
}

.oa-kv__label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: #909399;
}

.oa-kv__value {
  padding: 6px 10px;
  border-radius: 4px;
  background: #f5f7fa;
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
  word-break: break-word;

  &--block {
    white-space: pre-wrap;
    line-height: 1.75;
  }
}

.oa-attach {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
}

.oa-attach__item {
  width: 180px;
}

.oa-attach__thumb {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 128px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.oa-attach__ph {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;

  .el-icon {
    font-size: 26px;
  }
}

.oa-attach__meta {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.oa-attach__name {
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}

/* ---------- 审批时间轴 ---------- */
.oa-tl {
  font-size: 13px;
}

.oa-tl__item {
  display: flex;
  gap: 12px;
}

.oa-tl__axis {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 0 0 14px;
  padding-top: 4px;
}

.oa-tl__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  flex: 0 0 8px;
}

.oa-tl__line {
  flex: 1;
  width: 1px;
  min-height: 26px;
  margin-top: 5px;
  background: #ebeef5;
}

.oa-tl__item.is-green .oa-tl__dot {
  background: #67c23a;
}

.oa-tl__item.is-red .oa-tl__dot {
  background: #f56c6c;
}

.oa-tl__item.is-orange .oa-tl__dot {
  background: #e6a23c;
}

.oa-tl__item.is-blue .oa-tl__dot {
  background: #236de9;
}

.oa-tl__body {
  flex: 1;
  min-width: 0;
  padding-bottom: 18px;
}

.oa-tl__title {
  font-weight: 600;
  color: #303133;
}

.oa-tl__meta {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.7;
}

.oa-tl__comment {
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 4px;
  background: #f8f9fb;
  font-size: 12px;
  line-height: 1.7;
  color: #606266;
  word-break: break-word;
}

/* ---------- 错误态 ---------- */
.oa-load-error {
  padding: 48px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;

  .el-icon {
    font-size: 32px;
    color: #e6a23c;
  }

  p {
    margin: 10px 0 16px;
  }
}

.oa-drawer-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.oa-drawer-foot__spacer {
  flex: 1;
}
</style>
