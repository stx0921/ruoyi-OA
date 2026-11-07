/**
 * 人员模块（成员管理 / 成员详情 / 批量导入）前端公共工具
 *
 * 把「后端契约相关」的常量与格式化收敛到一处，页面只负责展示（仿 utils/oa.js）：
 * - 成员状态 / 申请状态 / 职务 / 年级 / 届次 / 来源 / 性别 / 动作 的文案与标签色调
 * - 日期时间展示（复用 utils/oa.js 的实现，避免两套口径）
 * - 操作日志 beforeJson / afterJson → 字段级 diff 明细与摘要
 * - 手机号 / 邮箱 / 微信 / QQ 打码兜底工具（仅用于兜底展示，正常情况下打码由后端完成，R12）
 *
 * 注意：R12 要求「打码由后端完成，前端不二次处理」，因此页面展示敏感字段时应：
 *   - 直接把后端返回值交给 OaSensitiveText 渲染
 *   - 用 isMaskedValue(value) 判断是否需要 tooltip / 禁选提示
 * 本文件里的 maskXxx 只用于「后端漏打码时的兜底展示」或非敏感场景占位，不要在列表里主动套用。
 */
import { parseTime } from '@/utils/ruoyi'

// 复用请假模块已沉淀的日期时间格式化，避免出现两套时间口径
export { oaDate, oaDateTime, oaTimestamp } from '@/utils/oa'

/* -------------------------------------------------------------------------- */
/* 1. 字典映射                                                                */
/* -------------------------------------------------------------------------- */

/**
 * 成员状态（oa_member_status）：0 在籍 / 1 已退会 / 2 已毕业
 * 状态标签的色值统一由 utils/status.js + .oa-pill--* 令牌提供（暗色自适应），
 * 这里只保留语义 tone，不再出现 hex。
 */
export const MEMBER_STATUS_MAP = {
  0: { label: '在籍', tone: 'green' },
  1: { label: '已退会', tone: 'red' },
  2: { label: '已毕业', tone: 'gray' }
}

/** 申请状态（oa_apply_status）：0 待审批 / 1 已通过 / 2 已驳回 / 3 已撤回 / 4 已关闭 */
export const APPLY_STATUS_MAP = {
  0: { label: '待审批', tone: 'orange' },
  1: { label: '已通过', tone: 'green' },
  2: { label: '已驳回', tone: 'red' },
  3: { label: '已撤回', tone: 'gray' },
  4: { label: '已关闭', tone: 'gray', prefix: '⊘ ' }
}

/** 性别（sys_user_sex）：0 男 / 1 女 / 2 未知 */
export const GENDER_MAP = {
  0: '男',
  1: '女',
  2: '未知'
}

/** 职务（oa_position） */
export const POSITION_MAP = {
  1: '会长',
  2: '副会长',
  3: '部长',
  4: '副部长',
  5: '干事'
}

/** 年级（oa_grade） */
export const GRADE_MAP = {
  1: '大一',
  2: '大二',
  3: '大三',
  4: '大四',
  5: '研一',
  6: '研二',
  7: '研三'
}

/** 来源（oa_member_source） */
export const SOURCE_MAP = {
  1: '自助申请',
  2: '手动录入',
  3: 'Excel 导入'
}

/** 动作 → { label, tone }（PC-06 日志标签配色，见 02-设计细节 §4.2） */
export const ACTION_MAP = {
  新增: { label: '新增', tone: 'green' },
  编辑: { label: '编辑', tone: 'blue' },
  状态变更: { label: '状态变更', tone: 'orange' },
  退会: { label: '退会', tone: 'red' },
  毕业: { label: '毕业', tone: 'gray' },
  恢复在籍: { label: '恢复在籍', tone: 'green' },
  删除: { label: '删除', tone: 'red' },
  调部门: { label: '调部门', tone: 'orange' },
  升职: { label: '升职', tone: 'orange' },
  审批: { label: '审批', tone: 'blue' },
  导入: { label: '导入', tone: 'blue' },
  导出: { label: '导出', tone: 'blue' },
  账号操作: { label: '账号操作', tone: 'gray' },
  同意: { label: '同意', tone: 'green' },
  驳回: { label: '驳回', tone: 'red' },
  撤回: { label: '撤回', tone: 'gray' },
  自动关闭: { label: '自动关闭', tone: 'gray' },
  重提: { label: '重提', tone: 'orange' },
  代审: { label: '代审', tone: 'orange' },
  提交: { label: '提交', tone: 'blue' }
}

/* -------------------------------------------------------------------------- */
/* 2. 下拉选项（字典接口不可用时的兜底）                                       */
/* -------------------------------------------------------------------------- */

/** 对象映射 → [{ value, label }]，忽略空白项 */
function mapToOptions(map) {
  return Object.keys(map || {})
    .filter(key => map[key] !== undefined && map[key] !== null)
    .map(key => {
      const item = map[key]
      return { value: String(key), label: typeof item === 'string' ? item : item.label }
    })
}

/** 成员状态下拉选项 */
export const MEMBER_STATUS_OPTIONS = mapToOptions(MEMBER_STATUS_MAP)

/** 申请状态下拉选项（纯文案，不带 ⊘ 前缀） */
export const APPLY_STATUS_OPTIONS = mapToOptions(APPLY_STATUS_MAP)

/** 职务下拉选项 */
export const POSITION_OPTIONS = mapToOptions(POSITION_MAP)

/** 年级下拉选项 */
export const GRADE_OPTIONS = mapToOptions(GRADE_MAP)

/** 来源下拉选项 */
export const SOURCE_OPTIONS = mapToOptions(SOURCE_MAP)

/** 性别下拉选项 */
export const GENDER_OPTIONS = mapToOptions(GENDER_MAP)

/** 动作下拉选项（PC-06 筛选用，顺序与 ACTION_MAP 一致） */
export const ACTION_OPTIONS = Object.keys(ACTION_MAP).map(key => ({
  value: key,
  label: ACTION_MAP[key].label
}))

/** 届次兜底选项：与 SQL 字典 oa_cohort 的默认取值保持一致（真实取值优先走字典） */
export const COHORT_OPTIONS = ['2024', '2025', '2026', '2027', '2028', '2029'].map(value => ({
  value,
  label: value + '届'
}))

/** 届次映射（value → 2026届） */
export const COHORT_MAP = COHORT_OPTIONS.reduce((map, item) => {
  map[item.value] = item.label
  return map
}, {})

/**
 * 若依部门树（/system/dept/treeselect）拍平成下拉选项
 * @param {Array} nodes [{ id, label, children }]
 * @param {Number} level 当前层级（子级用全角空格缩进）
 * @returns {Array<{ value, label, rawLabel }>}
 */
export function flattenDeptOptions(nodes, level) {
  const depth = level || 0
  const list = []
  ;(nodes || []).forEach(node => {
    const rawLabel = node.label || node.deptName || ''
    list.push({
      value: node.id || node.deptId,
      label: depth ? '　'.repeat(depth) + rawLabel : rawLabel,
      rawLabel
    })
    if (node.children && node.children.length) {
      list.push(...flattenDeptOptions(node.children, depth + 1))
    }
  })
  return list
}

/* -------------------------------------------------------------------------- */
/* 3. 通用取值函数                                                            */
/* -------------------------------------------------------------------------- */

/** 取映射里的 label（值可能是 { label } 或字符串） */
function mapLabel(map, value) {
  const hit = map ? map[String(value)] : undefined
  if (hit === undefined || hit === null) return ''
  return typeof hit === 'string' ? hit : hit.label
}

/** 取映射里的 tone（非标签映射返回 gray） */
function mapTone(map, value) {
  const hit = map ? map[String(value)] : undefined
  if (!hit) return 'gray'
  return typeof hit === 'string' ? 'gray' : hit.tone || 'gray'
}

/**
 * 字典优先、静态映射兜底
 * @param {*} value 后端值
 * @param {Array} dicts useDict 返回的 [{ label, value }]（可为空）
 * @param {Object} fallbackMap 静态映射 { value: 'label' }
 */
export function dictText(value, dicts, fallbackMap) {
  if (value === undefined || value === null || value === '') return '-'
  if (Array.isArray(dicts)) {
    const hit = dicts.find(item => String(item.value) === String(value))
    if (hit && hit.label) return hit.label
  }
  return mapLabel(fallbackMap, value) || String(value)
}

/**
 * 下拉选项：字典数组优先，否则用静态 OPTIONS 兜底
 * @param {Array} dicts useDict 返回的数组
 * @param {Array} fallbackOptions [{ value, label }]
 */
export function resolveOptions(dicts, fallbackOptions) {
  if (Array.isArray(dicts) && dicts.length) {
    return dicts.map(item => ({ value: String(item.value), label: item.label }))
  }
  return Array.isArray(fallbackOptions) ? fallbackOptions : []
}

/** 成员状态文案 */
export function memberStatusText(status) {
  return mapLabel(MEMBER_STATUS_MAP, status) || '-'
}

/** 成员状态标签色调 */
export function memberStatusTone(status) {
  return mapTone(MEMBER_STATUS_MAP, status)
}

/** 申请状态文案（标签展示；已关闭带 ⊘ 前缀） */
export function applyStatusText(status) {
  const hit = APPLY_STATUS_MAP[String(status)]
  if (!hit) return '-'
  return (hit.prefix || '') + hit.label
}

/** 申请状态纯文案（下拉 / 选项用，不带 ⊘） */
export function applyStatusLabel(status) {
  return mapLabel(APPLY_STATUS_MAP, status) || '-'
}

/** 申请状态标签色调 */
export function applyStatusTone(status) {
  return mapTone(APPLY_STATUS_MAP, status)
}

/** 动作文案 */
export function actionLabel(action) {
  if (action === undefined || action === null || action === '') return '-'
  return mapLabel(ACTION_MAP, action) || String(action)
}

/** 动作标签色调 */
export function actionTone(action) {
  return mapTone(ACTION_MAP, action)
}

/** 性别文案 */
export function genderText(value, dicts) {
  return dictText(value, dicts, GENDER_MAP)
}

/** 职务文案 */
export function positionText(value, dicts) {
  return dictText(value, dicts, POSITION_MAP)
}

/** 年级文案 */
export function gradeText(value, dicts) {
  return dictText(value, dicts, GRADE_MAP)
}

/** 来源文案 */
export function sourceText(value, dicts) {
  return dictText(value, dicts, SOURCE_MAP)
}

/** 届次文案：2026 → 2026届（已带「届」原样返回） */
export function cohortLabel(value) {
  if (value === undefined || value === null || value === '') return '-'
  const text = String(value)
  return text.indexOf('届') > -1 ? text : text + '届'
}

/** 届次文案（字典优先） */
export function cohortText(value, dicts) {
  if (value === undefined || value === null || value === '') return '-'
  if (Array.isArray(dicts)) {
    const hit = dicts.find(item => String(item.value) === String(value))
    if (hit && hit.label) return hit.label
  }
  return cohortLabel(value)
}

/* -------------------------------------------------------------------------- */
/* 4. 日期格式化                                                              */
/* -------------------------------------------------------------------------- */

/** 入会 / 离会日期：yyyy-MM-dd（空值显示 —） */
export function formatMemberDate(value) {
  if (!value) return '—'
  return parseTime(value, '{y}-{m}-{d}') || '—'
}

/** 动作时间：yyyy-MM-dd HH:mm（空值显示 —） */
export function formatMemberTime(value) {
  if (!value) return '—'
  return parseTime(value, '{y}-{m}-{d} {h}:{i}') || '—'
}

/* -------------------------------------------------------------------------- */
/* 5. 打码兜底工具（正常情况由后端打码，R12）                                 */
/* -------------------------------------------------------------------------- */

/** 保留首尾各 n 位，中间用 **** 代替 */
function maskMiddle(text, head, tail) {
  const value = text === undefined || text === null ? '' : String(text)
  if (!value) return ''
  if (value.indexOf('*') > -1) return value
  if (value.length <= head + tail) {
    return value.charAt(0) + '****'
  }
  return value.slice(0, head) + '****' + value.slice(value.length - tail)
}

/** 手机号打码：13812345678 → 138****5678 */
export function maskPhone(phone) {
  const value = phone === undefined || phone === null ? '' : String(phone)
  if (!value) return ''
  if (value.indexOf('*') > -1) return value
  if (/^1\d{10}$/.test(value)) return value.slice(0, 3) + '****' + value.slice(7)
  return maskMiddle(value, 3, 2)
}

/** 邮箱打码：linxiao@x.com → l***@x.com */
export function maskEmail(email) {
  const value = email === undefined || email === null ? '' : String(email)
  if (!value) return ''
  if (value.indexOf('*') > -1) return value
  const atIndex = value.indexOf('@')
  if (atIndex <= 0) return maskMiddle(value, 2, 2)
  const account = value.slice(0, atIndex)
  const domain = value.slice(atIndex)
  return account.charAt(0) + '***' + domain
}

/** 微信打码：zhangsan_wx → zh***wx */
export function maskWechat(wechat) {
  return maskMiddle(wechat, 2, 2)
}

/** QQ 打码：123456789 → 12***89 */
export function maskQQ(qq) {
  return maskMiddle(qq, 2, 2)
}

/**
 * 判断后端返回值是否已打码（用于 OaSensitiveText 的 masked 判定）
 * 只做「展示层」判断，不修改值、不缓存明文。
 */
export function isMaskedValue(value) {
  if (value === undefined || value === null) return false
  return /\*{2,}/.test(String(value))
}

/* -------------------------------------------------------------------------- */
/* 6. 操作日志 diff                                                           */
/* -------------------------------------------------------------------------- */

/** 字段中文名（兼容 camelCase / snake_case 两种后端序列化） */
export const FIELD_LABELS = {
  name: '姓名',
  gender: '性别',
  studentNo: '学号',
  student_no: '学号',
  phone: '手机号',
  email: '邮箱',
  wechat: '微信',
  qq: 'QQ',
  college: '学院',
  major: '专业',
  className: '班级',
  class_name: '班级',
  grade: '年级',
  cohort: '届次',
  deptId: '部门',
  dept_id: '部门',
  deptName: '部门',
  dept_name: '部门',
  position: '职务',
  status: '状态',
  source: '来源',
  joinDate: '入会日期',
  join_date: '入会日期',
  leaveDate: '离会日期',
  leave_date: '离会日期',
  skills: '特长',
  intro: '自我介绍',
  obeyAdjust: '服从调剂',
  obey_adjust: '服从调剂',
  remark: '备注',
  photoUrl: '照片',
  photo_url: '照片',
  cardUrl: '简历',
  card_url: '简历',
  photoFileId: '照片',
  photo_file_id: '照片',
  cardFileId: '简历',
  card_file_id: '简历',
  memberNo: '成员编号',
  member_no: '成员编号',
  memberId: '成员',
  member_id: '成员',
  accountStatus: '账号状态',
  account_status: '账号状态',
  password: '密码'
}

/** 值级格式化函数（按 camelCase 字段名取） */
const FIELD_FORMATTERS = {
  gender: value => mapLabel(GENDER_MAP, value),
  status: value => mapLabel(MEMBER_STATUS_MAP, value) || mapLabel(APPLY_STATUS_MAP, value),
  position: value => mapLabel(POSITION_MAP, value),
  grade: value => mapLabel(GRADE_MAP, value),
  source: value => mapLabel(SOURCE_MAP, value),
  cohort: value => cohortLabel(value),
  obeyAdjust: value => (String(value) === '1' ? '是' : String(value) === '0' ? '否' : String(value))
}

/** snake_case → camelCase */
function camelKey(field) {
  return String(field || '').replace(/_([a-z])/g, (match, char) => char.toUpperCase())
}

/** 字段中文名（未知字段回退原始 key） */
export function fieldLabel(field) {
  if (field === undefined || field === null || field === '') return ''
  return FIELD_LABELS[field] || FIELD_LABELS[camelKey(field)] || String(field)
}

/** 字段值 → 展示文案（null / '' → （空）；枚举转中文） */
export function formatDiffValue(field, value) {
  if (value === undefined || value === null || value === '') return '（空）'
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch (e) {
      return String(value)
    }
  }
  const formatter = FIELD_FORMATTERS[camelKey(field)]
  if (formatter) {
    const text = formatter(value)
    if (text) return text
  }
  return String(value)
}

/** 安全解析 beforeJson / afterJson（后端返回 JSON 字符串或对象） */
function parseJsonObject(text) {
  if (!text) return {}
  if (typeof text === 'object') return text
  try {
    const data = JSON.parse(String(text))
    return data && typeof data === 'object' ? data : {}
  } catch (e) {
    return {}
  }
}

/** 值比较（统一成字符串，null / undefined 视为空） */
function comparable(value) {
  if (value === undefined || value === null) return ''
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch (e) {
      return String(value)
    }
  }
  return String(value)
}

/**
 * 解析操作日志的字段级 diff
 *
 * @param {String|Object} beforeJson 变更前 JSON
 * @param {String|Object} afterJson 变更后 JSON
 * @param {Object} options { onlyChanged = true } 是否只返回有变化的字段
 * @returns {Array<{ field: String, label: String, before: String, after: String }>}
 *   before / after 已格式化为展示文案；新增日志 before 为「（空）」。
 */
export function diffMemberLog(beforeJson, afterJson, options) {
  const opts = options || {}
  const onlyChanged = opts.onlyChanged !== false
  const before = parseJsonObject(beforeJson)
  const after = parseJsonObject(afterJson)
  const keys = Object.keys(after)
  Object.keys(before).forEach(key => {
    if (keys.indexOf(key) === -1) keys.push(key)
  })

  const rows = []
  const usedLabels = {}
  keys.forEach(field => {
    const hasBefore = Object.prototype.hasOwnProperty.call(before, field)
    const hasAfter = Object.prototype.hasOwnProperty.call(after, field)
    const beforeRaw = hasBefore ? before[field] : null
    const afterRaw = hasAfter ? after[field] : null
    if (onlyChanged && comparable(beforeRaw) === comparable(afterRaw)) return
    const label = fieldLabel(field)
    const row = {
      field,
      label,
      before: hasBefore ? formatDiffValue(field, beforeRaw) : '（空）',
      after: hasAfter ? formatDiffValue(field, afterRaw) : '（空）'
    }
    // deptId / deptName 同时出现时只保留一行「部门」，优先展示部门名
    const existIndex = usedLabels[label]
    if (existIndex !== undefined) {
      const exist = rows[existIndex]
      const isDeptName = /^dept_?name$/i.test(field)
      const existIsDeptName = /^dept_?name$/i.test(exist.field)
      if ((row.after !== '（空）' && exist.after === '（空）') || (isDeptName && !existIsDeptName)) {
        rows[existIndex] = row
      }
      return
    }
    usedLabels[label] = rows.length
    rows.push(row)
  })
  return rows
}

/**
 * 操作日志摘要文案：手机号 138****5678 → 139****1234；部门 宣传部 → 外联部
 *
 * @param {String|Object} beforeJson 变更前
 * @param {String|Object} afterJson 变更后
 * @param {Object} options { onlyChanged = true, separator = '；', max = 0 }
 * @returns {String} 无差异返回空串
 */
export function diffSummary(beforeJson, afterJson, options) {
  const opts = options || {}
  const separator = opts.separator === undefined ? '；' : opts.separator
  const max = Number(opts.max) || 0
  const rows = diffMemberLog(beforeJson, afterJson, { onlyChanged: opts.onlyChanged !== false })
  const list = max > 0 ? rows.slice(0, max) : rows
  if (!list.length) return ''
  return list.map(row => row.label + ' ' + row.before + ' → ' + row.after).join(separator)
}

/**
 * 日志摘要：优先用后端 summary，缺失时用 beforeJson / afterJson 兜底
 * @param {Object} log MemberLogVO
 */
export function memberLogSummary(log) {
  if (!log) return '—'
  if (log.summary) return log.summary
  return diffSummary(log.beforeJson, log.afterJson) || '—'
}
