package com.ruoyi.oa.feedback.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.feedback.domain.OaFeedback;
import com.ruoyi.oa.feedback.dto.FeedbackQuery;
import com.ruoyi.oa.feedback.vo.FeedbackModuleVO;
import com.ruoyi.oa.feedback.vo.FeedbackStatsVO;

/**
 * 工单 / 意见反馈 Mapper
 *
 * @author ruoyi-oa
 */
public interface OaFeedbackMapper
{
    /**
     * 用户端列表（全部/我的，已过滤 del_flag='0'）
     */
    List<OaFeedback> selectFeedbackList(FeedbackQuery query);

    /**
     * 管理端列表（待处理优先 + 时间倒序）
     */
    List<OaFeedback> selectManageList(FeedbackQuery query);

    /**
     * 按主键查询（不过滤 del_flag，服务层判断 404）
     */
    OaFeedback selectFeedbackById(@Param("feedbackId") Long feedbackId);

    /**
     * 按主键查询并加行锁（仅未删除，回复用）
     */
    OaFeedback selectFeedbackByIdForUpdate(@Param("feedbackId") Long feedbackId);

    /**
     * 批量查询未删除工单（批量回复前逐条校验）
     */
    List<OaFeedback> selectFeedbackByIds(@Param("ids") Long[] ids);

    /**
     * 新增工单
     */
    int insertFeedback(OaFeedback feedback);

    /**
     * 提交人撤回（条件更新：本人 + 待处理 + 未删除 → del_flag=2）
     */
    int withdrawFeedback(@Param("feedbackId") Long feedbackId, @Param("submitterId") Long submitterId);

    /**
     * 官方回复（置已回复并写回复内容/人/时间）
     */
    int replyFeedback(OaFeedback feedback);

    /**
     * 改回待处理并清空回复内容/人/时间
     */
    int reopenFeedback(@Param("feedbackId") Long feedbackId);

    /**
     * 批量标记已回复
     */
    int batchReply(@Param("ids") Long[] ids, @Param("replyContent") String replyContent,
            @Param("replyBy") Long replyBy, @Param("replyByName") String replyByName);

    /**
     * 批量逻辑删除（del_flag=1，仅未删除数据）
     */
    int deleteFeedbackByIds(@Param("ids") Long[] ids);

    /**
     * 管理端统计卡
     */
    FeedbackStatsVO selectStats();

    /**
     * 指定日期前缀下最大工单序号（编号生成 DB 兜底）
     */
    Long selectMaxTicketNoSeq(@Param("datePart") String datePart);

    /**
     * 「影响模块/页面」两级下拉原始行（sys_menu status=0 且 visible=0）
     */
    List<FeedbackModuleVO> selectMenuOptions();

    /**
     * 按菜单ID查询名称/类型/父级（提交时快照与合法性校验）
     */
    FeedbackModuleVO selectMenuById(@Param("menuId") Long menuId);

}
