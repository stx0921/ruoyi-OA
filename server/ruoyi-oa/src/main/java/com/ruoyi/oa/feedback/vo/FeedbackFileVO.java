package com.ruoyi.oa.feedback.vo;

import java.io.Serializable;

/**
 * 工单附件 VO（url 为 /profile/oa/upload/... 相对路径，前端拼 VITE_APP_BASE_API）
 *
 * @author ruoyi-oa
 */
public class FeedbackFileVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 附件ID */
    private Long fileId;

    /** 存储文件名 */
    private String fileName;

    /** 原始文件名 */
    private String originalName;

    /** 相对访问路径（/profile/oa/upload/...） */
    private String url;

    /** 文件大小（字节） */
    private Long size;

    /** 文件类型：image / word */
    private String fileType;

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

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public Long getSize()
    {
        return size;
    }

    public void setSize(Long size)
    {
        this.size = size;
    }

    public String getFileType()
    {
        return fileType;
    }

    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }

}
