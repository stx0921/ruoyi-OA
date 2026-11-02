package com.ruoyi.oa.apply.vo;

import java.io.Serializable;
import java.util.List;

/**
 * H5 提交申请结果 VO
 *
 * @author ruoyi-oa
 */
public class SubmitResultVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 生成的申请ID列表 */
    private List<Long> applyIds;

    /** 生成的申请编号列表 */
    private List<String> applyNos;

    /** 进度查询提示语 */
    private String progressTip;

    public List<Long> getApplyIds()
    {
        return applyIds;
    }

    public void setApplyIds(List<Long> applyIds)
    {
        this.applyIds = applyIds;
    }

    public List<String> getApplyNos()
    {
        return applyNos;
    }

    public void setApplyNos(List<String> applyNos)
    {
        this.applyNos = applyNos;
    }

    public String getProgressTip()
    {
        return progressTip;
    }

    public void setProgressTip(String progressTip)
    {
        this.progressTip = progressTip;
    }

}
