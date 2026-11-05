package com.ruoyi.oa.notify.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.notify.domain.OaNotify;
import com.ruoyi.oa.notify.dto.NotifyQuery;

/**
 * 站内信 Mapper
 *
 * @author ruoyi-oa
 */
public interface OaNotifyMapper
{
    /**
     * 写入站内信（INSERT IGNORE：dedupeKey 为空时每次插入，非空时按 uk_user_dedupe 去重）
     *
     * @return 1=新插入 0=去重命中
     */
    int insertIgnore(OaNotify notify);

    /**
     * 批量写入站内信（INSERT IGNORE，按 (user_id, dedupe_key) 去重）
     */
    int insertBatchIgnore(@Param("list") List<OaNotify> list);

    /**
     * 当前登录人未读数
     */
    Long countUnread(@Param("userId") Long userId);

    /**
     * 站内信列表（只能查本人：userId 由服务端填充）
     */
    List<OaNotify> selectNotifyList(NotifyQuery query);

    /**
     * 标记指定站内信已读（仅限本人、仅未读行）
     */
    int markRead(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    /**
     * 全部标记已读（仅限本人、仅未读行）
     */
    int markAllRead(@Param("userId") Long userId);

    /**
     * 按业务清理站内信（任务删除 / 项目删除时级联清理）
     */
    int deleteByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);
}
