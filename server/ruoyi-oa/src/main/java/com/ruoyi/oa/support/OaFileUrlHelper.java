package com.ruoyi.oa.support;

import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;

/**
 * 附件逻辑地址工具：统一使用 /oa/file/{fileId}，不暴露物理路径
 *
 * @author ruoyi-oa
 */
@Component
public class OaFileUrlHelper
{
    /** 逻辑下载地址前缀 */
    public static final String FILE_URL_PREFIX = "/oa/file/";

    /**
     * 由文件 ID 生成逻辑地址
     */
    public static String url(Long fileId)
    {
        if (fileId == null)
        {
            return "";
        }
        return FILE_URL_PREFIX + fileId;
    }

    /**
     * 从逻辑地址中解析文件 ID（非法返回 null）
     */
    public static Long parseFileId(String url)
    {
        if (StringUtils.isEmpty(url) || !url.startsWith(FILE_URL_PREFIX))
        {
            return null;
        }
        try
        {
            return Long.valueOf(url.substring(FILE_URL_PREFIX.length()));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
