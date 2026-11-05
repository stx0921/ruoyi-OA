package com.ruoyi.oa.member.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.member.domain.OaDeptLeader;

/**
 * 部门负责人 Mapper
 *
 * @author ruoyi-oa
 */
public interface OaDeptLeaderMapper
{
    /**
     * 查询某部门负责人列表
     */
    List<OaDeptLeader> selectLeadersByDeptId(Long deptId);

    /**
     * 查询某部门正部长（leader_type=1，按 sort 升序取第一个）
     */
    OaDeptLeader selectPrimaryLeader(Long deptId);

    /**
     * 查询全部负责人
     */
    List<OaDeptLeader> selectAllLeaders();

    /**
     * 判断某账号是否是某部门负责人（正/副）
     */
    int countLeader(@Param("deptId") Long deptId, @Param("userId") Long userId);

    /**
     * 统计某账号担任负责人的部门数（删除成员前校验）
     */
    int countByUserId(Long userId);

    /**
     * 删除某账号担任的全部部门负责人配置（退会 / 账号失效时清理）
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 删除某部门全部负责人
     */
    int deleteByDeptId(Long deptId);

    /**
     * 新增负责人
     */
    int insertLeader(OaDeptLeader leader);
}
