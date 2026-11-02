package com.ruoyi.oa.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.oa.member.dto.MemberLogQuery;
import com.ruoyi.oa.member.service.IOaMemberLogService;

/**
 * 成员操作日志 Controller（PC-06）
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/member/log")
public class OaMemberLogController extends BaseController
{
    @Autowired
    private IOaMemberLogService memberLogService;

    /**
     * 日志列表（部长仅本部门）
     */
    @PreAuthorize("@ss.hasPermi('oa:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(MemberLogQuery query)
    {
        startPage();
        return getDataTable(memberLogService.selectLogList(query));
    }
}
