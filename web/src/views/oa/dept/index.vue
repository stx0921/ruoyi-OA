<template>
  <div class="app-container">
    <div class="oa-page-title">
      部门与负责人 <small>{{ deptList.length }} 个部门 · 在籍 {{ totalMembers }} 人</small>
    </div>

    <!-- 无权限态：部长不渲染菜单，直链访问给提示 -->
    <div v-if="!canView" class="oa-card">
      <div class="oa-load-error">
        <el-icon><Lock /></el-icon>
        <p>仅管理员 / 主席团可维护部门负责人</p>
      </div>
    </div>

    <template v-else>
      <div class="oa-card">
        <!-- 部门维护入口：组织架构独立维护，不再依赖系统管理 -->
        <div class="oa-card-head">
          <h3>内设部门</h3>
          <el-button type="primary" icon="Plus" v-hasPermi="['oa:dept:add']" @click="openCreate">新增部门</el-button>
        </div>

        <!-- 筛选：部门名称 / 状态 -->
        <el-form :model="filterForm" :inline="true" class="oa-filter-bar" @submit.prevent>
          <el-form-item label="部门名称">
            <el-input
              v-model="filterForm.deptName"
              placeholder="请输入部门名称"
              clearable
              style="width: 220px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
              <el-option label="正常" value="0" />
              <el-option label="停用" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 错误态 -->
        <div v-if="listError" class="oa-load-error">
          <el-icon><WarningFilled /></el-icon>
          <p>部门列表加载失败</p>
          <el-button type="primary" plain @click="getList">重新加载</el-button>
        </div>

        <!-- 列表 -->
        <el-table v-else v-loading="loading" :data="filteredDepts" class="oa-dept-table">
          <el-table-column label="部门名称" width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="oa-strong">{{ row.deptName || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="部门编码" width="120" align="center">
            <template #default="{ row }">
              <span class="oa-muted">{{ row.deptId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="在籍人数" width="90" align="center">
            <template #default="{ row }">{{ row.memberCount === null || row.memberCount === undefined ? 0 : row.memberCount }}</template>
          </el-table-column>
          <el-table-column label="正部长" width="120" align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.leaderName">{{ row.leaderName }}</span>
              <span v-else class="oa-muted">未设置</span>
            </template>
          </el-table-column>
          <el-table-column label="副部长" width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.viceLeaders && row.viceLeaders.length">{{ viceNames(row) }}</span>
              <span v-else class="oa-muted">未设置</span>
            </template>
          </el-table-column>
          <el-table-column label="排序" width="80" align="center">
            <template #default="{ row }">{{ row.orderNum === null || row.orderNum === undefined ? '—' : row.orderNum }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <span class="oa-pill" :class="String(row.status) === '0' ? 'oa-pill--green' : 'oa-pill--gray'">
                {{ String(row.status) === '0' ? '正常' : '停用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDeptForm(row)" v-hasPermi="['oa:dept:edit']">编辑</el-button>
              <el-button link type="primary" @click="openLeader(row)" v-hasPermi="['oa:dept:edit']">负责人</el-button>
              <el-button link type="primary" @click="viewMembers(row)">查看成员</el-button>
              <el-button link type="danger" @click="handleDelete(row)" v-hasPermi="['oa:dept:remove']">删除</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <div class="oa-dept-empty">
              <p class="oa-dept-empty__title">暂无内设部门</p>
              <p class="oa-dept-empty__desc">点击上方「新增部门」创建组织部、宣传部等内设部门</p>
            </div>
          </template>
        </el-table>
      </div>
    </template>

    <!-- 部门负责人编辑抽屉 -->
    <dept-leader-drawer v-model="drawerVisible" :dept="currentDept" @saved="getList" />
    <!-- 部门新增 / 编辑弹窗 -->
    <dept-form-dialog v-model="formVisible" :dept="formDept" @saved="getList" />
  </div>
</template>

<script setup name="OaDept">
import { delDept, listDept } from '@/api/oa/dept'
import DeptLeaderDrawer from './DeptLeaderDrawer.vue'
import DeptFormDialog from './DeptFormDialog.vue'

const { proxy } = getCurrentInstance()
const router = useRouter()

const loading = ref(false)
const listError = ref(false)
const deptList = ref([])

const filterForm = ref({ deptName: '', status: '' })
const appliedFilter = ref({ deptName: '', status: '' })

const drawerVisible = ref(false)
const currentDept = ref({})

const formVisible = ref(false)
const formDept = ref(null)

const canView = computed(() => proxy.$auth.hasPermi('oa:dept:list'))

const totalMembers = computed(() =>
  deptList.value.reduce((sum, item) => sum + (Number(item.memberCount) || 0), 0)
)

/** 部门按 sort 升序；筛选走本地（/oa/dept/list 无查询参数，返回全量部门） */
const filteredDepts = computed(() => {
  const keyword = (appliedFilter.value.deptName || '').trim()
  const status = appliedFilter.value.status
  return deptList.value.filter(item => {
    if (keyword && String(item.deptName || '').indexOf(keyword) < 0) return false
    if (status !== '' && status !== undefined && status !== null && String(item.status) !== String(status)) return false
    return true
  })
})

function viceNames(row) {
  return (row.viceLeaders || []).map(item => item.userName).filter(Boolean).join('、')
}

function getList() {
  if (!canView.value) return
  loading.value = true
  listError.value = false
  listDept()
    .then(res => {
      const rows = res.data || res.rows || []
      deptList.value = rows
        .map(item => ({ ...item }))
        .sort((a, b) => (Number(a.orderNum) || 0) - (Number(b.orderNum) || 0))
    })
    .catch(() => {
      deptList.value = []
      listError.value = true
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  appliedFilter.value = { ...filterForm.value }
}

function resetQuery() {
  filterForm.value = { deptName: '', status: '' }
  appliedFilter.value = { deptName: '', status: '' }
}

/** 打开「新增部门」 */
function openCreate() {
  formDept.value = null
  formVisible.value = true
}

/** 打开「编辑部门」（名称 / 排序 / 状态） */
function openDeptForm(row) {
  formDept.value = { ...row }
  formVisible.value = true
}

/** 打开部门负责人维护抽屉 */
function openLeader(row) {
  currentDept.value = row
  drawerVisible.value = true
}

/** 删除部门（后端会校验下级部门 / 在籍成员 / 系统用户） */
function handleDelete(row) {
  proxy.$modal
    .confirm('确认删除部门「' + (row.deptName || '') + '」？删除后不可恢复')
    .then(() => delDept(row.deptId))
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
    })
    .catch(() => {})
}

function viewMembers(row) {
  // 菜单实际 path 为 hr（SQL），优先 /hr/member，兼容旧文档 /oa/member
  const candidates = ['/hr/member', '/oa/member']
  const target = candidates.find(path => router.resolve(path).matched.length > 0)
  router.push({ path: target || candidates[0], query: { deptId: row.deptId } })
}

onMounted(() => {
  if (canView.value) getList()
})
</script>

<style scoped lang="scss">
.oa-dept-table {
  :deep(.el-table__header th) {
    background: #fafafa;
    color: #606266;
    font-weight: 600;
  }

  :deep(.el-table__row) {
    height: 48px;
  }
}

.oa-dept-empty {
  padding: 40px 0;
  text-align: center;
}

.oa-dept-empty__title {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.oa-dept-empty__desc {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
}

.oa-load-error {
  padding: 48px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;

  .el-icon {
    font-size: 32px;
    color: #e6a23c;
  }

  p {
    margin: 10px 0 16px;
  }
}
</style>
