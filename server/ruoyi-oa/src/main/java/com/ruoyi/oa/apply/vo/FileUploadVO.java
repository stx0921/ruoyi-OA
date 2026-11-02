package com.ruoyi.oa.apply.vo;

import java.io.Serializable;

/**
 * 文件上传结果 VO（前端用 fileId 绑定业务，fileUrl 为鉴权下载地址）
 *
 * @author ruoyi-oa
 */
public class FileUploadVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 文件ID（oa_file.file_id） */
    private Long fileId;

    /** 逻辑地址（/oa/file/{fileId}） */
    private String fileUrl;

    public Long getFileId()
    {
        return fileId;
    }

    public void setFileId(Long fileId)
    {
        this.fileId = fileId;
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
