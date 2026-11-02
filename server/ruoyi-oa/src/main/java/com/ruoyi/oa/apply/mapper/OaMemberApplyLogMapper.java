package com.ruoyi.oa.apply.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.apply.domain.OaMemberApplyLog;

/**
 * 入会申请操作记录 Mapper 接口（只追加）
 *
 * @author ruoyi-oa
 */
public interface OaMemberApplyLogMapper
{
    /**
     * 新增申请操作记录
     */
    int insertLog(OaMemberApplyLog log);

    /**
     * 按申请ID查询时间轴（正序）
     */
    List<OaMemberApplyLog> selectLogByApplyId(@Param("applyId") Long applyId);

}
