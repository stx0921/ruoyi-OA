package com.ruoyi.oa.volunteer.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.oa.volunteer.dto.StatQuery;
import com.ruoyi.oa.volunteer.service.IVolunteerService;
import com.ruoyi.oa.volunteer.vo.StatExportVO;

/**
 * 志愿活动统计 Controller
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/volunteer/stat")
public class VolunteerStatController extends BaseController
{
    @Autowired
    private IVolunteerService volunteerService;

    /**
     * 统计概览（totalHours / activityCount / participantCount / avgHours）
     */
    @PreAuthorize("@ss.hasPermi('oa:volunteer:list')")
    @GetMapping("/overview")
    public AjaxResult overview(StatQuery query)
    {
        return success(volunteerService.selectStatOverview(query));
    }

    /**
     * 按月统计（升序、空月补 0）
     */
    @PreAuthorize("@ss.hasPermi('oa:volunteer:list')")
    @GetMapping("/monthly")
    public AjaxResult monthly(StatQuery query)
    {
        return success(volunteerService.selectStatMonthly(query));
    }

    /**
     * 导出统计（按月明细 + 合计行）
     */
    @PreAuthorize("@ss.hasPermi('oa:volunteer:export')")
    @Log(title = "志愿活动", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(HttpServletResponse response, StatQuery query)
    {
        List<StatExportVO> list = volunteerService.selectStatExport(query);
        ExcelUtil<StatExportVO> util = new ExcelUtil<StatExportVO>(StatExportVO.class);
        util.exportExcel(response, list, "志愿活动统计");
    }

}
