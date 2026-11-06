package com.ruoyi.oa.recur.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.pagehelper.Page;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.member.domain.OaMember;
import com.ruoyi.oa.member.mapper.OaMemberMapper;
import com.ruoyi.oa.notify.service.IOaNotifyService;
import com.ruoyi.oa.project.domain.OaProject;
import com.ruoyi.oa.project.mapper.OaProjectMapper;
import com.ruoyi.oa.recur.domain.OaTaskRecur;
import com.ruoyi.oa.recur.dto.RecurQuery;
import com.ruoyi.oa.recur.dto.RecurSaveDTO;
import com.ruoyi.oa.recur.dto.RecurStatusDTO;
import com.ruoyi.oa.recur.mapper.OaTaskRecurMapper;
import com.ruoyi.oa.recur.service.IOaTaskRecurService;
import com.ruoyi.oa.recur.support.RecurDateCalculator;
import com.ruoyi.oa.recur.vo.RecurListVO;
import com.ruoyi.oa.recur.vo.RecurSaveResultVO;
import com.ruoyi.oa.support.OaCurrentUser;
import com.ruoyi.oa.support.OaDictLabelProvider;
import com.ruoyi.oa.task.domain.OaTask;
import com.ruoyi.oa.task.mapper.OaTaskLogMapper;
import com.ruoyi.oa.task.mapper.OaTaskMapper;

/**
 * 周期任务规则 Service 实现
 *
 * <p>week_days 保存规范：去重升序、无空格（如 1,3,5）；next_date 为派生字段，
 * 口径为「今天之后且不早于生效日的第一个命中日」；暂停/结束后为空。</p>
 *
 * @author ruoyi-oa
 */
@Service
public class OaTaskRecurServiceImpl implements IOaTaskRecurService
{
    private static final Logger log = LoggerFactory.getLogger(OaTaskRecurServiceImpl.class);

    /** 规则状态文案 */
    private static final Map<String, String> STATUS_LABELS = new HashMap<String, String>();

    /** 优先级文案 */
    private static final Map<String, String> PRIORITY_LABELS = new HashMap<String, String>();

    static
    {
        STATUS_LABELS.put("0", "启用");
        STATUS_LABELS.put("1", "暂停");
        STATUS_LABELS.put("2", "已结束");

        PRIORITY_LABELS.put("0", "高");
        PRIORITY_LABELS.put("1", "中");
        PRIORITY_LABELS.put("2", "低");
    }

    @Autowired
    private OaTaskRecurMapper recurMapper;

    @Autowired
    private OaTaskMapper taskMapper;

    @Autowired
    private OaTaskLogMapper taskLogMapper;

    @Autowired
    private OaMemberMapper memberMapper;

    @Autowired
    private OaProjectMapper projectMapper;

    @Autowired
    private OaCurrentUser currentUser;

    @Autowired
    private OaDictLabelProvider dictLabels;

    @Autowired
    private IOaNotifyService notifyService;

    @Override
    @DataScope(deptAlias = "r")
    public List<RecurListVO> selectRecurList(RecurQuery query)
    {
        RecurQuery condition = query == null ? new RecurQuery() : query;
        List<OaTaskRecur> list = recurMapper.selectRecurList(condition);
        List<RecurListVO> result = new ArrayList<RecurListVO>(list.size());
        Map<String, String> statusLabels = dictLabels.labels("oa_recur_status", STATUS_LABELS);
        Map<String, String> priorityLabels = dictLabels.labels("oa_task_priority", PRIORITY_LABELS);
        for (OaTaskRecur recur : list)
        {
            RecurListVO vo = new RecurListVO();
            fillListVO(vo, recur, statusLabels, priorityLabels);
            result.add(vo);
        }
        // 保留 PageHelper 分页信息（转换成 VO 后 getDataTable 仍能取到 total）
        if (list instanceof Page)
        {
            Page<OaTaskRecur> sourcePage = (Page<OaTaskRecur>) list;
            Page<RecurListVO> voPage = new Page<RecurListVO>(sourcePage.getPageNum(), sourcePage.getPageSize());
            voPage.setTotal(sourcePage.getTotal());
            voPage.addAll(result);
            return voPage;
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecurSaveResultVO insertRecur(RecurSaveDTO dto)
    {
        if (dto == null)
        {
            throw new ServiceException("请填写规则信息", 400);
        }
        String title = validateTitle(dto.getTitle());
        String description = validateDescription(dto.getDescription());
        List<Integer> weekDays = validateWeekDays(dto.getWeekDays());
        String dueTime = validateDueTime(dto.getDueTime());
        String priority = validatePriority(dto.getPriority());
        String tags = validateTags(dto.getTags());
        Date startDate = validateCreateStartDate(dto.getStartDate());
        validateProject(dto.getProjectId());
        validateAssignee(dto.getAssigneeId());

        SysUser me = currentUser.user();
        LocalDate today = RecurDateCalculator.today();
        OaTaskRecur recur = new OaTaskRecur();
        recur.setTitle(title);
        recur.setDescription(description);
        recur.setProjectId(dto.getProjectId());
        recur.setWeekDays(RecurDateCalculator.joinWeekDays(weekDays));
        recur.setDueTime(dueTime);
        recur.setAssigneeId(dto.getAssigneeId());
        recur.setPriority(priority);
        recur.setTags(tags);
        recur.setStartDate(startDate);
        recur.setNextDate(RecurDateCalculator.toDate(
                RecurDateCalculator.calcNextDate(startDate, recur.getWeekDays(), today)));
        recur.setCreatorId(me.getUserId());
        recur.setDeptId(me.getDeptId());
        recur.setCreateBy(currentUser.username());
        recurMapper.insertRecur(recur);

        RecurSaveResultVO result = new RecurSaveResultVO();
        result.setRecurId(recur.getRecurId());
        result.setNextDate(recur.getNextDate());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecur(RecurSaveDTO dto)
    {
        if (dto == null || dto.getRecurId() == null)
        {
            throw new ServiceException("规则不存在", 404);
        }
        OaTaskRecur db = recurMapper.selectByIdForUpdate(dto.getRecurId());
        if (db == null)
        {
            throw new ServiceException("规则不存在", 404);
        }
        if ("2".equals(db.getStatus()))
        {
            throw new ServiceException("已结束规则不可修改", 400);
        }
        String applyScope = StringUtils.isEmpty(dto.getApplyScope()) ? "all" : dto.getApplyScope();
        if (!"one".equals(applyScope) && !"all".equals(applyScope))
        {
            throw new ServiceException("修改范围取值非法", 400);
        }
        String title = validateTitle(dto.getTitle());
        String description = validateDescription(dto.getDescription());
        List<Integer> weekDays = validateWeekDays(dto.getWeekDays());
        String dueTime = validateDueTime(dto.getDueTime());
        String priority = validatePriority(dto.getPriority());
        String tags = validateTags(dto.getTags());
        Date startDate = validateEditStartDate(dto.getStartDate(), db);
        validateProject(dto.getProjectId());
        validateAssignee(dto.getAssigneeId());

        if ("one".equals(applyScope))
        {
            updateOneInstance(dto, db, title, description, dueTime, priority, tags);
            log.info("周期规则[{}]按「仅本次」修改最近实例", db.getRecurId());
            return;
        }

        // all：改模板，历史任务不动；next_date 按新模板重算
        String weekDaysText = RecurDateCalculator.joinWeekDays(weekDays);
        OaTaskRecur update = new OaTaskRecur();
        update.setRecurId(db.getRecurId());
        update.setTitle(title);
        update.setDescription(description);
        update.setProjectId(dto.getProjectId());
        update.setWeekDays(weekDaysText);
        update.setDueTime(dueTime);
        update.setAssigneeId(dto.getAssigneeId());
        update.setPriority(priority);
        update.setTags(tags);
        update.setStartDate(startDate);
        if ("0".equals(db.getStatus()))
        {
            update.setNextDate(RecurDateCalculator.toDate(
                    RecurDateCalculator.calcNextDate(startDate, weekDaysText, RecurDateCalculator.today())));
        }
        update.setUpdateBy(currentUser.username());
        int rows = recurMapper.updateRecur(update);
        if (rows == 0)
        {
            throw new ServiceException("规则当前状态不允许该操作", 409);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(RecurStatusDTO dto)
    {
        if (dto == null || dto.getRecurId() == null || StringUtils.isEmpty(dto.getStatus()))
        {
            throw new ServiceException("请选择要变更的状态", 400);
        }
        String target = dto.getStatus();
        if (!"0".equals(target) && !"1".equals(target) && !"2".equals(target))
        {
            throw new ServiceException("规则状态取值非法", 400);
        }
        OaTaskRecur db = recurMapper.selectByIdForUpdate(dto.getRecurId());
        if (db == null)
        {
            throw new ServiceException("规则不存在", 404);
        }
        if ("2".equals(db.getStatus()))
        {
            throw new ServiceException("已结束规则不可再启用", 400);
        }
        Date nextDate = null;
        if ("0".equals(target))
        {
            // 恢复：重算下次生成日
            nextDate = RecurDateCalculator.toDate(RecurDateCalculator.calcNextDate(db.getStartDate(),
                    db.getWeekDays(), RecurDateCalculator.today()));
        }
        int rows = recurMapper.updateStatus(db.getRecurId(), target, nextDate);
        if (rows == 0)
        {
            throw new ServiceException("规则当前状态不允许该操作", 409);
        }
        log.info("周期规则[{}]状态 {} -> {}", db.getRecurId(), db.getStatus(), target);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecurs(Long[] recurIds)
    {
        if (recurIds == null || recurIds.length == 0)
        {
            throw new ServiceException("请选择要删除的周期规则", 400);
        }
        // 1) 权限与存在性校验（不存在/已删除的 ID 跳过，幂等）
        for (Long recurId : recurIds)
        {
            if (recurId == null)
            {
                continue;
            }
            OaTaskRecur db = recurMapper.selectById(recurId);
            if (db == null)
            {
                continue;
            }
            checkDeletePermission(db);
        }
        // 2) 已生成任务保留：先把 oa_task.recur_id 置空，再删规则
        taskMapper.clearRecurIdByRecurIds(recurIds);
        recurMapper.deleteByIds(recurIds);
        // 3) 清理规则相关站内信（避免已删除规则的通知残留并跳转 404）
        for (Long recurId : recurIds)
        {
            if (recurId != null)
            {
                notifyService.deleteByBiz("recur", recurId);
            }
        }
        log.info("批量删除周期规则完成：requestIds={}", recurIds.length);
    }

    // ============================ 内部方法 ============================

    /**
     * 删除规则权限：管理员/主席团任意；部长仅本部门规则
     */
    private void checkDeletePermission(OaTaskRecur recur)
    {
        SysUser me = currentUser.user();
        if (me == null)
        {
            throw new ServiceException("登录已过期，请重新登录", 401);
        }
        if (me.isAdmin() || currentUser.hasRole(OaMemberConstants.ROLE_PRESIDENT))
        {
            return;
        }
        boolean minister = currentUser.hasRole(OaMemberConstants.ROLE_MINISTER);
        if (minister && me.getDeptId() != null && me.getDeptId().equals(recur.getDeptId()))
        {
            return;
        }
        throw new ServiceException("没有权限删除该周期规则", 403);
    }

    /**
     * 「仅本次」：只改最近一次已生成实例的标题/描述/截止时刻/优先级/标签，模板与历史实例不动
     */
    private void updateOneInstance(RecurSaveDTO dto, OaTaskRecur db, String title, String description, String dueTime,
            String priority, String tags)
    {
        OaTask target = null;
        if (dto.getTargetTaskId() != null)
        {
            target = taskMapper.selectById(dto.getTargetTaskId());
            if (target == null || target.getRecurId() == null || !target.getRecurId().equals(db.getRecurId()))
            {
                target = null;
            }
        }
        if (target == null)
        {
            List<OaTask> latest = taskMapper.selectByRecurId(db.getRecurId(), 1);
            if (latest != null && !latest.isEmpty())
            {
                target = latest.get(0);
            }
        }
        if (target == null)
        {
            throw new ServiceException("尚未生成任务实例，无法仅改本次", 400);
        }
        LocalDate baseDate = RecurDateCalculator.toLocalDate(target.getGenerateDate());
        if (baseDate == null)
        {
            baseDate = RecurDateCalculator.toLocalDate(target.getDueTime());
        }
        if (baseDate == null)
        {
            baseDate = RecurDateCalculator.today();
        }
        String oldTitle = target.getTitle();
        target.setTitle(RecurDateCalculator.renderTitle(title, baseDate));
        target.setDescription(description);
        target.setDueTime(RecurDateCalculator.toDateTime(baseDate, dueTime));
        target.setPriority(priority);
        target.setTags(tags);
        target.setUpdateBy(currentUser.username());
        int rows = taskMapper.updateTask(target);
        if (rows == 0)
        {
            throw new ServiceException("任务实例已变化，请刷新后重试", 409);
        }
        writeTaskLog(target.getTaskId(), "编辑",
                "周期规则「仅本次」修改：标题「" + oldTitle + "」→「" + target.getTitle() + "」");
    }

    /**
     * 写任务流转日志（失败不影响主流程）
     */
    private void writeTaskLog(Long taskId, String action, String remark)
    {
        try
        {
            taskLogMapper.insertLog(taskId, action, currentUser.userId(), currentUser.nickName(), remark, null, null);
        }
        catch (Exception e)
        {
            log.error("写任务日志失败 taskId={} action={}", taskId, action, e);
        }
    }

    /**
     * 名称校验：2–200 字，仅允许 {date}/{yyyy}/{MM}/{dd} 占位；
     * 兼容历史/演示数据的 {MM-DD} 写法，保存前归一为 {date}
     */
    private String validateTitle(String title)
    {
        String value = StringUtils.trim(title);
        if (StringUtils.isEmpty(value))
        {
            throw new ServiceException("请填写规则名称", 400);
        }
        value = RecurDateCalculator.normalizeTitleTemplate(value);
        if (value.length() < 2 || value.length() > 200)
        {
            throw new ServiceException("规则名称需为 2–200 个字符", 400);
        }
        String invalid = RecurDateCalculator.invalidPlaceholder(value);
        if (invalid != null)
        {
            throw new ServiceException("仅支持 {date}/{yyyy}/{MM}/{dd} 占位，生成时自动替换（非法占位：" + invalid + "）", 400);
        }
        return value;
    }

    private String validateDescription(String description)
    {
        String value = StringUtils.trim(description);
        if (value == null)
        {
            return "";
        }
        if (value.length() > 2000)
        {
            throw new ServiceException("描述最多 2000 字", 400);
        }
        return value;
    }

    /**
     * 每周几校验：至少一天、取值 1–7，服务端去重升序
     */
    private List<Integer> validateWeekDays(List<Integer> weekDays)
    {
        if (weekDays == null || weekDays.isEmpty())
        {
            throw new ServiceException("请至少选择一天", 400);
        }
        for (Integer day : weekDays)
        {
            if (day == null || day < 1 || day > 7)
            {
                throw new ServiceException("weekDays 只能包含 1-7", 400);
            }
        }
        String normalized = RecurDateCalculator.joinWeekDays(weekDays);
        if (StringUtils.isEmpty(normalized))
        {
            throw new ServiceException("请至少选择一天", 400);
        }
        return RecurDateCalculator.weekDayList(normalized);
    }

    /**
     * 截止时刻校验：必填，HH:mm / HH:mm:ss，归一化为 HH:mm:ss
     */
    private String validateDueTime(String dueTime)
    {
        if (StringUtils.isEmpty(dueTime))
        {
            throw new ServiceException("请选择截止时刻", 400);
        }
        String normalized = RecurDateCalculator.normalizeTime(dueTime);
        if (normalized == null)
        {
            throw new ServiceException("截止时刻格式应为 HH:mm", 400);
        }
        return normalized;
    }

    private String validatePriority(String priority)
    {
        if (StringUtils.isEmpty(priority))
        {
            return "1";
        }
        if (!dictLabels.labels("oa_task_priority", PRIORITY_LABELS).containsKey(priority))
        {
            throw new ServiceException("请选择优先级", 400);
        }
        return priority;
    }

    /**
     * 标签归一化：去空、逗号拼接，长度 ≤200
     */
    private String validateTags(List<String> tags)
    {
        StringBuilder sb = new StringBuilder();
        if (tags != null)
        {
            for (String tag : tags)
            {
                String value = StringUtils.trim(tag);
                if (StringUtils.isEmpty(value))
                {
                    continue;
                }
                if (sb.length() > 0)
                {
                    sb.append(',');
                }
                sb.append(value);
            }
        }
        String result = sb.toString();
        if (result.length() > 200)
        {
            throw new ServiceException("标签最多 200 字", 400);
        }
        return result;
    }

    /**
     * 新建：生效日必填且不早于今天
     */
    private Date validateCreateStartDate(Date startDate)
    {
        if (startDate == null)
        {
            throw new ServiceException("请选择生效日期", 400);
        }
        LocalDate date = RecurDateCalculator.toLocalDate(startDate);
        if (date == null || date.isBefore(RecurDateCalculator.today()))
        {
            throw new ServiceException("生效日期不能早于今天", 400);
        }
        return startDate;
    }

    /**
     * 编辑：生效日可保持原值；如修改为新日期则不得早于今天
     */
    private Date validateEditStartDate(Date startDate, OaTaskRecur db)
    {
        if (startDate == null)
        {
            if (db.getStartDate() == null)
            {
                throw new ServiceException("请选择生效日期", 400);
            }
            return db.getStartDate();
        }
        LocalDate date = RecurDateCalculator.toLocalDate(startDate);
        LocalDate today = RecurDateCalculator.today();
        if (date == null || date.isBefore(today))
        {
            LocalDate dbDate = RecurDateCalculator.toLocalDate(db.getStartDate());
            if (dbDate == null || !dbDate.equals(date))
            {
                throw new ServiceException("生效日期不能早于今天", 400);
            }
        }
        return startDate;
    }

    /**
     * 项目校验：存在且未归档
     */
    private void validateProject(Long projectId)
    {
        if (projectId == null)
        {
            return;
        }
        OaProject project = projectMapper.selectProjectById(projectId);
        if (project == null)
        {
            throw new ServiceException("项目不存在", 400);
        }
        if ("1".equals(project.getStatus()))
        {
            throw new ServiceException("项目已结束，不能关联", 400);
        }
    }

    /**
     * 主责人校验：在册成员；非主席团/管理员只能指派本部门成员（03 文档口径）
     */
    private void validateAssignee(Long assigneeId)
    {
        if (assigneeId == null)
        {
            throw new ServiceException("请选择主责人", 400);
        }
        OaMember member = memberMapper.selectByUserId(assigneeId);
        if (member == null || !OaMemberConstants.MEMBER_STATUS_ACTIVE.equals(member.getStatus()))
        {
            throw new ServiceException("主责人必须是未退会成员", 400);
        }
        SysUser me = currentUser.user();
        if (me == null)
        {
            throw new ServiceException("登录已过期，请重新登录", 401);
        }
        boolean bypass = me.isAdmin() || currentUser.hasRole(OaMemberConstants.ROLE_PRESIDENT);
        if (!bypass && me.getDeptId() != null && member.getDeptId() != null
                && !me.getDeptId().equals(member.getDeptId()))
        {
            throw new ServiceException("周期任务的负责人必须是本部门成员", 403);
        }
    }

    /**
     * 列表字段装配
     */
    private void fillListVO(RecurListVO vo, OaTaskRecur recur, Map<String, String> statusLabels,
            Map<String, String> priorityLabels)
    {
        vo.setRecurId(recur.getRecurId());
        vo.setTitle(recur.getTitle());
        vo.setDescription(recur.getDescription());
        vo.setProjectId(recur.getProjectId());
        vo.setProjectName(recur.getProjectName());
        vo.setWeekDays(RecurDateCalculator.weekDayList(recur.getWeekDays()));
        vo.setWeekDayLabels(RecurDateCalculator.weekDayLabels(recur.getWeekDays()));
        vo.setDueTime(recur.getDueTime());
        vo.setAssigneeId(recur.getAssigneeId());
        vo.setAssigneeName(recur.getAssigneeName());
        vo.setPriority(recur.getPriority());
        vo.setPriorityLabel(priorityLabels == null ? "" : priorityLabels.getOrDefault(recur.getPriority(), ""));
        vo.setTags(parseTags(recur.getTags()));
        vo.setStartDate(recur.getStartDate());
        vo.setStatus(recur.getStatus());
        vo.setStatusLabel(statusLabels == null ? "" : statusLabels.getOrDefault(recur.getStatus(), ""));
        vo.setNextDate(recur.getNextDate());
        vo.setCreatorId(recur.getCreatorId());
        vo.setCreatorName(recur.getCreatorName());
        vo.setDeptId(recur.getDeptId());
    }

    /**
     * 逗号串标签 → 列表
     */
    private List<String> parseTags(String tags)
    {
        List<String> result = new ArrayList<String>();
        if (StringUtils.isEmpty(tags))
        {
            return result;
        }
        for (String tag : tags.split(","))
        {
            if (StringUtils.isNotEmpty(tag))
            {
                result.add(tag.trim());
            }
        }
        return result;
    }
}
