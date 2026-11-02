import request from '@/utils/request'

/* -------------------------------------------------------------------------- */
/* 批量导入（PC-03，契约 §3.1 的 11–13 号接口）                               */
/* -------------------------------------------------------------------------- */

// 11. 下载导入模板（xlsx 流）
export function downloadImportTemplate() {
  return request({
    url: '/oa/member/import/template',
    method: 'get',
    responseType: 'blob',
    timeout: 30000
  })
}

// 12. 上传并校验（multipart：file + updateSupport；只落批次与行级错误，不写业务表）
export function validateImport(file, updateSupport) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('updateSupport', updateSupport === undefined || updateSupport === null ? '1' : String(updateSupport))
  return request({
    url: '/oa/member/import/validate',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: formData,
    timeout: 120000
  })
}

// 13. 确认导入（{ batchId, updateSupport }；后端重新解析文件并分批入库）
export function confirmImport(data) {
  return request({
    url: '/oa/member/import/confirm',
    method: 'post',
    data: data,
    timeout: 180000
  })
}

// 下载错误明细（errorFileUrl 形如 /oa/file/930，取文件流）
export function downloadImportError(fileId) {
  return request({
    url: '/oa/file/' + fileId,
    method: 'get',
    responseType: 'blob',
    timeout: 60000
  })
}
