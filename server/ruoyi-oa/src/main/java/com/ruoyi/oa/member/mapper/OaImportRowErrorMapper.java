package com.ruoyi.oa.member.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.member.domain.OaImportRowError;

/**
 * 导入行级错误 Mapper
 *
 * @author ruoyi-oa
 */
public interface OaImportRowErrorMapper
{
    /**
     * 批量新增行级错误
     */
    int batchInsert(@Param("list") List<OaImportRowError> list);

    /**
     * 按批次查询行级错误
     */
    List<OaImportRowError> selectByBatchId(@Param("batchId") Long batchId);

    /**
     * 按批次删除行级错误（confirm 重新校验前清理）
     */
    int deleteByBatchId(@Param("batchId") Long batchId);

}
