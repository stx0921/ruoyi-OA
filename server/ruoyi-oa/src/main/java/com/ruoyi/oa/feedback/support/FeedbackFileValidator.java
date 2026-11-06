package com.ruoyi.oa.feedback.support;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * 工单附件校验器：扩展名白名单 + 文件头魔数 + 单文件大小
 *
 * <p>图片 jpg/jpeg/png/gif/webp ≤5MB；Word doc/docx ≤20MB；不信任前端 MIME。</p>
 *
 * @author ruoyi-oa
 */
@Component
public class FeedbackFileValidator
{
    private static final Logger log = LoggerFactory.getLogger(FeedbackFileValidator.class);

    /** 图片最大 5MB */
    public static final long IMAGE_MAX_SIZE = 5L * 1024 * 1024;

    /** Word 最大 20MB */
    public static final long WORD_MAX_SIZE = 20L * 1024 * 1024;

    /** 文件类型：图片 */
    public static final String TYPE_IMAGE = "image";

    /** 文件类型：文档 */
    public static final String TYPE_WORD = "word";

    /** 图片扩展名 */
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"));

    /** Word 扩展名 */
    private static final Set<String> WORD_EXTENSIONS = new HashSet<>(Arrays.asList("doc", "docx"));

    /**
     * 校验并返回文件类型（image / word）
     *
     * @param file 上传文件
     * @return image / word
     */
    public String validate(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("上传文件不能为空", 400);
        }
        String originalName = file.getOriginalFilename();
        if (StringUtils.isEmpty(originalName))
        {
            throw new ServiceException("文件名不能为空", 400);
        }
        String extension = getExtension(originalName);
        boolean image = IMAGE_EXTENSIONS.contains(extension);
        boolean word = WORD_EXTENSIONS.contains(extension);
        if (!image && !word)
        {
            throw new ServiceException("附件仅支持 jpg/jpeg/png/gif/webp 图片或 doc/docx 文档", 400);
        }
        if (image && file.getSize() > IMAGE_MAX_SIZE)
        {
            throw new ServiceException("图片大小不能超过 5MB", 400);
        }
        if (word && file.getSize() > WORD_MAX_SIZE)
        {
            throw new ServiceException("Word 文档大小不能超过 20MB", 400);
        }
        byte[] head = readHead(file);
        if (image && !isImageMagic(extension, head))
        {
            throw new ServiceException("文件内容与扩展名不匹配，请上传真实的图片文件", 400);
        }
        if (word && !isWordMagic(extension, head))
        {
            throw new ServiceException("文件内容与扩展名不匹配，请上传真实的 Word 文档", 400);
        }
        log.debug("工单附件校验通过 name={} size={} type={}", originalName, file.getSize(),
                image ? TYPE_IMAGE : TYPE_WORD);
        return image ? TYPE_IMAGE : TYPE_WORD;
    }

    /**
     * 按文件名判断展示类型（详情附件用）
     */
    public String detectFileType(String fileName)
    {
        String extension = getExtension(fileName);
        return IMAGE_EXTENSIONS.contains(extension) ? TYPE_IMAGE : TYPE_WORD;
    }

    private byte[] readHead(MultipartFile file)
    {
        byte[] head = new byte[12];
        try (InputStream inputStream = file.getInputStream())
        {
            int read = inputStream.read(head);
            if (read < 4)
            {
                throw new ServiceException("文件内容不完整，请重新上传", 400);
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("读取文件头失败", e);
            throw new ServiceException("文件读取失败，请重新上传", 400);
        }
        return head;
    }

    private boolean isImageMagic(String extension, byte[] head)
    {
        switch (extension)
        {
            case "jpg":
            case "jpeg":
                return (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
            case "png":
                return (head[0] & 0xFF) == 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47
                        && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A;
            case "gif":
                return head[0] == 'G' && head[1] == 'I' && head[2] == 'F';
            case "webp":
                return head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                        && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
            default:
                return false;
        }
    }

    private boolean isWordMagic(String extension, byte[] head)
    {
        if ("docx".equals(extension))
        {
            // ZIP 头：PK\x03\x04（部分工具生成 PK\x05\x06 / PK\x07\x08）
            return head[0] == 'P' && head[1] == 'K'
                    && ((head[2] & 0xFF) == 0x03 || (head[2] & 0xFF) == 0x05 || (head[2] & 0xFF) == 0x07);
        }
        // doc：OLE2 复合文档头
        return (head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF && (head[2] & 0xFF) == 0x11
                && (head[3] & 0xFF) == 0xE0 && (head[4] & 0xFF) == 0xA1 && (head[5] & 0xFF) == 0xB1
                && (head[6] & 0xFF) == 0x1A && (head[7] & 0xFF) == 0xE1;
    }

    private String getExtension(String fileName)
    {
        if (StringUtils.isEmpty(fileName))
        {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1)
        {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

}
