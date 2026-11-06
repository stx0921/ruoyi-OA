<template>
  <div class="app-container project-page">
    <div class="oa-page-title">
      项目管理
      <small>用项目圈住活动 / 阶段工作，结束后归档只读</small>
    </div>

    <div class="oa-card">
      <!-- 顶部筛选 -->
      <el-form ref="queryRef" :model="queryParams" :inline="true" class="oa-filter-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            placeholder="项目名称 / 编号"
            clearable
            style="width: 220px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="oa-toolbar">
        <div class="oa-toolbar__left">
          <el-button v-hasPermi="['oa:project:add']" type="primary" plain icon="Plus" @click="handleAdd">新建项目</el-button>
        </div>
        <div class="oa-toolbar__right">
          <span class="oa-muted">共 {{ total }} 个项目</span>
        </div>
      </div>

      <!-- 加载骨架 -->
      <div v-if="loading && !projectList.length" class="proj-grid">
        <div v-for="index in 3" :key="index" class="proj-skeleton">
          <el-skeleton :rows="3" animated />
        </div>
      </div>

      <!-- 错误态 -->
      <div v-else-if="error" class="state-block">
        <el-icon :size="34"><WarningFilled /></el-icon>
        <p>项目列表加载失败</p>
        <el-button size="small" type="primary" @click="getList">重新加载</el-button>
      </div>

      <!-- 空态 -->
      <div v-else-if="!projectList.length" class="state-block">
        <el-icon :size="34"><FolderOpened /></el-icon>
        <p>{{ hasFilter ? '没有匹配的项目' : '还没有项目' }}</p>
        <el-button v-if="hasFilter" size="small" @click="resetQuery">重置筛选</el-button>
        <el-button v-hasPermi="['oa:project:add']" size="small" type="primary" @click="handleAdd">新建项目</el-button>
      </div>

      <!-- 项目卡网格 -->
      <div v-else class="proj-grid">
        <div
          v-for="project in projectList"
          :key="project.projectId"
          class="proj-card"
          :class="{ 'is-archived': String(project.status) === '1', 'is-highlight': highlightId === project.projectId }"
          @click="openDetail(project)"
        >
          <div class="pc-head">
            <div class="pc-head-main">
              <div class="pc-name" :title="project.name">{{ project.name }}</div>
              <div class="pc-no">{{ project.projectNo || '—' }}</div>
            </div>
            <OaTaskStatusTag type="project" :status="project.status" size="small" />
          </div>
          <div class="pc-meta">
            <div>负责人：<b>{{ project.ownerName || '—' }}</b></div>
            <div>起止：{{ formatDate(project.startDate) }} ~ {{ formatDate(project.endDate) }}</div>
          </div>
          <div class="pc-progress">
            <i :style="{ width: projectProgress(project) + '%' }" />
          </div>
          <div class="pc-counts">
            <span>任务 {{ project.taskCount || 0 }} · 已完成 {{ project.doneCount || 0 }}</span>
            <span class="pc-counts-right">
              <span>参与 {{ project.memberCount || 0 }} 人</span>
              <el-button
                v-if="canRemove"
                v-hasPermi="['oa:project:remove']"
                link
                type="danger"
                size="small"
                @click.stop="handleDeleteProject(project)"
              >删除</el-button>
            </span>
          </div>
        </div>
      </div>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :page-sizes="[12, 24, 48]"
        @pagination="getList"
      />
    </div>

    <!-- 项目详情抽屉 -->
    <el-drawer v-model="detailVisible" size="620px" append-to-body :close-on-click-modal="false">
      <template #header>
        <div class="detail-head">
          <span class="detail-no">{{ detail.projectNo || '—' }}</span>
          <span class="detail-name">{{ detail.name || '项目详情' }}</span>
          <OaTaskStatusTag type="project" :status="detail.status" size="small" />
        </div>
      </template>

      <div v-loading="detailLoading" class="detail-body">
        <div v-if="detail.status === '1'" class="readonly-banner">
          <el-icon><Lock /></el-icon>
          项目已结束，项目与任务只读
        </div>

        <div class="detail-section">
          <div class="section-title">属性</div>
          <div class="attr-grid">
            <div class="attr-item">
              <span class="attr-label">项目编号</span>
              <span class="attr-value">{{ detail.projectNo || '—' }}</span>
            </div>
            <div class="attr-item">
              <span class="attr-label">项目名称</span>
              <span class="attr-value">{{ detail.name || '—' }}</span>
            </div>
            <div class="attr-item">
              <span class="attr-label">负责人</span>
              <span class="attr-value">{{ detail.ownerName || '—' }}</span>
            </div>
            <div class="attr-item">
              <span class="attr-label">状态</span>
              <span class="attr-value"><OaTaskStatusTag type="project" :status="detail.status" size="small" /></span>
            </div>
            <div class="attr-item">
              <span class="attr-label">开始日期</span>
              <span class="attr-value">{{ formatDate(detail.startDate) }}</span>
            </div>
            <div class="attr-item">
              <span class="attr-label">结束日期</span>
              <span class="attr-value">{{ formatDate(detail.endDate) }}</span>
            </div>
            <div class="attr-item">
              <span class="attr-label">任务统计</span>
              <span class="attr-value">共 {{ detail.taskCount || 0 }} · 已完成 {{ detail.doneCount || 0 }}</span>
            </div>
            <div class="attr-item">
              <span class="attr-label">参与人数</span>
              <span class="attr-value">{{ (detail.members || []).length }} 人</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">项目描述</div>
          <div v-if="detail.description" class="desc-text">{{ detail.description }}</div>
          <div v-else class="oa-muted">暂无描述</div>
        </div>

        <div class="detail-section">
          <div class="section-title">
            参与成员
            <small>{{ (detail.members || []).length }} 人</small>
          </div>
          <div v-if="(detail.members || []).length" class="member-chips">
            <span v-for="member in detail.members" :key="member.userId" class="member-chip">
              <span class="avatar-xs" :style="{ background: avatarColor(member.userId) }">{{ nameInitial(member.userName) }}</span>
              <span>{{ member.userName }}</span>
              <span class="chip-dept">{{ member.deptName || '' }}</span>
              <span v-if="String(member.userId) === String(detail.ownerId)" class="owner-tag">负责人</span>
            </span>
          </div>
          <div v-else class="oa-muted">暂无参与成员</div>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button size="small" @click="viewTasks(detail)">查看任务</el-button>
          <el-button v-if="String(detail.status) === '0'" v-hasPermi="['oa:project:edit']" size="small" @click="handleEdit(detail)">编辑</el-button>
          <el-button
            v-if="String(detail.status) === '0'"
            v-hasPermi="['oa:project:edit']"
            size="small"
            type="danger"
            plain
            @click="handleFinish(detail)"
          >结束项目</el-button>
          <span v-if="canRemove" class="footer-divider" />
          <el-button
            v-if="canRemove"
            v-hasPermi="['oa:project:remove']"
            size="small"
            type="danger"
            plain
            :loading="deleting"
            @click="handleDeleteProject(detail)"
          >删除项目</el-button>
          <el-button size="small" @click="detailVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 新建 / 编辑项目抽屉 -->
    <project-form-drawer ref="formDrawerRef" @success="handleFormSuccess" @deleted="handleFormDeleted" />
  </div>
</template>

<script setup name="ProjectList">
import { ElMessageBox } from 'element-plus'
import OaTaskStatusTag from '@/components/oa/OaTaskStatusTag.vue'
import ProjectFormDrawer from './ProjectFormDrawer.vue'
import { deleteProject, finishProject, getProject, listProject } from '@/api/oa/project'
import { avatarColor, formatDate, nameInitial, resolveOptions } from '@/utils/oaTask'
import { useDict } from '@/utils/dict'

const { proxy } = getCurrentInstance()
const router = useRouter()

/** 兜底选项（字典 oa_project_status 优先） */
const PROJECT_STATUS_OPTIONS = [
  { value: '0', label: '进行中' },
  { value: '1', label: '已结束' }
]

const { oa_project_status } = useDict('oa_project_status')
const statusOptions = computed(() => resolveOptions(oa_project_status.value, PROJECT_STATUS_OPTIONS))

const loading = ref(false)
const error = ref(false)
const projectList = ref([])
const total = ref(0)
const queryRef = ref()
const formDrawerRef = ref()
const highlightId = ref(undefined)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})
const deleting = ref(false)

/** 是否具备删除项目权限（契约追加需求 §9） */
const canRemove = computed(() => proxy.$auth.hasPermi('oa:project:remove'))

const queryParams = ref({
  pageNum: 1,
  pageSize: 12,
  keyword: undefined,
  status: undefined
})

const hasFilter = computed(() => !!(queryParams.value.keyword || queryParams.value.status))

function projectProgress(project) {
  const taskCount = Number(project.taskCount) || 0
  const doneCount = Number(project.doneCount) || 0
  if (!taskCount) return 0
  return Math.min(100, Math.round((doneCount / taskCount) * 100))
}

function getList() {
  loading.value = true
  error.value = false
  listProject(queryParams.value)
    .then(response => {
      projectList.value = response.rows || []
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
  queryParams.value.keyword = undefined
  queryParams.value.status = undefined
  queryParams.value.pageNum = 1
  getList()
}

function handleAdd() {
  formDrawerRef.value.open({ mode: 'add' })
}

/** 编辑：先拉详情（含参与成员），避免用列表行数据保存时把成员覆盖成只剩负责人 */
async function handleEdit(project) {
  if (!project || !project.projectId) return
  try {
    const response = await getProject(project.projectId)
    formDrawerRef.value.open({ mode: 'edit', data: response.data || project })
  } catch (e) {
    proxy.$modal.msgError('项目详情加载失败，无法编辑')
  }
}

function handleFormSuccess(result) {
  getList()
  if (result && result.mode === 'add' && result.projectId) {
    highlightId.value = result.projectId
    setTimeout(() => {
      highlightId.value = undefined
    }, 1000)
  }
  if (detailVisible.value && detail.value.projectId) {
    loadDetail(detail.value.projectId)
  }
}

function openDetail(project) {
  if (!project || !project.projectId) return
  detail.value = { ...project, members: [] }
  detailVisible.value = true
  loadDetail(project.projectId)
}

function loadDetail(projectId) {
  detailLoading.value = true
  getProject(projectId)
    .then(response => {
      detail.value = response.data || {}
    })
    .catch(() => {})
    .finally(() => {
      detailLoading.value = false
    })
}

function viewTasks(project) {
  if (!project || !project.projectId) return
  router.push({ path: '/task/list', query: { projectId: project.projectId } })
}

/** 结束项目：未完成 > 0 时先提示，确认后带 force=true 重试 */
async function handleFinish(project) {
  const unfinished = Math.max(0, Number(project.taskCount || 0) - Number(project.doneCount || 0))
  if (unfinished > 0) {
    try {
      await ElMessageBox.confirm(
        '仍有 ' + unfinished + ' 个未完成任务，结束后项目归档只读，任务不可再流转',
        '结束项目',
        { type: 'warning', confirmButtonText: '确认结束', cancelButtonText: '取消' }
      )
    } catch (e) {
      return
    }
    callFinish(project.projectId, true)
  } else {
    try {
      await ElMessageBox.confirm('结束后项目归档只读，其下任务不可再流转，确认结束？', '结束项目', {
        type: 'warning',
        confirmButtonText: '确认结束',
        cancelButtonText: '取消'
      })
    } catch (e) {
      return
    }
    callFinish(project.projectId, false)
  }
}

function callFinish(projectId, force) {
  finishProject({ projectId, force })
    .then(() => {
      proxy.$modal.msgSuccess('项目已结束，项目与任务只读')
      detailVisible.value = false
      getList()
    })
    .catch(() => {
      // 409（存在未完成任务）等：二次确认后带 force=true 重试
      if (force) return
      ElMessageBox.confirm(
        '项目仍有未完成任务，结束后项目归档只读，任务不可再流转。是否继续结束？',
        '结束项目',
        { type: 'warning', confirmButtonText: '继续结束', cancelButtonText: '取消' }
      )
        .then(() => {
          finishProject({ projectId, force: true }).then(() => {
            proxy.$modal.msgSuccess('项目已结束，项目与任务只读')
            detailVisible.value = false
            getList()
          }).catch(() => {})
        })
        .catch(() => {})
    })
}

onMounted(() => {
  getList()
})

/** 删除项目（契约追加需求 §9，硬删除不可恢复；项目下仍有任务/规则时后端 409 并提示 msg） */
function handleDeleteProject(project) {
  if (!project || !project.projectId || deleting.value) return
  ElMessageBox.confirm(
    '删除项目「' + (project.name || '') + '」？项目下若仍有任务或周期规则将无法删除，请先删除。删除后不可恢复。',
    '删除项目',
    {
      type: 'error',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    }
  )
    .then(async () => {
      deleting.value = true
      try {
        await deleteProject(project.projectId)
        proxy.$modal.msgSuccess('删除成功')
        detailVisible.value = false
        getList()
      } catch (e) {
        // 409 等失败原因由 request 拦截器统一提示（保留当前页面）
      } finally {
        deleting.value = false
      }
    })
    .catch(() => {})
}

/** 编辑抽屉内删除成功后刷新列表并关闭详情 */
function handleFormDeleted() {
  detailVisible.value = false
  getList()
}
</script>

<style scoped lang="scss">
.project-page {
  .oa-filter-bar {
    :deep(.el-form-item) {
      margin-bottom: 0;
    }
  }

  .proj-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }

  .proj-skeleton {
    padding: 20px;
    background: #fff;
    border-radius: 6px;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
  }

  .proj-card {
    display: flex;
    flex-direction: column;
    gap: 10px;
    min-height: 180px;
    padding: 16px;
    background: #fff;
    border: 1px solid transparent;
    border-radius: 6px;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
    cursor: pointer;
    transition: border-color 0.15s, box-shadow 0.15s;

    &:hover {
      border-color: #b3d8ff;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    &.is-archived {
      background: #fafafa;
      opacity: 0.6;
    }

    &.is-highlight {
      border-color: #236de9;
      box-shadow: 0 0 0 3px rgba(35, 109, 233, 0.25);
    }
  }

  .pc-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
  }

  .pc-head-main {
    min-width: 0;
  }

  .pc-name {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    color: #303133;
    font-size: 15px;
    font-weight: 600;
    line-height: 1.5;
    word-break: break-all;
  }

  .pc-no {
    margin-top: 3px;
    color: #909399;
    font-size: 12px;
    font-family: Consolas, Monaco, monospace;
  }

  .pc-meta {
    color: #606266;
    font-size: 12px;
    line-height: 1.95;

    b {
      color: #303133;
      font-weight: 500;
    }
  }

  .pc-progress {
    height: 6px;
    border-radius: 3px;
    background: #f0f2f5;
    overflow: hidden;

    i {
      display: block;
      height: 100%;
      border-radius: 3px;
      background: linear-gradient(90deg, #5b8ff0, #236de9);
    }
  }

  .pc-counts {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-top: auto;
    color: #909399;
    font-size: 12px;

    .pc-counts-right {
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
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

  .detail-head {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;

    .detail-no {
      color: #909399;
      font-size: 13px;
      font-family: Consolas, Monaco, monospace;
    }

    .detail-name {
      color: #303133;
      font-size: 16px;
      font-weight: 600;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .readonly-banner {
    display: flex;
    align-items: center;
    gap: 6px;
    height: 36px;
    padding: 0 14px;
    margin: -20px -20px 16px;
    background: #f4f4f5;
    border-bottom: 1px solid #ebeef5;
    color: #909399;
    font-size: 13px;
  }

  .detail-section {
    margin-bottom: 22px;
  }

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #f0f2f5;
    color: #303133;
    font-size: 14px;
    font-weight: 600;

    &::before {
      content: '';
      width: 3px;
      height: 13px;
      border-radius: 2px;
      background: #236de9;
    }

    small {
      color: #909399;
      font-size: 12px;
      font-weight: 400;
    }
  }

  .attr-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px 16px;
  }

  .attr-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;

    .attr-label {
      color: #909399;
      font-size: 12px;
    }

    .attr-value {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
      color: #303133;
      font-size: 13px;
      word-break: break-all;
    }
  }

  .desc-text {
    color: #606266;
    font-size: 13px;
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .member-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .member-chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 4px 10px;
    background: #f7f8fa;
    border-radius: 14px;
    color: #303133;
    font-size: 13px;

    .chip-dept {
      color: #909399;
      font-size: 12px;
    }

    .owner-tag {
      padding: 0 4px;
      border-radius: 2px;
      background: #edf3fe;
      color: #1d61d5;
      font-size: 11px;
    }
  }

  .avatar-xs {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    color: #fff;
    font-size: 10px;
    flex: 0 0 20px;
  }

  .drawer-footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 10px;

    .footer-divider {
      width: 1px;
      height: 20px;
      margin: 0 2px;
      background: #dcdfe6;
    }
  }
}

@media (max-width: 1200px) {
  .project-page .proj-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
