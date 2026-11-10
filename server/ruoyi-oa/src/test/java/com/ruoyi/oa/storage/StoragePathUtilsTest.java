package com.ruoyi.oa.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.ruoyi.oa.storage.support.StoragePathUtils;

/**
 * 迁移路径规则单测（与 FileStorageRouter 的路径规则保持一致）。
 */
class StoragePathUtilsTest
{
    @Test
    void profileRefs()
    {
        assertTrue(StoragePathUtils.isProfileRef("/profile/oa/upload/2026/11/10/a.jpg"));
        assertFalse(StoragePathUtils.isProfileRef("/profile/"));
        assertFalse(StoragePathUtils.isProfileRef(null));
        assertEquals("oa/upload/2026/11/10/a.jpg",
                StoragePathUtils.localKeyOf("/profile/oa/upload/2026/11/10/a.jpg"));
        assertNull(StoragePathUtils.localKeyOf("oa/upload/a.jpg"));
        assertEquals("/profile/avatar/a.png", StoragePathUtils.toProfileRef("avatar/a.png"));
    }

    @Test
    void attachRefs()
    {
        assertTrue(StoragePathUtils.isAttachRef("/common/attach/avatar/2026/11/10/a.png"));
        assertEquals("avatar/2026/11/10/a.png",
                StoragePathUtils.attachKeyOf("/common/attach/avatar/2026/11/10/a.png"));
        assertNull(StoragePathUtils.attachKeyOf("avatar/a.png"));
        assertEquals("/common/attach/avatar/a.png", StoragePathUtils.toAttachRef("avatar/a.png"));
    }

    @Test
    void ossKeyRefs()
    {
        assertTrue(StoragePathUtils.isOssKeyRef("oa/upload/2026/11/10/a.jpg"));
        assertTrue(StoragePathUtils.isOssKeyRef("avatar/2026/11/10/a.png"));
        assertFalse(StoragePathUtils.isOssKeyRef("/profile/oa/upload/a.jpg"));
        assertFalse(StoragePathUtils.isOssKeyRef("/common/attach/avatar/a.png"));
        assertFalse(StoragePathUtils.isOssKeyRef("https://example.com/a.jpg"));
        assertFalse(StoragePathUtils.isOssKeyRef("http://example.com/a.jpg"));
        assertFalse(StoragePathUtils.isOssKeyRef(null));
        assertFalse(StoragePathUtils.isOssKeyRef(""));
    }

    @Test
    void httpRefs()
    {
        assertTrue(StoragePathUtils.isHttpRef("https://example.com/a.jpg"));
        assertFalse(StoragePathUtils.isHttpRef("/profile/a.jpg"));
        assertFalse(StoragePathUtils.isHttpRef(null));
    }
}
