package com.ruoyi.oa.volunteer.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
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
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.oa.volunteer.dto.SignupAuditDTO;
import com.ruoyi.oa.volunteer.dto.SignupQuery;
import com.ruoyi.oa.volunteer.service.IVolunteerService;
import com.ruoyi.oa.volunteer.vo.SignupExportVO;

/**
 * 志愿活动报名管理 Controller
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/volunteer/signup")
public class VolunteerSignupController extends BaseController
{
    @Autowired
    private IVolunteerService volunteerService;

    /**
     * 某活动报名名单
     */
    @PreAuthorize("@ss.hasPermi('oa:volunteer:list')")
    @GetMapping("/list")
    public TableDataInfo list(SignupQuery query)
    {
        startPage();
        return getDataTable(volunteerService.selectSignupList(query));
    }

    /**
     * 单条/批量审核报名（1通过 2驳回）
     */
    @PreAuthorize("@ss.hasPermi('oa:volunteer:audit')")
    @Log(title = "志愿活动", businessType = BusinessType.UPDATE)
    @PutMapping("/audit")
    public AjaxResult audit(@RequestBody SignupAuditDTO dto)
    {
        volunteerService.auditSignup(dto);
        return success();
    }

    /**
     * 导出某活动报名名单（xlsx 流）
     */
    @PreAuthorize("@ss.hasPermi('oa:volunteer:export')")
    @Log(title = "志愿活动", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(HttpServletResponse response, SignupQuery query)
    {
        List<SignupExportVO> list = volunteerService.selectSignupExportList(query);
        ExcelUtil<SignupExportVO> util = new ExcelUtil<SignupExportVO>(SignupExportVO.class);
        util.exportExcel(response, list, "报名名单");
    }

}
