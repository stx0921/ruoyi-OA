<template>
  <div class="app-container recur-page">
    <div class="oa-page-title">
      周期任务
      <small>把每周固定要做的事配成规则，系统按点生成与提醒</small>
    </div>

    <div class="oa-card">
      <!-- 顶部筛选 -->
      <el-form ref="queryRef" :model="queryParams" :inline="true" class="oa-filter-bar">
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            placeholder="规则名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="oa-toolbar">
        <div class="oa-toolbar__left">
          <el-button v-hasPermi="['oa:recur:add']" type="primary" plain icon="Plus" @click="handleAdd">新建规则</el-button>
        </div>
        <div class="oa-toolbar__right">
          <span class="oa-muted">共 {{ total }} 条规则</span>
        </div>
      </div>

      <!-- 加载骨架 -->
      <div v-if="loading && !recurList.length" class="skeleton-table">
        <div v-for="index in 5" :key="index" class="skeleton-row">
          <el-skeleton :rows="1" animated />
        </div>
      </div>

      <!-- 错误态 -->
      <div v-else-if="error" class="state-block">
        <el-icon :size="34"><WarningFilled /></el-icon>
        <p>规则列表加载失败</p>
        <el-button size="small" type="primary" @click="getList">重新加载</el-button>
      </div>

      <!-- 空态 -->
      <div v-else-if="!recurList.length" class="state-block">
        <el-icon :size="34"><Timer /></el-icon>
        <p>{{ hasFilter ? '没有匹配的周期规则' : '还没有周期规则' }}</p>
        <span class="oa-muted">例如：每周五 18:00 提交周报</span>
        <div class="empty-actions">
          <el-button v-if="hasFilter" size="small" @click="resetQuery">重置筛选</el-button>
          <el-button v-hasPermi="['oa:recur:add']" size="small" type="primary" @click="handleAdd">新建规则</el-button>
        </div>
      </div>

      <!-- 规则表格 -->
      <template v-else>
        <el-table v-loading="loading" :data="recurList" row-key="recurId">
          <el-table-column label="规则名称" min-width="200">
            <template #default="scope">
              <el-tooltip :content="scope.row.title" placement="top" :show-after="300">
                <span class="rule-title">{{ scope.row.title }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="每周几" width="140" align="center">
            <template #default="scope">
              <span class="weekday-row">
                <span v-for="day in weekDaysOf(scope.row)" :key="day" class="weekday-dot">{{ day }}</span>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="截止时刻" width="100" align="center">
            <template #default="scope">
              <span class="mono">{{ dueTimeOf(scope.row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="主责人" width="110" prop="assigneeName" show-overflow-tooltip />
          <el-table-column label="项目" width="130">
            <template #default="scope">
              <span v-if="scope.row.projectName" class="proj-tag">{{ scope.row.projectName }}</span>
              <span v-else class="oa-muted">未挂项目</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="scope">
              <OaTaskStatusTag type="recur" :status="scope.row.status" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="下次生成日" width="120" align="center">
            <template #default="scope">
              <span>{{ nextDateOf(scope.row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="230" align="center">
            <template #default="scope">
              <el-button
                v-if="String(scope.row.status) !== '2'"
                v-hasPermi="['oa:recur:edit']"
                link
                type="primary"
                @click="handleEdit(scope.row)"
              >编辑</el-button>
              <el-button
                v-if="String(scope.row.status) === '0'"
                v-hasPermi="['oa:recur:edit']"
                link
                type="warning"
                @click="handleStatus(scope.row, '1')"
              >暂停</el-button>
              <el-button
                v-if="String(scope.row.status) === '1'"
                v-hasPermi="['oa:recur:edit']"
                link
                type="success"
                @click="handleStatus(scope.row, '0')"
              >恢复</el-button>
              <el-button
                v-if="String(scope.row.status) !== '2'"
                v-hasPermi="['oa:recur:edit']"
                link
                type="danger"
                @click="handleStatus(scope.row, '2')"
              >结束</el-button>
              <el-button
                v-hasPermi="['oa:recur:remove']"
                link
                type="danger"
                :loading="deletingId === scope.row.recurId"
                @click="handleDelete(scope.row)"
              >删除</el-button>
              <span v-if="String(scope.row.status) === '2'" class="oa-muted ended-text">已结束</span>
            </template>
          </el-table-column>
          <template #empty>
            <div class="state-block">
              <p>{{ hasFilter ? '没有匹配的周期规则' : '还没有周期规则' }}</p>
              <el-button v-if="hasFilter" size="small" @click="resetQuery">重置筛选</el-button>
            </div>
          </template>
        </el-table>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </template>
    </div>

    <!-- 新建 / 编辑规则抽屉 -->
    <recur-form-drawer ref="formDrawerRef" @success="getList" />
  </div>
</template>

<script setup name="RecurList">
import { ElMessageBox } from 'element-plus'
import OaTaskStatusTag from '@/components/oa/OaTaskStatusTag.vue'
import RecurFormDrawer from './RecurFormDrawer.vue'
import { listRecur, deleteRecur, updateRecurStatus } from '@/api/oa/recur'
import { RECUR_STATUS_MAP, formatDate, formatWeekDays, resolveOptions } from '@/utils/oaTask'
import { useDict } from '@/utils/dict'

const { proxy } = getCurrentInstance()

/** 兜底选项（字典 oa_recur_status 优先） */
const RECUR_STATUS_OPTIONS = ['0', '1', '2'].map(value => ({
  value,
  label: RECUR_STATUS_MAP[value].label
}))

const { oa_recur_status } = useDict('oa_recur_status')
const statusOptions = computed(() => resolveOptions(oa_recur_status.value, RECUR_STATUS_OPTIONS))

const loading = ref(false)
const error = ref(false)
const recurList = ref([])
const total = ref(0)
const queryRef = ref()
const formDrawerRef = ref()
const deletingId = ref(undefined)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
  keyword: undefined
})

const hasFilter = computed(() => !!(queryParams.value.status || queryParams.value.keyword))

/** 每周几徽标：以 / 分隔的单个汉字 */
function weekDaysOf(row) {
  const text = formatWeekDays(row.weekDays)
  return text === '—' ? [] : text.split('/')
}

function dueTimeOf(row) {
  const time = String(row.dueTime || '')
  return time.length >= 5 ? time.slice(0, 5) : (time || '—')
}

function nextDateOf(row) {
  if (String(row.status) !== '0') return '—'
  return row.nextDate ? formatDate(row.nextDate) : '—'
}

function getList() {
  loading.value = true
  error.value = false
  listRecur(queryParams.value)
    .then(response => {
      recurList.value = response.rows || []
      total.value = response.total || 0
    })
    .catch(() => {
      error.value = true
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  if (queryRef.value) queryRef.value.resetFields()
  queryParams.value.status = undefined
  queryParams.value.keyword = undefined
  queryParams.value.pageNum = 1
  getList()
}

function handleAdd() {
  formDrawerRef.value.open({ mode: 'add' })
}

function handleEdit(row) {
  formDrawerRef.value.open({ mode: 'edit', data: row })
}

/** 暂停 / 恢复 / 结束（均二次确认） */
function handleStatus(row, status) {
  const textMap = {
    '1': '规则已暂停，将不再生成新任务。确认暂停？',
    '0': '规则已恢复，将于下一个命中日生成。确认恢复？',
    '2': '结束后不再生成新任务，历史任务保留，确认结束？'
  }
  const successMap = {
    '1': '规则已暂停，将不再生成新任务',
    '0': '规则已恢复，将于下一个命中日生成',
    '2': '规则已结束，历史任务保留'
  }
  ElMessageBox.confirm(textMap[status], '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => updateRecurStatus({ recurId: row.recurId, status }))
    .then(() => {
      proxy.$modal.msgSuccess(successMap[status])
      getList()
    })
    .catch(() => {})
}

/** 删除周期规则（契约追加需求 §9，硬删除不可恢复；已生成任务保留） */
function handleDelete(row) {
  if (!row || !row.recurId || deletingId.value) return
  ElMessageBox.confirm(
    '删除规则「' + (row.title || '') + '」？删除后不再生成新任务，已生成任务保留。删除后不可恢复。',
    '删除规则',
    {
      type: 'error',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    }
  )
    .then(async () => {
      deletingId.value = row.recurId
      try {
        await deleteRecur(row.recurId)
        proxy.$modal.msgSuccess('删除成功')
        getList()
      } catch (e) {
        // 接口异常已由 request 拦截器统一提示
      } finally {
        deletingId.value = undefined
      }
    })
    .catch(() => {})
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.recur-page {
  .oa-filter-bar {
    :deep(.el-form-item) {
      margin-bottom: 0;
    }
  }

  .rule-title {
    font-size: 13px;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
  }

  .weekday-row {
    display: inline-flex;
    gap: 4px;
  }

  .weekday-dot {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    background: #f4f4f5;
    color: #606266;
    font-size: 11px;
  }

  .mono {
    font-family: Consolas, Monaco, monospace;
    font-size: 13px;
  }

  .ended-text {
    margin-right: 8px;
  }

  .proj-tag {
    display: inline-block;
    padding: 1px 6px;
    border: 1px solid #dbe7fc;
    border-radius: 3px;
    background: #edf3fe;
    color: #1d61d5;
    font-size: 11px;
    white-space: nowrap;
  }

  .skeleton-table {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 8px 0;
  }

  .skeleton-row {
    padding: 8px 12px;
    border-bottom: 1px solid #f5f7fa;
  }

  .state-block {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    padding: 48px 0;
    color: #909399;
    font-size: 13px;

    p {
      margin: 0;
    }
  }

  .empty-actions {
    display: flex;
    gap: 8px;
  }
}
</style>
