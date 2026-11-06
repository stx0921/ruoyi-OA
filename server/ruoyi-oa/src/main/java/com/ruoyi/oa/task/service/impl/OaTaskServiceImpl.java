package com.ruoyi.oa.task.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.apply.domain.OaFile;
import com.ruoyi.oa.apply.service.IOaFileService;
import com.ruoyi.oa.notify.service.IOaNotifyService;
import com.ruoyi.oa.support.OaCurrentUser;
import com.ruoyi.oa.support.OaDictLabelProvider;
import com.ruoyi.oa.support.OaFileUrlHelper;
import com.ruoyi.oa.task.domain.OaTask;
import com.ruoyi.oa.task.dto.TaskAuditDTO;
import com.ruoyi.oa.task.dto.TaskCancelDTO;
import com.ruoyi.oa.task.dto.TaskChildDTO;
import com.ruoyi.oa.task.dto.TaskCreateDTO;
import com.ruoyi.oa.task.dto.TaskQuery;
import com.ruoyi.oa.task.dto.TaskReassignDTO;
import com.ruoyi.oa.task.dto.TaskSelfDTO;
import com.ruoyi.oa.task.dto.TaskSubmitDTO;
import com.ruoyi.oa.task.dto.TaskUpdateDTO;
import com.ruoyi.oa.task.mapper.OaTaskCollaboratorMapper;
import com.ruoyi.oa.task.mapper.OaTaskLogMapper;
import com.ruoyi.oa.task.mapper.OaTaskMapper;
import com.ruoyi.oa.task.service.IOaTaskLogService;
import com.ruoyi.oa.task.service.IOaTaskService;
import com.ruoyi.oa.task.support.OaTaskNoGenerator;
import com.ruoyi.oa.task.support.OaTaskPermissionChecker;
import com.ruoyi.oa.task.support.OaTaskScopeHelper;
import com.ruoyi.oa.task.support.OaTaskStateMachine;
import com.ruoyi.oa.task.vo.TaskBoardVO;
import com.ruoyi.oa.task.vo.TaskColumnVO;
import com.ruoyi.oa.task.vo.TaskCountsVO;
import com.ruoyi.oa.task.vo.TaskDetailVO;
import com.ruoyi.oa.task.vo.TaskFileVO;
import com.ruoyi.oa.task.vo.TaskListVO;
import com.ruoyi.oa.task.vo.TaskSaveResultVO;
import com.ruoyi.oa.task.vo.UserBriefVO;

/**
 * 任务核心 Service 实现
 *
 * <p>状态流转一律条件更新（0 行 → 409）；写操作入口统一「归档只读 → 状态机 → 权限」三层校验。</p>
 *
 * @author ruoyi-oa
 */
@Service
public class OaTaskServiceImpl implements IOaTaskService
{
    private static final Logger log = LoggerFactory.getLogger(OaTaskServiceImpl.class);

    /** 时间格式 */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 业务类型：任务附件 */
    private static final String BIZ_TYPE_TASK = "task";

    /** 通知类型：被派任务 */
    private static final String NOTIFY_TYPE_ASSIGN = "1";

    /** 通知类型：验收结果 */
    private static final String NOTIFY_TYPE_AUDIT = "4";

    /** 通知类型：系统通知（取消 / 改派 / 退回说明） */
    private static final String NOTIFY_TYPE_SYSTEM = "5";

    /** 看板四列定义 */
    private static final String[][] BOARD_COLUMNS = { { "0", "待办" }, { "1", "进行中" }, { "2", "待验收" },
            { "3", "已完成" } };

    /** 状态文案 */
    private static final Map<String, String> STATUS_LABELS = new HashMap<String, String>();

    /** 优先级文案 */
    private static final Map<String, String> PRIORITY_LABELS = new HashMap<String, String>();

    /** 来源文案 */
    private static final Map<String, String> SOURCE_LABELS = new HashMap<String, String>();

    static
    {
        STATUS_LABELS.put(OaTaskStateMachine.STATUS_TODO, "待办");
        STATUS_LABELS.put(OaTaskStateMachine.STATUS_DOING, "进行中");
        STATUS_LABELS.put(OaTaskStateMachine.STATUS_AUDIT, "待验收");
        STATUS_LABELS.put(OaTaskStateMachine.STATUS_DONE, "已完成");
        STATUS_LABELS.put(OaTaskStateMachine.STATUS_CANCELED, "已取消");

        PRIORITY_LABELS.put("0", "高");
        PRIORITY_LABELS.put("1", "中");
        PRIORITY_LABELS.put("2", "低");

        SOURCE_LABELS.put(OaTaskStateMachine.SOURCE_MANUAL, "手工创建");
        SOURCE_LABELS.put(OaTaskStateMachine.SOURCE_RECUR, "周期生成");
        SOURCE_LABELS.put(OaTaskStateMachine.SOURCE_CHILD, "子任务");
    }

    @Autowired
    private OaDictLabelProvider dictLabels;

    /**
     * 一次装配内复用的标签映射：字典优先、写死兜底
     */
    private Map<String, Map<String, String>> loadTaskDictMaps()
    {
        Map<String, Map<String, String>> maps = new HashMap<>();
        maps.put("oa_task_status", dictLabels.labels("oa_task_status", STATUS_LABELS));
        maps.put("oa_task_priority", dictLabels.labels("oa_task_priority", PRIORITY_LABELS));
        maps.put("oa_task_source", dictLabels.labels("oa_task_source", SOURCE_LABELS));
        return maps;
    }

    @Autowired
    private OaTaskMapper taskMapper;

    @Autowired
    private OaTaskCollaboratorMapper collaboratorMapper;

    @Autowired
    private OaTaskLogMapper taskLogMapper;

    @Autowired
    private IOaTaskLogService taskLogService;

    @Autowired
    private OaTaskPermissionChecker permissionChecker;

    @Autowired
    private OaTaskNoGenerator taskNoGenerator;

    @Autowired
    private OaCurrentUser currentUser;

    @Autowired
    private IOaFileService fileService;

    @Autowired
    private IOaNotifyService notifyService;

    /** 看板单列最大卡片数 */
    @Value("${oa.task.board-max-cards:300}")
    private int boardMaxCards;

    /* ==================== 列表 / 我的 / 看板 / 详情 ==================== */

    @Override
    @DataScope(deptAlias = "t", userAlias = "")
    public List<TaskListVO> selectTaskList(TaskQuery query)
    {
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        OaTaskScopeHelper.fill(query, ctx.getUserId());
        List<TaskListVO> list = taskMapper.selectTaskList(query);
        assembleList(list, ctx);
        return list;
    }

    @Override
    public List<TaskListVO> selectMyTaskList(TaskQuery query)
    {
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        OaTaskScopeHelper.fill(query, ctx.getUserId());
        List<TaskListVO> list = taskMapper.selectMyTaskList(query);
        assembleList(list, ctx);
        return list;
    }

    @Override
    public TaskCountsVO selectMyCounts()
    {
        Long me = currentUser.userId();
        TaskCountsVO counts = taskMapper.selectMyCounts(me);
        return counts == null ? new TaskCountsVO() : counts;
    }

    @Override
    @DataScope(deptAlias = "t", userAlias = "")
    public TaskBoardVO selectBoard(TaskQuery query)
    {
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        OaTaskScopeHelper.fill(query, ctx.getUserId());

        List<TaskListVO> tasks = taskMapper.selectBoardTasks(query);
        assembleList(tasks, ctx);

        TaskBoardVO board = new TaskBoardVO();
        Map<String, TaskColumnVO> columnMap = new LinkedHashMap<String, TaskColumnVO>();
        Map<String, String> boardStatusLabels = dictLabels.labels("oa_task_status", STATUS_LABELS);
        for (String[] def : BOARD_COLUMNS)
        {
            TaskColumnVO column = new TaskColumnVO(def[0], boardStatusLabels.getOrDefault(def[0], def[1]));
            board.getColumns().add(column);
            columnMap.put(def[0], column);
        }
        for (TaskListVO task : tasks)
        {
            TaskColumnVO column = columnMap.get(task.getStatus());
            if (column == null)
            {
                continue;
            }
            column.setCount(column.getCount() + 1);
            if (column.getTasks().size() < boardMaxCards)
            {
                column.getTasks().add(task);
            }
        }
        Long canceled = taskMapper.countBoardCanceled(query);
        board.setCanceledCount(canceled == null ? 0L : canceled);
        return board;
    }

    @Override
    public TaskDetailVO selectTaskDetail(Long taskId)
    {
        if (taskId == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        OaTask task = taskMapper.selectById(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        permissionChecker.assertCanView(ctx, task);

        TaskDetailVO detail = taskMapper.selectTaskDetailBase(taskId);
        if (detail == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        fillListFlags(detail, ctx);
        detail.setChildren(assembleChildren(taskId, ctx));
        detail.setCollaborators(collaboratorMapper.selectCollaborators(taskId));
        detail.setFiles(toFileVOs(fileService.selectByBiz(BIZ_TYPE_TASK, taskId)));
        detail.setLogs(taskLogService.selectLogList(taskId));
        if (detail.getParentId() != null)
        {
            OaTask parent = taskMapper.selectById(detail.getParentId());
            if (parent != null)
            {
                detail.setParentTaskNo(parent.getTaskNo());
            }
        }
        return detail;
    }

    /* ==================== 创建 / 编辑 / 状态流转 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskSaveResultVO insertTask(TaskCreateDTO dto)
    {
        if (dto == null)
        {
            throw new ServiceException("参数不能为空", HttpStatus.BAD_REQUEST);
        }
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        String title = requireTitle(dto.getTitle());
        validateDescription(dto.getDescription());
        validatePriority(dto.getPriority());
        validateTags(dto.getTags());
        Date dueTime = requireDueTime(dto.getDueTime());
        validateStartBeforeDue(dto.getStartTime(), dueTime);
        if (dto.getAssigneeId() == null)
        {
            throw new ServiceException("请选择主责人", HttpStatus.BAD_REQUEST);
        }

        Long projectId = dto.getProjectId();
        Long parentId = dto.getParentId();
        String parentRemark = null;
        if (parentId != null)
        {
            OaTask parent = taskMapper.selectById(parentId);
            if (parent == null)
            {
                throw new ServiceException("父任务不存在", HttpStatus.NOT_FOUND);
            }
            if (parent.getParentId() != null)
            {
                throw new ServiceException("子任务只能拆一层", HttpStatus.BAD_REQUEST);
            }
            if (!OaTaskStateMachine.isEditable(parent.getStatus()))
            {
                throw new ServiceException("父任务已完成或已取消，不能新建子任务", HttpStatus.CONFLICT);
            }
            if (dto.getProjectId() != null && !dto.getProjectId().equals(parent.getProjectId()))
            {
                throw new ServiceException("子任务项目需与父任务一致", HttpStatus.BAD_REQUEST);
            }
            projectId = parent.getProjectId();
            parentRemark = "创建子任务";
            if (dto.getChildren() != null && !dto.getChildren().isEmpty())
            {
                // 子任务只允许一层，子任务下不能再拆子任务（D1）
                throw new ServiceException("子任务只能拆一层", HttpStatus.BAD_REQUEST);
            }
        }
        else if (projectId != null)
        {
            permissionChecker.assertProjectOpenForCreate(projectId);
        }
        permissionChecker.assertDispatch(ctx, dto.getAssigneeId(), projectId);
        List<Long> collaboratorIds = permissionChecker.normalizeCollaborators(ctx, dto.getCollaboratorIds(), projectId,
                dto.getAssigneeId());

        OaTask task = new OaTask();
        task.setTaskNo(taskNoGenerator.next());
        task.setTitle(title);
        task.setDescription(defaultString(dto.getDescription()));
        task.setProjectId(projectId);
        task.setParentId(parentId);
        task.setAssigneeId(dto.getAssigneeId());
        task.setPriority(defaultPriority(dto.getPriority()));
        task.setTags(joinTags(dto.getTags()));
        task.setStatus(OaTaskStateMachine.STATUS_TODO);
        task.setStartTime(dto.getStartTime());
        task.setDueTime(dueTime);
        task.setRejectCount(0);
        task.setCreatorId(ctx.getUserId());
        task.setAuditorId(ctx.getUserId());
        task.setSource(parentId == null ? OaTaskStateMachine.SOURCE_MANUAL : OaTaskStateMachine.SOURCE_CHILD);
        task.setOverdueFlag(overdueFlagOf(dueTime, task.getStatus()));
        task.setDeptId(ctx.getDeptId());
        task.setCreateBy(currentUser.username());
        taskMapper.insertTask(task);

        if (!collaboratorIds.isEmpty())
        {
            collaboratorMapper.insertBatch(task.getTaskId(), collaboratorIds, currentUser.username());
        }
        String projectName = projectName(projectId);
        insertChildren(ctx, task, dto.getChildren(), projectName);
        bindFiles(task.getTaskId(), dto.getAttachmentFileIds());
        taskLogService.log(task, OaTaskStateMachine.ACTION_CREATE, parentRemark, null, snapshot(task));
        notifyAssigned(task, collaboratorIds, projectName, ctx);
        return new TaskSaveResultVO(task.getTaskId(), task.getTaskNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(TaskUpdateDTO dto)
    {
        if (dto == null || dto.getTaskId() == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        OaTask task = taskMapper.selectByIdForUpdate(dto.getTaskId());
        if (task == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        permissionChecker.assertActiveProject(task.getProjectId());
        if (!OaTaskStateMachine.isEditable(task.getStatus()))
        {
            throw OaTaskStateMachine.isFinished(task.getStatus()) ? OaTaskStateMachine.finished()
                    : OaTaskStateMachine.conflict();
        }
        permissionChecker.assertCanEdit(ctx, task);
        if (dto.getParentId() != null && !dto.getParentId().equals(task.getParentId()))
        {
            throw new ServiceException("不支持修改父任务", HttpStatus.BAD_REQUEST);
        }
        if (dto.getTitle() != null)
        {
            requireTitle(dto.getTitle());
        }
        if (dto.getDescription() != null)
        {
            validateDescription(dto.getDescription());
        }
        if (dto.getPriority() != null)
        {
            validatePriority(dto.getPriority());
        }
        if (dto.getTags() != null)
        {
            validateTags(dto.getTags());
        }
        Date newDue = dto.getDueTime() != null ? dto.getDueTime() : task.getDueTime();
        Date newStart = dto.getStartTime() != null ? dto.getStartTime() : task.getStartTime();
        validateStartBeforeDue(newStart, newDue);

        Long newProjectId = task.getProjectId();
        if (dto.getProjectId() != null && !dto.getProjectId().equals(task.getProjectId()))
        {
            permissionChecker.assertProjectOpenForCreate(dto.getProjectId());
            newProjectId = dto.getProjectId();
        }
        Long newAssignee = dto.getAssigneeId() != null ? dto.getAssigneeId() : task.getAssigneeId();
        if (dto.getAssigneeId() != null || !Objects.equals(newProjectId, task.getProjectId()))
        {
            permissionChecker.assertDispatch(ctx, newAssignee, newProjectId);
        }
        List<Long> collaboratorIds = null;
        if (dto.getCollaboratorIds() != null)
        {
            collaboratorIds = permissionChecker.normalizeCollaborators(ctx, dto.getCollaboratorIds(), newProjectId,
                    newAssignee);
        }

        // 字段级 diff（仅变更字段）
        Map<String, Object> before = new LinkedHashMap<String, Object>();
        Map<String, Object> after = new LinkedHashMap<String, Object>();
        if (dto.getTitle() != null)
        {
            putDiff(before, after, "title", task.getTitle(), dto.getTitle().trim());
        }
        if (dto.getDescription() != null)
        {
            putDiff(before, after, "description", defaultString(task.getDescription()),
                    defaultString(dto.getDescription()));
        }
        if (dto.getProjectId() != null)
        {
            putDiff(before, after, "projectId", task.getProjectId(), dto.getProjectId());
        }
        if (dto.getAssigneeId() != null)
        {
            putDiff(before, after, "assigneeId", task.getAssigneeId(), dto.getAssigneeId());
        }
        if (dto.getPriority() != null)
        {
            putDiff(before, after, "priority", task.getPriority(), dto.getPriority());
        }
        if (dto.getTags() != null)
        {
            putDiff(before, after, "tags", defaultString(task.getTags()), joinTags(dto.getTags()));
        }
        if (dto.getStartTime() != null)
        {
            putDiff(before, after, "startTime", formatDateTime(task.getStartTime()),
                    formatDateTime(dto.getStartTime()));
        }
        if (dto.getDueTime() != null)
        {
            putDiff(before, after, "dueTime", formatDateTime(task.getDueTime()), formatDateTime(dto.getDueTime()));
        }
        if (collaboratorIds != null)
        {
            List<Long> oldCollaborators = collaboratorMapper.selectUserIdsByTaskId(task.getTaskId());
            putDiff(before, after, "collaboratorIds", joinIds(oldCollaborators), joinIds(collaboratorIds));
        }

        Long oldAssignee = task.getAssigneeId();
        Long oldProjectId = task.getProjectId();
        if (dto.getTitle() != null)
        {
            task.setTitle(dto.getTitle().trim());
        }
        if (dto.getDescription() != null)
        {
            task.setDescription(defaultString(dto.getDescription()));
        }
        if (dto.getProjectId() != null)
        {
            task.setProjectId(dto.getProjectId());
        }
        if (dto.getAssigneeId() != null)
        {
            task.setAssigneeId(dto.getAssigneeId());
        }
        if (dto.getPriority() != null)
        {
            task.setPriority(dto.getPriority());
        }
        if (dto.getTags() != null)
        {
            task.setTags(joinTags(dto.getTags()));
        }
        if (dto.getStartTime() != null)
        {
            task.setStartTime(dto.getStartTime());
        }
        if (dto.getDueTime() != null)
        {
            task.setDueTime(dto.getDueTime());
        }
        task.setOverdueFlag(overdueFlagOf(newDue, task.getStatus()));
        task.setUpdateBy(currentUser.username());
        taskMapper.updateTask(task);

        // 父任务换项目：子任务跟随（D2）
        if (!Objects.equals(oldProjectId, task.getProjectId()))
        {
            taskMapper.updateChildrenProjectId(task.getTaskId(), task.getProjectId());
        }
        if (collaboratorIds != null)
        {
            collaboratorMapper.deleteByTaskId(task.getTaskId());
            if (!collaboratorIds.isEmpty())
            {
                collaboratorMapper.insertBatch(task.getTaskId(), collaboratorIds, currentUser.username());
            }
        }
        bindFiles(task.getTaskId(), dto.getAttachmentFileIds());
        if (!before.isEmpty())
        {
            taskLogService.log(task, OaTaskStateMachine.ACTION_EDIT, null, before, after);
        }
        if (!Objects.equals(oldAssignee, task.getAssigneeId()))
        {
            notifyReassigned(task, oldAssignee, task.getAssigneeId(), ctx);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTask(Long taskId)
    {
        if (taskId == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        OaTask task = taskMapper.selectByIdForUpdate(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        permissionChecker.assertActiveProject(task.getProjectId());
        if (!OaTaskStateMachine.STATUS_TODO.equals(task.getStatus()))
        {
            throw OaTaskStateMachine.isFinished(task.getStatus()) ? OaTaskStateMachine.finished()
                    : OaTaskStateMachine.conflict();
        }
        permissionChecker.assertCanStart(ctx, task);
        int rows = taskMapper.startTask(taskId);
        if (rows == 0)
        {
            throw OaTaskStateMachine.conflict();
        }
        Map<String, Object> after = new LinkedHashMap<String, Object>();
        after.put("status", OaTaskStateMachine.STATUS_DOING);
        after.put("startTime", formatDateTime(new Date()));
        taskLogService.log(task, OaTaskStateMachine.ACTION_START, null, null, after);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTask(TaskSubmitDTO dto)
    {
        if (dto == null || dto.getTaskId() == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        OaTask task = taskMapper.selectByIdForUpdate(dto.getTaskId());
        if (task == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        permissionChecker.assertActiveProject(task.getProjectId());
        if (!OaTaskStateMachine.STATUS_DOING.equals(task.getStatus()))
        {
            throw OaTaskStateMachine.isFinished(task.getStatus()) ? OaTaskStateMachine.finished()
                    : OaTaskStateMachine.conflict();
        }
        permissionChecker.assertCanSubmit(ctx, task);

        // TR1：父任务必须全部子任务完成
        List<OaTask> unfinished = taskMapper.selectUnfinishedChildren(task.getTaskId());
        if (unfinished != null && !unfinished.isEmpty())
        {
            StringBuilder message = new StringBuilder("还有 ").append(unfinished.size()).append(" 个子任务未完成：");
            for (int i = 0; i < unfinished.size(); i++)
            {
                if (i > 0)
                {
                    message.append("、");
                }
                message.append(unfinished.get(i).getTitle());
            }
            throw new ServiceException(message.toString(), HttpStatus.CONFLICT);
        }

        int rows = taskMapper.submitTask(task.getTaskId());
        if (rows == 0)
        {
            throw OaTaskStateMachine.conflict();
        }
        bindFiles(task.getTaskId(), dto.getAttachmentFileIds());
        Map<String, Object> after = new LinkedHashMap<String, Object>();
        after.put("status", OaTaskStateMachine.STATUS_AUDIT);
        after.put("submitTime", formatDateTime(new Date()));
        taskLogService.log(task, OaTaskStateMachine.ACTION_SUBMIT, dto.getRemark(), null, after);

        // TPL-4 待验收：通知验收人
        Long auditorId = task.getAuditorId() != null ? task.getAuditorId() : task.getCreatorId();
        if (auditorId != null && !auditorId.equals(ctx.getUserId()))
        {
            safeNotify(auditorId, NOTIFY_TYPE_AUDIT, "待你验收：" + task.getTitle(),
                    currentUser.nickName() + " 提交了「" + task.getTitle() + "」，请及时验收。", task.getTaskId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditTask(TaskAuditDTO dto)
    {
        if (dto == null || dto.getTaskId() == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        if (dto.getPass() == null)
        {
            throw new ServiceException("请选择验收结果", HttpStatus.BAD_REQUEST);
        }
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        OaTask task = taskMapper.selectByIdForUpdate(dto.getTaskId());
        if (task == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        permissionChecker.assertActiveProject(task.getProjectId());
        if (!OaTaskStateMachine.STATUS_AUDIT.equals(task.getStatus()))
        {
            throw OaTaskStateMachine.isFinished(task.getStatus()) ? OaTaskStateMachine.finished()
                    : OaTaskStateMachine.conflict();
        }
        permissionChecker.assertCanAudit(ctx, task);

        if (!dto.getPass().booleanValue())
        {
            // 打回：理由 2-200 必填
            String remark = requireReason(dto.getRemark(), "请填写打回理由（2–200 字）");
            Map<String, Object> before = new LinkedHashMap<String, Object>();
            before.put("status", task.getStatus());
            int rows = taskMapper.rejectTask(task.getTaskId());
            if (rows == 0)
            {
                throw new ServiceException("任务已被处理，请刷新", HttpStatus.CONFLICT);
            }
            Map<String, Object> after = new LinkedHashMap<String, Object>();
            after.put("status", OaTaskStateMachine.STATUS_DOING);
            after.put("rejectCount", (task.getRejectCount() == null ? 0 : task.getRejectCount()) + 1);
            taskLogService.log(task, OaTaskStateMachine.ACTION_REJECT, remark, before, after);
            Long assigneeId = task.getAssigneeId();
            if (assigneeId != null && !assigneeId.equals(ctx.getUserId()))
            {
                safeNotify(assigneeId, NOTIFY_TYPE_AUDIT, "任务被打回：" + task.getTitle(),
                        "「" + task.getTitle() + "」被打回：" + remark + "，请修改后重新提交。", task.getTaskId());
            }
            return;
        }

        // 验收通过：写 finish_time
        Map<String, Object> before = new LinkedHashMap<String, Object>();
        before.put("status", task.getStatus());
        int rows = taskMapper.finishTask(task.getTaskId());
        if (rows == 0)
        {
            throw new ServiceException("任务已被处理，请刷新", HttpStatus.CONFLICT);
        }
        bindFiles(task.getTaskId(), dto.getFinishFileIds());
        Date now = new Date();
        Map<String, Object> after = new LinkedHashMap<String, Object>();
        after.put("status", OaTaskStateMachine.STATUS_DONE);
        after.put("finishTime", formatDateTime(now));
        taskLogService.log(task, OaTaskStateMachine.ACTION_PASS, trimToNull(dto.getRemark()), before, after);
        Long assigneeId = task.getAssigneeId();
        if (assigneeId != null && !assigneeId.equals(ctx.getUserId()))
        {
            safeNotify(assigneeId, NOTIFY_TYPE_AUDIT, "验收通过：" + task.getTitle(),
                    "「" + task.getTitle() + "」已验收通过，完成时间 " + formatDateTime(now) + "。", task.getTaskId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(TaskCancelDTO dto)
    {
        if (dto == null || dto.getTaskId() == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        OaTask task = taskMapper.selectByIdForUpdate(dto.getTaskId());
        if (task == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        permissionChecker.assertActiveProject(task.getProjectId());
        if (OaTaskStateMachine.STATUS_AUDIT.equals(task.getStatus()))
        {
            throw new ServiceException("待验收任务请先打回再取消", HttpStatus.CONFLICT);
        }
        if (!OaTaskStateMachine.isCancelable(task.getStatus()))
        {
            throw OaTaskStateMachine.finished();
        }
        permissionChecker.assertCanCancel(ctx, task);
        String remark = requireReason(dto.getRemark(), "请填写取消原因（2–200 字）");
        int rows = taskMapper.cancelTask(task.getTaskId());
        if (rows == 0)
        {
            throw OaTaskStateMachine.conflict();
        }
        Map<String, Object> before = new LinkedHashMap<String, Object>();
        before.put("status", task.getStatus());
        Map<String, Object> after = new LinkedHashMap<String, Object>();
        after.put("status", OaTaskStateMachine.STATUS_CANCELED);
        taskLogService.log(task, OaTaskStateMachine.ACTION_CANCEL, remark, before, after);

        // 通知相关人（主责 / 派发 / 协作），跳过操作人
        Set<Long> receivers = new LinkedHashSet<Long>();
        addIfNotNull(receivers, task.getAssigneeId());
        addIfNotNull(receivers, task.getCreatorId());
        List<Long> collaborators = collaboratorMapper.selectUserIdsByTaskId(task.getTaskId());
        if (collaborators != null)
        {
            for (Long id : collaborators)
            {
                addIfNotNull(receivers, id);
            }
        }
        String content = currentUser.nickName() + " 取消了任务「" + task.getTitle() + "」，原因：" + remark;
        for (Long receiverId : receivers)
        {
            if (receiverId.equals(ctx.getUserId()))
            {
                continue;
            }
            safeNotify(receiverId, NOTIFY_TYPE_SYSTEM, "任务已取消：" + task.getTitle(), content, task.getTaskId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassignTask(TaskReassignDTO dto)
    {
        if (dto == null || dto.getTaskId() == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        if (dto.getAssigneeId() == null)
        {
            throw new ServiceException("请选择新主责人", HttpStatus.BAD_REQUEST);
        }
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        OaTask task = taskMapper.selectByIdForUpdate(dto.getTaskId());
        if (task == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        permissionChecker.assertActiveProject(task.getProjectId());
        if (!OaTaskStateMachine.isReassignable(task.getStatus()))
        {
            throw OaTaskStateMachine.finished();
        }
        permissionChecker.assertCanReassign(ctx, task);
        Long oldAssigneeId = task.getAssigneeId();
        boolean assigneeChanged = !dto.getAssigneeId().equals(oldAssigneeId);
        if (!assigneeChanged && dto.getCollaboratorIds() == null)
        {
            return;
        }
        if (assigneeChanged)
        {
            permissionChecker.assertDispatch(ctx, dto.getAssigneeId(), task.getProjectId());
        }
        List<Long> collaboratorIds = null;
        if (dto.getCollaboratorIds() != null)
        {
            collaboratorIds = permissionChecker.normalizeCollaborators(ctx, dto.getCollaboratorIds(),
                    task.getProjectId(), dto.getAssigneeId());
        }

        Map<String, Object> before = new LinkedHashMap<String, Object>();
        Map<String, Object> after = new LinkedHashMap<String, Object>();
        if (assigneeChanged)
        {
            before.put("assigneeId", oldAssigneeId);
            after.put("assigneeId", dto.getAssigneeId());
        }
        if (collaboratorIds != null)
        {
            List<Long> oldCollaborators = collaboratorMapper.selectUserIdsByTaskId(task.getTaskId());
            putDiff(before, after, "collaboratorIds", joinIds(oldCollaborators), joinIds(collaboratorIds));
        }

        if (assigneeChanged)
        {
            OaTask update = new OaTask();
            update.setTaskId(task.getTaskId());
            update.setAssigneeId(dto.getAssigneeId());
            update.setUpdateBy(currentUser.username());
            taskMapper.updateTask(update);
        }
        if (collaboratorIds != null)
        {
            collaboratorMapper.deleteByTaskId(task.getTaskId());
            if (!collaboratorIds.isEmpty())
            {
                collaboratorMapper.insertBatch(task.getTaskId(), collaboratorIds, currentUser.username());
            }
        }
        taskLogService.log(task, OaTaskStateMachine.ACTION_REASSIGN, trimToNull(dto.getRemark()), before, after);
        if (assigneeChanged)
        {
            notifyReassigned(task, oldAssigneeId, dto.getAssigneeId(), ctx);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskSaveResultVO insertSelfTask(TaskSelfDTO dto)
    {
        if (dto == null)
        {
            throw new ServiceException("参数不能为空", HttpStatus.BAD_REQUEST);
        }
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        String title = requireTitle(dto.getTitle());
        validateDescription(dto.getDescription());
        validatePriority(dto.getPriority());
        validateTags(dto.getTags());
        Date dueTime = requireDueTime(dto.getDueTime());

        OaTask task = new OaTask();
        task.setTaskNo(taskNoGenerator.next());
        task.setTitle(title);
        task.setDescription(defaultString(dto.getDescription()));
        task.setProjectId(null);
        task.setParentId(null);
        task.setAssigneeId(ctx.getUserId());
        task.setPriority(defaultPriority(dto.getPriority()));
        task.setTags(joinTags(dto.getTags()));
        task.setStatus(OaTaskStateMachine.STATUS_TODO);
        task.setDueTime(dueTime);
        task.setRejectCount(0);
        task.setCreatorId(ctx.getUserId());
        task.setAuditorId(ctx.getUserId());
        task.setSource(OaTaskStateMachine.SOURCE_MANUAL);
        task.setOverdueFlag(overdueFlagOf(dueTime, task.getStatus()));
        task.setDeptId(ctx.getDeptId());
        task.setCreateBy(currentUser.username());
        taskMapper.insertTask(task);
        bindFiles(task.getTaskId(), dto.getAttachmentFileIds());
        taskLogService.log(task, OaTaskStateMachine.ACTION_CREATE, "给自己创建任务", null, snapshot(task));
        return new TaskSaveResultVO(task.getTaskId(), task.getTaskNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTasks(Long[] taskIds)
    {
        if (taskIds == null || taskIds.length == 0)
        {
            return;
        }
        OaTaskPermissionChecker.Ctx ctx = permissionChecker.context();
        // ① 先全量收集 + 权限校验（任一越权直接 403，避免部分删除）
        Set<Long> topIds = new LinkedHashSet<Long>();
        List<OaTask> existed = new ArrayList<OaTask>();
        for (Long taskId : taskIds)
        {
            if (taskId == null || topIds.contains(taskId))
            {
                continue;
            }
            topIds.add(taskId);
            OaTask task = taskMapper.selectById(taskId);
            if (task == null)
            {
                // 幂等：不存在或已删除的 ID 跳过
                continue;
            }
            assertCanDeleteTask(ctx, task);
            existed.add(task);
        }
        if (existed.isEmpty())
        {
            return;
        }
        // ② 级联收集子任务（只允许一层）
        Set<Long> allTaskIds = new LinkedHashSet<Long>();
        for (OaTask task : existed)
        {
            allTaskIds.add(task.getTaskId());
            List<OaTask> children = taskMapper.selectChildren(task.getTaskId());
            if (children != null)
            {
                for (OaTask child : children)
                {
                    if (child.getTaskId() != null)
                    {
                        allTaskIds.add(child.getTaskId());
                    }
                }
            }
        }
        Long[] ids = allTaskIds.toArray(new Long[0]);
        // ③ 批量删协作人 / 流转日志 / 任务本体
        collaboratorMapper.deleteByTaskIds(ids);
        taskLogMapper.deleteByTaskIds(ids);
        taskMapper.deleteByTaskIds(ids);
        // ④ 附件记录 / 站内信按业务对象清理（旁路资源，失败只记日志不阻断删除）
        for (Long taskId : allTaskIds)
        {
            deleteTaskFiles(taskId);
            deleteTaskNotifies(taskId);
        }
        log.info("任务硬删除完成：任务 {} 条（含子任务 {} 条），操作人={}", existed.size(), allTaskIds.size(),
                ctx.getUserId());
    }

    /**
     * 删除任务权限：admin / 主席团任意；部长仅本部门（dept_id=派发人部门）；其余 403
     */
    private void assertCanDeleteTask(OaTaskPermissionChecker.Ctx ctx, OaTask task)
    {
        if (ctx.isAdminOrPresident())
        {
            return;
        }
        if (ctx.isMinister())
        {
            if (task.getDeptId() != null && task.getDeptId().equals(ctx.getDeptId()))
            {
                return;
            }
            throw new ServiceException("只能删除本部门的任务", HttpStatus.FORBIDDEN);
        }
        throw new ServiceException("没有权限删除任务", HttpStatus.FORBIDDEN);
    }

    /**
     * 删除任务附件记录（oa_file，biz_type=task）
     */
    private void deleteTaskFiles(Long taskId)
    {
        try
        {
            fileService.deleteByBiz(BIZ_TYPE_TASK, taskId);
        }
        catch (Exception e)
        {
            log.warn("任务附件记录删除失败：taskId={}, {}", taskId, e.getMessage());
        }
    }

    /**
     * 删除任务站内信（oa_notify，biz_type=task）
     */
    private void deleteTaskNotifies(Long taskId)
    {
        try
        {
            notifyService.deleteByBiz(BIZ_TYPE_TASK, taskId);
        }
        catch (Exception e)
        {
            log.warn("任务站内信删除失败：taskId={}, {}", taskId, e.getMessage());
        }
    }

    /* ==================== 内部方法 ==================== */

    /**
     * 创建子任务（单层，项目/主责默认继承校验）
     */
    private void insertChildren(OaTaskPermissionChecker.Ctx ctx, OaTask parent, List<TaskChildDTO> children,
            String projectName)
    {
        if (children == null || children.isEmpty())
        {
            return;
        }
        for (TaskChildDTO childDto : children)
        {
            if (childDto == null)
            {
                continue;
            }
            String childTitle = requireTitle(childDto.getTitle());
            validateTags(childDto.getTags());
            if (childDto.getAssigneeId() == null)
            {
                throw new ServiceException("请选择子任务主责人", HttpStatus.BAD_REQUEST);
            }
            permissionChecker.assertDispatch(ctx, childDto.getAssigneeId(), parent.getProjectId());
            Date childDue = childDto.getDueTime() != null ? childDto.getDueTime() : parent.getDueTime();
            OaTask child = new OaTask();
            child.setTaskNo(taskNoGenerator.next());
            child.setTitle(childTitle);
            child.setDescription("");
            child.setProjectId(parent.getProjectId());
            child.setParentId(parent.getTaskId());
            child.setAssigneeId(childDto.getAssigneeId());
            child.setPriority(defaultPriority(childDto.getPriority() != null ? childDto.getPriority()
                    : parent.getPriority()));
            child.setTags(childDto.getTags() != null ? joinTags(childDto.getTags()) : defaultString(parent.getTags()));
            child.setStatus(OaTaskStateMachine.STATUS_TODO);
            child.setDueTime(childDue);
            child.setRejectCount(0);
            child.setCreatorId(ctx.getUserId());
            child.setAuditorId(ctx.getUserId());
            child.setSource(OaTaskStateMachine.SOURCE_CHILD);
            child.setOverdueFlag(overdueFlagOf(childDue, child.getStatus()));
            child.setDeptId(ctx.getDeptId());
            child.setCreateBy(currentUser.username());
            taskMapper.insertTask(child);
            taskLogService.log(child, OaTaskStateMachine.ACTION_CREATE, "创建子任务", null, snapshot(child));
            notifyAssigned(child, null, projectName, ctx);
        }
    }

    /**
     * 详情子任务装配（归档只读 / 操作标记按当前人计算）
     */
    private List<TaskListVO> assembleChildren(Long taskId, OaTaskPermissionChecker.Ctx ctx)
    {
        List<TaskListVO> children = taskMapper.selectChildVoList(taskId);
        assembleList(children, ctx);
        return children;
    }

    /**
     * 列表 VO 统一补文案 / 逾期 / 操作标记
     */
    private void assembleList(List<TaskListVO> list, OaTaskPermissionChecker.Ctx ctx)
    {
        if (list == null || list.isEmpty())
        {
            return;
        }
        Map<String, Map<String, String>> dictMaps = loadTaskDictMaps();
        for (TaskListVO vo : list)
        {
            fillListFlags(vo, ctx, dictMaps);
        }
    }

    /**
     * 服务端计算 canXxx / readOnly / overdueDays（前端只渲染）
     */
    private void fillListFlags(TaskListVO vo, OaTaskPermissionChecker.Ctx ctx)
    {
        fillListFlags(vo, ctx, loadTaskDictMaps());
    }

    private void fillListFlags(TaskListVO vo, OaTaskPermissionChecker.Ctx ctx,
            Map<String, Map<String, String>> dictMaps)
    {
        if (vo == null)
        {
            return;
        }
        vo.setStatusLabel(label(dictMaps.get("oa_task_status"), vo.getStatus()));
        vo.setPriorityLabel(label(dictMaps.get("oa_task_priority"), vo.getPriority()));
        vo.setSourceLabel(label(dictMaps.get("oa_task_source"), vo.getSource()));
        if (vo.getRejectCount() == null)
        {
            vo.setRejectCount(0);
        }
        String status = vo.getStatus();
        Long me = ctx.getUserId();
        boolean adminOrPresident = ctx.isAdminOrPresident();
        boolean readOnly = "1".equals(vo.getProjectStatus());
        boolean isCreator = me != null && me.equals(vo.getCreatorId());
        boolean isAssignee = me != null && me.equals(vo.getAssigneeId());
        boolean isAuditor = me != null
                && (me.equals(vo.getAuditorId()) || me.equals(vo.getProjectOwnerId()));
        boolean editable = OaTaskStateMachine.isEditable(status);
        vo.setReadOnly(readOnly);
        vo.setCanStart(!readOnly && OaTaskStateMachine.STATUS_TODO.equals(status)
                && (isAssignee || isCreator || adminOrPresident));
        vo.setCanSubmit(!readOnly && OaTaskStateMachine.STATUS_DOING.equals(status) && isAssignee);
        vo.setCanAudit(!readOnly && OaTaskStateMachine.isAuditable(status)
                && (isAuditor || isCreator || adminOrPresident));
        vo.setCanEdit(!readOnly && editable && (isCreator || adminOrPresident));
        vo.setCanCancel(!readOnly && OaTaskStateMachine.isCancelable(status)
                && (isCreator || isAssignee || adminOrPresident));
        vo.setCanReassign(!readOnly && OaTaskStateMachine.isReassignable(status)
                && (isCreator || adminOrPresident));

        // 逾期展示以查询时计算为准（未完成且 due_time < now，最小 1 天）
        boolean unfinished = OaTaskStateMachine.STATUS_TODO.equals(status)
                || OaTaskStateMachine.STATUS_DOING.equals(status);
        if (unfinished && vo.getDueTime() != null && vo.getDueTime().before(new Date()))
        {
            long diff = System.currentTimeMillis() - vo.getDueTime().getTime();
            int days = (int) Math.ceil(diff / 86400000.0D);
            vo.setOverdueDays(days < 1 ? 1 : days);
            vo.setOverdueFlag("1");
        }
        else
        {
            vo.setOverdueDays(0);
            vo.setOverdueFlag("0");
        }
    }

    /**
     * 父任务创建成功后的 TPL-1 通知（主责 + 协作人各一条）
     */
    private void notifyAssigned(OaTask task, List<Long> collaboratorIds, String projectName,
            OaTaskPermissionChecker.Ctx ctx)
    {
        String content = currentUser.nickName() + " 给你派发了任务「" + task.getTitle() + "」，截止 "
                + formatDateTime(task.getDueTime()) + "。";
        content += StringUtils.isNotEmpty(projectName) ? "项目：" + projectName : "（独立任务）";
        String title = "新任务：" + task.getTitle();
        Set<Long> receivers = new LinkedHashSet<Long>();
        addIfNotNull(receivers, task.getAssigneeId());
        if (collaboratorIds != null)
        {
            for (Long id : collaboratorIds)
            {
                addIfNotNull(receivers, id);
            }
        }
        for (Long receiverId : receivers)
        {
            if (receiverId.equals(ctx.getUserId()))
            {
                continue;
            }
            safeNotify(receiverId, NOTIFY_TYPE_ASSIGN, title, content, task.getTaskId());
        }
    }

    /**
     * 改派通知：原主责 + 新主责（跳过操作人）
     */
    private void notifyReassigned(OaTask task, Long oldAssigneeId, Long newAssigneeId,
            OaTaskPermissionChecker.Ctx ctx)
    {
        String newName = "-";
        if (newAssigneeId != null)
        {
            UserBriefVO brief = taskMapper.selectUserBrief(newAssigneeId);
            if (brief != null && StringUtils.isNotEmpty(brief.getUserName()))
            {
                newName = brief.getUserName();
            }
        }
        if (oldAssigneeId != null && !oldAssigneeId.equals(ctx.getUserId()) && !oldAssigneeId.equals(newAssigneeId))
        {
            safeNotify(oldAssigneeId, NOTIFY_TYPE_SYSTEM, "任务已改派：" + task.getTitle(),
                    "你负责的任务「" + task.getTitle() + "」已改派给 " + newName + "。", task.getTaskId());
        }
        if (newAssigneeId != null && !newAssigneeId.equals(ctx.getUserId()))
        {
            safeNotify(newAssigneeId, NOTIFY_TYPE_SYSTEM, "任务已改派给你：" + task.getTitle(),
                    "任务「" + task.getTitle() + "」已改派给你，截止 " + formatDateTime(task.getDueTime()) + "。",
                    task.getTaskId());
        }
    }

    /**
     * 站内信写入（失败只记日志，不影响任务主流程）
     */
    private void safeNotify(Long userId, String type, String title, String content, Long taskId)
    {
        if (userId == null)
        {
            return;
        }
        try
        {
            notifyService.notify(userId, type, title, content, BIZ_TYPE_TASK, taskId, null);
        }
        catch (Exception e)
        {
            log.warn("任务站内信发送失败：userId={}, taskId={}, {}", userId, taskId, e.getMessage());
        }
    }

    /**
     * 绑定附件到任务（biz_type=task）
     */
    private void bindFiles(Long taskId, List<Long> fileIds)
    {
        if (taskId == null || fileIds == null || fileIds.isEmpty())
        {
            return;
        }
        for (Long fileId : fileIds)
        {
            if (fileId == null)
            {
                continue;
            }
            try
            {
                fileService.bindBiz(fileId, BIZ_TYPE_TASK, taskId);
            }
            catch (Exception e)
            {
                log.warn("任务附件绑定失败：fileId={}, taskId={}, {}", fileId, taskId, e.getMessage());
            }
        }
    }

    /**
     * 附件转 VO（统一逻辑下载地址）
     */
    private List<TaskFileVO> toFileVOs(List<OaFile> files)
    {
        List<TaskFileVO> result = new ArrayList<TaskFileVO>();
        if (files == null || files.isEmpty())
        {
            return result;
        }
        for (OaFile file : files)
        {
            if (file == null)
            {
                continue;
            }
            TaskFileVO vo = new TaskFileVO();
            vo.setFileId(file.getFileId());
            vo.setFileName(file.getFileName());
            vo.setOriginalName(file.getOriginalName());
            vo.setFileSize(file.getFileSize());
            vo.setFileUrl(OaFileUrlHelper.url(file.getFileId()));
            result.add(vo);
        }
        return result;
    }

    /**
     * 任务快照（创建日志 after_json / before-after diff 用）
     */
    private Map<String, Object> snapshot(OaTask task)
    {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        if (task == null)
        {
            return snapshot;
        }
        snapshot.put("taskNo", task.getTaskNo());
        snapshot.put("title", task.getTitle());
        snapshot.put("description", task.getDescription());
        snapshot.put("projectId", task.getProjectId());
        snapshot.put("parentId", task.getParentId());
        snapshot.put("assigneeId", task.getAssigneeId());
        snapshot.put("priority", task.getPriority());
        snapshot.put("tags", task.getTags());
        snapshot.put("status", task.getStatus());
        snapshot.put("startTime", formatDateTime(task.getStartTime()));
        snapshot.put("dueTime", formatDateTime(task.getDueTime()));
        snapshot.put("creatorId", task.getCreatorId());
        snapshot.put("auditorId", task.getAuditorId());
        snapshot.put("source", task.getSource());
        return snapshot;
    }

    /**
     * 仅记录发生变化的字段
     */
    private void putDiff(Map<String, Object> before, Map<String, Object> after, String key, Object oldValue,
            Object newValue)
    {
        if (newValue == null)
        {
            return;
        }
        if (!Objects.equals(normalize(oldValue), normalize(newValue)))
        {
            before.put(key, normalize(oldValue));
            after.put(key, normalize(newValue));
        }
    }

    private Object normalize(Object value)
    {
        return value == null ? "" : value;
    }

    private String requireTitle(String title)
    {
        String value = trimToNull(title);
        if (value == null)
        {
            throw new ServiceException("请填写任务标题", HttpStatus.BAD_REQUEST);
        }
        if (value.length() < 2 || value.length() > 200)
        {
            throw new ServiceException("标题需为 2–200 个字符", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private void validateDescription(String description)
    {
        if (description != null && description.length() > 2000)
        {
            throw new ServiceException("任务描述最多 2000 字", HttpStatus.BAD_REQUEST);
        }
    }

    private void validatePriority(String priority)
    {
        if (StringUtils.isEmpty(priority))
        {
            return;
        }
        if (!PRIORITY_LABELS.containsKey(priority))
        {
            throw new ServiceException("请选择优先级", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateTags(List<String> tags)
    {
        if (tags == null)
        {
            return;
        }
        int count = 0;
        for (String tag : tags)
        {
            if (StringUtils.isNotBlank(tag))
            {
                count++;
            }
        }
        if (count > 3)
        {
            throw new ServiceException("标签最多选择 3 个", HttpStatus.BAD_REQUEST);
        }
    }

    private Date requireDueTime(Date dueTime)
    {
        if (dueTime == null)
        {
            throw new ServiceException("请选择截止时间", HttpStatus.BAD_REQUEST);
        }
        return dueTime;
    }

    private void validateStartBeforeDue(Date startTime, Date dueTime)
    {
        if (startTime != null && dueTime != null && startTime.after(dueTime))
        {
            throw new ServiceException("开始时间不能晚于截止时间", HttpStatus.BAD_REQUEST);
        }
    }

    private String requireReason(String remark, String message)
    {
        String value = trimToNull(remark);
        if (value == null || value.length() < 2 || value.length() > 200)
        {
            throw new ServiceException(message, HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private String defaultPriority(String priority)
    {
        return StringUtils.isEmpty(priority) ? "1" : priority;
    }

    private String joinTags(List<String> tags)
    {
        if (tags == null || tags.isEmpty())
        {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        Set<String> unique = new LinkedHashSet<String>();
        for (String tag : tags)
        {
            if (StringUtils.isNotBlank(tag))
            {
                unique.add(tag.trim());
            }
        }
        int index = 0;
        for (String tag : unique)
        {
            if (index++ > 0)
            {
                builder.append(",");
            }
            builder.append(tag);
        }
        return builder.toString();
    }

    private String joinIds(List<Long> ids)
    {
        if (ids == null || ids.isEmpty())
        {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ids.size(); i++)
        {
            if (i > 0)
            {
                builder.append(",");
            }
            builder.append(ids.get(i));
        }
        return builder.toString();
    }

    private String overdueFlagOf(Date dueTime, String status)
    {
        boolean unfinished = OaTaskStateMachine.STATUS_TODO.equals(status)
                || OaTaskStateMachine.STATUS_DOING.equals(status);
        return unfinished && dueTime != null && dueTime.before(new Date()) ? "1" : "0";
    }

    private String projectName(Long projectId)
    {
        if (projectId == null)
        {
            return null;
        }
        return taskMapper.selectProjectName(projectId);
    }

    private void addIfNotNull(Set<Long> set, Long value)
    {
        if (value != null)
        {
            set.add(value);
        }
    }

    private String label(Map<String, String> labels, String key)
    {
        if (labels == null || key == null)
        {
            return "";
        }
        String label = labels.get(key);
        return label == null ? "" : label;
    }

    private String trimToNull(String value)
    {
        if (StringUtils.isBlank(value))
        {
            return null;
        }
        return value.trim();
    }

    private String defaultString(String value)
    {
        return value == null ? "" : value;
    }

    private String formatDateTime(Date date)
    {
        if (date == null)
        {
            return "";
        }
        return new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }
}
