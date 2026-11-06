package com.ruoyi.oa.feedback.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.oa.feedback.dto.FeedbackBatchReplyDTO;
import com.ruoyi.oa.feedback.dto.FeedbackQuery;
import com.ruoyi.oa.feedback.dto.FeedbackReplyDTO;
import com.ruoyi.oa.feedback.dto.FeedbackSubmitDTO;
import com.ruoyi.oa.feedback.service.IOaFeedbackService;

/**
 * 工单 / 意见反馈 Controller（11 个接口）
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/feedback")
public class OaFeedbackController extends BaseController
{
    @Autowired
    private IOaFeedbackService feedbackService;

    /**
     * 用户端工单列表（scope=all/mine）
     */
    @PreAuthorize("@ss.hasPermi('oa:feedback:list')")
    @GetMapping("/list")
    public TableDataInfo list(FeedbackQuery query)
    {
        startPage();
        return getDataTable(feedbackService.selectFeedbackList(query));
    }

    /**
     * 工单详情（含附件 + 官方回复）；管理端仅授 manage 的角色也能查看
     */
    @PreAuthorize("@ss.hasAnyPermi('oa:feedback:list,oa:feedback:manage')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(feedbackService.selectFeedbackDetail(id));
    }

    /**
     * 提交工单，返回工单编号
     */
    @PreAuthorize("@ss.hasPermi('oa:feedback:add')")
    @RateLimiter(time = 60, count = 20)
    @Log(title = "意见反馈", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FeedbackSubmitDTO dto)
    {
        return success(feedbackService.submitFeedback(dto));
    }

    /**
     * 撤回工单（本人 + 待处理 + 未删除）
     */
    @PreAuthorize("@ss.hasPermi('oa:feedback:withdraw')")
    @Log(title = "意见反馈", businessType = BusinessType.UPDATE)
    @PutMapping("/withdraw/{id}")
    public AjaxResult withdraw(@PathVariable("id") Long id)
    {
        feedbackService.withdrawFeedback(id);
        return success();
    }

    /**
     * 上传附件（类型/大小/魔数校验，落 feedback_temp）
     */
    @PreAuthorize("@ss.hasPermi('oa:feedback:add')")
    @RateLimiter(time = 60, count = 30)
    @Log(title = "意见反馈", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file)
    {
        return success(feedbackService.uploadAttachment(file));
    }

    /**
     * 工单附件鉴权输出：图片内联预览、Word 下载（附件 URL 不再使用公开静态路径）
     */
    @PreAuthorize("@ss.hasAnyPermi('oa:feedback:list,oa:feedback:manage')")
    @GetMapping("/attachment/{fileId}")
    public void attachment(@PathVariable("fileId") Long fileId,
            @RequestParam(value = "download", required = false, defaultValue = "false") boolean download,
            javax.servlet.http.HttpServletResponse response)
    {
        feedbackService.writeAttachment(fileId, download, response);
    }

    /**
     * 「影响模块」两级下拉（登录即可，不加 @PreAuthorize）
     */
    @GetMapping("/module-options")
    public AjaxResult moduleOptions()
    {
        return success(feedbackService.selectModuleOptions());
    }

    /**
     * 管理端工单列表（待处理优先）
     */
    @PreAuthorize("@ss.hasPermi('oa:feedback:manage')")
    @GetMapping("/manage/list")
    public TableDataInfo manageList(FeedbackQuery query)
    {
        startPage();
        return getDataTable(feedbackService.selectManageList(query));
    }

    /**
     * 管理端统计卡
     */
    @PreAuthorize("@ss.hasPermi('oa:feedback:manage')")
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        return success(feedbackService.selectStats());
    }

    /**
     * 官方回复 / 改状态
     */
    @PreAuthorize("@ss.hasPermi('oa:feedback:reply')")
    @Log(title = "意见反馈", businessType = BusinessType.UPDATE)
    @PutMapping("/reply")
    public AjaxResult reply(@RequestBody FeedbackReplyDTO dto)
    {
        feedbackService.replyFeedback(dto);
        return success();
    }

    /**
     * 批量逻辑删除
     */
    @PreAuthorize("@ss.hasPermi('oa:feedback:remove')")
    @Log(title = "意见反馈", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(feedbackService.deleteFeedbackByIds(ids));
    }

    /**
     * 批量标记已回复
     */
    @PreAuthorize("@ss.hasPermi('oa:feedback:reply')")
    @Log(title = "意见反馈", businessType = BusinessType.UPDATE)
    @PutMapping("/batch-reply")
    public AjaxResult batchReply(@RequestBody FeedbackBatchReplyDTO dto)
    {
        int rows = feedbackService.batchReply(dto.getIds(), dto.getReplyContent());
        return success(rows);
    }

}
