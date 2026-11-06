<template>
  <div class="app-container employee-page">
    <div class="oa-page-title">
      员工档案
      <small v-if="listLoaded">共 {{ total }} 人 · 正常 {{ normalTotal === null ? '—' : normalTotal }} 人</small>
    </div>

    <div class="employee-source oa-muted">
      <el-icon><InfoFilled /></el-icon>
      <span>员工档案数据来自系统用户，oa_employee 档案表将于 P1 接入；工号取登录账号、部门取用户部门，入职日期 / 岗位 / 离职等档案字段暂无数据源，缺省列不展示或显示为 —。</span>
    </div>

    <div class="employee-layout">
      <!-- 左侧：组织架构 -->
      <div class="oa-card employee-side">
        <div class="oa-card-head">
          <h3>组织架构</h3>
          <span class="oa-link employee-side__all" @click="handleAllDept">全部</span>
        </div>
        <el-input v-model="deptKeyword" class="employee-side__search" placeholder="请输入部门名称" clearable prefix-icon="Search" />
        <div class="employee-side__tree">
          <el-tree
            ref="deptTreeRef"
            :data="deptOptions"
            :props="{ label: 'label', children: 'children' }"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            :filter-node-method="filterDeptNode"
            @node-click="handleDeptNodeClick"
          />
          <div v-if="deptLoaded && !deptOptions.length" class="oa-empty">暂无部门数据</div>
        </div>
      </div>

      <!-- 右侧：员工列表 -->
      <div class="oa-card employee-main">
        <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px" class="oa-filter-bar">
          <el-form-item label="姓名" prop="nickName">
            <el-input v-model="queryParams.nickName" placeholder="请输入姓名" clearable style="width: 180px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="手机号" prop="phonenumber">
            <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号" clearable style="width: 180px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
              <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="oa-toolbar">
          <div class="oa-toolbar__left">
            <el-tooltip content="P1 员工档案落地后开放" placement="top">
              <span class="employee-toolbar__disabled">
                <el-button type="primary" plain icon="Plus" disabled>新增员工</el-button>
              </span>
            </el-tooltip>
            <el-tooltip content="P1 员工档案落地后开放" placement="top">
              <span class="employee-toolbar__disabled">
                <el-button type="info" plain icon="Upload" disabled>导入</el-button>
              </span>
            </el-tooltip>
            <el-button type="warning" plain icon="Download" v-hasPermi="['system:user:export']" @click="handleExport">导出</el-button>
          </div>
          <right-toolbar v-model:showSearch="showSearch" :columns="columns" @queryTable="getList" />
        </div>

        <el-table v-loading="loading" :data="userList">
          <el-table-column label="员工" min-width="180">
            <template #default="scope">
              <div class="employee-name">
                <span class="employee-avatar" :class="'employee-avatar--' + avatarTone(scope.row)">{{ avatarText(scope.row) }}</span>
                <span class="oa-strong">{{ scope.row.nickName || '—' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="工号" min-width="130" key="userName" prop="userName" v-if="columns.userName.visible" :show-overflow-tooltip="true" />
          <el-table-column label="部门" min-width="150" key="deptName" v-if="columns.deptName.visible" :show-overflow-tooltip="true">
            <template #default="scope">
              <span>{{ deptName(scope.row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="手机号" min-width="130" key="phonenumber" prop="phonenumber" v-if="columns.phonenumber.visible" :show-overflow-tooltip="true" />
          <el-table-column label="邮箱" min-width="190" key="email" prop="email" v-if="columns.email.visible" :show-overflow-tooltip="true" />
          <el-table-column label="状态" width="90" align="center" key="status" v-if="columns.status.visible">
            <template #default="scope">
              <OaStatusTag :status="scope.row.status" :map="EMPLOYEE_STATUS_MAP" />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="165" align="center" key="createTime" v-if="columns.createTime.visible">
            <template #default="scope">
              <span>{{ parseTime(scope.row.createTime) || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-button link type="primary" v-hasPermi="['system:user:query']" @click="handleDetail(scope.row)">详情</el-button>
              <el-tooltip content="P1 员工档案落地后开放" placement="top">
                <span class="employee-op__disabled">
                  <el-button link type="primary" disabled>编辑</el-button>
                </span>
              </el-tooltip>
              <el-tooltip content="P1 员工档案落地后开放" placement="top">
                <span class="employee-op__disabled">
                  <el-button link type="danger" disabled>离职</el-button>
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </div>
    </div>

    <!-- 员工详情抽屉 -->
    <el-drawer v-model="detailVisible" title="员工详情" size="480px">
      <div v-loading="detailLoading" class="employee-detail">
        <template v-if="detail.userId">
          <div class="employee-detail__head">
            <span class="employee-avatar employee-avatar--lg" :class="'employee-avatar--' + avatarTone(detail)">{{ avatarText(detail) }}</span>
            <div class="employee-detail__identity">
              <div class="employee-detail__name">{{ detail.nickName || '—' }}</div>
              <div class="oa-muted">{{ detail.userName || '—' }}</div>
            </div>
          </div>
          <table class="oa-desc-table">
            <tbody>
              <tr>
                <th>登录账号</th>
                <td>{{ detail.userName || '—' }}</td>
              </tr>
              <tr>
                <th>姓名</th>
                <td>{{ detail.nickName || '—' }}</td>
              </tr>
              <tr>
                <th>部门</th>
                <td>{{ deptName(detail) }}</td>
              </tr>
              <tr>
                <th>手机号</th>
                <td>{{ detail.phonenumber || '—' }}</td>
              </tr>
              <tr>
                <th>邮箱</th>
                <td>{{ detail.email || '—' }}</td>
              </tr>
              <tr>
                <th>性别</th>
                <td>{{ sexText(detail.sex) }}</td>
              </tr>
              <tr>
                <th>状态</th>
                <td><OaStatusTag :status="detail.status" :map="EMPLOYEE_STATUS_MAP" /></td>
              </tr>
              <tr>
                <th>岗位</th>
                <td>{{ detail.postText || '—' }}</td>
              </tr>
              <tr>
                <th>角色</th>
                <td>{{ detail.roleText || '—' }}</td>
              </tr>
              <tr>
                <th>创建时间</th>
                <td>{{ parseTime(detail.createTime) || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </template>
        <div v-else-if="!detailLoading" class="oa-empty">暂无详情数据</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="Employee">
import { listUser, getUser, deptTreeSelect } from '@/api/system/user'
import OaStatusTag from '@/components/oa/OaStatusTag.vue'

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = useDict('sys_normal_disable')

/** 用户状态 → 药丸标签（0 正常 / 1 停用） */
const EMPLOYEE_STATUS_MAP = {
  '0': { label: '正常', tone: 'green' },
  '1': { label: '停用', tone: 'gray' }
}

/** sys_user.sex：0 男 1 女 2 未知 */
const SEX_MAP = {
  '0': '男',
  '1': '女',
  '2': '未知'
}

/** 头像渐变色池（按 userId 取模，仅影响观感） */
const AVATAR_TONES = ['blue', 'green', 'orange', 'gray', 'purple']

const userList = ref([])
const loading = ref(false)
const listLoaded = ref(false)
const total = ref(0)
// 正常状态人数：后端没有员工档案统计接口，用用户列表按 status=0 取 total
const normalTotal = ref(null)
const showSearch = ref(true)
const deptOptions = ref([])
const deptLoaded = ref(false)
const deptKeyword = ref('')
const deptTreeRef = ref()
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})

const columns = ref({
  userName: { label: '工号', visible: true },
  deptName: { label: '部门', visible: true },
  phonenumber: { label: '手机号', visible: true },
  email: { label: '邮箱', visible: true },
  status: { label: '状态', visible: true },
  createTime: { label: '创建时间', visible: true }
})

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  nickName: undefined,
  phonenumber: undefined,
  status: undefined,
  deptId: undefined
})

// 列表请求序号，避免快速切换部门时旧响应覆盖新响应
let listSeq = 0

/** 查询员工列表 */
function getList() {
  const seq = ++listSeq
  loading.value = true
  listUser(queryParams.value)
    .then(res => {
      if (seq !== listSeq) return
      userList.value = res.rows || []
      total.value = Number(res.total) || 0
      listLoaded.value = true
      loading.value = false
      loadNormalCount(seq)
    })
    .catch(() => {
      if (seq !== listSeq) return
      userList.value = []
      total.value = 0
      loading.value = false
    })
}

/**
 * 正常人数：同一筛选条件下按 status=0 再取一次 total
 * 显式忽略当前的状态筛选，避免"正常 N 人"随状态筛选变成 0
 */
function loadNormalCount(seq) {
  listUser({
    pageNum: 1,
    pageSize: 1,
    nickName: queryParams.value.nickName,
    phonenumber: queryParams.value.phonenumber,
    deptId: queryParams.value.deptId,
    status: '0'
  })
    .then(res => {
      if (seq !== listSeq) return
      normalTotal.value = Number(res.total) || 0
    })
    .catch(() => {
      if (seq !== listSeq) return
      normalTotal.value = null
    })
}

/** 查询部门树 */
function getDeptTree() {
  deptTreeSelect()
    .then(res => {
      deptOptions.value = res.data || []
    })
    .catch(() => {
      deptOptions.value = []
    })
    .finally(() => {
      deptLoaded.value = true
    })
}

/** 部门树节点过滤 */
function filterDeptNode(value, data) {
  if (!value) return true
  return String(data.label || '').indexOf(value) > -1
}

watch(deptKeyword, value => {
  if (deptTreeRef.value) deptTreeRef.value.filter(value)
})

/** 点击部门节点：根节点视为"全部" */
function handleDeptNodeClick(data) {
  const root = deptOptions.value[0]
  const isRoot = root && data && String(root.id) === String(data.id)
  queryParams.value.deptId = isRoot ? undefined : data.id
  handleQuery()
}

/** 组织架构「全部」：清空部门筛选 */
function handleAllDept() {
  queryParams.value.deptId = undefined
  if (deptTreeRef.value) deptTreeRef.value.setCurrentKey(null)
  handleQuery()
}

/** 搜索 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置 */
function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.deptId = undefined
  if (deptTreeRef.value) deptTreeRef.value.setCurrentKey(null)
  handleQuery()
}

/** 导出：复用系统用户导出接口 */
function handleExport() {
  proxy.download('system/user/export', { ...queryParams.value }, `employee_${Date.now()}.xlsx`)
}

/** 详情 */
function handleDetail(row) {
  detail.value = {}
  detailVisible.value = true
  detailLoading.value = true
  getUser(row.userId)
    .then(res => {
      const data = res.data || {}
      detail.value = {
        ...data,
        postText: resolvePostNames(data, res),
        roleText: resolveRoleNames(data, res)
      }
    })
    .catch(() => {
      proxy.$modal.msgError('获取员工详情失败')
    })
    .finally(() => {
      detailLoading.value = false
    })
}

/**
 * 岗位名拼接
 * sys_user 详情接口的真实契约：用户自己的岗位是顶层 postIds，岗位字典是顶层 posts；
 * data.posts 在部分版本才有，这里两种都兼容。
 */
function resolvePostNames(data, resp) {
  const direct = Array.isArray(data.posts) ? data.posts : []
  const directNames = direct.map(post => post && post.postName).filter(Boolean)
  if (directNames.length) return directNames.join('、')

  const ids = Array.isArray(data.postIds) ? data.postIds : Array.isArray(resp.postIds) ? resp.postIds : []
  const all = Array.isArray(resp.posts) ? resp.posts : []
  const names = ids
    .map(id => {
      const hit = all.find(post => String(post.postId) === String(id))
      return hit ? hit.postName : ''
    })
    .filter(Boolean)
  return names.length ? names.join('、') : '—'
}

/** 角色名拼接：详情 data.roles 优先，其次顶层 roles + roleIds */
function resolveRoleNames(data, resp) {
  const direct = Array.isArray(data.roles) ? data.roles : []
  const directNames = direct.map(role => role && role.roleName).filter(Boolean)
  if (directNames.length) return directNames.join('、')

  const ids = Array.isArray(data.roleIds) ? data.roleIds : Array.isArray(resp.roleIds) ? resp.roleIds : []
  const all = Array.isArray(resp.roles) ? resp.roles : []
  const names = ids
    .map(id => {
      const hit = all.find(role => String(role.roleId) === String(id))
      return hit ? hit.roleName : ''
    })
    .filter(Boolean)
  return names.length ? names.join('、') : '—'
}

/** 部门名 */
function deptName(row) {
  return (row && row.dept && row.dept.deptName) || '—'
}

/** 性别文案 */
function sexText(sex) {
  const key = sex === undefined || sex === null ? '' : String(sex)
  return SEX_MAP[key] || '—'
}

/** 头像文字：昵称/账号首字符 */
function avatarText(row) {
  const name = row ? row.nickName || row.userName : ''
  const first = Array.from(String(name || '').trim())[0]
  return first ? String(first).toUpperCase() : '员'
}

/** 头像色调 */
function avatarTone(row) {
  const id = Number(row && row.userId)
  if (!Number.isFinite(id)) return 'blue'
  return AVATAR_TONES[Math.abs(id) % AVATAR_TONES.length]
}

onMounted(() => {
  getDeptTree()
  getList()
})
</script>

<style lang="scss" scoped>
.employee-source {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: -4px 0 16px;
  line-height: 1.6;

  :deep(.el-icon) {
    margin-top: 2px;
    flex: 0 0 auto;
  }
}

.employee-layout {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

/* 两个卡片是左右并列，抵消全局 .oa-card + .oa-card 的纵向间距 */
.employee-layout > .oa-card + .oa-card {
  margin-top: 0;
}

.employee-side {
  flex: 0 0 240px;
  width: 240px;
  box-sizing: border-box;
}

.employee-side__all {
  font-size: 13px;
  flex: 0 0 auto;
}

.employee-side__search {
  margin-bottom: 10px;
}

.employee-side__tree {
  max-height: 560px;
  overflow: auto;

  :deep(.el-tree-node__content) {
    height: 32px;
    border-radius: 4px;
  }
}

.employee-main {
  flex: 1;
  min-width: 0;
}

.employee-toolbar__disabled {
  display: inline-flex;
}

.employee-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.employee-op__disabled {
  display: inline-flex;
  margin-left: 12px;
}

.employee-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #236de9, #5b8ff0);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  user-select: none;
  overflow: hidden;

  &--green {
    background: linear-gradient(135deg, #67c23a, #95d475);
  }

  &--orange {
    background: linear-gradient(135deg, #e6a23c, #f3d19e);
  }

  &--gray {
    background: linear-gradient(135deg, #909399, #c0c4cc);
  }

  &--purple {
    background: linear-gradient(135deg, #7c6cf0, #a79bff);
  }

  &--lg {
    flex: 0 0 48px;
    width: 48px;
    height: 48px;
    font-size: 18px;
  }
}

.employee-detail {
  min-height: 240px;
}

.employee-detail__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.employee-detail__identity {
  min-width: 0;
}

.employee-detail__name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}
</style>
