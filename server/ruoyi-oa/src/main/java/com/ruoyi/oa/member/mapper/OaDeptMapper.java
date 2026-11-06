package com.ruoyi.oa.member.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.member.vo.DeptOptionVO;
import com.ruoyi.oa.member.vo.DeptVO;

/**
 * 组织部门 Mapper（复用 sys_dept）
 *
 * <p>注意：不再使用 dept_name 硬编码定位根部门——上级部门改名后会导致查不到，
 * 根部门改为由 {@code oa.org.rootDeptId} 参数或兜底规则解析（见 OaDeptServiceImpl）。</p>
 *
 * @author ruoyi-oa
 */
public interface OaDeptMapper
{
    /**
     * 按名称查询根节点（parent_id=0），仅作为默认安装的兜底
     */
    Long selectRootDeptIdByName(@Param("deptName") String deptName);

    /**
     * 兜底：查询“存在启用子部门”的根节点中 dept_id 最大的一个
     * （组织架构根节点通常晚于若依默认部门创建）
     */
    Long selectRootDeptIdWithActiveChild();

    /**
     * 查询组织根节点下的内设部门（含在籍人数）
     */
    List<DeptVO> selectOrgDeptList(@Param("rootId") Long rootId);

    /**
     * 查询部门下拉选项（组织根节点下启用部门）
     */
    List<DeptOptionVO> selectOrgDeptOptions(@Param("rootId") Long rootId);

    /**
     * 统计部门下未删除的成员数（删除部门前校验）
     */
    int countActiveMemberByDeptId(@Param("deptId") Long deptId);

    /**
     * 同步 sys_dept.leader 负责人姓名
     */
    int updateDeptLeader(@Param("deptId") Long deptId, @Param("leader") String leader);
}
