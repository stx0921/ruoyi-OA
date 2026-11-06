package com.ruoyi.oa.volunteer.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.volunteer.domain.VolunteerSignup;
import com.ruoyi.oa.volunteer.dto.SignupQuery;

/**
 * 志愿活动报名 Mapper
 *
 * @author ruoyi-oa
 */
public interface VolunteerSignupMapper
{
    /**
     * 某活动报名名单（联查活动，过滤活动软删除）
     */
    List<VolunteerSignup> selectSignupList(SignupQuery query);

    /**
     * 新增报名（唯一索引 uk_activity_student 兜底重复报名）
     */
    int insertSignup(VolunteerSignup signup);

    /**
     * 单条/批量审核（通过时清空驳回原因）
     */
    int auditSignup(@Param("ids") Long[] ids, @Param("auditStatus") String auditStatus,
            @Param("auditRemark") String auditRemark, @Param("auditorId") Long auditorId,
            @Param("auditorName") String auditorName);

    /**
     * H5 结果查询：studentNo + name 必须匹配；activityId 可空（空则取最近一条报名）
     * （联查活动，过滤活动软删除）
     */
    VolunteerSignup selectSignupForResult(@Param("activityId") Long activityId,
            @Param("studentNo") String studentNo, @Param("name") String name);

}
