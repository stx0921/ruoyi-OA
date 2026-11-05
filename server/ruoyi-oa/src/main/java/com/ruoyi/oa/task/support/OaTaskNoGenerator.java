package com.ruoyi.oa.task.support;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.oa.task.mapper.OaTaskMapper;

/**
 * 任务编号生成器：T + yyyyMMdd + 3 位序号，如 T20261102001
 *
 * <p>Redis INCR 为主，首次使用/Redis 不可用时退化为数据库最大序号 + 1。</p>
 *
 * @author ruoyi-oa
 */
@Component
public class OaTaskNoGenerator
{
    private static final Logger log = LoggerFactory.getLogger(OaTaskNoGenerator.class);

    /** Redis 序号 key 前缀（oa:seq:task:{yyyyMMdd}） */
    private static final String TASK_SEQ_KEY = "oa:seq:task:";

    /** 序号 key 有效期（天） */
    private static final long SEQ_KEY_TTL_DAYS = 2L;

    @Autowired
    private OaTaskMapper taskMapper;

    @Autowired
    private RedisCache redisCache;

    /**
     * 生成下一个任务编号（契约 §3.3：Job / 监听器调用 next()）
     */
    public String next()
    {
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String key = TASK_SEQ_KEY + datePart;
        long seq;
        synchronized (this)
        {
            try
            {
                Long current = redisCache.redisTemplate.opsForValue().increment(key);
                if (current != null && current.longValue() == 1L)
                {
                    // 首次使用：用数据库最大序号初始化，避免重启 / 缓存失效后重号
                    Long max = taskMapper.selectMaxTaskNoSeq(datePart);
                    long value = (max == null ? 0L : max.longValue()) + 1L;
                    redisCache.redisTemplate.opsForValue().set(key, value, SEQ_KEY_TTL_DAYS, TimeUnit.DAYS);
                    current = value;
                }
                seq = current == null ? 1L : current.longValue();
            }
            catch (Exception e)
            {
                log.warn("Redis 任务序号不可用，退化为数据库最大值+1：{}", e.getMessage());
                Long max = taskMapper.selectMaxTaskNoSeq(datePart);
                seq = (max == null ? 0L : max.longValue()) + 1L;
            }
        }
        return String.format("T%s%03d", datePart, seq);
    }

    /**
     * 语义别名（与 03 文档一致的命名）
     */
    public String nextTaskNo()
    {
        return next();
    }
}
