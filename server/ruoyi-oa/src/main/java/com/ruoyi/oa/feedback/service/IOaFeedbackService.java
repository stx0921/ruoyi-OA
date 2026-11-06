package com.ruoyi.oa.feedback.service;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.oa.feedback.dto.FeedbackQuery;
import com.ruoyi.oa.feedback.dto.FeedbackReplyDTO;
import com.ruoyi.oa.feedback.dto.FeedbackSubmitDTO;
import com.ruoyi.oa.feedback.vo.FeedbackDetailVO;
import com.ruoyi.oa.feedback.vo.FeedbackFileVO;
import com.ruoyi.oa.feedback.vo.FeedbackListVO;
import com.ruoyi.oa.feedback.vo.FeedbackModuleVO;
import com.ruoyi.oa.feedback.vo.FeedbackStatsVO;

/**
 * 工单 / 意见反馈 Service
 *
 * @author ruoyi-oa
 */
public interface IOaFeedbackService
{
    /**
     * 用户端列表（scope=all/mine；匿名脱敏在服务端完成）
     */
    List<FeedbackListVO> selectFeedbackList(FeedbackQuery query);

    /**
     * 工单详情（含附件与官方回复；del_flag != 0 返回 404）
     */
    FeedbackDetailVO selectFeedbackDetail(Long feedbackId);

    /**
     * 提交工单
     *
     * @return 工单编号 ticketNo
     */
    String submitFeedback(FeedbackSubmitDTO dto);

    /**
     * 提交人撤回（本人 + 待处理 + 未删除）
     */
    void withdrawFeedback(Long feedbackId);

    /**
     * 「影响模块」两级下拉（sys_menu 一级目录 + 二级页面）
     */
    List<FeedbackModuleVO> selectModuleOptions();

    /**
     * 管理端列表（待处理优先；返回真实提交人 + isAnonymous 标记）
     */
    List<FeedbackListVO> selectManageList(FeedbackQuery query);

    /**
     * 管理端统计卡
     */
    FeedbackStatsVO selectStats();

    /**
     * 官方回复 / 改状态（status=0 时清空回复内容/人/时间）
     */
    void replyFeedback(FeedbackReplyDTO dto);

    /**
     * 批量逻辑删除（del_flag=1）
     */
    int deleteFeedbackByIds(Long[] feedbackIds);

    /**
     * 批量标记已回复（默认文案「已批量处理，感谢反馈」）
     */
    int batchReply(Long[] feedbackIds, String replyContent);

    /**
     * 上传临时附件（feedback_temp）
     */
    FeedbackFileVO uploadAttachment(MultipartFile file);

    /**
     * 鉴权输出工单附件（attachment=true 下载；false 内联预览）
     */
    void writeAttachment(Long fileId, boolean attachment, HttpServletResponse response);

}
