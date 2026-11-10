package com.ruoyi.web.controller.common;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.storage.FileStorageRouter;
import com.ruoyi.common.storage.StorageException;
import com.ruoyi.common.storage.StorageKeyUtils;
import com.ruoyi.common.storage.StorageObject;
import com.ruoyi.common.storage.StoredFile;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.config.ServerConfig;

/**
 * 通用请求处理
 *
 * <p>上传走存储抽象：本地模式写 /profile/upload/...，OSS 模式写裸 key 并返回
 * /common/attach/upload/... 匿名代理 URL（富文本/编辑器图片可直接显示）。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/common")
public class CommonController
{
    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private FileStorageRouter storageRouter;

    private static final String FILE_DELIMITER = ",";

    /**
     * 通用下载请求（历史临时导出文件，固定本地 download 目录）
     *
     * @param fileName 文件名称
     * @param delete 是否删除
     */
    @GetMapping("/download")
    public void fileDownload(String fileName, Boolean delete, HttpServletResponse response, HttpServletRequest request)
    {
        try
        {
            if (!FileUtils.checkAllowDownload(fileName))
            {
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }
            String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
            String filePath = RuoYiConfig.getDownloadPath() + fileName;

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (delete)
            {
                FileUtils.deleteFile(filePath);
            }
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 通用上传请求（单个）
     *
     * <p>返回契约：图片返回浏览器可直接展示的相对 URL（local=/profile/upload/...，
     * oss=/common/attach/upload/...），非图片返回写库引用（local=/profile/...，oss=裸 key）。</p>
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(MultipartFile file) throws Exception
    {
        try
        {
            if (file == null || file.isEmpty())
            {
                return AjaxResult.error("请选择要上传的文件");
            }
            if (StringUtils.isEmpty(file.getOriginalFilename()))
            {
                return AjaxResult.error("文件名不能为空");
            }
            FileUploadUtils.assertFileNameLength(file);
            FileUploadUtils.assertAllowed(file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
            StoredFile stored = storageRouter.store(file.getInputStream(), file.getSize(), file.getOriginalFilename(),
                    "upload");
            String fileName = accessibleRef(stored.getKey());
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(stored.getKey()));
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        }
        catch (StorageException e)
        {
            log.error("通用上传失败", e);
            return AjaxResult.error("附件服务暂时不可用，请稍后再试");
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 通用上传请求（多个）
     */
    @PostMapping("/uploads")
    public AjaxResult uploadFiles(List<MultipartFile> files) throws Exception
    {
        List<String> storedRefs = new ArrayList<String>();
        try
        {
            List<String> urls = new ArrayList<String>();
            List<String> fileNames = new ArrayList<String>();
            List<String> newFileNames = new ArrayList<String>();
            List<String> originalFilenames = new ArrayList<String>();
            for (MultipartFile file : files)
            {
                if (file == null || file.isEmpty())
                {
                    continue;
                }
                if (StringUtils.isEmpty(file.getOriginalFilename()))
                {
                    return AjaxResult.error("文件名不能为空");
                }
                FileUploadUtils.assertFileNameLength(file);
                FileUploadUtils.assertAllowed(file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
                StoredFile stored = storageRouter.store(file.getInputStream(), file.getSize(),
                        file.getOriginalFilename(), "upload");
                String fileName = accessibleRef(stored.getKey());
                storedRefs.add(storageRouter.storeRef(stored.getKey()));
                urls.add(serverConfig.getUrl() + fileName);
                fileNames.add(fileName);
                newFileNames.add(FileUtils.getName(stored.getKey()));
                originalFilenames.add(file.getOriginalFilename());
            }
            AjaxResult ajax = AjaxResult.success();
            ajax.put("urls", StringUtils.join(urls, FILE_DELIMITER));
            ajax.put("fileNames", StringUtils.join(fileNames, FILE_DELIMITER));
            ajax.put("newFileNames", StringUtils.join(newFileNames, FILE_DELIMITER));
            ajax.put("originalFilenames", StringUtils.join(originalFilenames, FILE_DELIMITER));
            return ajax;
        }
        catch (Exception e)
        {
            // 批量上传中途失败：清理已写入的存储对象，避免残留孤儿文件
            for (String ref : storedRefs)
            {
                storageRouter.deleteByPath(ref);
            }
            if (e instanceof StorageException)
            {
                log.error("通用批量上传失败", e);
                return AjaxResult.error("附件服务暂时不可用，请稍后再试");
            }
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 图片使用浏览器可直接展示的代理路径；非图片返回写库引用（私有桶文档仍走鉴权接口）。
     */
    private String accessibleRef(String key)
    {
        return StorageKeyUtils.isSafePublicKey(key) ? storageRouter.publicRef(key) : storageRouter.storeRef(key);
    }

    /**
     * 资源下载（兼容历史 /profile 路径与 OSS 裸 key）。
     *
     * <p>安全：只允许 {@code /profile/...} 与安全裸 key；禁止 http(s) 外链（防止 SSRF）、
     * 禁止绝对路径/路径穿越。历史外链请走业务接口，不在此接口代取。</p>
     */
    @GetMapping("/download/resource")
    public void resourceDownload(String resource, HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        try
        {
            if (!FileUtils.checkAllowDownload(resource) || !isAllowedResourceRef(resource))
            {
                log.warn("非法的资源下载参数 resource={}", resource);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            try (StorageObject object = storageRouter.openByPath(resource))
            {
                if (object == null)
                {
                    log.warn("资源文件不存在 resource={}", resource);
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                // 下载名称
                String downloadName = StringUtils.substringAfterLast(resource, "/");
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                FileUtils.setAttachmentResponseHeader(response, downloadName);
                if (object.getSize() > 0)
                {
                    response.setContentLengthLong(object.getSize());
                }
                IOUtils.copyLarge(object.getInputStream(), response.getOutputStream());
                response.flushBuffer();
            }
        }
        catch (Exception e)
        {
            log.error("下载文件失败 resource={}", resource, e);
        }
    }

    /**
     * 资源下载参数白名单：/profile/ 历史路径或安全裸 key；显式拒绝 http(s)/绝对路径/attach 代理。
     */
    private boolean isAllowedResourceRef(String resource)
    {
        if (StringUtils.isEmpty(resource) || StorageKeyUtils.isHttpUrl(resource) || resource.startsWith("/common/"))
        {
            return false;
        }
        if (resource.startsWith("/profile/"))
        {
            return true;
        }
        return StorageKeyUtils.isBareKey(resource) && StorageKeyUtils.isSafeKey(resource);
    }
}
