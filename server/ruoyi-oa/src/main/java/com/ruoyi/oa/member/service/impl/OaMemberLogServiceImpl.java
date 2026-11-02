package com.ruoyi.oa.member.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.oa.member.domain.OaMemberLog;
import com.ruoyi.oa.member.dto.MemberLogQuery;
import com.ruoyi.oa.member.mapper.OaMemberLogMapper;
import com.ruoyi.oa.member.service.IOaMemberLogService;
import com.ruoyi.oa.member.vo.MemberLogVO;
import com.ruoyi.oa.support.OaMemberVoAssembler;

/**
 * 成员变动日志查询 Service 实现
 *
 * @author ruoyi-oa
 */
@Service
public class OaMemberLogServiceImpl implements IOaMemberLogService
{
    @Autowired
    private OaMemberLogMapper memberLogMapper;

    @Autowired
    private OaMemberVoAssembler voAssembler;

    @Override
    @DataScope(deptAlias = "l")
    public List<MemberLogVO> selectLogList(MemberLogQuery query)
    {
        List<OaMemberLog> logs = memberLogMapper.selectLogList(query);
        List<MemberLogVO> result = new ArrayList<>();
        if (logs != null)
        {
            for (OaMemberLog log : logs)
            {
                result.add(voAssembler.toLogVO(log));
            }
        }
        // 保留 PageHelper 分页信息（VO 转换后 total 不丢）
        if (logs instanceof com.github.pagehelper.Page)
        {
            com.github.pagehelper.Page<OaMemberLog> page = (com.github.pagehelper.Page<OaMemberLog>) logs;
            com.github.pagehelper.Page<MemberLogVO> voPage = new com.github.pagehelper.Page<MemberLogVO>(
                    page.getPageNum(), page.getPageSize());
            voPage.setTotal(page.getTotal());
            voPage.addAll(result);
            return voPage;
        }
        return result;
    }
}
