package com.ruoyi.oa.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.storage.StorageKeyUtils;

/**
 * 存储 key / 路径 / Content-Type 规则单测（纯函数）。
 */
class StorageKeyUtilsTest
{
    @Test
    void buildKeyFollowsBizDirAndDateAndUuid()
    {
        String key = StorageKeyUtils.buildKey("oa/upload/", "简历.PDF");
        assertTrue(key.startsWith("oa/upload/"), key);
        assertTrue(key.matches("oa/upload/\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{32}\\.pdf"), key);
        assertFalse(key.contains(".."));
    }

    @Test
    void buildKeySanitizesUnsafeExtension()
    {
        assertEquals("", StorageKeyUtils.extensionOf(null));
        assertEquals("", StorageKeyUtils.extensionOf("noext"));
        assertEquals("", StorageKeyUtils.extensionOf("a.verylongextension"));
        assertEquals("jpg", StorageKeyUtils.extensionOf("../evil.JPG"));
        String key = StorageKeyUtils.buildKey("avatar", "../../evil.jpg");
        assertTrue(key.startsWith("avatar/"), key);
        assertFalse(key.contains(".."));
    }

    @Test
    void normalizeBizDirRejectsTraversal()
    {
        assertEquals("oa/upload", StorageKeyUtils.normalizeBizDir("/oa/upload/"));
        assertEquals("", StorageKeyUtils.normalizeBizDir(null));
        assertThrows(IllegalArgumentException.class, () -> StorageKeyUtils.normalizeBizDir("../etc"));
        assertThrows(IllegalArgumentException.class, () -> StorageKeyUtils.normalizeBizDir("oa/up load"));
        assertThrows(IllegalArgumentException.class, () -> StorageKeyUtils.normalizeBizDir("oa/../../etc"));
    }

    @Test
    void safeKeyRejectsTraversalAndEncodingBypass()
    {
        assertTrue(StorageKeyUtils.isSafeKey("oa/upload/2026/11/10/abc.jpg"));
        assertTrue(StorageKeyUtils.isSafeKey("avatar/2026/11/10/abc.png"));
        assertFalse(StorageKeyUtils.isSafeKey(null));
        assertFalse(StorageKeyUtils.isSafeKey(""));
        assertFalse(StorageKeyUtils.isSafeKey("/oa/upload/a.jpg"));
        assertFalse(StorageKeyUtils.isSafeKey("../etc/passwd"));
        assertFalse(StorageKeyUtils.isSafeKey("oa/../etc/passwd"));
        assertFalse(StorageKeyUtils.isSafeKey("oa/%2e%2e/etc"));
        assertFalse(StorageKeyUtils.isSafeKey("oa\\upload\\a.jpg"));
        assertFalse(StorageKeyUtils.isSafeKey("C:/windows/a.jpg"));
        assertFalse(StorageKeyUtils.isSafeKey("oa/upload/"));
        StringBuilder longKey = new StringBuilder("oa/");
        for (int i = 0; i < 200; i++)
        {
            longKey.append("abcdefgh/");
        }
        assertFalse(StorageKeyUtils.isSafeKey(longKey.toString()));
    }

    @Test
    void publicKeyOnlyAllowsImages()
    {
        assertTrue(StorageKeyUtils.isSafePublicKey("avatar/2026/11/10/a.png"));
        assertTrue(StorageKeyUtils.isSafePublicKey("upload/2026/11/10/a.jpeg"));
        assertTrue(StorageKeyUtils.isSafePublicKey("upload/2026/11/10/a.webp"));
        assertFalse(StorageKeyUtils.isSafePublicKey("upload/2026/11/10/a.svg"));
        assertFalse(StorageKeyUtils.isSafePublicKey("upload/2026/11/10/a.html"));
        assertFalse(StorageKeyUtils.isSafePublicKey("upload/2026/11/10/a.pdf"));
        assertFalse(StorageKeyUtils.isSafePublicKey("avatar/2026/11/10/noext"));
    }

    @Test
    void publicKeyRejectsBusinessAttachmentDirectories()
    {
        // 业务附件（人员照片/工单图片等）在 oa/upload/ 下，绝不能走匿名代理
        assertFalse(StorageKeyUtils.isSafePublicKey("oa/upload/2026/11/10/a.png"));
        assertFalse(StorageKeyUtils.isSafePublicKey("oa/upload/2026/11/10/a.jpg"));
        assertFalse(StorageKeyUtils.isSafePublicKey("feedback/2026/11/10/a.png"));
        assertFalse(StorageKeyUtils.isSafePublicKey("member_photo/a.png"));
        assertFalse(StorageKeyUtils.isSafePublicKey("avatarx/2026/11/10/a.png"));
    }

    @Test
    void contentTypeByExtension()
    {
        assertEquals("image/png", StorageKeyUtils.contentTypeOf("a/b/c.png"));
        assertEquals("application/pdf", StorageKeyUtils.contentTypeOf("a.pdf"));
        assertEquals("application/octet-stream", StorageKeyUtils.contentTypeOf("unknown.xxx"));
        assertEquals("application/octet-stream", StorageKeyUtils.contentTypeOf(null));
    }

    @Test
    void urlClassification()
    {
        assertTrue(StorageKeyUtils.isHttpUrl("https://example.com/a.png"));
        assertTrue(StorageKeyUtils.isHttpUrl("http://example.com/a.png"));
        assertFalse(StorageKeyUtils.isHttpUrl("/profile/a.png"));
        assertTrue(StorageKeyUtils.isBareKey("oa/upload/a.png"));
        assertFalse(StorageKeyUtils.isBareKey("/profile/a.png"));
        assertFalse(StorageKeyUtils.isBareKey("https://example.com/a.png"));
    }
}
