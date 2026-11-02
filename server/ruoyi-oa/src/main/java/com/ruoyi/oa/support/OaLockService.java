package com.ruoyi.oa.support;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;

/**
 * 简易 Redis 业务锁：setIfAbsent 加锁、删除解锁
 *
 * @author ruoyi-oa
 */
@Component
public class OaLockService
{
    private static final Logger log = LoggerFactory.getLogger(OaLockService.class);

    @Autowired
    private RedisCache redisCache;

    /**
     * 尝试加锁
     *
     * @param key 锁 key
     * @param ttlSeconds 过期时间（秒）
     * @return true=加锁成功
     */
    public boolean tryLock(String key, long ttlSeconds)
    {
        if (StringUtils.isEmpty(key))
        {
            return false;
        }
        try
        {
            Boolean ok = redisCache.redisTemplate.opsForValue().setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(ok);
        }
        catch (Exception e)
        {
            // Redis 不可用时降级为放行：由数据库行锁 + 唯一索引兜底
            log.warn("Redis 加锁失败，降级放行：key={}", key, e);
            return true;
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String key)
    {
        if (StringUtils.isEmpty(key))
        {
            return;
        }
        try
        {
            redisCache.deleteObject(key);
        }
        catch (Exception e)
        {
            log.warn("Redis 解锁失败：key={}", key, e);
        }
    }
}
