<template>
  <el-dialog
    v-model="visible"
    title="提交工单"
    width="760px"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
    class="fb-submit-dialog"
    @closed="handleClosed"
  >
    <div class="fb-submit-tip">
      描述越清楚，越容易被快速处理。带 <span class="fb-required">*</span> 为必填项；附件支持图片（内嵌预览）与 Word（仅下载）。
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="fb-submit-form">
      <!-- 反馈类型 -->
      <el-form-item label="反馈类型" prop="type">
        <div class="fb-type-cards">
          <div
            v-for="item in typeCards"
            :key="item.value"
            class="fb-type-card"
            :class="['tone-' + item.tone, { 'is-active': form.type === item.value }]"
            tabindex="0"
            @click="pickType(item.value)"
            @keydown.enter.prevent="pickType(item.value)"
            @keydown.space.prevent="pickType(item.value)"
          >
            <div class="fb-type-card__ico">{{ item.icon }}</div>
            <div class="fb-type-card__name">{{ item.label }}</div>
            <div class="fb-type-card__desc">{{ item.desc }}</div>
          </div>
        </div>
      </el-form-item>

      <!-- 标题 -->
      <el-form-item label="标题" prop="title">
        <el-input
          v-model="form.title"
          maxlength="50"
          show-word-limit
          placeholder="一句话描述问题，例如：成员管理筛选「已退会」时列表偶发空白"
        />
        <div class="fb-field-tip">2–50 字</div>
      </el-form-item>

      <!-- 影响模块 / 页面（可选） -->
      <el-form-item label="影响模块（两级：模块 → 页面）" prop="moduleId">
        <div class="fb-cascade">
          <el-select
            v-model="form.moduleId"
            placeholder="请选择模块"
            filterable
            clearable
            :loading="moduleLoading"
            style="flex: 1"
            @change="handleModuleChange"
          >
            <el-option
              v-for="item in moduleOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-select
            v-model="form.pageId"
            :placeholder="form.moduleId ? '请选择页面' : '请先选择模块'"
            :disabled="!form.moduleId"
            filterable
            clearable
            style="flex: 1"
          >
            <el-option
              v-for="item in pageOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </div>
      </el-form-item>

      <!-- 详细描述 -->
      <el-form-item label="详细描述" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="5"
          maxlength="2000"
          show-word-limit
          placeholder="发生了什么？在哪个页面？当时的操作是什么？（10–2000 字）"
        />
        <div class="fb-field-tip">10–2000 字，描述越清楚越容易被快速处理</div>
      </el-form-item>

      <!-- 复现步骤 -->
      <el-form-item label="复现步骤" prop="steps">
        <el-input
          v-model="form.steps"
          type="textarea"
          :rows="3"
          maxlength="2000"
          show-word-limit
          placeholder="1. 打开成员管理&#10;2. 状态筛选选择「已退会」&#10;3. 点击搜索"
        />
      </el-form-item>

      <!-- 期望结果 -->
      <el-form-item label="期望结果" prop="expectResult">
        <el-input
          v-model="form.expectResult"
          type="textarea"
          :rows="2"
          maxlength="1000"
          show-word-limit
          placeholder="你希望页面如何表现？"
        />
      </el-form-item>

      <!-- 附件 -->
      <el-form-item label="附件">
        <feedback-attachment-uploader
          v-model="form.files"
          :disabled="submitting"
          @uploading-change="uploadingCount = $event"
        />
      </el-form-item>

      <!-- 匿名 -->
      <el-form-item label=" ">
        <div class="fb-anon-row">
          <div class="fb-anon-main">
            <div class="fb-anon-title">
              匿名提交
              <span class="oa-pill oa-pill--gray">可选</span>
            </div>
            <div class="fb-anon-hint" :class="{ 'is-on': form.isAnonymous }">
              {{ anonymousHint }}
            </div>
          </div>
          <el-switch v-model="form.isAnonymous" />
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="fb-dialog-foot">
        <el-button :disabled="submitting" @click="visible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="uploadingCount > 0" @click="handleSubmit">提交工单</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="FeedbackSubmit">
import { addFeedback, getModuleOptions, FEEDBACK_TYPE_META } from '@/api/oa/feedback'
import FeedbackAttachmentUploader from './components/AttachmentUploader.vue'
import useUserStore from '@/store/modules/user'

/**
 * 提交工单（760px 弹窗，契约 §4.1 / 原型 ②）
 * - 类型卡片单选 / 标题 2–50 / 模块→页面级联 / 描述 10–2000 / 步骤 / 期望 / 附件 / 匿名
 * - 附件走 AttachmentUploader（拖拽 + Ctrl+V + 缩略图/Word + 删除）
 * - 提交成功 toast 显示工单编号并刷新列表
 */
const { proxy } = getCurrentInstance()
const emit = defineEmits(['success'])
const userStore = useUserStore()

const visible = ref(false)
const submitting = ref(false)
const uploadingCount = ref(0)
const formRef = ref()
const moduleOptions = ref([])
const moduleLoading = ref(false)

const typeCards = Object.keys(FEEDBACK_TYPE_META).map(value => ({
  value,
  ...FEEDBACK_TYPE_META[value]
}))

const form = reactive({
  type: '',
  title: '',
  moduleId: undefined,
  pageId: undefined,
  content: '',
  steps: '',
  expectResult: '',
  isAnonymous: false,
  files: []
})

const anonymousHint = computed(() => {
  if (form.isAnonymous) {
    const name = userStore.nickName || userStore.name || '当前用户'
    return '已开启匿名：其他用户只会看到「匿名用户」，仅管理员可见你的真实身份（' + name + '）。'
  }
  return '开启后其他人只看到「匿名用户」，仅管理员可见你的真实身份'
})

const pageOptions = computed(() => {
  const hit = moduleOptions.value.find(item => String(item.id) === String(form.moduleId))
  return hit && Array.isArray(hit.children) ? hit.children : []
})

/* -------------------------------------------------------------------------- */
/* 校验                                                                       */
/* -------------------------------------------------------------------------- */
const rules = {
  type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
  title: [
    { required: true, message: '标题需 2–50 字', trigger: 'blur' },
    { validator: validateTitle, trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请填写详细描述', trigger: 'blur' },
    { validator: validateContent, trigger: 'blur' }
  ],
  steps: [{ max: 2000, message: '复现步骤最多 2000 字', trigger: 'blur' }],
  expectResult: [{ max: 1000, message: '期望结果最多 1000 字', trigger: 'blur' }]
}

function validateTitle(rule, value, callback) {
  const text = String(value || '').trim()
  if (text.length < 2 || text.length > 50) {
    return callback(new Error('标题需 2–50 字（当前 ' + text.length + ' 字）'))
  }
  callback()
}

function validateContent(rule, value, callback) {
  const text = String(value || '').trim()
  if (text.length < 10) {
    return callback(new Error('详细描述至少 10 字（当前 ' + text.length + ' 字）'))
  }
  if (text.length > 2000) {
    return callback(new Error('详细描述最多 2000 字'))
  }
  callback()
}

/* -------------------------------------------------------------------------- */
/* 打开 / 关闭 / 模块选项                                                      */
/* -------------------------------------------------------------------------- */
function open() {
  resetForm()
  visible.value = true
  loadModuleOptions()
}

function resetForm() {
  form.type = ''
  form.title = ''
  form.moduleId = undefined
  form.pageId = undefined
  form.content = ''
  form.steps = ''
  form.expectResult = ''
  form.isAnonymous = false
  form.files = []
  nextTick(() => formRef.value && formRef.value.clearValidate())
}

function handleClosed() {
  resetForm()
}

function pickType(type) {
  form.type = type
  if (formRef.value) formRef.value.clearValidate('type')
}

function handleModuleChange() {
  form.pageId = undefined
  if (formRef.value) formRef.value.clearValidate('moduleId')
}

/** 模块选项接口失败时不再使用「中文串兜底」：后端只认 sys_menu 的真实 id，兜底数据无法保存 */
function loadModuleOptions() {
  if (moduleLoading.value) return
  moduleLoading.value = true
  getModuleOptions()
    .then(response => {
      const normalized = normalizeModuleOptions(response && response.data)
      moduleOptions.value = normalized
    })
    .catch(() => {
      moduleOptions.value = []
      proxy.$modal.msgWarning('影响模块加载失败，可稍后重新打开弹窗重试（不选模块也可提交）')
    })
    .finally(() => {
      moduleLoading.value = false
    })
}

/** 兼容 {id,name,children} / {value,label} / {menuId,menuName} 三种后端命名 */
function normalizeModuleOptions(list) {
  if (!Array.isArray(list)) return []
  return list.map(item => ({
    id: pickOptionValue(item),
    name: pickOptionLabel(item),
    children: Array.isArray(item.children)
      ? item.children.map(child => ({ id: pickOptionValue(child), name: pickOptionLabel(child) }))
      : []
  }))
}

function pickOptionValue(item) {
  if (!item) return undefined
  return item.id !== undefined ? item.id : item.value !== undefined ? item.value : item.menuId
}

function pickOptionLabel(item) {
  if (!item) return ''
  return item.name || item.label || item.menuName || ''
}

/* -------------------------------------------------------------------------- */
/* 提交                                                                       */
/* -------------------------------------------------------------------------- */
async function handleSubmit() {
  if (submitting.value) return
  if (uploadingCount.value > 0) {
    proxy.$modal.msgWarning('附件正在上传，请等待上传完成后再提交')
    return
  }
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

  submitting.value = true
  try {
    const response = await addFeedback(buildPayload())
    const data = response && response.data
    const ticketNo =
      typeof data === 'string'
        ? data
        : data && typeof data === 'object'
          ? data.ticketNo || data.no || data.feedbackNo || ''
          : ''
    proxy.$modal.msgSuccess(ticketNo ? '提交成功，工单编号 ' + ticketNo : '提交成功')
    visible.value = false
    emit('success')
  } catch (e) {
    // 失败原因由 request 拦截器统一提示，保留表单内容
  } finally {
    submitting.value = false
  }
}

function buildPayload() {
  const moduleItem = moduleOptions.value.find(item => String(item.id) === String(form.moduleId))
  const pageItem = pageOptions.value.find(item => String(item.id) === String(form.pageId))
  return {
    type: form.type,
    title: String(form.title || '').trim(),
    moduleId: toNumericId(form.moduleId),
    moduleName: moduleItem ? moduleItem.name : '',
    pageId: toNumericId(form.pageId),
    pageName: pageItem ? pageItem.name : '',
    content: String(form.content || '').trim(),
    steps: String(form.steps || '').trim(),
    expectResult: String(form.expectResult || '').trim(),
    isAnonymous: form.isAnonymous ? '1' : '0',
    fileIds: (form.files || []).map(file => file.fileId).filter(Boolean)
  }
}

function toNumericId(value) {
  if (value === undefined || value === null || value === '') return null
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.fb-submit-dialog :deep(.el-dialog__body) {
  max-height: calc(82vh - 130px);
  overflow-y: auto;
  padding-top: 16px;
}

.fb-submit-tip {
  margin-bottom: 14px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-soft);
}

.fb-required {
  color: var(--red-text);
}

.fb-field-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--ink-faint);
}

.fb-submit-form {
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 6px;
    font-size: 13px;
    color: var(--ink-muted);
  }
}

/* ---------- 类型卡片 ---------- */
.fb-type-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

@media (max-width: 720px) {
  .fb-type-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.fb-type-card {
  padding: 14px 10px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-sm);
  background: var(--surface-solid);
  text-align: center;
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    box-shadow: var(--e1);
  }

  &.is-active {
    border-color: var(--blue);
    background: var(--blue-soft);
    box-shadow: 0 0 0 2px var(--blue-soft);
  }

  &.tone-red.is-active {
    border-color: var(--red-text);
    background: var(--red-soft);
    box-shadow: 0 0 0 2px var(--red-soft);
  }

  &.tone-green.is-active {
    border-color: var(--lime-text);
    background: var(--lime-soft);
    box-shadow: 0 0 0 2px var(--lime-soft);
  }

  &.tone-gray.is-active {
    border-color: var(--neutral);
    background: var(--neutral-soft);
    box-shadow: 0 0 0 2px var(--neutral-soft);
  }
}

.fb-type-card__ico {
  font-size: 20px;
  line-height: 1.2;
}

.fb-type-card__name {
  margin-top: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.fb-type-card__desc {
  margin-top: 4px;
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--ink-soft);
}

/* ---------- 级联 ---------- */
.fb-cascade {
  display: flex;
  gap: 10px;
  width: 100%;
}

/* ---------- 匿名 ---------- */
.fb-anon-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: var(--r-xs);
  background: var(--hover-bg);
}

.fb-anon-main {
  flex: 1;
  min-width: 0;
}

.fb-anon-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}

.fb-anon-hint {
  margin-top: 5px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink-soft);

  &.is-on {
    color: var(--blue);
  }
}

.fb-dialog-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
