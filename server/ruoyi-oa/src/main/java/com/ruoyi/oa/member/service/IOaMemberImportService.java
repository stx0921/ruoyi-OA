package com.ruoyi.oa.member.service;

import javax.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.oa.member.dto.ImportConfirmDTO;
import com.ruoyi.oa.member.vo.ImportConfirmVO;
import com.ruoyi.oa.member.vo.ImportValidateResultVO;

/**
 * 成员 Excel 导入 Service（模板 / 校验 / 确认三步）
 *
 * @author ruoyi-oa
 */
public interface IOaMemberImportService
{
    /**
     * 下载导入模板（12 列 + 填写说明）
     */
    void downloadTemplate(HttpServletResponse response);

    /**
     * 上传校验：只落导入批次与行级错误，不写业务表
     *
     * @param file          上传文件
     * @param updateSupport true=更新已有档案，false=跳过重复行
     */
    ImportValidateResultVO validate(MultipartFile file, boolean updateSupport);

    /**
     * 确认导入：重新解析校验后分批入库（每行独立事务，单行失败不回滚整批）
     */
    ImportConfirmVO confirm(ImportConfirmDTO dto);

}
