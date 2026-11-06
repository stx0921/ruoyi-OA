package com.ruoyi.oa.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.oa.member.dto.DeptLeaderDTO;
import com.ruoyi.oa.member.dto.DeptSaveDTO;
import com.ruoyi.oa.member.service.IOaDeptService;

/**
 * 部门与负责人 Controller（PC-05）
 *
 * <p>组织架构（内设部门）的增删改单独放在这里维护，不再依赖「系统管理 → 部门管理」，
 * 上级部门改名也不会影响本页数据。</p>
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
     * 新增内设部门
     */
    @PreAuthorize("@ss.hasPermi('oa:dept:add')")
    @Log(title = "部门管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DeptSaveDTO dto)
    {
        deptService.addDept(dto);
        return success();
    }

    /**
     * 编辑内设部门（名称 / 排序 / 状态）
     */
    @PreAuthorize("@ss.hasPermi('oa:dept:edit')")
    @Log(title = "部门管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DeptSaveDTO dto)
    {
        deptService.updateDept(dto);
        return success();
    }

    /**
     * 删除内设部门（存在下级部门 / 在籍成员 / 系统用户时拒绝）
     */
    @PreAuthorize("@ss.hasPermi('oa:dept:remove')")
    @Log(title = "部门管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deptId}")
    public AjaxResult remove(@PathVariable("deptId") Long deptId)
    {
        deptService.deleteDept(deptId);
        return success();
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
