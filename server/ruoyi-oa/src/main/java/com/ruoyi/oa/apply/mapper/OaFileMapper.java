package com.ruoyi.oa.apply.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.apply.domain.OaFile;

/**
 * 附件 Mapper 接口
 *
 * @author ruoyi-oa
 */
public interface OaFileMapper
{
    /**
     * 新增附件记录
     */
    int insertFile(OaFile file);

    /**
     * 按主键查询附件
     */
    OaFile selectFileById(@Param("fileId") Long fileId);

    /**
     * 绑定附件到业务对象（照片/学生证提交后回填 biz_id）
     */
    int updateFileBiz(@Param("fileId") Long fileId, @Param("bizType") String bizType,
            @Param("bizId") Long bizId);

    /**
     * 批量查询附件（详情装配用）
     */
    List<OaFile> selectFileByIds(@Param("fileIds") List<Long> fileIds);

    /**
     * 按业务类型 + 业务 ID 查询附件（任务详情）
     */
    List<OaFile> selectFilesByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    /**
     * 按业务删除附件记录（任务/项目硬删除时清理，物理文件保留）
     */
    int deleteByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);

}
