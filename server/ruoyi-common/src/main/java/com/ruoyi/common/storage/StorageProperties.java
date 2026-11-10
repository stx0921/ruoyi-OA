package com.ruoyi.common.storage;

import javax.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import com.ruoyi.common.utils.StringUtils;

/**
 * 文件存储配置（ruoyi.storage.*）
 *
 * <p>凭据只允许来自环境变量（或 ECS 实例 RAM 角色），配置文件中只写占位符；
 * {@link #validate()} 在 type=oss 时做启动期强校验，缺失凭据直接启动失败，
 * 避免运行到第一次上传才报错。</p>
 *
 * @author ruoyi-oa
 */
@ConfigurationProperties(prefix = "ruoyi.storage")
public class StorageProperties
{
    /** 存储类型：local | oss，默认 local */
    private String type = "local";

    /** OSS 读取失败时是否回退本地（迁移期/回滚期建议 true） */
    private boolean readFallback = true;

    /** OSS 配置 */
    private Oss oss = new Oss();

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

    public Oss getOss()
    {
        return oss;
    }

    public void setOss(Oss oss)
    {
        this.oss = oss;
    }

    /** 是否 OSS 写模式 */
    public boolean isOssMode()
    {
        return "oss".equalsIgnoreCase(type);
    }

    /**
     * 启动期强校验：type=oss 但配置不完整时直接失败，并给出可操作的提示。
     */
    @PostConstruct
    public void validate()
    {
        if (!"local".equalsIgnoreCase(type) && !"oss".equalsIgnoreCase(type))
        {
            throw new IllegalStateException("ruoyi.storage.type 非法：" + type + "（仅支持 local / oss）");
        }
        if (!isOssMode())
        {
            return;
        }
        if (oss == null || StringUtils.isEmpty(oss.getEndpoint()))
        {
            throw new IllegalStateException("OSS 配置缺失：请设置环境变量 OSS_ENDPOINT（如 https://oss-cn-hangzhou.aliyuncs.com）");
        }
        if (StringUtils.isEmpty(oss.getBucket()))
        {
            throw new IllegalStateException("OSS 配置缺失：请设置环境变量 OSS_BUCKET（私有 Bucket 名称）");
        }
        if (oss.isUseInstanceRole())
        {
            // ECS 实例 RAM 角色：完全没有静态 AK/SK
            return;
        }
        if (StringUtils.isEmpty(oss.getAccessKeyId()) || StringUtils.isEmpty(oss.getAccessKeySecret()))
        {
            throw new IllegalStateException("OSS 凭据缺失：请设置环境变量 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET，"
                    + "或启用 ECS 实例 RAM 角色（ruoyi.storage.oss.use-instance-role=true / 环境变量 OSS_USE_INSTANCE_ROLE=true）；"
                    + "禁止在配置文件中写明文 AK/SK");
        }
    }

    /**
     * 禁止使用 Lombok 默认 toString：AK 掩码、SK 不输出，避免误打日志泄漏凭据。
     */
    @Override
    public String toString()
    {
        String maskedAk = maskAk(oss == null ? null : oss.getAccessKeyId());
        return "StorageProperties{type='" + type + "', readFallback=" + readFallback + ", oss={endpoint='"
                + (oss == null ? null : oss.getEndpoint()) + "', bucket='" + (oss == null ? null : oss.getBucket())
                + "', keyPrefix='" + (oss == null ? null : oss.getKeyPrefix()) + "', private="
                + (oss != null && oss.isPrivate()) + ", accessKeyId='" + maskedAk + "', accessKeySecret='******'}}";
    }

    private static String maskAk(String ak)
    {
        if (StringUtils.isEmpty(ak))
        {
            return "";
        }
        if (ak.length() <= 8)
        {
            return "******";
        }
        return ak.substring(0, 4) + "****" + ak.substring(ak.length() - 4);
    }

    /**
     * 阿里云 OSS 配置
     */
    public static class Oss
    {
        /** 地域节点，如 https://oss-cn-hangzhou.aliyuncs.com */
        private String endpoint;

        /** 私有 Bucket 名称 */
        private String bucket;

        /** AccessKeyId，只能来自环境变量 OSS_ACCESS_KEY_ID */
        private String accessKeyId;

        /** AccessKeySecret，只能来自环境变量 OSS_ACCESS_KEY_SECRET */
        private String accessKeySecret;

        /** 是否使用 ECS 实例 RAM 角色（true 时 AK/SK 留空） */
        private boolean useInstanceRole;

        /** 私有桶（本方案恒定 true；public 直链仅显式配置 public-domain 时使用） */
        private boolean privateBucket = true;

        /** 可选：CDN/自定义域名（私有模式留空） */
        private String publicDomain = "";

        /** 签名 URL 有效期（备用能力，不存库） */
        private long signExpireSeconds = 7200L;

        /** key 前缀（多环境共用桶时隔离，如 dev/、prod/） */
        private String keyPrefix = "";

        /** ECS RAM 角色名（use-instance-role=true 时可留空，自动从 ECS 元数据发现） */
        private String ramRoleName = "";

        /** 连接超时（毫秒） */
        private int connectTimeout = 10000;

        /** 读写超时（毫秒） */
        private int socketTimeout = 60000;

        public String getEndpoint()
        {
            return endpoint;
        }

        public void setEndpoint(String endpoint)
        {
            this.endpoint = endpoint;
        }

        public String getBucket()
        {
            return bucket;
        }

        public void setBucket(String bucket)
        {
            this.bucket = bucket;
        }

        public String getAccessKeyId()
        {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId)
        {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret()
        {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret)
        {
            this.accessKeySecret = accessKeySecret;
        }

        public boolean isUseInstanceRole()
        {
            return useInstanceRole;
        }

        public void setUseInstanceRole(boolean useInstanceRole)
        {
            this.useInstanceRole = useInstanceRole;
        }

        /**
         * 对应配置项 private: true。
         * 方法名 setPrivate 满足 Spring Boot 的 JavaBean 绑定（属性名 private）。
         */
        public boolean isPrivate()
        {
            return privateBucket;
        }

        public void setPrivate(boolean privateBucket)
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

        public String getKeyPrefix()
        {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix)
        {
            this.keyPrefix = keyPrefix;
        }

        public String getRamRoleName()
        {
            return ramRoleName;
        }

        public void setRamRoleName(String ramRoleName)
        {
            this.ramRoleName = ramRoleName;
        }

        public int getConnectTimeout()
        {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout)
        {
            this.connectTimeout = connectTimeout;
        }

        public int getSocketTimeout()
        {
            return socketTimeout;
        }

        public void setSocketTimeout(int socketTimeout)
        {
            this.socketTimeout = socketTimeout;
        }
    }
}
