package com.ruoyi.oa.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.member.domain.OaMember;
import com.ruoyi.oa.member.mapper.OaAccountMapper;
import com.ruoyi.system.mapper.SysUserMapper;

/**
 * 成员账号生命周期（开号 / 停用 / 启用 / 重置密码 / 同步部门）
 *
 * @author ruoyi-oa
 */
@Service
public class MemberAccountService
{
    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private OaAccountMapper accountMapper;

    @Autowired
    private OaCurrentUser currentUser;

    /**
     * 幂等开通账号：user_name=学号、初始密码=手机号后 6 位、绑定 oa_staff
     */
    public SysUser ensureAccount(OaMember member)
    {
        if (member == null)
        {
            throw new ServiceException("成员信息不能为空");
        }
        if (member.getUserId() != null)
        {
            SysUser exists = userMapper.selectUserById(member.getUserId());
            if (exists != null)
            {
                return exists;
            }
        }
        SysUser byName = userMapper.selectUserByUserName(member.getStudentNo());
        if (byName != null)
        {
            if (!StringUtils.equals(byName.getPhonenumber(), member.getPhone()))
            {
                throw new ServiceException("该学号已存在账号且手机号不一致，请人工核对", 409);
            }
            return byName;
        }
        SysUser user = new SysUser();
        user.setDeptId(member.getDeptId());
        user.setUserName(member.getStudentNo());
        user.setNickName(member.getName());
        user.setEmail(member.getEmail());
        user.setPhonenumber(member.getPhone());
        user.setSex(member.getGender());
        user.setAvatar("");
        user.setPassword(SecurityUtils.encryptPassword(initialPassword(member.getPhone())));
        user.setStatus("0");
        user.setDelFlag("0");
        user.setCreateBy(currentUser.username());
        user.setRemark("人员录入自动开通，member_id=" + member.getMemberId());
        userMapper.insertUser(user);
        Long staffRoleId = accountMapper.selectRoleIdByKey(OaMemberConstants.ROLE_STAFF);
        if (staffRoleId != null)
        {
            accountMapper.insertUserRole(user.getUserId(), staffRoleId);
        }
        return user;
    }

    /**
     * 停用账号（退会 / 毕业，保留数据）
     */
    public void disableAccount(Long userId)
    {
        if (userId == null)
        {
            return;
        }
        userMapper.updateUserStatus(userId, "1");
    }

    /**
     * 启用账号（恢复在籍）
     */
    public void enableAccount(Long userId)
    {
        if (userId == null)
        {
            return;
        }
        userMapper.updateUserStatus(userId, "0");
    }

    /**
     * 重置密码为初始密码（手机号后 6 位）
     */
    public void resetPassword(Long userId, String phone)
    {
        if (userId == null)
        {
            throw new ServiceException("该成员尚未开通账号", 409);
        }
        userMapper.resetUserPwd(userId, SecurityUtils.encryptPassword(initialPassword(phone)));
    }

    /**
     * 同步账号部门
     */
    public void syncDept(Long userId, Long deptId)
    {
        if (userId == null || deptId == null)
        {
            return;
        }
        accountMapper.updateUserDept(userId, deptId);
    }

    /**
     * 初始密码：手机号后 6 位
     */
    public static String initialPassword(String phone)
    {
        if (StringUtils.isEmpty(phone) || phone.length() < 6)
        {
            return "123456";
        }
        return phone.substring(phone.length() - 6);
    }
}
