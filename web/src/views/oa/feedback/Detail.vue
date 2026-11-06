<template>
  <el-drawer
    v-model="visible"
    size="620px"
    append-to-body
    class="fb-detail-drawer"
    @closed="handleClosed"
  >
    <template #header>
      <div class="fb-detail-head">
        <span class="fb-detail-no">{{ detail.ticketNo || '工单详情' }}</span>
        <span v-if="detail.type" class="fb-type-tag" :class="'tone-' + typeMeta.tone">
          {{ typeMeta.icon }} {{ typeMeta.label }}
        </span>
        <span v-if="detail.status !== undefined" class="oa-pill" :class="'oa-pill--' + statusMeta.tone">
          {{ statusMeta.label }}
        </span>
      </div>
    </template>

    <div v-loading="loading" class="fb-detail-body">
      <!-- 错误 / 不存在 -->
      <div v-if="loadError" class="fb-detail-error">
        <div class="fb-detail-error__title">工单不存在或已被撤回</div>
        <div class="fb-detail-error__sub">该工单可能已被提交人撤回，或管理员已删除。</div>
        <div class="fb-detail-error__actions">
          <el-button type="primary" plain @click="loadDetail">重试</el-button>
          <el-button @click="visible = false">关 闭</el-button>
        </div>
      </div>

      <template v-else-if="!loading">
        <h2 class="fb-detail-title">{{ detail.title || '—' }}</h2>

        <!-- 信息行 -->
        <div class="fb-info-grid">
          <div class="fb-info-item">
            <div class="fb-info-item__k">提交人</div>
            <div class="fb-info-item__v">
              {{ detail.submitterName || '—' }}
              <span v-if="isAnonymous && detail.submitterName !== '匿名用户'" class="fb-anon-tag">匿名</span>
            </div>
          </div>
          <div class="fb-info-item">
            <div class="fb-info-item__k">提交时间</div>
            <div class="fb-info-item__v">{{ submitTimeText }}</div>
          </div>
          <div class="fb-info-item">
            <div class="fb-info-item__k">影响模块</div>
            <div class="fb-info-item__v">{{ moduleText }}</div>
          </div>
          <div class="fb-info-item">
            <div class="fb-info-item__k">附件</div>
            <div class="fb-info-item__v">{{ attachmentText }}</div>
          </div>
        </div>

        <!-- 正文分区 -->
        <div class="fb-detail-sec">
          <h3>详细描述</h3>
          <div class="fb-detail-text">{{ detail.content || '未填写' }}</div>
        </div>
        <div class="fb-detail-sec">
          <h3>复现步骤</h3>
          <div class="fb-detail-text" :class="{ 'is-empty': !detail.steps }">{{ detail.steps || '未填写' }}</div>
        </div>
        <div class="fb-detail-sec">
          <h3>期望结果</h3>
          <div class="fb-detail-text" :class="{ 'is-empty': !detail.expectResult }">{{ detail.expectResult || '未填写' }}</div>
        </div>

        <!-- 附件 -->
        <div class="fb-detail-sec">
          <h3>附件</h3>
          <template v-if="files.length">
            <div v-if="imageFiles.length" class="fb-att-grid">
              <el-image
                v-for="(file, index) in imageFiles"
                :key="'img-' + (file.fileId || index)"
                :src="feedbackFileUrl(file)"
                :preview-src-list="imageUrls"
                :initial-index="index"
                fit="cover"
                preview-teleported
                class="fb-detail-thumb"
              />
            </div>
            <div v-if="wordFiles.length" class="fb-att-files">
              <div v-for="(file, index) in wordFiles" :key="'file-' + (file.fileId || index)" class="fb-att-file">
                <span class="fb-att-file__ico">
                  <el-icon><Document /></el-icon>
                </span>
                <div class="fb-att-file__main">
                  <div class="fb-att-file__name" :title="feedbackFileName(file)">{{ feedbackFileName(file) }}</div>
                  <div class="fb-att-file__meta">
                    {{ formatFileSize(feedbackFileSize(file)) }} · Word 文档
                  </div>
                </div>
                <a
                  class="fb-att-file__action"
                  :href="feedbackFileUrl(file)"
                  :download="feedbackFileName(file)"
                  target="_blank"
                  rel="noopener"
                >下载</a>
              </div>
            </div>
          </template>
          <div v-else class="fb-empty">没有附件</div>
        </div>

        <!-- 官方回复 -->
        <div class="fb-detail-sec">
          <h3>官方回复</h3>
          <div v-if="replyInfo.content" class="fb-reply-card">
            <div class="fb-reply-head">
              <span class="fb-reply-avatar">管</span>
              <div>
                <div class="fb-reply-main">
                  <b>{{ replyInfo.byName || '管理员' }}</b>
                  <span class="fb-reply-role">· 官方回复</span>
                </div>
                <div class="fb-reply-meta">{{ formatFeedbackTime(replyInfo.time) }}</div>
              </div>
            </div>
            <div class="fb-reply-body">{{ replyInfo.content }}</div>
          </div>
          <div v-else class="fb-empty">管理员还没有回复，请耐心等待</div>
        </div>
      </template>
    </div>

    <template #footer>
      <div class="fb-detail-foot">
        <el-button @click="visible = false">关 闭</el-button>
        <el-button
          v-if="canWithdraw"
          v-hasPermi="['oa:feedback:withdraw']"
          type="danger"
          plain
          :loading="withdrawing"
          @click="handleWithdraw"
        >撤 回</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup name="FeedbackDetail">
import {
  getFeedback,
  withdrawFeedback,
  feedbackTypeMeta,
  feedbackStatusMeta,
  feedbackFilesOf,
  feedbackFileUrl,
  feedbackFileName,
  feedbackFileSize,
  feedbackFileType,
  formatFileSize,
  formatFeedbackTime
} from '@/api/oa/feedback'
import useUserStore from '@/store/modules/user'

/**
 * 工单详情（620px 抽屉，契约 §4.1 / 原型 ③）
 * - 编号/类型/状态、信息行、正文分区、附件（图片预览 / Word 下载）、官方回复或等待回复
 * - 本人 + 待处理可撤回（优先用后端 canWithdraw，缺失时按 submitterId 兜底）
 * - 匿名展示完全按后端 submitterName / isAnonymous 渲染
 */
const { proxy } = getCurrentInstance()
const emit = defineEmits(['success'])
const userStore = useUserStore()
const { oa_feedback_type, oa_feedback_status } = useDict('oa_feedback_type', 'oa_feedback_status')

const visible = ref(false)
const loading = ref(false)
const loadError = ref(false)
const withdrawing = ref(false)
const detail = ref({})
const feedbackId = ref(undefined)

const typeMeta = computed(() => feedbackTypeMeta(detail.value.type, oa_feedback_type.value))
const statusMeta = computed(() => feedbackStatusMeta(detail.value.status, oa_feedback_status.value))
/** 后端 isAnonymous 是字符串 '0'/'1' */
const isAnonymous = computed(() => String(detail.value.isAnonymous ?? '') === '1')

const files = computed(() => feedbackFilesOf(detail.value))
const imageFiles = computed(() => files.value.filter(file => feedbackFileType(file) === 'image'))
const wordFiles = computed(() => files.value.filter(file => feedbackFileType(file) === 'word'))
const imageUrls = computed(() => imageFiles.value.map(file => feedbackFileUrl(file)))

const moduleText = computed(() => {
  const moduleName = detail.value.moduleName || ''
  const pageName = detail.value.pageName || ''
  if (!moduleName && !pageName) return '—'
  if (!pageName) return moduleName
  if (!moduleName) return pageName
  return moduleName + ' / ' + pageName
})

const attachmentText = computed(() => {
  if (!files.value.length) {
    const total = Number(detail.value.attachmentCount || detail.value.fileCount || 0)
    return total > 0 ? '共 ' + total + ' 个' : '没有附件'
  }
  return imageFiles.value.length + ' 张图片 · ' + wordFiles.value.length + ' 个 Word'
})

const submitTimeText = computed(() => {
  const item = detail.value
  return formatFeedbackTime(item.submitTime || item.createTime)
})

/** 官方回复字段兼容扁平 replyContent/replyByName/replyTime 与嵌套 reply 对象 */
const replyInfo = computed(() => {
  const item = detail.value || {}
  const reply = item.reply || {}
  return {
    content: item.replyContent || reply.content || reply.replyContent || '',
    byName: item.replyByName || reply.byName || reply.replyByName || reply.operatorName || '',
    time: item.replyTime || reply.time || reply.replyTime || ''
  }
})

const canWithdraw = computed(() => {
  const item = detail.value
  if (!item) return false
  if (!item.feedbackId && !feedbackId.value) return false
  // 匿名本人场景只能靠后端下发 canWithdraw / isMine 判断
  if (item.canWithdraw === true || item.isMine === true || item.mine === true) return true
  if (item.canWithdraw === false) return false
  if (String(item.status) !== '0') return false
  if (item.submitterId === undefined || item.submitterId === null) return false
  return String(item.submitterId) === String(userStore.id)
})

/* -------------------------------------------------------------------------- */
/* 打开 / 加载 / 关闭                                                          */
/* -------------------------------------------------------------------------- */
function open(options) {
  const opts = options || {}
  feedbackId.value = opts.feedbackId || opts.id
  detail.value = {}
  loadError.value = false
  visible.value = true
  loadDetail()
}

async function loadDetail() {
  if (!feedbackId.value) {
    loadError.value = true
    return
  }
  loading.value = true
  loadError.value = false
  try {
    const response = await getFeedback(feedbackId.value)
    const data = (response && response.data) || {}
    if (!data || (!data.feedbackId && !data.ticketNo)) {
      loadError.value = true
      detail.value = {}
    } else {
      detail.value = data
    }
  } catch (e) {
    loadError.value = true
    detail.value = {}
  } finally {
    loading.value = false
  }
}

function handleClosed() {
  detail.value = {}
  loadError.value = false
}

/* -------------------------------------------------------------------------- */
/* 撤回                                                                       */
/* -------------------------------------------------------------------------- */
function handleWithdraw() {
  const targetId = detail.value.feedbackId || feedbackId.value
  if (withdrawing.value || !targetId) return
  const ticketNo = detail.value.ticketNo || ''
  proxy.$modal
    .confirm('撤回后该工单将从列表移除（仅「待处理」状态可撤回），确定撤回 ' + ticketNo + ' 吗？')
    .then(() => {
      withdrawing.value = true
      return withdrawFeedback(targetId)
    })
    .then(() => {
      proxy.$modal.msgSuccess('已撤回：工单已从列表移除')
      visible.value = false
      emit('success')
    })
    .catch(() => {})
    .finally(() => {
      withdrawing.value = false
    })
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.fb-detail-drawer :deep(.el-drawer__body) {
  padding-top: 16px;
}

.fb-detail-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}

.fb-detail-no {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--ink-soft);
}

.fb-detail-body {
  min-height: 200px;
}

.fb-detail-title {
  margin: 0 0 14px;
  font-size: 18px;
  font-weight: 650;
  line-height: 1.5;
  color: var(--ink);
  word-break: break-word;
}

/* ---------- 信息行 ---------- */
.fb-info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px 16px;
  margin-bottom: 18px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
  background: var(--hover-bg);
  font-size: 13px;
}

.fb-info-item {
  min-width: 0;
}

.fb-info-item__k {
  color: var(--ink-soft);
}

.fb-info-item__v {
  margin-top: 3px;
  color: var(--ink);
  word-break: break-all;
}

@media (max-width: 640px) {
  .fb-info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.fb-anon-tag {
  display: inline-block;
  margin-left: 6px;
  padding: 0 5px;
  border: 1px solid var(--line-strong);
  border-radius: 3px;
  background: var(--neutral-soft);
  color: var(--neutral-text);
  font-size: 11px;
  line-height: 16px;
  vertical-align: middle;
}

/* ---------- 正文分区 ---------- */
.fb-detail-sec {
  margin-bottom: 16px;

  h3 {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 8px;
    font-size: 14px;
    font-weight: 650;
    color: var(--ink);

    &::before {
      width: 3px;
      height: 13px;
      border-radius: 2px;
      background: var(--blue);
      content: '';
    }
  }
}

.fb-detail-text {
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: var(--hover-bg);
  color: var(--ink-muted);
  font-size: 13px;
  line-height: 1.85;
  white-space: pre-wrap;
  word-break: break-word;

  &.is-empty {
    color: var(--ink-faint);
  }
}

.fb-empty {
  padding: 14px 0;
  text-align: center;
  font-size: 13px;
  color: var(--ink-faint);
}

/* ---------- 附件 ---------- */
.fb-att-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 10px;
}

.fb-detail-thumb {
  width: 116px;
  height: 86px;
  overflow: hidden;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-xs);
  cursor: zoom-in;
}

.fb-att-file {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  padding: 9px 12px;
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
  background: var(--hover-bg);
  font-size: 13px;
  color: var(--ink-muted);
}

.fb-att-file__ico {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  border-radius: 4px;
  background: var(--blue-soft);
  color: var(--blue);
}

.fb-att-file__main {
  flex: 1;
  min-width: 0;
}

.fb-att-file__name {
  overflow: hidden;
  color: var(--ink);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.fb-att-file__meta {
  margin-top: 2px;
  font-size: 11.5px;
  color: var(--ink-soft);
}

.fb-att-file__action {
  color: var(--blue);
  font-weight: 600;
  text-decoration: none;
}

/* ---------- 官方回复 ---------- */
.fb-reply-card {
  padding: 14px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-xs);
  background: var(--blue-soft);
}

.fb-reply-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.fb-reply-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  border-radius: 50%;
  background: var(--blue);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.fb-reply-main {
  font-size: 13px;
  color: var(--ink);
  line-height: 1.5;
}

.fb-reply-role {
  color: var(--ink-soft);
  font-weight: 400;
}

.fb-reply-meta {
  margin-top: 2px;
  font-size: 11.5px;
  color: var(--ink-soft);
}

.fb-reply-body {
  font-size: 13px;
  line-height: 1.85;
  color: var(--ink);
  white-space: pre-wrap;
  word-break: break-word;
}

/* ---------- 类型标签 ---------- */
.fb-type-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 8px;
  border: 1px solid var(--line-strong);
  border-radius: 3px;
  font-size: 12px;
  white-space: nowrap;

  &.tone-red {
    border-color: var(--red-soft);
    background: var(--red-soft);
    color: var(--red-text);
  }

  &.tone-blue {
    border-color: var(--blue-soft);
    background: var(--blue-soft);
    color: var(--blue);
  }

  &.tone-green {
    border-color: var(--lime-soft);
    background: var(--lime-soft);
    color: var(--lime-text);
  }

  &.tone-gray {
    border-color: var(--neutral-soft);
    background: var(--neutral-soft);
    color: var(--neutral-text);
  }
}

/* ---------- 错误态 ---------- */
.fb-detail-error {
  padding: 48px 0;
  text-align: center;
}

.fb-detail-error__title {
  font-size: 15px;
  color: var(--ink-muted);
}

.fb-detail-error__sub {
  margin-top: 8px;
  font-size: 12px;
  color: var(--ink-soft);
}

.fb-detail-error__actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
}

.fb-detail-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
