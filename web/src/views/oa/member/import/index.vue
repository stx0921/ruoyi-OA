<template>
  <div class="app-container import-page">
    <div class="oa-page-title import-page__title">
      <span>批量导入成员</span>
      <small>支持 .xlsx，单次 ≤500 行</small>
      <span class="import-page__title-grow"></span>
      <el-button v-hasPermi="['oa:member:import']" icon="Download" :loading="templateLoading" @click="handleDownloadTemplate">
        下载模板
      </el-button>
      <el-button v-if="step > 1 && step < 3" icon="Close" @click="handleExit">退出导入</el-button>
    </div>

    <!-- 步骤条 -->
    <div class="oa-card import-steps-card">
      <div class="import-steps">
        <div
          v-for="item in stepItems"
          :key="item.value"
          class="import-step"
          :class="{
            'is-done': step > item.value,
            'is-current': step === item.value,
            'is-clickable': item.value < step && step < 3
          }"
          @click="handleStepClick(item.value)"
        >
          <span class="import-step__circle">{{ step > item.value ? '✓' : item.value }}</span>
          <span class="import-step__text">{{ item.label }}</span>
          <span v-if="item.value < 3" class="import-step__bar"></span>
        </div>
      </div>
    </div>

    <!-- 步骤 1：下载模板与填写说明 -->
    <div v-show="step === 1" class="oa-card">
      <div class="oa-card-head">
        <h3>第 1 步 · 下载模板与填写说明</h3>
        <span class="oa-muted">模板版本 V1 · 12 列</span>
      </div>

      <div class="import-notice import-notice--blue">
        模板共 <b>12 列</b>，其中 <b>9 列必填</b>（带 <span class="import-required">*</span>）。请勿修改表头、请勿新增列；一行一位成员，第 1 行为表头，第 2 行为示例行。
      </div>

      <div class="import-actions">
        <el-button v-hasPermi="['oa:member:import']" type="primary" icon="Download" :loading="templateLoading" @click="handleDownloadTemplate">
          下载导入模板（.xlsx）
        </el-button>
        <el-button icon="ArrowRight" @click="goStep(2)">下一步：上传文件</el-button>
      </div>

      <div class="import-col-grid">
        <div v-for="col in templateColumns" :key="col.name" class="import-col-card">
          <div class="import-col-card__name">
            <span v-if="col.required" class="import-required">*</span>{{ col.name }}
          </div>
          <div class="import-col-card__desc">{{ col.desc }}</div>
        </div>
      </div>

      <div class="import-notice">
        <b>字段规则：</b>学号 8–12 位数字，全局唯一；手机号 11 位且唯一（同时用于初始密码 = 手机号后 6 位）；
        年级取字典（大一 / 大二 / 大三 / 大四 / 研一 / 研二 / 研三）；部门须与「部门管理」中的名称完全一致；
        职务留空默认「干事」，届次留空默认取入会年份，入会日期格式 yyyy-MM-dd、留空默认导入当天。
      </div>
    </div>

    <!-- 步骤 2：上传并校验 -->
    <div v-show="step === 2" class="oa-card">
      <div class="oa-card-head">
        <h3>第 2 步 · 上传并校验</h3>
        <span class="oa-muted">上传后自动执行行级校验，不写库</span>
      </div>

      <!-- 上传区 -->
      <div
        class="import-dropzone"
        @click="triggerFileSelect"
        @dragover.prevent
        @drop.prevent="handleDrop"
      >
        <el-icon :size="30" class="import-dropzone__icon"><UploadFilled /></el-icon>
        <div class="import-dropzone__main">
          将 Excel 文件拖到此处，或 <span class="import-dropzone__link">点击选择文件</span>
        </div>
        <div class="import-dropzone__sub">仅支持 .xlsx，单文件 ≤10MB，单批 ≤500 行</div>
        <input ref="fileInputRef" type="file" accept=".xlsx" class="import-file-input" @change="handleFileInput" />
      </div>

      <!-- 已选文件 -->
      <div v-if="selectedFile" class="import-file-card">
        <el-icon class="import-file-card__icon" :size="18"><Document /></el-icon>
        <span class="import-file-card__name">{{ selectedFile.name }}</span>
        <span class="oa-muted">{{ formatFileSize(selectedFile.size) }}</span>
        <span v-if="validating" class="oa-pill oa-pill--orange">校验中</span>
        <span v-else-if="validateError" class="oa-pill oa-pill--red">校验失败</span>
        <span v-else-if="validateResult" class="oa-pill oa-pill--green">校验完成</span>
        <span class="import-file-card__grow"></span>
        <el-button link type="primary" @click.stop="triggerFileSelect">重新上传</el-button>
        <el-button link type="danger" @click.stop="removeFile">删除</el-button>
      </div>

      <!-- 校验进度 -->
      <div v-if="validating" class="import-progress">
        <div class="import-progress__text">正在校验{{ estimatedRows ? ' ' + estimatedRows + ' 行' : '文件' }}，请稍候…</div>
        <el-progress :percentage="progress" :show-text="false" :stroke-width="6" />
      </div>

      <!-- 校验失败 -->
      <div v-else-if="validateError" class="import-notice import-notice--red">
        {{ validateError }}
        <el-button link type="primary" @click="runValidate">重试校验</el-button>
      </div>

      <!-- 校验结果 -->
      <template v-if="validateResult && !validating">
        <div class="import-result-grid">
          <div class="import-result-card is-ok">
            <div class="import-result-card__num">{{ validateResult.validRows || 0 }}</div>
            <div class="import-result-card__label">通过</div>
          </div>
          <div class="import-result-card is-warn">
            <div class="import-result-card__num">{{ validateResult.skipRows || 0 }}</div>
            <div class="import-result-card__label">警告</div>
          </div>
          <div class="import-result-card is-fail">
            <div class="import-result-card__num">{{ validateResult.invalidRows || 0 }}</div>
            <div class="import-result-card__label">错误</div>
          </div>
          <div class="import-result-card is-total">
            <div class="import-result-card__num">{{ validateResult.totalRows || 0 }}</div>
            <div class="import-result-card__label">总计</div>
          </div>
        </div>

        <!-- 重复处理策略 -->
        <div class="import-strategy">
          <span class="import-strategy__label">重复处理策略</span>
          <el-radio-group v-model="updateSupport" :disabled="validating" @change="handleStrategyChange">
            <el-radio value="1">更新已有档案</el-radio>
            <el-radio value="0">跳过重复行</el-radio>
          </el-radio-group>
          <span class="oa-muted import-strategy__tip">
            「更新已有档案」将按学号更新院系 / 班级 / 年级 / 部门 / 职务 / 入会日期与联系方式，不改成员编号、状态与账号。
          </span>
        </div>

        <!-- 错误报告表 -->
        <div class="import-error-block">
          <div class="import-error-head">
            <span class="oa-strong">错误报告</span>
            <span class="oa-muted">共 {{ errorRows.length }} 条错误，需修正后重新上传</span>
            <span class="import-file-card__grow"></span>
            <el-button
              v-if="validateResult.errorFileUrl"
              link
              type="primary"
              :loading="errorFileLoading"
              @click="handleDownloadValidateErrors"
            >下载错误明细</el-button>
            <el-select v-model="errorFilter" placeholder="全部错误类型" clearable style="width: 180px">
              <el-option v-for="item in errorColumns" :key="item" :label="item" :value="item" />
            </el-select>
          </div>
          <el-table :data="filteredErrorRows" height="320" class="import-error-table" empty-text="没有校验错误">
            <el-table-column label="行号" width="60" align="center">
              <template #default="scope">
                <span class="import-error-row-num">{{ scope.row.rowNum }}</span>
              </template>
            </el-table-column>
            <el-table-column label="列名" width="120" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.column || '—' }}</template>
            </el-table-column>
            <el-table-column label="原值" width="160" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.value || '（空）' }}</template>
            </el-table-column>
            <el-table-column label="错误原因" width="260" show-overflow-tooltip>
              <template #default="scope">
                <span class="import-error-reason">{{ scope.row.msg }}</span>
              </template>
            </el-table-column>
            <el-table-column label="修正建议" width="240" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.suggestion || '请修正后重新上传' }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div class="import-step-foot">
          <el-button @click="goStep(1)">上一步</el-button>
          <div class="import-step-foot__right">
            <span class="oa-muted">第 2 / 3 步 · 校验不写库，确认后才入库</span>
            <el-tooltip :disabled="!confirmDisabled" :content="confirmDisabledTip" placement="top">
              <span>
                <el-button
                  type="primary"
                  :disabled="confirmDisabled"
                  v-hasPermi="['oa:member:import']"
                  @click="handleOpenConfirm"
                >确认导入</el-button>
              </span>
            </el-tooltip>
          </div>
        </div>
      </template>

      <div v-if="!selectedFile && !validating" class="import-empty-tip oa-muted">
        点击或拖拽上传 Excel，仅支持 .xlsx，单次 ≤500 行
      </div>
    </div>

    <!-- 步骤 3：确认导入结果 -->
    <div v-show="step === 3" class="oa-card">
      <div class="oa-card-head">
        <h3>第 3 步 · 确认导入结果</h3>
        <span class="oa-muted">批次号 {{ importResult.batchNo || '—' }}</span>
      </div>

      <div class="import-result-grid">
        <div class="import-result-card is-ok">
          <div class="import-result-card__num">{{ importResult.successRows || 0 }}</div>
          <div class="import-result-card__label">成功导入</div>
        </div>
        <div class="import-result-card is-info">
          <div class="import-result-card__num">{{ importResult.updateRows || 0 }}</div>
          <div class="import-result-card__label">更新档案</div>
        </div>
        <div class="import-result-card is-warn">
          <div class="import-result-card__num">{{ importResult.skipRows || 0 }}</div>
          <div class="import-result-card__label">跳过</div>
        </div>
        <div class="import-result-card is-fail">
          <div class="import-result-card__num">{{ importResult.failRows || 0 }}</div>
          <div class="import-result-card__label">失败</div>
        </div>
      </div>

      <div class="import-notice import-notice--blue">
        导入完成：新增 <b>{{ importResult.successRows || 0 }}</b> 条、更新 <b>{{ importResult.updateRows || 0 }}</b> 条、
        跳过 <b>{{ importResult.skipRows || 0 }}</b> 条、失败 <b>{{ importResult.failRows || 0 }}</b> 条。
        新成员已建档案并开通账号（账号 = 学号，初始密码 = 手机号后 6 位）。
      </div>

      <el-descriptions :column="2" border class="import-result-desc">
        <el-descriptions-item label="导入批次">{{ importResult.batchNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="总行数">{{ importResult.totalRows || 0 }}</el-descriptions-item>
        <el-descriptions-item label="重复处理策略">{{ updateSupport === '1' ? '更新已有档案' : '跳过重复行' }}</el-descriptions-item>
        <el-descriptions-item label="失败行数">{{ importResult.failRows || 0 }}</el-descriptions-item>
      </el-descriptions>

      <div class="import-step-foot">
        <div class="import-step-foot__right">
          <el-button
            v-if="importResult.errorFileUrl || importResult.failRows"
            icon="Download"
            :loading="errorFileLoading"
            @click="handleDownloadErrors"
          >下载错误明细{{ importResult.failRows ? '（' + importResult.failRows + ' 条）' : '' }}</el-button>
          <el-button type="primary" @click="backToMemberList">返回成员列表</el-button>
        </div>
      </div>
    </div>

    <!-- 确认导入二次确认（760px） -->
    <el-dialog v-model="confirmVisible" title="确认导入" width="760px" append-to-body :close-on-click-modal="false">
      <div class="import-confirm-text">
        将新增 <b>{{ validateResult.newRows || 0 }}</b> 条、更新 <b>{{ validateResult.updateRows || 0 }}</b> 条、跳过
        <b>{{ validateResult.skipRows || 0 }}</b> 条，确认导入？导入后立即生效并开通账号。
      </div>
      <template #footer>
        <el-button @click="confirmVisible = false">取 消</el-button>
        <el-button type="primary" :loading="importing" @click="handleConfirmImport">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="OaMemberImport">
import { saveAs } from 'file-saver'
import { downloadImportTemplate, validateImport, confirmImport, downloadImportError } from '@/api/oa/import'
import { blobValidate, parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()
const router = useRouter()
const fileInputRef = ref()

/* ------------------------------ 步骤 / 状态 ------------------------------ */
const step = ref(1)
const stepItems = [
  { value: 1, label: '下载模板与填写说明' },
  { value: 2, label: '上传并校验' },
  { value: 3, label: '确认导入结果' }
]

const templateLoading = ref(false)
const selectedFile = ref(null)
const estimatedRows = ref(0)
const validating = ref(false)
const validateResult = ref(null)
const validateError = ref('')
const progress = ref(0)
let progressTimer = null

const updateSupport = ref('1')
const errorFilter = ref('')
const confirmVisible = ref(false)
const importing = ref(false)
const importResult = ref({})
const errorFileLoading = ref(false)

/* 模板 12 列说明（与 03-开发文档 §6.4 一致） */
const templateColumns = [
  { name: '姓名', required: true, desc: '2–20 字，与学籍一致。例：陈嘉树' },
  { name: '性别', required: true, desc: '填「男」或「女」。例：男' },
  { name: '学号', required: true, desc: '8–12 位纯数字，全局唯一。例：2026010202' },
  { name: '手机号', required: true, desc: '11 位，唯一；同时用于初始密码。例：13800135678' },
  { name: '学院', required: true, desc: '文本，填写全称。例：计算机科学与技术学院' },
  { name: '专业', required: true, desc: '文本。例：软件工程' },
  { name: '班级', required: true, desc: '文本。例：2026 级软件工程 2 班' },
  { name: '年级', required: true, desc: '大一 / 大二 / 大三 / 大四 / 研一 / 研二 / 研三' },
  { name: '部门', required: true, desc: '须与「部门管理」名称完全一致。例：组织部' },
  { name: '职务', required: false, desc: '留空默认「干事」；会长 / 副会长 / 部长 / 副部长 / 干事' },
  { name: '届次', required: false, desc: '留空默认取入会年份。例：2026 届' },
  { name: '入会日期', required: false, desc: 'yyyy-MM-dd，留空默认导入当天。例：2026-11-01' }
]

/* ------------------------------ 步骤切换 ------------------------------ */
function goStep(target) {
  if (target === step.value) return
  if (step.value === 3) return
  if (target === 3) return
  step.value = target
  window.scrollTo(0, 0)
}

function handleStepClick(target) {
  // 仅允许回退到已完成步骤；进入步骤 3 后不可回退
  if (step.value === 3) return
  if (target < step.value) goStep(target)
}

/* ------------------------------ 模板下载 ------------------------------ */
function handleDownloadTemplate() {
  if (templateLoading.value) return
  templateLoading.value = true
  downloadImportTemplate()
    .then(data => {
      if (!blobValidate(data)) {
        proxy.$modal.msgError('模板下载失败，请稍后重试')
        return
      }
      saveAs(new Blob([data]), '成员导入模板.xlsx')
    })
    .catch(() => proxy.$modal.msgError('模板下载失败，请稍后重试'))
    .finally(() => {
      templateLoading.value = false
    })
}

/* ------------------------------ 文件选择 ------------------------------ */
function triggerFileSelect() {
  if (validating.value || importing.value) return
  fileInputRef.value && fileInputRef.value.click()
}

function handleFileInput(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (file) handlePickedFile(file)
}

function handleDrop(event) {
  if (validating.value || importing.value) return
  const file = event.dataTransfer && event.dataTransfer.files && event.dataTransfer.files[0]
  if (file) handlePickedFile(file)
}

async function handlePickedFile(file) {
  if (!/\.xlsx$/i.test(file.name || '')) {
    proxy.$modal.msgError('仅支持 .xlsx 文件，请重新选择')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    proxy.$modal.msgError('文件不能超过 10MB')
    return
  }
  const rowCheck = await precheckRowLimit(file)
  if (rowCheck === false) return
  selectedFile.value = { name: file.name, size: file.size, raw: file }
  validateResult.value = null
  validateError.value = ''
  step.value = 2
  runValidate()
}

/**
 * 500 行前置校验：直接解析 xlsx（zip）里的首个 worksheet，统计 <row> 数量
 * 解析失败（旧浏览器 / 非常规文件）时返回 null，交由后端行数校验兜底
 */
async function precheckRowLimit(file) {
  estimatedRows.value = 0
  const rows = await countXlsxRows(file)
  if (rows === null) return true
  const dataRows = Math.max(0, rows - 1)
  estimatedRows.value = dataRows
  if (dataRows > 500) {
    proxy.$modal.msgError('单次最多导入 500 行，当前 ' + dataRows + ' 行，请拆分后导入')
    return false
  }
  return true
}

async function countXlsxRows(file) {
  try {
    if (typeof DecompressionStream === 'undefined') return null
    const buffer = await file.arrayBuffer()
    const bytes = new Uint8Array(buffer)
    const view = new DataView(buffer)
    // 1. 找 EOCD
    let eocd = -1
    const minPos = Math.max(0, bytes.length - 65557)
    for (let index = bytes.length - 22; index >= minPos; index--) {
      if (view.getUint32(index, true) === 0x06054b50) {
        eocd = index
        break
      }
    }
    if (eocd < 0) return null
    // 2. 遍历中央目录，找首个 worksheet
    const totalEntries = view.getUint16(eocd + 10, true)
    let offset = view.getUint32(eocd + 16, true)
    let sheet = null
    for (let index = 0; index < totalEntries; index++) {
      if (view.getUint32(offset, true) !== 0x02014b50) break
      const nameLen = view.getUint16(offset + 28, true)
      const extraLen = view.getUint16(offset + 30, true)
      const commentLen = view.getUint16(offset + 32, true)
      const compression = view.getUint16(offset + 10, true)
      const compSize = view.getUint32(offset + 20, true)
      const localOffset = view.getUint32(offset + 42, true)
      const name = new TextDecoder().decode(bytes.slice(offset + 46, offset + 46 + nameLen))
      if (!sheet && /^xl\/worksheets\/sheet\d+\.xml$/.test(name)) {
        sheet = { compression, compSize, localOffset }
      }
      offset += 46 + nameLen + extraLen + commentLen
    }
    if (!sheet) return null
    // 3. 读本地文件头定位数据区
    const local = sheet.localOffset
    if (view.getUint32(local, true) !== 0x04034b50) return null
    const localNameLen = view.getUint16(local + 26, true)
    const localExtraLen = view.getUint16(local + 28, true)
    const start = local + 30 + localNameLen + localExtraLen
    let data = bytes.slice(start, start + sheet.compSize)
    if (sheet.compression === 8) {
      const stream = new Blob([data]).stream().pipeThrough(new DecompressionStream('deflate-raw'))
      data = new Uint8Array(await new Response(stream).arrayBuffer())
    } else if (sheet.compression !== 0) {
      return null
    }
    const xml = new TextDecoder().decode(data)
    const matched = xml.match(/<row[ >]/g)
    return matched ? matched.length : 0
  } catch (e) {
    return null
  }
}

function removeFile() {
  if (validating.value || importing.value) return
  selectedFile.value = null
  validateResult.value = null
  validateError.value = ''
  estimatedRows.value = 0
}

/* ------------------------------ 校验 ------------------------------ */
function startProgress() {
  progress.value = 5
  clearInterval(progressTimer)
  progressTimer = setInterval(() => {
    progress.value = Math.min(90, progress.value + Math.max(1, (90 - progress.value) * 0.08))
  }, 280)
}

function stopProgress() {
  clearInterval(progressTimer)
  progressTimer = null
  progress.value = 100
}

async function runValidate() {
  if (!selectedFile.value || validating.value) return
  validating.value = true
  validateError.value = ''
  validateResult.value = null
  startProgress()
  try {
    const response = await validateImport(selectedFile.value.raw, updateSupport.value)
    validateResult.value = response.data || {}
  } catch (e) {
    validateError.value = '校验服务暂时不可用，请稍后重试'
  } finally {
    stopProgress()
    validating.value = false
  }
}

/** 重复策略切换：用同一文件重新校验（不要求用户重新上传），重新统计新增 / 更新 / 跳过 */
function handleStrategyChange() {
  if (selectedFile.value && !validating.value) runValidate()
}

/* ------------------------------ 确认导入 ------------------------------ */
const confirmDisabled = computed(() => {
  if (validating.value || importing.value) return true
  if (!validateResult.value || !validateResult.value.batchId) return true
  return Number(validateResult.value.invalidRows || 0) > 0
})

const confirmDisabledTip = computed(() => {
  const count = Number((validateResult.value && validateResult.value.invalidRows) || 0)
  return count > 0 ? '请先修正 ' + count + ' 处错误后重新上传' : '请先上传并校验文件'
})

function handleOpenConfirm() {
  if (confirmDisabled.value) return
  confirmVisible.value = true
}

async function handleConfirmImport() {
  if (!validateResult.value || importing.value) return
  importing.value = true
  try {
    const response = await confirmImport({
      batchId: validateResult.value.batchId,
      updateSupport: updateSupport.value
    })
    importResult.value = response.data || {}
    confirmVisible.value = false
    step.value = 3
    proxy.$modal.msgSuccess(
      '导入完成：新增 ' +
        (importResult.value.successRows || 0) +
        ' 条、更新 ' +
        (importResult.value.updateRows || 0) +
        ' 条、跳过 ' +
        (importResult.value.skipRows || 0) +
        ' 条、失败 ' +
        (importResult.value.failRows || 0) +
        ' 条'
    )
  } catch (e) {
    // 失败原因由 request 拦截器统一提示，保留当前步骤可重试
    confirmVisible.value = false
  } finally {
    importing.value = false
  }
}

/* ------------------------------ 错误明细 / 返回 ------------------------------ */
function errorFileIdOf(url) {
  if (!url) return ''
  const matched = String(url).match(/\/oa\/file\/(\d+)/)
  if (matched) return matched[1]
  const pure = String(url).match(/^\d+$/)
  return pure ? pure[0] : ''
}

function handleDownloadErrors() {
  doDownloadErrorFile(importResult.value.errorFileUrl)
}

function handleDownloadValidateErrors() {
  doDownloadErrorFile(validateResult.value && validateResult.value.errorFileUrl)
}

function doDownloadErrorFile(url) {
  const fileId = errorFileIdOf(url)
  if (!fileId) {
    proxy.$modal.msgWarning('暂无可下载的错误明细')
    return
  }
  errorFileLoading.value = true
  downloadImportError(fileId)
    .then(data => {
      if (!blobValidate(data)) {
        proxy.$modal.msgError('错误明细下载失败，请稍后重试')
        return
      }
      saveAs(new Blob([data]), '导入错误明细_' + parseTime(new Date(), '{y}{m}{d}{h}{i}{s}') + '.xlsx')
    })
    .catch(() => proxy.$modal.msgError('错误明细下载失败，请稍后重试'))
    .finally(() => {
      errorFileLoading.value = false
    })
}

function backToMemberList() {
  allowLeave = true
  const candidates = ['/hr/member', '/oa/member']
  const target = candidates.find(path => router.resolve(path).matched.length > 0)
  router.push(target || candidates[0])
}

/* ------------------------------ 离开拦截 ------------------------------ */
let allowLeave = false

const shouldGuard = computed(() => {
  if (step.value === 3) return false
  if (step.value === 2) return true
  return false
})

let beforeUnloadHandler = null

function syncBeforeUnload() {
  const needGuard = importing.value || validating.value
  if (needGuard && !beforeUnloadHandler) {
    beforeUnloadHandler = event => {
      event.preventDefault()
      event.returnValue = '正在导入，请勿关闭页面'
      return '正在导入，请勿关闭页面'
    }
    window.addEventListener('beforeunload', beforeUnloadHandler)
    return
  }
  if (!needGuard && beforeUnloadHandler) {
    window.removeEventListener('beforeunload', beforeUnloadHandler)
    beforeUnloadHandler = null
  }
}

watch([importing, validating], syncBeforeUnload)
onMounted(() => {
  syncBeforeUnload()
})
onBeforeUnmount(() => {
  clearInterval(progressTimer)
  if (beforeUnloadHandler) {
    window.removeEventListener('beforeunload', beforeUnloadHandler)
    beforeUnloadHandler = null
  }
})

function handleExit() {
  if (importing.value) {
    proxy.$modal.msgWarning('正在导入，请勿关闭')
    return
  }
  proxy.$modal
    .confirm('退出将丢失当前导入进度，确认退出？')
    .then(backToMemberList)
    .catch(() => {})
}

onBeforeRouteLeave((to, from, next) => {
  if (allowLeave) {
    allowLeave = false
    next()
    return
  }
  if (importing.value) {
    proxy.$modal.msgWarning('正在导入，请勿关闭页面')
    next(false)
    return
  }
  if (!shouldGuard.value) {
    next()
    return
  }
  proxy.$modal
    .confirm('退出将丢失当前导入进度，确认退出？')
    .then(() => next())
    .catch(() => next(false))
})

/* ------------------------------ 工具 ------------------------------ */
function formatFileSize(size) {
  const value = Number(size) || 0
  if (value < 1024) return value + ' B'
  if (value < 1024 * 1024) return (value / 1024).toFixed(1) + ' KB'
  return (value / 1024 / 1024).toFixed(1) + ' MB'
}

/* ------------------------------ 校验结果派生 ------------------------------ */
const errorRows = computed(() => {
  const rows = (validateResult.value && validateResult.value.rows) || []
  const list = []
  rows.forEach(row => {
    const errors = row.errors || []
    errors.forEach(error => {
      list.push({
        rowNum: row.rowNum,
        studentNo: row.studentNo,
        name: row.name,
        column: error.column || '',
        value: error.value === undefined || error.value === null ? '' : String(error.value),
        msg: error.msg || '',
        suggestion: error.suggestion || ''
      })
    })
  })
  return list
})

const errorColumns = computed(() => {
  const set = []
  errorRows.value.forEach(row => {
    if (row.column && set.indexOf(row.column) === -1) set.push(row.column)
  })
  return set
})

const filteredErrorRows = computed(() => {
  if (!errorFilter.value) return errorRows.value
  return errorRows.value.filter(row => row.column === errorFilter.value)
})
</script>

<style scoped lang="scss">
.import-page__title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.import-page__title-grow {
  flex: 1;
}

.import-required {
  color: #f56c6c;
  margin-right: 2px;
}

/* ---------- 步骤条 ---------- */
.import-steps-card {
  padding: 14px 20px;
  margin-bottom: 16px;
}

.import-steps {
  display: flex;
  align-items: center;
  width: 720px;
  max-width: 100%;
  margin: 0 auto;
}

.import-step {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.import-step.is-clickable {
  cursor: pointer;
}

.import-step__circle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #909399;
  font-size: 12px;
  flex: 0 0 24px;
}

.import-step__text {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
}

.import-step__bar {
  flex: 1;
  height: 2px;
  margin: 0 10px;
  background: #ebeef5;
}

.import-step.is-done .import-step__circle {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.import-step.is-done .import-step__bar {
  background: #409eff;
}

.import-step.is-done .import-step__text {
  color: #606266;
}

.import-step.is-current .import-step__circle {
  border-color: #409eff;
  color: #409eff;
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.15);
}

.import-step.is-current .import-step__text {
  color: #409eff;
  font-weight: 600;
}

/* ---------- notice / 说明 ---------- */
.import-notice {
  padding: 10px 14px;
  margin-bottom: 16px;
  border-radius: 4px;
  background: #f4f4f5;
  border: 1px solid #ebeef5;
  font-size: 13px;
  line-height: 1.7;
  color: #606266;

  &--blue {
    background: #ecf5ff;
    border-color: #b3d8ff;
    color: #409eff;
  }

  &--red {
    background: #fef0f0;
    border-color: #fbc4c4;
    color: #f56c6c;
  }
}

.import-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

/* ---------- 模板列说明 ---------- */
.import-col-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

@media (max-width: 1100px) {
  .import-col-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.import-col-card {
  padding: 9px 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.import-col-card__name {
  margin-bottom: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.import-col-card__desc {
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
}

/* ---------- 上传区 ---------- */
.import-dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 480px;
  max-width: 100%;
  height: 160px;
  margin-bottom: 14px;
  border: 1.5px dashed #c6d3e2;
  border-radius: 6px;
  background: #fafbfd;
  color: #909399;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;

  &:hover {
    border-color: #409eff;
    background: #f5faff;
  }
}

.import-dropzone__icon {
  color: #a8bdd6;
  margin-bottom: 8px;
}

.import-dropzone__main {
  font-size: 14px;
  color: #606266;
}

.import-dropzone__link {
  color: #409eff;
}

.import-dropzone__sub {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.import-file-input {
  display: none;
}

.import-file-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  margin-bottom: 14px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #f5f7fa;
  font-size: 13px;
  color: #606266;
}

.import-file-card__icon {
  color: #67c23a;
}

.import-file-card__name {
  font-weight: 500;
  color: #303133;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.import-file-card__grow {
  flex: 1;
}

/* ---------- 校验进度 ---------- */
.import-progress {
  padding: 12px 0 18px;
}

.import-progress__text {
  margin-bottom: 8px;
  font-size: 13px;
  color: #409eff;
}

/* ---------- 校验结果数字块 ---------- */
.import-result-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.import-result-card {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafbfc;
  text-align: center;
}

.import-result-card__num {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
  color: #303133;
}

.import-result-card__label {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.import-result-card.is-ok {
  background: #f0f9eb;
  border-color: #c2e7b0;

  .import-result-card__num {
    color: #67c23a;
  }
}

.import-result-card.is-fail {
  background: #fef0f0;
  border-color: #fbc4c4;

  .import-result-card__num {
    color: #f56c6c;
  }
}

.import-result-card.is-warn {
  background: #fdf6ec;
  border-color: #f5dab1;

  .import-result-card__num {
    color: #e6a23c;
  }
}

.import-result-card.is-info,
.import-result-card.is-total {
  background: #ecf5ff;
  border-color: #b3d8ff;

  .import-result-card__num {
    color: #409eff;
  }
}

/* ---------- 重复策略 ---------- */
.import-strategy {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid #f0f2f5;
}

.import-strategy__label {
  font-size: 13px;
  color: #606266;
}

.import-strategy__tip {
  flex: 1 0 100%;
  line-height: 1.6;
}

/* ---------- 错误报告表 ---------- */
.import-error-block {
  margin-top: 8px;
}

.import-error-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.import-error-row-num {
  font-weight: 600;
  color: #f56c6c;
}

.import-error-reason {
  color: #f56c6c;
}

.import-error-table {
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

/* ---------- 步骤底部 ---------- */
.import-step-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #f0f2f5;
}

.import-step-foot__right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.import-empty-tip {
  padding: 12px 0 4px;
  text-align: center;
}

.import-result-desc {
  margin-bottom: 16px;
}

.import-confirm-text {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
}
</style>
