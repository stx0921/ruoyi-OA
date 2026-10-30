package com.ruoyi.oa.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 审批人解析所需的辅助查询
 *
 * <p>只做只读查询，不改动若依原有用户/角色表结构。</p>
 *
 * @author ruoyi-oa
 */
public interface OaAssigneeMapper
{
    /**
     * 按显示名（nick_name）查用户ID，用于把 sys_dept.leader 的姓名换成用户ID
     */
    @Select("select user_id from sys_user where nick_name = #{nickName} and status = '0' and del_flag = '0' limit 1")
    public Long selectUserIdByNickName(@Param("nickName") String nickName);

    /**
     * 按角色 key 查该角色下第一个在职用户ID
     */
    @Select("select u.user_id from sys_user u "
            + "join sys_user_role ur on ur.user_id = u.user_id "
            + "join sys_role r on r.role_id = ur.role_id "
            + "where r.role_key = #{roleKey} and r.status = '0' and u.status = '0' and u.del_flag = '0' "
            + "order by u.user_id limit 1")
    public Long selectUserIdByRoleKey(@Param("roleKey") String roleKey);
}
