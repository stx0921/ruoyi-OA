package com.ruoyi.oa.member.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.member.domain.OaDeptLeader;
import com.ruoyi.oa.member.dto.DeptLeaderDTO;
import com.ruoyi.oa.member.dto.DeptSaveDTO;
import com.ruoyi.oa.member.mapper.OaDeptLeaderMapper;
import com.ruoyi.oa.member.mapper.OaDeptMapper;
import com.ruoyi.oa.member.mapper.OaMemberMapper;
import com.ruoyi.oa.member.service.IOaDeptService;
import com.ruoyi.oa.member.vo.DeptLeaderVO;
import com.ruoyi.oa.member.vo.DeptOptionVO;
import com.ruoyi.oa.member.vo.DeptVO;
import com.ruoyi.oa.support.OaCurrentUser;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysDeptService;

/**
 * 部门与负责人 Service 实现
 *
 * @author ruoyi-oa
 */
@Service
public class OaDeptServiceImpl implements IOaDeptService
{
    private static final Logger log = LoggerFactory.getLogger(OaDeptServiceImpl.class);

    /** 系统参数：组织架构根部门 ID（防止上级部门改名后找不到组织架构） */
    private static final String CONFIG_ORG_ROOT_ID = "oa.org.rootDeptId";

    /** 初始化脚本创建的根部门名称，仅作兜底 */
    private static final String LEGACY_ORG_ROOT_NAME = "校学生会";

    @Autowired
    private OaDeptMapper deptMapper;

    @Autowired
    private OaDeptLeaderMapper deptLeaderMapper;

    @Autowired
    private OaMemberMapper memberMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private OaCurrentUser currentUser;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysDeptService sysDeptService;

    @Override
    public Long resolveOrgRootId()
    {
        // 1) 优先读系统参数（上级部门改名也不受影响）
        Long fromConfig = parseLong(safeConfig(CONFIG_ORG_ROOT_ID));
        if (fromConfig != null && isActiveDept(fromConfig))
        {
            return fromConfig;
        }
        // 2) 兜底：默认安装时的根部门名称
        Long byName = deptMapper.selectRootDeptIdByName(LEGACY_ORG_ROOT_NAME);
        if (byName != null)
        {
            return byName;
        }
        // 3) 兜底：存在启用子部门的根节点中 ID 最大的一个（组织树通常晚于若依默认部门创建）
        Long fallback = deptMapper.selectRootDeptIdWithActiveChild();
        if (fallback != null)
        {
            log.warn("[OA部门] 未匹配到组织根部门（名称已改或参数未配置），自动使用根部门 deptId={}，建议在参数设置中配置 {}",
                    fallback, CONFIG_ORG_ROOT_ID);
            return fallback;
        }
        throw new ServiceException("未找到组织架构根部门，请到「系统管理 → 部门管理」确认部门结构，"
                + "或在参数设置中配置 " + CONFIG_ORG_ROOT_ID, 500);
    }

    @Override
    public List<DeptVO> selectDeptWithLeader()
    {
        Long rootId = resolveOrgRootId();
        List<DeptVO> depts = deptMapper.selectOrgDeptList(rootId);
        List<OaDeptLeader> leaders = deptLeaderMapper.selectAllLeaders();
        Map<Long, List<OaDeptLeader>> leaderMap = new HashMap<>();
        for (OaDeptLeader leader : leaders)
        {
            leaderMap.computeIfAbsent(leader.getDeptId(), key -> new ArrayList<>()).add(leader);
        }
        for (DeptVO dept : depts)
        {
            List<OaDeptLeader> deptLeaders = leaderMap.get(dept.getDeptId());
            if (deptLeaders == null)
            {
                continue;
            }
            List<DeptLeaderVO> vices = new ArrayList<>();
            for (OaDeptLeader leader : deptLeaders)
            {
                SysUser user = userMapper.selectUserById(leader.getUserId());
                String name = user == null ? "已删除账号" : user.getNickName();
                if ("1".equals(leader.getLeaderType()) && dept.getLeaderUserId() == null)
                {
                    dept.setLeaderUserId(leader.getUserId());
                    dept.setLeaderName(name);
                }
                else
                {
                    DeptLeaderVO vice = new DeptLeaderVO();
                    vice.setUserId(leader.getUserId());
                    vice.setUserName(name);
                    vice.setLeaderType(leader.getLeaderType());
                    vice.setSort(leader.getSort());
                    vices.add(vice);
                }
            }
            dept.setViceLeaders(vices);
        }
        return depts;
    }

    @Override
    public List<DeptOptionVO> selectDeptOptions()
    {
        List<DeptOptionVO> options = selectAllDeptOptions();
        // 管理员 / 主席团可见全部；部长 / 干事仅见本部门（与数据权限口径一致）
        if (currentUser.isAdmin() || currentUser.hasRole(OaMemberConstants.ROLE_PRESIDENT))
        {
            return options;
        }
        Long myDeptId = currentUser.user() == null ? null : currentUser.user().getDeptId();
        List<DeptOptionVO> filtered = new ArrayList<>();
        if (myDeptId != null)
        {
            for (DeptOptionVO option : options)
            {
                if (myDeptId.equals(option.getDeptId()))
                {
                    filtered.add(option);
                }
            }
        }
        return filtered;
    }

    @Override
    public List<DeptOptionVO> selectAllDeptOptions()
    {
        return deptMapper.selectOrgDeptOptions(resolveOrgRootId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDept(DeptSaveDTO dto)
    {
        requireManage();
        String deptName = dto == null ? null : StringUtils.trim(dto.getDeptName());
        if (StringUtils.isEmpty(deptName))
        {
            throw new ServiceException("部门名称不能为空", 400);
        }
        Long rootId = resolveOrgRootId();
        SysDept parent = sysDeptService.selectDeptById(rootId);
        if (parent == null)
        {
            throw new ServiceException("组织架构根部门不存在", 400);
        }
        SysDept dept = new SysDept();
        dept.setParentId(rootId);
        dept.setDeptName(deptName);
        dept.setOrderNum(dto.getOrderNum() == null ? 0 : dto.getOrderNum());
        dept.setStatus(StringUtils.isEmpty(dto.getStatus()) ? "0" : dto.getStatus());
        dept.setDelFlag("0");
        dept.setCreateBy(currentUser.username());
        if (!sysDeptService.checkDeptNameUnique(dept))
        {
            throw new ServiceException("部门名称「" + deptName + "」已存在", 400);
        }
        sysDeptService.insertDept(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(DeptSaveDTO dto)
    {
        requireManage();
        if (dto == null || dto.getDeptId() == null)
        {
            throw new ServiceException("部门 ID 不能为空", 400);
        }
        String deptName = StringUtils.trim(dto.getDeptName());
        if (StringUtils.isEmpty(deptName))
        {
            throw new ServiceException("部门名称不能为空", 400);
        }
        Long rootId = resolveOrgRootId();
        SysDept old = sysDeptService.selectDeptById(dto.getDeptId());
        if (old == null || !"0".equals(old.getDelFlag()))
        {
            throw new ServiceException("部门不存在", 404);
        }
        if (!rootId.equals(old.getParentId()))
        {
            throw new ServiceException("只能维护组织架构下的内设部门", 403);
        }
        SysDept dept = new SysDept();
        dept.setDeptId(dto.getDeptId());
        dept.setParentId(rootId);
        dept.setDeptName(deptName);
        dept.setOrderNum(dto.getOrderNum() == null ? 0 : dto.getOrderNum());
        dept.setStatus(StringUtils.isEmpty(dto.getStatus()) ? "0" : dto.getStatus());
        dept.setUpdateBy(currentUser.username());
        if (!sysDeptService.checkDeptNameUnique(dept))
        {
            throw new ServiceException("部门名称「" + deptName + "」已存在", 400);
        }
        sysDeptService.updateDept(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long deptId)
    {
        requireManage();
        if (deptId == null)
        {
            throw new ServiceException("部门 ID 不能为空", 400);
        }
        Long rootId = resolveOrgRootId();
        if (rootId.equals(deptId))
        {
            throw new ServiceException("组织架构根部门不允许删除", 400);
        }
        SysDept dept = sysDeptService.selectDeptById(deptId);
        if (dept == null || !"0".equals(dept.getDelFlag()))
        {
            throw new ServiceException("部门不存在", 404);
        }
        if (!rootId.equals(dept.getParentId()))
        {
            throw new ServiceException("只能删除组织架构下的内设部门", 403);
        }
        if (sysDeptService.hasChildByDeptId(deptId))
        {
            throw new ServiceException("部门「" + dept.getDeptName() + "」存在下级部门，不允许删除", 400);
        }
        int members = deptMapper.countActiveMemberByDeptId(deptId);
        if (members > 0)
        {
            throw new ServiceException("部门「" + dept.getDeptName() + "」下还有 " + members
                    + " 名成员，请先调整成员所属部门后再删除", 400);
        }
        if (sysDeptService.checkDeptExistUser(deptId))
        {
            throw new ServiceException("部门「" + dept.getDeptName() + "」下存在系统用户，不允许删除", 400);
        }
        deptLeaderMapper.deleteByDeptId(deptId);
        sysDeptService.deleteDeptById(deptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLeaders(DeptLeaderDTO dto)
    {
        if (dto.getDeptId() == null)
        {
            throw new ServiceException("部门不能为空", 400);
        }
        List<DeptOptionVO> depts = deptMapper.selectOrgDeptOptions(resolveOrgRootId());
        boolean exists = false;
        for (DeptOptionVO dept : depts)
        {
            if (dto.getDeptId().equals(dept.getDeptId()))
            {
                exists = true;
                break;
            }
        }
        if (!exists)
        {
            throw new ServiceException("部门不存在或已停用", 400);
        }

        List<OaDeptLeader> toInsert = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        if (dto.getLeaders() != null)
        {
            for (DeptLeaderDTO.LeaderItem item : dto.getLeaders())
            {
                if (item == null || item.getUserId() == null)
                {
                    continue;
                }
                if (!"1".equals(item.getLeaderType()) && !"2".equals(item.getLeaderType()))
                {
                    throw new ServiceException("负责人类型不合法", 400);
                }
                if (!seen.add(item.getUserId()))
                {
                    throw new ServiceException("副部长不能重复选择", 400);
                }
                int active = memberMapper.countActiveMemberByUserIdAndDept(item.getUserId(), dto.getDeptId());
                if (active <= 0)
                {
                    throw new ServiceException("负责人必须是本部门在籍成员", 400);
                }
                OaDeptLeader leader = new OaDeptLeader();
                leader.setDeptId(dto.getDeptId());
                leader.setUserId(item.getUserId());
                leader.setLeaderType(item.getLeaderType());
                leader.setSort(item.getSort() == null ? 0 : item.getSort());
                leader.setCreateBy(currentUser.username());
                toInsert.add(leader);
            }
        }
        deptLeaderMapper.deleteByDeptId(dto.getDeptId());
        String primaryName = "";
        for (OaDeptLeader leader : toInsert)
        {
            deptLeaderMapper.insertLeader(leader);
            if ("1".equals(leader.getLeaderType()) && StringUtils.isEmpty(primaryName))
            {
                SysUser user = userMapper.selectUserById(leader.getUserId());
                primaryName = user == null ? "" : user.getNickName();
            }
        }
        deptMapper.updateDeptLeader(dto.getDeptId(), primaryName);
    }

    /**
     * 部门维护权限：管理员 / 主席团
     */
    private void requireManage()
    {
        if (!currentUser.isAdmin() && !currentUser.hasRole(OaMemberConstants.ROLE_PRESIDENT))
        {
            throw new ServiceException("仅管理员 / 主席团可维护部门", 403);
        }
    }

    private boolean isActiveDept(Long deptId)
    {
        if (deptId == null)
        {
            return false;
        }
        SysDept dept = sysDeptService.selectDeptById(deptId);
        return dept != null && "0".equals(dept.getDelFlag());
    }

    private String safeConfig(String key)
    {
        try
        {
            return configService.selectConfigByKey(key);
        }
        catch (Exception e)
        {
            log.warn("[OA部门] 读取参数 {} 失败: {}", key, e.getMessage());
            return null;
        }
    }

    private Long parseLong(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return null;
        }
        try
        {
            return Long.valueOf(value.trim());
        }
        catch (NumberFormatException e)
        {
            log.warn("[OA部门] 参数 {} 的值「{}」不是合法部门 ID", CONFIG_ORG_ROOT_ID, value);
            return null;
        }
    }
}
