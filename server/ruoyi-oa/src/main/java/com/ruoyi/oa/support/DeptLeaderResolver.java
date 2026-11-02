package com.ruoyi.oa.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.member.domain.OaDeptLeader;
import com.ruoyi.oa.member.mapper.OaAccountMapper;
import com.ruoyi.oa.member.mapper.OaDeptLeaderMapper;
import com.ruoyi.system.mapper.SysUserMapper;

/**
 * 审批人解析：意向部门正部长 → 主席团兜底 → admin
 *
 * @author ruoyi-oa
 */
@Component
public class DeptLeaderResolver
{
    @Autowired
    private OaDeptLeaderMapper deptLeaderMapper;

    @Autowired
    private OaAccountMapper accountMapper;

    @Autowired
    private SysUserMapper userMapper;

    /**
     * 解析部门应审批人；部门未设置部长时返回主席团（proxy=true）
     */
    public LeaderRef resolve(Long deptId)
    {
        if (deptId != null)
        {
            OaDeptLeader primary = deptLeaderMapper.selectPrimaryLeader(deptId);
            if (primary != null)
            {
                SysUser leader = userMapper.selectUserById(primary.getUserId());
                return new LeaderRef(primary.getUserId(), leader == null ? "" : leader.getNickName(), false);
            }
        }
        SysUser president = accountMapper.selectFirstUserByRoleKey(OaMemberConstants.ROLE_PRESIDENT);
        if (president != null)
        {
            return new LeaderRef(president.getUserId(), president.getNickName(), true);
        }
        SysUser admin = userMapper.selectUserById(1L);
        return new LeaderRef(1L, admin == null ? "admin" : admin.getNickName(), true);
    }

    /**
     * 是否该部门负责人（正/副部长）
     */
    public boolean isLeaderOf(Long deptId, Long userId)
    {
        if (deptId == null || userId == null)
        {
            return false;
        }
        return deptLeaderMapper.countLeader(deptId, userId) > 0;
    }

    /**
     * 判断当前登录人能否审批该部门申请：管理员 / 主席团（代审） / 本部门负责人
     */
    public boolean canAudit(Long deptId, Long userId, boolean admin, boolean president)
    {
        return admin || president || isLeaderOf(deptId, userId);
    }

    /**
     * 是否属于代审（当前人不是部门负责人，靠管理员/主席团身份兜底）
     */
    public boolean isProxy(Long deptId, Long userId, boolean admin, boolean president)
    {
        if (isLeaderOf(deptId, userId))
        {
            return false;
        }
        return admin || president;
    }
}
