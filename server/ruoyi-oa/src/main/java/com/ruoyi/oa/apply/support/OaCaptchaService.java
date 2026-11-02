package com.ruoyi.oa.apply.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.common.utils.sign.Base64;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.oa.apply.vo.CaptchaVO;
import com.ruoyi.oa.apply.vo.VerifyTokenVO;
import com.ruoyi.oa.constant.OaMemberConstants;

/**
 * H5 图形验证码服务（v1 替代短信）
 * <p>
 * 方案：图形码一次性校验 → 30 分钟 verifyToken（上传/提交复用） → 进度查询成功发 30 分钟 queryToken（绑手机号）。
 * 图形码与令牌均存 Redis：{@code oa:captcha:{uuid}}、{@code oa:verify:token:{token}}、{@code oa:query:token:{token}}。
 * </p>
 * <p>
 * 说明：kaptcha 依赖位于 ruoyi-framework 模块，ruoyi-oa 不直接依赖其 jar，因此这里通过 Spring 容器按 bean 名
 * {@code captchaProducerMath} 取到若依既有验证码组件（见 CaptchaController），再以反射方式调用其 Producer 接口方法。
 * </p>
 *
 * @author ruoyi-oa
 */
@Component
public class OaCaptchaService
{
    private static final Logger log = LoggerFactory.getLogger(OaCaptchaService.class);

    /** 图形码失败限流 key 前缀（同 IP 10 分钟失败上限） */
    private static final String CAPTCHA_FAIL_KEY = "oa:limit:captcha:fail:";

    /** 同号 24h 提交次数 key 前缀 */
    private static final String SUBMIT_LIMIT_KEY = "oa:limit:submit:24h:";

    /** 图形验证码 bean 名（与若依 CaptchaController 一致） */
    private static final String CAPTCHA_PRODUCER_BEAN = "captchaProducerMath";

    @Autowired
    private RedisCache redisCache;

    /** 图形码有效期（秒） */
    @Value("${oa.captcha.expire-seconds:300}")
    private int expireSeconds;

    /** verifyToken 有效期（分钟） */
    @Value("${oa.captcha.verify-token-minutes:30}")
    private int verifyTokenMinutes;

    /** queryToken 有效期（分钟） */
    @Value("${oa.captcha.query-token-minutes:30}")
    private int queryTokenMinutes;

    /** 同 IP 失败上限 */
    @Value("${oa.captcha.fail-limit:10}")
    private int failLimit;

    /** 同号 24h 提交上限 */
    @Value("${oa.submit.phone-24h-limit:5}")
    private int submitLimit;

    /** 若依 kaptcha Producer（懒加载） */
    private Object captchaProducer;

    /** Producer#createText() */
    private Method createTextMethod;

    /** Producer#createImage(String) */
    private Method createImageMethod;

    /**
     * 生成图形验证码（一次性，5 分钟）
     */
    public CaptchaVO generate()
    {
        ensureProducer();
        try
        {
            String capText = (String) createTextMethod.invoke(captchaProducer);
            if (StringUtils.isEmpty(capText) || capText.lastIndexOf("@") < 0)
            {
                throw new ServiceException("验证码生成失败，请稍后再试", 500);
            }
            String capStr = capText.substring(0, capText.lastIndexOf("@"));
            String code = capText.substring(capText.lastIndexOf("@") + 1);
            BufferedImage image = (BufferedImage) createImageMethod.invoke(captchaProducer, capStr);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", os);

            String uuid = IdUtils.simpleUUID();
            redisCache.setCacheObject(OaMemberConstants.CAPTCHA_KEY + uuid, code, expireSeconds, TimeUnit.SECONDS);

            CaptchaVO vo = new CaptchaVO();
            vo.setUuid(uuid);
            vo.setImg(Base64.encode(os.toByteArray()));
            vo.setExpireIn(expireSeconds);
            return vo;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("生成图形验证码失败", e);
            throw new ServiceException("验证码生成失败，请稍后再试", 500);
        }
    }

    /**
     * 校验图形验证码（一次性：先删后比），成功返回 verifyToken（30 分钟可复用）
     *
     * @param uuid 图形码 UUID
     * @param code 用户输入
     * @param ip   请求 IP（失败限流用）
     */
    public VerifyTokenVO verify(String uuid, String code, String ip)
    {
        if (StringUtils.isEmpty(uuid) || StringUtils.isEmpty(code))
        {
            throw new ServiceException("验证码错误，请重新获取", 400);
        }
        String key = OaMemberConstants.CAPTCHA_KEY + uuid;
        Object cached = redisCache.getCacheObject(key);
        if (cached == null)
        {
            throw new ServiceException("验证码已过期，请重新获取", 400);
        }
        // 一次性：先删除，保证并发下只有一次校验机会
        redisCache.deleteObject(key);
        if (!code.trim().equalsIgnoreCase(String.valueOf(cached)))
        {
            recordCaptchaFail(ip);
            throw new ServiceException("验证码错误，请重新获取", 400);
        }
        String token = IdUtils.simpleUUID();
        redisCache.setCacheObject(OaMemberConstants.VERIFY_TOKEN_KEY + token, "1", verifyTokenMinutes, TimeUnit.MINUTES);
        VerifyTokenVO vo = new VerifyTokenVO();
        vo.setVerifyToken(token);
        vo.setExpireIn(verifyTokenMinutes * 60);
        return vo;
    }

    /**
     * 校验 verifyToken（上传/提交必需），无效返回 400
     */
    public void requireVerifyToken(String verifyToken)
    {
        if (StringUtils.isEmpty(verifyToken))
        {
            throw new ServiceException("验证已过期，请重新验证", 400);
        }
        Object value = redisCache.getCacheObject(OaMemberConstants.VERIFY_TOKEN_KEY + verifyToken);
        if (value == null)
        {
            throw new ServiceException("验证已过期，请重新验证", 400);
        }
    }

    /**
     * 进度查询成功后生成 queryToken（30 分钟，绑定手机号）
     */
    public String createQueryToken(String phone)
    {
        String token = IdUtils.simpleUUID();
        redisCache.setCacheObject(OaMemberConstants.QUERY_TOKEN_KEY + token, phone, queryTokenMinutes, TimeUnit.MINUTES);
        return token;
    }

    /**
     * 校验 queryToken 与手机号是否匹配（重提/撤回用），无效返回 400
     */
    public void requireQueryToken(String queryToken, String phone)
    {
        if (StringUtils.isEmpty(queryToken))
        {
            throw new ServiceException("验证已过期，请重新验证", 400);
        }
        Object boundPhone = redisCache.getCacheObject(OaMemberConstants.QUERY_TOKEN_KEY + queryToken);
        if (boundPhone == null || !String.valueOf(boundPhone).equals(phone))
        {
            throw new ServiceException("验证已过期，请重新验证", 400);
        }
    }

    /**
     * 校验同号 24h 提交次数（R2/R8），超限 429
     */
    public void checkSubmitLimit(String phone)
    {
        Object value = redisCache.getCacheObject(SUBMIT_LIMIT_KEY + phone);
        if (value != null && Integer.parseInt(String.valueOf(value)) >= submitLimit)
        {
            throw new ServiceException("24 小时内提交次数已达上限（" + submitLimit + " 次），请明天再试", 429);
        }
    }

    /**
     * 记录一次成功提交（24h 滚动窗口）
     */
    public void recordSubmit(String phone)
    {
        try
        {
            String key = SUBMIT_LIMIT_KEY + phone;
            Long count = redisCache.redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L)
            {
                redisCache.expire(key, 24, TimeUnit.HOURS);
            }
        }
        catch (Exception e)
        {
            // 限流计数失败不阻断业务
            log.warn("提交限流计数失败 phone={}", phone, e);
        }
    }

    /**
     * verifyToken 有效期（秒），供接口返回值参考
     */
    public int getVerifyTokenExpireIn()
    {
        return verifyTokenMinutes * 60;
    }

    private void recordCaptchaFail(String ip)
    {
        try
        {
            String key = CAPTCHA_FAIL_KEY + StringUtils.nvl(ip, "unknown");
            Long count = redisCache.redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L)
            {
                redisCache.expire(key, 10, TimeUnit.MINUTES);
            }
            if (count != null && count >= failLimit)
            {
                throw new ServiceException("操作过于频繁，请稍后再试", 429);
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.warn("验证码失败限流计数失败 ip={}", ip, e);
        }
    }

    /**
     * 懒加载若依 kaptcha Producer（按 bean 名，兼容 ruoyi-oa 未直接依赖 kaptcha jar 的情况）
     */
    private synchronized void ensureProducer()
    {
        if (captchaProducer != null && createTextMethod != null && createImageMethod != null)
        {
            return;
        }
        try
        {
            captchaProducer = SpringUtils.getBean(CAPTCHA_PRODUCER_BEAN);
        }
        catch (Exception e)
        {
            log.error("未找到图形验证码 bean：{}", CAPTCHA_PRODUCER_BEAN, e);
            throw new ServiceException("图形验证码组件未初始化，请联系管理员", 500);
        }
        for (Method method : captchaProducer.getClass().getMethods())
        {
            if ("createText".equals(method.getName()) && method.getParameterTypes().length == 0)
            {
                createTextMethod = method;
            }
            else if ("createImage".equals(method.getName())
                    && method.getParameterTypes().length == 1
                    && String.class.equals(method.getParameterTypes()[0]))
            {
                createImageMethod = method;
            }
        }
        if (createTextMethod == null || createImageMethod == null)
        {
            throw new ServiceException("图形验证码组件不可用，请联系管理员", 500);
        }
    }

}
