package com.ruoyi.oa.feedback.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 提交工单 DTO
 *
 * @author ruoyi-oa
 */
public class FeedbackSubmitDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 类型（1 Bug / 2 优化建议 / 3 新增功能 / 4 其他） */
    private String type;

    /** 标题（2–50 字） */
    private String title;

    /** 影响模块ID（sys_menu.menu_id，一级目录） */
    private Long moduleId;

    /** 影响模块名称（前端回传仅作参考，服务端以菜单快照为准） */
    private String moduleName;

    /** 影响页面ID（sys_menu.menu_id，二级菜单） */
    private Long pageId;

    /** 影响页面名称（前端回传仅作参考，服务端以菜单快照为准） */
    private String pageName;

    /** 详细描述（10–2000 字） */
    private String content;

    /** 复现步骤（≤2000 字） */
    private String steps;

    /** 期望结果（≤1000 字） */
    private String expectResult;

    /** 是否匿名（0否 1是） */
    private String isAnonymous;

    /** 临时附件ID列表（上传接口返回的 fileId） */
    private List<Long> fileIds;

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public Long getModuleId()
    {
        return moduleId;
    }

    public void setModuleId(Long moduleId)
    {
        this.moduleId = moduleId;
    }

    public String getModuleName()
    {
        return moduleName;
    }

    public void setModuleName(String moduleName)
    {
        this.moduleName = moduleName;
    }

    public Long getPageId()
    {
        return pageId;
    }

    public void setPageId(Long pageId)
    {
        this.pageId = pageId;
    }

    public String getPageName()
    {
        return pageName;
    }

    public void setPageName(String pageName)
    {
        this.pageName = pageName;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getSteps()
    {
        return steps;
    }

    public void setSteps(String steps)
    {
        this.steps = steps;
    }

    public String getExpectResult()
    {
        return expectResult;
    }

    public void setExpectResult(String expectResult)
    {
        this.expectResult = expectResult;
    }

    public String getIsAnonymous()
    {
        return isAnonymous;
    }

    public void setIsAnonymous(String isAnonymous)
    {
        this.isAnonymous = isAnonymous;
    }

    public List<Long> getFileIds()
    {
        return fileIds;
    }

    public void setFileIds(List<Long> fileIds)
    {
        this.fileIds = fileIds;
    }

}
