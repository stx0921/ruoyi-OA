package com.ruoyi.oa.apply.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.oa.apply.domain.OaMemberApply;
import com.ruoyi.oa.apply.dto.ApplyAuditDTO;
import com.ruoyi.oa.apply.service.IOaMemberApplyService;
import com.ruoyi.oa.apply.vo.ApplyListVO;

/**
 * 入会审批 Controller（PC-04）
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/apply")
public class OaApplyController extends BaseController
{
    @Autowired
    private IOaMemberApplyService applyService;

    /**
     * 查询审批列表（scope=todo/done/all）
     */
    @PreAuthorize("@ss.hasPermi('oa:apply:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(value = "scope", required = false) String scope, OaMemberApply query)
    {
        startPage();
        List<ApplyListVO> list = applyService.selectApplyList(scope, query);
        return getDataTable(list);
    }

    /**
     * 申请详情（完整表单 + 附件 + 时间轴）
     */
    @PreAuthorize("@ss.hasPermi('oa:apply:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(applyService.selectApplyDetail(id));
    }

    /**
     * 批量审批：同意 / 驳回（驳回意见 2–200 字必填，单次 ≤50 条）
     */
    @PreAuthorize("@ss.hasPermi('oa:apply:audit')")
    @Log(title = "入会审批", businessType = BusinessType.UPDATE)
    @PostMapping("/audit")
    public AjaxResult audit(@RequestBody ApplyAuditDTO dto)
    {
        return success(applyService.audit(dto));
    }

}
