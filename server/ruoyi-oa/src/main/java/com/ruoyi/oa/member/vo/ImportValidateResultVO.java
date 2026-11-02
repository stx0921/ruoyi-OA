package com.ruoyi.oa.member.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 导入校验结果 VO（validate 返回）
 *
 * @author ruoyi-oa
 */
public class ImportValidateResultVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 批次ID */
    private Long batchId;

    /** 批次号 */
    private String batchNo;

    /** 数据行数 */
    private Integer totalRows;

    /** 可导入行数（新增+更新+跳过） */
    private Integer validRows;

    /** 错误行数 */
    private Integer invalidRows;

    /** 预计新增行数 */
    private Integer newRows;

    /** 预计更新行数 */
    private Integer updateRows;

    /** 预计跳过行数 */
    private Integer skipRows;

    /** 校验结论：ALL_VALID / PARTIAL / ALL_INVALID */
    private String result;

    /** 错误明细文件地址（/oa/file/{id}，无错误为空串） */
    private String errorFileUrl;

    /** 行级结果 */
    private List<ImportRowResultVO> rows;

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

    public Integer getValidRows()
    {
        return validRows;
    }

    public void setValidRows(Integer validRows)
    {
        this.validRows = validRows;
    }

    public Integer getInvalidRows()
    {
        return invalidRows;
    }

    public void setInvalidRows(Integer invalidRows)
    {
        this.invalidRows = invalidRows;
    }

    public Integer getNewRows()
    {
        return newRows;
    }

    public void setNewRows(Integer newRows)
    {
        this.newRows = newRows;
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

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    public String getErrorFileUrl()
    {
        return errorFileUrl;
    }

    public void setErrorFileUrl(String errorFileUrl)
    {
        this.errorFileUrl = errorFileUrl;
    }

    public List<ImportRowResultVO> getRows()
    {
        return rows;
    }

    public void setRows(List<ImportRowResultVO> rows)
    {
        this.rows = rows;
    }

}
