package com.ruoyi.oa.member.controller;

import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.oa.member.dto.ImportConfirmDTO;
import com.ruoyi.oa.member.service.IOaMemberImportService;

/**
 * 成员批量导入 Controller（PC-03 三步向导）
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/member/import")
public class OaMemberImportController extends BaseController
{
    @Autowired
    private IOaMemberImportService importService;

    /**
     * 下载导入模板（12 列 + 填写说明）
     */
    @PreAuthorize("@ss.hasPermi('oa:member:import')")
    @GetMapping("/template")
    public void template(HttpServletResponse response)
    {
        importService.downloadTemplate(response);
    }

    /**
     * 上传校验：返回行级错误与重复策略统计（不写业务表）
     */
    @PreAuthorize("@ss.hasPermi('oa:member:import')")
    @Log(title = "成员导入校验", businessType = BusinessType.IMPORT)
    @PostMapping("/validate")
    public AjaxResult validate(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateSupport", required = false, defaultValue = "1") String updateSupport)
    {
        boolean update = "1".equals(updateSupport) || "true".equalsIgnoreCase(updateSupport);
        return success(importService.validate(file, update));
    }

    /**
     * 确认导入（重新解析校验后入库，分批 100 行，单行失败不回滚整批）
     */
    @PreAuthorize("@ss.hasPermi('oa:member:import')")
    @Log(title = "成员导入确认", businessType = BusinessType.IMPORT)
    @PostMapping("/confirm")
    public AjaxResult confirm(@RequestBody ImportConfirmDTO dto)
    {
        if (dto.getUpdateSupport() == null)
        {
            dto.setUpdateSupport(Boolean.TRUE);
        }
        return success(importService.confirm(dto));
    }

}
