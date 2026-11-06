package com.ruoyi.oa.volunteer.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.volunteer.domain.VolunteerActivity;
import com.ruoyi.oa.volunteer.dto.ActivityQuery;
import com.ruoyi.oa.volunteer.vo.StatMonthlyVO;
import com.ruoyi.oa.volunteer.vo.StatOverviewVO;

/**
 * 志愿活动 Mapper
 *
 * @author ruoyi-oa
 */
public interface VolunteerActivityMapper
{
    /**
     * 活动列表（仅 del_flag='0'；showStatus 在 SQL 侧翻译为时间条件）
     */
    List<VolunteerActivity> selectActivityList(ActivityQuery query);

    /**
     * 按主键查询活动（不过滤 del_flag，服务层判断 404）
     */
    VolunteerActivity selectActivityById(@Param("activityId") Long activityId);

    /**
     * 新增活动
     */
    int insertActivity(VolunteerActivity activity);

    /**
     * 修改活动（仅未删除）
     */
    int updateActivity(VolunteerActivity activity);

    /**
     * 批量软删除（del_flag='1'，仅未删除数据）
     */
    int deleteActivityByIds(@Param("ids") Long[] ids);

    /**
     * 指定日期前缀下最大活动序号（编号生成 DB 兜底）
     */
    Long selectMaxActivityNoSeq(@Param("datePart") String datePart);

    /**
     * 统计概览（totalHours/participantCount 按区间内活动；activityCount/avgHours 仅计已开始）
     */
    StatOverviewVO selectStatOverview(@Param("beginTime") Date beginTime, @Param("endTime") Date endTime);

    /**
     * 按月统计（按活动开始时间归月）
     */
    List<StatMonthlyVO> selectStatMonthly(@Param("beginTime") Date beginTime, @Param("endTime") Date endTime);

}
