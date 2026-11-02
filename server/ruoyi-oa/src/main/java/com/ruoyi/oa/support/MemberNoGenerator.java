package com.ruoyi.oa.support;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.member.mapper.OaMemberMapper;

/**
 * 成员编号生成器：M{入会年份}-{4 位序号}，如 M2026-0031
 *
 * @author ruoyi-oa
 */
@Component
public class MemberNoGenerator
{
    private static final Logger log = LoggerFactory.getLogger(MemberNoGenerator.class);

    @Autowired
    private OaMemberMapper memberMapper;

    @Autowired
    private RedisCache redisCache;

    /**
     * 生成成员编号
     */
    public String nextMemberNo(Date joinDate)
    {
        Date date = joinDate == null ? new Date() : joinDate;
        String year = new SimpleDateFormat("yyyy").format(date);
        String key = OaMemberConstants.MEMBER_SEQ_KEY + year;
        long seq;
        synchronized (this)
        {
            try
            {
                Long current = redisCache.redisTemplate.opsForValue().increment(key);
                if (current != null && current.longValue() == 1L)
                {
                    // 首次使用：用数据库最大序号初始化，避免重启后重号
                    Long max = memberMapper.selectMaxMemberNoSeq(year);
                    long value = (max == null ? 0L : max.longValue()) + 1L;
                    redisCache.redisTemplate.opsForValue().set(key, value, 30, TimeUnit.DAYS);
                    current = value;
                }
                seq = current == null ? 1L : current.longValue();
            }
            catch (Exception e)
            {
                log.warn("Redis 序号不可用，退化为数据库最大值+1：{}", e.getMessage());
                Long max = memberMapper.selectMaxMemberNoSeq(year);
                seq = (max == null ? 0L : max.longValue()) + 1L;
            }
        }
        return String.format("M%s-%04d", year, seq);
    }
}
