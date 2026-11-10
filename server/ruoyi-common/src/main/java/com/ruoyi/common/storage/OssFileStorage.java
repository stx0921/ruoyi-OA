package com.ruoyi.common.storage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.InstanceProfileCredentialsProvider;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import com.ruoyi.common.utils.StringUtils;

/**
 * 阿里云 OSS 存储实现（私有桶）。
 *
 * <p>OSSClient 全局单例（线程安全），{@link #initialize()} 在 OSS 配置完整时构建，
 * {@link #shutdown()} 关闭；对象流式上传/下载，不把大文件读进内存。</p>
 *
 * <p>凭据：静态 AK/SK（仅来自环境变量）或 ECS 实例 RAM 角色，二选一；日志中不输出凭据。</p>
 *
 * @author ruoyi-oa
 */
public class OssFileStorage implements FileStorage
{
    private static final Logger log = LoggerFactory.getLogger(OssFileStorage.class);

    /** ECS 实例元数据中 RAM 角色列表地址（用于自动发现角色名） */
    private static final String ECS_RAM_ROLE_URL = "http://100.100.100.200/latest/meta-data/ram/security-credentials/";

    private final StorageProperties properties;

    private volatile OSS client;

    private volatile boolean ready;

    public OssFileStorage(StorageProperties properties)
    {
        this.properties = properties;
    }

    /**
     * 构建 OSS 客户端：只在配置完整时初始化，避免 type=local 的本地开发/构建被 OSS 依赖阻塞。
     */
    @PostConstruct
    public void initialize()
    {
        StorageProperties.Oss oss = properties.getOss();
        if (oss == null || StringUtils.isEmpty(oss.getEndpoint()) || StringUtils.isEmpty(oss.getBucket()))
        {
            log.info("OSS 未启用：endpoint/bucket 未配置（当前存储模式 {}）", properties.getType());
            return;
        }
        boolean staticCredentials = StringUtils.isNotEmpty(oss.getAccessKeyId())
                && StringUtils.isNotEmpty(oss.getAccessKeySecret());
        if (!staticCredentials && !oss.isUseInstanceRole())
        {
            log.info("OSS 未启用：未配置 AK/SK 且未启用 ECS 实例 RAM 角色（当前存储模式 {}）", properties.getType());
            return;
        }
        try
        {
            CredentialsProvider credentialsProvider;
            if (oss.isUseInstanceRole())
            {
                String roleName = resolveRamRoleName(oss.getRamRoleName());
                credentialsProvider = new InstanceProfileCredentialsProvider(roleName);
                log.info("OSS 使用 ECS 实例 RAM 角色凭据 role={}", roleName);
            }
            else
            {
                credentialsProvider = new DefaultCredentialProvider(oss.getAccessKeyId(), oss.getAccessKeySecret());
            }
            ClientBuilderConfiguration config = new ClientBuilderConfiguration();
            config.setConnectionTimeout(oss.getConnectTimeout());
            config.setSocketTimeout(oss.getSocketTimeout());
            config.setMaxConnections(64);
            this.client = new OSSClientBuilder().build(oss.getEndpoint(), credentialsProvider, config);
            this.ready = true;
            log.info("OSS 客户端初始化成功 endpoint={} bucket={} keyPrefix={} private={}", oss.getEndpoint(),
                    oss.getBucket(), oss.getKeyPrefix(), oss.isPrivate());
        }
        catch (Exception e)
        {
            // 初始化失败不打印凭据；type=oss 时由 StorageProperties.validate() 已在启动期拦截主要配置缺失
            log.error("OSS 客户端初始化失败（endpoint={} bucket={}）", oss.getEndpoint(), oss.getBucket(), e);
            this.ready = false;
        }
    }

    @PreDestroy
    public void shutdown()
    {
        if (client != null)
        {
            try
            {
                client.shutdown();
            }
            catch (Exception e)
            {
                log.warn("OSS 客户端关闭失败", e);
            }
            client = null;
            ready = false;
        }
    }

    /**
     * OSS 客户端是否可用（配置完整且初始化成功）。
     */
    public boolean isReady()
    {
        return ready && client != null;
    }

    @Override
    public StoredFile store(InputStream in, String originalName, String bizDir)
    {
        return store(in, -1L, originalName, bizDir);
    }

    @Override
    public StoredFile store(InputStream in, long size, String originalName, String bizDir)
    {
        if (!isReady())
        {
            throw new StorageException("OSS 存储未就绪，请检查 ruoyi.storage.oss 配置");
        }
        String key = StorageKeyUtils.buildKey(bizDir, originalName);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(StorageKeyUtils.contentTypeOf(key));
        if (size >= 0)
        {
            metadata.setContentLength(size);
        }
        try
        {
            PutObjectResult result = client.putObject(properties.getOss().getBucket(), fullKey(key), in, metadata);
            log.info("OSS 上传成功 key={} size={} etag={}", fullKey(key), size,
                    result == null ? "" : result.getETag());
        }
        catch (Exception e)
        {
            log.error("OSS 上传失败 key={} size={} error={}", fullKey(key), size, errorCode(e));
            throw new StorageException("附件上传失败，请稍后再试", e);
        }
        return new StoredFile(key, size, metadata.getContentType(), StorageKeyUtils.extensionOf(key));
    }

    @Override
    public void put(String key, InputStream in, long size)
    {
        if (!isReady())
        {
            throw new StorageException("OSS 存储未就绪，请检查 ruoyi.storage.oss 配置");
        }
        if (!StorageKeyUtils.isSafeKey(key))
        {
            throw new StorageException("非法的存储 key");
        }
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(StorageKeyUtils.contentTypeOf(key));
        if (size >= 0)
        {
            metadata.setContentLength(size);
        }
        try
        {
            client.putObject(properties.getOss().getBucket(), fullKey(key), in, metadata);
            log.info("OSS 写入成功 key={} size={}", fullKey(key), size);
        }
        catch (Exception e)
        {
            log.error("OSS 写入失败 key={} size={} error={}", fullKey(key), size, errorCode(e));
            throw new StorageException("附件上传失败，请稍后再试", e);
        }
    }

    @Override
    public StorageObject open(String key)
    {
        if (StringUtils.isEmpty(key))
        {
            return null;
        }
        if (!isReady())
        {
            throw new StorageException("OSS 存储未就绪，请检查 ruoyi.storage.oss 配置");
        }
        try
        {
            com.aliyun.oss.model.OSSObject object = client.getObject(properties.getOss().getBucket(), fullKey(key));
            if (object == null)
            {
                return null;
            }
            ObjectMetadata metadata = object.getObjectMetadata();
            long size = metadata == null ? -1L : metadata.getContentLength();
            String contentType = metadata == null ? null : metadata.getContentType();
            if (StringUtils.isEmpty(contentType))
            {
                contentType = StorageKeyUtils.contentTypeOf(key);
            }
            return new StorageObject(object.getObjectContent(), size, contentType);
        }
        catch (OSSException e)
        {
            if (isNoSuchKey(e))
            {
                return null;
            }
            log.error("OSS 读取失败 key={} error={}", fullKey(key), errorCode(e));
            throw new StorageException("附件服务暂时不可用，请稍后再试", e);
        }
        catch (Exception e)
        {
            log.error("OSS 读取异常 key={}", fullKey(key), e);
            throw new StorageException("附件服务暂时不可用，请稍后再试", e);
        }
    }

    @Override
    public boolean exists(String key)
    {
        if (StringUtils.isEmpty(key) || !isReady())
        {
            return false;
        }
        try
        {
            return client.doesObjectExist(properties.getOss().getBucket(), fullKey(key));
        }
        catch (Exception e)
        {
            log.warn("OSS 存在性检查失败 key={} error={}", fullKey(key), errorCode(e));
            throw new StorageException("附件服务暂时不可用，请稍后再试", e);
        }
    }

    @Override
    public void delete(String key)
    {
        if (StringUtils.isEmpty(key) || !isReady())
        {
            return;
        }
        try
        {
            client.deleteObject(properties.getOss().getBucket(), fullKey(key));
        }
        catch (Exception e)
        {
            log.warn("OSS 删除失败 key={} error={}", fullKey(key), errorCode(e));
        }
    }

    @Override
    public String signedUrl(String key, long expireSeconds)
    {
        if (StringUtils.isEmpty(key) || !isReady())
        {
            return null;
        }
        long expire = expireSeconds > 0 ? expireSeconds : properties.getOss().getSignExpireSeconds();
        Date expiration = new Date(System.currentTimeMillis() + expire * 1000L);
        return client.generatePresignedUrl(properties.getOss().getBucket(), fullKey(key), expiration).toString();
    }

    @Override
    public String type()
    {
        return "oss";
    }

    /**
     * 加 key-prefix（多环境共用桶隔离）。
     */
    private String fullKey(String key)
    {
        String prefix = properties.getOss().getKeyPrefix();
        if (StringUtils.isEmpty(prefix))
        {
            return key;
        }
        prefix = prefix.replace('\\', '/');
        while (prefix.startsWith("/"))
        {
            prefix = prefix.substring(1);
        }
        if (!prefix.isEmpty() && !prefix.endsWith("/"))
        {
            prefix = prefix + "/";
        }
        return prefix + key;
    }

    /**
     * 解析 RAM 角色名：优先配置项，其次从 ECS 元数据自动发现。
     */
    private String resolveRamRoleName(String configuredRoleName)
    {
        if (StringUtils.isNotEmpty(configuredRoleName))
        {
            return configuredRoleName.trim();
        }
        HttpURLConnection connection = null;
        try
        {
            URL url = new URL(ECS_RAM_ROLE_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() != 200)
            {
                throw new IllegalStateException("ECS 元数据返回状态码 " + connection.getResponseCode());
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)))
            {
                String roleName = reader.readLine();
                if (StringUtils.isEmpty(roleName))
                {
                    throw new IllegalStateException("ECS 元数据未返回 RAM 角色名");
                }
                return roleName.trim();
            }
        }
        catch (Exception e)
        {
            throw new StorageException("无法解析 ECS 实例 RAM 角色名，请显式配置 ruoyi.storage.oss.ram-role-name", e);
        }
        finally
        {
            if (connection != null)
            {
                connection.disconnect();
            }
        }
    }

    private boolean isNoSuchKey(OSSException e)
    {
        String code = e.getErrorCode();
        return "NoSuchKey".equals(code) || "NoSuchBucket".equals(code);
    }

    private String errorCode(Exception e)
    {
        if (e instanceof OSSException)
        {
            OSSException ossException = (OSSException) e;
            return ossException.getErrorCode() + "/" + ossException.getRequestId();
        }
        return e.getClass().getSimpleName();
    }
}
