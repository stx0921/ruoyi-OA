<template>
  <el-drawer
    v-model="visible"
    size="620px"
    append-to-body
    :close-on-click-modal="false"
  >
    <template #header>
      <div class="oa-drawer-head">
        <span class="oa-strong">编辑部门 · {{ dept.deptName || '—' }}</span>
      </div>
    </template>

    <div class="oa-dept-drawer">
      <div class="oa-dept-drawer__notice">
        负责人变更后，该账号将获得本部门审批与数据权限
      </div>

      <!-- 只读部门信息 -->
      <section class="oa-section">
        <h4 class="oa-section__title">
          部门信息
          <small>基础信息取自 sys_dept，本页只维护负责人映射</small>
        </h4>
        <div class="oa-kv">
          <div class="oa-kv__item">
            <span class="oa-kv__label">部门名称</span>
            <div class="oa-kv__value">{{ dept.deptName || '—' }}</div>
          </div>
          <div class="oa-kv__item">
            <span class="oa-kv__label">部门编码</span>
            <div class="oa-kv__value">{{ dept.deptId }}</div>
          </div>
          <div class="oa-kv__item">
            <span class="oa-kv__label">排序</span>
            <div class="oa-kv__value">{{ dept.orderNum === null || dept.orderNum === undefined ? '—' : dept.orderNum }}</div>
          </div>
          <div class="oa-kv__item">
            <span class="oa-kv__label">状态</span>
            <div class="oa-kv__value">{{ statusText }}</div>
          </div>
          <div class="oa-kv__item">
            <span class="oa-kv__label">在籍人数</span>
            <div class="oa-kv__value">{{ dept.memberCount === null || dept.memberCount === undefined ? 0 : dept.memberCount }} 人（只读，由成员档案汇总）</div>
          </div>
        </div>
      </section>

      <!-- 负责人 -->
      <section class="oa-section">
        <h4 class="oa-section__title">
          负责人
          <small>候选为本部门在籍成员，可搜索 / 可清空</small>
        </h4>
        <el-form label-width="88px">
          <el-form-item label="正部长">
            <el-select
              v-model="leaderUserId"
              filterable
              remote
              clearable
              :remote-method="remoteSearch"
              :loading="loadingCandidates"
              placeholder="请选择本部门在籍成员"
              style="width: 100%"
            >
              <el-option
                v-for="member in candidates"
                :key="'leader-' + member.userId"
                :label="optionLabel(member)"
                :value="member.userId"
                :disabled="!member.userId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="副部长">
            <el-select
              v-model="viceUserIds"
              multiple
              filterable
              remote
              clearable
              collapse-tags
              collapse-tags-tooltip
              :remote-method="remoteSearch"
              :loading="loadingCandidates"
              placeholder="可多选，可清空"
              style="width: 100%"
            >
              <el-option
                v-for="member in candidates"
                :key="'vice-' + member.userId"
                :label="optionLabel(member)"
                :value="member.userId"
                :disabled="!member.userId"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <p class="oa-dept-drawer__tip">
          部长缺位时，该部门入会申请自动升级至主席团 / 管理员代审，代审记录写入审批时间轴。
        </p>
      </section>
    </div>

    <template #footer>
      <div class="oa-drawer-foot">
        <span class="oa-drawer-foot__spacer"></span>
        <el-button @click="visible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!canEdit" @click="handleSave">保 存</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup name="DeptLeaderDrawer">
import { updateDeptLeader } from '@/api/oa/dept'
import { listMember } from '@/api/oa/member'
import { positionText } from '@/utils/oaMember'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // DeptVO 行数据
  dept: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const { proxy } = getCurrentInstance()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const canEdit = computed(() => proxy.$auth.hasPermi('oa:dept:edit'))
const statusText = computed(() => (String(props.dept.status) === '1' ? '停用' : '正常'))

const leaderUserId = ref(null)
const viceUserIds = ref([])
const fetchedCandidates = ref([])
const memberCache = ref({})
const loadingCandidates = ref(false)
const saving = ref(false)

/**
 * 下拉候选：搜索结果 + 当前已选（保证回显「姓名（学号·职务）」而不是 userId）
 */
const candidates = computed(() => {
  const map = new Map()
  fetchedCandidates.value.forEach(member => map.set(member.userId, member))
  const selectedIds = [leaderUserId.value, ...viceUserIds.value]
    .filter(Boolean)
    .map(Number)
  selectedIds.forEach(userId => {
    const member = memberCache.value[userId]
    if (member && !map.has(userId)) map.set(userId, member)
  })
  return [...map.values()]
})

let searchTimer = null

/** 统一成员展示结构：{ userId, name, studentNo, memberNo, positionLabel } */
function normalizeMember(member) {
  return {
    userId: member.userId,
    name: member.name || member.nickName || '—',
    studentNo: member.studentNo || '—',
    memberNo: member.memberNo || '',
    positionLabel: member.positionLabel || positionText(member.position)
  }
}

/** 下拉展示「姓名（学号·职务）」 */
function optionLabel(member) {
  if (!member) return '—'
  return `${member.name}（${member.studentNo || '—'}·${member.positionLabel || '—'}）`
}

/** 本部门在籍成员候选（可搜索：纯数字按学号，其他按姓名） */
function loadCandidates(keyword) {
  if (!canEdit.value) return
  const query = {
    pageNum: 1,
    pageSize: 50,
    deptId: props.dept.deptId,
    status: '0'
  }
  const text = (keyword || '').trim()
  if (text) {
    if (/^\d+$/.test(text)) query.studentNo = text
    else query.name = text
  }
  loadingCandidates.value = true
  listMember(query)
    .then(res => {
      const rows = res.rows || []
      const map = { ...memberCache.value }
      const list = []
      rows.forEach(row => {
        if (!row.userId) return
        const member = normalizeMember(row)
        map[member.userId] = member
        list.push(member)
      })
      memberCache.value = map
      fetchedCandidates.value = list
    })
    .catch(() => {
      fetchedCandidates.value = []
    })
    .finally(() => {
      loadingCandidates.value = false
    })
}

/** 搜索 300ms 防抖 */
function remoteSearch(keyword) {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadCandidates(keyword), 300)
}

function init() {
  const dept = props.dept || {}
  leaderUserId.value = dept.leaderUserId ? Number(dept.leaderUserId) : null
  viceUserIds.value = (dept.viceLeaders || []).map(item => Number(item.userId)).filter(Boolean)
  fetchedCandidates.value = []
  memberCache.value = {}

  if (dept.leaderUserId) {
    memberCache.value[dept.leaderUserId] = {
      userId: Number(dept.leaderUserId),
      name: dept.leaderName || '用户' + dept.leaderUserId,
      studentNo: '—',
      memberNo: '',
      positionLabel: '部长'
    }
  }
  ;(dept.viceLeaders || []).forEach(item => {
    if (!item.userId) return
    memberCache.value[item.userId] = {
      userId: Number(item.userId),
      name: item.userName || '用户' + item.userId,
      studentNo: '—',
      memberNo: '',
      positionLabel: '副部长'
    }
  })

  loadCandidates('')
}

watch(
  () => props.modelValue,
  value => {
    if (value) init()
  }
)

/** 负责人是否有变更 */
const changed = computed(() => {
  const dept = props.dept || {}
  const oldLeader = dept.leaderUserId ? Number(dept.leaderUserId) : null
  const newLeader = leaderUserId.value ? Number(leaderUserId.value) : null
  if (oldLeader !== newLeader) return true
  const oldVice = (dept.viceLeaders || []).map(item => Number(item.userId)).sort((a, b) => a - b)
  const newVice = [...viceUserIds.value].map(Number).sort((a, b) => a - b)
  return oldVice.join(',') !== newVice.join(',')
})

function memberNameOf(userId) {
  const member = memberCache.value[userId]
  return member ? member.name : '该成员'
}

/** 保存二次确认文案：清空 / 变更正部长 / 变更副部长 */
function confirmText() {
  const dept = props.dept || {}
  const oldLeader = dept.leaderUserId ? Number(dept.leaderUserId) : null
  const newLeader = leaderUserId.value ? Number(leaderUserId.value) : null
  if (!newLeader && oldLeader) {
    return '清空后该部门申请将由主席团 / 管理员代审，确认？'
  }
  if (newLeader && newLeader !== oldLeader) {
    const member = memberCache.value[newLeader]
    let label = '该成员'
    if (member) {
      const code = member.memberNo || (member.studentNo && member.studentNo !== '—' ? member.studentNo : '')
      label = code ? `${member.name}（${code}）` : member.name
    }
    return `确认将 ${dept.deptName} 正部长设为 ${label}？`
  }
  const names = viceUserIds.value.map(id => memberNameOf(Number(id))).filter(Boolean).join('、')
  return `确认更新 ${dept.deptName} 副部长为 ${names || '未设置'}？`
}

function buildPayload() {
  const leaders = []
  if (leaderUserId.value) {
    leaders.push({ userId: Number(leaderUserId.value), leaderType: '1', sort: 1 })
  }
  viceUserIds.value.forEach((userId, index) => {
    leaders.push({ userId: Number(userId), leaderType: '2', sort: index + 1 })
  })
  return { deptId: props.dept.deptId, leaders }
}

function handleSave() {
  if (!canEdit.value) {
    proxy.$modal.msgWarning('没有维护部门负责人的权限')
    return
  }
  if (!changed.value) {
    proxy.$modal.msgWarning('负责人未变更')
    return
  }
  const duplicated = viceUserIds.value.some(id => Number(id) === Number(leaderUserId.value))
  if (leaderUserId.value && duplicated) {
    proxy.$modal.msgWarning('副部长不能重复选择')
    return
  }
  proxy.$modal
    .confirm(confirmText())
    .then(() => {
      saving.value = true
      return updateDeptLeader(buildPayload())
    })
    .then(() => {
      proxy.$modal.msgSuccess(`已更新 ${props.dept.deptName} 负责人，权限已生效`)
      emit('saved')
      visible.value = false
    })
    .catch(() => {})
    .finally(() => {
      saving.value = false
    })
}
</script>

<style scoped lang="scss">
.oa-drawer-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
}

.oa-dept-drawer__notice {
  padding: 8px 12px;
  margin-bottom: 18px;
  border-radius: 4px;
  background: #f4f8ff;
  border: 1px solid #d9ecff;
  font-size: 12px;
  line-height: 1.7;
  color: #606266;
}

.oa-section {
  margin-bottom: 22px;
}

.oa-section__title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;

  small {
    font-size: 12px;
    font-weight: 400;
    color: #909399;
  }
}

.oa-kv {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.oa-kv__label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: #909399;
}

.oa-kv__value {
  padding: 6px 10px;
  border-radius: 4px;
  background: #f5f7fa;
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
  word-break: break-word;
}

.oa-dept-drawer__tip {
  margin: 4px 0 0;
  padding-left: 88px;
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

.oa-drawer-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.oa-drawer-foot__spacer {
  flex: 1;
}
</style>
