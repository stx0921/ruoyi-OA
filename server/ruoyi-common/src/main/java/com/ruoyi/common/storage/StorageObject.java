package com.ruoyi.common.storage;

import java.io.Closeable;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

/**
 * 读取体：输入流 + 大小 + Content-Type，必须 close（释放文件句柄 / OSS 连接）。
 *
 * @author ruoyi-oa
 */
public class StorageObject implements Closeable
{
    private final InputStream inputStream;

    /** 字节数，未知时为 -1 */
    private final long size;

    private final String contentType;

    public StorageObject(InputStream inputStream, long size, String contentType)
    {
        this.inputStream = inputStream;
        this.size = size;
        this.contentType = contentType;
    }

    public InputStream getInputStream()
    {
        return inputStream;
    }

    public long getSize()
    {
        return size;
    }

    public String getContentType()
    {
        return contentType;
    }

    @Override
    public void close()
    {
        IOUtils.closeQuietly(inputStream);
    }
}
