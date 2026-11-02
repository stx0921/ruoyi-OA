package com.ruoyi.oa.member.service;

import java.util.List;
import com.ruoyi.oa.member.dto.MemberLogQuery;
import com.ruoyi.oa.member.vo.MemberLogVO;

/**
 * 成员变动日志查询 Service
 *
 * @author ruoyi-oa
 */
public interface IOaMemberLogService
{
    /**
     * 分页查询日志（数据权限：部长仅本部门）
     */
    List<MemberLogVO> selectLogList(MemberLogQuery query);
}
