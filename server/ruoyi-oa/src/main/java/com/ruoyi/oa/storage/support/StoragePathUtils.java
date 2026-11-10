package com.ruoyi.oa.storage.support;

/**
 * 存储路径规则（迁移/回滚用，纯函数便于单测）。
 *
 * <p>规则与 FileStorageRouter 保持一致：</p>
 * <ul>
 *   <li>历史文件：{@code /profile/{key}} → 本地</li>
 *   <li>公共代理：{@code /common/attach/{key}} → OSS（头像/富文本图片）</li>
 *   <li>业务附件 OSS：裸 key（不以 / 开头、非 http）</li>
 * </ul>
 *
 * @author ruoyi-oa
 */
public final class StoragePathUtils
{
    public static final String PROFILE_PREFIX = "/profile/";

    public static final String ATTACH_PREFIX = "/common/attach/";

    private StoragePathUtils()
    {
    }

    /** 是否历史本地路径（/profile/...） */
    public static boolean isProfileRef(String path)
    {
        return path != null && path.startsWith(PROFILE_PREFIX) && path.length() > PROFILE_PREFIX.length();
    }

    /** 是否公共代理路径（/common/attach/...） */
    public static boolean isAttachRef(String path)
    {
        return path != null && path.startsWith(ATTACH_PREFIX) && path.length() > ATTACH_PREFIX.length();
    }

    /** 是否 http(s) 外链 */
    public static boolean isHttpRef(String path)
    {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }

    /** 是否 OSS 业务附件裸 key（非 /profile、非 /common/attach、非 http、非绝对路径） */
    public static boolean isOssKeyRef(String path)
    {
        return path != null && !path.isEmpty() && !path.startsWith("/") && !isHttpRef(path);
    }

    /**
     * /profile/{key} → {key}；非历史路径返回 null。
     */
    public static String localKeyOf(String profileRef)
    {
        if (!isProfileRef(profileRef))
        {
            return null;
        }
        return profileRef.substring(PROFILE_PREFIX.length());
    }

    /**
     * /common/attach/{key} → {key}；非代理路径返回 null。
     */
    public static String attachKeyOf(String attachRef)
    {
        if (!isAttachRef(attachRef))
        {
            return null;
        }
        return attachRef.substring(ATTACH_PREFIX.length());
    }

    /** key → /profile/{key}（本地模式写库值） */
    public static String toProfileRef(String key)
    {
        return PROFILE_PREFIX + key;
    }

    /** key → /common/attach/{key}（OSS 模式头像/编辑器图片写库值） */
    public static String toAttachRef(String key)
    {
        return ATTACH_PREFIX + key;
    }
}
