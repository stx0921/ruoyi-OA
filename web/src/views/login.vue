<template>
  <div class="login-page" :style="{ backgroundImage: `linear-gradient(105deg, rgba(6, 18, 38, 0.72) 0%, rgba(6, 18, 38, 0.42) 42%, rgba(6, 18, 38, 0.16) 72%, rgba(6, 18, 38, 0.28) 100%), url(${loginBg})` }">
    <div class="login-glow"></div>

    <main class="login-shell">
      <!-- 左侧品牌区（移动端隐藏） -->
      <section class="login-brand">
        <div class="brand-badge">
          <span class="brand-badge__icon">
            <el-icon><DataBoard /></el-icon>
          </span>
          <span>RuoYi-OA</span>
        </div>
        <h1 class="brand-headline">让每一次协作<br />都清晰可见</h1>
        <ul class="brand-points">
          <li><el-icon><CircleCheck /></el-icon><span>人员入会、档案与权限分层</span></li>
          <li><el-icon><CircleCheck /></el-icon><span>任务派发、看板与周期提醒</span></li>
        </ul>
      </section>

      <!-- 右侧登录卡片 -->
      <div
        ref="loginCard"
        class="login-card-wrap"
        @pointermove="onCardPointerMove"
        @pointerleave="resetCardTilt"
      >
        <section class="login-card" :class="{ 'is-shake': shake, 'is-success': succeeded }">
        <header class="login-card__head">
          <div class="login-card__logo">
            <el-icon><DataBoard /></el-icon>
          </div>
          <h2>欢迎回来</h2>
          <p>登录 {{ title }}，继续今天的工作</p>
        </header>

        <el-form
          ref="loginRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          autocomplete="off"
          @submit.prevent
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              type="text"
              size="large"
              autocomplete="off"
              placeholder="请输入账号"
            >
              <template #prefix><el-icon class="input-icon"><User /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              size="large"
              autocomplete="new-password"
              placeholder="请输入密码"
              show-password
              @keyup.enter="handleLogin"
            >
              <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="code" v-if="captchaEnabled">
            <div class="captcha-row">
              <el-input
                v-model="loginForm.code"
                size="large"
                autocomplete="off"
                placeholder="请输入验证码"
                @keyup.enter="handleLogin"
              >
                <template #prefix><el-icon class="input-icon"><Key /></el-icon></template>
              </el-input>
              <div class="login-code" @click="getCode" title="点击刷新验证码">
                <img :src="codeUrl" alt="验证码" />
                <span class="login-code__refresh">换一张</span>
              </div>
            </div>
          </el-form-item>

          <div class="login-options">
            <el-checkbox v-model="loginForm.rememberMe" @change="onRememberChange">记住密码</el-checkbox>
            <el-tooltip content="仅在本机 Cookie 保存 30 天，公用电脑请勿勾选" placement="top">
              <span class="remember-hint">
                <el-icon><InfoFilled /></el-icon>
                仅本机保存
              </span>
            </el-tooltip>
          </div>

          <el-button
            v-if="!succeeded"
            ref="loginBtn"
            class="login-submit"
            type="primary"
            size="large"
            :loading="loading"
            @click.prevent="handleLogin"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登 录 中...</span>
            <el-icon v-if="!loading" class="login-submit__icon"><ArrowRight /></el-icon>
          </el-button>
          <div v-else class="success-orb" role="status" aria-label="登录成功，正在进入">
            <svg width="30" height="30" viewBox="0 0 46 46" aria-hidden="true">
              <circle cx="23" cy="23" r="20" />
              <path d="m15 23.5 5.5 5.5L31.5 18" />
            </svg>
          </div>

          <div class="login-register" v-if="register">
            <router-link class="link-type" to="/register">还没有账号？立即注册</router-link>
          </div>
        </el-form>
      </section>
      </div>
    </main>

    <footer class="el-login-footer">
      <span>{{ footerContent }}</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { getCodeImg } from '@/api/login'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'
import { User, Lock, Key, ArrowRight, CircleCheck, DataBoard, InfoFilled } from '@element-plus/icons-vue'
import loginBg from '@/assets/images/login-bg.jpg'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: '',
  password: '',
  rememberMe: false,
  code: '',
  uuid: ''
})

const loginRules = {
  username: [{ required: true, trigger: 'blur', message: '请输入您的账号' }],
  password: [{ required: true, trigger: 'blur', message: '请输入您的密码' }],
  code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
}

const codeUrl = ref('')
const loading = ref(false)
const shake = ref(false)
const succeeded = ref(false)
const loginBtn = ref(null)
const loginCard = ref(null)
// 验证码开关
const captchaEnabled = ref(true)
// 注册开关
const register = ref(false)
const redirect = ref(undefined)

function prefersReducedMotion() {
  return typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
}

/** 登录卡随指针轻倾（≤ 4°），离开回正；只在可悬停设备上启用 */
function onCardPointerMove(event) {
  if (!loginCard.value || prefersReducedMotion()) return
  if (!window.matchMedia?.('(hover: hover)').matches) return
  const rect = loginCard.value.getBoundingClientRect()
  const px = (event.clientX - rect.left) / rect.width - 0.5
  const py = (event.clientY - rect.top) / rect.height - 0.5
  const rotateY = Math.max(-1, Math.min(1, px * 2)) * 4
  const rotateX = Math.max(-1, Math.min(1, py * 2)) * -3
  loginCard.value.style.transform =
    `perspective(1000px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg)`
  loginCard.value.style.setProperty('--mx', `${((px + 0.5) * 100).toFixed(1)}%`)
  loginCard.value.style.setProperty('--my', `${((py + 0.5) * 100).toFixed(1)}%`)
}

function resetCardTilt() {
  if (loginCard.value) {
    loginCard.value.style.transform = ''
  }
}

/** 主按钮涟漪：从实际点击位置化开；键盘触发落回中心 */
function fireRipple(event) {
  if (prefersReducedMotion()) return
  const button = loginBtn.value?.$el || loginBtn.value
  if (!button) return
  const rect = button.getBoundingClientRect()
  if (!rect.width || !rect.height) return
  let clip = button.querySelector('.ripple-clip')
  if (!clip) {
    clip = document.createElement('span')
    clip.className = 'ripple-clip'
    clip.setAttribute('aria-hidden', 'true')
    button.appendChild(clip)
  }
  const pointer = event && typeof event.clientX === 'number' && event.detail > 0
  const x = pointer ? event.clientX - rect.left : rect.width / 2
  const y = pointer ? event.clientY - rect.top : rect.height / 2
  const radius = Math.hypot(Math.max(x, rect.width - x), Math.max(y, rect.height - y))
  const dot = document.createElement('span')
  dot.className = 'ripple-dot'
  dot.style.left = `${x}px`
  dot.style.top = `${y}px`
  clip.appendChild(dot)
  const scale = Math.max((radius * 2.2) / 20, 1)
  const animation = dot.animate(
    [
      { transform: 'translate(-50%, -50%) scale(1)', opacity: 0.85 },
      { transform: `translate(-50%, -50%) scale(${scale})`, opacity: 0 }
    ],
    { duration: 680, easing: 'cubic-bezier(.16, 1, .3, 1)' }
  )
  animation.onfinish = () => dot.remove()
}

/** 登录成功三拍：按钮收拢成圆（放）→ 对勾圆环描出（结）→ 进入系统 */
async function playSuccess() {
  succeeded.value = true
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, prefersReducedMotion() ? 0 : 1050))
}

watch(route, (newRoute) => {
  redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

/** 记住密码：只在勾选时读取/写入 Cookie，未勾选时清空，避免自动填充 */
const REMEMBER_KEY = 'rememberMe'

function clearRememberCookies() {
  Cookies.remove('username')
  Cookies.remove('password')
  Cookies.remove(REMEMBER_KEY)
}

function getCookie() {
  const remembered = Cookies.get(REMEMBER_KEY)
  if (remembered !== '1' && remembered !== 'true') {
    // 兼容旧版本：曾把用户名单独存在 cookie 里，未勾选记住密码时一并清掉
    clearRememberCookies()
    return
  }
  const username = Cookies.get('username')
  const encrypted = Cookies.get('password')
  loginForm.value.rememberMe = true
  if (username) {
    loginForm.value.username = username
  }
  if (encrypted) {
    loginForm.value.password = decrypt(encrypted) || ''
  }
}

function onRememberChange(checked) {
  if (!checked) {
    clearRememberCookies()
  }
}

function saveRememberCookie() {
  if (loginForm.value.rememberMe) {
    Cookies.set('username', loginForm.value.username, { expires: 30, sameSite: 'lax' })
    Cookies.set('password', encrypt(loginForm.value.password), { expires: 30, sameSite: 'lax' })
    Cookies.set(REMEMBER_KEY, '1', { expires: 30, sameSite: 'lax' })
  } else {
    clearRememberCookies()
  }
}

function triggerShake() {
  shake.value = true
  setTimeout(() => { shake.value = false }, 520)
}

function handleLogin(event) {
  if (loading.value || succeeded.value) return
  fireRipple(event)
  proxy.$refs.loginRef.validate(async valid => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login(loginForm.value)
      saveRememberCookie()
      loading.value = false
      await playSuccess()
      const query = route.query
      const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
        if (cur !== 'redirect') {
          acc[cur] = query[cur]
        }
        return acc
      }, {})
      const failure = await router.push({ path: redirect.value || '/', query: otherQueryParams })
      if (failure || router.currentRoute.value.path === '/login') {
        succeeded.value = false
      }
    } catch (error) {
      loading.value = false
      succeeded.value = false
      triggerShake()
      if (captchaEnabled.value) {
        getCode()
      }
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = 'data:image/gif;base64,' + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

onMounted(() => {
  getCookie()
  getCode()
})

onBeforeUnmount(() => {
  resetCardTilt()
})
</script>

<style lang="scss" scoped>
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 48px 5vw 72px;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  overflow: hidden;
}

.login-glow {
  position: absolute;
  width: 760px;
  height: 760px;
  right: -180px;
  top: -260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 214, 120, 0.32), rgba(255, 214, 120, 0) 68%);
  pointer-events: none;
}

.login-shell {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  align-items: center;
  gap: clamp(32px, 6vw, 96px);
  width: min(1180px, 100%);
}

/* ---------------- 品牌区 ---------------- */
.login-brand {
  color: #fff;
  text-shadow: 0 2px 18px rgba(4, 14, 30, 0.45);
  animation: brand-in 0.8s ease both;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px 8px 10px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(10px);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;

  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: linear-gradient(135deg, #67c23a, #236de9);
    color: #fff;
    font-size: 16px;
  }
}

.brand-headline {
  margin: 28px 0 18px;
  font-size: clamp(34px, 4.2vw, 54px);
  line-height: 1.18;
  font-weight: 800;
  letter-spacing: 2px;
  color: #fff;
}

.brand-points {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.9);

    .el-icon {
      color: #95d475;
      font-size: 18px;
    }
  }
}

/* ---------------- 登录卡片 ---------------- */
.login-card-wrap {
  position: relative;
  width: 100%;
  perspective: 1000px;
  transform-style: preserve-3d;
  transition: transform 0.28s cubic-bezier(0.16, 1, 0.3, 1);
  will-change: transform;
}

.login-card {
  position: relative;
  width: 100%;
  padding: 40px 38px 30px;
  border: 1px solid var(--glass-line);
  border-radius: var(--r-xl);
  background: var(--surface);
  box-shadow: var(--edge), var(--e3);
  backdrop-filter: blur(18px) saturate(140%);
  animation: card-in 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.login-card__head {
  text-align: center;
  margin-bottom: 28px;

  h2 {
    margin: 14px 0 8px;
    font-size: 26px;
    font-weight: 700;
    color: var(--ink);
    letter-spacing: 1px;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: var(--ink-soft);
  }
}

.login-card__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: var(--r-md);
  background: linear-gradient(135deg, #2f7bf0, #236de9);
  color: #fff;
  font-size: 28px;
  box-shadow: var(--e-accent), inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  :deep(.el-input__wrapper) {
    height: 48px;
    padding: 0 14px;
    border-radius: var(--r-sm);
    background: var(--input-bg);
    box-shadow: 0 0 0 1px var(--line-strong) inset;
    transition: box-shadow 0.2s ease, background 0.2s ease;

    &.is-focus {
      background: var(--surface-solid);
      box-shadow: 0 0 0 1px var(--blue) inset, 0 0 0 3px var(--blue-soft);
    }
  }

  :deep(.el-input__inner) {
    font-size: 15px;
    color: var(--ink);
  }

  :deep(.el-form-item__error) {
    padding-top: 4px;
    font-size: 12px;
  }
}

.input-icon {
  color: #8b98a8;
  font-size: 17px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;

  .el-input {
    flex: 1;
  }
}

.login-code {
  position: relative;
  flex: 0 0 118px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: #eef2f8;
  box-shadow: 0 0 0 1px #e3e9f4 inset;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  &__refresh {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #fff;
    background: rgba(20, 32, 52, 0.5);
    opacity: 0;
    transition: opacity 0.2s ease;
  }

  &:hover &__refresh {
    opacity: 1;
  }
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -2px 0 22px;

  :deep(.el-checkbox__label) {
    font-size: 13px;
    color: #5b6b80;
  }
}

.remember-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #9aa7b8;
  cursor: help;
}

.login-submit {
  position: relative;
  width: 100%;
  height: 48px;
  overflow: hidden;
  border: none;
  border-radius: var(--r-sm);
  background: var(--blue);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  box-shadow: var(--e-accent);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    background: var(--blue-hover);
    box-shadow: var(--e-accent-hover);
  }

  &:active {
    transform: translateY(0);
  }

  &__icon {
    margin-left: 8px;
    font-size: 16px;
  }
}

/* 主按钮涟漪：从点击点化开 */
.ripple-clip {
  position: absolute;
  inset: 0;
  z-index: 1;
  overflow: hidden;
  border-radius: inherit;
  pointer-events: none;
}

.ripple-dot {
  position: absolute;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.42);
  pointer-events: none;
  will-change: transform, opacity;
}

/* 登录成功：按钮收拢成圆 → 圆环描出 → 对勾落定 */
.success-orb {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin: 0 auto;
  border-radius: 50%;
  background: var(--blue);
  color: #fff;
  box-shadow: var(--e-accent);
  animation: orb-collapse 0.42s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.success-orb svg {
  width: 30px;
  height: 30px;
  overflow: visible;
}

.success-orb circle {
  fill: none;
  stroke: #fff;
  stroke-width: 2;
  stroke-dasharray: 126;
  stroke-dashoffset: 126;
  animation: draw-ring 0.5s 0.28s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.success-orb path {
  fill: none;
  stroke: #fff;
  stroke-width: 2.6;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 24;
  stroke-dashoffset: 24;
  animation: draw-check 0.32s 0.52s ease forwards;
}

.login-card.is-success {
  box-shadow: var(--edge), var(--e3), 0 0 0 3px var(--blue-soft);
}

/* 指针跟随的极淡高光（材质层，不是可点承诺） */
.login-card::after {
  position: absolute;
  inset: 0;
  z-index: 0;
  border-radius: inherit;
  background: radial-gradient(180px circle at var(--mx, 50%) var(--my, 50%), rgba(255, 255, 255, 0.5), transparent 72%);
  content: "";
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
  mix-blend-mode: screen;
}

@media (hover: hover) {
  .login-card:hover::after {
    opacity: 0.55;
  }
}

.login-card > * {
  position: relative;
  z-index: 1;
}

@keyframes orb-collapse {
  from {
    width: 100%;
    height: 48px;
    border-radius: var(--r-sm);
  }
  to {
    width: 48px;
    height: 48px;
    border-radius: 50%;
  }
}

@keyframes draw-ring {
  to { stroke-dashoffset: 0; }
}

@keyframes draw-check {
  to { stroke-dashoffset: 0; }
}

@media (prefers-reduced-motion: reduce) {
  .success-orb,
  .success-orb circle,
  .success-orb path {
    animation: none;
  }

  .success-orb circle,
  .success-orb path {
    stroke-dashoffset: 0;
  }

  .login-card-wrap {
    transition: none;
  }
}

.login-register {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
}

.el-login-footer {
  position: absolute;
  bottom: 20px;
  left: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  letter-spacing: 1px;
  text-shadow: 0 1px 8px rgba(4, 14, 30, 0.4);
  z-index: 2;
}

/* 登录失败抖动 */
.is-shake {
  animation: shake 0.5s ease both;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(26px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes brand-in {
  from {
    opacity: 0;
    transform: translateX(-22px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-10px); }
  40% { transform: translateX(8px); }
  60% { transform: translateX(-6px); }
  80% { transform: translateX(4px); }
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: minmax(0, 430px);
    justify-content: center;
  }

  .login-brand {
    display: none;
  }

  .login-page {
    padding: 40px 20px 64px;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 30px 22px 24px;
    border-radius: 20px;
  }

  .login-code {
    flex-basis: 100px;
  }

  .login-card__head h2 {
    font-size: 22px;
  }
}

/* ---------------- 暗黑模式 ---------------- */
html.dark .login-card {
  background: rgba(26, 32, 44, 0.9);
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: 0 32px 80px rgba(0, 0, 0, 0.55);

  .login-card__head h2 {
    color: #eaf2ff;
  }

  .login-card__head p {
    color: #8d9bb0;
  }
}

html.dark .login-form {
  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.06);
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12) inset;

    &.is-focus {
      background: rgba(255, 255, 255, 0.1);
    }
  }

  :deep(.el-input__inner) {
    color: #eaf2ff;
  }
}

html.dark .login-options {
  :deep(.el-checkbox__label) {
    color: #a9b6c8;
  }
}
</style>
