package com.ruoyi.oa.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.member.mapper.OaAccountMapper;

/**
 * 职务 → 角色同步（1/2 会长副会长 → oa_president；3/4 部长副部长 → oa_minister；5 干事 → oa_staff）
 *
 * @author ruoyi-oa
 */
@Service
public class RoleSyncService
{
    @Autowired
    private OaAccountMapper accountMapper;

    /**
     * 按职务同步 OA 业务角色（删除旧业务角色，保留其他角色）
     */
    public void sync(Long userId, String position)
    {
        if (userId == null)
        {
            return;
        }
        Long roleId = accountMapper.selectRoleIdByKey(roleKeyOf(position));
        if (roleId == null)
        {
            return;
        }
        accountMapper.deleteBusinessRoles(userId);
        accountMapper.insertUserRole(userId, roleId);
    }

    /**
     * 职务对应的角色 key
     */
    public static String roleKeyOf(String position)
    {
        if (OaMemberConstants.POSITION_PRESIDENT.equals(position)
                || OaMemberConstants.POSITION_VICE_PRESIDENT.equals(position))
        {
            return OaMemberConstants.ROLE_PRESIDENT;
        }
        if (OaMemberConstants.POSITION_MINISTER.equals(position)
                || OaMemberConstants.POSITION_VICE_MINISTER.equals(position))
        {
            return OaMemberConstants.ROLE_MINISTER;
        }
        return OaMemberConstants.ROLE_STAFF;
    }
}
