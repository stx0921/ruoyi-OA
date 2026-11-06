<template>
  <div class="vh5-stage">
    <div class="vh5-shell">
      <!-- 手机状态栏：仅桌面预览时展示，真机由系统状态栏承担 -->
      <div class="vh5-statusbar" aria-hidden="true">
        <span class="vh5-statusbar__time">9:41</span>
        <span class="vh5-statusbar__sigs">
          <span class="vh5-sig"><i></i><i></i><i></i><i></i></span>
          <span>5G</span>
          <span class="vh5-battery"><i></i></span>
        </span>
      </div>

      <div class="vh5-page">
        <header class="vh5-nav">
          <button type="button" class="vh5-nav__back" aria-label="返回" @click="goBack">‹</button>
          <span class="vh5-nav__title">志愿活动报名</span>
          <span class="vh5-nav__side">免登录</span>
        </header>

        <div ref="scrollRef" class="vh5-scroll">
          <!-- 加载骨架 -->
          <template v-if="loading">
            <div class="vh5-skel-hero"></div>
            <div class="vh5-skel-card">
              <div class="vh5-skel-line vh5-skel-line--short"></div>
              <div class="vh5-skel-line"></div>
              <div class="vh5-skel-line vh5-skel-line--wide"></div>
            </div>
            <div class="vh5-skel-card">
              <div class="vh5-skel-line vh5-skel-line--short"></div>
              <div class="vh5-skel-line"></div>
              <div class="vh5-skel-line"></div>
              <div class="vh5-skel-line vh5-skel-line--wide"></div>
            </div>
          </template>

          <!-- 缺少活动参数 -->
          <div v-else-if="!activityId" class="vh5-empty">
            <div class="vh5-empty__ico">!</div>
            <div class="vh5-empty__title">缺少活动参数</div>
            <div class="vh5-empty__sub">请重新扫描活动二维码进入报名页</div>
          </div>

          <!-- 加载失败 -->
          <div v-else-if="loadError" class="vh5-empty">
            <div class="vh5-empty__ico">!</div>
            <div class="vh5-empty__title">活动加载失败</div>
            <div class="vh5-empty__sub">活动可能已删除或网络异常，请稍后重试</div>
            <button type="button" class="vh5-btn vh5-btn--ghost vh5-empty__btn" @click="fetchActivity">重新加载</button>
          </div>

          <!-- 报名成功 -->
          <div v-else-if="success" class="vh5-success">
            <div class="vh5-success__ok">✓</div>
            <h3>报名成功，等待审核</h3>
            <p class="vh5-success__sub">
              已提交至「{{ activity.name }}」，请等待活动负责人审核
            </p>
            <div class="vh5-success__meta">
              <div><span>活动时间</span><b>{{ fmtDateTime(activity.startTime) }}</b></div>
              <div><span>活动地点</span><b>{{ activity.location || '—' }}</b></div>
              <div><span>志愿时长</span><b>{{ durationText(activity.durationHours) }}</b></div>
            </div>
            <button type="button" class="vh5-btn vh5-btn--primary" @click="goQuery">查询审核结果</button>
            <p class="vh5-tip">同一学号同一活动只能报名一次，请勿重复提交</p>
          </div>

          <!-- 正常报名 -->
          <template v-else>
            <section class="vh5-hero">
              <span class="vh5-status" :class="'vh5-status--' + statusTone">{{ statusLabel }}</span>
              <div class="vh5-hero__tag">校园志愿服务 · 校团委</div>
              <h2>{{ activity.name }}</h2>
              <div class="vh5-hero__meta">
                <span>{{ fmtDateTime(activity.startTime) }} 开始</span>
                <span v-if="activity.location">{{ activity.location }}</span>
                <span>{{ durationText(activity.durationHours) }}</span>
                <span v-if="activity.capacity">参考人数 {{ activity.capacity }} 人</span>
                <span v-if="signupCount !== null">已报名 {{ signupCount }} 人</span>
              </div>
            </section>

            <div class="vh5-card">
              <div class="vh5-sec">活动详情</div>
              <div class="vh5-desc" v-text="activity.content || '—'"></div>
            </div>

            <div class="vh5-card">
              <div class="vh5-sec">报名信息<span class="vh5-sec__tip">全部必填</span></div>

              <div class="vh5-field">
                <div class="vh5-label"><span class="vh5-req">*</span>姓名</div>
                <input
                  v-model="form.name"
                  class="vh5-input"
                  :class="{ 'is-error': errors.name }"
                  type="text"
                  maxlength="20"
                  placeholder="请输入真实姓名"
                  @blur="validateField('name')"
                />
                <div v-if="errors.name" class="vh5-err">{{ errors.name }}</div>
              </div>

              <div class="vh5-field">
                <div class="vh5-label"><span class="vh5-req">*</span>学号</div>
                <input
                  v-model="form.studentNo"
                  class="vh5-input"
                  :class="{ 'is-error': errors.studentNo }"
                  type="text"
                  inputmode="numeric"
                  maxlength="12"
                  placeholder="8–12 位数字，如 2026010202"
                  @blur="validateField('studentNo')"
                />
                <div v-if="errors.studentNo" class="vh5-err">{{ errors.studentNo }}</div>
              </div>

              <div class="vh5-field">
                <div class="vh5-label"><span class="vh5-req">*</span>学院</div>
                <input
                  v-model="form.college"
                  class="vh5-input"
                  :class="{ 'is-error': errors.college }"
                  type="text"
                  maxlength="50"
                  placeholder="如：计算机科学与技术学院"
                  @blur="validateField('college')"
                />
                <div v-if="errors.college" class="vh5-err">{{ errors.college }}</div>
              </div>

              <div class="vh5-field">
                <div class="vh5-label"><span class="vh5-req">*</span>专业</div>
                <input
                  v-model="form.major"
                  class="vh5-input"
                  :class="{ 'is-error': errors.major }"
                  type="text"
                  maxlength="50"
                  placeholder="如：软件工程"
                  @blur="validateField('major')"
                />
                <div v-if="errors.major" class="vh5-err">{{ errors.major }}</div>
              </div>
            </div>

            <div v-if="!canSignup" class="vh5-alert vh5-alert--red">{{ closedReason }}</div>

            <p class="vh5-tip">
              提交后由活动负责人在 PC 端审核，可用「学号 + 姓名」查询审核结果
            </p>
            <div class="vh5-gap"></div>
          </template>
        </div>

        <!-- 底部固定操作条 -->
        <div v-if="showActionBar" class="vh5-actionbar">
          <button type="button" class="vh5-btn vh5-btn--primary" :disabled="!canSignup || submitting" @click="handleSubmit">
            {{ submitting ? '提交中…' : canSignup ? '提交报名' : '报名已关闭' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { getPublicActivity, publicSignup } from '@/api/oa/volunteer'

defineOptions({ name: 'VolunteerH5Apply' })

const route = useRoute()
const router = useRouter()

const scrollRef = ref(null)
const activityId = computed(() => {
  const raw = route.query.activityId
  return raw === undefined || raw === null || raw === '' ? '' : String(Array.isArray(raw) ? raw[0] : raw)
})

const loading = ref(!!activityId.value)
const loadError = ref(false)
const activity = ref(null)
const success = ref(false)
const submitting = ref(false)

const form = reactive({ name: '', studentNo: '', college: '', major: '' })
const errors = reactive({ name: '', studentNo: '', college: '', major: '' })

const STATUS_LABELS = { '0': '报名中', '1': '进行中', '2': '已结束', '3': '已取消', '4': '报名已截止' }
const STATUS_TONES = { '0': 'open', '1': 'ongoing', '2': 'ended', '3': 'cancelled', '4': 'deadline' }
const CLOSED_REASONS = {
  '1': '活动正在进行中，报名已截止',
  '2': '该活动已结束，无法报名',
  '3': '该活动已取消，无法报名',
  '4': '报名已截止，活动尚未开始'
}

const statusLabel = computed(() => {
  if (!activity.value) return ''
  return activity.value.showStatusLabel || STATUS_LABELS[activity.value.showStatus] || ''
})

const statusTone = computed(() => {
  if (!activity.value) return 'ended'
  return STATUS_TONES[activity.value.showStatus] || 'ended'
})

const signupCount = computed(() => {
  const count = activity.value && activity.value.signupCount
  return count === undefined || count === null || count === '' ? null : Number(count)
})

const canSignup = computed(() => {
  if (!activity.value) return false
  if (activity.value.canSignup !== null) return activity.value.canSignup
  return activity.value.showStatus === '0' || activity.value.showStatus === ''
})

const closedReason = computed(() => {
  if (!activity.value) return ''
  return (
    activity.value.closedReason ||
    CLOSED_REASONS[activity.value.showStatus] ||
    '该活动当前不可报名，请等待下一次活动'
  )
})

const showActionBar = computed(
  () => !loading.value && !loadError.value && !success.value && !!activityId.value && !!activity.value
)

onMounted(() => {
  if (activityId.value) {
    fetchActivity()
  }
})

/* -------------------------------------------------------------------------- */
/* 活动详情                                                                    */
/* -------------------------------------------------------------------------- */
async function fetchActivity() {
  if (!activityId.value) return
  loading.value = true
  loadError.value = false
  try {
    const res = await getPublicActivity(activityId.value)
    const data = (res && res.data !== undefined ? res.data : res) || {}
    if (!data || (!data.name && !data.activityId)) throw new Error('empty activity')
    activity.value = normalizeActivity(data)
  } catch (e) {
    loadError.value = true
    activity.value = null
  } finally {
    loading.value = false
  }
}

function normalizeActivity(data) {
  const toStr = value => (value === undefined || value === null ? '' : String(value))
  return {
    activityId: data.activityId || activityId.value,
    name: data.name || '',
    location: data.location || '',
    capacity: data.capacity === undefined || data.capacity === null ? '' : data.capacity,
    durationHours: data.durationHours !== undefined && data.durationHours !== null ? Number(data.durationHours) : 0,
    startTime: data.startTime || '',
    endTime: data.endTime || '',
    signupDeadline: data.signupDeadline || '',
    content: data.content || '',
    showStatus: toStr(data.showStatus),
    showStatusLabel: data.showStatusLabel || '',
    signupCount:
      data.signupCount !== undefined && data.signupCount !== null
        ? data.signupCount
        : data.signedCount !== undefined && data.signedCount !== null
          ? data.signedCount
          : null,
    canSignup: data.canSignup === undefined || data.canSignup === null ? null : !!data.canSignup,
    closedReason: data.closedReason || ''
  }
}

/* -------------------------------------------------------------------------- */
/* 校验与提交                                                                  */
/* -------------------------------------------------------------------------- */
const RULES = {
  name(value) {
    const text = String(value || '').trim()
    if (!text) return '请填写姓名'
    if (text.length < 2 || text.length > 20) return '姓名需为 2–20 个字符'
    return ''
  },
  studentNo(value) {
    const text = String(value || '').trim()
    if (!text) return '请填写学号'
    if (!/^\d{8,12}$/.test(text)) return '请填写 8–12 位数字学号'
    return ''
  },
  college(value) {
    const text = String(value || '').trim()
    if (!text) return '请填写学院'
    if (text.length < 2 || text.length > 50) return '学院需为 2–50 个字符'
    return ''
  },
  major(value) {
    const text = String(value || '').trim()
    if (!text) return '请填写专业'
    if (text.length < 2 || text.length > 50) return '专业需为 2–50 个字符'
    return ''
  }
}

function validateField(field) {
  if (!RULES[field]) return true
  errors[field] = RULES[field](form[field])
  return !errors[field]
}

function validateAll() {
  const fields = ['name', 'studentNo', 'college', 'major']
  const valid = fields.reduce((ok, field) => {
    validateField(field)
    return ok && !errors[field]
  }, true)
  if (!valid) {
    nextTick(() => {
      const input = document.querySelector('.vh5-input.is-error')
      if (input) {
        input.scrollIntoView({ block: 'center', behavior: 'smooth' })
        input.focus({ preventScroll: true })
      }
    })
  }
  return valid
}

async function handleSubmit() {
  if (!canSignup.value) {
    ElMessage.warning(closedReason.value)
    return
  }
  if (!validateAll()) {
    ElMessage.warning('请先补全报名信息')
    return
  }
  submitting.value = true
  try {
    await publicSignup({
      activityId: Number(activityId.value),
      name: form.name.trim(),
      studentNo: form.studentNo.trim(),
      college: form.college.trim(),
      major: form.major.trim()
    })
    success.value = true
    ElMessage.success('报名成功，待审核')
    scrollToTop()
  } catch (e) {
    // 失败提示由 utils/request.js 统一弹出（重复报名 409 / 活动不可报名 400）
  } finally {
    submitting.value = false
  }
}

/* -------------------------------------------------------------------------- */
/* 导航 / 格式化                                                               */
/* -------------------------------------------------------------------------- */
function goQuery() {
  router.push({
    path: '/volunteer/query',
    query: { activityId: activityId.value, studentNo: form.studentNo.trim() }
  })
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  }
}

function scrollToTop() {
  nextTick(() => {
    if (scrollRef.value) scrollRef.value.scrollTop = 0
  })
}

function fmtDateTime(value) {
  if (!value) return '—'
  const text = String(value).replace('T', ' ')
  return text.length > 16 ? text.slice(0, 16) : text
}

function durationText(hours) {
  const num = Number(hours)
  if (!num || Number.isNaN(num)) return '—'
  return num.toFixed(1) + ' 小时'
}
</script>

<style lang="scss" scoped>
/* ==========================================================================
   志愿活动 · H5 报名页（独立于 /h5 布局，自包含 390px 手机壳）
   视觉令牌沿用 assets/styles/ui.scss 的珍珠玻璃主题
   ========================================================================== */

.vh5-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 30px 18px;
  background:
    radial-gradient(55% 45% at 92% 0%, var(--ambient-a), transparent 70%),
    radial-gradient(45% 40% at 20% 100%, var(--ambient-b), transparent 70%),
    var(--stage);
}

.vh5-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 390px;
  height: 812px;
  max-height: calc(100vh - 60px);
  max-height: calc(100dvh - 60px);
  overflow: hidden;
  border-radius: 38px;
  background: var(--canvas);
  box-shadow:
    0 24px 70px rgba(17, 23, 27, 0.24),
    0 3px 12px rgba(17, 23, 27, 0.16),
    0 0 0 8px var(--frame);
}

/* ---------- 模拟状态栏 ---------- */
.vh5-statusbar {
  display: flex;
  flex: 0 0 38px;
  align-items: center;
  justify-content: space-between;
  height: 38px;
  padding: 0 24px;
  background: var(--surface);
  color: var(--ink);
  font-size: 12px;
  font-weight: 600;
}

.vh5-statusbar__sigs {
  display: flex;
  align-items: center;
  gap: 5px;
}

.vh5-sig {
  display: inline-flex;
  align-items: flex-end;
  gap: 2px;
  height: 10px;

  i {
    display: block;
    width: 3px;
    border-radius: 1px;
    background: var(--ink);

    &:nth-child(1) { height: 4px; }
    &:nth-child(2) { height: 6px; }
    &:nth-child(3) { height: 8px; }
    &:nth-child(4) { height: 10px; }
  }
}

.vh5-battery {
  position: relative;
  width: 22px;
  height: 11px;
  margin-left: 3px;
  border: 1px solid var(--ink);
  border-radius: 2px;

  &::after {
    content: '';
    position: absolute;
    top: 3px;
    right: -3px;
    width: 2px;
    height: 5px;
    border-radius: 0 1px 1px 0;
    background: var(--ink);
  }

  i {
    position: absolute;
    inset: 1px 6px 1px 1px;
    border-radius: 1px;
    background: var(--ink);
  }
}

/* ---------- 页面容器 ---------- */
.vh5-page {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  background:
    radial-gradient(70% 45% at 90% 0%, var(--ambient-a), transparent 70%),
    var(--canvas);
}

.vh5-nav {
  display: flex;
  flex: 0 0 44px;
  align-items: center;
  height: 44px;
  padding: 0 10px;
  border-bottom: 1px solid var(--line);
  background: var(--rail-bg);
  box-shadow: 0 1px 0 var(--line-strong);
  backdrop-filter: blur(22px) saturate(1.1);
  -webkit-backdrop-filter: blur(22px) saturate(1.1);
}

.vh5-nav__back {
  display: flex;
  flex: 0 0 34px;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 44px;
  padding: 0;
  border: none;
  background: none;
  color: var(--ink-muted);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.vh5-nav__title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: var(--ink);
  font-size: 16px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vh5-nav__side {
  flex: 0 0 auto;
  padding: 0 4px;
  color: var(--ink-soft);
  font-size: 11.5px;
}

.vh5-scroll {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding-bottom: 6px;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain;

  &::-webkit-scrollbar {
    width: 0;
    height: 0;
  }
}

/* ---------- 顶部活动头图 ---------- */
.vh5-hero {
  position: relative;
  margin: 0 0 2px;
  padding: 26px 20px 24px;
  overflow: hidden;
  border-radius: 0 0 var(--r-xl) var(--r-xl);
  background: linear-gradient(155deg, var(--blue) 0%, #4c8dff 55%, #6ea6ff 100%);
  color: #fff;

  &::after {
    content: '';
    position: absolute;
    top: -34px;
    right: -30px;
    width: 136px;
    height: 136px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.14);
  }
}

.vh5-hero__tag {
  position: relative;
  z-index: 1;
  margin-bottom: 8px;
  font-size: 11.5px;
  letter-spacing: 0.5px;
  opacity: 0.92;
}

.vh5-hero h2 {
  position: relative;
  z-index: 1;
  max-width: 290px;
  margin: 0 0 12px;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: 0.3px;
}

.vh5-hero__meta {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.vh5-hero__meta span {
  padding: 3px 9px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 11.5px;
  line-height: 1.5;
}

.vh5-status {
  position: absolute;
  top: 18px;
  right: 18px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.94);
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1;
  box-shadow: 0 4px 12px rgba(9, 30, 80, 0.18);

  &--open { color: var(--leaf-text); }
  &--ongoing { color: var(--blue); }
  &--ended { color: var(--ink-soft); }
  &--cancelled { color: var(--red-text); }
  &--deadline { color: var(--warn-text); }
}

/* ---------- 卡片 / 区块 ---------- */
.vh5-card {
  margin: 12px 14px;
  padding: 16px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-md);
  background: var(--surface);
  box-shadow: var(--edge), var(--e1);
}

.vh5-sec {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  color: var(--ink);
  font-size: 14px;
  font-weight: 650;

  &::before {
    content: '';
    width: 3px;
    height: 13px;
    border-radius: 2px;
    background: var(--blue);
  }
}

.vh5-sec__tip {
  margin-left: auto;
  color: var(--ink-soft);
  font-size: 11px;
  font-weight: 400;
}

.vh5-desc {
  color: var(--ink-muted);
  font-size: 13px;
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ---------- 表单 ---------- */
.vh5-field {
  margin-bottom: 14px;

  &:last-child {
    margin-bottom: 0;
  }
}

.vh5-label {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-bottom: 6px;
  color: var(--ink-muted);
  font-size: 13px;
  font-weight: 500;
}

.vh5-req {
  color: var(--red-text);
}

.vh5-input {
  box-sizing: border-box;
  width: 100%;
  height: 44px;
  padding: 0 12px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-sm);
  background: var(--input-bg);
  color: var(--ink);
  font-family: inherit;
  font-size: 16px;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;

  &::placeholder {
    color: #c0c4cc;
  }

  &:focus {
    border-color: var(--blue);
    box-shadow: 0 0 0 3px var(--blue-soft);
  }

  &.is-error {
    border-color: var(--red-text);
  }
}

.vh5-err {
  margin-top: 5px;
  color: var(--red-text);
  font-size: 12px;
  line-height: 1.5;
}

/* ---------- 提示条 / 按钮 ---------- */
.vh5-alert {
  margin: 0 14px 4px;
  padding: 10px 12px;
  border: 1px solid #f5dab1;
  border-radius: var(--r-sm);
  background: var(--warn-soft);
  color: var(--warn-text);
  font-size: 12px;
  line-height: 1.6;

  &--red {
    border-color: #fbc4c4;
    background: var(--red-soft);
    color: var(--red-text);
  }
}

.vh5-tip {
  margin: 10px 18px 0;
  color: var(--ink-soft);
  font-size: 11.5px;
  line-height: 1.7;
  text-align: center;
}

.vh5-gap {
  height: 18px;
}

.vh5-actionbar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
  padding: 10px 14px calc(10px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--line);
  background: var(--rail-bg);
  box-shadow: 0 -1px 0 var(--line-strong);
  backdrop-filter: blur(22px) saturate(1.1);
  -webkit-backdrop-filter: blur(22px) saturate(1.1);
}

.vh5-btn {
  display: inline-flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 44px;
  padding: 0 16px;
  border: none;
  border-radius: 999px;
  background: var(--groove-bg);
  color: var(--ink-muted);
  font-family: inherit;
  font-size: 16px;
  letter-spacing: 1px;
  cursor: pointer;
  transition: opacity 0.15s, box-shadow 0.15s;

  &:active {
    opacity: 0.9;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.55;
    box-shadow: none;
  }

  &--primary {
    background: var(--blue);
    box-shadow: var(--e-accent);
    color: #fff;
  }

  &--ghost {
    flex: 0 0 auto;
    border: 1px solid var(--blue);
    background: var(--surface);
    color: var(--blue);
    letter-spacing: 0;
  }
}

/* ---------- 成功态 ---------- */
.vh5-success {
  padding: 40px 22px 26px;
  text-align: center;

  &__ok {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 72px;
    height: 72px;
    margin: 0 auto 16px;
    border: 2px solid var(--lime);
    border-radius: 50%;
    background: var(--lime-soft);
    color: var(--leaf-text);
    font-size: 34px;
    line-height: 1;
  }

  h3 {
    margin: 0 0 8px;
    color: var(--ink);
    font-size: 19px;
    font-weight: 650;
  }

  &__sub {
    margin: 0 0 18px;
    color: var(--ink-soft);
    font-size: 13px;
    line-height: 1.7;
  }

  &__meta {
    margin-bottom: 20px;
    padding: 12px 14px;
    border: 1px solid var(--line-strong);
    border-radius: var(--r-sm);
    background: var(--surface);
    text-align: left;

    div {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
      padding: 5px 0;
      font-size: 12.5px;
      line-height: 1.6;

      span {
        flex: 0 0 62px;
        color: var(--ink-soft);
      }

      b {
        flex: 1;
        color: var(--ink);
        font-weight: 500;
        text-align: right;
        word-break: break-all;
      }
    }
  }
}

/* ---------- 空态 / 错误态 ---------- */
.vh5-empty {
  padding: 64px 24px 40px;
  text-align: center;

  &__ico {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 60px;
    height: 60px;
    margin: 0 auto 14px;
    border-radius: 50%;
    background: var(--groove-bg);
    color: var(--ink-soft);
    font-size: 26px;
    font-weight: 600;
  }

  &__title {
    margin-bottom: 8px;
    color: var(--ink);
    font-size: 15px;
    font-weight: 600;
  }

  &__sub {
    margin-bottom: 18px;
    color: var(--ink-soft);
    font-size: 12.5px;
    line-height: 1.7;
  }

  &__btn {
    flex: 0 0 auto;
    width: auto;
    min-width: 132px;
    margin: 0 auto;
  }
}

/* ---------- 骨架 ---------- */
.vh5-skel-hero {
  height: 164px;
  border-radius: 0 0 var(--r-xl) var(--r-xl);
  background: linear-gradient(90deg, rgba(35, 109, 233, 0.14) 25%, rgba(35, 109, 233, 0.24) 37%, rgba(35, 109, 233, 0.14) 63%);
  background-size: 400% 100%;
  animation: vh5-skel 1.3s ease infinite;
}

.vh5-skel-card {
  margin: 12px 14px;
  padding: 16px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-md);
  background: var(--surface);
}

.vh5-skel-line {
  height: 12px;
  margin-bottom: 10px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--hover-bg) 25%, var(--line-strong) 37%, var(--hover-bg) 63%);
  background-size: 400% 100%;
  animation: vh5-skel 1.3s ease infinite;

  &:last-child {
    margin-bottom: 0;
  }

  &--wide {
    width: 70%;
  }

  &--short {
    width: 40%;
  }
}

@keyframes vh5-skel {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

/* ---------- 真机优先：小屏 / 触屏 / 横屏手机铺满整屏 ---------- */
@media (max-width: 480px), (pointer: coarse), (orientation: landscape) and (max-height: 520px) {
  .vh5-stage {
    display: block;
    padding: 0;
    background: var(--canvas);
  }

  .vh5-shell {
    width: 100%;
    height: 100vh;
    height: 100dvh;
    max-height: none;
    border-radius: 0;
    box-shadow: none;
  }

  .vh5-statusbar {
    display: none;
  }

  .vh5-nav {
    flex: 0 0 auto;
    height: auto;
    min-height: calc(44px + env(safe-area-inset-top));
    padding-top: env(safe-area-inset-top);
  }

  .vh5-nav__back {
    height: 44px;
  }
}
</style>
