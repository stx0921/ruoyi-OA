package com.ruoyi.oa.member.service;

import java.util.List;
import com.ruoyi.oa.member.dto.DeptLeaderDTO;
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
     * 部门列表（含在籍人数、正/副部长）
     */
    List<DeptVO> selectDeptWithLeader();

    /**
     * 部门下拉选项
     */
    List<DeptOptionVO> selectDeptOptions();

    /**
     * 保存部门负责人（整体覆盖）
     */
    void saveLeaders(DeptLeaderDTO dto);
}
