<template>
  <el-drawer
    v-model="visible"
    :title="mode === 'edit' ? '编辑项目' : '新建项目'"
    size="620px"
    append-to-body
    :close-on-click-modal="false"
    @closed="reset"
  >
    <div v-loading="pageLoading" class="project-form">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px" label-position="top">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit placeholder="请输入项目名称（2–50 个字符）" />
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="项目负责人" prop="ownerId">
              <el-select
                v-model="form.ownerId"
                placeholder="搜索姓名 / 学号"
                filterable
                remote
                clearable
                :remote-method="searchOwner"
                :loading="ownerLoading"
                style="width: 100%"
                @change="handleOwnerChange"
              >
                <el-option v-for="item in ownerOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-input :model-value="mode === 'edit' ? '进行中（仅可结束，不可回退）' : '进行中（创建后默认）'" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择结束日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="项目描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
            placeholder="项目目标、范围、备注等（≤2000 字）"
          />
        </el-form-item>

        <el-form-item label="参与成员">
          <div class="member-field">
            <div class="member-chips">
              <span v-for="member in members" :key="member.userId" class="member-chip">
                <span class="avatar-xs" :style="{ background: avatarColor(member.userId) }">{{ nameInitial(member.userName) }}</span>
                {{ member.userName }}
                <span class="chip-dept">{{ member.deptName || '' }}</span>
                <el-icon v-if="String(member.userId) !== String(form.ownerId)" class="chip-close" @click="removeMember(member.userId)">
                  <Close />
                </el-icon>
              </span>
              <span v-if="!members.length" class="oa-muted">尚未选择参与成员</span>
            </div>
            <el-button link type="primary" icon="Plus" @click="openMemberDialog">选择参与成员</el-button>
            <div class="oa-muted">可跨部门选择；负责人保存时自动加入成员列表</div>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-button
          v-if="mode === 'edit' && canRemove"
          v-hasPermi="['oa:project:remove']"
          type="danger"
          plain
          :loading="deleting"
          class="delete-btn"
          @click="handleDelete"
        >删除项目</el-button>
        <span class="footer-spacer" />
        <el-button :disabled="saving" @click="visible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
      </div>
    </template>

    <project-member-dialog ref="memberDialogRef" @confirm="handleMemberConfirm" />
  </el-drawer>
</template>

<script setup name="ProjectFormDrawer">
import ProjectMemberDialog from './ProjectMemberDialog.vue'
import { ElMessageBox } from 'element-plus'
import { addProject, deleteProject, getProject, updateProject } from '@/api/oa/project'
import { avatarColor, nameInitial, searchMemberOptions, toDateString } from '@/utils/oaTask'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['success', 'deleted'])

const visible = ref(false)
const saving = ref(false)
const deleting = ref(false)
/** 是否具备删除项目权限（契约追加需求 §9） */
const canRemove = computed(() => proxy.$auth.hasPermi('oa:project:remove'))
const pageLoading = ref(false)
const mode = ref('add')
const projectId = ref(undefined)
const formRef = ref()
const memberDialogRef = ref()

const ownerOptions = ref([])
const ownerLoading = ref(false)
const members = ref([])

const form = ref(createForm())

const rules = {
  name: [
    { required: true, message: '请填写项目名称', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        const text = String(value || '').trim()
        if (!text) return callback(new Error('请填写项目名称'))
        if (text.length < 2 || text.length > 50) return callback(new Error('项目名称需为 2–50 个字符'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  ownerId: [{ required: true, message: '请选择项目负责人', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [
    { required: true, message: '请选择结束日期', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (form.value.startDate && value && value < form.value.startDate) {
          return callback(new Error('结束日期不能早于开始日期'))
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

function createForm() {
  return {
    name: '',
    ownerId: undefined,
    startDate: undefined,
    endDate: undefined,
    description: ''
  }
}

async function open(options = {}) {
  const opts = options || {}
  mode.value = opts.mode || 'add'
  reset()
  visible.value = true
  if (mode.value === 'edit' && opts.data) {
    const data = opts.data
    projectId.value = data.projectId
    form.value.name = data.name || ''
    form.value.ownerId = data.ownerId || undefined
    form.value.startDate = toDateString(data.startDate)
    form.value.endDate = toDateString(data.endDate)
    form.value.description = data.description || ''
    ownerOptions.value = memberOptionsFrom([{ userId: data.ownerId, userName: data.ownerName, deptName: data.deptName }])
    members.value = normalizeMembers(data.members)
    // 详情接口未返回 members 时补负责人，保证编辑保存不丢成员
    if (!members.value.length && data.ownerId) {
      members.value = [{ userId: data.ownerId, userName: data.ownerName, deptName: data.deptName || '' }]
    }
    if (opts.refreshDetail) {
      pageLoading.value = true
      try {
        const response = await getProject(data.projectId)
        const detail = response.data || {}
        members.value = normalizeMembers(detail.members)
        if (!form.value.description) form.value.description = detail.description || ''
      } catch (e) {
        // 详情拉取失败时保留列表数据
      } finally {
        pageLoading.value = false
      }
    }
  } else {
    // 默认开始日期为今天
    form.value.startDate = parseToday()
  }
  nextTick(() => formRef.value && formRef.value.clearValidate())
}

function parseToday() {
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return date.getFullYear() + '-' + month + '-' + day
}

function normalizeMembers(list) {
  return (list || []).map(item => ({
    userId: item.userId,
    userName: item.userName || item.nickName || '用户' + item.userId,
    deptName: item.deptName || ''
  }))
}

function memberOptionsFrom(list) {
  return (list || [])
    .filter(item => item.userId)
    .map(item => ({
      value: item.userId,
      label: (item.userName || item.nickName || '用户' + item.userId) + '（' + (item.deptName || '—') + '）',
      name: item.userName,
      deptName: item.deptName || ''
    }))
}

function searchOwner(keyword) {
  ownerLoading.value = true
  searchMemberOptions(keyword).then(options => {
    ownerOptions.value = options
    ownerLoading.value = false
  })
}

/** 负责人自动加入参与成员 */
function handleOwnerChange(value) {
  if (!value) return
  if (!members.value.some(item => String(item.userId) === String(value))) {
    const hit = ownerOptions.value.find(item => String(item.value) === String(value))
    members.value.push({
      userId: value,
      userName: hit ? hit.name : '负责人',
      deptName: hit ? hit.deptName : ''
    })
  }
}

function openMemberDialog() {
  memberDialogRef.value.open(members.value)
}

function handleMemberConfirm(list) {
  members.value = normalizeMembers(list)
  // 保留负责人
  if (form.value.ownerId) handleOwnerChange(form.value.ownerId)
}

function removeMember(userId) {
  if (String(userId) === String(form.value.ownerId)) {
    proxy.$modal.msgWarning('负责人不能从参与成员中移除')
    return
  }
  members.value = members.value.filter(item => String(item.userId) !== String(userId))
}

function reset() {
  projectId.value = undefined
  form.value = createForm()
  members.value = []
  ownerOptions.value = []
  if (formRef.value) formRef.value.clearValidate()
}

function buildPayload() {
  const memberIds = members.value.map(item => item.userId)
  if (form.value.ownerId && !memberIds.some(id => String(id) === String(form.value.ownerId))) {
    memberIds.push(form.value.ownerId)
  }
  const payload = {
    name: String(form.value.name || '').trim(),
    ownerId: form.value.ownerId,
    startDate: form.value.startDate,
    endDate: form.value.endDate,
    description: form.value.description || '',
    memberIds
  }
  if (projectId.value) payload.projectId = projectId.value
  return payload
}

async function handleSave() {
  if (saving.value) return
  let valid = false
  try {
    valid = await formRef.value.validate()
  } catch (e) {
    valid = false
  }
  if (!valid) return

  saving.value = true
  try {
    if (mode.value === 'edit') {
      await updateProject(buildPayload())
      proxy.$modal.msgSuccess('项目已保存')
      visible.value = false
      emit('success', { projectId: projectId.value, mode: 'edit' })
    } else {
      const response = await addProject(buildPayload())
      const data = response.data || {}
      proxy.$modal.msgSuccess('项目已创建' + (data.projectNo ? '（' + data.projectNo + '）' : ''))
      visible.value = false
      emit('success', { projectId: data.projectId, projectNo: data.projectNo, mode: 'add' })
    }
  } catch (e) {
    // 接口异常已由 request 拦截器统一提示
  } finally {
    saving.value = false
  }
}

/** 删除项目（契约追加需求 §9，硬删除不可恢复；有任务/规则时后端 409 提示） */
function handleDelete() {
  if (!projectId.value || deleting.value) return
  ElMessageBox.confirm(
    '删除项目「' + (form.value.name || '') + '」？项目下若仍有任务或周期规则将无法删除，请先删除。删除后不可恢复。',
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
        await deleteProject(projectId.value)
        proxy.$modal.msgSuccess('删除成功')
        visible.value = false
        emit('deleted')
      } catch (e) {
        // 409 等失败原因由 request 拦截器统一提示（保留抽屉）
      } finally {
        deleting.value = false
      }
    })
    .catch(() => {})
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.project-form {
  padding-bottom: 8px;

  :deep(.el-form-item) {
    margin-bottom: 16px;
  }
}

.member-field {
  width: 100%;
}

.member-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

.member-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #f7f8fa;
  border-radius: 14px;
  font-size: 13px;
  color: #303133;

  .chip-dept {
    color: #909399;
    font-size: 12px;
  }

  .chip-close {
    color: #c0c4cc;
    cursor: pointer;

    &:hover {
      color: #f56c6c;
    }
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

  .delete-btn {
    margin-right: auto;
  }

  .footer-spacer {
    display: none;
  }
}
</style>
