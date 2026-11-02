package com.ruoyi.oa.member.mapper;

import java.util.List;
import com.ruoyi.oa.member.vo.DeptOptionVO;
import com.ruoyi.oa.member.vo.DeptVO;

/**
 * 组织部门 Mapper（复用 sys_dept）
 *
 * @author ruoyi-oa
 */
public interface OaDeptMapper
{
    /**
     * 查询组织根节点 ID（parent_id=0 且 dept_name='校学生会'）
     */
    Long selectOrgRootDeptId();

    /**
     * 查询组织根节点下的内设部门（含在籍人数）
     */
    List<DeptVO> selectOrgDeptList();

    /**
     * 查询部门下拉选项（组织根节点下启用部门）
     */
    List<DeptOptionVO> selectOrgDeptOptions();

    /**
     * 同步 sys_dept.leader 负责人姓名
     */
    int updateDeptLeader(@org.apache.ibatis.annotations.Param("deptId") Long deptId,
            @org.apache.ibatis.annotations.Param("leader") String leader);
}
