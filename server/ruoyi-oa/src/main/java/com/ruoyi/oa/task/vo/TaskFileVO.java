package com.ruoyi.oa.task.vo;

import java.io.Serializable;

/**
 * 任务附件 VO（下载统一走 /oa/file/{fileId}）
 *
 * @author ruoyi-oa
 */
public class TaskFileVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 文件ID */
    private Long fileId;

    /** 存储文件名 */
    private String fileName;

    /** 原始文件名 */
    private String originalName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 逻辑下载地址（/oa/file/{fileId}） */
    private String fileUrl;

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

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public String getFileUrl()
    {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl)
    {
        this.fileUrl = fileUrl;
    }

}
