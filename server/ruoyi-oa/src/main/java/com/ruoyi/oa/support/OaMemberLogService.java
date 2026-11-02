package com.ruoyi.oa.support;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.oa.member.domain.OaMember;
import com.ruoyi.oa.member.domain.OaMemberLog;
import com.ruoyi.oa.member.mapper.OaMemberLogMapper;

/**
 * 成员变动日志服务（只追加）
 *
 * @author ruoyi-oa
 */
@Service
public class OaMemberLogService
{
    @Autowired
    private OaMemberLogMapper memberLogMapper;

    @Autowired
    private OaCurrentUser currentUser;

    /**
     * 记录日志（before/after 为仅变更字段）
     */
    public void record(OaMember member, String action, Map<String, Object> before, Map<String, Object> after)
    {
        if (member == null)
        {
            return;
        }
        record(member.getMemberId(), member.getDeptId(), action,
                before == null ? null : JSON.toJSONString(before),
                after == null ? null : JSON.toJSONString(after));
    }

    /**
     * 记录日志（原始 JSON）
     */
    public void record(Long memberId, Long deptId, String action, String beforeJson, String afterJson)
    {
        if (memberId == null)
        {
            return;
        }
        OaMemberLog log = new OaMemberLog();
        log.setMemberId(memberId);
        log.setDeptId(deptId);
        log.setAction(action);
        log.setBeforeJson(beforeJson);
        log.setAfterJson(afterJson);
        try
        {
            log.setOperatorId(currentUser.userId());
            log.setOperatorName(currentUser.nickName());
            HttpServletRequest request = ServletUtils.getRequest();
            log.setIp(request == null ? "" : IpUtils.getIpAddr(request));
        }
        catch (Exception e)
        {
            log.setOperatorName("系统");
            log.setIp("");
        }
        log.setCreateTime(new Date());
        memberLogMapper.insertMemberLog(log);
    }

    /**
     * 成员快照（仅业务可读字段，用于 diff）
     */
    public Map<String, Object> snapshot(OaMember member)
    {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (member == null)
        {
            return snapshot;
        }
        snapshot.put("name", member.getName());
        snapshot.put("gender", member.getGender());
        snapshot.put("phone", member.getPhone());
        snapshot.put("college", member.getCollege());
        snapshot.put("major", member.getMajor());
        snapshot.put("className", member.getClassName());
        snapshot.put("grade", member.getGrade());
        snapshot.put("cohort", member.getCohort());
        snapshot.put("deptId", member.getDeptId());
        snapshot.put("position", member.getPosition());
        snapshot.put("status", member.getStatus());
        snapshot.put("email", member.getEmail());
        snapshot.put("wechat", member.getWechat());
        snapshot.put("qq", member.getQq());
        snapshot.put("skills", member.getSkills());
        snapshot.put("intro", member.getIntro());
        snapshot.put("obeyAdjust", member.getObeyAdjust());
        snapshot.put("remark", member.getRemark());
        return snapshot;
    }
}
