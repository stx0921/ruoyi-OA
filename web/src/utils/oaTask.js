/**
 * 任务协作前端公共工具（契约 04-开发实施契约 §5.1）
 *
 * 收敛三类内容，页面只负责拼装与展示：
 * - 状态 / 优先级 / 来源 / 周期规则 / 项目 / 站内信的文案与配色（02 设计细节 §5）
 * - 逾期天数、时间格式、标签解析、附件大小等纯函数
 * - 看板列定义、拖拽落位规则、任务卡按钮可见性判断（契约 §3.4 canX 口径）
 *
 * 注意：本文件不依赖任何页面组件，可被 PC / H5 复用。
 */
import { parseTime } from '@/utils/ruoyi'
import { listMember } from '@/api/oa/member'
import { toneColor, toneMeta } from '@/utils/status'

/**
 * 选项列表：字典优先、前端常量兜底
 * 说明：字典在「系统管理 → 字典管理」改过后，下拉/筛选里的文案同步变化
 *
 * @param {Array} dicts useDict 返回的字典数组
 * @param {Array} fallbackOptions 兜底选项 [{ value, label }]
 */
export function resolveOptions(dicts, fallbackOptions) {
  if (Array.isArray(dicts) && dicts.length) {
    return dicts.map(item => ({ value: String(item.value), label: item.label }))
  }
  return Array.isArray(fallbackOptions) ? fallbackOptions : []
}

/* -------------------------------------------------------------------------- */
/* 一、状态 / 优先级 / 来源 / 周期 / 项目 / 站内信映射（02 §5.1、SQL 字典）      */
/* -------------------------------------------------------------------------- */

/** 任务状态值（后端 char(1)） */
export const TASK_STATUS = {
  TODO: '0',
  DOING: '1',
  AUDIT: '2',
  DONE: '3',
  CANCELED: '4'
}

/**
 * 任务状态展示映射
 * tone 对应全局 .oa-pill--xxx；color/bg/border 取自 utils/status.js 的令牌，
 * 字段名与结构保持不变（页面用内联 :style 直接读取），只把 hex 换成 var(--token)。
 */
export const TASK_STATUS_MAP = {
  '0': { label: '待办', tone: 'gray', ...toneMeta('gray') },
  '1': { label: '进行中', tone: 'blue', ...toneMeta('blue') },
  '2': { label: '待验收', tone: 'orange', ...toneMeta('orange') },
  '3': { label: '已完成', tone: 'green', ...toneMeta('green') },
  '4': { label: '已取消', tone: 'gray', ...toneMeta('gray'), strike: true }
}

/** 任务状态下拉（筛选 / 表单） */
export const TASK_STATUS_OPTIONS = ['0', '1', '2', '3', '4'].map(value => ({
  value,
  label: TASK_STATUS_MAP[value].label
}))

/** 优先级映射：0 高 / 1 中 / 2 低（字典 oa_task_priority） */
export const PRIORITY_MAP = {
  '0': { label: '高', tone: 'red', ...toneMeta('red'), bar: toneColor('red') },
  '1': { label: '中', tone: 'orange', ...toneMeta('orange'), bar: toneColor('orange') },
  '2': { label: '低', tone: 'gray', ...toneMeta('gray'), bar: toneColor('gray') }
}

/** 优先级下拉 */
export const PRIORITY_OPTIONS = ['0', '1', '2'].map(value => ({
  value,
  label: PRIORITY_MAP[value].label
}))

/** 优先级排序权重（列内排序兜底用） */
export const PRIORITY_ORDER = { '0': 0, '1': 1, '2': 2 }

/** 任务来源：0 手工创建 / 1 周期生成 / 2 子任务 */
export const SOURCE_MAP = {
  '0': { label: '手工创建', tone: 'gray' },
  '1': { label: '周期生成', tone: 'orange' },
  '2': { label: '子任务', tone: 'blue' }
}

/** 周期规则状态：0 启用 / 1 暂停 / 2 已结束 */
export const RECUR_STATUS_MAP = {
  '0': { label: '启用', tone: 'green', ...toneMeta('green') },
  '1': { label: '暂停', tone: 'orange', ...toneMeta('orange') },
  '2': { label: '已结束', tone: 'gray', ...toneMeta('gray') }
}

/** 项目状态：0 进行中 / 1 已结束（归档） */
export const PROJECT_STATUS_MAP = {
  '0': { label: '进行中', tone: 'blue', ...toneMeta('blue') },
  '1': { label: '已结束', tone: 'gray', ...toneMeta('gray') }
}

/** 站内信类型：1 被派任务 / 2 周期提醒 / 3 截止提醒 / 4 验收结果 / 5 系统通知 */
export const NOTIFY_TYPE_MAP = {
  '1': { label: '被派任务', tone: 'blue', icon: 'Tickets' },
  '2': { label: '周期提醒', tone: 'orange', icon: 'Refresh' },
  '3': { label: '截止提醒', tone: 'orange', icon: 'AlarmClock' },
  '4': { label: '验收结果', tone: 'green', icon: 'CircleCheck' },
  '5': { label: '系统通知', tone: 'gray', icon: 'Bell' }
}

/** 标签字典兜底（oa_task_tag，字典接口未就绪时仍可展示） */
export const TASK_TAG_FALLBACK = {
  '1': '宣传',
  '2': '物资',
  '3': '场地',
  '4': '外联',
  '5': '文书',
  '6': '其他'
}

/** 星期文案：索引 0 对应周一（1） */
export const WEEK_DAY_LABELS = ['一', '二', '三', '四', '五', '六', '日']

/** 星期多选选项 */
export const WEEK_DAY_OPTIONS = WEEK_DAY_LABELS.map((label, index) => ({
  value: index + 1,
  label: '周' + label,
  short: label
}))

/** 看板列定义（顺序 = 展示顺序；color 引用 status.js 令牌，暗色可切换） */
export const BOARD_COLUMNS = [
  { status: '0', label: '待办', color: toneColor('gray') },
  { status: '1', label: '进行中', color: toneColor('blue') },
  { status: '2', label: '待验收', color: toneColor('orange') },
  { status: '3', label: '已完成', color: toneColor('green') }
]

/** 看板列状态 → 落位成功 toast 文案 */
export const BOARD_STATUS_LABEL = BOARD_COLUMNS.reduce((map, item) => {
  map[item.status] = item.label
  return map
}, {})

/* -------------------------------------------------------------------------- */
/* 二、取值函数（组件里直接 statusMeta(row.status)）                            */
/* -------------------------------------------------------------------------- */

/** 任务状态 meta */
export function statusMeta(status) {
  return TASK_STATUS_MAP[String(status)] || { label: '—', tone: 'gray', ...toneMeta('gray') }
}

/** 任务状态文案 */
export function statusLabel(status) {
  return statusMeta(status).label
}

/** 优先级 meta */
export function priorityMeta(priority) {
  return PRIORITY_MAP[String(priority)] || { label: '—', tone: 'gray', ...toneMeta('gray'), bar: toneColor('gray') }
}

/** 优先级文案 */
export function priorityLabel(priority) {
  return priorityMeta(priority).label
}

/** 优先级左侧色条（3px / 4px）颜色 */
export function priorityBarColor(priority) {
  return priorityMeta(priority).bar
}

/** 来源文案 */
export function sourceLabel(source) {
  return (SOURCE_MAP[String(source)] || {}).label || '—'
}

/** 来源色调（对应 .oa-pill--xxx） */
export function sourceTone(source) {
  return (SOURCE_MAP[String(source)] || {}).tone || 'gray'
}

/** 周期规则状态 meta */
export function recurStatusMeta(status) {
  return RECUR_STATUS_MAP[String(status)] || { label: '—', tone: 'gray', ...toneMeta('gray') }
}

/** 项目状态 meta */
export function projectStatusMeta(status) {
  return PROJECT_STATUS_MAP[String(status)] || { label: '—', tone: 'gray', ...toneMeta('gray') }
}

/** 站内信类型 meta */
export function notifyTypeMeta(type) {
  return NOTIFY_TYPE_MAP[String(type)] || { label: '通知', tone: 'gray', icon: 'Bell' }
}

/* -------------------------------------------------------------------------- */
/* 三、时间 / 逾期 / 文件等格式化                                               */
/* -------------------------------------------------------------------------- */

/** 统一转 Date（兼容 yyyy-MM-dd HH:mm:ss 在部分浏览器的解析差异） */
export function toDate(value) {
  if (!value) return null
  if (value instanceof Date) return value
  if (typeof value === 'number') return new Date(value)
  const date = new Date(String(value).replace(/-/g, '/'))
  return isNaN(date.getTime()) ? null : date
}

/** yyyy-MM-dd HH:mm */
export function formatDateTime(value) {
  if (!value) return '—'
  return parseTime(value, '{y}-{m}-{d} {h}:{i}') || '—'
}

/** yyyy-MM-dd */
export function formatDate(value) {
  if (!value) return '—'
  return parseTime(value, '{y}-{m}-{d}') || '—'
}

/** MM-DD HH:mm（卡片 / 看板紧凑展示） */
export function formatDueTime(value) {
  if (!value) return '—'
  return parseTime(value, '{m}-{d} {h}:{i}') || '—'
}

/** 今天的 yyyy-MM-dd */
export function todayStr() {
  return parseTime(new Date(), '{y}-{m}-{d}')
}

/** 任意时间值（日期对象 / 时间戳 / 字符串）→ yyyy-MM-dd HH:mm:ss，供表单回显 */
export function toDateTimeString(value) {
  if (value === undefined || value === null || value === '') return undefined
  return parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || undefined
}

/** 任意时间值 → yyyy-MM-dd，供表单回显 */
export function toDateString(value) {
  if (value === undefined || value === null || value === '') return undefined
  return parseTime(value, '{y}-{m}-{d}') || undefined
}

/** 明天的 yyyy-MM-dd */
export function tomorrowStr() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  return parseTime(date, '{y}-{m}-{d}')
}

/**
 * 是否逾期（02 §5.2）
 * 优先信任后端 { overdueFlag, overdueDays }，缺失时按 dueTime 兜底计算。
 * 口径：未完成（待办 / 进行中）且截止时间早于当前。
 */
export function isOverdue(row) {
  if (!row) return false
  const status = String(row.status)
  const due = toDate(row.dueTime)
  const unfinished = status === TASK_STATUS.TODO || status === TASK_STATUS.DOING
  if (row.overdueFlag !== undefined && row.overdueFlag !== null && row.overdueFlag !== '') {
    return String(row.overdueFlag) === '1' && unfinished
  }
  return unfinished && !!due && due.getTime() < Date.now()
}

/**
 * 逾期天数：后端优先，缺失时 ceil((now - dueTime) / 86400)，最小 1。
 */
export function overdueDays(row) {
  if (!row) return 0
  if (!isOverdue(row)) return 0
  const provided = Number(row.overdueDays)
  if (row.overdueDays !== undefined && row.overdueDays !== null && row.overdueDays !== '' && !isNaN(provided) && provided > 0) {
    return Math.ceil(provided)
  }
  const due = toDate(row.dueTime)
  if (!due) return 1
  const days = Math.ceil((Date.now() - due.getTime()) / 86400000)
  return Math.max(1, days)
}

/** 逾期红标文案：已逾期 N 天 */
export function overdueText(row) {
  const days = overdueDays(row)
  return days > 0 ? '已逾期 ' + days + ' 天' : ''
}

/** 卡片 / 看板底部截止文案：未完成显示截止时间，完成显示完成时间 */
export function dueOrFinishText(row) {
  if (!row) return '—'
  if (String(row.status) === TASK_STATUS.DONE) {
    return row.finishTime ? '✓ ' + parseTime(row.finishTime, '{m}-{d}') + ' 完成' : '已完成'
  }
  if (String(row.status) === TASK_STATUS.CANCELED) return '已取消'
  return formatDueTime(row.dueTime) + ' 截止'
}

/** 文件大小：1.2 MB */
export function formatFileSize(size) {
  const num = Number(size)
  if (!size || isNaN(num) || num <= 0) return '—'
  if (num < 1024) return num + ' B'
  if (num < 1024 * 1024) return (num / 1024).toFixed(1) + ' KB'
  return (num / 1024 / 1024).toFixed(1) + ' MB'
}

/** 文件名（附件 VO 的 originalName / fileName 兜底） */
export function fileName(file) {
  if (!file) return ''
  return file.originalName || file.fileName || ('附件' + (file.fileId || ''))
}

/* -------------------------------------------------------------------------- */
/* 四、标签 / 星期 / 头像                                                       */
/* -------------------------------------------------------------------------- */

/** 逗号串或数组 → 标签数组 */
export function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags.map(item => String(item).trim()).filter(Boolean)
  return String(tags)
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

/**
 * 标签展示文案：字典 value → label，已是中文标签则原样返回
 * @param {string} tag 标签值 / 标签名
 * @param {Array<{label:string,value:string}>} dictOptions 字典 oa_task_tag（可选）
 */
export function normalizeTag(tag, dictOptions) {
  if (tag === undefined || tag === null || tag === '') return ''
  const key = String(tag)
  if (Array.isArray(dictOptions)) {
    const hit = dictOptions.find(item => String(item.value) === key || item.label === key)
    if (hit) return hit.label
  }
  return TASK_TAG_FALLBACK[key] || key
}

/** 标签数组 → 展示文案数组（自动去重） */
export function normalizeTags(tags, dictOptions) {
  return [...new Set(parseTags(tags).map(item => normalizeTag(item, dictOptions)).filter(Boolean))]
}

/** [1,3,5] → 一/三/五 */
export function formatWeekDays(weekDays) {
  let list = weekDays
  if (typeof weekDays === 'string') list = weekDays.split(',')
  if (!Array.isArray(list)) return '—'
  return list
    .map(day => {
      const value = Number(day)
      return value >= 1 && value <= 7 ? WEEK_DAY_LABELS[value - 1] : ''
    })
    .filter(Boolean)
    .join('/') || '—'
}

/** 姓名首字（头像占位） */
export function nameInitial(name) {
  const text = String(name || '').trim()
  return text ? text.slice(0, 1) : '?'
}

/** 头像背景色（按 userId 稳定取色；引用 ui.scss 的 --avatar-* 令牌，暗色下同样可读） */
const AVATAR_COLORS = [
  'var(--avatar-1)',
  'var(--avatar-2)',
  'var(--avatar-3)',
  'var(--avatar-4)',
  'var(--avatar-5)',
  'var(--avatar-6)',
  'var(--avatar-7)'
]
export function avatarColor(userId) {
  const num = Number(userId) || 1
  return AVATAR_COLORS[Math.abs(num) % AVATAR_COLORS.length]
}

/* -------------------------------------------------------------------------- */
/* 五、看板列 / 拖拽落位规则（02 §3.3 表格的严格实现）                          */
/* -------------------------------------------------------------------------- */

/**
 * 判断卡片是否可以拖动（当前用户有可见权且至少有 1 条合法目标列）
 * @param {object} task TaskListVO
 */
export function canDragCard(task) {
  if (!task || task.readOnly) return false
  return BOARD_COLUMNS.some(column => boardDropCheck(task.status, column.status, task).ok)
}

/**
 * 拖拽落位校验（02 §3.3「拖拽落点规则」）
 * @param {string} fromStatus 起点列状态
 * @param {string} toStatus   落点列状态
 * @param {object} task       任务（需含 canStart / canSubmit / canAudit / readOnly）
 * @returns {{ok:boolean, action:string|null, reason:string, same:boolean}}
 *   action：start / submit / audit（调用对应接口）；reason：不可落位时的提示文案
 */
export function boardDropCheck(fromStatus, toStatus, task) {
  const from = String(fromStatus)
  const to = String(toStatus)
  const result = { ok: false, action: null, reason: '', same: from === to }
  if (!task) {
    result.reason = '任务数据异常，请刷新后重试'
    return result
  }
  // 同一列：不触发请求、无 toast
  if (from === to) return result
  // 归档项目：任意方向都禁止
  if (task.readOnly) {
    result.reason = '项目已结束，任务只读'
    return result
  }
  // 终态保护
  if (from === TASK_STATUS.DONE) {
    result.reason = '已完成任务不可再流转'
    return result
  }
  if (from === TASK_STATUS.CANCELED) {
    result.reason = '已取消任务不可再流转'
    return result
  }

  // 待办 → 进行中：开始
  if (from === TASK_STATUS.TODO && to === TASK_STATUS.DOING) {
    if (task.canStart === false) {
      result.reason = '只有主责人可以开始任务'
      return result
    }
    return { ...result, ok: true, action: 'start' }
  }
  // 待办 → 待验收 / 已完成：必须先开始
  if (from === TASK_STATUS.TODO && to === TASK_STATUS.AUDIT) {
    result.reason = '请先开始任务，再提交完成'
    return result
  }
  if (from === TASK_STATUS.TODO && to === TASK_STATUS.DONE) {
    result.reason = '不能跳过进行中直接完成'
    return result
  }
  // 进行中 → 待验收：提交完成
  if (from === TASK_STATUS.DOING && to === TASK_STATUS.AUDIT) {
    if (task.canSubmit === false) {
      result.reason = '只有主责人可以提交完成'
      return result
    }
    return { ...result, ok: true, action: 'submit' }
  }
  // 进行中 → 已完成：必须先提交
  if (from === TASK_STATUS.DOING && to === TASK_STATUS.DONE) {
    result.reason = '请先提交，由验收人验收通过'
    return result
  }
  // 进行中 → 待办：无回退流转
  if (from === TASK_STATUS.DOING && to === TASK_STATUS.TODO) {
    result.reason = '不支持退回待办'
    return result
  }
  // 待验收 → 已完成：验收通过
  if (from === TASK_STATUS.AUDIT && to === TASK_STATUS.DONE) {
    if (task.canAudit === false) {
      result.reason = '只有验收人可以验收'
      return result
    }
    return { ...result, ok: true, action: 'audit' }
  }
  // 待验收 → 进行中：打回必须填理由，走详情弹窗
  if (from === TASK_STATUS.AUDIT && to === TASK_STATUS.DOING) {
    result.reason = '打回需填写理由，请在任务详情中操作'
    return result
  }
  // 待验收 → 待办：无流转
  if (from === TASK_STATUS.AUDIT && to === TASK_STATUS.TODO) {
    result.reason = '不支持退回待办'
    return result
  }

  result.reason = '不支持的状态流转'
  return result
}

/** 落位成功文案：已移动到 进行中 */
export function boardMoveSuccessText(status) {
  return '已移动到 ' + (BOARD_STATUS_LABEL[String(status)] || '')
}

/** 失败提示（带原因）：无法移动到 已完成：请先由主责人提交 */
export function boardMoveFailText(status, reason) {
  return '无法移动到 ' + (BOARD_STATUS_LABEL[String(status)] || '') + '：' + reason
}

/* -------------------------------------------------------------------------- */
/* 六、任务卡 / 详情按钮可见性（契约 §3.4 canX + 状态矩阵 02 §3.4）              */
/* -------------------------------------------------------------------------- */

/** 后端 canX 字段缺失时的状态兜底 */
function flag(row, name, fallback) {
  if (!row) return false
  if (row.readOnly) return false
  if (row[name] === undefined || row[name] === null) return fallback
  return row[name] !== false
}

/** 开始：status=0 且本人主责/派发人/管理员（后端 canStart） */
export function canStart(row) {
  return flag(row, 'canStart', String(row && row.status) === TASK_STATUS.TODO)
}

/** 提交完成：status=1 且本人主责（后端 canSubmit） */
export function canSubmit(row) {
  return flag(row, 'canSubmit', String(row && row.status) === TASK_STATUS.DOING)
}

/** 验收通过 / 打回：status=2 且本人验收人（后端 canAudit） */
export function canAudit(row) {
  return flag(row, 'canAudit', String(row && row.status) === TASK_STATUS.AUDIT)
}

/** 编辑：未完成态且派发人/管理员（后端 canEdit） */
export function canEdit(row) {
  return flag(row, 'canEdit', false)
}

/** 取消：待办 / 进行中且派发人/主责人（后端 canCancel） */
export function canCancel(row) {
  return flag(row, 'canCancel', false)
}

/** 改派：待办 / 进行中 / 待验收且派发人（后端 canReassign） */
export function canReassign(row) {
  return flag(row, 'canReassign', false)
}

/** 是否只读（归档项目任务） */
export function isReadOnly(row) {
  return !!(row && (row.readOnly === true || String(row.readOnly) === 'true'))
}

/**
 * 表格行操作（02 §3.2）：按状态与角色渲染，超 3 个折叠进「更多」
 * @returns {{actions: Array<{key:string,label:string,type:string}>, more: Array}}
 */
export function rowActions(row) {
  if (!row || isReadOnly(row)) return { actions: [], more: [] }
  const status = String(row.status)
  const actions = []
  if (status === TASK_STATUS.TODO && canStart(row)) actions.push({ key: 'start', label: '开始', type: 'primary' })
  if (status === TASK_STATUS.DOING && canSubmit(row)) actions.push({ key: 'submit', label: '提交', type: 'primary' })
  if (status === TASK_STATUS.AUDIT && canAudit(row)) {
    actions.push({ key: 'audit', label: '验收', type: 'success' })
    actions.push({ key: 'reject', label: '打回', type: 'warning' })
  }
  if (canEdit(row) && (status === TASK_STATUS.TODO || status === TASK_STATUS.DOING)) {
    actions.push({ key: 'edit', label: '编辑', type: 'primary' })
  }
  if (canReassign(row)) actions.push({ key: 'reassign', label: '改派', type: 'primary' })
  if (canCancel(row)) actions.push({ key: 'cancel', label: '取消', type: 'danger' })
  if (actions.length <= 3) return { actions, more: [] }
  return { actions: actions.slice(0, 2), more: actions.slice(2) }
}

/* -------------------------------------------------------------------------- */
/* 七、成员远程搜索（复用 GET /oa/member/list，需 oa:member:list）               */
/* -------------------------------------------------------------------------- */

/**
 * 成员下拉选项（仅返回已开号 member.userId 的成员，任务主责 / 协作人需要账号）
 * @param {string} keyword 姓名 / 学号 / 手机号
 * @returns {Promise<Array<{value:number,label:string,name:string,deptName:string}>>}
 */
export function searchMemberOptions(keyword) {
  return listMember({ keyword: keyword || undefined, status: '0', pageNum: 1, pageSize: 20 })
    .then(response => {
      const rows = (response && response.rows) || []
      return rows
        .filter(member => member && member.userId !== null && member.userId !== undefined)
        .map(member => ({
          value: member.userId,
          label: member.name + '（' + (member.deptName || '—') + '）',
          name: member.name,
          deptName: member.deptName || ''
        }))
    })
    .catch(() => [])
}

/**
 * 把成员 ID 数组补全为下拉选项（编辑时回显：后端返回 userId + userName + deptName）
 * @param {Array<{userId:number,userName:string,deptName:string}>} members
 */
export function memberBriefOptions(members) {
  return (members || []).map(member => ({
    value: member.userId,
    label: (member.userName || member.nickName || '用户' + member.userId) + '（' + (member.deptName || '—') + '）',
    name: member.userName || member.nickName,
    deptName: member.deptName || ''
  }))
}

/* -------------------------------------------------------------------------- */
/* 八、其他前端约定                                                             */
/* -------------------------------------------------------------------------- */

/** 打回 / 取消理由快捷选项（02 §3.4） */
export const REJECT_REASONS = ['与要求不符', '材料不完整', '需要重做', '其他']

/** 任务标题校验（2–200） */
export function validateTitle(rule, value, callback) {
  const text = String(value || '').trim()
  if (!text) return callback(new Error('请填写任务标题'))
  if (text.length < 2 || text.length > 200) return callback(new Error('标题需为 2–200 个字符'))
  callback()
}

/** 打回 / 取消理由校验（2–200） */
export function validateRemark(rule, value, callback) {
  const text = String(value || '').trim()
  if (!text) return callback(new Error('请填写理由'))
  if (text.length < 2 || text.length > 200) return callback(new Error('理由需为 2–200 个字符'))
  callback()
}

/** 星期数组归一化：去重 + 升序 + 只保留 1–7 */
export function normalizeWeekDays(weekDays) {
  let list = weekDays
  if (typeof weekDays === 'string') list = weekDays.split(',')
  if (!Array.isArray(list)) return []
  return [...new Set(list.map(Number).filter(value => value >= 1 && value <= 7))].sort((a, b) => a - b)
}
