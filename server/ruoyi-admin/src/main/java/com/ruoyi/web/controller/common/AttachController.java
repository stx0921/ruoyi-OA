package com.ruoyi.web.controller.common;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.storage.FileStorageRouter;
import com.ruoyi.common.storage.StorageException;
import com.ruoyi.common.storage.StorageKeyUtils;
import com.ruoyi.common.storage.StorageObject;

/**
 * 匿名附件代理（仅用于头像 / 富文本图片这类 &lt;img&gt; 场景）。
 *
 * <p>业务附件（Word / 简历 / 工单材料）绝不走这里，仍走 {@code /oa/file/{id}} 鉴权 + 业务归属校验。
 * 安全约束：key 只允许 UUID 形态的安全字符，扩展名只允许 jpg/jpeg/png/gif/webp，
 * 拒绝 ../、绝对路径、编码绕过；响应加 nosniff。</p>
 *
 * @author ruoyi-oa
 */
@RestController
public class AttachController
{
    private static final Logger log = LoggerFactory.getLogger(AttachController.class);

    /** 公共代理前缀（与 FileStorageRouter 保持一致） */
    private static final String ATTACH_PREFIX = "/common/attach/";

    /** 浏览器缓存 7 天（key 为 UUID，内容不可变） */
    private static final String CACHE_CONTROL = "public, max-age=604800";

    @Autowired
    private FileStorageRouter storageRouter;

    @GetMapping("/common/attach/**")
    public void read(HttpServletRequest request, HttpServletResponse response)
    {
        String key = resolveKey(request);
        if (key == null || !StorageKeyUtils.isSafePublicKey(key))
        {
            log.warn("非法的公共附件路径 uri={}", request.getRequestURI());
            sendError(response, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try (StorageObject object = storageRouter.openByPath(ATTACH_PREFIX + key))
        {
            if (object == null)
            {
                sendError(response, HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setStatus(HttpServletResponse.SC_OK);
            String contentType = object.getContentType();
            response.setContentType(contentType == null ? StorageKeyUtils.contentTypeOf(key) : contentType);
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", CACHE_CONTROL);
            if (object.getSize() > 0)
            {
                response.setContentLengthLong(object.getSize());
            }
            IOUtils.copyLarge(object.getInputStream(), response.getOutputStream());
            response.flushBuffer();
        }
        catch (StorageException e)
        {
            log.error("公共附件读取失败 key={}", key, e);
            sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
        catch (IOException e)
        {
            log.warn("公共附件输出中断 key={}", key, e);
        }
    }

    /**
     * 从 requestURI 中截取 AMBIGUOUS 前缀后的 key；兼容 context-path（通过在 URI 中定位前缀）。
     */
    private String resolveKey(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        if (uri == null)
        {
            return null;
        }
        int index = uri.indexOf(ATTACH_PREFIX);
        if (index < 0)
        {
            return null;
        }
        String key = uri.substring(index + ATTACH_PREFIX.length());
        // 查询串不会出现在 requestURI 中，这里只做空值兜底
        return key.isEmpty() ? null : key;
    }

    private void sendError(HttpServletResponse response, int status)
    {
        try
        {
            response.sendError(status);
        }
        catch (IOException e)
        {
            log.debug("写出错误状态失败 status={}", status, e);
        }
    }
}
