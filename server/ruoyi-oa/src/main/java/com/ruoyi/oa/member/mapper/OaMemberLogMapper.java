package com.ruoyi.oa.member.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.member.domain.OaMemberLog;
import com.ruoyi.oa.member.dto.MemberLogQuery;

/**
 * 成员变动日志 Mapper
 *
 * @author ruoyi-oa
 */
public interface OaMemberLogMapper
{
    /**
     * 追加日志
     */
    int insertMemberLog(OaMemberLog log);

    /**
     * 分页查询日志（需配合 @DataScope，SQL 末尾拼 ${params.dataScope}，别名 l）
     */
    List<OaMemberLog> selectLogList(MemberLogQuery query);

    /**
     * 按 ID 查询日志
     */
    OaMemberLog selectLogById(Long logId);

    /**
     * 查询某成员最近 N 条日志
     */
    List<OaMemberLog> selectLogByMemberId(@Param("memberId") Long memberId, @Param("limit") Integer limit);
}
