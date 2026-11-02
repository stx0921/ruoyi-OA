<template>
  <div class="h5-stage">
    <div class="h5-shell">
      <!-- 手机状态栏：仅桌面预览时展示，真机由系统状态栏承担 -->
      <div class="h5-statusbar" aria-hidden="true">
        <span class="h5-statusbar__time">9:41</span>
        <span class="h5-statusbar__sigs">
          <span class="h5-sig">
            <i></i><i></i><i></i><i></i>
          </span>
          <span>5G</span>
          <span class="h5-battery"><i></i></span>
        </span>
      </div>

      <!-- 页面内容：由子路由渲染；如需在壳内直接投放内容可用默认插槽 -->
      <div class="h5-view">
        <slot>
          <router-view />
        </slot>
      </div>

      <!-- 可选底部 Tab（如页面需在壳内自定义 Tab 栏可传入；默认不渲染） -->
      <slot name="tabbar"></slot>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'H5Layout' })
</script>

<style lang="scss">
/* ==========================================================================
   H5 学生端 / 干事端外壳与公共样式
   - 390 × 844 手机壳，桌面预览居中，≤430px 时铺满整屏（100dvh 兜底）
   - 只放 h5- 前缀类名，不影响 PC 端页面
   ========================================================================== */

.h5-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 26px 16px;
  background: #e9edf2;
}

.h5-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 390px;
  height: 812px;
  max-height: calc(100vh - 52px);
  max-height: calc(100dvh - 52px);
  overflow: hidden;
  background: #fff;
  border-radius: 40px;
  box-shadow: 0 18px 50px rgba(20, 30, 50, 0.28), 0 0 0 10px #1f2d3d;
}

/* ---------- 模拟状态栏 ---------- */
.h5-statusbar {
  display: flex;
  flex: 0 0 38px;
  align-items: center;
  justify-content: space-between;
  height: 38px;
  padding: 0 24px;
  background: #fff;
  color: #111;
  font-size: 12px;
  font-weight: 600;
}

.h5-statusbar__sigs {
  display: flex;
  align-items: center;
  gap: 5px;
}

.h5-sig {
  display: inline-flex;
  align-items: flex-end;
  gap: 2px;
  height: 10px;

  i {
    display: block;
    width: 3px;
    border-radius: 1px;
    background: #111;

    &:nth-child(1) { height: 4px; }
    &:nth-child(2) { height: 6px; }
    &:nth-child(3) { height: 8px; }
    &:nth-child(4) { height: 10px; }
  }
}

.h5-battery {
  position: relative;
  width: 22px;
  height: 11px;
  margin-left: 3px;
  border: 1px solid #111;
  border-radius: 2px;

  &::after {
    content: '';
    position: absolute;
    top: 3px;
    right: -3px;
    width: 2px;
    height: 5px;
    border-radius: 0 1px 1px 0;
    background: #111;
  }

  i {
    position: absolute;
    inset: 1px 6px 1px 1px;
    border-radius: 1px;
    background: #111;
  }
}

/* ---------- 视口 / 页面容器 ---------- */
.h5-view {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.h5-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #f5f7fa;
}

/* ---------- 顶部导航 ---------- */
.h5-nav {
  position: relative;
  z-index: 2;
  display: flex;
  flex: 0 0 44px;
  align-items: center;
  height: 44px;
  padding: 0 14px;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
}

.h5-nav__back {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  width: 30px;
  height: 44px;
  padding: 0;
  border: none;
  background: none;
  color: #606266;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.h5-nav__title {
  position: absolute;
  top: 50%;
  left: 50%;
  max-width: 60%;
  overflow: hidden;
  transform: translate(-50%, -50%);
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.h5-nav__side {
  margin-left: auto;
  padding: 0;
  border: none;
  background: none;
  color: #409eff;
  font-size: 13px;
  cursor: pointer;
}

.h5-nav__side--muted {
  color: #909399;
}

/* ---------- 滚动区 ---------- */
.h5-scroll {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  background: #f5f7fa;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain;

  &::-webkit-scrollbar {
    width: 0;
    height: 0;
  }
}

/* ---------- 主色渐变介绍区 ---------- */
.h5-hero {
  position: relative;
  padding: 20px 18px 18px;
  overflow: hidden;
  background: linear-gradient(135deg, #409eff, #79bbff 70%, #9fcaff);
  color: #fff;

  &::after {
    content: '';
    position: absolute;
    top: -30px;
    right: -30px;
    width: 130px;
    height: 130px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.14);
  }
}

.h5-hero__org {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.h5-hero__logo {
  display: flex;
  flex: 0 0 38px;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.92);
  color: #337ecc;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.15;
  text-align: center;
}

.h5-hero__meta {
  font-size: 12px;
  line-height: 1.6;
  opacity: 0.94;
}

.h5-hero h2 {
  position: relative;
  z-index: 1;
  margin: 0 0 8px;
  font-size: 19px;
  font-weight: 600;
}

.h5-hero p {
  position: relative;
  z-index: 1;
  margin: 0;
  font-size: 12.5px;
  line-height: 1.75;
  opacity: 0.94;
}

/* ---------- 卡片 ---------- */
.h5-card {
  margin: 10px 12px;
  padding: 14px;
  border-radius: 8px;
  background: #fff;
}

.h5-card--flat {
  padding: 0;
  overflow: hidden;
}

.h5-sec-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 2px 0 12px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;

  &::before {
    content: '';
    width: 3px;
    height: 13px;
    border-radius: 2px;
    background: #409eff;
  }
}

.h5-sec-title__tip {
  margin-left: auto;
  color: #909399;
  font-size: 11px;
  font-weight: 400;
}

/* ---------- 表单 ---------- */
.h5-field {
  margin-bottom: 13px;

  &:last-child {
    margin-bottom: 0;
  }
}

.h5-field__label {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-bottom: 6px;
  color: #606266;
  font-size: 13px;

  small {
    margin-left: auto;
    color: #909399;
    font-size: 11px;
    font-weight: 400;
  }
}

.h5-req {
  color: #f56c6c;
}

.h5-input,
.h5-textarea,
.h5-select {
  box-sizing: border-box;
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  color: #303133;
  font-family: inherit;
  font-size: 16px;
  outline: none;
  transition: border-color 0.15s;

  &::placeholder {
    color: #c0c4cc;
  }

  &:focus {
    border-color: #409eff;
  }

  &.is-error {
    border-color: #f56c6c;
  }

  &.is-readonly,
  &[readonly] {
    background: #f5f7fa;
    color: #606266;
  }
}

.h5-input,
.h5-select {
  height: 44px;
  padding: 0 12px;
  line-height: 42px;
}

.h5-textarea {
  min-height: 84px;
  padding: 10px 12px;
  line-height: 1.6;
  resize: none;
}

.h5-select {
  padding-right: 34px;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath d='M1 1l5 5 5-5' fill='none' stroke='%23c0c4cc' stroke-width='1.5' stroke-linecap='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  appearance: none;
  -webkit-appearance: none;
}

.h5-field__row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.h5-err {
  margin-top: 4px;
  color: #f56c6c;
  font-size: 12px;
  line-height: 1.5;
}

.h5-hint {
  margin-top: 6px;
  color: #909399;
  font-size: 11.5px;
  line-height: 1.6;
}

/* 图形验证码 */
.h5-captcha {
  display: block;
  flex: 0 0 112px;
  width: 112px;
  height: 44px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #f5f7fa;
  cursor: pointer;
  object-fit: cover;
}

.h5-captcha--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
}

/* 单选 / 开关式选项 */
.h5-radio-row {
  display: flex;
  align-items: center;
  gap: 18px;
  min-height: 44px;
}

.h5-radio {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: none;
  background: none;
  color: #606266;
  font-size: 15px;
  cursor: pointer;

  i {
    position: relative;
    display: block;
    width: 17px;
    height: 17px;
    border: 1px solid #dcdfe6;
    border-radius: 50%;
    background: #fff;
  }

  &.is-on {
    color: #303133;

    i {
      border-color: #409eff;
    }

    i::after {
      content: '';
      position: absolute;
      inset: 3px;
      border-radius: 50%;
      background: #409eff;
    }
  }
}

/* 部门多选 */
.h5-dept-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.h5-dept {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 42px;
  padding: 0 10px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  color: #606266;
  font-size: 14px;
  text-align: left;
  cursor: pointer;

  &__check {
    display: inline-flex;
    flex: 0 0 16px;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    border: 1px solid #dcdfe6;
    border-radius: 3px;
    color: transparent;
    font-size: 11px;
    line-height: 1;
  }

  &.is-on {
    border-color: #409eff;
    background: #ecf5ff;
    color: #337ecc;

    .h5-dept__check {
      border-color: #409eff;
      background: #409eff;
      color: #fff;
    }
  }
}

/* 附件上传 */
.h5-upload-row {
  display: flex;
  gap: 10px;
}

.h5-upload {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 100px;
  padding: 8px;
  border: 1px dashed #c6d3e2;
  border-radius: 8px;
  background: #fafbfd;
  color: #909399;
  font-size: 12px;
  text-align: center;
  cursor: pointer;
  overflow: hidden;

  &__ico {
    color: #a8bdd6;
    font-size: 22px;
    line-height: 1;
  }

  &__note {
    color: #c0c4cc;
    font-size: 11px;
  }

  &__thumb {
    width: 100%;
    height: 78px;
    border-radius: 4px;
    object-fit: cover;
  }

  &__file {
    max-width: 100%;
    overflow: hidden;
    color: #606266;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__bar {
    position: absolute;
    right: 0;
    bottom: 0;
    left: 0;
    height: 3px;
    background: #ebeef5;
  }

  &__bar-inner {
    height: 100%;
    background: #409eff;
    transition: width 0.2s;
  }

  &__del {
    position: absolute;
    top: 4px;
    right: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border: none;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.45);
    color: #fff;
    font-size: 12px;
    line-height: 1;
    cursor: pointer;
  }
}

.h5-file-input {
  display: none;
}

/* 协议勾选 */
.h5-agree {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 2px 4px 0;
  color: #606266;
  font-size: 12.5px;
  line-height: 1.7;
  cursor: pointer;

  input {
    flex: 0 0 16px;
    width: 16px;
    height: 16px;
    margin: 3px 0 0;
    accent-color: #409eff;
  }
}

.h5-agree__link {
  padding: 0;
  border: none;
  background: none;
  color: #409eff;
  font-size: inherit;
  cursor: pointer;
}

/* ---------- 步骤条 ---------- */
.h5-steps {
  display: flex;
  align-items: center;
  margin: 10px 12px 0;
  padding: 12px 10px;
  border-radius: 8px;
  background: #fff;
}

.h5-step {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 5px;
  min-width: 0;
  color: #c0c4cc;
  font-size: 11.5px;

  &__dot {
    display: inline-flex;
    flex: 0 0 20px;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border: 1px solid #dcdfe6;
    border-radius: 50%;
    font-size: 11px;
    line-height: 1;
  }

  &__label {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__line {
    flex: 1;
    height: 1px;
    margin: 0 4px;
    background: #ebeef5;
  }

  &.is-active {
    color: #409eff;
    font-weight: 600;

    .h5-step__dot {
      border-color: #409eff;
      color: #409eff;
    }
  }

  &.is-done {
    color: #606266;

    .h5-step__dot {
      border-color: #409eff;
      background: #409eff;
      color: #fff;
    }
  }
}

/* ---------- 摘要（确认页） ---------- */
.h5-summary {
  &__row {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 9px 0;
    border-bottom: 1px dashed #f0f2f5;
    font-size: 13px;
    line-height: 1.6;

    &:last-child {
      border-bottom: none;
    }

    > span {
      flex: 0 0 84px;
      color: #909399;
    }

    > b {
      flex: 1;
      min-width: 0;
      color: #303133;
      font-weight: 400;
      word-break: break-all;
    }

    > button {
      flex: 0 0 auto;
      padding: 0;
      border: none;
      background: none;
      color: #409eff;
      font-size: 12px;
      cursor: pointer;
    }
  }
}

/* ---------- 底部固定操作条 / 主按钮 ---------- */
.h5-submit-bar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  padding: 10px 14px calc(10px + env(safe-area-inset-bottom));
  border-top: 1px solid #ebeef5;
  background: #fff;
}

.h5-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 44px;
  padding: 0 16px;
  border: none;
  border-radius: 22px;
  background: linear-gradient(90deg, #409eff, #66b1ff);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.32);
  color: #fff;
  font-family: inherit;
  font-size: 16px;
  letter-spacing: 1px;
  cursor: pointer;

  &:active {
    opacity: 0.9;
  }

  &:disabled {
    box-shadow: none;
    opacity: 0.55;
    cursor: not-allowed;
  }

  &.is-plain {
    border: 1px solid #409eff;
    background: #fff;
    box-shadow: none;
    color: #409eff;
  }

  &.is-sm {
    height: 36px;
    border-radius: 18px;
    font-size: 14px;
  }
}

.h5-btn-text {
  flex: 0 0 auto;
  height: 44px;
  padding: 0 14px;
  border: none;
  background: none;
  color: #606266;
  font-family: inherit;
  font-size: 14px;
  cursor: pointer;
}

.h5-tip-line {
  margin-top: 10px;
  color: #909399;
  font-size: 11.5px;
  line-height: 1.6;
  text-align: center;
}

/* ---------- 状态标签（配色对齐设计细节 §4.2） ---------- */
.h5-status-pill {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border: 1px solid transparent;
  border-radius: 11px;
  font-size: 11.5px;
  line-height: 1;
  white-space: nowrap;

  &--orange {
    border-color: #f5dab1;
    background: #fdf6ec;
    color: #e6a23c;
  }

  &--green {
    border-color: #c2e7b0;
    background: #f0f9eb;
    color: #67c23a;
  }

  &--red {
    border-color: #fbc4c4;
    background: #fef0f0;
    color: #f56c6c;
  }

  &--gray {
    border-color: #d3d4d6;
    background: #f4f4f5;
    color: #909399;
  }

  &--blue {
    border-color: #b3d8ff;
    background: #ecf5ff;
    color: #409eff;
  }
}

/* ---------- 申请卡片 ---------- */
.h5-app-item {
  margin: 10px 12px;
  padding: 14px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.h5-app-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;

  &__dept {
    color: #303133;
    font-size: 15px;
    font-weight: 600;
  }

  &__no {
    margin-top: 4px;
    color: #909399;
    font-family: Consolas, Menlo, monospace;
    font-size: 11.5px;
  }
}

.h5-app-meta {
  color: #909399;
  font-size: 12px;
  line-height: 1.8;
}

.h5-app-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}

.h5-app-actions--start {
  justify-content: flex-start;
}

.h5-reason {
  margin-top: 10px;
  padding: 9px 10px;
  border-left: 2px solid #f56c6c;
  border-radius: 4px;
  background: #fef0f0;
  color: #606266;
  font-size: 12.5px;
  line-height: 1.7;

  b {
    color: #f56c6c;
  }

  &--gray {
    border-left-color: #c0c4cc;
    background: #f4f4f5;

    b {
      color: #909399;
    }
  }

  &__meta {
    margin-top: 4px;
    color: #909399;
    font-size: 11.5px;
  }
}

.h5-account-hint {
  margin-top: 10px;
  padding: 9px 10px;
  border-left: 2px solid #67c23a;
  border-radius: 4px;
  background: #f0f9eb;
  color: #606266;
  font-size: 12.5px;
  line-height: 1.7;

  b {
    color: #67c23a;
  }
}

/* ---------- 按钮（次要 / 描边小按钮） ---------- */
.h5-btn-sm {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 84px;
  height: 34px;
  padding: 0 14px;
  border: 1px solid #dcdfe6;
  border-radius: 17px;
  background: #fff;
  color: #606266;
  font-family: inherit;
  font-size: 13px;
  cursor: pointer;

  &--primary {
    border-color: #409eff;
    color: #409eff;
  }

  &--danger {
    border-color: #f56c6c;
    color: #f56c6c;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

/* ---------- 通讯录 ---------- */
.h5-search {
  position: sticky;
  top: 0;
  z-index: 3;
  padding: 10px 12px;
  background: #f5f7fa;
}

.h5-search__box {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 12px;
  border-radius: 20px;
  background: #fff;
}

.h5-search__input {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0;
  border: none;
  background: none;
  color: #303133;
  font-family: inherit;
  font-size: 16px;
  outline: none;

  &::placeholder {
    color: #c0c4cc;
  }
}

.h5-contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #f2f4f7;
  background: #fff;
  cursor: pointer;

  &:last-child {
    border-bottom: none;
  }
}

.h5-ava {
  display: flex;
  flex: 0 0 38px;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, #79bbff, #409eff);
  color: #fff;
  font-size: 14px;

  &--lg {
    flex: 0 0 56px;
    width: 56px;
    height: 56px;
    background: rgba(255, 255, 255, 0.92);
    color: #337ecc;
    font-size: 20px;
    font-weight: 600;
  }
}

.h5-contact-main {
  flex: 1;
  min-width: 0;
}

.h5-contact-name {
  margin-bottom: 2px;
  color: #303133;
  font-size: 14px;
}

.h5-contact-pos {
  margin-left: 6px;
  color: #909399;
  font-size: 11.5px;
}

.h5-contact-me {
  display: inline-block;
  margin-left: 6px;
  padding: 0 5px;
  border-radius: 3px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 10.5px;
  line-height: 16px;
}

.h5-contact-sub {
  color: #909399;
  font-size: 12.5px;
  line-height: 1.6;
}

.h5-contact-dept {
  flex: 0 0 auto;
  color: #c0c4cc;
  font-size: 11.5px;
}

.h5-contact-detail {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #f0f2f5;
  color: #606266;
  font-size: 12px;
  line-height: 1.9;

  span {
    display: inline-block;
    width: 40px;
    color: #909399;
  }
}

.h5-contact-caret {
  flex: 0 0 auto;
  color: #c0c4cc;
  font-size: 16px;
  transition: transform 0.15s;

  &.is-open {
    transform: rotate(90deg);
  }
}

/* ---------- 个人资料 ---------- */
.h5-profile-head {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 14px;
  padding: 20px 18px 18px;
  background: linear-gradient(135deg, #409eff, #79bbff);
  color: #fff;

  &__name {
    margin-bottom: 4px;
    font-size: 18px;
    font-weight: 600;
  }

  &__sub {
    font-size: 12px;
    line-height: 1.7;
    opacity: 0.92;
  }
}

.h5-readonly-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;

  &__item {
    padding: 8px 10px;
    border-radius: 6px;
    background: #f5f7fa;
    font-size: 13px;
    line-height: 1.5;

    span {
      display: block;
      margin-bottom: 2px;
      color: #909399;
      font-size: 11px;
    }

    b {
      color: #303133;
      font-weight: 400;
      word-break: break-all;
    }
  }
}

.h5-photo-field {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 4px 0;
}

.h5-photo {
  display: flex;
  flex: 0 0 76px;
  align-items: center;
  justify-content: center;
  width: 76px;
  height: 96px;
  overflow: hidden;
  border: 1px dashed #c6d3e2;
  border-radius: 8px;
  background: #fafbfd;
  color: #a8bdd6;
  font-size: 22px;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.h5-photo__meta {
  flex: 1;
  min-width: 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

/* ---------- Tab 栏 ---------- */
.h5-tabbar {
  display: flex;
  flex: 0 0 auto;
  border-top: 1px solid #ebeef5;
  background: #fff;
  padding-bottom: env(safe-area-inset-bottom);
}

.h5-tab {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 56px;
  padding: 0;
  border: none;
  background: none;
  color: #909399;
  font-family: inherit;
  font-size: 11px;
  cursor: pointer;

  &__ico {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    line-height: 1;
  }

  &.is-active {
    color: #409eff;
  }
}

/* ---------- 底部弹层 ---------- */
.h5-sheet-mask {
  position: absolute;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: flex-end;
  background: rgba(0, 0, 0, 0.45);
}

.h5-sheet {
  width: 100%;
  padding: 20px 18px calc(18px + env(safe-area-inset-bottom));
  border-radius: 16px 16px 0 0;
  background: #fff;

  &__title {
    margin-bottom: 14px;
    color: #303133;
    font-size: 16px;
    font-weight: 600;
    text-align: center;
  }

  &__row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 12px;
    padding: 11px 0;
    border-bottom: 1px solid #f2f4f7;
    font-size: 13px;

    &:last-of-type {
      border-bottom: none;
    }

    span {
      flex: 0 0 76px;
      color: #909399;
    }

    b {
      flex: 1;
      color: #303133;
      font-weight: 600;
      text-align: right;
      word-break: break-all;
    }
  }

  &__actions {
    margin-top: 16px;
  }
}

/* ---------- 空态 / 加载骨架 / 错误 ---------- */
.h5-empty {
  padding: 52px 20px 40px;
  color: #909399;
  text-align: center;

  &__title {
    margin-bottom: 8px;
    color: #303133;
    font-size: 14px;
  }

  &__sub {
    margin-bottom: 18px;
    font-size: 12px;
    line-height: 1.7;
  }

  &__action {
    min-width: 140px;
    width: auto;
  }
}

.h5-skeleton {
  margin: 10px 12px;
  padding: 14px;
  border-radius: 8px;
  background: #fff;
}

.h5-skel-line {
  height: 12px;
  margin-bottom: 10px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f2f5 25%, #f8fafc 37%, #f0f2f5 63%);
  background-size: 400% 100%;
  animation: h5-skel 1.3s ease infinite;

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
    height: 64px;
  }
}

@keyframes h5-skel {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

/* ---------- Element Plus 移动端适配 ---------- */
.h5-shell {
  .el-input__inner,
  .el-textarea__inner {
    font-size: 16px;
  }

  .el-message-box,
  .el-dialog {
    max-width: calc(100vw - 44px);
  }
}

/* MessageBox / Dialog 挂载在 body 上，需全局约束宽度（移动端 320px） */
.h5-message-box {
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

.h5-dialog {
  width: 320px !important;
  max-width: calc(100vw - 40px);
  border-radius: 12px;
}

/* ---------- 小屏：真机全屏 ---------- */
@media (max-width: 430px) {
  .h5-stage {
    display: block;
    padding: 0;
    background: #f5f7fa;
  }

  .h5-shell {
    width: 100%;
    height: 100vh;
    height: 100dvh;
    max-height: none;
    border-radius: 0;
    box-shadow: none;
  }

  .h5-statusbar {
    display: none;
  }

  .h5-nav {
    flex: 0 0 auto;
    height: auto;
    min-height: calc(44px + env(safe-area-inset-top));
    padding-top: env(safe-area-inset-top);
    padding-bottom: 0;
  }

  .h5-nav__back {
    height: 44px;
  }

  .h5-nav__title {
    top: env(safe-area-inset-top);
    height: 44px;
    line-height: 44px;
    transform: translateX(-50%);
  }
}
</style>
