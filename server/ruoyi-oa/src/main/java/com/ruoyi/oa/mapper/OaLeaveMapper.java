package com.ruoyi.oa.mapper;

import java.util.List;
import com.ruoyi.oa.domain.OaLeave;

/**
 * 请假申请 Mapper
 *
 * @author ruoyi-oa
 */
public interface OaLeaveMapper
{
    /**
     * 查询请假申请列表
     */
    public List<OaLeave> selectOaLeaveList(OaLeave oaLeave);

    /**
     * 根据ID查询请假申请
     */
    public OaLeave selectOaLeaveById(Long leaveId);

    /**
     * 根据流程实例ID查询请假申请
     */
    public OaLeave selectOaLeaveByProcessInstanceId(String processInstanceId);

    /**
     * 新增请假申请
     */
    public int insertOaLeave(OaLeave oaLeave);

    /**
     * 修改请假申请
     */
    public int updateOaLeave(OaLeave oaLeave);

    /**
     * 删除请假申请
     */
    public int deleteOaLeaveById(Long leaveId);

    /**
     * 批量删除请假申请
     */
    public int deleteOaLeaveByIds(Long[] leaveIds);
}
