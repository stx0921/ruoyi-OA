package com.ruoyi.oa.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.storage.StorageProperties;

/**
 * 存储配置启动期强校验（fail-fast）+ 凭据掩码单测。
 */
class StoragePropertiesTest
{
    @Test
    void localModeNeedsNoCredentials()
    {
        StorageProperties properties = new StorageProperties();
        assertDoesNotThrow(properties::validate);
        assertFalse(properties.isOssMode());
    }

    @Test
    void invalidTypeFailsFast()
    {
        StorageProperties properties = new StorageProperties();
        properties.setType("osss");
        IllegalStateException error = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(error.getMessage().contains("ruoyi.storage.type"), error.getMessage());
    }

    @Test
    void ossModeFailsFastWithoutEndpointAndBucket()
    {
        StorageProperties properties = new StorageProperties();
        properties.setType("oss");
        IllegalStateException error = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(error.getMessage().contains("OSS_ENDPOINT"), error.getMessage());

        properties.getOss().setEndpoint("https://oss-cn-hangzhou.aliyuncs.com");
        IllegalStateException bucketError = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(bucketError.getMessage().contains("OSS_BUCKET"), bucketError.getMessage());
    }

    @Test
    void ossModeFailsFastWithoutCredentials()
    {
        StorageProperties properties = ossWithoutCredentials();
        IllegalStateException error = assertThrows(IllegalStateException.class, properties::validate);
        // 提示必须给出环境变量名，且不包含任何真实凭据
        assertTrue(error.getMessage().contains("OSS_ACCESS_KEY_ID"), error.getMessage());
        assertTrue(error.getMessage().contains("OSS_ACCESS_KEY_SECRET"), error.getMessage());
        assertTrue(error.getMessage().contains("use-instance-role"), error.getMessage());
    }

    @Test
    void ossModeAllowsInstanceRole()
    {
        StorageProperties properties = ossWithoutCredentials();
        properties.getOss().setUseInstanceRole(true);
        assertDoesNotThrow(properties::validate);
    }

    @Test
    void ossModeAllowsStaticCredentials()
    {
        StorageProperties properties = ossWithoutCredentials();
        properties.getOss().setAccessKeyId("LTAI5tABCDEFGHIJKLMN");
        properties.getOss().setAccessKeySecret("super-secret-value");
        assertDoesNotThrow(properties::validate);
    }

    @Test
    void toStringMasksCredentials()
    {
        StorageProperties properties = ossWithoutCredentials();
        properties.getOss().setAccessKeyId("LTAI5tABCDEFGHIJKLMN");
        properties.getOss().setAccessKeySecret("super-secret-value");
        String text = properties.toString();
        assertFalse(text.contains("LTAI5tABCDEFGHIJKLMN"), text);
        assertFalse(text.contains("super-secret-value"), text);
        assertTrue(text.contains("LTAI****KLMN"), text);
        assertTrue(text.contains("******"), text);
    }

    private StorageProperties ossWithoutCredentials()
    {
        StorageProperties properties = new StorageProperties();
        properties.setType("oss");
        properties.getOss().setEndpoint("https://oss-cn-hangzhou.aliyuncs.com");
        properties.getOss().setBucket("oa-private-bucket");
        return properties;
    }
}
