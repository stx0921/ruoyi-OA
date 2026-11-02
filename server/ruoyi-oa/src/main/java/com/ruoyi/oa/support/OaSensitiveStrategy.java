package com.ruoyi.oa.support;

import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.constant.OaMemberConstants;

/**
 * 敏感字段脱敏策略（R12 / D-06）
 *
 * <p>只有干事视角打码；部长（本部门）、主席团、管理员、本人一律明文。越权访问由服务层直接 403。</p>
 *
 * @author ruoyi-oa
 */
@Component
public class OaSensitiveStrategy
{
    @Autowired
    private OaCurrentUser currentUser;

    /**
     * 当前视角是否需要打码
     */
    public boolean mask()
    {
        try
        {
            if (currentUser.isAdmin())
            {
                return false;
            }
            Set<String> roles = currentUser.roleKeys();
            if (roles.contains(OaMemberConstants.ROLE_PRESIDENT) || roles.contains(OaMemberConstants.ROLE_MINISTER))
            {
                return false;
            }
            return true;
        }
        catch (Exception e)
        {
            return true;
        }
    }

    /**
     * 手机号打码：138****5678
     */
    public String phone(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return value;
        }
        return value.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 邮箱打码：a***@x.com
     */
    public String email(String value)
    {
        if (StringUtils.isEmpty(value) || value.indexOf('@') < 0)
        {
            return value;
        }
        return value.replaceAll("(^.)[^@]*(@.*$)", "$1***$2");
    }

    /**
     * 微信打码：zh***wx
     */
    public String wechat(String value)
    {
        return maskMiddle(value);
    }

    /**
     * QQ 打码：12***89
     */
    public String qq(String value)
    {
        return maskMiddle(value);
    }

    private String maskMiddle(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return value;
        }
        if (value.length() <= 2)
        {
            return "***";
        }
        if (value.length() <= 4)
        {
            return value.charAt(0) + "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
}
