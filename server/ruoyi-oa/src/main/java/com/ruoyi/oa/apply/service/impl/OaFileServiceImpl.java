package com.ruoyi.oa.apply.service.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.storage.FileStorageRouter;
import com.ruoyi.common.storage.StorageException;
import com.ruoyi.common.storage.StorageObject;
import com.ruoyi.common.storage.StoredFile;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.oa.apply.domain.OaFile;
import com.ruoyi.oa.apply.mapper.OaFileMapper;
import com.ruoyi.oa.apply.service.IOaFileService;
import com.ruoyi.oa.apply.support.OaCaptchaService;
import com.ruoyi.oa.apply.vo.FileUploadVO;
import com.ruoyi.oa.support.OaFileUrlHelper;

/**
 * 附件 Service 实现（统一存储抽象：本地磁盘 / 阿里云 OSS，统一落 oa_file，下载走鉴权接口）
 *
 * @author ruoyi-oa
 */
@Service
public class OaFileServiceImpl implements IOaFileService
{
    private static final Logger log = LoggerFactory.getLogger(OaFileServiceImpl.class);

    /** 业务类型：成员照片 */
    public static final String BIZ_MEMBER_PHOTO = "member_photo";

    /** 业务类型：简历 */
    public static final String BIZ_MEMBER_CARD = "member_card";

    /** 业务类型：导入错误明细 */
    public static final String BIZ_IMPORT_ERROR = "import_error";

    /** 业务类型：任务附件 */
    public static final String BIZ_TASK = "task";

    /** 业务类型：工单临时附件（提交前） */
    public static final String BIZ_FEEDBACK_TEMP = "feedback_temp";

    /** 业务类型：工单正式附件（提交后绑定） */
    public static final String BIZ_FEEDBACK = "feedback";

    /** 照片允许扩展名 */
    private static final String[] PHOTO_EXTENSION = { "jpg", "jpeg", "png" };

    /** 简历允许扩展名 */
    private static final String[] CARD_EXTENSION = { "jpg", "jpeg", "png", "pdf" };

    /** 任务附件允许扩展名 */
    private static final String[] TASK_EXTENSION = { "jpg", "jpeg", "png", "pdf", "docx", "xlsx" };

    /** 工单图片允许扩展名 */
    private static final String[] FEEDBACK_IMAGE_EXTENSION = { "jpg", "jpeg", "png", "gif", "webp" };

    /** 工单 Word 允许扩展名 */
    private static final String[] FEEDBACK_DOC_EXTENSION = { "doc", "docx" };

    /** 工单附件全部允许扩展名 */
    private static final String[] FEEDBACK_EXTENSION = { "jpg", "jpeg", "png", "gif", "webp", "doc", "docx" };

    /** 照片最大 2MB */
    private static final long PHOTO_MAX_SIZE = 2 * 1024 * 1024L;

    /** 简历最大 5MB */
    private static final long CARD_MAX_SIZE = 5 * 1024 * 1024L;

    /** 任务附件最大 10MB */
    private static final long TASK_MAX_SIZE = 10 * 1024 * 1024L;

    /** 工单图片最大 5MB */
    private static final long FEEDBACK_IMAGE_MAX_SIZE = 5 * 1024 * 1024L;

    /** 工单 Word 最大 20MB */
    private static final long FEEDBACK_DOC_MAX_SIZE = 20 * 1024 * 1024L;

    /** 常见文件扩展名（用于解析存储名） */
    private static final Collection<String> ALLOWED_BIZ_TYPES = Arrays.asList(BIZ_MEMBER_PHOTO, BIZ_MEMBER_CARD,
            BIZ_TASK, BIZ_FEEDBACK_TEMP, BIZ_FEEDBACK);

    @Autowired
    private OaFileMapper fileMapper;

    @Autowired
    private OaCaptchaService captchaService;

    @Autowired
    private FileStorageRouter storageRouter;

    @Override
    public OaFile upload(MultipartFile file, String bizType, Long uploaderId)
    {
        assertUploadAllowed(file, bizType);
        StoredFile stored;
        try
        {
            // 先存存储（本地/OSS），再落库；失败不留半成品记录
            stored = storageRouter.store(file.getInputStream(), file.getSize(), file.getOriginalFilename(), "oa/upload");
        }
        catch (StorageException e)
        {
            log.error("附件上传失败 bizType={}", bizType, e);
            throw new ServiceException("附件服务暂时不可用，请稍后再试", 503);
        }
        catch (Exception e)
        {
            log.error("附件上传失败 bizType={}", bizType, e);
            throw new ServiceException("附件上传失败，请稍后再试", 500);
        }
        OaFile oaFile = new OaFile();
        oaFile.setFileName(FileUtils.getName(stored.getKey()));
        oaFile.setOriginalName(file.getOriginalFilename());
        // 本地模式：/profile/oa/upload/...；OSS 模式：裸 key oa/upload/...
        oaFile.setFilePath(storageRouter.storeRef(stored.getKey()));
        oaFile.setBizType(bizType);
        oaFile.setFileSize(file.getSize());
        oaFile.setUploaderId(uploaderId);
        try
        {
            fileMapper.insertFile(oaFile);
        }
        catch (RuntimeException e)
        {
            // 落库失败：尽力清理已写入的存储对象，避免产生孤儿文件
            try
            {
                storageRouter.deleteByPath(oaFile.getFilePath());
            }
            catch (Exception ignored)
            {
                // 清理失败不影响原始异常
            }
            throw e;
        }
        log.info("附件上传成功 fileId={} bizType={} size={}", oaFile.getFileId(), bizType, file.getSize());
        return oaFile;
    }

    @Override
    public FileUploadVO uploadPublic(MultipartFile file, String bizType, String verifyToken)
    {
        captchaService.requireVerifyToken(verifyToken);
        // 匿名上传仅开放给人员报名附件，避免匿名写任务/导入类附件
        if (!BIZ_MEMBER_PHOTO.equals(bizType) && !BIZ_MEMBER_CARD.equals(bizType))
        {
            throw new ServiceException("匿名上传不支持的业务类型", 400);
        }
        OaFile oaFile = upload(file, bizType, null);
        FileUploadVO vo = new FileUploadVO();
        vo.setFileId(oaFile.getFileId());
        vo.setFileUrl(OaFileUrlHelper.url(oaFile.getFileId()));
        return vo;
    }

    @Override
    public OaFile saveBytes(String fileName, String originalName, byte[] data, String bizType, Long bizId,
            Long uploaderId)
    {
        String filePath;
        try
        {
            filePath = FileUtils.writeImportBytes(data);
        }
        catch (Exception e)
        {
            log.error("写入附件失败 bizType={}", bizType, e);
            throw new ServiceException("错误明细文件生成失败，请稍后再试", 500);
        }
        OaFile oaFile = new OaFile();
        oaFile.setFileName(StringUtils.isNotEmpty(fileName) ? fileName : FileUtils.getName(filePath));
        oaFile.setOriginalName(StringUtils.isNotEmpty(originalName) ? originalName : oaFile.getFileName());
        oaFile.setFilePath(filePath);
        oaFile.setBizType(bizType);
        oaFile.setBizId(bizId);
        oaFile.setFileSize(data == null ? 0L : (long) data.length);
        oaFile.setUploaderId(uploaderId);
        fileMapper.insertFile(oaFile);
        return oaFile;
    }

    @Override
    public OaFile selectFileById(Long fileId)
    {
        if (fileId == null)
        {
            return null;
        }
        return fileMapper.selectFileById(fileId);
    }

    @Override
    public void bindBiz(Long fileId, String bizType, Long bizId)
    {
        if (fileId == null || bizId == null)
        {
            return;
        }
        fileMapper.updateFileBiz(fileId, bizType, bizId);
    }

    @Override
    public void bindBiz(List<Long> fileIds, String bizType, Long bizId)
    {
        if (fileIds == null || fileIds.isEmpty() || bizId == null)
        {
            return;
        }
        for (Long fileId : fileIds)
        {
            if (fileId != null)
            {
                fileMapper.updateFileBiz(fileId, bizType, bizId);
            }
        }
    }

    @Override
    public List<OaFile> selectByBiz(String bizType, Long bizId)
    {
        if (StringUtils.isEmpty(bizType) || bizId == null)
        {
            return java.util.Collections.emptyList();
        }
        return fileMapper.selectFilesByBiz(bizType, bizId);
    }

    @Override
    public void deleteByBiz(String bizType, Long bizId)
    {
        if (StringUtils.isEmpty(bizType) || bizId == null)
        {
            return;
        }
        fileMapper.deleteByBiz(bizType, bizId);
    }

    @Override
    public void download(Long fileId, HttpServletResponse response)
    {
        writeFile(fileId, true, response);
    }

    @Override
    public void writeFile(Long fileId, boolean attachment, HttpServletResponse response)
    {
        OaFile file = selectFileById(fileId);
        if (file == null)
        {
            throw new ServiceException("附件不存在", 404);
        }
        StorageObject storageObject;
        try
        {
            // 统一路径路由：/profile/ 读本地、/common/attach/ 读 OSS、裸 key 读 OSS、http 读外链
            storageObject = storageRouter.openByPath(file.getFilePath());
        }
        catch (StorageException e)
        {
            log.error("附件读取失败 fileId={} path={}", fileId, file.getFilePath(), e);
            throw new ServiceException("附件服务暂时不可用，请稍后再试", 503);
        }
        if (storageObject == null)
        {
            log.warn("附件物理文件不存在 fileId={} path={}", fileId, file.getFilePath());
            throw new ServiceException("附件文件已丢失，请联系管理员", 404);
        }
        try (StorageObject stream = storageObject)
        {
            String downloadName = StringUtils.isNotEmpty(file.getOriginalName()) ? file.getOriginalName()
                    : file.getFileName();
            if (attachment)
            {
                FileUtils.setAttachmentResponseHeader(response, downloadName);
            }
            else
            {
                response.setContentType(contentType(downloadName));
                response.setHeader("Content-Disposition", "inline");
            }
            if (stream.getSize() > 0)
            {
                response.setContentLengthLong(stream.getSize());
            }
            IOUtils.copyLarge(stream.getInputStream(), response.getOutputStream());
            response.flushBuffer();
        }
        catch (Exception e)
        {
            log.error("附件输出失败 fileId={}", fileId, e);
            throw new ServiceException("附件下载失败，请稍后再试", 500);
        }
    }

    /**
     * 推断内联预览 Content-Type（图片、Word）
     */
    private String contentType(String fileName)
    {
        String extension = getExtension(fileName);
        switch (extension)
        {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 上传前校验：业务类型、扩展名、大小（照片 ≤2MB、简历 ≤5MB）
     */
    private void assertUploadAllowed(MultipartFile file, String bizType)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("请选择要上传的文件", 400);
        }
        if (!ALLOWED_BIZ_TYPES.contains(bizType))
        {
            throw new ServiceException("不支持的业务类型：" + bizType, 400);
        }
        String extension = getExtension(file.getOriginalFilename());
        if (isFeedbackBiz(bizType))
        {
            if (!Arrays.asList(FEEDBACK_EXTENSION).contains(extension))
            {
                throw new ServiceException("工单附件仅支持 jpg/jpeg/png/gif/webp 或 doc/docx", 400);
            }
            boolean doc = Arrays.asList(FEEDBACK_DOC_EXTENSION).contains(extension);
            long maxSize = doc ? FEEDBACK_DOC_MAX_SIZE : FEEDBACK_IMAGE_MAX_SIZE;
            if (file.getSize() > maxSize)
            {
                throw new ServiceException(doc ? "Word 附件不能超过 20MB" : "图片不能超过 5MB", 400);
            }
            return;
        }
        if (BIZ_TASK.equals(bizType))
        {
            if (!Arrays.asList(TASK_EXTENSION).contains(extension))
            {
                throw new ServiceException("附件仅支持 jpg / png / pdf / docx / xlsx", 400);
            }
            if (file.getSize() > TASK_MAX_SIZE)
            {
                throw new ServiceException("单个文件不能超过 10MB", 400);
            }
            return;
        }
        boolean photo = BIZ_MEMBER_PHOTO.equals(bizType);
        String[] allowed = photo ? PHOTO_EXTENSION : CARD_EXTENSION;
        if (!Arrays.asList(allowed).contains(extension))
        {
            if (photo)
            {
                throw new ServiceException("照片仅支持 jpg / png，且不超过 2MB", 400);
            }
            throw new ServiceException("简历仅支持 jpg / png / pdf，且不超过 5MB", 400);
        }
        long maxSize = photo ? PHOTO_MAX_SIZE : CARD_MAX_SIZE;
        if (file.getSize() > maxSize)
        {
            if (photo)
            {
                throw new ServiceException("照片仅支持 jpg / png，且不超过 2MB", 400);
            }
            throw new ServiceException("简历仅支持 jpg / png / pdf，且不超过 5MB", 400);
        }
    }

    private boolean isFeedbackBiz(String bizType)
    {
        return BIZ_FEEDBACK.equals(bizType) || BIZ_FEEDBACK_TEMP.equals(bizType);
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
