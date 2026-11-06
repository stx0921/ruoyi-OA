package com.ruoyi.oa.volunteer.support;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.oa.volunteer.mapper.VolunteerActivityMapper;

/**
 * 志愿活动编号生成器：VA + yyyyMMdd + 3 位序号，如 VA20261106001
 *
 * <p>Redis INCR 为主（key {@code oa:volunteer:no:{yyyyMMdd}}，TTL 2 天），
 * 首次使用/Redis 不可用时退化为数据库最大序号 + 1；唯一索引冲突由 Service 重试兜底。</p>
 *
 * @author ruoyi-oa
 */
@Component
public class VolunteerNoGenerator
{
    private static final Logger log = LoggerFactory.getLogger(VolunteerNoGenerator.class);

    /** Redis 序号 key 前缀 */
    private static final String ACTIVITY_NO_KEY = "oa:volunteer:no:";

    /** 序号 key 有效期（天） */
    private static final long SEQ_KEY_TTL_DAYS = 2L;

    @Autowired
    private VolunteerActivityMapper activityMapper;

    @Autowired
    private RedisCache redisCache;

    /**
     * 生成下一个活动编号
     */
    public String next()
    {
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String key = ACTIVITY_NO_KEY + datePart;
        long seq;
        synchronized (this)
        {
            try
            {
                Long current = redisCache.redisTemplate.opsForValue().increment(key);
                if (current != null && current.longValue() == 1L)
                {
                    // 首次使用：用数据库最大序号初始化，避免重启 / 缓存失效后重号
                    Long max = activityMapper.selectMaxActivityNoSeq(datePart);
                    long value = (max == null ? 0L : max.longValue()) + 1L;
                    redisCache.redisTemplate.opsForValue().set(key, value, SEQ_KEY_TTL_DAYS, TimeUnit.DAYS);
                    current = value;
                }
                seq = current == null ? 1L : current.longValue();
            }
            catch (Exception e)
            {
                log.warn("Redis 活动序号不可用，退化为数据库最大值+1：{}", e.getMessage());
                Long max = activityMapper.selectMaxActivityNoSeq(datePart);
                seq = (max == null ? 0L : max.longValue()) + 1L;
            }
        }
        return String.format("VA%s%03d", datePart, seq);
    }

}
