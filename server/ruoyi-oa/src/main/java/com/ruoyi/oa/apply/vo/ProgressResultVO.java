package com.ruoyi.oa.apply.vo;

import java.io.Serializable;
import java.util.List;

/**
 * H5 报名进度查询结果 VO
 *
 * @author ruoyi-oa
 */
public class ProgressResultVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 进度查询令牌（30 分钟，绑定手机号，用于撤回/重提） */
    private String queryToken;

    /** 该手机号关联的申请列表（按提交时间倒序） */
    private List<ProgressApplyVO> applyList;

    public String getQueryToken()
    {
        return queryToken;
    }

    public void setQueryToken(String queryToken)
    {
        this.queryToken = queryToken;
    }

    public List<ProgressApplyVO> getApplyList()
    {
        return applyList;
    }

    public void setApplyList(List<ProgressApplyVO> applyList)
    {
        this.applyList = applyList;
    }

}
