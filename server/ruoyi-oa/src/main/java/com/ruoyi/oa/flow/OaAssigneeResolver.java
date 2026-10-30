package com.ruoyi.oa.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.constant.OaConstants;
import com.ruoyi.oa.mapper.OaAssigneeMapper;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.system.service.ISysDeptService;

/**
 * 审批人解析器
 *
 * <p>BPMN 里以 {@code ${oaAssigneeResolver.deptLeader(applyDeptId)}} 形式调用。
 * 把"节点该派给谁"这个业务规则从流程定义里剥离出来，改规则只改这里。</p>
 *
 * <p>解析不到人时兜底指派 admin，保证流程不会因为缺人卡死——
 * 生产环境应该改为抛异常或转交管理员，这里为了演示可跑通先兜底。</p>
 *
 * @author ruoyi-oa
 */
@Component("oaAssigneeResolver")
public class OaAssigneeResolver
{
    private static final Logger log = LoggerFactory.getLogger(OaAssigneeResolver.class);

    /** 防死循环的最大向上查找层数 */
    private static final int MAX_DEPT_DEPTH = 20;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private OaAssigneeMapper assigneeMapper;

    /**
     * 部门负责人：从申请部门开始逐级向上找第一个配置了负责人的部门。
     *
     * <p>注意：若依的 sys_dept.leader 存的是姓名（varchar），不是用户 ID，
     * 因此需要用 nick_name 反查 sys_user。</p>
     */
    public String deptLeader(Long deptId)
    {
        Long current = deptId;
        int depth = 0;
        while (current != null && current > 0 && depth++ < MAX_DEPT_DEPTH)
        {
            SysDept dept = deptService.selectDeptById(current);
            if (dept == null)
            {
                break;
            }
            String leader = dept.getLeader();
            if (StringUtils.isNotEmpty(leader))
            {
                Long userId = assigneeMapper.selectUserIdByNickName(leader);
                if (userId != null)
                {
                    return String.valueOf(userId);
                }
                log.warn("部门[{}]负责人[{}]未匹配到系统用户，继续向上查找", dept.getDeptName(), leader);
            }
            current = dept.getParentId();
        }
        log.warn("未解析到部门负责人，deptId={}，兜底指派 admin", deptId);
        return OaConstants.FALLBACK_ASSIGNEE;
    }

    /**
     * 按角色 key 取该角色下第一个在职用户（人事、总经理用）
     */
    public String roleUser(String roleKey)
    {
        Long userId = assigneeMapper.selectUserIdByRoleKey(roleKey);
        if (userId != null)
        {
            return String.valueOf(userId);
        }
        log.warn("角色[{}]下没有可用用户，兜底指派 admin", roleKey);
        return OaConstants.FALLBACK_ASSIGNEE;
    }
}
