package com.ruoyi.oa.recur.controller;

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
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.oa.recur.dto.RecurQuery;
import com.ruoyi.oa.recur.dto.RecurSaveDTO;
import com.ruoyi.oa.recur.dto.RecurStatusDTO;
import com.ruoyi.oa.recur.service.IOaTaskRecurService;

/**
 * 周期任务规则 Controller（PC-06）
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/recur")
public class OaRecurController extends BaseController
{
    @Autowired
    private IOaTaskRecurService recurService;

    /**
     * 规则列表
     */
    @PreAuthorize("@ss.hasPermi('oa:recur:list')")
    @GetMapping("/list")
    public TableDataInfo list(RecurQuery query)
    {
        startPage();
        return getDataTable(recurService.selectRecurList(query));
    }

    /**
     * 新建规则
     */
    @PreAuthorize("@ss.hasPermi('oa:recur:add')")
    @Log(title = "周期规则", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RecurSaveDTO dto)
    {
        return success(recurService.insertRecur(dto));
    }

    /**
     * 编辑规则（applyScope=one 仅本次 / all 本次及以后）
     */
    @PreAuthorize("@ss.hasPermi('oa:recur:edit')")
    @Log(title = "周期规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RecurSaveDTO dto)
    {
        recurService.updateRecur(dto);
        return success();
    }

    /**
     * 暂停 / 恢复 / 结束规则
     */
    @PreAuthorize("@ss.hasPermi('oa:recur:edit')")
    @Log(title = "周期规则状态", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult changeStatus(@RequestBody RecurStatusDTO dto)
    {
        recurService.updateStatus(dto);
        return success();
    }

    /**
     * 批量删除周期规则（硬删除，已生成任务保留）
     */
    @PreAuthorize("@ss.hasPermi('oa:recur:remove')")
    @Log(title = "周期规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recurIds}")
    public AjaxResult remove(@PathVariable Long[] recurIds)
    {
        recurService.deleteRecurs(recurIds);
        return success();
    }
}
