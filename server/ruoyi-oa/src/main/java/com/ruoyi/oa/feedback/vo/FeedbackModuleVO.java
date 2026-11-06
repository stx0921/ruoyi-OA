package com.ruoyi.oa.feedback.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 影响模块/页面两级下拉 VO
 *
 * @author ruoyi-oa
 */
public class FeedbackModuleVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 菜单ID */
    private Long id;

    /** 菜单名称 */
    private String name;

    /** 子菜单（页面） */
    private List<FeedbackModuleVO> children = new ArrayList<>();

    /** 父级ID（仅内部构建用，不序列化） */
    @JsonIgnore
    private Long parentId;

    /** 菜单类型 M/C（仅内部构建用，不序列化） */
    @JsonIgnore
    private String menuType;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<FeedbackModuleVO> getChildren()
    {
        return children;
    }

    public void setChildren(List<FeedbackModuleVO> children)
    {
        this.children = children;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getMenuType()
    {
        return menuType;
    }

    public void setMenuType(String menuType)
    {
        this.menuType = menuType;
    }

}
