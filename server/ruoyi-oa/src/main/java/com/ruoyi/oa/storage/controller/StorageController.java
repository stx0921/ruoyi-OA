package com.ruoyi.oa.storage.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.oa.storage.service.StorageMigrationService;

/**
 * 存储管理（运维）：状态/对账/存量迁移/回滚。
 *
 * <p>能力等价于若依「系统管理」下的运维接口：权限点 system:config:edit（兼容文档口径）
 * 或新增的 system:storage:*；且必须是管理员（isAdmin）。迁移与对账均为只读 + CAS 更新，
 * 不提供删除本地文件的能力。</p>
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/system/storage")
public class StorageController extends BaseController
{
    /** 状态查询权限（页面菜单权限也允许） */
    private static final String STATUS_PERMISSION = "@ss.hasAnyPermi('system:storage:list,system:storage:query,system:storage:migrate,system:config:edit')";

    /** 迁移/回滚权限（文档口径 system:config:edit 也可） */
    private static final String MIGRATE_PERMISSION = "@ss.hasAnyPermi('system:storage:migrate,system:storage:rollback,system:config:edit')";

    @Autowired
    private StorageMigrationService storageMigrationService;

    /**
     * 存储状态 + 待迁移/已迁移统计
     */
    @PreAuthorize(STATUS_PERMISSION)
    @GetMapping("/status")
    public AjaxResult status()
    {
        return success(storageMigrationService.status());
    }

    /**
     * 对账：两条对账 SQL 计数（迁移后都应为 0）
     */
    @PreAuthorize(STATUS_PERMISSION)
    @GetMapping("/reconcile")
    public AjaxResult reconcile()
    {
        return success(storageMigrationService.reconcile());
    }

    /**
     * 执行迁移 / 回滚
     *
     * @param dryRun true 只统计，不上传不写库（默认 true，必须显式 false 才执行）
     * @param limit 单次处理条数（默认 200，最大 2000）
     * @param type oa_file | avatar | all
     * @param mode migrate | rollback
     */
    @PreAuthorize(MIGRATE_PERMISSION)
    @Log(title = "存储迁移", businessType = BusinessType.OTHER)
    @PostMapping("/migrate")
    public AjaxResult migrate(@RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun,
            @RequestParam(value = "limit", defaultValue = "200") int limit,
            @RequestParam(value = "type", defaultValue = "all") String type,
            @RequestParam(value = "mode", defaultValue = "migrate") String mode)
    {
        if (!SecurityUtils.isAdmin(getUserId()))
        {
            return AjaxResult.error(403, "仅管理员可执行存储迁移");
        }
        return success(storageMigrationService.migrate(mode, type, dryRun, limit));
    }
}
