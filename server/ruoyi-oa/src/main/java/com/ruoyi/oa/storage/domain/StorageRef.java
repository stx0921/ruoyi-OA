package com.ruoyi.oa.storage.domain;

/**
 * 迁移扫描的轻量记录（oa_file / sys_user 的 id + 路径引用）。
 *
 * @author ruoyi-oa
 */
public class StorageRef
{
    /** oa_file.file_id 或 sys_user.user_id */
    private Long id;

    /** oa_file.file_path 或 sys_user.avatar */
    private String path;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }
}
