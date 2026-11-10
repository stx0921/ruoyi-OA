import request from '@/utils/request'

/* -------------------------------------------------------------------------- */
/* 系统管理 · 存储管理（本地 /profile ↔ 阿里云 OSS）                          */
/*                                                                            */
/* 后端契约：                                                                  */
/*   GET  /system/storage/status   存储状态（模式 / OSS 就绪 / 待迁移数量等）  */
/*   POST /system/storage/migrate  迁移或回滚，query 参数：                     */
/*        dryRun（默认 true，只统计不上传、不写库）                            */
/*        limit（单次处理条数，默认 200）                                       */
/*        type（oa_file / avatar / all，默认 all）                              */
/*        mode（migrate 本地→OSS / rollback OSS→本地，默认 migrate）            */
/*                                                                            */
/* 说明：迁移会逐个上传对象，耗时远大于普通接口，这里单独放宽超时；            */
/*       业务错误由 request 响应拦截器统一提示，页面不重复弹错。               */
/* -------------------------------------------------------------------------- */

// 查询存储状态
export function getStorageStatus() {
  return request({
    url: '/system/storage/status',
    method: 'get'
  })
}

// 迁移 / 回滚（dryRun=true 只统计；mode=rollback 为 OSS→本地反向迁移）
export function migrateStorage(query) {
  return request({
    url: '/system/storage/migrate',
    method: 'post',
    params: query,
    timeout: 300000
  })
}
