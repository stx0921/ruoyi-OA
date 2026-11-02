import request from '@/utils/request'

/**
 * H5 学生端公开接口（契约 §3.2 P1–P8，类级 @Anonymous 白名单）
 *
 * 约定：
 * - 均为免登录接口，统一不携带登录令牌（isToken:false），避免过期令牌影响匿名访问；
 * - 关闭防重复提交拦截（repeatSubmit:false），上传 / 查询 / 提交由页面按钮 loading 自行防抖；
 * - 后端保证 public 接口不返回 401（03 文档 §5.1），因此失败不会触发登录弹窗。
 */
const anonymousHeaders = { isToken: false }

/**
 * P1 获取图形验证码
 * @returns {Promise<{data:{uuid:string,img:string,expireIn:number}}>} img 为 base64 jpg（不含前缀）
 */
export function getApplyCaptcha() {
  return request({
    url: '/oa/apply/public/captcha',
    method: 'get',
    timeout: 15000,
    headers: { ...anonymousHeaders }
  })
}

/**
 * P2 校验图形验证码，换取一次性校验令牌
 * @param {{uuid:string, code:string}} data
 * @returns {Promise<{data:{verifyToken:string,expireIn:number}}>} expireIn 默认 1800 秒
 */
export function verifyApplyCaptcha(data) {
  return request({
    url: '/oa/apply/public/verify-captcha',
    method: 'post',
    data,
    timeout: 15000,
    headers: { ...anonymousHeaders, repeatSubmit: false }
  })
}

/**
 * P3 意向部门列表（组织根节点下启用部门）
 * @returns {Promise<{data:Array<{deptId:number,deptName:string}>}>}
 */
export function listApplyDepts() {
  return request({
    url: '/oa/apply/public/depts',
    method: 'get',
    timeout: 15000,
    headers: { ...anonymousHeaders }
  })
}

/**
 * P4 报名附件上传（multipart/form-data：file + bizType + verifyToken）
 * @param {FormData} formData
 * @param {(event:ProgressEvent)=>void} [onUploadProgress] 上传进度回调
 * @returns {Promise<{data:{fileId:number,fileUrl:string}}>}
 */
export function uploadApplyFile(formData, onUploadProgress) {
  return request({
    url: '/oa/apply/public/upload',
    method: 'post',
    data: formData,
    timeout: 60000,
    headers: { ...anonymousHeaders, repeatSubmit: false },
    onUploadProgress
  })
}

/**
 * P5 提交报名（verifyToken + 表单 + agreed）
 * @param {object} data ApplySubmitDTO：name/gender/studentNo/phone/college/major/className/grade/
 *   applyDeptIds[]/obeyAdjust/skills/intro/photoFileId/cardFileId/verifyToken/agreed
 * @returns {Promise<{data:{applyIds:number[],applyNos:string[],progressTip:string}}>}
 */
export function submitApply(data) {
  return request({
    url: '/oa/apply/public/submit',
    method: 'post',
    data,
    timeout: 30000,
    headers: { ...anonymousHeaders, repeatSubmit: false }
  })
}

/**
 * P6 查询报名进度（手机号 + 图形验证码 → queryToken + 申请列表）
 * @param {{phone:string, captchaUuid:string, captchaCode:string}} data
 * @returns {Promise<{data:{queryToken:string,applyList:Array}}>}
 */
export function queryApplyProgress(data) {
  return request({
    url: '/oa/apply/public/progress',
    method: 'post',
    data,
    timeout: 20000,
    headers: { ...anonymousHeaders, repeatSubmit: false }
  })
}

/**
 * P7 修改重提（携带 queryToken，不再校验图形码）
 * @param {object} data { applyId, phone, queryToken, ...可编辑字段 }
 * @returns {Promise}
 */
export function resubmitApply(data) {
  return request({
    url: '/oa/apply/public/resubmit',
    method: 'put',
    data,
    timeout: 30000,
    headers: { ...anonymousHeaders, repeatSubmit: false }
  })
}

/**
 * P8 撤回申请（携带 queryToken，仅待审批可撤回）
 * @param {{applyId:number, phone:string, queryToken:string}} data
 * @returns {Promise}
 */
export function withdrawApply(data) {
  return request({
    url: '/oa/apply/public/withdraw',
    method: 'put',
    data,
    timeout: 20000,
    headers: { ...anonymousHeaders, repeatSubmit: false }
  })
}
