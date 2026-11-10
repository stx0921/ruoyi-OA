package com.ruoyi.oa.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.storage.FileStorageRouter;
import com.ruoyi.common.storage.LocalFileStorage;
import com.ruoyi.common.storage.OssFileStorage;
import com.ruoyi.common.storage.StorageException;
import com.ruoyi.common.storage.StorageObject;
import com.ruoyi.common.storage.StorageProperties;
import com.ruoyi.common.storage.StoredFile;

/**
 * 本地存储实现 + 路径路由单测（不依赖 Spring / OSS）。
 */
class LocalFileStorageTest
{
    @TempDir
    Path tempDir;

    private LocalFileStorage local;

    private StorageProperties properties;

    private FileStorageRouter router;

    @BeforeEach
    void setUp()
    {
        RuoYiConfig config = new RuoYiConfig();
        config.setProfile(tempDir.toString());
        local = new LocalFileStorage();
        properties = new StorageProperties();
        OssFileStorage oss = new OssFileStorage(properties);
        // 不调用 @PostConstruct initialize()，OSS 未就绪，用于验证本地兜底
        router = new FileStorageRouter(properties, local, oss);
    }

    @AfterEach
    void tearDown()
    {
        new RuoYiConfig().setProfile(null);
    }

    @Test
    void storeOpenExistsDelete() throws IOException
    {
        byte[] data = "hello-storage".getBytes(StandardCharsets.UTF_8);
        StoredFile stored = local.store(new ByteArrayInputStream(data), "photo.PNG", "oa/upload");
        assertTrue(stored.getKey().startsWith("oa/upload/"), stored.getKey());
        assertTrue(stored.getKey().endsWith(".png"));
        assertEquals(data.length, stored.getSize());
        assertEquals("image/png", stored.getContentType());

        File physical = new File(tempDir.toFile(), stored.getKey());
        assertTrue(physical.isFile());
        assertEquals(data.length, physical.length());

        assertTrue(local.exists(stored.getKey()));
        try (StorageObject object = local.open(stored.getKey()))
        {
            assertNotNull(object);
            assertEquals(data.length, object.getSize());
            assertEquals("image/png", object.getContentType());
            assertEquals("hello-storage", new String(readAll(object.getInputStream()), StandardCharsets.UTF_8));
        }

        local.delete(stored.getKey());
        assertFalse(local.exists(stored.getKey()));
        assertNull(local.open(stored.getKey()));
    }

    @Test
    void putAtExactKeyUsedByMigration()
    {
        byte[] data = "migrated".getBytes(StandardCharsets.UTF_8);
        local.put("oa/upload/2026/11/10/fixed.jpg", new ByteArrayInputStream(data), data.length);
        assertTrue(local.exists("oa/upload/2026/11/10/fixed.jpg"));
        assertEquals("local", local.type());
        assertNull(local.signedUrl("oa/upload/2026/11/10/fixed.jpg", 60));
    }

    @Test
    void putRejectsTraversalKey()
    {
        assertThrows(StorageException.class,
                () -> local.put("../evil.jpg", new ByteArrayInputStream(new byte[] { 1 }), 1));
    }

    @Test
    void routerLocalModeRefsAndRead()
    {
        assertEquals("/profile/oa/upload/a.png", router.storeRef("oa/upload/a.png"));
        assertEquals("/profile/avatar/a.png", router.publicRef("avatar/a.png"));

        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        local.put("oa/upload/2026/11/10/a.png", new ByteArrayInputStream(data), data.length);
        try (StorageObject object = router.openByPath("/profile/oa/upload/2026/11/10/a.png"))
        {
            assertNotNull(object);
            assertEquals(3L, object.getSize());
        }
        // 裸 key（OSS 未就绪）→ read-fallback 回退本地
        try (StorageObject fallback = router.openByPath("oa/upload/2026/11/10/a.png"))
        {
            assertNotNull(fallback);
        }
        // 未知路径 / 非法 key 按丢失或异常处理
        assertNull(router.openByPath("/unknown/a.png"));
        assertThrows(StorageException.class, () -> router.openByPath("../etc/passwd"));
        assertTrue(router.existsByPath("/profile/oa/upload/2026/11/10/a.png"));
    }

    @Test
    void routerOssModeRefsDoNotWrite()
    {
        properties.setType("oss");
        assertEquals("oa/upload/a.png", router.storeRef("oa/upload/a.png"));
        assertEquals("/common/attach/avatar/a.png", router.publicRef("avatar/a.png"));
        assertTrue(router.isOssMode());
        assertFalse(router.isOssReady());
        // OSS 未就绪时写模式目标必须明确报错
        assertThrows(StorageException.class,
                () -> router.store(new ByteArrayInputStream(new byte[] { 1 }), 1, "a.png", "upload"));
    }

    private byte[] readAll(InputStream in) throws IOException
    {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0)
        {
            out.write(buffer, 0, length);
        }
        return out.toByteArray();
    }
}
