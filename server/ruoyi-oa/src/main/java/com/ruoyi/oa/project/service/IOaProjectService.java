package com.ruoyi.oa.project.service;

import java.util.List;
import com.ruoyi.oa.project.dto.ProjectQuery;
import com.ruoyi.oa.project.dto.ProjectSaveDTO;
import com.ruoyi.oa.project.vo.ProjectDetailVO;
import com.ruoyi.oa.project.vo.ProjectListVO;
import com.ruoyi.oa.project.vo.ProjectSaveResultVO;

/**
 * 项目 Service（PC-05）
 *
 * @author ruoyi-oa
 */
public interface IOaProjectService
{
    /**
     * 项目列表（含任务数/已完成数/参与人数，@DataScope 部门数据权限）
     */
    List<ProjectListVO> selectProjectList(ProjectQuery query);

    /**
     * 项目详情（含参与成员与任务统计）
     */
    ProjectDetailVO selectProjectDetail(Long projectId);

    /**
     * 新建项目（负责人自动加入参与成员，编号 P+yyyy+2位）
     */
    ProjectSaveResultVO insertProject(ProjectSaveDTO dto);

    /**
     * 编辑项目（仅进行中可编辑，已结束 400）
     */
    void updateProject(ProjectSaveDTO dto);

    /**
     * 结束项目前置校验：项目存在、进行中、当前人有权限；返回未完成任务数
     */
    int prepareFinish(Long projectId);

    /**
     * 结束项目（归档）：置 status=1 + finish_time，自动暂停项目下启用中的周期规则并通知创建人
     *
     * @return 归档时未完成任务数
     */
    int finishProject(Long projectId);

    /**
     * 批量删除项目（硬删除）：项目下还有任务或周期规则时 409；级联删除参与成员与项目站内信。
     * 权限：管理员/主席团任意；部长仅本部门项目；不存在或已删除的 ID 跳过。
     */
    void deleteProjects(Long[] projectIds);
}
