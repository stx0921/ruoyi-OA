package com.ruoyi.oa.notify.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.notify.domain.OaNotify;
import com.ruoyi.oa.notify.dto.NotifyQuery;
import com.ruoyi.oa.notify.mapper.OaNotifyMapper;
import com.ruoyi.oa.notify.service.IOaNotifyService;
import com.ruoyi.oa.notify.vo.NotifyVO;

/**
 * 站内信 Service 实现
 *
 * <p>写入统一 INSERT IGNORE：dedupeKey 非空时按 (user_id, dedupe_key) 幂等去重，为空时普通插入；
 * 通知属于旁路能力，写失败只记 ERROR 日志，不影响主业务事务。</p>
 *
 * @author ruoyi-oa
 */
@Service
public class OaNotifyServiceImpl implements IOaNotifyService
{
    private static final Logger log = LoggerFactory.getLogger(OaNotifyServiceImpl.class);

    /** 批量写入单批上限 */
    private static final int BATCH_LIMIT = 200;

    /** 站内信类型文案（与字典 oa_notify_type 一致） */
    private static final Map<String, String> TYPE_LABELS = new HashMap<String, String>();

    static
    {
        TYPE_LABELS.put("1", "被派任务");
        TYPE_LABELS.put("2", "周期提醒");
        TYPE_LABELS.put("3", "截止提醒");
        TYPE_LABELS.put("4", "验收结果");
        TYPE_LABELS.put("5", "系统通知");
    }

    @Autowired
    private OaNotifyMapper notifyMapper;

    @Override
    public void notify(Long userId, String type, String title, String content, String bizType, Long bizId,
            String dedupeKey)
    {
        if (userId == null)
        {
            return;
        }
        OaNotify notify = new OaNotify();
        notify.setUserId(userId);
        notify.setType(StringUtils.isEmpty(type) ? "5" : type);
        notify.setTitle(limit(StringUtils.isEmpty(title) ? "系统通知" : title, 200));
        notify.setContent(limit(StringUtils.nvl(content, ""), 1000));
        notify.setBizType(StringUtils.nvl(bizType, ""));
        notify.setBizId(bizId);
        notify.setReadFlag("0");
        notify.setDedupeKey(StringUtils.isEmpty(dedupeKey) ? null : dedupeKey);
        notify.setCreateTime(new Date());
        try
        {
            notifyMapper.insertIgnore(notify);
        }
        catch (Exception e)
        {
            // 通知失败不影响主流程
            log.error("站内信写入失败 userId={} type={} title={}", userId, type, title, e);
        }
    }

    @Override
    public void notifyBatch(List<OaNotify> list)
    {
        if (list == null || list.isEmpty())
        {
            return;
        }
        Date now = new Date();
        List<OaNotify> batch = new ArrayList<OaNotify>();
        for (OaNotify notify : list)
        {
            if (notify == null || notify.getUserId() == null)
            {
                continue;
            }
            if (StringUtils.isEmpty(notify.getType()))
            {
                notify.setType("5");
            }
            notify.setTitle(limit(StringUtils.isEmpty(notify.getTitle()) ? "系统通知" : notify.getTitle(), 200));
            notify.setContent(limit(StringUtils.nvl(notify.getContent(), ""), 1000));
            notify.setBizType(StringUtils.nvl(notify.getBizType(), ""));
            notify.setReadFlag("0");
            if (StringUtils.isEmpty(notify.getDedupeKey()))
            {
                notify.setDedupeKey(null);
            }
            notify.setCreateTime(now);
            batch.add(notify);
        }
        for (int from = 0; from < batch.size(); from += BATCH_LIMIT)
        {
            List<OaNotify> chunk = batch.subList(from, Math.min(from + BATCH_LIMIT, batch.size()));
            try
            {
                notifyMapper.insertBatchIgnore(chunk);
            }
            catch (Exception e)
            {
                log.error("站内信批量写入失败 size={}", chunk.size(), e);
            }
        }
    }

    @Override
    public Long countUnread(Long userId)
    {
        if (userId == null)
        {
            return 0L;
        }
        try
        {
            Long count = notifyMapper.countUnread(userId);
            return count == null ? 0L : count;
        }
        catch (Exception e)
        {
            log.error("查询未读数失败 userId={}", userId, e);
            return 0L;
        }
    }

    @Override
    public List<NotifyVO> selectNotifyList(NotifyQuery query)
    {
        List<NotifyVO> result = new ArrayList<NotifyVO>();
        if (query == null || query.getUserId() == null)
        {
            return result;
        }
        List<OaNotify> list = notifyMapper.selectNotifyList(query);
        for (OaNotify notify : list)
        {
            NotifyVO vo = new NotifyVO();
            vo.setNotifyId(notify.getNotifyId());
            vo.setType(notify.getType());
            vo.setTypeLabel(TYPE_LABELS.get(notify.getType()));
            vo.setTitle(notify.getTitle());
            vo.setContent(notify.getContent());
            vo.setBizType(notify.getBizType());
            vo.setBizId(notify.getBizId());
            vo.setReadFlag(notify.getReadFlag());
            vo.setReadTime(notify.getReadTime());
            vo.setCreateTime(notify.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    @Override
    public int markRead(Long userId, List<Long> ids)
    {
        if (userId == null || ids == null || ids.isEmpty())
        {
            return 0;
        }
        // 过滤空 ID，避免 SQL IN 出现 null
        List<Long> validIds = new ArrayList<Long>(ids.size());
        for (Long id : ids)
        {
            if (id != null)
            {
                validIds.add(id);
            }
        }
        if (validIds.isEmpty())
        {
            return 0;
        }
        return notifyMapper.markRead(userId, validIds);
    }

    @Override
    public int markAllRead(Long userId)
    {
        if (userId == null)
        {
            return 0;
        }
        return notifyMapper.markAllRead(userId);
    }

    @Override
    public void deleteByBiz(String bizType, Long bizId)
    {
        if (bizId == null)
        {
            return;
        }
        try
        {
            notifyMapper.deleteByBiz(StringUtils.nvl(bizType, ""), bizId);
        }
        catch (Exception e)
        {
            // 清理失败不影响主业务删除
            log.error("按业务清理站内信失败 bizType={} bizId={}", bizType, bizId, e);
        }
    }

    /**
     * 截断超长文本，避免超出列长度导致写入失败
     */
    private String limit(String text, int maxLength)
    {
        if (text == null)
        {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
