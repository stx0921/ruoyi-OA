<template>
  <div class="ht-stage">
    <div class="ht-shell">
      <!-- 手机状态栏：仅桌面预览时展示，真机由系统状态栏承担 -->
      <div class="ht-statusbar" aria-hidden="true">
        <span class="ht-statusbar__time">9:41</span>
        <span class="ht-statusbar__sigs">
          <span class="ht-sig"><i></i><i></i><i></i><i></i></span>
          <span>5G</span>
          <span class="ht-battery"><i></i></span>
        </span>
      </div>

      <!-- 页面内容：flex 纵向布局（header / 滚动区 / 底部操作条） -->
      <div class="ht-page">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * H5 任务端独立外壳（390 × 844 手机壳）
 *
 * `/h5/task`、`/h5/task/detail/:id` 不在 `views/h5/layout` 之下（该布局底部 Tab 仅任务/我的），
 * 因此任务端两页各自套一层外壳；外壳样式与公共原子类统一放在本组件（全局 ht- 前缀），
 * 由 index.vue / detail.vue 共用，避免两页重复。
 */
defineOptions({ name: 'H5TaskShell' })
</script>

<style lang="scss">
/* ==========================================================================
   任务端 H5 手机壳 + 公共原子类
   - 只放 ht- 前缀类名，参照 views/h5/layout/index.vue 的 390px 手机壳方案
   - 小屏 / 触屏 / 横屏手机优先铺满整屏（100dvh），底部安全区用 env(safe-area-inset-bottom)
   ========================================================================== */

.ht-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 26px 16px;
  background: var(--stage);
}

.ht-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 390px;
  height: 812px;
  /* 上下 padding 30px × 2 = 60px，与 .ht-stage 实际 padding 一致，避免矮屏多出 8px 整页滚动 */
  max-height: calc(100vh - 60px);
  max-height: calc(100dvh - 60px);
  overflow: hidden;
  border-radius: 40px;
  background: var(--surface);
  box-shadow: 0 18px 50px rgba(20, 30, 50, 0.28), 0 0 0 10px var(--frame);
}

/* ---------- 模拟状态栏 ---------- */
.ht-statusbar {
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

.ht-statusbar__sigs {
  display: flex;
  align-items: center;
  gap: 5px;
}

.ht-sig {
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

.ht-battery {
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

/* ---------- 视口 / 页面容器 ---------- */
.ht-page {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  background: var(--canvas);
}

.ht-scroll {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  background: var(--canvas);
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain;

  &::-webkit-scrollbar {
    width: 0;
    height: 0;
  }
}

/* ---------- 顶部导航 ---------- */
.ht-nav {
  display: flex;
  flex: 0 0 44px;
  align-items: center;
  gap: 6px;
  height: 44px;
  padding: 0 8px 0 4px;
  border-bottom: 1px solid var(--line);
  background: var(--surface);
}

.ht-nav__back {
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

.ht-nav__title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: var(--ink);
  font-size: 16px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- 站内信铃铛 ---------- */
.ht-bell {
  position: relative;
  display: flex;
  flex: 0 0 40px;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  margin-left: auto;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: none;
  color: var(--ink-muted);
  cursor: pointer;

  svg {
    width: 21px;
    height: 21px;
  }

  &__badge {
    position: absolute;
    top: 3px;
    right: 1px;
    box-sizing: border-box;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 8px;
    background: #f56c6c;
    color: #fff;
    font-size: 10px;
    font-style: normal;
    font-weight: 600;
    line-height: 16px;
    text-align: center;
  }

  &__dot {
    position: absolute;
    top: 8px;
    right: 7px;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #f56c6c;
  }
}

/* ---------- 卡片 / 区块标题 ---------- */
.ht-card {
  margin: 10px 12px;
  padding: 14px;
  border-radius: 8px;
  background: var(--surface);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.ht-sec {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  padding: 0;
  border: none;
  background: none;
  color: var(--ink);
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  text-align: left;

  &::before {
    content: '';
    width: 3px;
    height: 13px;
    border-radius: 2px;
    background: var(--blue);
  }

  &__tip {
    color: var(--ink-soft);
    font-size: 11px;
    font-weight: 400;
  }

  &__tip--auto {
    margin-left: auto;
  }

  &__link {
    margin-left: auto;
    padding: 0;
    border: none;
    background: none;
    color: var(--blue);
    font-family: inherit;
    font-size: 12px;
    font-weight: 400;
    cursor: pointer;
  }

  &--toggle {
    width: 100%;
    margin-bottom: 0;
    cursor: pointer;
  }

  &__hint {
    margin-top: 10px;
    color: #e6a23c;
    font-size: 11.5px;
    line-height: 1.6;
  }
}

.ht-caret {
  display: inline-block;
  margin-left: auto;
  color: #c0c4cc;
  font-size: 16px;
  font-weight: 400;
  transition: transform 0.15s;

  &.is-open {
    transform: rotate(90deg);
  }
}

/* ---------- 项目标签 / 状态标签 / 优先级 ---------- */
.ht-proj {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  padding: 1px 6px;
  border: 1px solid #dbe7fc;
  border-radius: 3px;
  background: var(--blue-soft);
  color: var(--blue);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.ht-pill {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border: 1px solid transparent;
  border-radius: 3px;
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  vertical-align: middle;

  &--sm {
    height: 20px;
    padding: 0 6px;
    font-size: 11px;
  }

  &--gray {
    border-color: var(--line-strong);
    background: var(--groove-bg);
    color: var(--ink-soft);
  }

  &--strike {
    text-decoration: line-through;
  }
}

.ht-priority {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  line-height: 1;

  i {
    display: block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
}

.ht-overdue-badge {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  border: 1px solid #fbc4c4;
  border-radius: 2px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  vertical-align: middle;
}

/* ---------- 按钮 ---------- */
.ht-btn {
  display: inline-flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 44px;
  padding: 0 16px;
  border: none;
  border-radius: 22px;
  background: var(--groove-bg);
  color: var(--ink-muted);
  font-family: inherit;
  font-size: 15px;
  letter-spacing: 0.5px;
  cursor: pointer;

  &:active {
    opacity: 0.9;
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }

  &--primary {
    background: linear-gradient(90deg, var(--blue), #5b8ff0);
    box-shadow: 0 6px 16px rgba(35, 109, 233, 0.32);
    color: #fff;

    &:disabled {
      box-shadow: none;
    }
  }

  &--ghost {
    border: 1px solid var(--blue);
    background: var(--surface);
    color: var(--blue);
  }

  &--ghost-2 {
    border: 1px solid var(--line-strong);
    background: var(--surface);
    color: var(--ink-muted);
  }

  &--danger {
    background: #f56c6c;
    color: #fff;
  }

  &--danger-ghost {
    border: 1px solid #f56c6c;
    background: var(--surface);
    color: #f56c6c;
  }

  &--sm {
    flex: 0 0 auto;
    height: 34px;
    padding: 0 14px;
    border-radius: 17px;
    font-size: 13px;
  }
}

/* ---------- 底部固定操作条 / 提示条 ---------- */
.ht-action-bar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
  padding: 10px 14px calc(10px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--line);
  background: var(--surface);

  &__readonly {
    flex: 1;
    padding: 4px 0;
    color: var(--ink-soft);
    font-size: 13px;
    line-height: 1.6;
    text-align: center;
  }
}

.ht-hint-bar {
  flex: 0 0 auto;
  padding: 8px 14px;
  border-top: 1px solid #f5dab1;
  background: #fdf6ec;
  color: #b96a1e;
  font-size: 11.5px;
  line-height: 1.6;
}

.ht-hint {
  color: var(--ink-soft);
  font-size: 11.5px;
  line-height: 1.7;
}

.ht-req {
  color: #f56c6c;
}

.ht-err {
  margin-top: 6px;
  color: #f56c6c;
  font-size: 12px;
  line-height: 1.5;
}

/* ---------- 空态 / 骨架 ---------- */
.ht-empty {
  padding: 56px 24px 40px;
  color: var(--ink-soft);
  text-align: center;

  &__ico {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 60px;
    height: 60px;
    margin: 0 auto 14px;
    border-radius: 50%;
    background: var(--canvas);
    color: #c0c4cc;
    font-size: 26px;
    font-weight: 600;
  }

  &__title {
    margin-bottom: 8px;
    color: var(--ink);
    font-size: 14px;
  }

  &__sub {
    margin-bottom: 18px;
    color: var(--ink-soft);
    font-size: 12px;
    line-height: 1.7;
  }

  &__action {
    flex: 0 0 auto;
    min-width: 132px;
    width: auto;
    margin: 0 auto;
  }

  &--small {
    padding: 40px 20px;
  }
}

.ht-skeleton {
  margin: 10px 12px;
  padding: 14px;
  border-radius: 8px;
  background: var(--surface);
}

.ht-skel-line {
  height: 12px;
  margin-bottom: 10px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--hover-bg) 25%, var(--line-strong) 37%, var(--hover-bg) 63%);
  background-size: 400% 100%;
  animation: ht-skel 1.3s ease infinite;

  &:last-child {
    margin-bottom: 0;
  }

  &--wide {
    width: 70%;
  }

  &--short {
    width: 40%;
  }

  &--tall {
    height: 44px;
  }
}

@keyframes ht-skel {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

/* ---------- 底部 / 半屏弹层 ---------- */
.ht-sheet-mask {
  position: absolute;
  inset: 0;
  z-index: 90;
  display: flex;
  align-items: flex-end;
  background: rgba(0, 0, 0, 0.45);
}

.ht-sheet-panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-height: 70%;
  border-radius: 16px 16px 0 0;
  background: var(--surface);
  animation: ht-sheet-up 0.2s ease-out;

  &--tall {
    height: 70%;
  }

  &--half {
    height: 60%;
  }
}

@keyframes ht-sheet-up {
  from { transform: translateY(24px); opacity: 0.6; }
  to { transform: translateY(0); opacity: 1; }
}

.ht-sheet-head {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  padding: 16px 16px 10px;
  border-bottom: 1px solid var(--line);

  &__title {
    flex: 1;
    min-width: 0;
    color: var(--ink);
    font-size: 16px;
    font-weight: 600;
  }

  &__unread {
    margin-left: 6px;
    color: #f56c6c;
    font-size: 11.5px;
    font-weight: 400;
  }

  &__act {
    flex: 0 0 auto;
    padding: 0 2px;
    border: none;
    background: none;
    color: var(--blue);
    font-family: inherit;
    font-size: 12.5px;
    cursor: pointer;

    &:disabled {
      color: #c0c4cc;
      cursor: not-allowed;
    }
  }

  &__close {
    display: flex;
    flex: 0 0 26px;
    align-items: center;
    justify-content: center;
    width: 26px;
    height: 26px;
    padding: 0;
    border: none;
    border-radius: 50%;
    background: var(--groove-bg);
    color: var(--ink-soft);
    font-size: 16px;
    line-height: 1;
    cursor: pointer;
  }
}

.ht-sheet-body {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 12px 16px;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar {
    width: 0;
    height: 0;
  }
}

.ht-sheet-foot {
  display: flex;
  flex: 0 0 auto;
  gap: 10px;
  padding: 10px 16px calc(12px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--line);
}

/* ---------- 表单 ---------- */
.ht-field {
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }
}

.ht-label {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-bottom: 6px;
  color: var(--ink-muted);
  font-size: 13px;

  small {
    margin-left: auto;
    color: var(--ink-soft);
    font-size: 11px;
    font-weight: 400;
  }
}

.ht-textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 96px;
  padding: 10px 12px;
  border: 1px solid var(--line-strong);
  border-radius: 8px;
  background: var(--surface);
  color: var(--ink);
  font-family: inherit;
  font-size: 16px;
  line-height: 1.6;
  outline: none;
  resize: none;

  &::placeholder {
    color: #c0c4cc;
  }

  &:focus {
    border-color: var(--blue);
  }
}

.ht-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.ht-chip {
  padding: 5px 10px;
  border: 1px solid var(--line-strong);
  border-radius: 14px;
  background: var(--surface);
  color: var(--ink-muted);
  font-family: inherit;
  font-size: 12px;
  line-height: 1.5;
  cursor: pointer;

  &:active {
    border-color: var(--blue);
    color: var(--blue);
  }
}

/* ---------- 站内信列表（两页共用） ---------- */
.ht-notify-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  padding: 11px 0;
  border: none;
  border-bottom: 1px solid var(--line);
  background: none;
  font-family: inherit;
  text-align: left;
  cursor: pointer;

  &:last-child {
    border-bottom: none;
  }
}

.ht-notify-item__ico {
  display: flex;
  flex: 0 0 30px;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--blue-soft);
  color: var(--blue);
  font-size: 12px;
  font-weight: 600;

  &.is-type-2 { background: #f0f9eb; color: #67c23a; }
  &.is-type-3 { background: #fdf6ec; color: #e6a23c; }
  &.is-type-4 { background: #fef0f0; color: #f56c6c; }
  &.is-type-5 { background: var(--groove-bg); color: var(--ink-soft); }
}

.ht-notify-item__main {
  flex: 1;
  min-width: 0;
}

.ht-notify-item__title {
  display: block;
  overflow: hidden;
  color: var(--ink);
  font-size: 13px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ht-notify-item__content {
  display: block;
  margin-top: 2px;
  overflow: hidden;
  color: var(--ink-soft);
  font-size: 11.5px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ht-notify-item__time {
  display: block;
  margin-top: 3px;
  color: #c0c4cc;
  font-size: 11px;
  line-height: 1.5;
}

.ht-notify-item__dot {
  flex: 0 0 8px;
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--blue);
}

.ht-notify-item.is-unread .ht-notify-item__title {
  font-weight: 600;
}

.ht-notify-more {
  display: flex;
  justify-content: center;
  padding: 12px 0 4px;
}

/* ---------- Element Plus 移动端适配 ---------- */
.ht-message-box {
  box-sizing: border-box;
  width: 320px !important;
  max-width: calc(100vw - 48px);
  border-radius: 12px;

  .el-message-box__title {
    font-size: 16px;
  }

  .el-message-box__message {
    font-size: 14px;
    line-height: 1.7;
  }
}

/* ---------- 真机优先：小屏 / 触屏 / 横屏手机铺满整屏 ---------- */
@media (max-width: 480px), (pointer: coarse), (orientation: landscape) and (max-height: 520px) {
  .ht-stage {
    display: block;
    padding: 0;
    background: var(--canvas);
  }

  .ht-shell {
    width: 100%;
    height: 100vh;
    height: 100dvh;
    max-height: none;
    border-radius: 0;
    box-shadow: none;
  }

  .ht-statusbar {
    display: none;
  }

  .ht-nav {
    flex: 0 0 auto;
    height: auto;
    min-height: calc(44px + env(safe-area-inset-top));
    padding-top: env(safe-area-inset-top);
  }

  .ht-nav__back {
    height: 44px;
  }
}

/* ==========================================================================
   UI 优化覆盖（2026-11）：与 H5 人员端保持同一套珍珠玻璃视觉
   ========================================================================== */
.ht-stage {
  background:
    radial-gradient(55% 45% at 92% 0%, var(--ambient-a), transparent 70%),
    radial-gradient(45% 40% at 20% 100%, var(--ambient-b), transparent 70%),
    var(--stage);
  padding: 30px 18px;
}

.ht-shell {
  border-radius: 38px;
  background: var(--canvas);
  box-shadow:
    0 24px 70px rgba(17, 23, 27, 0.24),
    0 3px 12px rgba(17, 23, 27, 0.16),
    0 0 0 8px var(--frame);
}

.ht-page {
  background:
    radial-gradient(70% 45% at 90% 0%, var(--ambient-a), transparent 70%),
    var(--canvas);
}

.ht-nav {
  border-bottom: 1px solid var(--line);
  background: var(--rail-bg);
  backdrop-filter: blur(22px) saturate(1.1);
  -webkit-backdrop-filter: blur(22px) saturate(1.1);
  box-shadow: 0 1px 0 var(--line-strong);
}

.ht-card {
  border: 1px solid var(--line-strong);
  border-radius: var(--r-md);
  background: var(--surface);
  box-shadow: var(--edge), var(--e1);
}

.ht-btn {
  border-radius: var(--r-sm);
}

.ht-btn--primary {
  background: var(--blue);
  box-shadow: var(--e-accent);
}

.ht-sheet-panel {
  border-radius: var(--r-xl) var(--r-xl) 0 0;
  background: var(--surface);
  box-shadow: 0 -18px 50px rgba(17, 23, 27, 0.18);
}

.ht-action-bar {
  border-top: 1px solid var(--line);
  background: var(--rail-bg);
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
}

@media (max-width: 480px), (pointer: coarse), (orientation: landscape) and (max-height: 520px) {
  .ht-stage {
    padding: 0;
    background: var(--canvas);
  }

  .ht-shell {
    border-radius: 0;
    box-shadow: none;
  }
}
</style>
