package com.ruoyi.common.storage;

/**
 * 上传结果：存储 key、大小、Content-Type、扩展名。
 *
 * <p>key 为相对 key（如 {@code oa/upload/2026/11/10/uuid.jpg}），
 * 是否加 {@code /profile/} 前缀由 {@link FileStorageRouter#storeRef(String)} 决定。</p>
 *
 * @author ruoyi-oa
 */
public class StoredFile
{
    private final String key;

    private final long size;

    private final String contentType;

    private final String extension;

    public StoredFile(String key, long size, String contentType, String extension)
    {
        this.key = key;
        this.size = size;
        this.contentType = contentType;
        this.extension = extension;
    }

    public String getKey()
    {
        return key;
    }

    public long getSize()
    {
        return size;
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getExtension()
    {
        return extension;
    }

    @Override
    public String toString()
    {
        return "StoredFile{key='" + key + "', size=" + size + ", contentType='" + contentType + "'}";
    }
}
