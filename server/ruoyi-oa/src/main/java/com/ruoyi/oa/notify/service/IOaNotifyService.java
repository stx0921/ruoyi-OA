package com.ruoyi.oa.notify.service;

import java.util.List;
import com.ruoyi.oa.notify.domain.OaNotify;
import com.ruoyi.oa.notify.dto.NotifyQuery;
import com.ruoyi.oa.notify.vo.NotifyVO;

/**
 * 站内信 Service（任务模块与人员模块共用）
 *
 * @author ruoyi-oa
 */
public interface IOaNotifyService
{
    /**
     * 单条通知（dedupeKey 为空时每次插入，非空时按 (user_id, dedupe_key) 幂等去重）
     *
     * @param userId    接收人
     * @param type      类型（1被派任务 2周期提醒 3截止提醒 4验收结果 5系统通知）
     * @param title     标题
     * @param content   内容
     * @param bizType   业务类型（task/recur/project）
     * @param bizId     业务ID
     * @param dedupeKey 去重键
     */
    void notify(Long userId, String type, String title, String content, String bizType, Long bizId,
            String dedupeKey);

    /**
     * 批量写入站内信（INSERT IGNORE，去重键命中自动跳过）
     */
    void notifyBatch(List<OaNotify> list);

    /**
     * 未读数
     */
    Long countUnread(Long userId);

    /**
     * 站内信列表（只能查本人：query.userId 由 Controller 强制填充）
     */
    List<NotifyVO> selectNotifyList(NotifyQuery query);

    /**
     * 标记已读（只能操作本人站内信）
     */
    int markRead(Long userId, List<Long> ids);

    /**
     * 全部已读（只能操作本人站内信）
     */
    int markAllRead(Long userId);

    /**
     * 按业务清理站内信（任务删除 / 项目删除时级联清理）
     *
     * @param bizType 业务类型（task/recur/project）
     * @param bizId   业务ID
     */
    void deleteByBiz(String bizType, Long bizId);
}
