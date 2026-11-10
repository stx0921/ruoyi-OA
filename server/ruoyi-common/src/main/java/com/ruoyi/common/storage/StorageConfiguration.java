package com.ruoyi.common.storage;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储抽象装配：本地实现 + OSS 实现 + 路由。
 *
 * <p>两个实现始终注册为 Bean：写路由由 ruoyi.storage.type 决定；
 * 读取裸 key 时即使切回 local 也能继续读 OSS（read-fallback 回滚期兼容）。</p>
 *
 * @author ruoyi-oa
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration
{
    @Bean
    public LocalFileStorage localFileStorage()
    {
        return new LocalFileStorage();
    }

    @Bean
    public OssFileStorage ossFileStorage(StorageProperties storageProperties)
    {
        return new OssFileStorage(storageProperties);
    }

    @Bean
    public FileStorageRouter fileStorageRouter(StorageProperties storageProperties,
            LocalFileStorage localFileStorage, OssFileStorage ossFileStorage)
    {
        return new FileStorageRouter(storageProperties, localFileStorage, ossFileStorage);
    }

    /**
     * type=oss 时启动期再确认 OSS 客户端确实可用（例如实例 RAM 角色元数据不可达时 fail-fast），
     * 避免“配置看起来齐全、应用能启动、首次上传才 503”的假成功。
     */
    @Bean
    public ApplicationRunner storageStartupCheck(StorageProperties storageProperties, OssFileStorage ossFileStorage)
    {
        return args -> {
            if (storageProperties.isOssMode() && !ossFileStorage.isReady())
            {
                throw new IllegalStateException("OSS 客户端初始化失败：请检查 OSS_ENDPOINT / OSS_BUCKET / 凭据"
                        + "（或 ECS 实例 RAM 角色）配置与网络连通性；禁止在配置文件中写明文 AK/SK");
            }
        };
    }
}
