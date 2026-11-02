package com.ruoyi.oa.support;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.oa.member.mapper.OaAccountMapper;

/**
 * 当前登录人上下文工具（角色判定走数据库，避免依赖 LoginUser 中未加载的 roles）
 *
 * @author ruoyi-oa
 */
@Component
public class OaCurrentUser
{
    @Autowired
    private OaAccountMapper accountMapper;

    /**
     * 当前登录用户
     */
    public SysUser user()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return loginUser == null ? null : loginUser.getUser();
    }

    /**
     * 当前登录用户 ID
     */
    public Long userId()
    {
        return SecurityUtils.getUserId();
    }

    /**
     * 是否超级管理员
     */
    public boolean isAdmin()
    {
        SysUser user = user();
        return user != null && user.isAdmin();
    }

    /**
     * 当前用户角色 key 集合
     */
    public Set<String> roleKeys()
    {
        Set<String> result = new HashSet<>();
        try
        {
            List<String> keys = accountMapper.selectRoleKeysByUserId(userId());
            if (keys != null)
            {
                result.addAll(keys);
            }
        }
        catch (Exception e)
        {
            // 查询失败按无角色处理，脱敏策略取最强
        }
        return result;
    }

    /**
     * 是否拥有某角色
     */
    public boolean hasRole(String roleKey)
    {
        return roleKeys().contains(roleKey);
    }

    /**
     * 目标账号是否就是当前登录人
     */
    public boolean isSelf(Long targetUserId)
    {
        return targetUserId != null && targetUserId.equals(userId());
    }

    /**
     * 当前登录人昵称
     */
    public String nickName()
    {
        SysUser user = user();
        return user == null ? "" : user.getNickName();
    }

    /**
     * 当前登录人账号名
     */
    public String username()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return "";
        }
    }
}
