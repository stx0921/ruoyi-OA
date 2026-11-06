package com.ruoyi.oa.volunteer.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.oa.volunteer.dto.ActivityQuery;
import com.ruoyi.oa.volunteer.dto.ActivitySaveDTO;
import com.ruoyi.oa.volunteer.dto.PublicSignupDTO;
import com.ruoyi.oa.volunteer.dto.SignupAuditDTO;
import com.ruoyi.oa.volunteer.dto.SignupQuery;
import com.ruoyi.oa.volunteer.dto.StatQuery;
import com.ruoyi.oa.volunteer.vo.ActivityDetailVO;
import com.ruoyi.oa.volunteer.vo.ActivityListVO;
import com.ruoyi.oa.volunteer.vo.PublicActivityVO;
import com.ruoyi.oa.volunteer.vo.PublicResultVO;
import com.ruoyi.oa.volunteer.vo.SignupExportVO;
import com.ruoyi.oa.volunteer.vo.SignupListVO;
import com.ruoyi.oa.volunteer.vo.StatExportVO;
import com.ruoyi.oa.volunteer.vo.StatMonthlyVO;
import com.ruoyi.oa.volunteer.vo.StatOverviewVO;

/**
 * 志愿活动 Service（活动 / 报名 / 公开 H5 / 统计）
 *
 * @author ruoyi-oa
 */
public interface IVolunteerService
{
    /**
     * 活动列表（showStatus 在 SQL 侧翻译为时间条件）
     */
    List<ActivityListVO> selectActivityList(ActivityQuery query);

    /**
     * 活动详情（含报名统计 + 二维码 base64）
     */
    ActivityDetailVO selectActivityDetail(Long activityId);

    /**
     * 新增活动
     *
     * @return { activityId, activityNo }
     */
    Map<String, Object> insertActivity(ActivitySaveDTO dto);

    /**
     * 修改活动（含取消活动 status=1）
     */
    void updateActivity(ActivitySaveDTO dto);

    /**
     * 批量软删除活动
     */
    int deleteActivityByIds(Long[] activityIds);

    /**
     * 活动二维码：{ content, base64 }
     */
    Map<String, Object> selectActivityQrCode(Long activityId);

    /**
     * 某活动报名名单
     */
    List<SignupListVO> selectSignupList(SignupQuery query);

    /**
     * 单条/批量审核报名（≤200）
     */
    void auditSignup(SignupAuditDTO dto);

    /**
     * 报名名单导出数据
     */
    List<SignupExportVO> selectSignupExportList(SignupQuery query);

    /**
     * H5 免登录活动详情（只暴露必要字段）
     */
    PublicActivityVO selectPublicActivity(Long activityId);

    /**
     * H5 免登录报名
     */
    PublicResultVO publicSignup(PublicSignupDTO dto);

    /**
     * H5 免登录查询报名结果（studentNo + name 必须匹配，activityId 可空）
     */
    PublicResultVO queryPublicResult(Long activityId, String studentNo, String name);

    /**
     * 统计概览
     */
    StatOverviewVO selectStatOverview(StatQuery query);

    /**
     * 按月统计（升序、空月补 0）
     */
    List<StatMonthlyVO> selectStatMonthly(StatQuery query);

    /**
     * 统计导出数据（按月明细，ExcelUtil isStatistics 自动合计）
     */
    List<StatExportVO> selectStatExport(StatQuery query);

}
