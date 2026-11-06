package com.ruoyi.oa.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.member.domain.OaMember;
import com.ruoyi.oa.member.domain.OaMemberLog;
import com.ruoyi.oa.member.vo.MemberDetailVO;
import com.ruoyi.oa.member.vo.MemberListVO;
import com.ruoyi.oa.member.vo.MemberLogVO;

/**
 * 成员 VO 装配与脱敏（打码只在干事视角发生，R12）
 *
 * @author ruoyi-oa
 */
@Component
public class OaMemberVoAssembler
{
    /** 性别文案 */
    public static final Map<String, String> GENDER_LABELS = new HashMap<>();

    /** 年级文案 */
    public static final Map<String, String> GRADE_LABELS = new HashMap<>();

    /** 职务文案 */
    public static final Map<String, String> POSITION_LABELS = new HashMap<>();

    /** 成员状态文案 */
    public static final Map<String, String> STATUS_LABELS = new HashMap<>();

    /** 来源文案 */
    public static final Map<String, String> SOURCE_LABELS = new HashMap<>();

    /** 变更字段中文名 */
    private static final Map<String, String> FIELD_LABELS = new LinkedHashMap<>();

    static
    {
        GENDER_LABELS.put("0", "男");
        GENDER_LABELS.put("1", "女");

        GRADE_LABELS.put("1", "大一");
        GRADE_LABELS.put("2", "大二");
        GRADE_LABELS.put("3", "大三");
        GRADE_LABELS.put("4", "大四");
        GRADE_LABELS.put("5", "研一");
        GRADE_LABELS.put("6", "研二");
        GRADE_LABELS.put("7", "研三");

        POSITION_LABELS.put("1", "会长");
        POSITION_LABELS.put("2", "副会长");
        POSITION_LABELS.put("3", "部长");
        POSITION_LABELS.put("4", "副部长");
        POSITION_LABELS.put("5", "干事");

        STATUS_LABELS.put("0", "在籍");
        STATUS_LABELS.put("1", "已退会");
        STATUS_LABELS.put("2", "已毕业");

        SOURCE_LABELS.put("1", "自助申请");
        SOURCE_LABELS.put("2", "手动录入");
        SOURCE_LABELS.put("3", "Excel导入");

        FIELD_LABELS.put("name", "姓名");
        FIELD_LABELS.put("gender", "性别");
        FIELD_LABELS.put("phone", "手机号");
        FIELD_LABELS.put("college", "学院");
        FIELD_LABELS.put("major", "专业");
        FIELD_LABELS.put("className", "班级");
        FIELD_LABELS.put("grade", "年级");
        FIELD_LABELS.put("cohort", "届次");
        FIELD_LABELS.put("deptId", "部门");
        FIELD_LABELS.put("position", "职务");
        FIELD_LABELS.put("status", "状态");
        FIELD_LABELS.put("email", "邮箱");
        FIELD_LABELS.put("wechat", "微信");
        FIELD_LABELS.put("qq", "QQ");
        FIELD_LABELS.put("skills", "特长");
        FIELD_LABELS.put("intro", "自我介绍");
        FIELD_LABELS.put("obeyAdjust", "服从调剂");
        FIELD_LABELS.put("remark", "备注");
        FIELD_LABELS.put("joinDate", "入会日期");
        FIELD_LABELS.put("leaveDate", "离会日期");
        FIELD_LABELS.put("password", "密码");
    }

    @Autowired
    private OaSensitiveStrategy sensitiveStrategy;

    @Autowired
    private OaCurrentUser currentUser;

    @Autowired
    private OaDictLabelProvider dictLabels;

    /**
     * 列表装配（一次请求只判定一次脱敏级别，也只读一次字典）
     */
    public List<MemberListVO> toListVO(List<OaMember> members)
    {
        List<MemberListVO> result = new ArrayList<>();
        if (members == null)
        {
            return result;
        }
        boolean mask = sensitiveStrategy.mask();
        Map<String, Map<String, String>> dictMaps = loadDictMaps();
        for (OaMember member : members)
        {
            result.add(toListVO(member, mask, dictMaps));
        }
        return result;
    }

    /**
     * 列表装配（指定脱敏）
     */
    public MemberListVO toListVO(OaMember member, boolean mask)
    {
        return toListVO(member, mask, loadDictMaps());
    }

    private MemberListVO toListVO(OaMember member, boolean mask, Map<String, Map<String, String>> dictMaps)
    {
        MemberListVO vo = new MemberListVO();
        fillBase(vo, member, mask, dictMaps);
        return vo;
    }

    /**
     * 详情装配：本人明细文；干事查看他人时打码并隐藏附件
     */
    public MemberDetailVO toDetailVO(OaMember member, List<OaMemberLog> logs, boolean canViewAccount)
    {
        MemberDetailVO vo = new MemberDetailVO();
        boolean mask = sensitiveStrategy.mask() && !currentUser.isSelf(member.getUserId());
        Map<String, Map<String, String>> dictMaps = loadDictMaps();
        fillBase(vo, member, mask, dictMaps);
        vo.setCollege(member.getCollege());
        vo.setMajor(member.getMajor());
        vo.setClassName(member.getClassName());
        vo.setSkills(member.getSkills());
        vo.setIntro(member.getIntro());
        vo.setObeyAdjust(member.getObeyAdjust());
        vo.setRemark(member.getRemark());
        vo.setLeaveDate(member.getLeaveDate());
        vo.setCreateTime(member.getCreateTime());
        vo.setUpdateTime(member.getUpdateTime());
        vo.setLastLoginTime(member.getLastLoginTime());
        vo.setCanViewAccount(canViewAccount);
        if (mask)
        {
            vo.setPhotoFileId(null);
            vo.setPhotoUrl(null);
            vo.setCardFileId(null);
            vo.setCardUrl(null);
        }
        else
        {
            Long photoId = OaFileUrlHelper.parseFileId(member.getPhotoUrl());
            Long cardId = OaFileUrlHelper.parseFileId(member.getCardUrl());
            vo.setPhotoFileId(photoId);
            vo.setPhotoUrl(OaFileUrlHelper.url(photoId));
            vo.setCardFileId(cardId);
            vo.setCardUrl(OaFileUrlHelper.url(cardId));
        }
        List<MemberLogVO> logVos = new ArrayList<>();
        if (logs != null)
        {
            for (OaMemberLog log : logs)
            {
                logVos.add(toLogVO(log));
            }
        }
        vo.setMemberLogs(logVos);
        return vo;
    }

    /**
     * 日志装配（含变更摘要）
     */
    public MemberLogVO toLogVO(OaMemberLog log)
    {
        MemberLogVO vo = new MemberLogVO();
        vo.setLogId(log.getLogId());
        vo.setMemberId(log.getMemberId());
        vo.setMemberName(log.getMemberName());
        vo.setStudentNo(log.getStudentNo());
        vo.setAction(log.getAction());
        vo.setDeptId(log.getDeptId());
        vo.setDeptName(log.getDeptName());
        vo.setBeforeJson(log.getBeforeJson());
        vo.setAfterJson(log.getAfterJson());
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorName(StringUtils.isEmpty(log.getOperatorName()) ? "系统" : log.getOperatorName());
        vo.setIp(log.getIp());
        vo.setCreateTime(log.getCreateTime());
        vo.setSummary(summary(log.getBeforeJson(), log.getAfterJson()));
        return vo;
    }

    /**
     * 生成变更摘要：手机号 138****5678 → 139****1234；部门 组织部 → 外联部
     */
    public String summary(String beforeJson, String afterJson)
    {
        JSONObject before = parse(beforeJson);
        JSONObject after = parse(afterJson);
        if (before == null && after == null)
        {
            return "";
        }
        Map<String, String> parts = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>();
        if (before != null)
        {
            keys.addAll(before.keySet());
        }
        if (after != null)
        {
            for (String key : after.keySet())
            {
                if (!keys.contains(key))
                {
                    keys.add(key);
                }
            }
        }
        for (String key : keys)
        {
            String oldValue = before == null ? null : valueOf(before.get(key));
            String newValue = after == null ? null : valueOf(after.get(key));
            if (StringUtils.equals(oldValue, newValue))
            {
                continue;
            }
            String label = FIELD_LABELS.getOrDefault(key, key);
            if (StringUtils.isEmpty(oldValue))
            {
                parts.put(label, label + " " + defaultValue(newValue));
            }
            else
            {
                parts.put(label, label + " " + oldValue + " → " + defaultValue(newValue));
            }
        }
        return String.join("；", parts.values());
    }

    private String valueOf(Object value)
    {
        if (value == null)
        {
            return "";
        }
        if (value instanceof JSONObject)
        {
            return JSON.toJSONString(value);
        }
        return String.valueOf(value);
    }

    private String defaultValue(String value)
    {
        return StringUtils.isEmpty(value) ? "空" : value;
    }

    private JSONObject parse(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return null;
        }
        try
        {
            return JSON.parseObject(json);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private void fillBase(MemberListVO vo, OaMember member, boolean mask,
            Map<String, Map<String, String>> dictMaps)
    {
        vo.setMemberId(member.getMemberId());
        vo.setMemberNo(member.getMemberNo());
        vo.setName(member.getName());
        vo.setGender(member.getGender());
        vo.setGenderLabel(label(dictMaps, "sys_user_sex", member.getGender()));
        vo.setStudentNo(member.getStudentNo());
        vo.setDeptId(member.getDeptId());
        vo.setDeptName(member.getDeptName());
        vo.setGrade(member.getGrade());
        vo.setGradeLabel(label(dictMaps, "oa_grade", member.getGrade()));
        vo.setCohort(member.getCohort());
        vo.setCohortLabel(cohortLabel(dictMaps, member.getCohort()));
        vo.setPosition(member.getPosition());
        vo.setPositionLabel(label(dictMaps, "oa_position", member.getPosition()));
        vo.setStatus(member.getStatus());
        vo.setStatusLabel(label(dictMaps, "oa_member_status", member.getStatus()));
        vo.setSource(member.getSource());
        vo.setSourceLabel(label(dictMaps, "oa_member_source", member.getSource()));
        vo.setJoinDate(member.getJoinDate());
        vo.setUserId(member.getUserId());
        vo.setAccountStatus(member.getAccountStatus());
        if (mask)
        {
            vo.setPhone(sensitiveStrategy.phone(member.getPhone()));
            vo.setEmail(sensitiveStrategy.email(member.getEmail()));
            vo.setWechat(sensitiveStrategy.wechat(member.getWechat()));
            vo.setQq(sensitiveStrategy.qq(member.getQq()));
        }
        else
        {
            vo.setPhone(member.getPhone());
            vo.setEmail(member.getEmail());
            vo.setWechat(member.getWechat());
            vo.setQq(member.getQq());
        }
    }

    /**
     * 一次装配内复用的标签映射：字典优先、硬编码兜底
     */
    private Map<String, Map<String, String>> loadDictMaps()
    {
        Map<String, Map<String, String>> maps = new HashMap<>();
        maps.put("sys_user_sex", dictLabels.labels("sys_user_sex", GENDER_LABELS));
        maps.put("oa_grade", dictLabels.labels("oa_grade", GRADE_LABELS));
        maps.put("oa_cohort", dictLabels.labels("oa_cohort", null));
        maps.put("oa_position", dictLabels.labels("oa_position", POSITION_LABELS));
        maps.put("oa_member_status", dictLabels.labels("oa_member_status", STATUS_LABELS));
        maps.put("oa_member_source", dictLabels.labels("oa_member_source", SOURCE_LABELS));
        return maps;
    }

    private String label(Map<String, Map<String, String>> dictMaps, String dictType, String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return "";
        }
        Map<String, String> dict = dictMaps.get(dictType);
        return dict == null ? "" : dict.getOrDefault(value, "");
    }

    /** 届次：字典 oa_cohort 优先，缺省回退「值 + 届」 */
    private String cohortLabel(Map<String, Map<String, String>> dictMaps, String cohort)
    {
        if (StringUtils.isEmpty(cohort))
        {
            return "";
        }
        String label = label(dictMaps, "oa_cohort", cohort);
        return StringUtils.isEmpty(label) ? cohort + "届" : label;
    }
}
