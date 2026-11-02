package com.ruoyi.oa.member.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 导入批次对象 oa_import_batch
 *
 * @author ruoyi-oa
 */
public class OaImportBatch extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long batchId;

    /** 批次号（IMP+yyyyMMddHHmmss+3位序号） */
    private String batchNo;

    /** 原始文件名 */
    private String fileName;

    /** 暂存路径（/profile/import/） */
    private String filePath;

    /** 模板版本 */
    private String templateVersion;

    /** 数据行数（不含表头） */
    private Integer totalRows;

    /** 成功行数（新增） */
    private Integer successRows;

    /** 失败行数 */
    private Integer failRows;

    /** 更新行数 */
    private Integer updateRows;

    /** 跳过行数 */
    private Integer skipRows;

    /** 状态（0待确认 1已确认 2已作废） */
    private String status;

    /** 错误明细 xlsx 逻辑地址 */
    private String errorFileUrl;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作人部门（数据权限） */
    private Long deptId;

    /** 确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date confirmTime;

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

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public String getTemplateVersion()
    {
        return templateVersion;
    }

    public void setTemplateVersion(String templateVersion)
    {
        this.templateVersion = templateVersion;
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

    public Integer getFailRows()
    {
        return failRows;
    }

    public void setFailRows(Integer failRows)
    {
        this.failRows = failRows;
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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getErrorFileUrl()
    {
        return errorFileUrl;
    }

    public void setErrorFileUrl(String errorFileUrl)
    {
        this.errorFileUrl = errorFileUrl;
    }

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Date getConfirmTime()
    {
        return confirmTime;
    }

    public void setConfirmTime(Date confirmTime)
    {
        this.confirmTime = confirmTime;
    }

}
