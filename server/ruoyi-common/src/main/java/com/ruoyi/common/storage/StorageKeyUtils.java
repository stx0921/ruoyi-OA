package com.ruoyi.common.storage;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;

/**
 * 存储 key / 路径 / Content-Type 工具。
 *
 * <p>key 形如 {@code oa/upload/2026/11/10/32位uuid.jpg}，不含前导斜杠；
 * DB 中的引用由 {@link FileStorageRouter} 决定是否加 {@code /profile/} 或 {@code /common/attach/} 前缀。</p>
 *
 * @author ruoyi-oa
 */
public final class StorageKeyUtils
{
    /** 公共代理（/common/attach/**）只允许的图片扩展名 */
    private static final Set<String> PUBLIC_IMAGE_EXTENSIONS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp")));

    /**
     * 公共代理只允许的业务目录前缀：头像（avatar/）与富文本/编辑器图片（upload/）。
     * 业务附件目录（oa/upload/、feedback 系列等）一律不允许走匿名代理。
     */
    private static final String[] PUBLIC_KEY_DIR_PREFIXES = { "avatar/", "upload/" };

    /** 扩展名 -> Content-Type */
    private static final Map<String, String> CONTENT_TYPES = new HashMap<>();

    /** key 中每一段允许的字符（不含斜杠） */
    private static final String SEGMENT_PATTERN = "[A-Za-z0-9._-]+";

    static
    {
        CONTENT_TYPES.put("jpg", "image/jpeg");
        CONTENT_TYPES.put("jpeg", "image/jpeg");
        CONTENT_TYPES.put("png", "image/png");
        CONTENT_TYPES.put("gif", "image/gif");
        CONTENT_TYPES.put("webp", "image/webp");
        CONTENT_TYPES.put("bmp", "image/bmp");
        CONTENT_TYPES.put("pdf", "application/pdf");
        CONTENT_TYPES.put("doc", "application/msword");
        CONTENT_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        CONTENT_TYPES.put("xls", "application/vnd.ms-excel");
        CONTENT_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        CONTENT_TYPES.put("ppt", "application/vnd.ms-powerpoint");
        CONTENT_TYPES.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        CONTENT_TYPES.put("txt", "text/plain;charset=utf-8");
        CONTENT_TYPES.put("csv", "text/csv;charset=utf-8");
        CONTENT_TYPES.put("json", "application/json");
        CONTENT_TYPES.put("xml", "application/xml");
        CONTENT_TYPES.put("zip", "application/zip");
        CONTENT_TYPES.put("rar", "application/x-rar-compressed");
        CONTENT_TYPES.put("7z", "application/x-7z-compressed");
        CONTENT_TYPES.put("gz", "application/gzip");
        CONTENT_TYPES.put("mp4", "video/mp4");
        CONTENT_TYPES.put("mp3", "audio/mpeg");
    }

    private StorageKeyUtils()
    {
    }

    /**
     * 生成存储 key：{@code bizDir/yyyy/MM/dd/uuid.ext}
     *
     * @param bizDir 业务目录（oa/upload、avatar、upload …），只允许字母数字/下划线/短横线/斜杠
     * @param originalName 原始文件名，用于取扩展名（不可信，只取安全扩展名）
     */
    public static String buildKey(String bizDir, String originalName)
    {
        String dir = normalizeBizDir(bizDir);
        String extension = extensionOf(originalName);
        StringBuilder key = new StringBuilder();
        if (!dir.isEmpty())
        {
            key.append(dir).append('/');
        }
        key.append(DateUtils.datePath()).append('/').append(IdUtils.fastSimpleUUID());
        if (!extension.isEmpty())
        {
            key.append('.').append(extension);
        }
        return key.toString();
    }

    /**
     * 规范化业务目录：去首尾斜杠、反斜杠转斜杠；非法目录直接抛异常（内部调用，防御性检查）。
     */
    public static String normalizeBizDir(String bizDir)
    {
        if (StringUtils.isEmpty(bizDir))
        {
            return "";
        }
        String dir = bizDir.replace('\\', '/').trim();
        while (dir.startsWith("/"))
        {
            dir = dir.substring(1);
        }
        while (dir.endsWith("/"))
        {
            dir = dir.substring(0, dir.length() - 1);
        }
        if (dir.isEmpty())
        {
            return "";
        }
        if (dir.contains("..") || dir.contains(":") || dir.contains("%"))
        {
            throw new IllegalArgumentException("非法存储目录：" + bizDir);
        }
        for (String segment : dir.split("/"))
        {
            if (!segment.matches(SEGMENT_PATTERN))
            {
                throw new IllegalArgumentException("非法存储目录：" + bizDir);
            }
        }
        return dir;
    }

    /**
     * 取安全的扩展名（小写字母/数字，长度 1-10），非法或缺失返回空串。
     */
    public static String extensionOf(String fileName)
    {
        if (StringUtils.isEmpty(fileName))
        {
            return "";
        }
        String name = fileName;
        int query = name.indexOf('?');
        if (query >= 0)
        {
            name = name.substring(0, query);
        }
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1)
        {
            return "";
        }
        String extension = name.substring(index + 1).toLowerCase(Locale.ROOT);
        if (!extension.matches("[a-z0-9]{1,10}"))
        {
            return "";
        }
        return extension;
    }

    /**
     * 扩展名 -> Content-Type，未知按 application/octet-stream。
     */
    public static String contentTypeOf(String fileNameOrKey)
    {
        String contentType = CONTENT_TYPES.get(extensionOf(fileNameOrKey));
        return contentType == null ? "application/octet-stream" : contentType;
    }

    /**
     * 校验 key 是否安全：非空、不超过 512 字符、无前导斜杠、无 {@code ..}、无反斜杠/百分号/冒号，
     * 每一段只允许字母数字及 {@code . _ -}。
     */
    public static boolean isSafeKey(String key)
    {
        if (StringUtils.isEmpty(key) || key.length() > 512)
        {
            return false;
        }
        if (key.startsWith("/") || key.endsWith("/") || key.contains("..") || key.contains("\\")
                || key.contains("%") || key.contains(":") || key.contains("?") || key.contains("#"))
        {
            return false;
        }
        for (String segment : key.split("/"))
        {
            if (!segment.matches(SEGMENT_PATTERN))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 公共代理 key 校验：安全 key + 图片扩展名 + 目录白名单（avatar/、upload/）。
     * 业务附件（oa/upload/ 等）即使图片扩展名也不允许匿名代理读取。
     */
    public static boolean isSafePublicKey(String key)
    {
        if (!isSafeKey(key) || !PUBLIC_IMAGE_EXTENSIONS.contains(extensionOf(key)))
        {
            return false;
        }
        for (String prefix : PUBLIC_KEY_DIR_PREFIXES)
        {
            if (key.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * key 是否看起来是 OSS 裸 key（非 /profile/、非 /common/attach/、非 http 外链）。
     */
    public static boolean isBareKey(String path)
    {
        return StringUtils.isNotEmpty(path) && !path.startsWith("/") && !isHttpUrl(path);
    }

    /**
     * 是否 http(s) 外链。
     */
    public static boolean isHttpUrl(String path)
    {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }
}
