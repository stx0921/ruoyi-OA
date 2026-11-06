/**
 * 全局状态色唯一色源
 *
 * 背景：状态色原先分散在 utils/oaTask.js（任务 / 优先级 / 周期 / 项目）、
 * utils/oa.js（请假）、utils/oaMember.js（成员 / 入会申请）三处，且大量 hex
 * 通过内联 :style 传给页面，暗色模式下无法跟随主题切换。
 *
 * 约定：
 * - 语义色调固定为五档：blue（选择 / 进行中）、lime（成功 / 已完成）、
 *   red（危险 / 驳回）、warn（待处理 / 待验收）、neutral（默认 / 已结束 / 已取消）
 * - 颜色只引用 ui.scss 的令牌（:root 与 html.dark 各定义一份），本文件不出现 hex
 * - 任务 / 周期 / 项目 / 站内信等映射仍保留 color / bg / border 三个字段名，
 *   页面上的 :style="{ color: meta.color, background: meta.bg, borderColor: meta.border }"
 *   无需改动即可获得暗色适配
 * - 旧类名 .oa-pill--green / --orange / --gray 是 17 个 OA 页面在用的契约，
 *   只在取值时归一化为 lime / warn / neutral，不重命名类名
 *
 * 注意：本文件只放映射与纯函数，不依赖任何页面组件，PC / H5 均可复用。
 */

/** 五档语义色调 → 内联样式令牌（color / bg / border 与既有 map 字段一一对应） */
export const TONE_META = {
  blue: { color: 'var(--blue)', bg: 'var(--blue-soft)', border: 'var(--line-strong)' },
  lime: { color: 'var(--lime-text)', bg: 'var(--lime-soft)', border: 'var(--line-strong)' },
  red: { color: 'var(--red-text)', bg: 'var(--red-soft)', border: 'var(--line-strong)' },
  warn: { color: 'var(--warn-text)', bg: 'var(--warn-soft)', border: 'var(--line-strong)' },
  neutral: { color: 'var(--neutral-text)', bg: 'var(--neutral-soft)', border: 'var(--line-strong)' }
}

/** 旧命名 → 语义色调（兼容 .oa-pill--green / --orange / --gray） */
export const TONE_ALIAS = { green: 'lime', orange: 'warn', gray: 'neutral' }

/** 语义色调 → 既有 .oa-pill--xxx 类名（保持页面 class 契约不变） */
export const TONE_PILL_CLASS = { blue: 'blue', lime: 'green', red: 'red', warn: 'orange', neutral: 'gray' }

/** 归一化色调：兼容旧命名，未知值收敛到 neutral */
export function normalizeTone(tone) {
  const key = TONE_ALIAS[tone] || tone
  return TONE_META[key] ? key : 'neutral'
}

/**
 * 取某色调的 { color, bg, border }
 * 每次返回新对象，避免调用方改写共享常量。
 */
export function toneMeta(tone) {
  return { ...TONE_META[normalizeTone(tone)] }
}

/** 取某色调主色（优先级色条等只需要单色的场景） */
export function toneColor(tone) {
  return TONE_META[normalizeTone(tone)].color
}

/** 取某色调对应的药丸类名，例如 pillToneClass('green') → 'oa-pill--green' */
export function pillToneClass(tone) {
  return 'oa-pill--' + TONE_PILL_CLASS[normalizeTone(tone)]
}
