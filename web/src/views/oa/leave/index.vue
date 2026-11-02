<template>
  <div class="app-container">
    <div class="oa-page-title">
      请假申请 <small>共 {{ total }} 条记录</small>
    </div>

    <div class="oa-card">
      <!-- 查询区 -->
      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" class="oa-filter-bar">
        <el-form-item label="申请人" prop="applyUserName">
          <el-input
            v-model="queryParams.applyUserName"
            placeholder="请输入申请人"
            clearable
            style="width: 160px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="queryParams.leaveType" placeholder="全部" clearable style="width: 150px">
            <el-option
              v-for="dict in oa_leave_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 130px">
            <el-option
              v-for="item in LEAVE_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="提交时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
            unlink-panels
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 工具条 -->
      <div class="oa-toolbar">
        <div class="oa-toolbar__left">
          <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
            v-hasPermi="['oa:leave:add']"
          >新增请假</el-button>
          <el-button
            type="warning"
            plain
            icon="Download"
            @click="handleExport"
            v-hasPermi="['oa:leave:export']"
          >导出 Excel</el-button>
          <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleBatchDelete"
            v-hasPermi="['oa:leave:remove']"
          >批量删除</el-button>
        </div>
        <div class="oa-toolbar__right">
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>
      </div>

      <!-- 列表 -->
      <el-table
        v-loading="loading"
        :data="leaveList"
        empty-text="暂无请假数据"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="请假单号" align="center" width="130">
          <template #default="scope">
            <span class="oa-link" @click="handleTrace(scope.row)">{{ oaLeaveNo(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请人" align="center" prop="applyUserName" width="110" show-overflow-tooltip />
        <el-table-column label="部门" align="center" prop="applyDeptName" width="130" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.applyDeptName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" align="center" width="100">
          <template #default="scope">
            <span>{{ leaveTypeText(scope.row.leaveType, oa_leave_type) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" align="center" width="150">
          <template #default="scope">
            <span>{{ oaDateTime(scope.row.startTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结束时间" align="center" width="150">
          <template #default="scope">
            <span>{{ oaDateTime(scope.row.endTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时长" align="center" width="90">
          <template #default="scope">
            <span>{{ oaDurationText(scope.row.duration) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="100">
          <template #default="scope">
            <oa-status-tag :status="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="提交时间" align="center" width="150">
          <template #default="scope">
            <span>{{ oaDateTime(scope.row.applyTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="230" fixed="right" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
            <!-- 草稿：查看 / 编辑 / 提交 / 删除 -->
            <template v-if="statusOf(scope.row) === '0'">
              <el-button
                link
                type="primary"
                icon="Edit"
                @click="handleEdit(scope.row)"
                v-hasPermi="['oa:leave:edit']"
              >编辑</el-button>
              <el-button
                link
                type="primary"
                icon="Promotion"
                @click="handleSubmit(scope.row)"
                v-hasPermi="['oa:leave:submit']"
              >提交</el-button>
              <el-button
                link
                type="danger"
                icon="Delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['oa:leave:remove']"
              >删除</el-button>
            </template>
            <!-- 审批中：查看 / 流程 / 撤回（申请人可再编辑后重新提交） -->
            <template v-else-if="statusOf(scope.row) === '1'">
              <el-button link type="primary" icon="Share" @click="handleTrace(scope.row)">流程</el-button>
              <el-button
                v-if="isOwner(scope.row)"
                link
                type="primary"
                icon="Edit"
                @click="handleEdit(scope.row)"
                v-hasPermi="['oa:leave:edit']"
              >编辑</el-button>
              <el-button
                link
                type="warning"
                icon="RefreshLeft"
                @click="handleRevoke(scope.row)"
                v-hasPermi="['oa:leave:submit']"
              >撤回</el-button>
            </template>
            <!-- 已通过 / 已撤销 / 已驳回：查看 / 流程 -->
            <template v-else>
              <el-button link type="primary" icon="Share" @click="handleTrace(scope.row)">流程</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <!-- 新增 / 编辑请假弹窗 -->
    <leave-form-dialog ref="leaveFormRef" @success="getList" />

    <!-- 请假详情抽屉 -->
    <el-drawer v-model="detailVisible" size="560px" append-to-body>
      <template #header>
        <div class="oa-detail-head">
          <span class="oa-strong">请假单 {{ oaLeaveNo(detail) }}</span>
          <oa-status-tag :status="detail.status" />
        </div>
      </template>
      <div v-loading="detailLoading">
        <table class="oa-desc-table">
          <tbody>
            <tr>
              <th>申请人</th>
              <td>{{ detail.applyUserName || '—' }}</td>
              <th>所属部门</th>
              <td>{{ detail.applyDeptName || '—' }}</td>
            </tr>
            <tr>
              <th>请假类型</th>
              <td>{{ leaveTypeText(detail.leaveType, oa_leave_type) }}</td>
              <th>请假时长</th>
              <td>{{ oaDurationText(detail.duration) }}</td>
            </tr>
            <tr>
              <th>开始时间</th>
              <td>{{ oaDateTime(detail.startTime) }}</td>
              <th>结束时间</th>
              <td>{{ oaDateTime(detail.endTime) }}</td>
            </tr>
            <tr>
              <th>提交时间</th>
              <td>{{ oaDateTime(detail.applyTime) }}</td>
              <th>创建时间</th>
              <td>{{ oaDateTime(detail.createTime) }}</td>
            </tr>
            <tr>
              <th>请假事由</th>
              <td colspan="3" class="oa-pre">{{ detail.reason || '—' }}</td>
            </tr>
            <tr>
              <th>附件</th>
              <td colspan="3">
                <el-link
                  v-if="detail.attachmentUrl"
                  type="primary"
                  underline="never"
                  :href="attachmentHref(detail.attachmentUrl)"
                  target="_blank"
                >{{ attachmentName(detail.attachmentUrl) }}</el-link>
                <span v-else class="oa-muted">无</span>
              </td>
            </tr>
            <tr>
              <th>备注</th>
              <td colspan="3" class="oa-pre">{{ detail.remark || '—' }}</td>
            </tr>
          </tbody>
        </table>

        <div class="oa-detail-section" v-if="traceRecords.length">
          <h4>审批记录</h4>
          <el-timeline>
            <el-timeline-item
              v-for="(record, index) in traceRecords"
              :key="index"
              :timestamp="recordTime(record)"
              placement="top"
            >
              <div class="oa-strong">{{ record.taskName || '审批节点' }}</div>
              <div class="oa-muted">
                {{ oaUserName({}, record.assignee, detail) }} · {{ record.finished ? '已处理' : '处理中' }}
              </div>
              <div v-if="record.comment" class="oa-comment">审批意见：{{ record.comment }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>
        <div v-else-if="!detailLoading" class="oa-empty">暂无审批记录</div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关 闭</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="Leave">
import { listLeave, delLeave, submitLeave, revokeLeave, getLeave, getLeaveTrace } from '@/api/oa/leave'
import {
  LEAVE_STATUS_OPTIONS,
  oaLeaveNo,
  oaDateTime,
  oaDurationText,
  leaveTypeText,
  oaUserName
} from '@/utils/oa'
import OaStatusTag from '@/components/oa/OaStatusTag.vue'
import LeaveFormDialog from './LeaveFormDialog.vue'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()
const { oa_leave_type } = useDict('oa_leave_type')

const loading = ref(true)
const showSearch = ref(true)
const leaveList = ref([])
const total = ref(0)
const selectedRows = ref([])
const multiple = computed(() => selectedRows.value.length === 0)
const dateRange = ref([])

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  applyUserName: undefined,
  leaveType: undefined,
  status: undefined,
  params: {}
})

const leaveFormRef = ref()

/** 详情抽屉 */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})
const traceRecords = ref([])
const fileBaseUrl = import.meta.env.VITE_APP_BASE_API

/** 状态码统一转字符串比较（后端返回 string，避免误判） */
function statusOf(row) {
  if (!row || row.status === undefined || row.status === null) return ''
  return String(row.status)
}

/** 当前登录人是否为该单申请人 */
function isOwner(row) {
  return String(row.applyUserId) === String(userStore.id)
}

/** 把提交时间区间写进 queryParams.params（后端约定 params[beginTime|endTime]） */
function syncDateRange() {
  if (dateRange.value && dateRange.value.length === 2) {
    queryParams.value.params = { beginTime: dateRange.value[0], endTime: dateRange.value[1] }
  } else {
    queryParams.value.params = {}
  }
}

/** 查询请假列表 */
function getList() {
  syncDateRange()
  loading.value = true
  listLeave(queryParams.value)
    .then(response => {
      leaveList.value = response.rows || []
      total.value = response.total || 0
    })
    .catch(() => {})
    .finally(() => {
      loading.value = false
    })
}

/** 搜索 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置查询 */
function resetQuery() {
  proxy.resetForm('queryRef')
  dateRange.value = []
  handleQuery()
}

/** 表格勾选 */
function handleSelectionChange(selection) {
  selectedRows.value = selection
}

/** 新增 */
function handleAdd() {
  leaveFormRef.value.open({ mode: 'add' })
}

/** 编辑（草稿 / 驳回退回发起人） */
function handleEdit(row) {
  leaveFormRef.value.open({ mode: 'edit', data: row })
}

/** 查看详情 */
function handleView(row) {
  detail.value = { ...row }
  traceRecords.value = []
  detailVisible.value = true
  detailLoading.value = true

  // 没有 oa:leave:query 时直接用列表行数据展示，避免无意义的 403 提示
  if (!proxy.$auth.hasPermi('oa:leave:query')) {
    detailLoading.value = false
    return
  }

  getLeave(row.leaveId)
    .then(response => {
      if (response && response.data) {
        detail.value = response.data
      }
    })
    .catch(() => {})
    .finally(() => {
      detailLoading.value = false
    })

  getLeaveTrace(row.leaveId)
    .then(response => {
      const data = (response && response.data) || {}
      if (data.leave) {
        detail.value = data.leave
      }
      traceRecords.value = data.records || []
    })
    .catch(() => {
      traceRecords.value = []
    })
}

/** 流程跟踪 */
function handleTrace(row) {
  router.push({ path: '/oa/trace', query: { leaveId: row.leaveId } })
}

/** 提交审批 */
function handleSubmit(row) {
  proxy.$modal
    .confirm('是否确认提交请假单「' + oaLeaveNo(row) + '」走审批流程？')
    .then(() => submitLeave(row.leaveId))
    .then(() => {
      proxy.$modal.msgSuccess('提交成功')
      getList()
    })
    .catch(() => {})
}

/** 撤回（仅审批中） */
function handleRevoke(row) {
  proxy.$modal
    .confirm('是否确认撤回请假单「' + oaLeaveNo(row) + '」？撤回后流程终止，需重新提交。')
    .then(() => revokeLeave(row.leaveId))
    .then(() => {
      proxy.$modal.msgSuccess('撤回成功')
      getList()
    })
    .catch(() => {})
}

/** 删除（仅草稿） */
function handleDelete(row) {
  proxy.$modal
    .confirm('是否确认删除请假单「' + oaLeaveNo(row) + '」？')
    .then(() => delLeave(row.leaveId))
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
    })
    .catch(() => {})
}

/** 批量删除（只允许草稿） */
function handleBatchDelete() {
  const rows = selectedRows.value
  if (!rows.length) {
    proxy.$modal.msgWarning('请先选择要删除的请假单')
    return
  }
  if (rows.some(row => statusOf(row) !== '0')) {
    proxy.$modal.msgWarning('只有草稿状态的请假单才能删除，请重新选择')
    return
  }
  const ids = rows.map(row => row.leaveId)
  proxy.$modal
    .confirm('是否确认删除选中的 ' + ids.length + ' 条请假记录？')
    .then(() => delLeave(ids.join(',')))
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
    })
    .catch(() => {})
}

/** 导出 Excel */
function handleExport() {
  syncDateRange()
  proxy.download('oa/leave/export', { ...queryParams.value }, `leave_${Date.now()}.xlsx`)
}

/** 详情里的附件文件名 */
function attachmentName(url) {
  if (!url) return ''
  const name = String(url).split('/').pop()
  return name || '查看附件'
}

/** 详情里的附件地址：相对路径补全接口前缀 */
function attachmentHref(url) {
  if (!url) return ''
  return /^https?:\/\//i.test(url) ? url : fileBaseUrl + url
}

/** 审批记录时间文案 */
function recordTime(record) {
  if (!record) return ''
  if (!record.endTime) return oaDateTime(record.startTime) + ' 起'
  return oaDateTime(record.startTime) + ' → ' + oaDateTime(record.endTime)
}

getList()
</script>

<style scoped lang="scss">
.oa-filter-bar {
  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.oa-detail-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
}

.oa-detail-section {
  margin-top: 18px;

  h4 {
    margin: 0 0 12px;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }
}

.oa-pre {
  white-space: pre-wrap;
  word-break: break-word;
}

.oa-comment {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
}
</style>
