package com.ruoyi.common.storage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.utils.StringUtils;

/**
 * 存储路由器：统一路径解析（兼容旧 /profile 路径、新 OSS 裸 key、/common/attach 代理、http 外链）。
 *
 * <p>路径规则（必须严格保持）：</p>
 * <ul>
 *   <li>{@code /profile/...} → 本地存储（历史文件，静态映射保留）</li>
 *   <li>{@code /common/attach/...} → OSS（头像/编辑器图片匿名代理）</li>
 *   <li>{@code http(s)://...} → 直接读取外链</li>
 *   <li>其余（裸 key）→ OSS，读取失败且 read-fallback=true 时回退本地</li>
 * </ul>
 *
 * @author ruoyi-oa
 */
public class FileStorageRouter
{
    private static final Logger log = LoggerFactory.getLogger(FileStorageRouter.class);

    /** 历史文件前缀 */
    private static final String PROFILE_PREFIX = "/profile/";

    /** 公共代理前缀 */
    public static final String ATTACH_PREFIX = "/common/attach/";

    private final StorageProperties properties;

    private final LocalFileStorage localFileStorage;

    private final OssFileStorage ossFileStorage;

    public FileStorageRouter(StorageProperties properties, LocalFileStorage localFileStorage,
            OssFileStorage ossFileStorage)
    {
        this.properties = properties;
        this.localFileStorage = localFileStorage;
        this.ossFileStorage = ossFileStorage;
    }

    /**
     * 当前写模式使用的存储：type=oss → OSS；其余 → 本地。
     */
    public FileStorage targetStorage()
    {
        if (properties.isOssMode())
        {
            if (!ossFileStorage.isReady())
            {
                throw new StorageException("OSS 存储未就绪，请检查 ruoyi.storage.oss 配置");
            }
            return ossFileStorage;
        }
        return localFileStorage;
    }

    /**
     * 上传（调用方已知文件大小，OSS 用于设置 Content-Length，避免大文件进内存）。
     */
    public StoredFile store(InputStream in, long size, String originalName, String bizDir)
    {
        return targetStorage().store(in, size, originalName, bizDir);
    }

    /**
     * 上传（大小未知时使用；本地实现不受影响）。
     */
    public StoredFile store(InputStream in, String originalName, String bizDir)
    {
        return targetStorage().store(in, originalName, bizDir);
    }

    /**
     * 写库用：本地模式返回 {@code /profile/xxx}；OSS 模式返回裸 key（业务附件用）。
     */
    public String storeRef(String key)
    {
        if (StringUtils.isEmpty(key))
        {
            return key;
        }
        return properties.isOssMode() ? key : PROFILE_PREFIX + key;
    }

    /**
     * 写库用：需要浏览器直接展示的 URL（头像/编辑器图片）；OSS 模式返回 {@code /common/attach/xxx}。
     */
    public String publicRef(String key)
    {
        if (StringUtils.isEmpty(key))
        {
            return key;
        }
        return properties.isOssMode() ? ATTACH_PREFIX + key : PROFILE_PREFIX + key;
    }

    /**
     * 读取任意历史/新路径。
     *
     * @return 对象；不存在返回 null
     * @throws StorageException 服务不可用或路径非法
     */
    public StorageObject openByPath(String path)
    {
        if (StringUtils.isEmpty(path))
        {
            return null;
        }
        String value = path.trim();
        if (value.startsWith(PROFILE_PREFIX))
        {
            return localFileStorage.open(requireSafeKey(value.substring(PROFILE_PREFIX.length())));
        }
        if (value.startsWith(ATTACH_PREFIX))
        {
            return openAttach(value.substring(ATTACH_PREFIX.length()));
        }
        if (StorageKeyUtils.isHttpUrl(value))
        {
            return openHttp(value);
        }
        // 未知的绝对路径（不含 /profile/ 前缀）不再猜测，按丢失处理
        if (value.startsWith("/"))
        {
            log.warn("无法识别的历史文件路径 path={}", value);
            return null;
        }
        // 裸 key → OSS；读取失败按 read-fallback 回退本地
        String key = requireSafeKey(value);
        StorageObject object = openOssQuietly(key);
        if (object == null && properties.isReadFallback())
        {
            object = localFileStorage.open(key);
        }
        return object;
    }

    /**
     * 对象是否存在（迁移/对账用）。
     */
    public boolean existsByPath(String path)
    {
        if (StringUtils.isEmpty(path))
        {
            return false;
        }
        String value = path.trim();
        if (value.startsWith(PROFILE_PREFIX))
        {
            return localFileStorage.exists(value.substring(PROFILE_PREFIX.length()));
        }
        if (value.startsWith(ATTACH_PREFIX))
        {
            String key = value.substring(ATTACH_PREFIX.length());
            if (ossFileStorage.isReady() && ossFileStorage.exists(requireSafeKey(key)))
            {
                return true;
            }
            return properties.isReadFallback() && localFileStorage.exists(requireSafeKey(key));
        }
        if (StorageKeyUtils.isHttpUrl(value))
        {
            return true;
        }
        return localFileStorage.exists(value) || (ossFileStorage.isReady() && ossFileStorage.exists(requireSafeKey(value)));
    }

    /**
     * 按逻辑路径删除（旧头像替换、迁移回滚等）；失败只记录日志，不影响主流程。
     */
    public void deleteByPath(String path)
    {
        if (StringUtils.isEmpty(path))
        {
            return;
        }
        String value = path.trim();
        try
        {
            if (value.startsWith(PROFILE_PREFIX))
            {
                localFileStorage.delete(value.substring(PROFILE_PREFIX.length()));
                return;
            }
            if (value.startsWith(ATTACH_PREFIX))
            {
                String key = value.substring(ATTACH_PREFIX.length());
                if (ossFileStorage.isReady())
                {
                    ossFileStorage.delete(key);
                }
                if (properties.isReadFallback())
                {
                    localFileStorage.delete(key);
                }
                return;
            }
            if (StorageKeyUtils.isHttpUrl(value))
            {
                return;
            }
            if (ossFileStorage.isReady())
            {
                ossFileStorage.delete(value);
            }
            if (properties.isReadFallback())
            {
                localFileStorage.delete(value);
            }
        }
        catch (Exception e)
        {
            log.warn("删除存储对象失败 path={}", value, e);
        }
    }

    /** 当前是否 OSS 写模式 */
    public boolean isOssMode()
    {
        return properties.isOssMode();
    }

    /** OSS 客户端是否就绪 */
    public boolean isOssReady()
    {
        return ossFileStorage.isReady();
    }

    public StorageProperties getProperties()
    {
        return properties;
    }

    public LocalFileStorage getLocalFileStorage()
    {
        return localFileStorage;
    }

    public OssFileStorage getOssFileStorage()
    {
        return ossFileStorage;
    }

    /**
     * /common/attach/** 读取：匿名代理，只允许头像/编辑器图片目录的安全 key；
     * OSS 不可用时按兜底尝试本地。
     */
    private StorageObject openAttach(String key)
    {
        if (!StorageKeyUtils.isSafePublicKey(key))
        {
            throw new StorageException("非法的存储路径");
        }
        if (ossFileStorage.isReady())
        {
            StorageObject object = openOssQuietly(key);
            if (object != null)
            {
                return object;
            }
        }
        if (properties.isReadFallback())
        {
            return localFileStorage.open(key);
        }
        return null;
    }

    /**
     * 读 OSS：未就绪返回 null（由调用方决定是否本地兜底）；其它异常向上抛 StorageException。
     */
    private StorageObject openOssQuietly(String key)
    {
        if (!ossFileStorage.isReady())
        {
            return null;
        }
        return ossFileStorage.open(key);
    }

    /**
     * 读取 http(s) 外链（历史富文本图片）。
     */
    private StorageObject openHttp(String url)
    {
        HttpURLConnection connection = null;
        try
        {
            URLConnection urlConnection = new URL(url).openConnection();
            urlConnection.setConnectTimeout(properties.getOss().getConnectTimeout());
            urlConnection.setReadTimeout(properties.getOss().getSocketTimeout());
            urlConnection.setDoInput(true);
            connection = (HttpURLConnection) urlConnection;
            int status = connection.getResponseCode();
            if (status == 404)
            {
                return null;
            }
            if (status < 200 || status >= 300)
            {
                throw new StorageException("外链附件读取失败，状态码 " + status);
            }
            long size = connection.getContentLengthLong();
            String contentType = connection.getContentType();
            return new StorageObject(connection.getInputStream(), size,
                    StringUtils.isEmpty(contentType) ? StorageKeyUtils.contentTypeOf(url) : contentType);
        }
        catch (StorageException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            if (connection != null)
            {
                connection.disconnect();
            }
            log.error("外链附件读取失败 url={}", url, e);
            throw new StorageException("附件服务暂时不可用，请稍后再试", e);
        }
    }

    /**
     * key 安全校验（拒绝 ../、绝对路径、编码绕过、非法字符）。
     */
    private String requireSafeKey(String key)
    {
        if (!StorageKeyUtils.isSafeKey(key))
        {
            throw new StorageException("非法的存储路径");
        }
        return key;
    }
}
