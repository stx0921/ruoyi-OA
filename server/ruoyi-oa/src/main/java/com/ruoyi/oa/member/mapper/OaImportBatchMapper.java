package com.ruoyi.oa.member.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.member.domain.OaImportBatch;

/**
 * 导入批次 Mapper
 *
 * @author ruoyi-oa
 */
public interface OaImportBatchMapper
{
    /**
     * 新增批次
     */
    int insertBatch(OaImportBatch batch);

    /**
     * 按主键查询批次
     */
    OaImportBatch selectBatchById(@Param("batchId") Long batchId);

    /**
     * validate 阶段回写计数与错误明细地址（状态仍为待确认）
     */
    int updateBatchValidate(@Param("batchId") Long batchId, @Param("successRows") Integer successRows,
            @Param("failRows") Integer failRows, @Param("updateRows") Integer updateRows,
            @Param("skipRows") Integer skipRows, @Param("errorFileUrl") String errorFileUrl);

    /**
     * confirm 阶段回写计数并置为已确认
     */
    int updateBatchConfirm(@Param("batchId") Long batchId, @Param("successRows") Integer successRows,
            @Param("failRows") Integer failRows, @Param("updateRows") Integer updateRows,
            @Param("skipRows") Integer skipRows, @Param("errorFileUrl") String errorFileUrl);

    /**
     * 查询指定前缀下最大批次号（批次号生成 DB 兜底）
     */
    String selectMaxBatchNo(@Param("prefix") String prefix);

}
