package com.ruoyi.oa.member.service;

import java.util.List;
import com.ruoyi.oa.member.dto.DeptLeaderDTO;
import com.ruoyi.oa.member.dto.DeptSaveDTO;
import com.ruoyi.oa.member.vo.DeptOptionVO;
import com.ruoyi.oa.member.vo.DeptVO;

/**
 * 部门与负责人 Service
 *
 * @author ruoyi-oa
 */
public interface IOaDeptService
{
    /**
     * 组织架构根部门 ID（配置优先，避免上级部门改名后失效）
     */
    Long resolveOrgRootId();

    /**
     * 部门列表（含在籍人数、正/副部长）
     */
    List<DeptVO> selectDeptWithLeader();

    /**
     * 部门下拉选项
     */
    List<DeptOptionVO> selectDeptOptions();

    /**
     * 全部内设部门下拉选项（不做登录人数据过滤，H5 公开表单 / 导入校验用）
     */
    List<DeptOptionVO> selectAllDeptOptions();

    /**
     * 新增内设部门（挂在组织架构根部门下）
     */
    void addDept(DeptSaveDTO dto);

    /**
     * 编辑内设部门（名称 / 排序 / 状态）
     */
    void updateDept(DeptSaveDTO dto);

    /**
     * 删除内设部门
     */
    void deleteDept(Long deptId);

    /**
     * 保存部门负责人（整体覆盖）
     */
    void saveLeaders(DeptLeaderDTO dto);
}
