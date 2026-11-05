package com.ruoyi.oa.task.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.support.OaCurrentUser;
import com.ruoyi.oa.task.domain.OaTask;
import com.ruoyi.oa.task.mapper.OaTaskCollaboratorMapper;
import com.ruoyi.oa.task.mapper.OaTaskMapper;
import com.ruoyi.oa.task.vo.UserBriefVO;

/**
 * 任务权限校验器（可见性 / 派发范围 / 跨部门 / 验收人 / 归档只读）
 *
 * <p>列表与看板走 {@code @DataScope} 做数据范围过滤；详情与写操作在这里做第二层逐条校验，防改 URL 越权。</p>
 *
 * @author ruoyi-oa
 */
@Component
public class OaTaskPermissionChecker
{
    /** 协作人上限 */
    private static final int COLLABORATOR_MAX = 10;

    @Autowired
    private OaTaskMapper taskMapper;

    @Autowired
    private OaTaskCollaboratorMapper collaboratorMapper;

    @Autowired
    private OaCurrentUser currentUser;

    /**
     * 当前登录人权限上下文（一次请求只解析一次角色，避免重复查库）
     */
    public Ctx context()
    {
        Ctx ctx = new Ctx();
        SysUser user = currentUser.user();
        ctx.userId = user != null ? user.getUserId() : currentUser.userId();
        ctx.deptId = user == null ? null : user.getDeptId();
        ctx.admin = user != null && user.isAdmin();
        Set<String> roles = ctx.admin ? new LinkedHashSet<String>() : currentUser.roleKeys();
        ctx.president = roles.contains(OaMemberConstants.ROLE_PRESIDENT);
        ctx.minister = roles.contains(OaMemberConstants.ROLE_MINISTER);
        ctx.staff = !ctx.admin && !ctx.president && !ctx.minister;
        return ctx;
    }

    /**
     * 归档项目只读：所有任务写操作第一行调用（409）
     */
    public void assertActiveProject(Long projectId)
    {
        if (isProjectFinished(projectId))
        {
            throw new ServiceException("项目已结束，任务只读", HttpStatus.CONFLICT);
        }
    }

    /**
     * 新建任务时归档项目拒绝（400）
     */
    public void assertProjectOpenForCreate(Long projectId)
    {
        if (isProjectFinished(projectId))
        {
            throw new ServiceException("项目已结束，不能新建任务", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 项目是否已结束（项目不存在按未结束处理）
     */
    public boolean isProjectFinished(Long projectId)
    {
        if (projectId == null)
        {
            return false;
        }
        return "1".equals(taskMapper.selectProjectStatus(projectId));
    }

    /**
     * 派发范围校验：admin/president 任意；本人任意；部长本部门或项目内跨部门；干事不能派他人
     */
    public void assertDispatch(Ctx ctx, Long targetUserId, Long projectId)
    {
        if (targetUserId == null)
        {
            throw new ServiceException("请选择主责人", HttpStatus.BAD_REQUEST);
        }
        if (targetUserId.equals(ctx.getUserId()))
        {
            return;
        }
        UserBriefVO target = taskMapper.selectActiveUserBrief(targetUserId);
        if (target == null)
        {
            throw new ServiceException("主责人不存在或已退会", HttpStatus.BAD_REQUEST);
        }
        if (ctx.isAdmin() || ctx.isPresident())
        {
            return;
        }
        boolean sameDept = ctx.getDeptId() != null && ctx.getDeptId().equals(target.getDeptId());
        if (sameDept)
        {
            return;
        }
        if (ctx.isMinister())
        {
            // 部长仅当任务挂在项目下且目标人是项目参与成员时允许跨部门
            if (projectId != null && taskMapper.countProjectMember(projectId, targetUserId) > 0)
            {
                return;
            }
            throw new ServiceException("跨部门派发需在项目内", HttpStatus.FORBIDDEN);
        }
        throw new ServiceException("干事不能给他人派发任务", HttpStatus.FORBIDDEN);
    }

    /**
     * 协作人校验 + 归一化（去重、剔除主责人、上限 10、在册、跨部门须在项目内）
     */
    public List<Long> normalizeCollaborators(Ctx ctx, List<Long> collaboratorIds, Long projectId, Long assigneeId)
    {
        List<Long> result = new ArrayList<Long>();
        if (collaboratorIds == null || collaboratorIds.isEmpty())
        {
            return result;
        }
        Set<Long> unique = new LinkedHashSet<Long>();
        for (Long id : collaboratorIds)
        {
            if (id != null && !id.equals(assigneeId))
            {
                unique.add(id);
            }
        }
        if (unique.size() > COLLABORATOR_MAX)
        {
            throw new ServiceException("协作人最多 10 人", HttpStatus.BAD_REQUEST);
        }
        for (Long id : unique)
        {
            UserBriefVO brief = taskMapper.selectActiveUserBrief(id);
            if (brief == null)
            {
                throw new ServiceException("协作人不存在或已退会", HttpStatus.BAD_REQUEST);
            }
            boolean sameDept = ctx.getDeptId() != null && ctx.getDeptId().equals(brief.getDeptId());
            boolean inProject = projectId != null && taskMapper.countProjectMember(projectId, id) > 0;
            if (!ctx.isAdmin() && !ctx.isPresident() && !sameDept && !inProject)
            {
                throw new ServiceException("协作人需在项目成员中", HttpStatus.FORBIDDEN);
            }
            result.add(id);
        }
        return result;
    }

    /**
     * 任务是否可见：数据范围（本部门）或我参与（主责 / 派发 / 验收 / 协作）
     */
    public boolean canView(Ctx ctx, OaTask task)
    {
        if (task == null)
        {
            return false;
        }
        if (ctx.isAdmin() || ctx.isPresident())
        {
            return true;
        }
        Long me = ctx.getUserId();
        if (me.equals(task.getAssigneeId()) || me.equals(task.getCreatorId()) || me.equals(task.getAuditorId()))
        {
            return true;
        }
        if (task.getTaskId() != null && collaboratorMapper.countByTaskAndUser(task.getTaskId(), me) > 0)
        {
            return true;
        }
        return ctx.getDeptId() != null && ctx.getDeptId().equals(task.getDeptId());
    }

    /**
     * 详情可见性断言（403）
     */
    public void assertCanView(Ctx ctx, OaTask task)
    {
        if (!canView(ctx, task))
        {
            throw new ServiceException("没有访问该任务的权限", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * 是否验收人：auditor / creator / 项目负责人 / admin / 主席团
     */
    public boolean isAuditor(Ctx ctx, OaTask task)
    {
        if (ctx.isAdmin() || ctx.isPresident())
        {
            return true;
        }
        Long me = ctx.getUserId();
        if (me.equals(task.getAuditorId()) || me.equals(task.getCreatorId()))
        {
            return true;
        }
        if (task.getProjectId() == null)
        {
            return false;
        }
        Long owner = taskMapper.selectProjectOwnerId(task.getProjectId());
        return owner != null && owner.equals(me);
    }

    /** 开始：主责人 / 派发人 / admin / 主席团 */
    public void assertCanStart(Ctx ctx, OaTask task)
    {
        if (ctx.isAdmin() || ctx.isPresident() || ctx.getUserId().equals(task.getAssigneeId())
                || ctx.getUserId().equals(task.getCreatorId()))
        {
            return;
        }
        throw new ServiceException("只有主责人可以开始任务", HttpStatus.FORBIDDEN);
    }

    /** 提交完成：仅主责人（TR2） */
    public void assertCanSubmit(Ctx ctx, OaTask task)
    {
        if (ctx.getUserId().equals(task.getAssigneeId()))
        {
            return;
        }
        throw new ServiceException("只有主责人可以提交完成", HttpStatus.FORBIDDEN);
    }

    /** 验收 / 打回：验收人 / 派发人 / 项目负责人 / admin / 主席团 */
    public void assertCanAudit(Ctx ctx, OaTask task)
    {
        if (isAuditor(ctx, task))
        {
            return;
        }
        throw new ServiceException("你没有该任务的验收权限", HttpStatus.FORBIDDEN);
    }

    /** 取消：派发人 / 主责人 / admin / 主席团 */
    public void assertCanCancel(Ctx ctx, OaTask task)
    {
        if (ctx.isAdmin() || ctx.isPresident() || ctx.getUserId().equals(task.getCreatorId())
                || ctx.getUserId().equals(task.getAssigneeId()))
        {
            return;
        }
        throw new ServiceException("你没有权限取消该任务", HttpStatus.FORBIDDEN);
    }

    /** 编辑：派发人 / admin / 主席团 */
    public void assertCanEdit(Ctx ctx, OaTask task)
    {
        if (ctx.isAdmin() || ctx.isPresident() || ctx.getUserId().equals(task.getCreatorId()))
        {
            return;
        }
        throw new ServiceException("只有派发人可以编辑任务", HttpStatus.FORBIDDEN);
    }

    /** 改派：派发人 / admin / 主席团 */
    public void assertCanReassign(Ctx ctx, OaTask task)
    {
        if (ctx.isAdmin() || ctx.isPresident() || ctx.getUserId().equals(task.getCreatorId()))
        {
            return;
        }
        throw new ServiceException("只有派发人可以改派任务", HttpStatus.FORBIDDEN);
    }

    /**
     * 当前登录人权限上下文
     */
    public static class Ctx
    {
        /** 用户ID */
        private Long userId;

        /** 部门ID */
        private Long deptId;

        /** 是否管理员 */
        private boolean admin;

        /** 是否主席团 */
        private boolean president;

        /** 是否部长 */
        private boolean minister;

        /** 是否干事（无管理角色） */
        private boolean staff;

        public Long getUserId()
        {
            return userId;
        }

        public Long getDeptId()
        {
            return deptId;
        }

        public boolean isAdmin()
        {
            return admin;
        }

        public boolean isPresident()
        {
            return president;
        }

        public boolean isMinister()
        {
            return minister;
        }

        public boolean isStaff()
        {
            return staff;
        }

        /** admin 或主席团 */
        public boolean isAdminOrPresident()
        {
            return admin || president;
        }
    }
}
