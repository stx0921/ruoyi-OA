package com.ruoyi.oa.volunteer.controller;

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
import com.ruoyi.oa.volunteer.dto.ActivityQuery;
import com.ruoyi.oa.volunteer.dto.ActivitySaveDTO;
import com.ruoyi.oa.volunteer.service.IVolunteerService;

/**
 * 志愿活动管理 Controller
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/volunteer/activity")
public class VolunteerActivityController extends BaseController
{
    @Autowired
    private IVolunteerService volunteerService;

    /**
     * 活动列表（keyword/showStatus/beginTime/endTime）
     */
    @PreAuthorize("@ss.hasPermi('oa:volunteer:list')")
    @GetMapping("/list")
    public TableDataInfo list(ActivityQuery query)
    {
        startPage();
        return getDataTable(volunteerService.selectActivityList(query));
    }

    /**
     * 活动详情（含报名统计 + 二维码）
     */
    @PreAuthorize("@ss.hasPermi('oa:volunteer:list')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(volunteerService.selectActivityDetail(id));
    }

    /**
     * 新增活动
     */
    @PreAuthorize("@ss.hasPermi('oa:volunteer:add')")
    @Log(title = "志愿活动", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ActivitySaveDTO dto)
    {
        return success(volunteerService.insertActivity(dto));
    }

    /**
     * 修改活动（含取消活动 status=1）
     */
    @PreAuthorize("@ss.hasPermi('oa:volunteer:edit')")
    @Log(title = "志愿活动", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ActivitySaveDTO dto)
    {
        volunteerService.updateActivity(dto);
        return success();
    }

    /**
     * 批量软删除活动
     */
    @PreAuthorize("@ss.hasPermi('oa:volunteer:remove')")
    @Log(title = "志愿活动", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(volunteerService.deleteActivityByIds(ids));
    }

    /**
     * 活动二维码：{ content, base64 }
     */
    @PreAuthorize("@ss.hasPermi('oa:volunteer:list')")
    @GetMapping("/{id}/qrcode")
    public AjaxResult qrcode(@PathVariable("id") Long id)
    {
        return success(volunteerService.selectActivityQrCode(id));
    }

}
