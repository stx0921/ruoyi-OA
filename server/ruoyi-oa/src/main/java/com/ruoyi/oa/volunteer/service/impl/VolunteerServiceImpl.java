package com.ruoyi.oa.volunteer.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.pagehelper.Page;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.volunteer.domain.VolunteerActivity;
import com.ruoyi.oa.volunteer.domain.VolunteerSignup;
import com.ruoyi.oa.volunteer.dto.ActivityQuery;
import com.ruoyi.oa.volunteer.dto.ActivitySaveDTO;
import com.ruoyi.oa.volunteer.dto.PublicSignupDTO;
import com.ruoyi.oa.volunteer.dto.SignupAuditDTO;
import com.ruoyi.oa.volunteer.dto.SignupQuery;
import com.ruoyi.oa.volunteer.dto.StatQuery;
import com.ruoyi.oa.volunteer.mapper.VolunteerActivityMapper;
import com.ruoyi.oa.volunteer.mapper.VolunteerSignupMapper;
import com.ruoyi.oa.volunteer.service.IVolunteerService;
import com.ruoyi.oa.volunteer.support.VolunteerNoGenerator;
import com.ruoyi.oa.volunteer.support.VolunteerQrCodeUtil;
import com.ruoyi.oa.volunteer.support.VolunteerShowStatus;
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
 * 志愿活动 Service 实现（活动 / 报名 / 公开 H5 / 统计）
 *
 * @author ruoyi-oa
 */
@Service
public class VolunteerServiceImpl implements IVolunteerService
{
    private static final Logger log = LoggerFactory.getLogger(VolunteerServiceImpl.class);

    /** 删除标志：正常 */
    private static final String DEL_NORMAL = "0";

    /** 活动状态：正常 */
    private static final String STATUS_NORMAL = "0";

    /** 审核状态：待审核 */
    private static final String AUDIT_PENDING = "0";

    /** 审核状态：通过 */
    private static final String AUDIT_PASS = "1";

    /** 审核状态：驳回 */
    private static final String AUDIT_REJECT = "2";

    /** 单次批量审核上限 */
    private static final int MAX_AUDIT_BATCH = 200;

    /** 学号正则 */
    private static final String STUDENT_NO_REGEX = "^\\d{8,12}$";

    /** 志愿时长步进 */
    private static final BigDecimal DURATION_STEP = new BigDecimal("0.5");

    /** 默认统计范围：近 12 个月 */
    private static final int DEFAULT_STAT_MONTHS = 12;

    /** 审核状态标签（与字典 oa_volunteer_audit_status 一致） */
    private static final Map<String, String> AUDIT_STATUS_LABELS = new HashMap<String, String>();

    static
    {
        AUDIT_STATUS_LABELS.put(AUDIT_PENDING, "待审核");
        AUDIT_STATUS_LABELS.put(AUDIT_PASS, "通过");
        AUDIT_STATUS_LABELS.put(AUDIT_REJECT, "驳回");
    }

    @Autowired
    private VolunteerActivityMapper activityMapper;

    @Autowired
    private VolunteerSignupMapper signupMapper;

    @Autowired
    private VolunteerNoGenerator noGenerator;

    @Autowired
    private VolunteerQrCodeUtil qrCodeUtil;

    // ============================ 活动 ============================

    @Override
    public List<ActivityListVO> selectActivityList(ActivityQuery query)
    {
        ActivityQuery condition = query == null ? new ActivityQuery() : query;
        condition.setKeyword(trim(condition.getKeyword()));
        normalizeActivityTimeRange(condition);
        Date now = new Date();
        List<VolunteerActivity> list = activityMapper.selectActivityList(condition);
        List<ActivityListVO> result = new ArrayList<>(list.size());
        for (VolunteerActivity activity : list)
        {
            ActivityListVO vo = new ActivityListVO();
            fillListVO(vo, activity, now);
            result.add(vo);
        }
        return toVoPage(list, result);
    }

    @Override
    public ActivityDetailVO selectActivityDetail(Long activityId)
    {
        VolunteerActivity activity = getExistingActivity(activityId);
        Date now = new Date();
        ActivityDetailVO vo = new ActivityDetailVO();
        fillListVO(vo, activity, now);
        vo.setContent(activity.getContent());
        vo.setPendingCount(nvl(activity.getPendingCount()));
        vo.setRejectCount(nvl(activity.getRejectCount()));
        String content = qrCodeUtil.buildContent(activity.getActivityId());
        vo.setQrcodeContent(content);
        vo.setQrcode(qrCodeUtil.toBase64Png(content));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> insertActivity(ActivitySaveDTO dto)
    {
        if (dto == null)
        {
            throw new ServiceException("提交内容不能为空", 400);
        }
        validateActivity(dto);
        SysUser me = SecurityUtils.getLoginUser().getUser();
        VolunteerActivity activity = buildActivity(dto, false);
        activity.setStatus(STATUS_NORMAL);
        activity.setCreatorId(me.getUserId());
        activity.setCreatorName(nickName(me));
        insertWithRetry(activity);
        Map<String, Object> result = new LinkedHashMap<>(4);
        result.put("activityId", activity.getActivityId());
        result.put("activityNo", activity.getActivityNo());
        log.info("新增志愿活动成功 activityId={} activityNo={}", activity.getActivityId(), activity.getActivityNo());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActivity(ActivitySaveDTO dto)
    {
        if (dto == null || dto.getActivityId() == null)
        {
            throw new ServiceException("活动ID不能为空", 400);
        }
        getExistingActivity(dto.getActivityId());
        validateActivity(dto);
        VolunteerActivity update = buildActivity(dto, true);
        int rows = activityMapper.updateActivity(update);
        if (rows == 0)
        {
            throw new ServiceException("活动不存在或已删除", 404);
        }
        log.info("修改志愿活动成功 activityId={} status={}", dto.getActivityId(), update.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteActivityByIds(Long[] activityIds)
    {
        if (activityIds == null || activityIds.length == 0)
        {
            throw new ServiceException("请选择要删除的活动", 400);
        }
        int rows = activityMapper.deleteActivityByIds(activityIds);
        if (rows == 0)
        {
            throw new ServiceException("活动不存在或已删除", 404);
        }
        log.info("软删除志愿活动 rows={} ids={}", rows, activityIds.length);
        return rows;
    }

    @Override
    public Map<String, Object> selectActivityQrCode(Long activityId)
    {
        getExistingActivity(activityId);
        return qrCodeUtil.generate(activityId);
    }

    // ============================ 报名 ============================

    @Override
    public List<SignupListVO> selectSignupList(SignupQuery query)
    {
        if (query == null || query.getActivityId() == null)
        {
            throw new ServiceException("请选择活动", 400);
        }
        query.setKeyword(trim(query.getKeyword()));
        List<VolunteerSignup> list = signupMapper.selectSignupList(query);
        List<SignupListVO> result = new ArrayList<>(list.size());
        for (VolunteerSignup signup : list)
        {
            SignupListVO vo = new SignupListVO();
            fillSignupVO(vo, signup);
            result.add(vo);
        }
        return toVoPage(list, result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSignup(SignupAuditDTO dto)
    {
        if (dto == null || dto.getIds() == null || dto.getIds().length == 0)
        {
            throw new ServiceException("请选择要审核的报名记录", 400);
        }
        if (dto.getIds().length > MAX_AUDIT_BATCH)
        {
            throw new ServiceException("单次最多审核 " + MAX_AUDIT_BATCH + " 条", 400);
        }
        Integer auditStatus = dto.getAuditStatus();
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2))
        {
            throw new ServiceException("审核状态取值非法", 400);
        }
        String remark = trim(dto.getAuditRemark());
        if (auditStatus == 2 && remark.length() > 200)
        {
            throw new ServiceException("驳回原因最多 200 字", 400);
        }
        String statusValue = String.valueOf(auditStatus);
        SysUser me = SecurityUtils.getLoginUser().getUser();
        int rows = signupMapper.auditSignup(dto.getIds(), statusValue,
                auditStatus == 2 ? remark : null, me.getUserId(), nickName(me));
        if (rows == 0)
        {
            throw new ServiceException("报名记录不存在或活动已删除", 404);
        }
        log.info("审核志愿报名 rows={} auditStatus={}", rows, statusValue);
    }

    @Override
    public List<SignupExportVO> selectSignupExportList(SignupQuery query)
    {
        if (query == null || query.getActivityId() == null)
        {
            throw new ServiceException("请选择要导出的活动", 400);
        }
        query.setKeyword(trim(query.getKeyword()));
        List<VolunteerSignup> list = signupMapper.selectSignupList(query);
        List<SignupExportVO> result = new ArrayList<>(list.size());
        int index = 1;
        for (VolunteerSignup signup : list)
        {
            SignupExportVO vo = new SignupExportVO();
            vo.setIndex(index++);
            vo.setName(safeExcel(signup.getName()));
            vo.setStudentNo(safeExcel(signup.getStudentNo()));
            vo.setCollege(safeExcel(signup.getCollege()));
            vo.setMajor(safeExcel(signup.getMajor()));
            vo.setSignupTime(signup.getSignupTime());
            vo.setAuditStatusLabel(AUDIT_STATUS_LABELS.get(signup.getAuditStatus()));
            vo.setAuditRemark(safeExcel(signup.getAuditRemark()));
            vo.setDurationHours(signup.getDurationHours());
            result.add(vo);
        }
        return result;
    }

    // ============================ H5 公开接口 ============================

    @Override
    public PublicActivityVO selectPublicActivity(Long activityId)
    {
        VolunteerActivity activity = getExistingActivity(activityId);
        Date now = new Date();
        PublicActivityVO vo = new PublicActivityVO();
        vo.setActivityId(activity.getActivityId());
        vo.setName(activity.getName());
        vo.setLocation(activity.getLocation());
        vo.setCapacity(activity.getCapacity());
        vo.setDurationHours(activity.getDurationHours());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setSignupDeadline(activity.getSignupDeadline());
        vo.setContent(activity.getContent());
        VolunteerShowStatus showStatus = VolunteerShowStatus.resolve(activity.getStatus(),
                activity.getStartTime(), activity.getEndTime(), activity.getSignupDeadline(), now);
        vo.setShowStatus(showStatus.getCode());
        vo.setShowStatusLabel(showStatus.getLabel());
        vo.setSignupCount(nvl(activity.getSignupCount()));
        boolean canSignup = VolunteerShowStatus.canSignup(activity.getStatus(), activity.getEndTime(),
                activity.getSignupDeadline(), now);
        vo.setCanSignup(canSignup);
        vo.setClosedReason(canSignup ? "" : VolunteerShowStatus.closedReason(activity.getStatus(),
                activity.getEndTime(), activity.getSignupDeadline(), now));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublicResultVO publicSignup(PublicSignupDTO dto)
    {
        if (dto == null || dto.getActivityId() == null)
        {
            throw new ServiceException("请选择活动", 400);
        }
        String name = trim(dto.getName());
        if (name.length() < 2 || name.length() > 20)
        {
            throw new ServiceException("姓名需为 2–20 个字符", 400);
        }
        String studentNo = trim(dto.getStudentNo());
        if (StringUtils.isEmpty(studentNo) || !studentNo.matches(STUDENT_NO_REGEX))
        {
            throw new ServiceException("学号需为 8–12 位数字", 400);
        }
        String college = trim(dto.getCollege());
        if (college.length() < 2 || college.length() > 50)
        {
            throw new ServiceException("学院需为 2–50 个字符", 400);
        }
        String major = trim(dto.getMajor());
        if (major.length() < 2 || major.length() > 50)
        {
            throw new ServiceException("专业需为 2–50 个字符", 400);
        }
        VolunteerActivity activity = getExistingActivity(dto.getActivityId());
        Date now = new Date();
        if (!VolunteerShowStatus.canSignup(activity.getStatus(), activity.getEndTime(),
                activity.getSignupDeadline(), now))
        {
            throw new ServiceException(VolunteerShowStatus.closedReason(activity.getStatus(),
                    activity.getEndTime(), activity.getSignupDeadline(), now) + "，无法报名", 400);
        }
        VolunteerSignup signup = new VolunteerSignup();
        signup.setActivityId(activity.getActivityId());
        signup.setName(name);
        signup.setStudentNo(studentNo);
        signup.setCollege(college);
        signup.setMajor(major);
        signup.setAuditStatus(AUDIT_PENDING);
        try
        {
            signupMapper.insertSignup(signup);
        }
        catch (DuplicateKeyException e)
        {
            // 唯一索引 uk_activity_student 兜底并发重复报名
            throw new ServiceException("该学号已报名本活动，请勿重复提交", 409);
        }
        log.info("H5 报名成功 activityId={} studentNo={}", activity.getActivityId(), studentNo);
        PublicResultVO vo = new PublicResultVO();
        vo.setActivityName(activity.getName());
        vo.setDurationHours(activity.getDurationHours());
        vo.setAuditStatus(AUDIT_PENDING);
        return vo;
    }

    @Override
    public PublicResultVO queryPublicResult(Long activityId, String studentNo, String name)
    {
        String no = trim(studentNo);
        String realName = trim(name);
        if (StringUtils.isEmpty(no) || StringUtils.isEmpty(realName))
        {
            throw new ServiceException("未查询到报名记录，请核对学号与姓名", 404);
        }
        VolunteerSignup signup = signupMapper.selectSignupForResult(activityId, no, realName);
        if (signup == null)
        {
            throw new ServiceException("未查询到报名记录，请核对学号与姓名", 404);
        }
        PublicResultVO vo = new PublicResultVO();
        vo.setActivityName(signup.getActivityName());
        vo.setDurationHours(signup.getDurationHours());
        vo.setAuditStatus(signup.getAuditStatus());
        vo.setAuditRemark(signup.getAuditRemark());
        vo.setAuditTime(signup.getAuditTime());
        return vo;
    }

    // ============================ 统计 ============================

    @Override
    public StatOverviewVO selectStatOverview(StatQuery query)
    {
        StatRange range = resolveStatRange(query);
        StatOverviewVO vo = activityMapper.selectStatOverview(range.begin, range.end);
        if (vo == null)
        {
            vo = new StatOverviewVO();
        }
        if (vo.getTotalHours() == null)
        {
            vo.setTotalHours(BigDecimal.ZERO);
        }
        if (vo.getActivityCount() == null)
        {
            vo.setActivityCount(0L);
        }
        if (vo.getParticipantCount() == null)
        {
            vo.setParticipantCount(0L);
        }
        if (vo.getAvgHours() == null)
        {
            vo.setAvgHours(BigDecimal.ZERO);
        }
        return vo;
    }

    @Override
    public List<StatMonthlyVO> selectStatMonthly(StatQuery query)
    {
        StatRange range = resolveStatRange(query);
        List<StatMonthlyVO> rows = activityMapper.selectStatMonthly(range.begin, range.end);
        Map<String, StatMonthlyVO> dataMap = new LinkedHashMap<>();
        if (rows != null)
        {
            for (StatMonthlyVO row : rows)
            {
                if (StringUtils.isEmpty(row.getMonth()))
                {
                    continue;
                }
                if (row.getActivityCount() == null)
                {
                    row.setActivityCount(0L);
                }
                if (row.getPassCount() == null)
                {
                    row.setPassCount(0L);
                }
                if (row.getTotalHours() == null)
                {
                    row.setTotalHours(BigDecimal.ZERO);
                }
                dataMap.put(row.getMonth(), row);
            }
        }
        List<StatMonthlyVO> result = new ArrayList<>();
        YearMonth cursor = range.beginMonth;
        while (!cursor.isAfter(range.endMonth))
        {
            String month = cursor.toString();
            StatMonthlyVO vo = dataMap.get(month);
            if (vo == null)
            {
                vo = new StatMonthlyVO();
                vo.setMonth(month);
                vo.setActivityCount(0L);
                vo.setPassCount(0L);
                vo.setTotalHours(BigDecimal.ZERO);
            }
            result.add(vo);
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    @Override
    public List<StatExportVO> selectStatExport(StatQuery query)
    {
        List<StatMonthlyVO> monthly = selectStatMonthly(query);
        List<StatExportVO> result = new ArrayList<>(monthly.size());
        for (StatMonthlyVO item : monthly)
        {
            StatExportVO vo = new StatExportVO();
            vo.setMonth(item.getMonth());
            vo.setActivityCount(item.getActivityCount());
            vo.setPassCount(item.getPassCount());
            vo.setTotalHours(item.getTotalHours());
            result.add(vo);
        }
        return result;
    }

    // ============================ 内部方法 ============================

    /**
     * 查询未删除活动；不存在/已删除抛 404
     */
    private VolunteerActivity getExistingActivity(Long activityId)
    {
        if (activityId == null)
        {
            throw new ServiceException("活动不存在或已删除", 404);
        }
        VolunteerActivity activity = activityMapper.selectActivityById(activityId);
        if (activity == null || !DEL_NORMAL.equals(activity.getDelFlag()))
        {
            throw new ServiceException("活动不存在或已删除", 404);
        }
        return activity;
    }

    /**
     * 活动字段校验（逐项 400）
     */
    private void validateActivity(ActivitySaveDTO dto)
    {
        String name = trim(dto.getName());
        if (name.length() < 2 || name.length() > 100)
        {
            throw new ServiceException("活动名称需为 2–100 个字符", 400);
        }
        String location = trim(dto.getLocation());
        if (location.length() < 2 || location.length() > 100)
        {
            throw new ServiceException("活动地点需为 2–100 个字符", 400);
        }
        Integer capacity = dto.getCapacity();
        if (capacity == null || capacity < 1 || capacity > 9999)
        {
            throw new ServiceException("活动人数需为 1–9999 的整数", 400);
        }
        BigDecimal duration = dto.getDurationHours();
        if (duration == null)
        {
            throw new ServiceException("请填写志愿时长", 400);
        }
        if (duration.compareTo(new BigDecimal("0.5")) < 0 || duration.compareTo(new BigDecimal("99.9")) > 0)
        {
            throw new ServiceException("志愿时长需为 0.5–99.9 小时", 400);
        }
        if (duration.remainder(DURATION_STEP).compareTo(BigDecimal.ZERO) != 0
                && duration.compareTo(new BigDecimal("99.9")) != 0)
        {
            throw new ServiceException("志愿时长需以 0.5 小时为步进", 400);
        }
        if (dto.getStartTime() == null)
        {
            throw new ServiceException("请选择活动开始时间", 400);
        }
        if (dto.getEndTime() == null)
        {
            throw new ServiceException("请选择活动结束时间", 400);
        }
        if (dto.getSignupDeadline() == null)
        {
            throw new ServiceException("请选择报名截止时间", 400);
        }
        if (!dto.getEndTime().after(dto.getStartTime()))
        {
            throw new ServiceException("活动结束时间必须晚于开始时间", 400);
        }
        if (dto.getSignupDeadline().after(dto.getStartTime()))
        {
            throw new ServiceException("报名截止时间不能晚于活动开始时间", 400);
        }
        String content = trim(dto.getContent());
        if (content.length() < 10 || content.length() > 5000)
        {
            throw new ServiceException("活动详情需为 10–5000 个字符", 400);
        }
        String status = trim(dto.getStatus());
        if (StringUtils.isNotEmpty(status) && !STATUS_NORMAL.equals(status) && !"1".equals(status))
        {
            throw new ServiceException("状态取值非法", 400);
        }
    }

    /**
     * DTO → 活动对象（新增/编辑共用；编辑时 status 为空表示不变）
     */
    private VolunteerActivity buildActivity(ActivitySaveDTO dto, boolean update)
    {
        VolunteerActivity activity = new VolunteerActivity();
        activity.setActivityId(dto.getActivityId());
        activity.setName(trim(dto.getName()));
        activity.setLocation(trim(dto.getLocation()));
        activity.setCapacity(dto.getCapacity());
        if (dto.getDurationHours() != null)
        {
            activity.setDurationHours(dto.getDurationHours().setScale(1, RoundingMode.HALF_UP));
        }
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setSignupDeadline(dto.getSignupDeadline());
        activity.setContent(trim(dto.getContent()));
        if (update)
        {
            String status = trim(dto.getStatus());
            activity.setStatus(StringUtils.isEmpty(status) ? null : status);
        }
        else
        {
            activity.setStatus(STATUS_NORMAL);
        }
        return activity;
    }

    /**
     * 活动列表/详情字段装配（展示状态按统一规则推导）
     */
    private void fillListVO(ActivityListVO vo, VolunteerActivity activity, Date now)
    {
        vo.setActivityId(activity.getActivityId());
        vo.setActivityNo(activity.getActivityNo());
        vo.setName(activity.getName());
        vo.setLocation(activity.getLocation());
        vo.setCapacity(activity.getCapacity());
        vo.setDurationHours(activity.getDurationHours());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setSignupDeadline(activity.getSignupDeadline());
        vo.setStatus(activity.getStatus());
        vo.setCreatorName(activity.getCreatorName());
        vo.setCreateTime(activity.getCreateTime());
        vo.setSignupCount(nvl(activity.getSignupCount()));
        vo.setPassCount(nvl(activity.getPassCount()));
        VolunteerShowStatus showStatus = VolunteerShowStatus.resolve(activity.getStatus(),
                activity.getStartTime(), activity.getEndTime(), activity.getSignupDeadline(), now);
        vo.setShowStatus(showStatus.getCode());
        vo.setShowStatusLabel(showStatus.getLabel());
    }

    /**
     * 报名列表字段装配
     */
    private void fillSignupVO(SignupListVO vo, VolunteerSignup signup)
    {
        vo.setSignupId(signup.getSignupId());
        vo.setActivityId(signup.getActivityId());
        vo.setActivityName(signup.getActivityName());
        vo.setName(signup.getName());
        vo.setStudentNo(signup.getStudentNo());
        vo.setCollege(signup.getCollege());
        vo.setMajor(signup.getMajor());
        vo.setSignupTime(signup.getSignupTime());
        vo.setAuditStatus(signup.getAuditStatus());
        vo.setAuditStatusLabel(AUDIT_STATUS_LABELS.get(signup.getAuditStatus()));
        vo.setAuditRemark(signup.getAuditRemark());
        vo.setAuditorName(signup.getAuditorName());
        vo.setAuditTime(signup.getAuditTime());
        vo.setDurationHours(signup.getDurationHours());
    }

    /**
     * 新增活动；活动编号唯一索引冲突重试 ≤3 次
     */
    private void insertWithRetry(VolunteerActivity activity)
    {
        for (int attempt = 0; attempt < 3; attempt++)
        {
            activity.setActivityNo(noGenerator.next());
            try
            {
                activityMapper.insertActivity(activity);
                return;
            }
            catch (DuplicateKeyException e)
            {
                log.warn("活动编号冲突，重试第 {} 次 activityNo={}", attempt + 1, activity.getActivityNo());
            }
        }
        throw new ServiceException("活动编号生成冲突，请稍后重试", 409);
    }

    /**
     * 转换 VO 列表时保留 PageHelper 分页信息（避免 getDataTable total 退化为当前页大小）
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <S, T> List<T> toVoPage(List<S> source, List<T> target)
    {
        if (source instanceof Page)
        {
            Page<S> sourcePage = (Page<S>) source;
            Page<T> voPage = new Page<>(sourcePage.getPageNum(), sourcePage.getPageSize());
            voPage.setTotal(sourcePage.getTotal());
            voPage.addAll(target);
            return voPage;
        }
        return target;
    }

    /**
     * 活动列表时间范围规范化：yyyy-MM-dd → 当天 00:00:00 / 23:59:59（闭区间）
     */
    private void normalizeActivityTimeRange(ActivityQuery query)
    {
        LocalDate beginDay = parseOptionalDate(query.getBeginTime());
        LocalDate endDay = parseOptionalDate(query.getEndTime());
        if (beginDay != null && endDay != null && beginDay.isAfter(endDay))
        {
            throw new ServiceException("开始时间不能晚于结束时间", 400);
        }
        query.setBeginTime(beginDay == null ? null : beginDay + " 00:00:00");
        query.setEndTime(endDay == null ? null : endDay + " 23:59:59");
    }

    /**
     * 统计时间范围解析：缺省近 12 个月；补日界（begin 00:00:00 / end 23:59:59）
     */
    private StatRange resolveStatRange(StatQuery query)
    {
        LocalDate beginDay = parseOptionalDate(query == null ? null : query.getBeginTime());
        LocalDate endDay = parseOptionalDate(query == null ? null : query.getEndTime());
        LocalDate today = LocalDate.now();
        if (beginDay == null && endDay == null)
        {
            endDay = today;
            beginDay = YearMonth.from(today).minusMonths(DEFAULT_STAT_MONTHS - 1).atDay(1);
        }
        else if (beginDay == null)
        {
            beginDay = YearMonth.from(endDay).minusMonths(DEFAULT_STAT_MONTHS - 1).atDay(1);
        }
        else if (endDay == null)
        {
            endDay = today;
        }
        if (beginDay.isAfter(endDay))
        {
            throw new ServiceException("开始时间不能晚于结束时间", 400);
        }
        StatRange range = new StatRange();
        range.begin = toDate(beginDay.atStartOfDay());
        range.end = toDate(endDay.atTime(23, 59, 59));
        range.beginMonth = YearMonth.from(beginDay);
        range.endMonth = YearMonth.from(endDay);
        return range;
    }

    private LocalDate parseOptionalDate(String value)
    {
        String text = trim(value);
        if (StringUtils.isEmpty(text))
        {
            return null;
        }
        if (text.length() > 10)
        {
            text = text.substring(0, 10);
        }
        try
        {
            return LocalDate.parse(text);
        }
        catch (Exception e)
        {
            throw new ServiceException("时间格式应为 yyyy-MM-dd", 400);
        }
    }

    private Date toDate(LocalDateTime localDateTime)
    {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private String nickName(SysUser user)
    {
        if (user == null)
        {
            return "";
        }
        return StringUtils.isNotEmpty(user.getNickName()) ? user.getNickName() : user.getUserName();
    }

    private Long nvl(Long value)
    {
        return value == null ? 0L : value;
    }

    /**
     * 导出字符串防 Excel 公式注入：以 = + - @ 开头时前置单引号
     */
    private String safeExcel(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@')
        {
            return "'" + value;
        }
        return value;
    }

    private String trim(String value)
    {
        return value == null ? "" : value.trim();
    }

    /**
     * 统计范围（含月份区间，供空月补 0）
     */
    private static class StatRange
    {
        private Date begin;

        private Date end;

        private YearMonth beginMonth;

        private YearMonth endMonth;
    }

}
