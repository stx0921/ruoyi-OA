<template>
  <div class="app-container member-page">
    <div class="oa-page-title">
      成员管理
      <small>共 {{ total }} 名成员{{ ministerView ? '（仅本部门）' : '' }}</small>
    </div>

    <!-- 统计卡：4 列等宽，点击下钻 -->
    <div class="member-stat-grid">
      <div
        v-for="card in statCards"
        :key="card.key"
        class="member-stat"
        :class="['is-' + card.tone, { 'is-clickable': !statsLoading && !statsError }]"
        @click="handleStatClick(card.key)"
      >
        <div class="member-stat__ico">
          <el-icon :size="20"><component :is="card.icon" /></el-icon>
        </div>
        <div class="member-stat__body">
          <template v-if="statsLoading">
            <span class="member-skeleton__bar" style="width: 48px; height: 22px"></span>
            <span class="member-skeleton__bar" style="width: 64px"></span>
          </template>
          <template v-else-if="statsError">
            <el-tooltip content="统计加载失败" placement="top">
              <div class="member-stat__num">--</div>
            </el-tooltip>
            <div class="member-stat__label">{{ card.label }}</div>
          </template>
          <template v-else>
            <div class="member-stat__num">{{ formatStatValue(card.value) }}</div>
            <div class="member-stat__label">{{ card.label }}</div>
          </template>
        </div>
      </div>
    </div>

    <div class="oa-card">
      <!-- 筛选栏：手动触发（查询 / 回车） -->
      <el-form :model="filters" :inline="true" class="oa-filter-bar" @submit.prevent="handleQuery">
        <el-form-item label="关键字">
          <el-input
            v-model="filters.keyword"
            placeholder="姓名 / 学号 / 手机号"
            clearable
            style="width: 240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="filters.deptId" placeholder="全部部门" clearable style="width: 160px">
            <el-option
              v-for="item in deptOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="职务">
          <el-select v-model="filters.position" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="filters.grade" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in gradeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="届次">
          <el-select v-model="filters.cohort" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in cohortOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="filters.source" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" :loading="loading" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 统计卡下钻后的入会日期范围提示 -->
      <div v-if="joinDateRange.length === 2" class="member-extra-filter">
        <span class="oa-muted">入会日期：{{ joinDateRange[0] }} ~ {{ joinDateRange[1] }}</span>
        <el-button link type="primary" @click="clearJoinDateRange">清除</el-button>
      </div>

      <!-- 操作条 -->
      <div class="oa-toolbar">
        <div class="oa-toolbar__left">
          <el-button v-if="canAdd" v-hasPermi="['oa:member:add']" type="primary" plain icon="Plus" @click="handleAdd">新增成员</el-button>
          <el-button v-if="canImport" v-hasPermi="['oa:member:import']" icon="Upload" @click="goImport">批量导入</el-button>
          <el-dropdown
            v-if="canEdit && selectedRows.length"
            v-hasPermi="['oa:member:edit']"
            trigger="click"
            @command="handleBatchCommand"
          >
            <el-button icon="Operation">批量操作<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="status">批量改状态</el-dropdown-item>
                <el-dropdown-item v-if="canTransfer" command="transfer">批量调部门</el-dropdown-item>
                <el-dropdown-item v-if="canRemove" command="delete" divided>批量删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="oa-toolbar__right">
          <span v-if="ministerView" class="oa-muted">当前视角：部长（仅本部门数据）</span>
          <el-button
            v-if="canExport"
            v-hasPermi="['oa:member:export']"
            icon="Download"
            :loading="exporting"
            @click="handleExport"
          >导出</el-button>
        </div>
      </div>

      <!-- 批量操作条 -->
      <div v-if="selectedRows.length" class="member-batch-bar">
        <div class="member-batch-bar__left">
          <span class="member-batch-bar__count">已选 {{ selectedRows.length }} 项</span>
          <el-button link type="primary" @click="clearSelection(true)">清空选择</el-button>
        </div>
        <div class="member-batch-bar__right">
          <el-tooltip :disabled="selectedRows.length <= 200" content="单次最多选择 200 条，请分批操作" placement="top">
            <span>
              <el-button
                size="small"
                v-hasPermi="['oa:member:edit']"
                :disabled="selectedRows.length > 200"
                @click="openBatchStatus"
              >批量改状态</el-button>
            </span>
          </el-tooltip>
          <el-tooltip v-if="canTransfer" :disabled="selectedRows.length <= 200" content="单次最多选择 200 条，请分批操作" placement="top">
            <span>
              <el-button
                size="small"
                v-hasPermi="['oa:member:edit']"
                :disabled="selectedRows.length > 200"
                @click="openBatchTransfer"
              >批量调部门</el-button>
            </span>
          </el-tooltip>
          <el-tooltip v-if="canRemove" :disabled="selectedRows.length <= 200" content="单次最多选择 200 条，请分批操作" placement="top">
            <span>
              <el-button
                size="small"
                type="danger"
                plain
                v-hasPermi="['oa:member:remove']"
                :disabled="selectedRows.length > 200"
                @click="handleBatchDelete"
              >批量删除</el-button>
            </span>
          </el-tooltip>
        </div>
      </div>

      <!-- 错误态：保留筛选，可重新加载 -->
      <div v-if="loadError" class="member-error">
        <div class="member-error__title">成员列表加载失败</div>
        <el-button type="primary" plain @click="getList">重新加载</el-button>
      </div>

      <!-- loading 骨架 -->
      <div v-else-if="loading" class="member-skeleton">
        <div class="member-skeleton__head"></div>
        <div v-for="index in 8" :key="index" class="member-skeleton__row">
          <span class="member-skeleton__bar" style="width: 3%"></span>
          <span class="member-skeleton__bar" style="width: 9%"></span>
          <span class="member-skeleton__bar" style="width: 7%"></span>
          <span class="member-skeleton__bar" style="width: 9%"></span>
          <span class="member-skeleton__bar" style="width: 5%"></span>
          <span class="member-skeleton__bar" style="width: 6%"></span>
          <span class="member-skeleton__bar" style="width: 8%"></span>
          <span class="member-skeleton__bar" style="width: 7%"></span>
          <span class="member-skeleton__bar" style="width: 6%"></span>
          <span class="member-skeleton__bar" style="width: 9%"></span>
          <span class="member-skeleton__bar" style="width: 9%"></span>
          <span class="member-skeleton__bar" style="width: 12%"></span>
        </div>
      </div>

      <!-- 成员表格 -->
      <el-table
        v-else
        ref="memberTableRef"
        :data="memberList"
        row-key="memberId"
        class="member-table"
        :row-class-name="() => 'member-row'"
        @selection-change="handleSelectionChange"
        @row-click="handleRowClick"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column label="成员编号" prop="memberNo" width="120" sortable="custom">
          <template #default="scope">
            <span>{{ scope.row.memberNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="姓名" prop="name" width="90" show-overflow-tooltip>
          <template #default="scope">
            <span class="oa-strong">{{ scope.row.name || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学号" prop="studentNo" width="110" show-overflow-tooltip />
        <el-table-column label="性别" width="60" align="center">
          <template #default="scope">{{ genderText(scope.row.gender, sys_user_sex) }}</template>
        </el-table-column>
        <el-table-column label="年级" width="80" align="center">
          <template #default="scope">{{ scope.row.gradeLabel || gradeText(scope.row.grade, oa_grade) }}</template>
        </el-table-column>
        <el-table-column label="部门" prop="deptName" width="110" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.deptName || '—' }}</template>
        </el-table-column>
        <el-table-column label="职务" width="90" align="center">
          <template #default="scope">{{ scope.row.positionLabel || positionText(scope.row.position, oa_position) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <oa-status-tag :status="scope.row.status" :map="MEMBER_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="140" align="center">
          <template #default="scope">
            <oa-sensitive-text
              :value="scope.row.phone"
              :masked="isMaskedValue(scope.row.phone)"
              copyable
            />
          </template>
        </el-table-column>
        <el-table-column label="入会日期" prop="joinDate" width="120" align="center" sortable="custom">
          <template #default="scope">{{ formatMemberDate(scope.row.joinDate) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button v-hasPermi="['oa:member:query']" link type="primary" icon="View" @click.stop="handleDetail(scope.row)">详情</el-button>
            <el-button
              v-if="canEdit"
              v-hasPermi="['oa:member:edit']"
              link
              type="primary"
              icon="Edit"
              @click.stop="handleEdit(scope.row)"
            >编辑</el-button>
            <span v-if="hasRowMoreActions(scope.row)" v-hasPermi="['oa:member:edit']" @click.stop>
              <el-dropdown trigger="click" @command="command => handleRowCommand(command, scope.row)">
                <el-button link type="primary">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="canTransfer" command="transfer">调部门</el-dropdown-item>
                    <el-dropdown-item v-if="canTransfer" command="promote">升职</el-dropdown-item>
                    <el-dropdown-item v-if="statusOf(scope.row) === '0'" command="quit" divided>退会</el-dropdown-item>
                    <el-dropdown-item v-if="statusOf(scope.row) === '0'" command="graduate">毕业</el-dropdown-item>
                    <el-dropdown-item v-if="statusOf(scope.row) !== '0'" command="restore">恢复在籍</el-dropdown-item>
                    <el-dropdown-item v-if="canRemove" command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </span>
          </template>
        </el-table-column>

        <template #empty>
          <div class="member-empty">
            <template v-if="hasActiveFilter">
              <div class="member-empty__text">没有找到匹配的成员，试试调整筛选条件</div>
              <el-button type="primary" plain @click="resetQuery">重置筛选</el-button>
            </template>
            <template v-else>
              <div class="member-empty__text">还没有成员</div>
              <div class="member-empty__sub">导入 Excel 或在系统里手动录入</div>
              <div class="member-empty__actions">
                <el-button v-if="canImport" type="primary" @click="goImport">批量导入</el-button>
                <el-button v-if="canAdd" @click="handleAdd">手动录入</el-button>
              </div>
            </template>
          </div>
        </template>
      </el-table>

      <pagination
        v-show="!loading && !loadError && total > 0"
        :total="total"
        v-model:page="page.pageNum"
        v-model:limit="page.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @pagination="handlePageChange"
      />
    </div>

    <!-- 详情抽屉（PC-02） -->
    <member-detail-drawer ref="detailDrawerRef" @success="handleMutated" />
    <!-- 新增 / 编辑抽屉 -->
    <member-form-dialog ref="formDialogRef" @success="handleMutated" />

    <!-- 批量改状态 -->
    <el-dialog v-model="batchStatusVisible" title="批量改状态" width="520px" append-to-body :close-on-click-modal="false">
      <el-form label-width="88px">
        <el-form-item label="目标状态">
          <el-radio-group v-model="batchStatusForm.status">
            <el-radio value="1">退会</el-radio>
            <el-radio value="2">毕业</el-radio>
            <el-radio value="0">恢复在籍</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="batchStatusForm.status !== '0'" label="离会日期">
          <el-date-picker
            v-model="batchStatusForm.leaveDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="默认今天"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="batchStatusForm.remark"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="选填，写入操作日志"
          />
        </el-form-item>
        <div class="member-dialog-tip">将处理已选 {{ selectedRows.length }} 名成员，逐条调用状态变更接口。</div>
      </el-form>
      <template #footer>
        <el-button @click="batchStatusVisible = false">取 消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="submitBatchStatus">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 批量调部门 -->
    <el-dialog v-model="batchTransferVisible" title="批量调部门" width="520px" append-to-body :close-on-click-modal="false">
      <el-form label-width="88px">
        <el-form-item label="目标部门">
          <el-select v-model="batchTransferForm.deptId" placeholder="请选择部门" filterable style="width: 100%">
            <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="batchTransferForm.remark"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="选填，写入操作日志"
          />
        </el-form-item>
        <div class="member-dialog-tip">将把已选 {{ selectedRows.length }} 名成员调入目标部门，逐条调用调部门接口。</div>
      </el-form>
      <template #footer>
        <el-button @click="batchTransferVisible = false">取 消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="submitBatchTransfer">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 单条：调部门 / 升职（760px） -->
    <el-dialog
      v-model="transferVisible"
      :title="transferForm.type === 'promote' ? '升职' : '调部门'"
      width="760px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form label-width="88px">
        <el-form-item label="成员">
          <el-input :model-value="transferForm.name" disabled />
        </el-form-item>
        <el-form-item v-if="transferForm.type !== 'promote'" label="目标部门">
          <el-select v-model="transferForm.deptId" placeholder="请选择部门" filterable style="width: 100%">
            <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="transferForm.type !== 'transfer'" label="目标职务">
          <el-select v-model="transferForm.position" placeholder="请选择职务" style="width: 100%">
            <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="transferForm.remark"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="选填，写入操作日志"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取 消</el-button>
        <el-button type="primary" :loading="transferSubmitting" @click="submitTransfer">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="OaMember">
import { saveAs } from 'file-saver'
import {
  listMember,
  getMemberStats,
  exportMember,
  changeMemberStatus,
  transferMember,
  delMember,
  deptTreeSelect
} from '@/api/oa/member'
import {
  MEMBER_STATUS_MAP,
  MEMBER_STATUS_OPTIONS,
  POSITION_OPTIONS,
  GRADE_OPTIONS,
  COHORT_OPTIONS,
  SOURCE_OPTIONS,
  genderText,
  gradeText,
  positionText,
  formatMemberDate,
  isMaskedValue,
  resolveOptions,
  flattenDeptOptions
} from '@/utils/oaMember'
import { blobValidate, parseTime } from '@/utils/ruoyi'
import OaStatusTag from '@/components/oa/OaStatusTag.vue'
import OaSensitiveText from '@/components/oa/OaSensitiveText.vue'
import MemberDetailDrawer from './MemberDetailDrawer.vue'
import MemberFormDialog from './MemberFormDialog.vue'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()
const { oa_grade, oa_cohort, oa_position, oa_member_status, oa_member_source, sys_user_sex } = useDict(
  'oa_grade',
  'oa_cohort',
  'oa_position',
  'oa_member_status',
  'oa_member_source',
  'sys_user_sex'
)

/* -------------------------------------------------------------------------- */
/* 权限与角色                                                                 */
/* -------------------------------------------------------------------------- */
const canAdd = computed(() => proxy.$auth.hasPermi('oa:member:add'))
const canEdit = computed(() => proxy.$auth.hasPermi('oa:member:edit'))
const canExport = computed(() => proxy.$auth.hasPermi('oa:member:export'))
const canImport = computed(() => proxy.$auth.hasPermi('oa:member:import'))
const canRemove = computed(() => proxy.$auth.hasPermi('oa:member:remove'))
const isAdmin = computed(() => userStore.roles.indexOf('admin') > -1)
const isPresident = computed(() => userStore.roles.indexOf('oa_president') > -1)
const isMinister = computed(() => userStore.roles.indexOf('oa_minister') > -1)
// 调部门 / 升职：仅管理员 / 主席团（部长前端隐藏，后端仍拦截）
const canTransfer = computed(() => (isAdmin.value || isPresident.value) && canEdit.value)
// 部长视角：仅本部门（后端 @DataScope 裁剪，前端只做提示）
const ministerView = computed(() => isMinister.value && !isAdmin.value && !isPresident.value)

/* -------------------------------------------------------------------------- */
/* 字典选项（字典优先，静态映射兜底）                                          */
/* -------------------------------------------------------------------------- */
const statusOptions = computed(() => resolveOptions(oa_member_status.value, MEMBER_STATUS_OPTIONS))
const positionOptions = computed(() => resolveOptions(oa_position.value, POSITION_OPTIONS))
const gradeOptions = computed(() => resolveOptions(oa_grade.value, GRADE_OPTIONS))
const cohortOptions = computed(() => resolveOptions(oa_cohort.value, COHORT_OPTIONS))
const sourceOptions = computed(() => resolveOptions(oa_member_source.value, SOURCE_OPTIONS))

/* -------------------------------------------------------------------------- */
/* 查询状态                                                                   */
/* -------------------------------------------------------------------------- */
const loading = ref(true)
const loadError = ref(false)
const exporting = ref(false)
const memberList = ref([])
const total = ref(0)
const selectedRows = ref([])
const memberTableRef = ref()
const detailDrawerRef = ref()
const formDialogRef = ref()

const page = reactive({ pageNum: 1, pageSize: 10 })
const sort = reactive({ orderByColumn: undefined, isAsc: undefined })
const filters = reactive({
  keyword: '',
  deptId: undefined,
  position: undefined,
  status: undefined,
  grade: undefined,
  cohort: undefined,
  source: undefined
})
// 统计卡「本月新增」下钻用：入会日期范围（列表接口 params[beginJoinDate,endJoinDate]）
const joinDateRange = ref([])

const stats = ref({ total: 0, pendingApply: 0, monthJoin: 0, monthLeave: 0 })
const statsLoading = ref(true)
const statsError = ref(false)

/** 统计卡配置 */
const statCards = computed(() => [
  { key: 'total', label: '在籍总人数', value: stats.value.total, icon: 'UserFilled', tone: 'blue' },
  { key: 'pending', label: '待审批申请', value: stats.value.pendingApply, icon: 'Stamp', tone: 'orange' },
  { key: 'join', label: '本月新增', value: stats.value.monthJoin, icon: 'CirclePlus', tone: 'green' },
  { key: 'leave', label: '本月退会', value: stats.value.monthLeave, icon: 'RemoveFilled', tone: 'red' }
])

const hasActiveFilter = computed(() => {
  if ((filters.keyword || '').trim()) return true
  if (joinDateRange.value && joinDateRange.value.length === 2) return true
  return ['deptId', 'position', 'status', 'grade', 'cohort', 'source'].some(key => {
    const value = filters[key]
    return value !== undefined && value !== null && value !== ''
  })
})

function formatStatValue(value) {
  if (value === undefined || value === null || value === '') return 0
  return value
}

/* -------------------------------------------------------------------------- */
/* 部门下拉（若依部门树，部长仅见本部门）                                      */
/* -------------------------------------------------------------------------- */
const deptOptions = ref([])

function loadDeptOptions() {
  deptTreeSelect()
    .then(response => {
      deptOptions.value = flattenDeptOptions(response.data || [])
    })
    .catch(() => {})
}

/* -------------------------------------------------------------------------- */
/* 列表查询                                                                   */
/* -------------------------------------------------------------------------- */

/** 关键字：<4 位纯数字按手机号片段处理，提示后不发起查询 */
function validateKeyword() {
  const keyword = (filters.keyword || '').trim()
  if (keyword && /^\d+$/.test(keyword) && keyword.length < 4) {
    proxy.$modal.msgWarning('请输入至少 4 位手机号后再查询')
    return false
  }
  return true
}

/** 关键字落到契约字段：学号（20 开头 8–12 位）/ 手机号片段 / 姓名 */
function applyKeywordParams(params) {
  const keyword = (filters.keyword || '').trim()
  if (!keyword) return
  params.keyword = keyword
  if (!/^\d+$/.test(keyword)) {
    params.name = keyword
    return
  }
  if (/^20\d{6,10}$/.test(keyword)) {
    params.studentNo = keyword
  } else {
    params.phone = keyword
  }
}

/** 组装列表 / 导出参数（契约 §3.1 的 1 号接口） */
function buildQueryParams(withPagination) {
  const params = {}
  if (withPagination !== false) {
    params.pageNum = page.pageNum
    params.pageSize = page.pageSize
  }
  applyKeywordParams(params)
  ;['deptId', 'position', 'status', 'grade', 'cohort', 'source'].forEach(key => {
    const value = filters[key]
    if (value !== undefined && value !== null && value !== '') params[key] = value
  })
  if (joinDateRange.value && joinDateRange.value.length === 2) {
    params.params = {
      beginJoinDate: joinDateRange.value[0],
      endJoinDate: joinDateRange.value[1]
    }
  }
  if (sort.orderByColumn) {
    params.orderByColumn = sort.orderByColumn
    params.isAsc = sort.isAsc
  }
  return params
}

function getList() {
  if (!validateKeyword()) return
  loading.value = true
  loadError.value = false
  listMember(buildQueryParams())
    .then(response => {
      memberList.value = response.rows || []
      total.value = response.total || 0
    })
    .catch(() => {
      loadError.value = true
      memberList.value = []
      total.value = 0
    })
    .finally(() => {
      loading.value = false
    })
}

function getStats() {
  statsLoading.value = true
  statsError.value = false
  getMemberStats()
    .then(response => {
      stats.value = response.data || {}
    })
    .catch(() => {
      statsError.value = true
    })
    .finally(() => {
      statsLoading.value = false
    })
}

/** 查询 */
function handleQuery() {
  clearSelection(true)
  page.pageNum = 1
  getList()
}

/** 只清空筛选条件，不发请求 */
function resetFilters() {
  filters.keyword = ''
  filters.deptId = undefined
  filters.position = undefined
  filters.status = undefined
  filters.grade = undefined
  filters.cohort = undefined
  filters.source = undefined
  joinDateRange.value = []
  sort.orderByColumn = undefined
  sort.isAsc = undefined
}

/** 重置 */
function resetQuery() {
  resetFilters()
  clearSelection(false)
  page.pageNum = 1
  getList()
}

/** 分页：翻页清空选中并提示 */
function handlePageChange() {
  clearSelection(true)
  getList()
}

/** 服务端排序 */
function handleSortChange({ prop, order }) {
  if (!order) {
    sort.orderByColumn = undefined
    sort.isAsc = undefined
  } else {
    // 契约里排序字段用 snake_case（后端 SQL 列名）
    sort.orderByColumn = prop === 'memberNo' ? 'member_no' : prop === 'joinDate' ? 'join_date' : prop
    sort.isAsc = order === 'ascending' ? 'asc' : 'desc'
  }
  clearSelection(true)
  page.pageNum = 1
  getList()
}

function handleSelectionChange(selection) {
  selectedRows.value = selection || []
}

/** 清空选择；notify=true 时提示「已清空选择」 */
function clearSelection(notify) {
  if (selectedRows.value.length && notify) {
    proxy.$modal.msg('已清空选择')
  }
  selectedRows.value = []
  if (memberTableRef.value) memberTableRef.value.clearSelection()
}

function statusOf(row) {
  if (!row || row.status === undefined || row.status === null) return ''
  return String(row.status)
}

/** 行点击打开详情抽屉（勾选列除外） */
function handleRowClick(row, column) {
  if (column && column.type === 'selection') return
  handleDetail(row)
}

function hasRowMoreActions(row) {
  if (canRemove.value) return true
  if (!canEdit.value) return false
  if (canTransfer.value) return true
  return ['0', '1', '2'].indexOf(statusOf(row)) > -1
}

/* -------------------------------------------------------------------------- */
/* 统计卡下钻                                                                 */
/* -------------------------------------------------------------------------- */
function handleStatClick(key) {
  if (statsLoading.value || statsError.value) return
  if (key === 'pending') {
    gotoRoute(['/hr/apply', '/oa/apply'])
    return
  }
  resetFilters()
  if (key === 'total') {
    filters.status = '0'
  } else if (key === 'join') {
    const now = new Date()
    const begin = parseTime(new Date(now.getFullYear(), now.getMonth(), 1), '{y}-{m}-{d}')
    const end = parseTime(now, '{y}-{m}-{d}')
    joinDateRange.value = [begin, end]
  } else if (key === 'leave') {
    // 列表接口不提供离会日期筛选：回填「已退会」状态（本月退会人数的下钻口径）
    filters.status = '1'
  }
  page.pageNum = 1
  clearSelection(false)
  getList()
}

function clearJoinDateRange() {
  joinDateRange.value = []
  handleQuery()
}

/* -------------------------------------------------------------------------- */
/* 新增 / 详情 / 编辑                                                          */
/* -------------------------------------------------------------------------- */
function handleAdd() {
  formDialogRef.value.open({ mode: 'add' })
}

function handleDetail(row) {
  if (!proxy.$auth.hasPermi('oa:member:query')) {
    proxy.$modal.msgWarning('没有访问成员详情的权限')
    return
  }
  detailDrawerRef.value.open({ memberId: row.memberId })
}

function handleEdit(row) {
  formDialogRef.value.open({ mode: 'edit', memberId: row.memberId })
}

/** 子组件保存 / 状态变更成功后刷新列表与统计 */
function handleMutated() {
  getList()
  getStats()
}

/* -------------------------------------------------------------------------- */
/* 单条状态 / 调部门 / 升职                                                    */
/* -------------------------------------------------------------------------- */
function handleRowCommand(command, row) {
  if (command === 'delete') {
    handleDelete(row)
    return
  }
  if (command === 'quit' || command === 'graduate' || command === 'restore') {
    handleStatusAction(row, command)
    return
  }
  openTransfer(row, command)
}

/* -------------------------------------------------------------------------- */
/* 删除（软删除 del_flag=1：列表隐藏、账号停用、数据保留）                      */
/* -------------------------------------------------------------------------- */
function handleDelete(row) {
  proxy.$modal
    .confirm('确认删除 ' + row.name + '（' + row.studentNo + '）？删除后将从成员列表隐藏、账号停用，数据保留并记录操作日志')
    .then(() => delMember([row.memberId]))
    .then(() => {
      proxy.$modal.msgSuccess('已删除，账号已停用，数据保留并记录日志')
      clearSelection(false)
      handleMutated()
    })
    .catch(() => {})
}

function handleBatchDelete() {
  if (!ensureBatchLimit()) return
  const rows = [...selectedRows.value]
  proxy.$modal
    .confirm('确认删除选中的 ' + rows.length + ' 名成员？删除后将从成员列表隐藏、账号停用，数据保留并记录操作日志')
    .then(() => delMember(rows.map(item => item.memberId)))
    .then(() => {
      proxy.$modal.msgSuccess('已删除 ' + rows.length + ' 名成员，账号已停用，数据保留并记录日志')
      clearSelection(false)
      handleMutated()
    })
    .catch(() => {})
}

/** 状态变更二次确认 */
function handleStatusAction(row, type) {
  const props = {
    quit: {
      status: '1',
      message: '确认将 ' + row.name + ' 设为 已退会？账号将停用，数据保留',
      success: '已设为 已退会，账号已停用，数据保留'
    },
    graduate: {
      status: '2',
      message: '确认将 ' + row.name + ' 设为 已毕业？账号将停用，数据保留',
      success: '已设为 已毕业，账号已停用，数据保留'
    },
    restore: {
      status: '0',
      message: '确认将 ' + row.name + ' 恢复为 在籍？账号将重新启用',
      success: '已恢复在籍，账号已重新启用'
    }
  }[type]
  if (!props) return
  proxy.$modal
    .confirm(props.message)
    .then(() =>
      changeMemberStatus({
        memberId: row.memberId,
        status: props.status,
        leaveDate: props.status === '0' ? null : parseTime(new Date(), '{y}-{m}-{d}'),
        remark: ''
      })
    )
    .then(() => {
      proxy.$modal.msgSuccess(props.success)
      handleMutated()
    })
    .catch(() => {})
}

/* -------------------------------------------------------------------------- */
/* 调部门 / 升职（单条，760px）                                                */
/* -------------------------------------------------------------------------- */
const transferVisible = ref(false)
const transferSubmitting = ref(false)
const transferForm = reactive({
  memberId: undefined,
  name: '',
  type: 'transfer',
  deptId: undefined,
  position: undefined,
  remark: ''
})

function openTransfer(row, type) {
  transferForm.memberId = row.memberId
  transferForm.name = row.name || ''
  transferForm.type = type === 'promote' ? 'promote' : 'transfer'
  transferForm.deptId = row.deptId
  transferForm.position = row.position
  transferForm.remark = ''
  transferVisible.value = true
}

async function submitTransfer() {
  if (transferForm.type === 'transfer' && !transferForm.deptId) {
    proxy.$modal.msgWarning('请选择目标部门')
    return
  }
  if (transferForm.type === 'promote' && !transferForm.position) {
    proxy.$modal.msgWarning('请选择目标职务')
    return
  }
  const payload = {
    memberId: transferForm.memberId,
    deptId: transferForm.deptId,
    position: transferForm.position,
    remark: transferForm.remark || ''
  }
  const message =
    transferForm.type === 'promote'
      ? '确认将 ' + transferForm.name + ' 的职务调整为 ' + positionText(transferForm.position, oa_position.value) + '？'
      : '确认将 ' + transferForm.name + ' 调入 ' + deptLabel(transferForm.deptId) + '？'
  try {
    await proxy.$modal.confirm(message)
  } catch (e) {
    return
  }
  transferSubmitting.value = true
  try {
    await transferMember(payload)
    proxy.$modal.msgSuccess(
      transferForm.type === 'promote'
        ? '职务已更新为 ' + positionText(transferForm.position, oa_position.value)
        : '已调入 ' + deptLabel(transferForm.deptId) + '，操作已记录日志'
    )
    transferVisible.value = false
    handleMutated()
  } catch (e) {
    // 失败原因由 request 拦截器统一提示
  } finally {
    transferSubmitting.value = false
  }
}

function deptLabel(deptId) {
  const hit = deptOptions.value.find(item => String(item.value) === String(deptId))
  return hit ? hit.label.replace(/^　+/, '') : ''
}

/* -------------------------------------------------------------------------- */
/* 批量操作（v1 按单条接口循环，≤200 条）                                       */
/* -------------------------------------------------------------------------- */
const batchStatusVisible = ref(false)
const batchSubmitting = ref(false)
const batchStatusForm = reactive({ status: '1', leaveDate: undefined, remark: '' })

const batchTransferVisible = ref(false)
const batchTransferForm = reactive({ deptId: undefined, remark: '' })

function ensureBatchLimit() {
  if (!selectedRows.value.length) {
    proxy.$modal.msgWarning('请先选择要操作的成员')
    return false
  }
  if (selectedRows.value.length > 200) {
    proxy.$modal.msgWarning('单次最多选择 200 条，请分批操作')
    return false
  }
  return true
}

function handleBatchCommand(command) {
  if (command === 'status') openBatchStatus()
  if (command === 'transfer') openBatchTransfer()
  if (command === 'delete') handleBatchDelete()
}

function openBatchStatus() {
  if (!ensureBatchLimit()) return
  batchStatusForm.status = '1'
  batchStatusForm.leaveDate = parseTime(new Date(), '{y}-{m}-{d}')
  batchStatusForm.remark = ''
  batchStatusVisible.value = true
}

function openBatchTransfer() {
  if (!ensureBatchLimit()) return
  batchTransferForm.deptId = undefined
  batchTransferForm.remark = ''
  batchTransferVisible.value = true
}

/** 顺序执行，逐条累计成功 / 失败，最后一次性提示 */
async function runBatch(tasks, successText) {
  batchSubmitting.value = true
  let success = 0
  let fail = 0
  for (let index = 0; index < tasks.length; index++) {
    try {
      await tasks[index]()
      success++
    } catch (e) {
      fail++
    }
  }
  batchSubmitting.value = false
  if (fail) {
    proxy.$modal.msgWarning('已处理 ' + success + ' 条，' + fail + ' 条失败，请重试失败项')
  } else {
    proxy.$modal.msgSuccess(successText.replace('{count}', success))
  }
  batchStatusVisible.value = false
  batchTransferVisible.value = false
  clearSelection(false)
  handleMutated()
}

function submitBatchStatus() {
  const rows = [...selectedRows.value]
  const { status, leaveDate, remark } = batchStatusForm
  const actionText = status === '0' ? '恢复在籍' : status === '1' ? '退会' : '毕业'
  const tasks = rows.map(row => () =>
    changeMemberStatus({
      memberId: row.memberId,
      status,
      leaveDate: status === '0' ? null : leaveDate || parseTime(new Date(), '{y}-{m}-{d}'),
      remark: remark || ''
    })
  )
  runBatch(tasks, '已将 {count} 名成员设为 ' + actionText)
}

function submitBatchTransfer() {
  if (!batchTransferForm.deptId) {
    proxy.$modal.msgWarning('请选择目标部门')
    return
  }
  const rows = [...selectedRows.value]
  const deptId = batchTransferForm.deptId
  const remark = batchTransferForm.remark || ''
  const tasks = rows.map(row => () =>
    transferMember({
      memberId: row.memberId,
      deptId,
      position: row.position,
      remark
    })
  )
  runBatch(tasks, '已将 {count} 名成员调入 ' + deptLabel(deptId))
}

/* -------------------------------------------------------------------------- */
/* 导出（二次确认 + blob 下载）                                                */
/* -------------------------------------------------------------------------- */
function handleExport() {
  if (total.value > 10000) {
    proxy.$modal.msgWarning('单次最多导出 10000 条，请先缩小筛选范围')
    return
  }
  proxy.$modal
    .confirm('将按当前筛选导出 ' + total.value + ' 条成员数据，导出行为会记录日志。确认导出？')
    .then(doExport)
    .catch(() => {})
}

async function doExport() {
  exporting.value = true
  try {
    const data = await exportMember(buildQueryParams(false))
    if (!blobValidate(data)) {
      const resText = await data.text()
      let message = '导出失败，请稍后重试'
      try {
        const rspObj = JSON.parse(resText)
        message = rspObj.msg || message
      } catch (e) {
        message = '导出失败，请稍后重试'
      }
      proxy.$modal.msgError(message)
      return
    }
    const filename = '成员数据_' + parseTime(new Date(), '{y}{m}{d}{h}{i}{s}') + '.xlsx'
    saveAs(new Blob([data]), filename)
    proxy.$modal.msgSuccess('已导出 ' + total.value + ' 条成员数据，导出行为已记录日志')
  } catch (e) {
    proxy.$modal.msgError('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

/* -------------------------------------------------------------------------- */
/* 路由跳转（以动态菜单 /hr 前缀为准，/oa 仅作兜底）                           */
/* -------------------------------------------------------------------------- */
function goImport() {
  gotoRoute(['/hr/import', '/oa/member/import'])
}

function gotoRoute(candidates) {
  const target = (candidates || []).find(path => router.resolve(path).matched.length > 0)
  router.push(target || candidates[0])
}

/* -------------------------------------------------------------------------- */
/* 初始化                                                                     */
/* -------------------------------------------------------------------------- */
getList()
getStats()
loadDeptOptions()
</script>

<style scoped lang="scss">
/* ---------- 统计卡 ---------- */
.member-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

@media (max-width: 1100px) {
  .member-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.member-stat {
  display: flex;
  align-items: center;
  gap: 14px;
  height: 96px;
  padding: 0 20px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
  transition: box-shadow 0.2s, border-color 0.2s;
}

.member-stat.is-clickable {
  cursor: pointer;

  &:hover {
    border-color: #236de9;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }
}

.member-stat__ico {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  flex: 0 0 36px;
}

.member-stat.is-blue .member-stat__ico {
  background: rgba(35, 109, 233, 0.1);
  color: #236de9;
}

.member-stat.is-orange .member-stat__ico {
  background: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}

.member-stat.is-green .member-stat__ico {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.member-stat.is-red .member-stat__ico {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.member-stat__body {
  min-width: 0;
}

.member-stat__num {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
  color: #303133;
}

.member-stat__label {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
}

/* ---------- 筛选补充条件 ---------- */
.member-extra-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: -4px 0 10px;
}

/* ---------- 批量操作条 ---------- */
.member-batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 48px;
  padding: 0 14px;
  margin-bottom: 12px;
  background: #edf3fe;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
}

.member-batch-bar__left,
.member-batch-bar__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-batch-bar__count {
  font-size: 13px;
  font-weight: 600;
  color: #236de9;
}

/* ---------- 骨架 ---------- */
.member-skeleton {
  padding: 4px 0;
  overflow: hidden;
}

.member-skeleton__head {
  height: 44px;
  background: #fafafa;
  border-radius: 2px;
  margin-bottom: 4px;
}

.member-skeleton__row {
  display: flex;
  align-items: center;
  gap: 18px;
  height: 48px;
  border-bottom: 1px solid #f0f2f5;
  padding: 0 8px;
}

.member-skeleton__bar {
  display: inline-block;
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f2f4f7 25%, #e9edf3 37%, #f2f4f7 63%);
  background-size: 400% 100%;
  animation: member-skeleton-loading 1.4s ease infinite;
}

.member-stat__body .member-skeleton__bar {
  display: block;
  margin-bottom: 6px;
}

@keyframes member-skeleton-loading {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

/* ---------- 错误态 / 空态 ---------- */
.member-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 0;
  color: #909399;
}

.member-error__title {
  font-size: 14px;
  color: #606266;
}

.member-empty {
  padding: 36px 0;
  text-align: center;
}

.member-empty__text {
  font-size: 14px;
  color: #606266;
}

.member-empty__sub {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.member-empty__actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
}

/* ---------- 表格行可点击 ---------- */
.member-table :deep(.member-row) {
  cursor: pointer;
}

.member-table :deep(.member-row:hover > td) {
  background: #f5f7fa;
}

/* ---------- 弹窗提示 ---------- */
.member-dialog-tip {
  padding-left: 88px;
  font-size: 12px;
  line-height: 1.6;
  color: #909399;
}
</style>
