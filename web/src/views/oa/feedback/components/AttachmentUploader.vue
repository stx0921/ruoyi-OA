<template>
  <div class="fb-uploader" :class="{ 'is-disabled': disabled }">
    <!-- 已上传：图片缩略图 -->
    <div v-if="imageFiles.length" class="fb-att-grid">
      <div v-for="(file, index) in imageFiles" :key="fileKey(file, index)" class="fb-att-thumb">
        <el-image
          :src="feedbackFileUrl(file)"
          :preview-src-list="imageUrls"
          :initial-index="index"
          fit="cover"
          preview-teleported
          class="fb-att-img"
        />
        <span v-if="!disabled" class="fb-att-del" title="删除" @click.stop="removeFile(file)">×</span>
      </div>
    </div>

    <!-- 已上传：Word 列表 -->
    <div v-if="wordFiles.length" class="fb-att-files">
      <div v-for="(file, index) in wordFiles" :key="fileKey(file, index)" class="fb-att-file">
        <span class="fb-att-file__ico">
          <el-icon><Document /></el-icon>
        </span>
        <div class="fb-att-file__main">
          <div class="fb-att-file__name" :title="feedbackFileName(file)">{{ feedbackFileName(file) }}</div>
          <div class="fb-att-file__meta">{{ formatFileSize(feedbackFileSize(file)) }} · Word 文档</div>
        </div>
        <a
          class="fb-att-file__action"
          :href="feedbackFileUrl(file)"
          :download="feedbackFileName(file)"
          target="_blank"
          rel="noopener"
        >下载</a>
        <span v-if="!disabled" class="fb-att-file__del" title="删除" @click.stop="removeFile(file)">×</span>
      </div>
    </div>

    <!-- 上传区：点击 / 拖拽 / Ctrl+V -->
    <div
      v-if="!disabled"
      class="fb-dropzone"
      :class="{ 'is-drag': dragging, 'is-uploading': uploading > 0 }"
      @click="triggerFileSelect"
      @dragover.prevent="dragging = true"
      @dragenter.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="handleDrop"
    >
      <el-icon :size="22" class="fb-dropzone__icon"><UploadFilled /></el-icon>
      <div class="fb-dropzone__main">
        拖拽文件到此处，或按 <b>Ctrl+V</b> 粘贴截图
      </div>
      <div class="fb-dropzone__sub">
        图片 ≤ {{ maxImages }} 张 / 每张 ≤ {{ maxImageSize }}MB（内嵌预览）；Word ≤ {{ maxWords }} 个 / 每个 ≤ {{ maxWordSize }}MB（仅下载）
      </div>
      <input
        ref="fileInputRef"
        type="file"
        multiple
        class="fb-file-input"
        accept=".jpg,.jpeg,.png,.gif,.webp,.doc,.docx"
        @change="handleFileInput"
      />
    </div>

    <!-- 计数 / 上传状态 -->
    <div class="fb-uploader__tip">
      <span>已添加：图片 <b>{{ imageFiles.length }}</b> / {{ maxImages }} 张 · Word <b>{{ wordFiles.length }}</b> / {{ maxWords }} 个 · 共 <b>{{ files.length }}</b> / {{ maxTotal }} 个</span>
      <span v-if="uploading > 0" class="fb-uploader__uploading">
        <el-icon class="is-loading"><Loading /></el-icon> 正在上传 {{ uploading }} 个文件…
      </span>
    </div>
  </div>
</template>

<script setup name="FeedbackAttachmentUploader">
import {
  uploadFeedbackFile,
  feedbackFileUrl,
  feedbackFileName,
  feedbackFileSize,
  feedbackFileType,
  formatFileSize
} from '@/api/oa/feedback'

/**
 * 反馈附件上传（契约 §4.1）
 * - 点击选择 / 拖拽 / 文本框内 Ctrl+V 粘贴截图
 * - 图片 ≤5 张、每张 ≤5MB（jpg/jpeg/png/gif/webp，缩略图 + el-image 预览）
 * - Word ≤3 个、每个 ≤20MB（doc/docx，仅下载）
 * - 单工单附件总数 ≤8
 * - v-model 绑定已上传的附件 VO 数组，提交时父组件取 fileId
 */
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false },
  maxTotal: { type: Number, default: 8 },
  maxImages: { type: Number, default: 5 },
  maxWords: { type: Number, default: 3 },
  maxImageSize: { type: Number, default: 5 },
  maxWordSize: { type: Number, default: 20 }
})
const { proxy } = getCurrentInstance()
const emit = defineEmits(['update:modelValue', 'change', 'uploading-change'])

const files = computed(() => (Array.isArray(props.modelValue) ? props.modelValue : []))
const imageFiles = computed(() => files.value.filter(file => feedbackFileType(file) === 'image'))
const wordFiles = computed(() => files.value.filter(file => feedbackFileType(file) === 'word'))
const imageUrls = computed(() => imageFiles.value.map(file => feedbackFileUrl(file)))

const fileInputRef = ref()
const dragging = ref(false)
const uploading = ref(0)
const pendingImages = ref(0)
const pendingWords = ref(0)

// 上传中状态透出给父组件，提交按钮据此禁用，避免未传完就提交导致附件丢失
watch(uploading, value => emit('uploading-change', value))

const IMAGE_EXT = /\.(jpg|jpeg|png|gif|webp)$/i
const WORD_EXT = /\.(doc|docx)$/i

/* -------------------------------------------------------------------------- */
/* 选择 / 拖拽 / 粘贴                                                          */
/* -------------------------------------------------------------------------- */
function triggerFileSelect() {
  if (props.disabled || uploading.value > 0) return
  fileInputRef.value && fileInputRef.value.click()
}

function handleFileInput(event) {
  const picked = event.target.files ? Array.from(event.target.files) : []
  event.target.value = ''
  uploadFiles(picked)
}

function handleDrop(event) {
  dragging.value = false
  if (props.disabled) return
  const dropped = event.dataTransfer && event.dataTransfer.files ? Array.from(event.dataTransfer.files) : []
  uploadFiles(dropped)
}

/** 全局 paste：仅在组件挂载期间生效（Submit 弹窗 destroy-on-close），不拦截普通文本粘贴 */
function handlePaste(event) {
  if (props.disabled) return
  const clipboard = event.clipboardData
  if (!clipboard) return
  const images = []
  if (clipboard.items) {
    for (let index = 0; index < clipboard.items.length; index++) {
      const item = clipboard.items[index]
      if (item.kind === 'file' && /^image\//i.test(item.type || '')) {
        const file = item.getAsFile()
        if (file) images.push(file)
      }
    }
  }
  // 部分浏览器粘贴截图只给 clipboardData.files，这里兜底
  if (!images.length && clipboard.files && clipboard.files.length) {
    for (let index = 0; index < clipboard.files.length; index++) {
      const file = clipboard.files[index]
      if (/^image\//i.test(file.type || '') || IMAGE_EXT.test(file.name || '')) images.push(file)
    }
  }
  if (!images.length) return
  event.preventDefault()
  uploadFiles(images)
}

onMounted(() => {
  document.addEventListener('paste', handlePaste)
})

onBeforeUnmount(() => {
  document.removeEventListener('paste', handlePaste)
})

/* -------------------------------------------------------------------------- */
/* 校验 + 上传                                                                 */
/* -------------------------------------------------------------------------- */
let uploadChain = Promise.resolve()

/** 串行处理多批文件，避免并发上传时数量校验竞态 */
function uploadFiles(list) {
  if (!list || !list.length) return uploadChain
  uploadChain = uploadChain
    .then(async () => {
      for (const file of list) {
        if (!validateFile(file)) continue
        await uploadOne(file)
      }
    })
    .catch(() => {})
  return uploadChain
}

/** 按类型 / 大小 / 数量校验，失败直接 toast 并返回 false */
function validateFile(file) {
  const name = file.name || ''
  const isImage = IMAGE_EXT.test(name)
  const isWord = WORD_EXT.test(name)
  if (!isImage && !isWord) {
    proxy.$modal.msgWarning('仅支持图片（jpg / jpeg / png / gif / webp）或 Word（doc / docx）')
    return false
  }
  const pendingTotal = files.value.length + pendingImages.value + pendingWords.value
  if (pendingTotal >= props.maxTotal) {
    proxy.$modal.msgWarning('单工单附件总数最多 ' + props.maxTotal + ' 个')
    return false
  }
  if (isImage) {
    if (imageFiles.value.length + pendingImages.value >= props.maxImages) {
      proxy.$modal.msgWarning('图片最多 ' + props.maxImages + ' 张')
      return false
    }
    if (file.size > props.maxImageSize * 1024 * 1024) {
      proxy.$modal.msgWarning('单张图片不能超过 ' + props.maxImageSize + 'MB')
      return false
    }
    return true
  }
  if (wordFiles.value.length + pendingWords.value >= props.maxWords) {
    proxy.$modal.msgWarning('Word 附件最多 ' + props.maxWords + ' 个')
    return false
  }
  if (file.size > props.maxWordSize * 1024 * 1024) {
    proxy.$modal.msgWarning('单个 Word 不能超过 ' + props.maxWordSize + 'MB')
    return false
  }
  return true
}

async function uploadOne(file) {
  const kind = IMAGE_EXT.test(file.name || '') ? 'image' : 'word'
  uploading.value += 1
  if (kind === 'image') pendingImages.value += 1
  else pendingWords.value += 1
  try {
    const response = await uploadFeedbackFile(file)
    const data = (response && response.data) || {}
    if (!data.fileId) {
      throw new Error('上传返回缺少 fileId')
    }
    const attachment = {
      fileId: data.fileId,
      fileName: data.fileName || file.name,
      originalName: data.originalName || file.name,
      url: data.url || data.filePath || '',
      size: data.size !== undefined ? data.size : file.size,
      fileType: data.fileType || kind
    }
    updateFiles([...files.value, attachment])
    proxy.$modal.msgSuccess('附件已上传')
  } catch (e) {
    // 具体原因由 request 拦截器提示；这里补一句便于确认是哪个文件失败
    proxy.$modal.msgError('「' + (file.name || '附件') + '」上传失败，请重试')
  } finally {
    uploading.value -= 1
    if (kind === 'image') pendingImages.value -= 1
    else pendingWords.value -= 1
  }
}

function removeFile(file) {
  if (props.disabled) return
  updateFiles(files.value.filter(item => item !== file))
  proxy.$modal.msgSuccess('已删除附件，可重新上传')
}

function updateFiles(next) {
  emit('update:modelValue', next)
  emit('change', next)
}

function fileKey(file, index) {
  return (file && (file.fileId || file.url)) || 'attachment-' + index
}

defineExpose({ clear: () => updateFiles([]), uploading })
</script>

<style scoped lang="scss">
.fb-uploader {
  width: 100%;
}

.fb-att-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 10px;
}

.fb-att-thumb {
  position: relative;
  width: 116px;
  height: 86px;
  overflow: hidden;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-xs);
  background: var(--groove-bg);
}

.fb-att-img {
  width: 100%;
  height: 100%;
  cursor: zoom-in;
}

.fb-att-del {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  cursor: pointer;

  &:hover {
    background: var(--red-text);
  }
}

.fb-att-files {
  margin-bottom: 10px;
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
  text-decoration: none;
  font-weight: 600;
}

.fb-att-file__del {
  padding: 2px 4px;
  color: var(--ink-faint);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;

  &:hover {
    color: var(--red-text);
  }
}

.fb-dropzone {
  padding: 16px;
  border: 1.5px dashed var(--line-strong);
  border-radius: var(--r-xs);
  background: var(--hover-bg);
  color: var(--ink-soft);
  font-size: 13px;
  line-height: 1.8;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.15s ease, background-color 0.15s ease;

  &:hover,
  &.is-drag {
    border-color: var(--blue);
    background: var(--blue-soft);
  }
}

.fb-dropzone__icon {
  margin-bottom: 6px;
  color: var(--ink-faint);
}

.fb-dropzone__main b {
  color: var(--ink);
}

.fb-dropzone__sub {
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--ink-faint);
}

.fb-file-input {
  display: none;
}

.fb-uploader__tip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--ink-soft);

  b {
    color: var(--ink);
  }
}

.fb-uploader__uploading {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--blue);
}

.is-disabled .fb-dropzone {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
