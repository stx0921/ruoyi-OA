package com.ruoyi.oa.apply.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 附件对象 oa_file（下载走鉴权接口，不暴露物理路径）
 *
 * @author ruoyi-oa
 */
public class OaFile implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long fileId;

    /** 存储文件名 */
    private String fileName;

    /** 原始文件名 */
    private String originalName;

    /** 物理相对路径（/profile/upload/...） */
    private String filePath;

    /** 业务类型（member_photo/member_card/import_error） */
    private String bizType;

    /** 业务主键（成员ID/批次ID） */
    private Long bizId;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 上传人（H5匿名上传为NULL） */
    private Long uploaderId;

    /** 上传时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getFileId()
    {
        return fileId;
    }

    public void setFileId(Long fileId)
    {
        this.fileId = fileId;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getOriginalName()
    {
        return originalName;
    }

    public void setOriginalName(String originalName)
    {
        this.originalName = originalName;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public Long getBizId()
    {
        return bizId;
    }

    public void setBizId(Long bizId)
    {
        this.bizId = bizId;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public Long getUploaderId()
    {
        return uploaderId;
    }

    public void setUploaderId(Long uploaderId)
    {
        this.uploaderId = uploaderId;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

}
