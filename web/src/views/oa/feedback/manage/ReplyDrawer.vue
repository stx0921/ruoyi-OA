<template>
  <el-drawer
    v-model="visible"
    size="620px"
    append-to-body
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <template #header>
      <div class="fb-drawer-head">
        <span class="fb-drawer-head__title">工单处理</span>
        <span class="fb-mono">{{ ticketNo }}</span>
      </div>
    </template>

    <div v-loading="loading" class="fb-drawer-body">
      <!-- 错误态 -->
      <div v-if="error" class="fb-state">
        <el-icon :size="32"><WarningFilled /></el-icon>
        <p>工单详情加载失败</p>
        <div class="fb-state__actions">
          <el-button size="small" type="primary" @click="load">重试</el-button>
          <el-button size="small" @click="visible = false">关闭</el-button>
        </div>
      </div>

      <template v-else>
        <!-- 头部：类型 / 状态 / 提交时间 -->
        <div class="fb-detail-head">
          <span class="oa-pill" :class="'oa-pill--' + typeMeta.tone">
            <span class="fb-type-icon">{{ typeMeta.icon }}</span>{{ typeMeta.label }}
          </span>
          <span class="oa-pill" :class="'oa-pill--' + statusMeta.tone">{{ statusMeta.label }}</span>
          <span class="fb-detail-head__time">{{ oaDateTime(submitTime) }}</span>
        </div>

        <h2 class="fb-detail-title">{{ detail.title || '—' }}</h2>

        <!-- 信息行 -->
        <div class="fb-info-grid">
          <div class="fb-info-item">
            <span class="fb-info-item__k">提交人</span>
            <span class="fb-info-item__v">
              {{ detail.submitterName || '—' }}
              <span v-if="isAnonymous" class="fb-anon">匿名</span>
            </span>
          </div>
          <div class="fb-info-item">
            <span class="fb-info-item__k">影响模块</span>
            <span class="fb-info-item__v">{{ moduleText }}</span>
          </div>
          <div class="fb-info-item">
            <span class="fb-info-item__k">附件</span>
            <span class="fb-info-item__v">{{ images.length }} 张图片 · {{ words.length }} 个 Word</span>
          </div>
          <div class="fb-info-item">
            <span class="fb-info-item__k">当前状态</span>
            <span class="fb-info-item__v">{{ statusMeta.label }}</span>
          </div>
        </div>

        <!-- 正文（只读） -->
        <section class="fb-sec">
          <h3 class="fb-sec__title">详细描述</h3>
          <div class="fb-text">{{ detail.content || '未填写' }}</div>
        </section>
        <section class="fb-sec">
          <h3 class="fb-sec__title">复现步骤</h3>
          <div class="fb-text">{{ detail.steps || '未填写' }}</div>
        </section>
        <section class="fb-sec">
          <h3 class="fb-sec__title">期望结果</h3>
          <div class="fb-text">{{ detail.expectResult || '未填写' }}</div>
        </section>

        <!-- 附件 -->
        <section class="fb-sec">
          <h3 class="fb-sec__title">附件</h3>
          <div v-if="images.length" class="fb-img-grid">
            <el-image
              v-for="(file, index) in images"
              :key="'image-' + (file.fileId || index)"
              class="fb-img"
              :src="feedbackFileUrl(file)"
              :preview-src-list="imageUrls"
              :initial-index="index"
              fit="cover"
              preview-teleported
              :alt="feedbackFileName(file)"
            />
          </div>
          <div v-for="(file, index) in words" :key="'word-' + (file.fileId || index)" class="fb-file">
            <span class="fb-file__ico">
              <el-icon :size="18"><Document /></el-icon>
            </span>
            <div class="fb-file__main">
              <div class="fb-file__name">{{ feedbackFileName(file) }}</div>
              <div class="fb-file__size">{{ formatFileSize(feedbackFileSize(file)) }} · Word 文档</div>
            </div>
            <a
              class="fb-file__download"
              :href="feedbackFileUrl(file)"
              :download="feedbackFileName(file)"
              target="_blank"
              rel="noopener"
            >下载</a>
          </div>
          <div v-if="!images.length && !words.length" class="fb-text fb-text--empty">没有附件</div>
        </section>

        <!-- 官方回复（必填 ≤500） -->
        <section class="fb-sec fb-sec--reply">
          <h3 class="fb-sec__title">
            <span class="fb-req">*</span>官方回复（必填，≤500 字）
          </h3>
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="填写给提交人的回复，回复后将通过站内信通知提交人"
          />
          <p v-if="!replyValid" class="fb-hint">
            回复内容必填：未填写时「保存并回复」不可用，回复后将通知提交人。
          </p>
        </section>

        <!-- 处理状态 -->
        <section class="fb-sec">
          <h3 class="fb-sec__title">处理状态</h3>
          <el-select v-model="status" style="width: 160px">
            <el-option label="待处理" value="0" />
            <el-option label="已回复" value="1" />
          </el-select>
          <p class="fb-status-hint">选择「待处理」保存后，将清空本工单的官方回复内容。</p>
        </section>
      </template>
    </div>

    <template #footer>
      <div class="fb-drawer-foot">
        <span v-if="!replyValid" class="fb-hint fb-hint--foot">回复内容必填</span>
        <span class="fb-drawer-foot__spacer"></span>
        <el-button @click="visible = false">关 闭</el-button>
        <el-button
          type="danger"
          plain
          :loading="deleting"
          @click="handleDelete"
          v-hasPermi="['oa:feedback:remove']"
        >删除工单</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="!replyValid"
          @click="handleSave"
          v-hasPermi="['oa:feedback:reply']"
        >保存并回复</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup name="FeedbackReplyDrawer">
import {
  deleteFeedback,
  feedbackFileName,
  feedbackFileSize,
  feedbackFileUrl,
  feedbackFilesOf,
  feedbackStatusMeta,
  feedbackTypeMeta,
  formatFileSize,
  getFeedback,
  isImageFile,
  isWordFile,
  replyFeedback
} from '@/api/oa/feedback'
import { oaDateTime } from '@/utils/oa'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  feedbackId: { type: [Number, String], default: null },
  // 列表行：详情接口返回前先用它渲染头部，避免抽屉白屏
  row: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'saved', 'deleted'])

const { proxy } = getCurrentInstance()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const loading = ref(false)
const error = ref(false)
const detail = ref({})
const replyContent = ref('')
const status = ref('1')
const saving = ref(false)
const deleting = ref(false)

const currentId = computed(() => {
  if (props.feedbackId !== null && props.feedbackId !== undefined && props.feedbackId !== '') {
    return props.feedbackId
  }
  return props.row ? props.row.feedbackId ?? props.row.id : null
})

const ticketNo = computed(() => detail.value.ticketNo || props.row.ticketNo || '')

const submitTime = computed(() => detail.value.submitTime || detail.value.createTime || '')

const isAnonymous = computed(() => {
  const value = detail.value.isAnonymous
  return value === true || value === 1 || String(value) === '1'
})

const moduleText = computed(() => {
  const text = [detail.value.moduleName, detail.value.pageName].filter(Boolean).join(' / ')
  return text || '—'
})

const typeMeta = computed(() => {
  const meta = feedbackTypeMeta(detail.value.type || '')
  return { ...meta, label: detail.value.typeLabel || meta.label }
})
const statusMeta = computed(() => {
  const meta = feedbackStatusMeta(detail.value.status || '')
  return { ...meta, label: detail.value.statusLabel || meta.label }
})

const files = computed(() => feedbackFilesOf(detail.value))
const images = computed(() => files.value.filter(file => isImageFile(file)))
const words = computed(() => files.value.filter(file => isWordFile(file)))
const imageUrls = computed(() => images.value.map(file => feedbackFileUrl(file)))

const replyValid = computed(() => {
  const text = (replyContent.value || '').trim()
  return text.length > 0 && text.length <= 500
})

function load() {
  error.value = false
  replyContent.value = ''
  status.value = '1'
  detail.value = { ...(props.row || {}) }
  const id = currentId.value
  if (!id) return
  loading.value = true
  getFeedback(id)
    .then(res => {
      const data = (res && res.data) || {}
      detail.value = { ...(props.row || {}), ...data }
      replyContent.value = data.replyContent || ''
      // 默认按「已回复」处理：保存即回复；如需回退为待处理可手动切换
      status.value = '1'
    })
    .catch(() => {
      error.value = true
    })
    .finally(() => {
      loading.value = false
    })
}

function handleSave() {
  if (!replyValid.value) {
    proxy.$modal.msgWarning('请填写回复内容')
    return
  }
  const id = currentId.value
  if (!id) return
  saving.value = true
  replyFeedback({ feedbackId: id, replyContent: replyContent.value.trim(), status: Number(status.value) })
    .then(() => {
      if (status.value === '1') {
        proxy.$modal.msgSuccess('已回复，站内信已通知提交人')
      } else {
        proxy.$modal.msgSuccess('已更新工单状态为待处理')
      }
      emit('saved')
      visible.value = false
    })
    .catch(() => {})
    .finally(() => {
      saving.value = false
    })
}

function handleDelete() {
  const id = currentId.value
  if (!id) return
  proxy.$modal
    .confirm('删除后不可在列表查看，确定删除？')
    .then(() => {
      deleting.value = true
      return deleteFeedback([id])
    })
    .then(() => {
      proxy.$modal.msgSuccess(`已删除工单 ${ticketNo.value || ''}`.trim())
      emit('deleted')
      visible.value = false
    })
    .catch(() => {})
    .finally(() => {
      deleting.value = false
    })
}

function handleClosed() {
  detail.value = {}
  replyContent.value = ''
  status.value = '1'
  error.value = false
  saving.value = false
  deleting.value = false
}

watch(
  [() => props.modelValue, () => props.feedbackId],
  ([visible]) => {
    if (visible) load()
  }
)
</script>

<style scoped lang="scss">
.fb-drawer-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.fb-drawer-head__title {
  font-size: 15px;
  font-weight: 650;
  color: var(--ink);
}

.fb-mono {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--ink-soft);
}

.fb-drawer-body {
  min-height: 260px;
}

/* ---------- 头部 ---------- */
.fb-detail-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.fb-type-icon {
  margin-right: 4px;
  font-size: 12px;
}

.fb-detail-head__time {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--ink-faint);
}

.fb-detail-title {
  margin: 0 0 14px;
  font-size: 17px;
  font-weight: 650;
  line-height: 1.5;
  color: var(--ink);
  word-break: break-word;
}

/* ---------- 信息行 ---------- */
.fb-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 20px;
  padding: 12px 14px;
  margin-bottom: 18px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-sm);
  background: var(--groove-bg);
}

.fb-info-item__k {
  display: block;
  font-size: 12px;
  color: var(--ink-soft);
}

.fb-info-item__v {
  display: block;
  margin-top: 3px;
  font-size: 13px;
  color: var(--ink);
  word-break: break-all;
}

.fb-anon {
  display: inline-block;
  margin-left: 6px;
  padding: 0 5px;
  border: 1px solid var(--line-strong);
  border-radius: 999px;
  background: var(--neutral-soft);
  color: var(--neutral-text);
  font-size: 11px;
  line-height: 16px;
  vertical-align: middle;
}

/* ---------- 正文分区 ---------- */
.fb-sec {
  margin-bottom: 18px;
}

.fb-sec + .fb-sec {
  padding-top: 2px;
}

.fb-sec__title {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 650;
  color: var(--ink);

  &::before {
    display: inline-block;
    width: 3px;
    height: 12px;
    margin-right: 7px;
    border-radius: 2px;
    background: var(--blue);
    vertical-align: -1px;
    content: "";
  }
}

.fb-req {
  margin-right: 3px;
  color: var(--red-text);
}

.fb-text {
  font-size: 13px;
  line-height: 1.85;
  color: var(--ink-muted);
  white-space: pre-wrap;
  word-break: break-word;

  &--empty {
    color: var(--ink-faint);
  }
}

/* ---------- 附件 ---------- */
.fb-img-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 10px;
}

.fb-img {
  width: 116px;
  height: 86px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-xs);
  overflow: hidden;
  cursor: zoom-in;
}

.fb-file {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  margin-bottom: 8px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-xs);
  background: var(--input-bg);
}

.fb-file__ico {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  border-radius: var(--r-xs);
  background: var(--blue-soft);
  color: var(--blue);
}

.fb-file__main {
  flex: 1;
  min-width: 0;
}

.fb-file__name {
  overflow: hidden;
  color: var(--ink);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fb-file__size {
  margin-top: 2px;
  font-size: 11.5px;
  color: var(--ink-soft);
}

.fb-file__download {
  flex: 0 0 auto;
  color: var(--blue);
  font-size: 12px;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

/* ---------- 回复 / 状态 ---------- */
.fb-sec--reply {
  padding-top: 16px;
  border-top: 1px solid var(--line-strong);
}

.fb-hint {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--red-text);

  &--foot {
    margin: 0;
  }
}

.fb-status-hint {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink-faint);
}

/* ---------- 底部 ---------- */
.fb-drawer-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.fb-drawer-foot__spacer {
  flex: 1;
}

.fb-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 56px 0;
  color: var(--ink-soft);
  font-size: 13px;

  p {
    margin: 0;
  }
}

.fb-state__actions {
  display: flex;
  gap: 8px;
}
</style>
