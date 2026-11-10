package com.ruoyi.common.storage;

/**
 * 存储访问异常（对外只暴露友好提示，内部细节只写日志）。
 *
 * @author ruoyi-oa
 */
public class StorageException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /** 是否为“对象不存在”（区别于服务不可用） */
    private final boolean notFound;

    public StorageException(String message)
    {
        this(message, null, false);
    }

    public StorageException(String message, Throwable cause)
    {
        this(message, cause, false);
    }

    public StorageException(String message, Throwable cause, boolean notFound)
    {
        super(message, cause);
        this.notFound = notFound;
    }

    public boolean isNotFound()
    {
        return notFound;
    }
}
