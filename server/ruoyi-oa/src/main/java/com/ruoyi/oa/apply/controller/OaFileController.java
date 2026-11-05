package com.ruoyi.oa.apply.controller;

import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.oa.apply.domain.OaFile;
import com.ruoyi.oa.apply.service.IOaFileService;
import com.ruoyi.oa.apply.vo.FileUploadVO;
import com.ruoyi.oa.support.OaFileUrlHelper;

/**
 * 附件 Controller（PC 上传 + 鉴权下载）
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/file")
public class OaFileController extends BaseController
{
    @Autowired
    private IOaFileService fileService;

    /**
     * PC 端上传附件（人员照片/学生证、任务附件），返回 {fileId,fileUrl}
     */
    @PreAuthorize("@ss.hasAnyPermi('oa:member:edit,oa:task:query,oa:task:add,oa:task:edit,oa:task:submit,oa:task:audit')")
    @Log(title = "附件管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "bizType", required = false, defaultValue = "member_photo") String bizType)
    {
        OaFile oaFile = fileService.upload(file, bizType, getUserId());
        FileUploadVO vo = new FileUploadVO();
        vo.setFileId(oaFile.getFileId());
        vo.setFileUrl(OaFileUrlHelper.url(oaFile.getFileId()));
        return success(vo);
    }

    /**
     * 鉴权下载 / 预览附件（不暴露物理路径）
     */
    @PreAuthorize("@ss.hasAnyPermi('oa:member:query,oa:apply:query,oa:task:query')")
    @GetMapping("/{fileId}")
    public void download(@PathVariable("fileId") Long fileId, HttpServletResponse response)
    {
        fileService.download(fileId, response);
    }

}
