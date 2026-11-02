package com.ruoyi.oa.member.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.common.core.domain.entity.SysUser;

/**
 * 组织人事模块与若依账号体系交互 Mapper
 *
 * @author ruoyi-oa
 */
public interface OaAccountMapper
{
    /**
     * 查询用户角色 key 列表
     */
    List<String> selectRoleKeysByUserId(Long userId);

    /**
     * 按 role_key 查询角色 ID
     */
    Long selectRoleIdByKey(String roleKey);

    /**
     * 删除用户的 OA 业务角色（oa_president / oa_minister / oa_staff）
     */
    int deleteBusinessRoles(Long userId);

    /**
     * 新增用户角色
     */
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 查询拥有某角色的第一个用户（主席团兜底审批人）
     */
    SysUser selectFirstUserByRoleKey(String roleKey);

    /**
     * 同步账号部门
     */
    int updateUserDept(@Param("userId") Long userId, @Param("deptId") Long deptId);

    /**
     * 同步账号手机号（成员编辑手机号时）
     */
    int updateUserPhone(@Param("userId") Long userId, @Param("phonenumber") String phonenumber);

    /**
     * 同步账号登录名（学号变更时）
     */
    int updateUserName(@Param("userId") Long userId, @Param("userName") String userName);
}
