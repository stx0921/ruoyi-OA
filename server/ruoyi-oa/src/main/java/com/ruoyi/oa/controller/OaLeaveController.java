package com.ruoyi.oa.controller;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
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
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.oa.domain.OaLeave;
import com.ruoyi.oa.domain.vo.OaTaskHandleBody;
import com.ruoyi.oa.service.IOaLeaveService;

/**
 * 请假申请 Controller
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/leave")
public class OaLeaveController extends BaseController
{
    @Autowired
    private IOaLeaveService oaLeaveService;

    /**
     * 查询请假申请列表
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaLeave oaLeave)
    {
        startPage();
        List<OaLeave> list = oaLeaveService.selectOaLeaveList(oaLeave);
        return getDataTable(list);
    }

    /**
     * 导出请假申请列表
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:export')")
    @Log(title = "请假申请", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OaLeave oaLeave)
    {
        List<OaLeave> list = oaLeaveService.selectOaLeaveList(oaLeave);
        ExcelUtil<OaLeave> util = new ExcelUtil<OaLeave>(OaLeave.class);
        util.exportExcel(response, list, "请假申请数据");
    }

    /**
     * 获取请假申请详细信息
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:query')")
    @GetMapping(value = "/{leaveId}")
    public AjaxResult getInfo(@PathVariable("leaveId") Long leaveId)
    {
        return success(oaLeaveService.selectOaLeaveById(leaveId));
    }

    /**
     * 新增请假申请（草稿）
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:add')")
    @Log(title = "请假申请", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OaLeave oaLeave)
    {
        return toAjax(oaLeaveService.insertOaLeave(oaLeave));
    }

    /**
     * 修改请假申请
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:edit')")
    @Log(title = "请假申请", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OaLeave oaLeave)
    {
        return toAjax(oaLeaveService.updateOaLeave(oaLeave));
    }

    /**
     * 删除请假申请
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:remove')")
    @Log(title = "请假申请", businessType = BusinessType.DELETE)
    @DeleteMapping("/{leaveIds}")
    public AjaxResult remove(@PathVariable Long[] leaveIds)
    {
        return toAjax(oaLeaveService.deleteOaLeaveByIds(leaveIds));
    }

    /**
     * 提交审批（首次提交 / 驳回后重新提交）
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:submit')")
    @Log(title = "请假申请提交", businessType = BusinessType.UPDATE)
    @PostMapping("/submit/{leaveId}")
    public AjaxResult submit(@PathVariable("leaveId") Long leaveId)
    {
        oaLeaveService.submitLeave(leaveId);
        return success();
    }

    /**
     * 同意
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:approve')")
    @Log(title = "请假审批-同意", businessType = BusinessType.UPDATE)
    @PostMapping("/approve")
    public AjaxResult approve(@RequestBody OaTaskHandleBody body)
    {
        oaLeaveService.approveTask(body.getTaskId(), body.getComment());
        return success();
    }

    /**
     * 驳回（退回发起人修改）
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:approve')")
    @Log(title = "请假审批-驳回", businessType = BusinessType.UPDATE)
    @PostMapping("/reject")
    public AjaxResult reject(@RequestBody OaTaskHandleBody body)
    {
        oaLeaveService.rejectTask(body.getTaskId(), body.getComment());
        return success();
    }

    /**
     * 撤回
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:submit')")
    @Log(title = "请假申请撤回", businessType = BusinessType.UPDATE)
    @PostMapping("/revoke/{leaveId}")
    public AjaxResult revoke(@PathVariable("leaveId") Long leaveId)
    {
        oaLeaveService.revokeLeave(leaveId);
        return success();
    }

    /**
     * 流程跟踪
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:query')")
    @GetMapping("/trace/{leaveId}")
    public AjaxResult trace(@PathVariable("leaveId") Long leaveId)
    {
        Map<String, Object> trace = oaLeaveService.selectLeaveTrace(leaveId);
        return success(trace);
    }
}
