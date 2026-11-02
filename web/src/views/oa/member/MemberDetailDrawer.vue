<template>
  <el-drawer
    v-model="visible"
    size="620px"
    append-to-body
    :before-close="handleBeforeClose"
    class="member-drawer"
    @closed="handleClosed"
  >
    <template #header>
      <div class="member-drawer__head">
        <span class="member-drawer__name">{{ member.name || '成员详情' }}</span>
        <span v-if="member.memberNo" class="oa-muted">{{ member.memberNo }}</span>
        <span v-if="member.status !== undefined" class="oa-pill" :class="'oa-pill--' + statusTone">{{ statusText }}</span>
      </div>
    </template>

    <!-- loading 骨架 -->
    <div v-if="loading" class="member-drawer-skeleton">
      <span class="member-skeleton__bar" style="width: 36%; height: 24px"></span>
      <span class="member-skeleton__bar" style="width: 60%"></span>
      <div v-for="index in 4" :key="index" class="member-drawer-skeleton__group">
        <span class="member-skeleton__bar" style="width: 22%; height: 18px"></span>
        <div class="member-drawer-skeleton__grid">
          <span v-for="cell in 6" :key="cell" class="member-skeleton__bar" style="width: 100%; height: 32px"></span>
        </div>
      </div>
    </div>

    <!-- 错误态 -->
    <div v-else-if="loadError" class="member-drawer-error">
      <div class="member-drawer-error__title">成员详情加载失败</div>
      <div class="oa-muted">若为跨部门访问，请确认当前账号的数据权限</div>
      <div class="member-drawer-error__actions">
        <el-button type="primary" plain @click="loadDetail">重试</el-button>
        <el-button @click="closeWithoutConfirm">关 闭</el-button>
      </div>
    </div>

    <!-- 无权限态 -->
    <div v-else-if="noPermission || permissionDenied" class="member-drawer-error">
      <div class="member-drawer-error__title">没有访问该成员的权限</div>
      <div class="oa-muted">跨部门访问会被后端拦截，请联系管理员</div>
      <div class="member-drawer-error__actions">
        <el-button @click="closeWithoutConfirm">关 闭</el-button>
      </div>
    </div>

    <template v-else>
      <!-- 操作条：按角色与状态渲染，超过 3 个折叠进「更多」 -->
      <div v-if="canEdit" class="member-detail-actions">
        <el-button v-if="canQuit" type="danger" plain size="small" @click="handleStatusAction('quit')">退会</el-button>
        <el-button v-if="canGraduate" size="small" @click="handleStatusAction('graduate')">毕业</el-button>
        <el-button v-if="canRestore" type="success" plain size="small" @click="handleStatusAction('restore')">恢复在籍</el-button>
        <el-dropdown v-if="hasMoreActions" trigger="click" @command="handleMoreCommand">
          <el-button size="small">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="canTransfer" command="transfer">调部门</el-dropdown-item>
              <el-dropdown-item v-if="canTransfer" command="promote">升职</el-dropdown-item>
              <el-dropdown-item v-if="canManageAccount && member.userId" command="resetPwd" divided>重置密码</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px" class="member-form">
        <!-- 基础身份 -->
        <div class="member-form-section">
          <div class="member-form-section__title">基础身份</div>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="姓名" prop="name">
                <el-input v-model="form.name" maxlength="20" placeholder="请输入姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="性别" prop="gender">
                <el-radio-group v-model="form.gender">
                  <el-radio value="0">男</el-radio>
                  <el-radio value="1">女</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="学号" prop="studentNo">
                <el-input v-model="form.studentNo" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号" prop="phone">
                <div v-if="phoneEditing" class="member-phone">
                  <el-input v-model="form.phone" maxlength="11" placeholder="请输入 11 位手机号" />
                  <el-button link type="primary" @click="cancelPhoneEdit">取消</el-button>
                </div>
                <div v-else class="member-phone">
                  <oa-sensitive-text :value="member.phone" :masked="isMaskedValue(member.phone)" copyable />
                  <el-button v-if="canEdit" link type="primary" @click="startPhoneEdit">变更手机号</el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="成员编号">
                <el-input :model-value="member.memberNo || '—'" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="来源">
                <el-input :model-value="sourceText(member.source)" disabled />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 院系信息 -->
        <div class="member-form-section">
          <div class="member-form-section__title">院系信息</div>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="学院" prop="college">
                <el-input v-model="form.college" maxlength="100" placeholder="请填写学院" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="专业" prop="major">
                <el-input v-model="form.major" maxlength="100" placeholder="请填写专业" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="班级" prop="className">
                <el-input v-model="form.className" maxlength="50" placeholder="请填写班级" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="年级" prop="grade">
                <el-select v-model="form.grade" placeholder="请选择年级" style="width: 100%">
                  <el-option v-for="item in gradeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 组织信息 -->
        <div class="member-form-section">
          <div class="member-form-section__title">组织信息</div>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="部门" prop="deptId">
                <el-select
                  v-model="form.deptId"
                  placeholder="请选择部门"
                  filterable
                  :disabled="!canTransfer"
                  style="width: 100%"
                >
                  <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="职务" prop="position">
                <el-select v-model="form.position" placeholder="请选择职务" :disabled="!canTransfer" style="width: 100%">
                  <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="届次" prop="cohort">
                <el-select v-model="form.cohort" placeholder="请选择届次" style="width: 100%">
                  <el-option v-for="item in cohortOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="入会日期" prop="joinDate">
                <el-date-picker
                  v-model="form.joinDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择入会日期"
                  :disabled-date="disabledFutureDate"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态">
                <el-input :model-value="statusText" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="离会日期">
                <el-input :model-value="member.leaveDate ? formatMemberDate(member.leaveDate) : '—'" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="2"
                  maxlength="500"
                  show-word-limit
                  placeholder="选填"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 联系方式 -->
        <div class="member-form-section">
          <div class="member-form-section__title">联系方式</div>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" maxlength="100" placeholder="选填" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="微信" prop="wechat">
                <el-input v-model="form.wechat" maxlength="50" placeholder="选填" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="QQ" prop="qq">
                <el-input v-model="form.qq" maxlength="12" placeholder="选填" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 招新信息 -->
        <div class="member-form-section">
          <div class="member-form-section__title">招新信息</div>
          <el-form-item label="服从调剂">
            <el-switch v-model="form.obeyAdjust" active-value="1" inactive-value="0" />
          </el-form-item>
          <el-form-item label="特长" prop="skills">
            <el-input v-model="form.skills" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
          </el-form-item>
          <el-form-item label="自我介绍" prop="intro">
            <el-input v-model="form.intro" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="选填，最多 500 字" />
          </el-form-item>
        </div>

        <!-- 附件 -->
        <div class="member-form-section">
          <div class="member-form-section__title">附件</div>
          <div class="member-att-list">
            <div class="member-att-block">
              <el-upload
                :show-file-list="false"
                :http-request="options => handleUpload(options, 'photo')"
                :before-upload="file => beforeUpload(file, 'photo')"
                accept=".jpg,.jpeg,.png"
                :disabled="!canEdit"
              >
                <div class="member-att-thumb">
                  <img v-if="photoPreview" :src="photoPreview" class="member-att-img" alt="照片" />
                  <div v-else class="member-att-placeholder">
                    <el-icon :size="20"><Picture /></el-icon>
                    <span>{{ attachments.photoFileId ? '照片' : '上传照片' }}</span>
                  </div>
                </div>
              </el-upload>
              <div class="member-att-meta">
                <span class="oa-strong">照片</span>
                <span class="oa-muted">jpg / png，≤2MB</span>
                <el-button v-if="attachments.photoFileId" link type="primary" @click="downloadAttachment('photo')">下载</el-button>
                <span v-else class="oa-muted">未上传</span>
              </div>
            </div>

            <div class="member-att-block">
              <el-upload
                :show-file-list="false"
                :http-request="options => handleUpload(options, 'card')"
                :before-upload="file => beforeUpload(file, 'card')"
                accept=".jpg,.jpeg,.png,.pdf"
                :disabled="!canEdit"
              >
                <div class="member-att-thumb">
                  <img v-if="cardPreview" :src="cardPreview" class="member-att-img" alt="学生证" />
                  <div v-else class="member-att-placeholder">
                    <el-icon :size="20"><Document /></el-icon>
                    <span>{{ attachments.cardFileId ? '学生证' : '上传学生证' }}</span>
                  </div>
                </div>
              </el-upload>
              <div class="member-att-meta">
                <span class="oa-strong">学生证</span>
                <span class="oa-muted">jpg / png / pdf，≤5MB</span>
                <el-button v-if="attachments.cardFileId" link type="primary" @click="downloadAttachment('card')">下载</el-button>
                <span v-else class="oa-muted">未上传</span>
              </div>
            </div>
          </div>
          <div class="oa-muted member-att-tip">附件只提供鉴权下载，不暴露裸 URL（R12）</div>
        </div>

        <!-- 账号信息（仅管理员） -->
        <div v-if="canManageAccount" class="member-form-section">
          <div class="member-form-section__title">账号信息</div>
          <div class="member-account-box">
            <span class="oa-muted">账号</span>
            <b>{{ member.userId ? member.studentNo : '未开通' }}</b>
            <span class="oa-muted">状态</span>
            <el-switch
              v-if="member.userId"
              v-model="accountActive"
              :loading="accountSwitching"
              inline-prompt
              active-text="启用"
              inactive-text="停用"
              @change="handleAccountToggle"
            />
            <span v-else class="oa-muted">—</span>
            <span class="oa-muted">最后登录</span>
            <span>{{ member.lastLoginTime ? formatMemberTime(member.lastLoginTime) : '从未登录' }}</span>
            <span class="member-account-box__grow"></span>
            <el-button v-if="member.userId" size="small" @click="handleResetPwd">重置密码</el-button>
          </div>
          <div class="oa-muted member-account-box__tip">初始密码为手机号后 6 位，首次登录请修改密码。</div>
        </div>

        <!-- 变动记录 -->
        <div class="member-form-section">
          <div class="member-form-section__title">变动记录</div>
          <div class="member-log-timeline">
            <el-timeline v-if="memberLogs.length">
              <el-timeline-item
                v-for="log in memberLogs"
                :key="log.logId"
                :timestamp="formatMemberTime(log.createTime)"
                placement="top"
                :type="timelineType(log.action)"
              >
                <div class="member-log-item__title">
                  <span>{{ actionLabel(log.action) }}</span>
                  <span class="oa-pill" :class="'oa-pill--' + actionTone(log.action)">{{ log.operatorName || '系统' }}</span>
                </div>
                <div class="member-log-item__meta">{{ formatMemberTime(log.createTime) }}<span v-if="log.ip"> · IP {{ log.ip }}</span></div>
                <div class="member-log-item__summary">{{ memberLogSummary(log) }}</div>
              </el-timeline-item>
            </el-timeline>
            <div v-else class="oa-empty">暂无变动记录</div>
          </div>
        </div>
      </el-form>
    </template>

    <template #footer>
      <div class="member-drawer__foot">
        <el-button link type="primary" @click="goLogs">查看全部日志</el-button>
        <span class="member-drawer__foot-grow"></span>
        <el-button :disabled="saving" @click="handleBeforeClose()">取 消</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="loading || loadError || noPermission || permissionDenied"
          @click="handleSave"
        >保 存</el-button>
      </div>
    </template>

    <!-- 调部门 / 升职（760px） -->
    <el-dialog
      v-model="transferVisible"
      :title="transferType === 'promote' ? '升职' : '调部门'"
      width="760px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form label-width="88px">
        <el-form-item label="成员">
          <el-input :model-value="member.name" disabled />
        </el-form-item>
        <el-form-item v-if="transferType !== 'promote'" label="目标部门">
          <el-select v-model="transferForm.deptId" placeholder="请选择部门" filterable style="width: 100%">
            <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="transferType !== 'transfer'" label="目标职务">
          <el-select v-model="transferForm.position" placeholder="请选择职务" style="width: 100%">
            <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="transferForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填，写入操作日志" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取 消</el-button>
        <el-button type="primary" :loading="transferSubmitting" @click="submitTransfer">确 定</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup name="MemberDetailDrawer">
import { ElMessageBox } from 'element-plus'
import { saveAs } from 'file-saver'
import { getMember, updateMember, changeMemberStatus, transferMember, uploadOaFile, getOaFileBlob, deptTreeSelect } from '@/api/oa/member'
import { resetUserPwd, changeUserStatus } from '@/api/system/user'
import {
  POSITION_OPTIONS,
  GRADE_OPTIONS,
  COHORT_OPTIONS,
  memberStatusText,
  memberStatusTone,
  sourceText,
  formatMemberDate,
  formatMemberTime,
  isMaskedValue,
  actionLabel,
  actionTone,
  memberLogSummary,
  resolveOptions,
  flattenDeptOptions
} from '@/utils/oaMember'
import { blobValidate, parseTime } from '@/utils/ruoyi'
import { oaTimestamp } from '@/utils/oa'
import OaSensitiveText from '@/components/oa/OaSensitiveText.vue'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()
const emit = defineEmits(['success'])
const { oa_grade, oa_cohort, oa_position } = useDict('oa_grade', 'oa_cohort', 'oa_position')

const visible = ref(false)
const loading = ref(false)
const loadError = ref(false)
const permissionDenied = ref(false)
const saving = ref(false)
const memberId = ref(undefined)
const member = ref({})
const formRef = ref()
const phoneEditing = ref(false)
const pendingAction = ref('')
const deptOptions = ref([])

const noPermission = computed(() => !proxy.$auth.hasPermi('oa:member:query'))
const canEdit = computed(() => proxy.$auth.hasPermi('oa:member:edit'))
const canTransfer = computed(() => {
  const admin = userStore.roles.indexOf('admin') > -1
  const president = userStore.roles.indexOf('oa_president') > -1
  return (admin || president) && canEdit.value
})
const canManageAccount = computed(() => {
  const admin = userStore.roles.indexOf('admin') > -1
  return admin && proxy.$auth.hasPermi('system:user:edit')
})

const statusText = computed(() => memberStatusText(member.value.status))
const statusTone = computed(() => memberStatusTone(member.value.status))
const memberLogs = computed(() => {
  const list = (member.value.memberLogs || []).slice()
  return list.sort((a, b) => oaTimestamp(b.createTime) - oaTimestamp(a.createTime))
})
const canQuit = computed(() => String(member.value.status) === '0' && canEdit.value)
const canGraduate = computed(() => String(member.value.status) === '0' && canEdit.value)
const canRestore = computed(() => String(member.value.status) !== '0' && member.value.status !== undefined && canEdit.value)
const hasMoreActions = computed(() => canTransfer.value || (canManageAccount.value && !!member.value.userId))

const gradeOptions = computed(() => resolveOptions(oa_grade.value, GRADE_OPTIONS))
const positionOptions = computed(() => resolveOptions(oa_position.value, POSITION_OPTIONS))
const cohortOptions = computed(() => resolveOptions(oa_cohort.value, COHORT_OPTIONS))

const form = ref(createForm())
const originalSnapshot = ref('')
const attachments = reactive({ photoFileId: null, photoUrl: '', cardFileId: null, cardUrl: '' })
const photoPreview = ref('')
const cardPreview = ref('')
const accountActive = ref(true)
const accountSwitching = ref(false)

/* ------------------------------ 校验 ------------------------------ */
const rules = {
  name: [
    { required: true, message: '请填写姓名', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5A-Za-z·]{2,20}$/, message: '姓名需为 2–20 个中英文字符', trigger: 'blur' }
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  studentNo: [
    { required: true, message: '请填写学号', trigger: 'blur' },
    { pattern: /^\d{8,12}$/, message: '学号需为 8–12 位数字', trigger: 'blur' }
  ],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  college: [{ required: true, message: '请填写学院', trigger: 'blur' }],
  major: [{ required: true, message: '请填写专业', trigger: 'blur' }],
  className: [{ required: true, message: '请填写班级', trigger: 'blur' }],
  grade: [{ required: true, message: '请选择年级', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  position: [{ required: true, message: '请选择职务', trigger: 'change' }],
  cohort: [{ required: true, message: '请选择届次', trigger: 'change' }],
  joinDate: [
    { required: true, message: '请选择入会日期', trigger: 'change' },
    { validator: validateJoinDate, trigger: 'change' }
  ],
  email: [{ pattern: /^[\w.-]+@[\w-]+(\.[\w-]+)+$/, message: '请输入正确的邮箱地址', trigger: 'blur' }],
  wechat: [
    {
      pattern: /^[a-zA-Z][-_a-zA-Z0-9]{5,19}$/,
      message: '微信号需字母开头，6–20 位字母 / 数字 / 下划线 / 减号',
      trigger: 'blur'
    }
  ],
  qq: [{ pattern: /^\d{5,12}$/, message: 'QQ 号需为 5–12 位数字', trigger: 'blur' }],
  skills: [{ max: 500, message: '特长最多 500 字', trigger: 'blur' }],
  intro: [{ max: 500, message: '自我介绍最多 500 字', trigger: 'blur' }],
  remark: [{ max: 500, message: '备注最多 500 字', trigger: 'blur' }]
}

function validateJoinDate(rule, value, callback) {
  if (!value) return callback()
  const today = parseTime(new Date(), '{y}-{m}-{d}')
  if (String(value) > today) return callback(new Error('入会日期不能晚于今天'))
  callback()
}

/** 手机号：未展开「变更手机号」时不校验（展示的是后端返回值） */
function validatePhone(rule, value, callback) {
  if (!phoneEditing.value) return callback()
  if (!value) return callback(new Error('请填写手机号'))
  if (!/^1[3-9]\d{9}$/.test(String(value))) return callback(new Error('请输入 11 位手机号'))
  callback()
}

function disabledFutureDate(date) {
  return date.getTime() > Date.now()
}

/* ------------------------------ 数据 ------------------------------ */
function createForm() {
  return {
    name: '',
    gender: '0',
    studentNo: '',
    phone: '',
    college: '',
    major: '',
    className: '',
    grade: undefined,
    deptId: undefined,
    position: '5',
    cohort: undefined,
    joinDate: parseTime(new Date(), '{y}-{m}-{d}'),
    email: '',
    wechat: '',
    qq: '',
    skills: '',
    intro: '',
    obeyAdjust: '0',
    remark: ''
  }
}

function fillForm(data) {
  const source = data || {}
  form.value = {
    name: source.name || '',
    gender: source.gender === undefined || source.gender === null ? '0' : String(source.gender),
    studentNo: source.studentNo || '',
    phone: source.phone || '',
    college: source.college || '',
    major: source.major || '',
    className: source.className || '',
    grade: source.grade === undefined || source.grade === null ? undefined : String(source.grade),
    deptId: source.deptId,
    position: source.position === undefined || source.position === null ? '5' : String(source.position),
    cohort: source.cohort === undefined || source.cohort === null ? undefined : String(source.cohort),
    joinDate: source.joinDate || parseTime(new Date(), '{y}-{m}-{d}'),
    email: source.email || '',
    wechat: source.wechat || '',
    qq: source.qq || '',
    skills: source.skills || '',
    intro: source.intro || '',
    obeyAdjust: source.obeyAdjust === undefined || source.obeyAdjust === null ? '0' : String(source.obeyAdjust),
    remark: source.remark || ''
  }
  originalSnapshot.value = formSnapshot()
}

function formSnapshot() {
  return JSON.stringify({
    form: form.value,
    photoFileId: attachments.photoFileId,
    cardFileId: attachments.cardFileId
  })
}

const dirty = computed(() => {
  if (loading.value || loadError.value || !member.value.memberId) return false
  return formSnapshot() !== originalSnapshot.value
})

/* ------------------------------ 打开 / 关闭 ------------------------------ */
function open(options) {
  const opts = options || {}
  resetState()
  memberId.value = opts.memberId
  pendingAction.value = opts.action || ''
  visible.value = true
  loadDeptOptions()
  loadDetail()
}

function resetState() {
  member.value = {}
  memberId.value = undefined
  loading.value = false
  loadError.value = false
  permissionDenied.value = false
  saving.value = false
  phoneEditing.value = false
  pendingAction.value = ''
  attachments.photoFileId = null
  attachments.photoUrl = ''
  attachments.cardFileId = null
  attachments.cardUrl = ''
  revokePreview('photo')
  revokePreview('card')
  form.value = createForm()
  originalSnapshot.value = ''
  accountActive.value = true
}

async function loadDetail() {
  if (!memberId.value) return
  if (noPermission.value) {
    loading.value = false
    loadError.value = false
    return
  }
  loading.value = true
  loadError.value = false
  permissionDenied.value = false
  try {
    const response = await getMember(memberId.value)
    member.value = response.data || {}
    attachments.photoFileId = member.value.photoFileId || null
    attachments.photoUrl = member.value.photoUrl || ''
    attachments.cardFileId = member.value.cardFileId || null
    attachments.cardUrl = member.value.cardUrl || ''
    fillForm(member.value)
    accountActive.value = String(member.value.accountStatus) === '0'
    phoneEditing.value = false
    await nextTick()
    if (formRef.value) formRef.value.clearValidate()
    loadAttachmentPreview('photo')
    loadAttachmentPreview('card')
    if (pendingAction.value) {
      const action = pendingAction.value
      pendingAction.value = ''
      nextTick(() => triggerAction(action))
    }
  } catch (e) {
    // request 拦截器对 403 / 非 200 业务码统一 reject('error')，按「无访问权限」处理
    if (e === 'error') {
      permissionDenied.value = true
    } else {
      loadError.value = true
    }
    member.value = {}
  } finally {
    loading.value = false
  }
}

function triggerAction(action) {
  if (action === 'transfer' || action === 'promote') {
    openTransfer(action)
    return
  }
  handleStatusAction(action)
}

/** 有未保存修改时二次确认 */
function handleBeforeClose(done) {
  if (saving.value) return
  const close = typeof done === 'function' ? done : () => (visible.value = false)
  if (!dirty.value) {
    close()
    return
  }
  ElMessageBox.confirm('有未保存的修改，确认放弃？', '提示', {
    confirmButtonText: '放弃修改',
    cancelButtonText: '继 续 编 辑',
    type: 'warning'
  })
    .then(close)
    .catch(() => {})
}

function closeWithoutConfirm() {
  visible.value = false
}

function handleClosed() {
  revokePreview('photo')
  revokePreview('card')
  resetState()
}

/* ------------------------------ 手机号 ------------------------------ */
function startPhoneEdit() {
  phoneEditing.value = true
  form.value.phone = ''
  nextTick(() => formRef.value && formRef.value.clearValidate('phone'))
}

function cancelPhoneEdit() {
  phoneEditing.value = false
  form.value.phone = member.value.phone || ''
  nextTick(() => formRef.value && formRef.value.clearValidate('phone'))
}

/* ------------------------------ 保存 ------------------------------ */
function buildPayload() {
  return {
    memberId: memberId.value,
    name: form.value.name,
    gender: form.value.gender,
    studentNo: form.value.studentNo,
    phone: form.value.phone,
    college: form.value.college,
    major: form.value.major,
    className: form.value.className,
    grade: form.value.grade,
    cohort: form.value.cohort,
    deptId: form.value.deptId,
    position: form.value.position,
    joinDate: form.value.joinDate,
    email: form.value.email || '',
    wechat: form.value.wechat || '',
    qq: form.value.qq || '',
    skills: form.value.skills || '',
    intro: form.value.intro || '',
    obeyAdjust: form.value.obeyAdjust,
    photoFileId: attachments.photoFileId || null,
    cardFileId: attachments.cardFileId || null,
    remark: form.value.remark || ''
  }
}

async function handleSave() {
  if (saving.value) return
  if (!dirty.value) {
    proxy.$modal.msgWarning('未检测到修改')
    return
  }
  let valid = false
  try {
    valid = await formRef.value.validate()
  } catch (e) {
    valid = false
  }
  if (!valid) return

  saving.value = true
  try {
    await updateMember(buildPayload())
    proxy.$modal.msgSuccess('已保存')
    emit('success')
    visible.value = false
  } catch (e) {
    // 保存失败保留输入、不关闭抽屉；原因由 request 拦截器提示
  } finally {
    saving.value = false
  }
}

/* ------------------------------ 状态 / 调部门 / 升职 ------------------------------ */
function handleStatusAction(type) {
  const configs = {
    quit: {
      status: '1',
      message: '确认将 ' + member.value.name + ' 设为 已退会？账号将停用，数据保留',
      success: '已设为 已退会，账号已停用，数据保留'
    },
    graduate: {
      status: '2',
      message: '确认将 ' + member.value.name + ' 设为 已毕业？账号将停用，数据保留',
      success: '已设为 已毕业，账号已停用，数据保留'
    },
    restore: {
      status: '0',
      message: '确认将 ' + member.value.name + ' 恢复为 在籍？账号将重新启用',
      success: '已恢复在籍，账号已重新启用'
    }
  }
  const config = configs[type]
  if (!config) return
  proxy.$modal
    .confirm(config.message)
    .then(() =>
      changeMemberStatus({
        memberId: memberId.value,
        status: config.status,
        leaveDate: config.status === '0' ? null : parseTime(new Date(), '{y}-{m}-{d}'),
        remark: ''
      })
    )
    .then(() => {
      proxy.$modal.msgSuccess(config.success)
      emit('success')
      loadDetail()
    })
    .catch(() => {})
}

const transferVisible = ref(false)
const transferSubmitting = ref(false)
const transferType = ref('transfer')
const transferForm = reactive({ deptId: undefined, position: undefined, remark: '' })

function openTransfer(type) {
  transferType.value = type === 'promote' ? 'promote' : 'transfer'
  transferForm.deptId = member.value.deptId
  transferForm.position = member.value.position
  transferForm.remark = ''
  transferVisible.value = true
}

async function submitTransfer() {
  if (transferType.value === 'transfer' && !transferForm.deptId) {
    proxy.$modal.msgWarning('请选择目标部门')
    return
  }
  if (transferType.value === 'promote' && !transferForm.position) {
    proxy.$modal.msgWarning('请选择目标职务')
    return
  }
  const message =
    transferType.value === 'promote'
      ? '确认将 ' + member.value.name + ' 的职务调整为 ' + positionText(transferForm.position) + '？'
      : '确认将 ' + member.value.name + ' 调入 ' + deptLabel(transferForm.deptId) + '？'
  try {
    await proxy.$modal.confirm(message)
  } catch (e) {
    return
  }
  transferSubmitting.value = true
  try {
    await transferMember({
      memberId: memberId.value,
      deptId: transferForm.deptId,
      position: transferForm.position,
      remark: transferForm.remark || ''
    })
    proxy.$modal.msgSuccess(
      transferType.value === 'promote'
        ? '职务已更新为 ' + positionText(transferForm.position)
        : '已调入 ' + deptLabel(transferForm.deptId) + '，操作已记录日志'
    )
    transferVisible.value = false
    emit('success')
    loadDetail()
  } catch (e) {
    // 失败原因由 request 拦截器统一提示
  } finally {
    transferSubmitting.value = false
  }
}

function positionText(value) {
  const hit = positionOptions.value.find(item => String(item.value) === String(value))
  return hit ? hit.label : ''
}

function deptLabel(deptId) {
  const hit = deptOptions.value.find(item => String(item.value) === String(deptId))
  return hit ? hit.rawLabel || hit.label.replace(/^　+/, '') : ''
}

function handleMoreCommand(command) {
  if (command === 'transfer' || command === 'promote') {
    openTransfer(command)
    return
  }
  if (command === 'resetPwd') {
    handleResetPwd()
  }
}

/* ------------------------------ 部门 / 附件 ------------------------------ */
function loadDeptOptions() {
  deptTreeSelect()
    .then(response => {
      deptOptions.value = flattenDeptOptions(response.data || [])
    })
    .catch(() => {})
}

function beforeUpload(file, kind) {
  const isImage = /\.(jpg|jpeg|png)$/i.test(file.name || '')
  if (kind === 'photo') {
    if (!isImage || file.size > 2 * 1024 * 1024) {
      proxy.$modal.msgError('仅支持 jpg / png，且不超过 2MB')
      return false
    }
    return true
  }
  if (!/\.(jpg|jpeg|png|pdf)$/i.test(file.name || '') || file.size > 5 * 1024 * 1024) {
    proxy.$modal.msgError('学生证仅支持 jpg / png / pdf，且不超过 5MB')
    return false
  }
  return true
}

async function handleUpload(options, kind) {
  const file = options.file
  const bizType = kind === 'photo' ? 'member_photo' : 'member_card'
  try {
    const response = await uploadOaFile(file, bizType)
    const data = response.data || {}
    if (kind === 'photo') {
      attachments.photoFileId = data.fileId || null
      attachments.photoUrl = data.fileUrl || ''
      revokePreview('photo')
      loadAttachmentPreview('photo')
    } else {
      attachments.cardFileId = data.fileId || null
      attachments.cardUrl = data.fileUrl || ''
      revokePreview('card')
      loadAttachmentPreview('card')
    }
    proxy.$modal.msgSuccess('上传成功')
  } catch (e) {
    proxy.$modal.msgError('上传失败，请稍后重试')
  }
}

function loadAttachmentPreview(kind) {
  const fileId = kind === 'photo' ? attachments.photoFileId : attachments.cardFileId
  if (!fileId) return
  getOaFileBlob(fileId)
    .then(blob => {
      if (!blobValidate(blob)) return
      const isPdf = kind === 'card' && attachments.cardUrl && /\.pdf$/i.test(attachments.cardUrl)
      if (isPdf) return
      const url = URL.createObjectURL(new Blob([blob]))
      if (kind === 'photo') photoPreview.value = url
      else cardPreview.value = url
    })
    .catch(() => {})
}

function revokePreview(kind) {
  const url = kind === 'photo' ? photoPreview.value : cardPreview.value
  if (url) URL.revokeObjectURL(url)
  if (kind === 'photo') photoPreview.value = ''
  else cardPreview.value = ''
}

function downloadAttachment(kind) {
  const fileId = kind === 'photo' ? attachments.photoFileId : attachments.cardFileId
  if (!fileId) return
  getOaFileBlob(fileId)
    .then(blob => {
      if (!blobValidate(blob)) {
        proxy.$modal.msgError('附件下载失败')
        return
      }
      saveAs(new Blob([blob]), (kind === 'photo' ? '照片_' : '学生证_') + (member.value.studentNo || fileId) + '.jpg')
    })
    .catch(() => proxy.$modal.msgError('附件下载失败'))
}

/* ------------------------------ 账号 ------------------------------ */
function handleAccountToggle(value) {
  if (!member.value.userId || accountSwitching.value) return
  const targetStatus = value ? '0' : '1'
  const actionText = value ? '启用' : '停用'
  accountSwitching.value = true
  changeUserStatus(member.value.userId, targetStatus)
    .then(() => {
      member.value.accountStatus = targetStatus
      proxy.$modal.msgSuccess('账号已' + actionText)
      emit('success')
    })
    .catch(() => {
      accountActive.value = !value
    })
    .finally(() => {
      accountSwitching.value = false
    })
}

function handleResetPwd() {
  if (!member.value.userId) return
  ElMessageBox.confirm(
    '确认将 ' + (member.value.name || '该成员') + ' 的密码重置为初始密码（手机号后 6 位）？',
    '系统提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
    .then(async () => {
      const phone = String(member.value.phone || '')
      if (/\*/.test(phone) || phone.length < 6) {
        proxy.$modal.msgError('无法获取手机号后 6 位，无法重置为初始密码')
        return
      }
      await resetUserPwd(member.value.userId, phone.slice(-6))
      proxy.$modal.msgSuccess('密码已重置为手机号后 6 位')
    })
    .catch(() => {})
}

/* ------------------------------ 时间轴 / 日志 ------------------------------ */
function timelineType(action) {
  const tone = actionTone(action)
  if (tone === 'green') return 'success'
  if (tone === 'red') return 'danger'
  if (tone === 'orange') return 'warning'
  return 'primary'
}

function goLogs() {
  const query = { memberId: memberId.value }
  const candidates = ['/hr/log', '/oa/log']
  const target = candidates.find(path => router.resolve(path).matched.length > 0)
  visible.value = false
  router.push({ path: target || candidates[0], query })
}

defineExpose({ open })
</script>

<style scoped lang="scss">
@import './member-drawer.scss';

.member-detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f0f2f5;
}
</style>
