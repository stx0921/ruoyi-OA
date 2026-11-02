package com.ruoyi.oa.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.oa.member.dto.DeptLeaderDTO;
import com.ruoyi.oa.member.service.IOaDeptService;

/**
 * 部门与负责人 Controller（PC-05）
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/dept")
public class OaDeptController extends BaseController
{
    @Autowired
    private IOaDeptService deptService;

    /**
     * 部门列表（含在籍人数、正/副部长）
     */
    @PreAuthorize("@ss.hasPermi('oa:dept:list')")
    @GetMapping("/list")
    public AjaxResult list()
    {
        return success(deptService.selectDeptWithLeader());
    }

    /**
     * 部门下拉选项（申请表单可用）
     */
    @PreAuthorize("@ss.hasPermi('oa:dept:list')")
    @GetMapping("/options")
    public AjaxResult options()
    {
        return success(deptService.selectDeptOptions());
    }

    /**
     * 保存部门正/副部长
     */
    @PreAuthorize("@ss.hasPermi('oa:dept:edit')")
    @Log(title = "部门负责人", businessType = BusinessType.UPDATE)
    @PutMapping("/leader")
    public AjaxResult saveLeaders(@RequestBody DeptLeaderDTO dto)
    {
        deptService.saveLeaders(dto);
        return success();
    }
}
