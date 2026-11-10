package com.ruoyi.common.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.StringUtils;

/**
 * 本地磁盘存储实现：key -> ${ruoyi.profile}/{key}。
 *
 * <p>继续作为：本地模式写目标、历史文件读取源、迁移源、临时文件目录。</p>
 *
 * @author ruoyi-oa
 */
public class LocalFileStorage implements FileStorage
{
    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    @Override
    public StoredFile store(InputStream in, String originalName, String bizDir)
    {
        String key = StorageKeyUtils.buildKey(bizDir, originalName);
        File target = resolve(key);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists())
        {
            throw new StorageException("本地存储目录创建失败：" + key);
        }
        long size = 0L;
        try (OutputStream out = new FileOutputStream(target))
        {
            size = IOUtils.copyLarge(in, out);
        }
        catch (Exception e)
        {
            log.error("本地文件写入失败 key={}", key, e);
            throw new StorageException("本地文件写入失败，请稍后再试", e);
        }
        return new StoredFile(key, size, StorageKeyUtils.contentTypeOf(key), StorageKeyUtils.extensionOf(key));
    }

    @Override
    public void put(String key, InputStream in, long size)
    {
        if (!StorageKeyUtils.isSafeKey(key))
        {
            throw new StorageException("非法的存储 key");
        }
        File target = resolve(key);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists())
        {
            throw new StorageException("本地存储目录创建失败：" + key);
        }
        try (OutputStream out = new FileOutputStream(target))
        {
            long copied = IOUtils.copyLarge(in, out);
            if (size >= 0 && copied != size)
            {
                log.warn("本地文件写入字节数与预期不一致 key={} expected={} actual={}", key, size, copied);
            }
        }
        catch (Exception e)
        {
            log.error("本地文件写入失败 key={}", key, e);
            throw new StorageException("本地文件写入失败，请稍后再试", e);
        }
    }

    @Override
    public StorageObject open(String key)
    {
        if (StringUtils.isEmpty(key))
        {
            return null;
        }
        File file = resolve(key);
        if (!file.isFile())
        {
            return null;
        }
        try
        {
            return new StorageObject(new FileInputStream(file), file.length(), StorageKeyUtils.contentTypeOf(key));
        }
        catch (Exception e)
        {
            log.error("本地文件读取失败 key={}", key, e);
            throw new StorageException("本地文件读取失败", e);
        }
    }

    @Override
    public boolean exists(String key)
    {
        return StringUtils.isNotEmpty(key) && resolve(key).isFile();
    }

    @Override
    public void delete(String key)
    {
        if (StringUtils.isEmpty(key))
        {
            return;
        }
        try
        {
            Files.deleteIfExists(resolve(key).toPath());
        }
        catch (Exception e)
        {
            log.warn("本地文件删除失败 key={}", key, e);
        }
    }

    @Override
    public String signedUrl(String key, long expireSeconds)
    {
        // 本地存储无签名 URL 能力
        return null;
    }

    @Override
    public String type()
    {
        return "local";
    }

    /**
     * key -> 物理文件，并做目录逃逸校验（防御性，key 生成处已校验）。
     */
    private File resolve(String key)
    {
        String profile = RuoYiConfig.getProfile();
        if (StringUtils.isEmpty(profile))
        {
            throw new StorageException("本地存储根目录 ruoyi.profile 未配置");
        }
        Path root = Paths.get(profile).toAbsolutePath().normalize();
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root))
        {
            throw new StorageException("非法存储路径");
        }
        return target.toFile();
    }
}
