<template>
  <div class="app-container storage-page">
    <div class="oa-page-title">
      存储管理
      <small>查看当前文件存储模式，并把本地 /profile 存量附件、头像迁移到阿里云 OSS；迁移会改写数据库路径，操作前请先试运行。</small>
    </div>

    <!-- ==================== 一、存储状态 ==================== -->
    <el-card shadow="never" class="storage-card">
      <template #header>
        <div class="card-head">
          <span class="oa-card-head">存储状态</span>
          <div class="card-head__right">
            <span class="oa-muted card-head__tip">状态数据来自 /system/storage/status</span>
            <el-button
              type="primary"
              plain
              icon="Refresh"
              :loading="statusLoading"
              @click="loadStatus"
            >刷新状态</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="statusError"
        type="warning"
        :closable="false"
        show-icon
        class="status-alert"
        title="存储状态加载失败，请确认后端 /system/storage/status 可用后点击「刷新状态」重试"
      />

      <div class="stat-grid">
        <oa-stat-card
          label="存储模式"
          :value="statusLoading ? '--' : modeText"
          :tone="isOss ? 'green' : 'blue'"
          icon="FolderOpened"
        />
        <oa-stat-card
          label="待迁移 OA 附件"
          :value="statusLoading ? '--' : numberText(status.pendingOaFiles)"
          suffix="个"
          tone="orange"
          icon="Document"
        />
        <oa-stat-card
          label="待迁移头像"
          :value="statusLoading ? '--' : numberText(status.pendingAvatars)"
          suffix="个"
          tone="orange"
          icon="Avatar"
        />
        <oa-stat-card
          label="OSS 已存（附件 / 头像）"
          :value="statusLoading ? '--' : ossCountText"
          tone="green"
          icon="Cloudy"
        />
      </div>

      <el-descriptions v-loading="statusLoading" :column="2" border class="status-desc">
        <el-descriptions-item label="OSS 状态">
          <el-tag :type="ossTagType" size="small">{{ ossStateText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Bucket">{{ blankText(status.bucket) }}</el-descriptions-item>
        <el-descriptions-item label="Endpoint">{{ blankText(status.endpoint) }}</el-descriptions-item>
        <el-descriptions-item label="Key 前缀">{{ blankText(status.keyPrefix) }}</el-descriptions-item>
        <el-descriptions-item label="私有桶">{{ yesNoText(status.privateBucket) }}</el-descriptions-item>
        <el-descriptions-item label="读取兜底">{{ yesNoText(status.readFallback) }}</el-descriptions-item>
        <el-descriptions-item label="签名有效期">{{ status.signExpireSeconds ? status.signExpireSeconds + ' 秒' : '—' }}</el-descriptions-item>
        <el-descriptions-item label="ECS 实例角色">{{ yesNoText(status.instanceRole) }}</el-descriptions-item>
        <el-descriptions-item label="公共域名" :span="2">{{ blankText(status.publicDomain) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- ==================== 二、迁移到 OSS ==================== -->
    <el-card shadow="never" class="storage-card">
      <template #header>
        <div class="card-head">
          <span class="oa-card-head">迁移到 OSS（local → oss）</span>
          <span class="oa-muted card-head__tip">上传本地 /profile 存量文件，成功后再改写数据库路径</span>
        </div>
      </template>

      <el-alert
        v-if="statusLoaded && !status.ossEnabled"
        type="warning"
        :closable="false"
        show-icon
        class="status-alert"
        title="OSS 客户端未就绪（未配置或已禁用），迁移操作暂不可用"
      />

      <el-form :model="migrateForm" :inline="true" label-width="86px" @submit.prevent>
        <el-form-item label="迁移类型">
          <el-select v-model="migrateForm.type" style="width: 160px">
            <el-option label="全部" value="all" />
            <el-option label="仅 OA 附件" value="oa_file" />
            <el-option label="仅头像" value="avatar" />
          </el-select>
        </el-form-item>
        <el-form-item label="单次条数">
          <el-input-number
            v-model="migrateForm.limit"
            :min="1"
            :max="2000"
            :step="50"
            controls-position="right"
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            plain
            icon="View"
            :loading="running && runningKey === 'migrate-dry'"
            :disabled="isBusy"
            @click="handleMigrate(true)"
            v-hasPermi="['system:storage:migrate']"
          >试运行（dry-run）</el-button>
          <el-button
            type="primary"
            icon="Upload"
            :loading="running && runningKey === 'migrate-run'"
            :disabled="isBusy || ossUnavailable"
            @click="handleMigrate(false)"
            v-hasPermi="['system:storage:migrate']"
          >执行迁移</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="op-alert"
        title="dry-run 只统计待迁移数量，不上传文件、不写数据库；执行迁移会真实上传并改写数据库路径，建议先备份数据库。"
      />
    </el-card>

    <!-- ==================== 三、回滚（OSS → 本地） ==================== -->
    <el-card shadow="never" class="storage-card rollback-card">
      <template #header>
        <div class="card-head">
          <span class="oa-card-head card-head__danger">回滚（OSS → 本地）</span>
          <span class="oa-muted card-head__tip">把 OSS 对象下载回本地 /profile 并还原数据库路径，仅管理员可用</span>
        </div>
      </template>

      <el-form :model="rollbackForm" :inline="true" label-width="86px" @submit.prevent>
        <el-form-item label="回滚类型">
          <el-select v-model="rollbackForm.type" style="width: 160px">
            <el-option label="全部" value="all" />
            <el-option label="仅 OA 附件" value="oa_file" />
            <el-option label="仅头像" value="avatar" />
          </el-select>
        </el-form-item>
        <el-form-item label="单次条数">
          <el-input-number
            v-model="rollbackForm.limit"
            :min="1"
            :max="2000"
            :step="50"
            controls-position="right"
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="danger"
            plain
            icon="View"
            :loading="running && runningKey === 'rollback-dry'"
            :disabled="isBusy"
            @click="handleRollback(true)"
            v-hasPermi="['system:storage:rollback']"
          >试运行（dry-run）</el-button>
          <el-button
            type="danger"
            icon="RefreshLeft"
            :loading="running && runningKey === 'rollback-run'"
            :disabled="isBusy || ossUnavailable"
            @click="handleRollback(false)"
            v-hasPermi="['system:storage:rollback']"
          >执行回滚</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        type="error"
        :closable="false"
        show-icon
        class="op-alert"
        title="回滚属于高风险操作：会把 OSS 上的对象重新下载到本地并还原数据库路径，仅在迁移异常且确认数据可回退时使用。"
      />
    </el-card>

    <!-- ==================== 四、执行结果 ==================== -->
    <el-card v-if="result" shadow="never" class="storage-card result-card">
      <template #header>
        <div class="card-head">
          <span class="oa-card-head">执行结果</span>
          <div class="card-head__right">
            <el-tag v-if="result.dryRun" type="warning" size="small">dry-run · 未改动数据</el-tag>
            <el-tag v-else type="success" size="small">已执行</el-tag>
            <el-tag :type="result.mode === 'rollback' ? 'danger' : 'info'" size="small" effect="plain">{{ resultModeText }}</el-tag>
            <el-tag type="info" size="small" effect="plain">{{ resultTypeText }}</el-tag>
          </div>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="迁移前待处理">{{ numberText(result.pendingBefore) }}</el-descriptions-item>
        <el-descriptions-item label="迁移后待处理">{{ numberText(result.pendingAfter) }}</el-descriptions-item>
        <el-descriptions-item label="扫描数">{{ numberText(result.scanned) }}</el-descriptions-item>
        <el-descriptions-item label="成功">{{ numberText(result.migrated) }}</el-descriptions-item>
        <el-descriptions-item label="跳过">{{ numberText(result.skipped) }}</el-descriptions-item>
        <el-descriptions-item label="对象不存在">{{ numberText(result.missing) }}</el-descriptions-item>
        <el-descriptions-item label="失败">{{ numberText(result.failed) }}</el-descriptions-item>
        <el-descriptions-item label="传输大小">{{ formatBytes(result.bytes) }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ formatDuration(result.durationMs) }}</el-descriptions-item>
      </el-descriptions>

      <div class="failure-head">
        <span class="oa-card-head">失败清单</span>
        <span class="oa-muted">共 {{ failures.length }} 条明细（失败 {{ numberText(result.failed) }}，缺失 {{ numberText(result.missing) }}），最多展示 20 条</span>
      </div>
      <el-table :data="failures" empty-text="无失败记录" class="failure-table">
        <el-table-column label="ID" prop="id" width="120" />
        <el-table-column label="路径" prop="path" min-width="280" :show-overflow-tooltip="true" />
        <el-table-column label="错误" prop="error" min-width="280" :show-overflow-tooltip="true" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="Storage">
/**
 * 系统管理 · 存储管理
 *
 * 页面级权限：system:storage:list（由菜单 system/storage/index 下发，见
 * server/sql/oa/07_storage_oss.sql）；按钮级权限见模板 v-hasPermi：
 *   system:storage:migrate  迁移 / 试运行
 *   system:storage:rollback 回滚 / 回滚试运行
 */
import OaStatCard from '@/components/oa/OaStatCard.vue'
import { getStorageStatus, migrateStorage } from '@/api/system/storage'

const { proxy } = getCurrentInstance()

const statusLoading = ref(false)
const statusLoaded = ref(false)
const statusError = ref(false)
const status = ref({})

const running = ref(false)
const runningKey = ref('')
const result = ref(null)

const migrateForm = reactive({ type: 'all', limit: 200 })
const rollbackForm = reactive({ type: 'all', limit: 200 })

const isOss = computed(() => status.value.type === 'oss')
const isBusy = computed(() => running.value)
const ossUnavailable = computed(() => statusLoaded.value && !status.value.ossEnabled)
const modeText = computed(() => {
  if (status.value.type === 'oss') return '阿里云 OSS'
  if (status.value.type === 'local') return '本地存储'
  return statusLoaded.value ? '未配置' : '--'
})
const ossStateText = computed(() => {
  if (!statusLoaded.value) return '—'
  if (status.value.ossEnabled) return '已就绪'
  return status.value.type === 'oss' ? '未配置' : '已禁用'
})
const ossTagType = computed(() => {
  if (!statusLoaded.value) return 'info'
  if (status.value.ossEnabled) return 'success'
  return status.value.type === 'oss' ? 'warning' : 'info'
})
const ossCountText = computed(() => `${numberText(status.value.ossOaFiles)} / ${numberText(status.value.ossAvatars)}`)
const resultModeText = computed(() => (result.value && result.value.mode === 'rollback' ? '回滚（OSS → 本地）' : '迁移（本地 → OSS）'))
const resultTypeText = computed(() => {
  const typeMap = { all: '全部', oa_file: '仅 OA 附件', avatar: '仅头像' }
  return typeMap[result.value && result.value.type] || '全部'
})
const failures = computed(() => {
  const list = result.value && result.value.failures
  return Array.isArray(list) ? list : []
})

/** 查询存储状态 */
function loadStatus() {
  statusLoading.value = true
  statusError.value = false
  getStorageStatus().then(response => {
    status.value = (response && response.data) || {}
    statusLoaded.value = true
  }).catch(() => {
    // 错误提示由 request 拦截器统一处理，这里只标记状态加载失败
    statusError.value = true
  }).finally(() => {
    statusLoading.value = false
  })
}

/**
 * 执行迁移 / 回滚
 * @param {'migrate'|'rollback'} mode
 * @param {boolean} dryRun 是否只统计
 */
function runMigrate(mode, dryRun) {
  const form = mode === 'rollback' ? rollbackForm : migrateForm
  running.value = true
  runningKey.value = `${mode === 'rollback' ? 'rollback' : 'migrate'}-${dryRun ? 'dry' : 'run'}`
  migrateStorage({
    mode,
    dryRun,
    type: form.type,
    limit: form.limit
  }).then(response => {
    result.value = (response && response.data) || null
    const action = dryRun ? '试运行' : mode === 'rollback' ? '回滚' : '迁移'
    if (result.value && Number(result.value.failed) > 0) {
      proxy.$modal.msgWarning(`${action}完成，其中 ${result.value.failed} 条失败，详见失败清单`)
    } else {
      proxy.$modal.msgSuccess(`${action}完成`)
    }
    // 迁移/回滚会改变待迁移数量，刷新状态卡片
    loadStatus()
  }).catch(() => {
    // 错误提示由 request 拦截器统一处理，这里只恢复按钮状态
  }).finally(() => {
    running.value = false
    runningKey.value = ''
  })
}

/** 迁移按钮：dry-run 直接执行，正式迁移二次确认 */
function handleMigrate(dryRun) {
  if (dryRun) {
    runMigrate('migrate', true)
    return
  }
  proxy.$modal.confirm('将把本地 /profile 存量文件上传到 OSS 并改写数据库路径，操作前请确认已备份数据库，是否继续？').then(() => {
    runMigrate('migrate', false)
  }).catch(() => {})
}

/** 回滚按钮：dry-run 直接执行，正式回滚强制确认 */
function handleRollback(dryRun) {
  if (dryRun) {
    runMigrate('rollback', true)
    return
  }
  proxy.$modal.confirm('回滚会把 OSS 上已迁移的对象下载回本地 /profile 并还原数据库路径，属于高风险操作，确认执行回滚？').then(() => {
    runMigrate('rollback', false)
  }).catch(() => {})
}

/* ---------------------------- 展示格式化 ---------------------------- */
function numberText(value) {
  const num = Number(value)
  return Number.isFinite(num) ? String(num) : '0'
}

function blankText(value) {
  return value === undefined || value === null || value === '' ? '—' : String(value)
}

function yesNoText(value) {
  if (value === undefined || value === null) return '—'
  return value ? '是' : '否'
}

function formatBytes(value) {
  const num = Number(value)
  if (!Number.isFinite(num) || num <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = num
  let index = 0
  while (size >= 1024 && index < units.length - 1) {
    size = size / 1024
    index++
  }
  return `${index === 0 ? size : size.toFixed(2)} ${units[index]}`
}

function formatDuration(value) {
  const num = Number(value)
  if (!Number.isFinite(num) || num < 0) return '0 ms'
  if (num < 1000) return `${Math.round(num)} ms`
  if (num < 60000) return `${(num / 1000).toFixed(2)} 秒`
  const minutes = Math.floor(num / 60000)
  const seconds = (num % 60000) / 1000
  return `${minutes} 分 ${seconds.toFixed(1)} 秒`
}

loadStatus()
</script>

<style scoped lang="scss">
.storage-page {
  .oa-page-title small {
    margin-left: 10px;
    font-size: 12px;
    font-weight: 400;
    line-height: 1.7;
    color: var(--ink-soft);
  }
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.card-head__right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-head__tip {
  font-size: 12px;
}

.card-head__danger {
  color: var(--red-text);
}

.status-alert {
  margin-bottom: 16px;
}

.op-alert {
  margin-top: 4px;
}

/* ---------- 状态统计卡 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.status-desc {
  :deep(.el-descriptions__content) {
    word-break: break-all;
  }
}

/* ---------- 执行结果 ---------- */
.failure-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin: 18px 0 10px;
  padding-top: 16px;
  border-top: 1px solid var(--line-strong);
}

.failure-table {
  :deep(.el-table__row) {
    height: 46px;
  }
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
