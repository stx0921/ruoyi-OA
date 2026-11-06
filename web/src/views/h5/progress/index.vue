<template>
  <div class="h5-page">
    <header class="h5-nav">
      <button type="button" class="h5-nav__back" aria-label="返回" @click="handleBack">‹</button>
      <span class="h5-nav__title">报名进度</span>
      <button v-if="phase === 'list'" type="button" class="h5-nav__side" @click="resetToVerify('请重新验证后查看最新进度')">刷新</button>
      <span v-else class="h5-nav__side h5-nav__side--muted"></span>
    </header>

    <div ref="scrollRef" class="h5-scroll">
      <!-- ================= 身份验证 ================= -->
      <template v-if="phase === 'verify'">
        <div class="h5-login-card">
          <h3>查询我的报名进度</h3>
          <p class="h5-login-card__sub">
            输入报名时填写的手机号，完成图形验证码后即可查看全部申请、驳回原因与账号指引。
          </p>

          <div class="h5-field">
            <label class="h5-field__label"><span class="h5-req">*</span>手机号</label>
            <input
              id="progress-phone"
              v-model="phone"
              class="h5-input"
              :class="{ 'is-error': errors.phone }"
              type="tel"
              inputmode="numeric"
              maxlength="11"
              placeholder="请输入报名手机号"
              @blur="validateField('phone')"
            />
            <div v-if="errors.phone" class="h5-err">{{ errors.phone }}</div>
          </div>

          <div class="h5-field">
            <label class="h5-field__label"><span class="h5-req">*</span>图形验证码</label>
            <div class="h5-field__row">
              <input
                id="progress-captcha"
                v-model="captchaCode"
                class="h5-input"
                :class="{ 'is-error': errors.captchaCode }"
                type="text"
                maxlength="8"
                autocomplete="off"
                placeholder="请输入右侧验证码"
                @blur="validateField('captchaCode')"
                @keyup.enter="handleQuery"
              />
              <img v-if="captchaSrc" class="h5-captcha" :src="captchaSrc" alt="图形验证码" title="看不清？点击刷新" @click="refreshCaptcha" />
              <button v-else type="button" class="h5-captcha h5-captcha--placeholder" @click="refreshCaptcha">
                {{ captchaLoading ? '加载中…' : '点击获取' }}
              </button>
            </div>
            <div v-if="errors.captchaCode" class="h5-err">{{ errors.captchaCode }}</div>
          </div>

          <button type="button" class="h5-submit" :disabled="querying" @click="handleQuery">
            {{ querying ? '验证中…' : '查询' }}
          </button>

          <div class="h5-tip-line">验证通过后 30 分钟内可查看、撤回或修改重提；刷新页面需重新验证</div>
        </div>

        <div class="h5-login-card h5-login-card--plain">
          <div class="h5-login-card__title">查不到？</div>
          <p class="h5-login-card__sub">请确认手机号与报名时填写的一致；同一手机号可查询该号码下的全部报名记录。</p>
          <button type="button" class="h5-submit is-plain is-sm" @click="goApply">去报名</button>
        </div>
      </template>

      <!-- ================= 我的申请列表 ================= -->
      <template v-else>
        <div class="h5-list-head">
          <span>手机号 <b>{{ queriedPhone }}</b> 的申请，共 <b>{{ applies.length }}</b> 条</span>
          <button type="button" class="h5-list-head__refresh" @click="resetToVerify('请重新验证后查看最新进度')">刷新</button>
        </div>

        <div v-for="item in applies" :key="item.applyId" class="h5-app-item">
          <div class="h5-app-top">
            <div>
              <div class="h5-app-top__dept">{{ item.deptName || '意向部门' }}</div>
              <div class="h5-app-top__no">{{ item.applyNo || '—' }}</div>
            </div>
            <span class="h5-status-pill" :class="'h5-status-pill--' + statusTone(item)">{{ statusLabel(item) }}</span>
          </div>

          <div class="h5-app-meta">
            <div>提交时间：{{ item.createTime || '—' }}</div>
            <div v-if="item.auditTime">审批时间：{{ item.auditTime }}<template v-if="item.auditUserName"> · 审批人：{{ item.auditUserName }}</template></div>
          </div>

          <!-- 驳回原因 -->
          <div v-if="item.status === '2'" class="h5-reason">
            <b>驳回原因：</b>{{ item.auditRemark || '未填写驳回原因' }}
            <div v-if="item.auditUserName || item.auditTime" class="h5-reason__meta">
              {{ item.auditUserName || '审批人' }}<template v-if="item.auditTime"> · {{ item.auditTime }}</template>
            </div>
          </div>

          <!-- 已关闭原因 -->
          <div v-if="item.status === '4'" class="h5-reason h5-reason--gray">
            <b>已关闭：</b>{{ item.auditRemark || '已被其他部门录用，本条申请自动关闭' }}
          </div>

          <!-- 已通过：账号提示 -->
          <div v-if="item.status === '1'" class="h5-account-hint">
            <b>已通过，账号已开通。</b> 点击下方「获取账号指引」查看登录方式。
          </div>

          <div v-if="hasActions(item)" class="h5-app-actions">
            <button
              v-if="item.status === '0'"
              type="button"
              class="h5-btn-sm h5-btn-sm--danger"
              :disabled="withdrawingId === item.applyId"
              @click="handleWithdraw(item)"
            >
              {{ withdrawingId === item.applyId ? '撤回中…' : '撤回' }}
            </button>
            <button v-if="item.status === '2'" type="button" class="h5-btn-sm h5-btn-sm--primary" @click="handleResubmit(item)">
              修改重提
            </button>
            <button v-if="item.status === '1'" type="button" class="h5-btn-sm h5-btn-sm--primary" @click="openAccountSheet(item)">
              获取账号指引
            </button>
          </div>
        </div>

        <!-- 空态 -->
        <div v-if="!applies.length" class="h5-empty">
          <div class="h5-empty__title">该手机号下暂无报名记录</div>
          <div class="h5-empty__sub">换个手机号查查，或直接去报名</div>
          <button type="button" class="h5-submit h5-empty__action" @click="goApply">去报名</button>
        </div>

        <!-- 再来一份 -->
        <div v-if="applies.length" class="h5-list-foot">
          <button type="button" class="h5-submit is-plain is-sm" @click="goApply">再填一份（投放其他部门）</button>
          <div class="h5-tip-line">同一学号可向不同部门并行投递，每个部门同时只允许 1 条待审批</div>
        </div>
      </template>
    </div>

    <!-- 账号指引底部弹层 -->
    <div v-if="sheetVisible" class="h5-sheet-mask" @click.self="sheetVisible = false">
      <div class="h5-sheet">
        <div class="h5-sheet__title">账号指引</div>
        <template v-if="sheetApply && sheetApply.accountTip">
          <p class="h5-account-tip">{{ sheetApply.accountTip }}</p>
        </template>
        <template v-else>
          <div class="h5-sheet__row"><span>账号</span><b>学号（报名时填写）</b></div>
          <div class="h5-sheet__row"><span>初始密码</span><b>手机号后 6 位</b></div>
          <div class="h5-sheet__row"><span>意向部门</span><b>{{ (sheetApply && sheetApply.deptName) || '—' }}</b></div>
        </template>
        <div class="h5-hint">首次登录请修改初始密码，密码需 8–20 位且同时包含字母和数字</div>
        <div class="h5-sheet__actions">
          <button type="button" class="h5-submit" @click="goLogin">去登录</button>
        </div>
        <button type="button" class="h5-btn-text h5-sheet__close" @click="sheetVisible = false">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApplyCaptcha, queryApplyProgress, withdrawApply } from '@/api/oa/public'

defineOptions({ name: 'H5Progress' })

const RESUBMIT_KEY = 'oa:h5:resubmit:'
const PHONE_STORAGE_KEY = 'oa:h5:progress:phone'
const QUERY_TOKEN_TTL = 30 * 60 * 1000

const router = useRouter()
const scrollRef = ref(null)

const phase = ref('verify')
const phone = ref('')
const captcha = reactive({ uuid: '', img: '' })
const captchaCode = ref('')
const captchaLoading = ref(false)
const querying = ref(false)
const errors = reactive({ phone: '', captchaCode: '' })

const queryToken = ref('')
const queryExpireAt = ref(0)
const queriedPhone = ref('')
const applies = ref([])
const withdrawingId = ref(null)
const sheetVisible = ref(false)
const sheetApply = ref(null)

const STATUS_MAP = {
  '0': { label: '待审批', tone: 'orange' },
  '1': { label: '已通过', tone: 'green' },
  '2': { label: '已驳回', tone: 'red' },
  '3': { label: '已撤回', tone: 'gray' },
  '4': { label: '已关闭', tone: 'gray' }
}

const captchaSrc = computed(() => (captcha.img ? 'data:image/jpeg;base64,' + captcha.img : ''))

onMounted(() => {
  const saved = localStorage.getItem(PHONE_STORAGE_KEY)
  if (saved) phone.value = saved
  refreshCaptcha()
})

// ---------- 图形验证码 ----------
async function refreshCaptcha() {
  if (captchaLoading.value) return
  captchaLoading.value = true
  try {
    const res = await getApplyCaptcha()
    const data = res.data !== undefined ? res.data : res || {}
    captcha.uuid = data.uuid || ''
    captcha.img = data.img || ''
    captchaCode.value = ''
    errors.captchaCode = ''
  } catch (e) {
    captcha.uuid = ''
    captcha.img = ''
  } finally {
    captchaLoading.value = false
  }
}

// ---------- 校验 ----------
function validateField(field) {
  if (field === 'phone') {
    const text = phone.value.trim()
    if (!text) errors.phone = '请输入手机号'
    else if (!/^1[3-9]\d{9}$/.test(text)) errors.phone = '请输入 11 位手机号'
    else errors.phone = ''
    return !errors.phone
  }
  if (field === 'captchaCode') {
    const text = captchaCode.value.trim()
    if (!text) errors.captchaCode = '请输入图形验证码'
    else if (!/^[A-Za-z0-9]{1,8}$/.test(text)) errors.captchaCode = '请输入正确的图形验证码'
    else errors.captchaCode = ''
    return !errors.captchaCode
  }
  return true
}

function focusField(id) {
  nextTick(() => {
    const element = document.getElementById(id)
    if (element) {
      element.scrollIntoView({ block: 'center', behavior: 'smooth' })
      element.focus({ preventScroll: true })
    }
  })
}

// ---------- 查询 ----------
async function handleQuery() {
  if (querying.value) return
  const phoneValid = validateField('phone')
  const codeValid = validateField('captchaCode')
  if (!phoneValid) {
    focusField('progress-phone')
    return
  }
  if (!codeValid) {
    focusField('progress-captcha')
    return
  }
  querying.value = true
  try {
    const res = await queryApplyProgress({
      phone: phone.value.trim(),
      captchaUuid: captcha.uuid,
      captchaCode: captchaCode.value.trim()
    })
    const data = res.data !== undefined ? res.data : res || {}
    queryToken.value = data.queryToken || ''
    queryExpireAt.value = Date.now() + QUERY_TOKEN_TTL
    queriedPhone.value = phone.value.trim()
    localStorage.setItem(PHONE_STORAGE_KEY, queriedPhone.value)
    applies.value = Array.isArray(data.applyList) ? data.applyList : []
    phase.value = 'list'
    captchaCode.value = ''
    scrollToTop()
  } catch (e) {
    refreshCaptcha()
  } finally {
    querying.value = false
  }
}

function resetToVerify(message) {
  phase.value = 'verify'
  applies.value = []
  queryToken.value = ''
  queryExpireAt.value = 0
  sheetVisible.value = false
  refreshCaptcha()
  if (message) ElMessage.warning(message)
  scrollToTop()
}

function isQueryValid() {
  return !!queryToken.value && Date.now() < queryExpireAt.value
}

function ensureQueryValid() {
  if (isQueryValid()) return true
  resetToVerify('验证已过期，请重新验证')
  return false
}

// ---------- 状态 ----------
function statusLabel(item) {
  return item.statusLabel || (STATUS_MAP[String(item.status)] || {}).label || '未知'
}

function statusTone(item) {
  return (STATUS_MAP[String(item.status)] || {}).tone || 'gray'
}

function hasActions(item) {
  return item.status === '0' || item.status === '2' || item.status === '1'
}

// ---------- 撤回 ----------
async function handleWithdraw(item) {
  if (!ensureQueryValid()) return
  try {
    await ElMessageBox.confirm('撤回后不可恢复，确认撤回？', '撤回申请', {
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'h5-message-box'
    })
  } catch (e) {
    return
  }
  withdrawingId.value = item.applyId
  try {
    await withdrawApply({
      applyId: item.applyId,
      phone: queriedPhone.value,
      queryToken: queryToken.value
    })
    item.status = '3'
    item.statusLabel = '已撤回'
    ElMessage.success('已撤回申请，可重新提交')
  } catch (e) {
    // 错误提示由 utils/request.js 统一处理
  } finally {
    withdrawingId.value = null
  }
}

// ---------- 修改重提 ----------
function handleResubmit(item) {
  if (!ensureQueryValid()) return
  // 整条申请快照（ProgressApplyVO 已含原申请表单字段）+ queryToken 一并交接给报名页回填
  sessionStorage.setItem(
    RESUBMIT_KEY + item.applyId,
    JSON.stringify({
      ...item,
      queryToken: queryToken.value,
      phone: queriedPhone.value
    })
  )
  router.push({
    path: '/h5/apply',
    query: { resubmitApplyId: String(item.applyId), phone: queriedPhone.value }
  })
}

// ---------- 账号指引 ----------
function openAccountSheet(item) {
  sheetApply.value = item
  sheetVisible.value = true
}

function goLogin() {
  sheetVisible.value = false
  router.push('/login')
}

function goApply() {
  router.push('/h5/apply')
}

function handleBack() {
  router.replace('/h5/apply')
}

function scrollToTop() {
  nextTick(() => {
    if (scrollRef.value) scrollRef.value.scrollTop = 0
  })
}
</script>

<style lang="scss" scoped>
.h5-field__row .h5-input {
  flex: 1;
  width: auto;
  min-width: 0;
}

.h5-captcha--placeholder {
  padding: 0;
  font-family: inherit;
}

.h5-login-card {
  margin: 14px 12px;
  padding: 18px;
  border-radius: 10px;
  background: #fff;

  h3 {
    margin: 0 0 6px;
    color: #303133;
    font-size: 17px;
  }

  &__sub {
    margin: 0 0 16px;
    color: #909399;
    font-size: 12.5px;
    line-height: 1.7;
  }

  &--plain {
    background: transparent;
    border: 1px dashed #dcdfe6;
    text-align: center;
  }

  &__title {
    margin-bottom: 6px;
    color: #606266;
    font-size: 14px;
    font-weight: 600;
  }
}

.h5-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 14px 4px;
  color: #606266;
  font-size: 13px;

  b {
    color: #303133;
  }

  &__refresh {
    flex: 0 0 auto;
    padding: 0;
    border: none;
    background: none;
    color: #236de9;
    font-size: 12.5px;
    cursor: pointer;
  }
}

.h5-list-foot {
  padding: 6px 12px 24px;
}

.h5-account-tip {
  margin: 0 0 12px;
  padding: 12px;
  border-radius: 8px;
  background: #f0f9eb;
  color: #4e8f2e;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-line;
}

.h5-sheet__close {
  width: 100%;
  margin-top: 4px;
}
</style>
