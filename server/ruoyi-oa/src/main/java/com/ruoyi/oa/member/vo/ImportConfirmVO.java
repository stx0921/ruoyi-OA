package com.ruoyi.oa.member.vo;

import java.io.Serializable;

/**
 * 导入确认结果 VO（confirm 返回）
 *
 * @author ruoyi-oa
 */
public class ImportConfirmVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 批次ID */
    private Long batchId;

    /** 批次号 */
    private String batchNo;

    /** 数据行数 */
    private Integer totalRows;

    /** 新增成功行数 */
    private Integer successRows;

    /** 更新行数 */
    private Integer updateRows;

    /** 跳过行数 */
    private Integer skipRows;

    /** 失败行数 */
    private Integer failRows;

    /** 错误明细文件地址（无错误为空串） */
    private String errorFileUrl;

    public Long getBatchId()
    {
        return batchId;
    }

    public void setBatchId(Long batchId)
    {
        this.batchId = batchId;
    }

    public String getBatchNo()
    {
        return batchNo;
    }

    public void setBatchNo(String batchNo)
    {
        this.batchNo = batchNo;
    }

    public Integer getTotalRows()
    {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows)
    {
        this.totalRows = totalRows;
    }

    public Integer getSuccessRows()
    {
        return successRows;
    }

    public void setSuccessRows(Integer successRows)
    {
        this.successRows = successRows;
    }

    public Integer getUpdateRows()
    {
        return updateRows;
    }

    public void setUpdateRows(Integer updateRows)
    {
        this.updateRows = updateRows;
    }

    public Integer getSkipRows()
    {
        return skipRows;
    }

    public void setSkipRows(Integer skipRows)
    {
        this.skipRows = skipRows;
    }

    public Integer getFailRows()
    {
        return failRows;
    }

    public void setFailRows(Integer failRows)
    {
        this.failRows = failRows;
    }

    public String getErrorFileUrl()
    {
        return errorFileUrl;
    }

    public void setErrorFileUrl(String errorFileUrl)
    {
        this.errorFileUrl = errorFileUrl;
    }

}
