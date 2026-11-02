package com.ruoyi.oa.member.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.member.domain.OaDeptLeader;
import com.ruoyi.oa.member.dto.DeptLeaderDTO;
import com.ruoyi.oa.member.mapper.OaDeptLeaderMapper;
import com.ruoyi.oa.member.mapper.OaDeptMapper;
import com.ruoyi.oa.member.mapper.OaMemberMapper;
import com.ruoyi.oa.member.service.IOaDeptService;
import com.ruoyi.oa.member.vo.DeptLeaderVO;
import com.ruoyi.oa.member.vo.DeptOptionVO;
import com.ruoyi.oa.member.vo.DeptVO;
import com.ruoyi.oa.support.OaCurrentUser;
import com.ruoyi.system.mapper.SysUserMapper;

/**
 * 部门与负责人 Service 实现
 *
 * @author ruoyi-oa
 */
@Service
public class OaDeptServiceImpl implements IOaDeptService
{
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

    @Override
    public List<DeptVO> selectDeptWithLeader()
    {
        List<DeptVO> depts = deptMapper.selectOrgDeptList();
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
        List<DeptOptionVO> options = deptMapper.selectOrgDeptOptions();
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
    @Transactional(rollbackFor = Exception.class)
    public void saveLeaders(DeptLeaderDTO dto)
    {
        if (dto.getDeptId() == null)
        {
            throw new ServiceException("部门不能为空", 400);
        }
        List<DeptOptionVO> depts = deptMapper.selectOrgDeptOptions();
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
}
