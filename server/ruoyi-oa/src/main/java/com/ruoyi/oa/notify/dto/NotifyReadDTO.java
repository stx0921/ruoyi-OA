package com.ruoyi.oa.notify.dto;

import java.util.List;

/**
 * 站内信标记已读入参：ids 非空时按 ID 标记，all=true 时全部已读
 *
 * @author ruoyi-oa
 */
public class NotifyReadDTO
{
    /** 要标记已读的站内信 ID 列表 */
    private List<Long> ids;

    /** 是否全部已读 */
    private Boolean all;

    public List<Long> getIds()
    {
        return ids;
    }

    public void setIds(List<Long> ids)
    {
        this.ids = ids;
    }

    public Boolean getAll()
    {
        return all;
    }

    public void setAll(Boolean all)
    {
        this.all = all;
    }
}
