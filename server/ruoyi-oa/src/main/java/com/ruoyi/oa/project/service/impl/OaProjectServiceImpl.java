package com.ruoyi.oa.project.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.ruoyi.oa.project.domain.OaProjectMember;
import com.ruoyi.oa.project.dto.ProjectQuery;
import com.ruoyi.oa.project.dto.ProjectSaveDTO;
import com.ruoyi.oa.project.mapper.OaProjectMapper;
import com.ruoyi.oa.project.mapper.OaProjectMemberMapper;
import com.ruoyi.oa.project.service.IOaProjectService;
import com.ruoyi.oa.project.support.OaProjectNoGenerator;
import com.ruoyi.oa.project.vo.ProjectDetailVO;
import com.ruoyi.oa.project.vo.ProjectListVO;
import com.ruoyi.oa.project.vo.ProjectMemberVO;
import com.ruoyi.oa.project.vo.ProjectSaveResultVO;
import com.ruoyi.oa.recur.domain.OaTaskRecur;
import com.ruoyi.oa.recur.mapper.OaTaskRecurMapper;
import com.ruoyi.oa.support.OaCurrentUser;
import com.ruoyi.oa.task.mapper.OaTaskMapper;

/**
 * 项目 Service 实现
 *
 * <p>规则：项目两态（0 进行中 / 1 已结束）；名称 2–50 字、结束日期 ≥ 开始日期、负责人必须在册且自动加入成员；
 * 结束即归档：写入 finish_time、项目下启用中的周期规则自动暂停并通知创建人；归档后编辑返回 400。</p>
 *
 * @author ruoyi-oa
 */
@Service
public class OaProjectServiceImpl implements IOaProjectService
{
    private static final Logger log = LoggerFactory.getLogger(OaProjectServiceImpl.class);

    /** 项目状态文案 */
    private static final Map<String, String> STATUS_LABELS = new HashMap<String, String>();

    static
    {
        STATUS_LABELS.put("0", "进行中");
        STATUS_LABELS.put("1", "已结束");
    }

    @Autowired
    private OaProjectMapper projectMapper;

    @Autowired
    private OaProjectMemberMapper projectMemberMapper;

    @Autowired
    private OaProjectNoGenerator projectNoGenerator;

    @Autowired
    private OaMemberMapper memberMapper;

    @Autowired
    private OaTaskRecurMapper recurMapper;

    @Autowired
    private OaTaskMapper taskMapper;

    @Autowired
    private IOaNotifyService notifyService;

    @Autowired
    private OaCurrentUser currentUser;

    @Override
    @DataScope(deptAlias = "p")
    public List<ProjectListVO> selectProjectList(ProjectQuery query)
    {
        ProjectQuery condition = query == null ? new ProjectQuery() : query;
        List<OaProject> list = projectMapper.selectProjectList(condition);
        List<ProjectListVO> result = new ArrayList<ProjectListVO>(list.size());
        for (OaProject project : list)
        {
            ProjectListVO vo = new ProjectListVO();
            fillListVO(vo, project);
            result.add(vo);
        }
        // 保留 PageHelper 分页信息（转换成 VO 后 getDataTable 仍能取到 total）
        if (list instanceof Page)
        {
            Page<OaProject> sourcePage = (Page<OaProject>) list;
            Page<ProjectListVO> voPage = new Page<ProjectListVO>(sourcePage.getPageNum(), sourcePage.getPageSize());
            voPage.setTotal(sourcePage.getTotal());
            voPage.addAll(result);
            return voPage;
        }
        return result;
    }

    @Override
    public ProjectDetailVO selectProjectDetail(Long projectId)
    {
        if (projectId == null)
        {
            throw new ServiceException("项目不存在", 404);
        }
        OaProject project = projectMapper.selectProjectById(projectId);
        if (project == null)
        {
            throw new ServiceException("项目不存在", 404);
        }
        ProjectDetailVO vo = new ProjectDetailVO();
        fillListVO(vo, project);
        List<OaProjectMember> members = projectMemberMapper.selectByProjectId(projectId);
        List<ProjectMemberVO> memberVos = new ArrayList<ProjectMemberVO>(members.size());
        for (OaProjectMember member : members)
        {
            ProjectMemberVO memberVo = new ProjectMemberVO();
            memberVo.setUserId(member.getUserId());
            memberVo.setUserName(member.getUserName());
            memberVo.setNickName(member.getNickName());
            memberVo.setDeptName(member.getDeptName());
            memberVo.setMemberId(member.getMemberId());
            memberVos.add(memberVo);
        }
        vo.setMembers(memberVos);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectSaveResultVO insertProject(ProjectSaveDTO dto)
    {
        if (dto == null)
        {
            throw new ServiceException("请填写项目信息", 400);
        }
        String name = validateName(dto.getName());
        Date startDate = requireDate(dto.getStartDate(), "请选择开始日期");
        Date endDate = requireDate(dto.getEndDate(), "请选择结束日期");
        if (startDate.after(endDate))
        {
            throw new ServiceException("结束日期不能早于开始日期", 400);
        }
        String description = validateDescription(dto.getDescription());
        OaMember owner = requireActiveMember(dto.getOwnerId(), "负责人");
        Map<Long, OaMember> memberMap = loadActiveMembers(normalizeMemberIds(dto.getMemberIds(), owner.getUserId()));
        String operator = currentUser.username();

        OaProject project = new OaProject();
        project.setProjectNo(projectNoGenerator.nextProjectNo(new Date()));
        project.setName(name);
        project.setOwnerId(owner.getUserId());
        project.setOwnerName(StringUtils.nvl(owner.getName(), ""));
        project.setStatus("0");
        project.setStartDate(startDate);
        project.setEndDate(endDate);
        project.setDescription(description);
        project.setDeptId(owner.getDeptId());
        project.setCreateBy(operator);
        projectMapper.insertProject(project);
        insertMembers(project.getProjectId(), memberMap, operator);

        ProjectSaveResultVO result = new ProjectSaveResultVO();
        result.setProjectId(project.getProjectId());
        result.setProjectNo(project.getProjectNo());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(ProjectSaveDTO dto)
    {
        if (dto == null || dto.getProjectId() == null)
        {
            throw new ServiceException("项目不存在", 404);
        }
        OaProject exist = projectMapper.selectProjectByIdForUpdate(dto.getProjectId());
        if (exist == null)
        {
            throw new ServiceException("项目不存在", 404);
        }
        if ("1".equals(exist.getStatus()))
        {
            throw new ServiceException("项目已结束，不可编辑", 400);
        }
        String name = validateName(dto.getName());
        Date startDate = requireDate(dto.getStartDate(), "请选择开始日期");
        Date endDate = requireDate(dto.getEndDate(), "请选择结束日期");
        if (startDate.after(endDate))
        {
            throw new ServiceException("结束日期不能早于开始日期", 400);
        }
        String description = validateDescription(dto.getDescription());
        OaMember owner = requireActiveMember(dto.getOwnerId(), "负责人");
        Map<Long, OaMember> memberMap = loadActiveMembers(normalizeMemberIds(dto.getMemberIds(), owner.getUserId()));
        String operator = currentUser.username();

        OaProject project = new OaProject();
        project.setProjectId(exist.getProjectId());
        project.setName(name);
        project.setOwnerId(owner.getUserId());
        project.setOwnerName(StringUtils.nvl(owner.getName(), ""));
        project.setStartDate(startDate);
        project.setEndDate(endDate);
        project.setDescription(description);
        project.setDeptId(owner.getDeptId());
        project.setUpdateBy(operator);
        int rows = projectMapper.updateProject(project);
        if (rows == 0)
        {
            throw new ServiceException("项目状态已变化，请刷新后重试", 409);
        }
        projectMemberMapper.deleteByProjectId(exist.getProjectId());
        insertMembers(exist.getProjectId(), memberMap, operator);
    }

    @Override
    public int prepareFinish(Long projectId)
    {
        if (projectId == null)
        {
            throw new ServiceException("项目不存在", 404);
        }
        OaProject project = projectMapper.selectProjectById(projectId);
        if (project == null)
        {
            throw new ServiceException("项目不存在", 404);
        }
        if ("1".equals(project.getStatus()))
        {
            throw new ServiceException("项目已结束", 400);
        }
        checkFinishPermission(project);
        return projectMapper.countUnfinishedTasks(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int finishProject(Long projectId)
    {
        if (projectId == null)
        {
            throw new ServiceException("项目不存在", 404);
        }
        OaProject project = projectMapper.selectProjectByIdForUpdate(projectId);
        if (project == null)
        {
            throw new ServiceException("项目不存在", 404);
        }
        if ("1".equals(project.getStatus()))
        {
            throw new ServiceException("项目已结束", 400);
        }
        checkFinishPermission(project);
        int unfinishedCount = projectMapper.countUnfinishedTasks(projectId);
        String operator = currentUser.username();
        int rows = projectMapper.finishProject(projectId, new Date(), operator);
        if (rows == 0)
        {
            throw new ServiceException("项目状态已变化，请刷新后重试", 409);
        }
        // 归档：自动暂停项目下启用中的周期规则并通知规则创建人
        List<OaTaskRecur> rules = recurMapper.selectActiveByProject(projectId);
        for (OaTaskRecur rule : rules)
        {
            int updated = recurMapper.updateStatus(rule.getRecurId(), "1", null);
            if (updated > 0 && rule.getCreatorId() != null)
            {
                notifyService.notify(rule.getCreatorId(), "5", "周期规则已暂停",
                        "项目「" + project.getName() + "」已结束，规则「" + rule.getTitle() + "」已自动暂停",
                        "recur", rule.getRecurId(), "project-finish:recur:" + rule.getRecurId());
            }
        }
        log.info("项目[{}]已结束归档，未完成任务数={}，自动暂停周期规则数={}", projectId, unfinishedCount, rules.size());
        return unfinishedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProjects(Long[] projectIds)
    {
        if (projectIds == null || projectIds.length == 0)
        {
            throw new ServiceException("请选择要删除的项目", 400);
        }
        // 1) 权限与存在性校验（不存在/已删除的 ID 跳过，幂等）
        for (Long projectId : projectIds)
        {
            if (projectId == null)
            {
                continue;
            }
            OaProject project = projectMapper.selectProjectById(projectId);
            if (project == null)
            {
                continue;
            }
            checkDeletePermission(project);
        }
        // 2) 关联数据前置校验：项目下还有任务或周期规则时拒绝删除
        int taskCount = taskMapper.countByProjectIds(projectIds);
        int recurCount = recurMapper.countByProjectIds(projectIds);
        if (taskCount > 0 || recurCount > 0)
        {
            throw new ServiceException(buildDeleteBlockedMessage(taskCount, recurCount), 409);
        }
        // 3) 级联删除：参与成员 → 项目 → 项目站内信
        projectMemberMapper.deleteByProjectIds(projectIds);
        projectMapper.deleteByIds(projectIds);
        for (Long projectId : projectIds)
        {
            if (projectId != null)
            {
                notifyService.deleteByBiz("project", projectId);
            }
        }
        log.info("批量删除项目完成：requestIds={}", projectIds.length);
    }

    // ============================ 内部方法 ============================

    /**
     * 删除项目权限：管理员/主席团任意；部长仅本部门项目
     */
    private void checkDeletePermission(OaProject project)
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
        if (minister && me.getDeptId() != null && me.getDeptId().equals(project.getDeptId()))
        {
            return;
        }
        throw new ServiceException("没有权限删除该项目", 403);
    }

    /**
     * 项目删除被拦截提示：项目下还有 N 个任务、M 条周期规则，请先删除
     */
    private String buildDeleteBlockedMessage(int taskCount, int recurCount)
    {
        StringBuilder message = new StringBuilder("项目下还有 ");
        if (taskCount > 0)
        {
            message.append(taskCount).append(" 个任务");
        }
        if (taskCount > 0 && recurCount > 0)
        {
            message.append("、");
        }
        if (recurCount > 0)
        {
            message.append(recurCount).append(" 条周期规则");
        }
        message.append("，请先删除");
        return message.toString();
    }

    /**
     * 结束项目权限：项目负责人 / 主席团 / 管理员
     */
    private void checkFinishPermission(OaProject project)
    {
        SysUser me = currentUser.user();
        if (me == null)
        {
            throw new ServiceException("登录已过期，请重新登录", 401);
        }
        boolean allowed = me.isAdmin() || currentUser.hasRole(OaMemberConstants.ROLE_PRESIDENT)
                || (project.getOwnerId() != null && project.getOwnerId().equals(me.getUserId()));
        if (!allowed)
        {
            throw new ServiceException("只有项目负责人或主席团可以结束项目", 403);
        }
    }

    /**
     * 名称校验：非空、2–50 字
     */
    private String validateName(String name)
    {
        String value = StringUtils.trim(name);
        if (StringUtils.isEmpty(value))
        {
            throw new ServiceException("请填写项目名称", 400);
        }
        if (value.length() < 2 || value.length() > 50)
        {
            throw new ServiceException("项目名称需为 2–50 个字符", 400);
        }
        return value;
    }

    private Date requireDate(Date date, String message)
    {
        if (date == null)
        {
            throw new ServiceException(message, 400);
        }
        return date;
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
            throw new ServiceException("项目描述最多 2000 字", 400);
        }
        return value;
    }

    /**
     * 校验账号对应在册成员（未退会、未毕业、未软删除）
     */
    private OaMember requireActiveMember(Long userId, String label)
    {
        if (userId == null)
        {
            throw new ServiceException("请选择" + label, 400);
        }
        OaMember member = memberMapper.selectByUserId(userId);
        if (member == null || !OaMemberConstants.MEMBER_STATUS_ACTIVE.equals(member.getStatus()))
        {
            throw new ServiceException(label + "必须是未退会成员", 400);
        }
        return member;
    }

    /**
     * 成员 ID 去重 + 自动补负责人
     */
    private Set<Long> normalizeMemberIds(List<Long> memberIds, Long ownerId)
    {
        Set<Long> result = new LinkedHashSet<Long>();
        if (memberIds != null)
        {
            for (Long userId : memberIds)
            {
                if (userId != null)
                {
                    result.add(userId);
                }
            }
        }
        // 负责人必须加入参与成员
        result.add(ownerId);
        return result;
    }

    /**
     * 批量加载在册成员（含校验）
     */
    private Map<Long, OaMember> loadActiveMembers(Set<Long> userIds)
    {
        Map<Long, OaMember> result = new HashMap<Long, OaMember>();
        for (Long userId : userIds)
        {
            OaMember member = memberMapper.selectByUserId(userId);
            if (member == null || !OaMemberConstants.MEMBER_STATUS_ACTIVE.equals(member.getStatus()))
            {
                throw new ServiceException("参与成员必须是未退会成员（用户 " + userId + "）", 400);
            }
            result.put(userId, member);
        }
        return result;
    }

    /**
     * 批量写入项目参与成员
     */
    private void insertMembers(Long projectId, Map<Long, OaMember> memberMap, String operator)
    {
        if (memberMap.isEmpty())
        {
            return;
        }
        Date now = new Date();
        List<OaProjectMember> members = new ArrayList<OaProjectMember>(memberMap.size());
        for (OaMember member : memberMap.values())
        {
            OaProjectMember item = new OaProjectMember();
            item.setProjectId(projectId);
            item.setUserId(member.getUserId());
            item.setMemberId(member.getMemberId());
            item.setJoinTime(now);
            item.setCreateBy(operator);
            members.add(item);
        }
        projectMemberMapper.insertBatch(members);
    }

    /**
     * 列表字段装配
     */
    private void fillListVO(ProjectListVO vo, OaProject project)
    {
        vo.setProjectId(project.getProjectId());
        vo.setProjectNo(project.getProjectNo());
        vo.setName(project.getName());
        vo.setOwnerId(project.getOwnerId());
        vo.setOwnerName(project.getOwnerName());
        vo.setStatus(project.getStatus());
        vo.setStatusLabel(STATUS_LABELS.get(project.getStatus()));
        vo.setStartDate(project.getStartDate());
        vo.setEndDate(project.getEndDate());
        vo.setFinishTime(project.getFinishTime());
        vo.setDescription(project.getDescription());
        vo.setDeptId(project.getDeptId());
        vo.setDeptName(project.getDeptName());
        vo.setMemberCount(project.getMemberCount());
        vo.setTaskCount(project.getTaskCount());
        vo.setDoneCount(project.getDoneCount());
        vo.setCreateTime(project.getCreateTime());
    }
}
