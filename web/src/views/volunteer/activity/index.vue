<template>
  <div class="app-container va-page">
    <div class="va-page-head">
      <div class="va-page-head__main">
        <div class="oa-page-title">志愿活动</div>
        <p class="va-page-desc">
          所有登录用户均可查看、新增、编辑与删除活动；活动人数仅作参考，不做报名硬上限。
        </p>
      </div>
      <el-button
        v-hasPermi="['oa:volunteer:add']"
        type="primary"
        icon="Plus"
        @click="handleAdd"
      >新增活动</el-button>
    </div>

    <div class="oa-card">
      <!-- 筛选栏 -->
      <el-form :model="filters" :inline="true" class="oa-filter-bar" @submit.prevent="handleQuery">
        <el-form-item label="状态">
          <el-select v-model="filters.showStatus" placeholder="全部状态" clearable style="width: 140px">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="活动名称 / 地点"
            clearable
            style="width: 240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            unlink-panels
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" :loading="loading" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 列表说明 -->
      <div class="va-list-meta">
        <span class="oa-muted">
          共 <b class="va-count">{{ total }}</b> 个活动 · 状态由「报名截止 / 开始 / 结束」时间自动推导，可手动设为「已取消」
        </span>
        <span class="oa-muted">排序：按开始时间倒序</span>
      </div>

      <!-- 活动表格 -->
      <el-table
        v-loading="loading"
        :data="list"
        row-key="activityId"
        empty-text="没有符合条件的活动"
        class="va-table"
      >
        <el-table-column label="活动名称" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <span class="va-name">{{ scope.row.name || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="地点" width="150" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.location || '—' }}</template>
        </el-table-column>
        <el-table-column label="活动人数" width="100" align="center">
          <template #default="scope">{{ capacityText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="志愿时长" width="95" align="center">
          <template #default="scope">{{ formatHours(scope.row.durationHours) }} h</template>
        </el-table-column>
        <el-table-column label="起止时间" width="215">
          <template #default="scope">
            <div>{{ formatActivityTime(scope.row.startTime) }}</div>
            <div class="oa-muted">~ {{ formatActivityTime(scope.row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="报名 / 通过" width="120" align="center">
          <template #default="scope">
            <span>{{ signupCountOf(scope.row) }} / </span>
            <span class="va-pass">{{ passCountOf(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="104" align="center">
          <template #default="scope">
            <span class="oa-pill" :class="'oa-pill--' + rowStatusMeta(scope.row).tone">
              {{ rowStatusMeta(scope.row).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" @click.stop="handleDetail(scope.row)">详情</el-button>
            <el-button
              v-hasPermi="['oa:volunteer:edit']"
              link
              type="primary"
              @click.stop="handleEdit(scope.row)"
            >编辑</el-button>
            <el-button
              v-hasPermi="['oa:volunteer:remove']"
              link
              type="danger"
              @click.stop="handleDelete(scope.row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="page.pageNum"
        v-model:limit="page.pageSize"
        :page-sizes="[10, 20, 50]"
        @pagination="getList"
      />
    </div>

    <!-- 新增 / 编辑（620px 抽屉） -->
    <activity-form ref="formRef" @success="handleSuccess" />
    <!-- 活动详情（含报名名单 / 审核 / 导出） -->
    <activity-detail ref="detailRef" @success="handleSuccess" @edit="handleDetailEdit" />
  </div>
</template>

<script setup name="VolunteerActivityIndex">
import {
  listActivity,
  deleteActivity,
  SHOW_STATUS_OPTIONS,
  showStatusMeta,
  formatHours,
  formatActivityTime,
  signupCountOf,
  passCountOf
} from '@/api/oa/volunteer'
import ActivityForm from './ActivityForm.vue'
import ActivityDetail from './Detail.vue'

const { proxy } = getCurrentInstance()

const formRef = ref()
const detailRef = ref()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const filters = reactive({
  showStatus: undefined,
  keyword: '',
  dateRange: []
})

const statusOptions = SHOW_STATUS_OPTIONS

/* -------------------------------------------------------------------------- */
/* 展示辅助                                                                   */
/* -------------------------------------------------------------------------- */
function rowStatusMeta(row) {
  return showStatusMeta(row && row.showStatus, row && row.showStatusLabel)
}

function capacityText(row) {
  const value = row && row.capacity
  if (value === undefined || value === null || value === '') return '—'
  return value + ' 人'
}

/* -------------------------------------------------------------------------- */
/* 列表查询                                                                   */
/* -------------------------------------------------------------------------- */
function buildQuery() {
  const params = {
    pageNum: page.pageNum,
    pageSize: page.pageSize
  }
  if (filters.showStatus !== undefined && filters.showStatus !== null && filters.showStatus !== '') {
    params.showStatus = filters.showStatus
  }
  const keyword = String(filters.keyword || '').trim()
  if (keyword) params.keyword = keyword
  if (filters.dateRange && filters.dateRange.length === 2) {
    params.beginTime = filters.dateRange[0]
    params.endTime = filters.dateRange[1]
  }
  return params
}

function getList() {
  loading.value = true
  listActivity(buildQuery())
    .then(response => {
      list.value = (response && response.rows) || []
      total.value = (response && response.total) || 0
    })
    .catch(() => {
      list.value = []
      total.value = 0
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  page.pageNum = 1
  getList()
}

function resetQuery() {
  filters.showStatus = undefined
  filters.keyword = ''
  filters.dateRange = []
  page.pageNum = 1
  getList()
}

/* -------------------------------------------------------------------------- */
/* 操作                                                                       */
/* -------------------------------------------------------------------------- */
function handleAdd() {
  formRef.value && formRef.value.open({ mode: 'add' })
}

function handleEdit(row) {
  formRef.value && formRef.value.open({ mode: 'edit', activityId: row.activityId })
}

function handleDetail(row) {
  detailRef.value && detailRef.value.open({ activityId: row.activityId })
}

function handleDetailEdit(payload) {
  const activityId = payload && payload.activityId
  if (activityId) {
    formRef.value && formRef.value.open({ mode: 'edit', activityId })
  }
}

function handleDelete(row) {
  const name = row.name || ''
  proxy.$modal
    .confirm('删除后活动与报名名单将从列表隐藏、不计入统计（软删除），确定删除「' + name + '」吗？')
    .then(() => deleteActivity(row.activityId))
    .then(() => {
      proxy.$modal.msgSuccess('已删除活动：' + name + '（软删除，统计已同步）')
      if (list.value.length === 1 && page.pageNum > 1) page.pageNum -= 1
      getList()
    })
    .catch(() => {})
}

/** 表单 / 详情里保存、审核、删除后刷新列表（保持当前筛选与页码） */
function handleSuccess() {
  getList()
}

/* -------------------------------------------------------------------------- */
/* 初始化                                                                     */
/* -------------------------------------------------------------------------- */
getList()
</script>

<style scoped lang="scss">
.va-page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.va-page-head__main {
  min-width: 0;
}

.va-page-desc {
  margin: -8px 0 0;
  max-width: 760px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-soft);
}

.va-list-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  font-size: 12px;

  b {
    color: var(--ink);
  }
}

.va-name {
  color: var(--ink);
  font-weight: 600;
}

.va-pass {
  color: var(--lime-text);
  font-weight: 600;
}

.va-table {
  width: 100%;
}
</style>
