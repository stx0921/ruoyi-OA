package com.ruoyi.oa.apply.service;

import javax.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.oa.apply.domain.OaFile;
import com.ruoyi.oa.apply.vo.FileUploadVO;

/**
 * 附件 Service 接口（统一落 oa_file，下载走鉴权接口）
 *
 * @author ruoyi-oa
 */
public interface IOaFileService
{
    /**
     * PC 端上传附件（照片/学生证）
     *
     * @param file       文件
     * @param bizType    业务类型（member_photo/member_card）
     * @param uploaderId 上传人ID
     * @return 附件记录
     */
    OaFile upload(MultipartFile file, String bizType, Long uploaderId);

    /**
     * H5 匿名上传附件（需 verifyToken 有效）
     *
     * @param file        文件
     * @param bizType     业务类型
     * @param verifyToken 图形验证码校验令牌
     * @return {fileId,fileUrl}
     */
    FileUploadVO uploadPublic(MultipartFile file, String bizType, String verifyToken);

    /**
     * 保存内存字节为附件（导入错误明细 xlsx 等）
     *
     * @param fileName     存储文件名（含后缀）
     * @param originalName 原始文件名
     * @param data         文件内容
     * @param bizType      业务类型
     * @param bizId        业务主键（可空）
     * @param uploaderId   上传人（可空）
     * @return 附件记录
     */
    OaFile saveBytes(String fileName, String originalName, byte[] data, String bizType, Long bizId,
            Long uploaderId);

    /**
     * 按主键查询附件
     */
    OaFile selectFileById(Long fileId);

    /**
     * 鉴权下载（流式输出到 response）
     */
    void download(Long fileId, HttpServletResponse response);

    /**
     * 绑定附件到业务（任务保存/提交/验收时回填 biz_type=task、biz_id=taskId）
     *
     * @param fileId  附件 ID
     * @param bizType 业务类型
     * @param bizId   业务主键
     */
    void bindBiz(Long fileId, String bizType, Long bizId);

    /**
     * 批量绑定附件到业务
     */
    void bindBiz(java.util.List<Long> fileIds, String bizType, Long bizId);

    /**
     * 查询业务附件（如任务详情附件）
     */
    java.util.List<OaFile> selectByBiz(String bizType, Long bizId);

    /**
     * 删除业务附件记录（任务/项目硬删除时清理；物理文件保留）
     */
    void deleteByBiz(String bizType, Long bizId);

}
