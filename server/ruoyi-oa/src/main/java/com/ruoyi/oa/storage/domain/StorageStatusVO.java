package com.ruoyi.oa.storage.domain;

/**
 * 存储状态（管理端「存储管理」页面顶部卡片）。
 *
 * @author ruoyi-oa
 */
public class StorageStatusVO
{
    /** local | oss */
    private String type;

    /** 读取兜底开关 */
    private boolean readFallback;

    /** OSS 客户端是否已就绪（配置完整且初始化成功） */
    private boolean ossEnabled;

    /** Bucket 名称（非敏感） */
    private String bucket;

    /** OSS Endpoint */
    private String endpoint;

    /** key 前缀 */
    private String keyPrefix;

    /** 是否私有桶 */
    private boolean privateBucket;

    /** 可选自定义域名 */
    private String publicDomain;

    /** 签名 URL 有效期（秒，备用能力） */
    private long signExpireSeconds;

    /** 是否使用 ECS 实例 RAM 角色 */
    private boolean instanceRole;

    /** 待迁移 OA 附件数（file_path like '/profile/%' 且非 import_error） */
    private long pendingOaFiles;

    /** 待迁移头像数 */
    private long pendingAvatars;

    /** 已在 OSS 的 OA 附件数（裸 key） */
    private long ossOaFiles;

    /** 已在 OSS 的头像数（/common/attach/%） */
    private long ossAvatars;

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public boolean isReadFallback()
    {
        return readFallback;
    }

    public void setReadFallback(boolean readFallback)
    {
        this.readFallback = readFallback;
    }

    public boolean isOssEnabled()
    {
        return ossEnabled;
    }

    public void setOssEnabled(boolean ossEnabled)
    {
        this.ossEnabled = ossEnabled;
    }

    public String getBucket()
    {
        return bucket;
    }

    public void setBucket(String bucket)
    {
        this.bucket = bucket;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public String getKeyPrefix()
    {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix)
    {
        this.keyPrefix = keyPrefix;
    }

    public boolean isPrivateBucket()
    {
        return privateBucket;
    }

    public void setPrivateBucket(boolean privateBucket)
    {
        this.privateBucket = privateBucket;
    }

    public String getPublicDomain()
    {
        return publicDomain;
    }

    public void setPublicDomain(String publicDomain)
    {
        this.publicDomain = publicDomain;
    }

    public long getSignExpireSeconds()
    {
        return signExpireSeconds;
    }

    public void setSignExpireSeconds(long signExpireSeconds)
    {
        this.signExpireSeconds = signExpireSeconds;
    }

    public boolean isInstanceRole()
    {
        return instanceRole;
    }

    public void setInstanceRole(boolean instanceRole)
    {
        this.instanceRole = instanceRole;
    }

    public long getPendingOaFiles()
    {
        return pendingOaFiles;
    }

    public void setPendingOaFiles(long pendingOaFiles)
    {
        this.pendingOaFiles = pendingOaFiles;
    }

    public long getPendingAvatars()
    {
        return pendingAvatars;
    }

    public void setPendingAvatars(long pendingAvatars)
    {
        this.pendingAvatars = pendingAvatars;
    }

    public long getOssOaFiles()
    {
        return ossOaFiles;
    }

    public void setOssOaFiles(long ossOaFiles)
    {
        this.ossOaFiles = ossOaFiles;
    }

    public long getOssAvatars()
    {
        return ossAvatars;
    }

    public void setOssAvatars(long ossAvatars)
    {
        this.ossAvatars = ossAvatars;
    }
}
