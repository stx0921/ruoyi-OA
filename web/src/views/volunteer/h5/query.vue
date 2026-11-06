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
          <span class="vh5-nav__title">查询审核结果</span>
          <span class="vh5-nav__side">免登录</span>
        </header>

        <div ref="scrollRef" class="vh5-scroll">
          <!-- 查询表单 -->
          <div class="vh5-card vh5-card--form">
            <div class="vh5-sec">报名信息查询</div>
            <p class="vh5-form-sub">输入报名时填写的学号与姓名，查询审核状态、志愿时长与驳回原因。</p>

            <div class="vh5-field">
              <div class="vh5-label"><span class="vh5-req">*</span>学号</div>
              <input
                v-model="form.studentNo"
                class="vh5-input"
                :class="{ 'is-error': errors.studentNo }"
                type="text"
                inputmode="numeric"
                maxlength="12"
                placeholder="报名时填写的学号"
                @blur="validateField('studentNo')"
                @keyup.enter="doQuery"
              />
              <div v-if="errors.studentNo" class="vh5-err">{{ errors.studentNo }}</div>
            </div>

            <div class="vh5-field">
              <div class="vh5-label"><span class="vh5-req">*</span>姓名</div>
              <input
                v-model="form.name"
                class="vh5-input"
                :class="{ 'is-error': errors.name }"
                type="text"
                maxlength="20"
                placeholder="报名时填写的姓名"
                @blur="validateField('name')"
                @keyup.enter="doQuery"
              />
              <div v-if="errors.name" class="vh5-err">{{ errors.name }}</div>
            </div>

            <button type="button" class="vh5-btn vh5-btn--primary vh5-query-btn" :disabled="querying" @click="doQuery">
              {{ querying ? '查询中…' : '查 询' }}
            </button>
          </div>

          <!-- 三态结果卡：待审核橙 / 通过绿 / 驳回红 -->
          <div v-if="queried && found" ref="resultRef" class="vh5-result" :class="'vh5-result--' + tone">
            <div class="vh5-result__head">
              <span class="vh5-result__ico">{{ resultIcon }}</span>
              <div class="vh5-result__head-main">
                <div class="vh5-result__title">{{ resultTitle }}</div>
                <div class="vh5-result__sub">{{ resultSub }}</div>
              </div>
            </div>
            <div class="vh5-result__body">
              <div><span>姓名</span><b>{{ queriedName }}</b></div>
              <div><span>学号</span><b>{{ queriedStudentNo }}</b></div>
              <div v-if="info.activityName"><span>活动</span><b>{{ info.activityName }}</b></div>
              <div v-if="tone === 'approved'"><span>志愿时长</span><b>{{ durationText(info.durationHours) }}</b></div>
              <div v-if="tone === 'rejected'"><span>驳回原因</span><b>{{ info.auditRemark || '未填写驳回原因' }}</b></div>
              <div v-if="tone === 'pending'"><span>当前状态</span><b>待审核，请耐心等待负责人审核</b></div>
              <div v-if="info.auditTime"><span>审核时间</span><b>{{ fmtDateTime(info.auditTime) }}</b></div>
            </div>
          </div>

          <!-- 未匹配 -->
          <div v-else-if="queried && !found" class="vh5-result vh5-result--none">
            <div class="vh5-result__head">
              <span class="vh5-result__ico">？</span>
              <div class="vh5-result__head-main">
                <div class="vh5-result__title">未查询到报名记录</div>
                <div class="vh5-result__sub">请核对学号与姓名是否与报名时一致</div>
              </div>
            </div>
            <div class="vh5-result__body">
              <div class="vh5-result__note">若确认已提交，请联系活动负责人核对报名信息。</div>
            </div>
          </div>

          <p class="vh5-tip">审核结果由活动负责人在 PC 端「活动详情 → 报名名单」审核后同步</p>
          <div class="vh5-gap"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { queryPublicResult } from '@/api/oa/volunteer'

defineOptions({ name: 'VolunteerH5Query' })

const route = useRoute()
const router = useRouter()

const scrollRef = ref(null)
const resultRef = ref(null)

const activityId = computed(() => {
  const raw = route.query.activityId
  return raw === undefined || raw === null || raw === '' ? '' : String(Array.isArray(raw) ? raw[0] : raw)
})

const form = reactive({
  studentNo: String(route.query.studentNo || ''),
  name: String(route.query.name || '')
})
const errors = reactive({ studentNo: '', name: '' })

const querying = ref(false)
const queried = ref(false)
const found = ref(false)
const queriedStudentNo = ref('')
const queriedName = ref('')
const info = reactive({
  auditStatus: '',
  auditRemark: '',
  auditTime: '',
  activityName: '',
  durationHours: null
})

const STATUS_MAP = {
  '0': { tone: 'pending', title: '待审核', icon: '…' },
  '1': { tone: 'approved', title: '审核通过', icon: '✓' },
  '2': { tone: 'rejected', title: '审核驳回', icon: '✕' }
}

const current = computed(() => STATUS_MAP[String(info.auditStatus)] || STATUS_MAP['0'])
const tone = computed(() => current.value.tone)
const resultTitle = computed(() => current.value.title)
const resultIcon = computed(() => current.value.icon)
const resultSub = computed(() => {
  const activity = info.activityName || '活动'
  if (tone.value === 'approved') return `${activity} · 志愿时长 ${durationText(info.durationHours)}`
  if (tone.value === 'rejected') return `${activity} · 报名未通过`
  return `${activity} · 等待负责人审核`
})

onMounted(() => {
  // 从报名成功页进入时带入学号，可直接补填姓名查询
  if (route.query.studentNo) {
    form.studentNo = String(route.query.studentNo)
  }
})

/* -------------------------------------------------------------------------- */
/* 校验与查询                                                                  */
/* -------------------------------------------------------------------------- */
const RULES = {
  studentNo(value) {
    const text = String(value || '').trim()
    if (!text) return '请填写学号'
    if (!/^\d{8,12}$/.test(text)) return '请填写 8–12 位数字学号'
    return ''
  },
  name(value) {
    const text = String(value || '').trim()
    if (!text) return '请填写姓名'
    if (text.length < 2 || text.length > 20) return '姓名需为 2–20 个字符'
    return ''
  }
}

function validateField(field) {
  if (!RULES[field]) return true
  errors[field] = RULES[field](form[field])
  return !errors[field]
}

function validateAll() {
  const fields = ['studentNo', 'name']
  const valid = fields.reduce((ok, field) => {
    validateField(field)
    return ok && !errors[field]
  }, true)
  if (!valid) {
    nextTick(() => {
      const input = document.querySelector('.vh5-input.is-error')
      if (input) input.focus({ preventScroll: true })
    })
  }
  return valid
}

async function doQuery() {
  if (querying.value) return
  if (!validateAll()) return
  querying.value = true
  try {
    const params = {
      studentNo: form.studentNo.trim(),
      name: form.name.trim()
    }
    if (activityId.value) params.activityId = activityId.value
    const res = await queryPublicResult(params)
    const data = (res && res.data !== undefined ? res.data : res) || {}
    if (!data || (data.auditStatus === undefined && !data.activityName)) {
      throw new Error('empty result')
    }
    found.value = true
    queriedStudentNo.value = form.studentNo.trim()
    queriedName.value = form.name.trim()
    info.auditStatus = data.auditStatus === undefined || data.auditStatus === null ? '' : String(data.auditStatus)
    info.auditRemark = data.auditRemark || ''
    info.auditTime = data.auditTime || ''
    info.activityName = data.activityName || ''
    info.durationHours =
      data.durationHours !== undefined && data.durationHours !== null ? Number(data.durationHours) : null
  } catch (e) {
    // 404 未匹配 / 400 参数错误：展示未匹配卡，具体原因由 utils/request.js 统一提示
    found.value = false
    queriedStudentNo.value = form.studentNo.trim()
    queriedName.value = form.name.trim()
  } finally {
    queried.value = true
    querying.value = false
    scrollToResult()
  }
}

/* -------------------------------------------------------------------------- */
/* 导航 / 格式化                                                               */
/* -------------------------------------------------------------------------- */
function goBack() {
  if (window.history.length > 1) {
    router.back()
  }
}

function scrollToResult() {
  nextTick(() => {
    const target = resultRef.value || scrollRef.value
    if (resultRef.value) {
      resultRef.value.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    } else if (target) {
      target.scrollTop = target.scrollHeight
    }
  })
}

function fmtDateTime(value) {
  if (!value) return '—'
  const text = String(value).replace('T', ' ')
  return text.length > 16 ? text.slice(0, 16) : text
}

function durationText(hours) {
  const num = Number(hours)
  if (hours === undefined || hours === null || hours === '' || Number.isNaN(num)) return '—'
  return num.toFixed(1) + ' 小时'
}
</script>

<style lang="scss" scoped>
/* ==========================================================================
   志愿活动 · H5 审核结果查询（独立于 /h5 布局，自包含 390px 手机壳）
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
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain;

  &::-webkit-scrollbar {
    width: 0;
    height: 0;
  }
}

/* ---------- 卡片 / 表单 ---------- */
.vh5-card {
  margin: 14px 14px 0;
  padding: 18px 16px 16px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-md);
  background: var(--surface);
  box-shadow: var(--edge), var(--e1);
}

.vh5-sec {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 6px;
  color: var(--ink);
  font-size: 15px;
  font-weight: 650;

  &::before {
    content: '';
    width: 3px;
    height: 13px;
    border-radius: 2px;
    background: var(--blue);
  }
}

.vh5-form-sub {
  margin: 0 0 16px;
  color: var(--ink-soft);
  font-size: 12px;
  line-height: 1.7;
}

.vh5-field {
  margin-bottom: 14px;
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

.vh5-query-btn {
  width: 100%;
  margin-top: 2px;
}

/* ---------- 按钮 ---------- */
.vh5-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
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
}

/* ---------- 三态结果卡 ---------- */
.vh5-result {
  margin: 14px;
  padding: 16px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-md);
  background: var(--surface);
  box-shadow: var(--edge), var(--e1);

  &__head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
  }

  &__ico {
    display: flex;
    flex: 0 0 42px;
    align-items: center;
    justify-content: center;
    width: 42px;
    height: 42px;
    border-radius: 50%;
    background: #fff;
    font-size: 20px;
    font-weight: 600;
    line-height: 1;
  }

  &__head-main {
    flex: 1;
    min-width: 0;
  }

  &__title {
    color: var(--ink);
    font-size: 16px;
    font-weight: 650;
  }

  &__sub {
    margin-top: 2px;
    color: var(--ink-soft);
    font-size: 11.5px;
    line-height: 1.6;
    word-break: break-all;
  }

  &__body {
    padding-top: 10px;
    border-top: 1px dashed var(--line-strong);
    color: var(--ink-muted);
    font-size: 13px;
    line-height: 1.9;

    > div {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 3px 0;
    }

    span {
      flex: 0 0 62px;
      color: var(--ink-soft);
    }

    b {
      flex: 1;
      color: var(--ink);
      font-weight: 500;
      word-break: break-all;
    }
  }

  &__note {
    color: var(--ink-soft);
    font-size: 12.5px;
    line-height: 1.8;
  }

  &--pending {
    border-color: #f5dab1;
    background: linear-gradient(150deg, var(--warn-soft), var(--surface) 62%);

    .vh5-result__ico {
      color: var(--warn-text);
    }
  }

  &--approved {
    border-color: #c2e7b0;
    background: linear-gradient(150deg, var(--lime-soft), var(--surface) 62%);

    .vh5-result__ico {
      color: var(--leaf-text);
    }
  }

  &--rejected {
    border-color: #fbc4c4;
    background: linear-gradient(150deg, var(--red-soft), var(--surface) 62%);

    .vh5-result__ico {
      color: var(--red-text);
    }
  }

  &--none {
    border-color: var(--line-strong);
    background: linear-gradient(150deg, var(--groove-bg), var(--surface) 62%);

    .vh5-result__ico {
      color: var(--ink-soft);
    }
  }
}

/* ---------- 底部提示 / 骨架 ---------- */
.vh5-tip {
  margin: 16px 18px 0;
  color: var(--ink-soft);
  font-size: 11.5px;
  line-height: 1.7;
  text-align: center;
}

.vh5-gap {
  height: 24px;
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
