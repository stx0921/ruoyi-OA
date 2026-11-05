package com.ruoyi.oa.task.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务详情 VO（列表字段 + 描述 / 子任务 / 协作人 / 附件 / 流转日志）
 *
 * @author ruoyi-oa
 */
public class TaskDetailVO extends TaskListVO
{
    private static final long serialVersionUID = 1L;

    /** 描述 */
    private String description;

    /** 父任务ID */
    private Long parentId;

    /** 父任务编号 */
    private String parentTaskNo;

    /** 子任务列表 */
    private List<TaskListVO> children = new ArrayList<TaskListVO>();

    /** 协作人列表 */
    private List<UserBriefVO> collaborators = new ArrayList<UserBriefVO>();

    /** 附件列表 */
    private List<TaskFileVO> files = new ArrayList<TaskFileVO>();

    /** 流转日志（倒序） */
    private List<TaskLogVO> logs = new ArrayList<TaskLogVO>();

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getParentTaskNo()
    {
        return parentTaskNo;
    }

    public void setParentTaskNo(String parentTaskNo)
    {
        this.parentTaskNo = parentTaskNo;
    }

    public List<TaskListVO> getChildren()
    {
        return children;
    }

    public void setChildren(List<TaskListVO> children)
    {
        this.children = children;
    }

    public List<UserBriefVO> getCollaborators()
    {
        return collaborators;
    }

    public void setCollaborators(List<UserBriefVO> collaborators)
    {
        this.collaborators = collaborators;
    }

    public List<TaskFileVO> getFiles()
    {
        return files;
    }

    public void setFiles(List<TaskFileVO> files)
    {
        this.files = files;
    }

    public List<TaskLogVO> getLogs()
    {
        return logs;
    }

    public void setLogs(List<TaskLogVO> logs)
    {
        this.logs = logs;
    }

}
