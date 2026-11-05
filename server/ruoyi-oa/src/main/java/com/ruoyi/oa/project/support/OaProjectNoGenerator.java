package com.ruoyi.oa.project.support;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.project.mapper.OaProjectMapper;

/**
 * 项目编号生成器：P + yyyy + 2 位序号（如 P202601）
 *
 * <p>Redis INCR 为主，DB MAX 兜底；Redis key {@code oa:seq:project:{yyyy}} 保留 48 小时。</p>
 *
 * @author ruoyi-oa
 */
@Component
public class OaProjectNoGenerator
{
    private static final Logger log = LoggerFactory.getLogger(OaProjectNoGenerator.class);

    /** Redis 序列 key 前缀 */
    private static final String SEQ_KEY_PREFIX = "oa:seq:project:";

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private OaProjectMapper projectMapper;

    /**
     * 生成下一个项目编号
     *
     * @param now 当前时间（决定年份段）
     * @return 项目编号
     */
    public String nextProjectNo(Date now)
    {
        String year = new SimpleDateFormat("yyyy").format(now == null ? new Date() : now);
        String prefix = "P" + year;
        String key = SEQ_KEY_PREFIX + year;
        try
        {
            Long seq = redisCache.redisTemplate.opsForValue().increment(key);
            if (seq != null && seq == 1L)
            {
                redisCache.expire(key, 48, TimeUnit.HOURS);
            }
            if (seq != null && seq > 0)
            {
                return prefix + String.format("%02d", seq);
            }
        }
        catch (Exception e)
        {
            log.warn("项目编号 Redis 序列生成失败，回退数据库自增", e);
        }
        // DB 兜底：查当年最大编号 +1
        int next = 1;
        try
        {
            String max = projectMapper.selectMaxProjectNo(prefix);
            if (StringUtils.isNotEmpty(max) && max.length() > prefix.length())
            {
                next = Integer.parseInt(max.substring(prefix.length())) + 1;
            }
        }
        catch (Exception e)
        {
            log.warn("项目编号数据库兜底查询失败，使用 01", e);
        }
        return prefix + String.format("%02d", next);
    }
}
