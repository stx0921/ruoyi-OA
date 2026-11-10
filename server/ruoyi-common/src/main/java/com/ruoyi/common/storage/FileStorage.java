package com.ruoyi.common.storage;

import java.io.InputStream;

/**
 * 文件存储抽象：本地实现 / OSS 实现。
 *
 * @author ruoyi-oa
 */
public interface FileStorage
{
    /**
     * 存储文件。
     *
     * @param in 输入流（不负责关闭）
     * @param originalName 原始文件名，仅用于取安全扩展名
     * @param bizDir 业务目录（oa/upload、avatar、upload …）
     * @return 相对 key（如 oa/upload/2026/11/10/uuid.jpg）
     */
    StoredFile store(InputStream in, String originalName, String bizDir);

    /**
     * 存储文件（调用方已知准确字节数）。
     *
     * <p>默认实现忽略 size；OSS 实现用它设置 Content-Length，保证大文件流式上传且不进入内存。</p>
     */
    default StoredFile store(InputStream in, long size, String originalName, String bizDir)
    {
        return store(in, originalName, bizDir);
    }

    /**
     * 按指定 key 写入（迁移/回滚等运维场景：不生成新 key，保持路径可预测）。
     *
     * @param key 已存在的相对 key
     * @param in 输入流（不负责关闭）
     * @param size 准确字节数，未知传 -1
     */
    void put(String key, InputStream in, long size);

    /**
     * 按相对 key 读取；对象不存在返回 null。
     */
    StorageObject open(String key);

    /**
     * 对象是否存在。
     */
    boolean exists(String key);

    /**
     * 删除对象（不存在时静默）。
     */
    void delete(String key);

    /**
     * 生成带签名的临时 URL（私有桶备用能力，不存库、不下发前端业务附件）。
     */
    String signedUrl(String key, long expireSeconds);

    /**
     * 存储类型：local / oss
     */
    String type();
}
