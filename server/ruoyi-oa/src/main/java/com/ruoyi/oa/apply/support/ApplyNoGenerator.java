package com.ruoyi.oa.apply.support;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.apply.mapper.OaMemberApplyMapper;

/**
 * 申请编号生成器：AP + yyyyMMdd + 3 位序号（如 AP20261102007）
 * <p>Redis INCR 为主，DB MAX 兜底；Redis key {@code oa:seq:apply:{yyyyMMdd}} 保留 48 小时。</p>
 *
 * @author ruoyi-oa
 */
@Component
public class ApplyNoGenerator
{
    private static final Logger log = LoggerFactory.getLogger(ApplyNoGenerator.class);

    /** Redis 序列 key 前缀 */
    private static final String SEQ_KEY_PREFIX = "oa:seq:apply:";

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private OaMemberApplyMapper applyMapper;

    /**
     * 生成下一个申请编号
     *
     * @param now 当前时间（决定日期段）
     * @return 申请编号
     */
    public String nextApplyNo(Date now)
    {
        String day = new SimpleDateFormat("yyyyMMdd").format(now == null ? new Date() : now);
        String prefix = "AP" + day;
        String key = SEQ_KEY_PREFIX + day;
        try
        {
            Long seq = redisCache.redisTemplate.opsForValue().increment(key);
            if (seq != null && seq == 1L)
            {
                redisCache.expire(key, 48, TimeUnit.HOURS);
            }
            if (seq != null && seq > 0)
            {
                return prefix + String.format("%03d", seq);
            }
        }
        catch (Exception e)
        {
            log.warn("申请编号 Redis 序列生成失败，回退数据库自增", e);
        }
        // DB 兜底：查当日最大编号 +1
        int next = 1;
        try
        {
            String max = applyMapper.selectMaxApplyNo(prefix);
            if (StringUtils.isNotEmpty(max) && max.length() > prefix.length())
            {
                next = Integer.parseInt(max.substring(prefix.length())) + 1;
            }
        }
        catch (Exception e)
        {
            log.warn("申请编号数据库兜底查询失败，使用 001", e);
        }
        return prefix + String.format("%03d", next);
    }

}
