package com.ruoyi.oa.member.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.member.domain.OaMember;
import com.ruoyi.oa.member.domain.OaMemberLog;
import com.ruoyi.oa.member.dto.MemberAddDTO;
import com.ruoyi.oa.member.dto.MemberQuery;
import com.ruoyi.oa.member.dto.MemberSelfDTO;
import com.ruoyi.oa.member.dto.MemberStatusDTO;
import com.ruoyi.oa.member.dto.MemberTransferDTO;
import com.ruoyi.oa.member.dto.MemberUpdateDTO;
import com.ruoyi.oa.member.mapper.OaAccountMapper;
import com.ruoyi.oa.member.mapper.OaMemberLogMapper;
import com.ruoyi.oa.member.mapper.OaMemberMapper;
import com.ruoyi.oa.member.service.IOaMemberService;
import com.ruoyi.oa.member.vo.MemberDetailVO;
import com.ruoyi.oa.member.vo.MemberListVO;
import com.ruoyi.oa.member.vo.MemberStatsVO;
import com.ruoyi.oa.support.MemberAccountService;
import com.ruoyi.oa.support.MemberNoGenerator;
import com.ruoyi.oa.support.OaCurrentUser;
import com.ruoyi.oa.support.OaFileUrlHelper;
import com.ruoyi.oa.support.OaLockService;
import com.ruoyi.oa.support.OaMemberLogService;
import com.ruoyi.oa.support.OaMemberVoAssembler;
import com.ruoyi.oa.support.RoleSyncService;
import com.ruoyi.system.service.ISysDeptService;

/**
 * 成员档案 Service 实现
 *
 * @author ruoyi-oa
 */
@Service
public class OaMemberServiceImpl implements IOaMemberService
{
    @Autowired
    private OaMemberMapper memberMapper;

    @Autowired
    private OaMemberLogMapper memberLogMapper;

    @Autowired
    private OaAccountMapper accountMapper;

    @Autowired
    private OaMemberVoAssembler voAssembler;

    @Autowired
    private OaCurrentUser currentUser;

    @Autowired
    private MemberNoGenerator memberNoGenerator;

    @Autowired
    private MemberAccountService accountService;

    @Autowired
    private RoleSyncService roleSyncService;

    @Autowired
    private OaMemberLogService memberLogService;

    @Autowired
    private OaLockService lockService;

    @Autowired
    private ISysDeptService deptService;

    @Override
    @DataScope(deptAlias = "d")
    public List<MemberListVO> selectMemberList(MemberQuery query)
    {
        List<OaMember> members = memberMapper.selectMemberList(query);
        List<MemberListVO> voList = voAssembler.toListVO(members);
        return wrapPage(members, voList);
    }

    /**
     * 保留 PageHelper 分页信息（VO 转换后 total 不丢）
     */
    private List<MemberListVO> wrapPage(List<OaMember> raw, List<MemberListVO> voList)
    {
        if (raw instanceof com.github.pagehelper.Page)
        {
            com.github.pagehelper.Page<OaMember> page = (com.github.pagehelper.Page<OaMember>) raw;
            com.github.pagehelper.Page<MemberListVO> voPage = new com.github.pagehelper.Page<MemberListVO>(
                    page.getPageNum(), page.getPageSize());
            voPage.setTotal(page.getTotal());
            voPage.addAll(voList);
            return voPage;
        }
        return voList;
    }

    @Override
    public MemberDetailVO selectMemberDetail(Long memberId)
    {
        OaMember member = memberMapper.selectMemberById(memberId);
        if (member == null)
        {
            throw new ServiceException("成员不存在", 404);
        }
        assertReadAllowed(member);
        List<OaMemberLog> logs = memberLogMapper.selectLogByMemberId(memberId, 20);
        return voAssembler.toDetailVO(member, logs, currentUser.isAdmin());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> insertMember(MemberAddDTO dto)
    {
        if (!canWrite())
        {
            throw new ServiceException("没有权限访问该数据", 403);
        }
        validateRequired(dto.getName(), dto.getGender(), dto.getStudentNo(), dto.getPhone(), dto.getCollege(),
                dto.getMajor(), dto.getClassName(), dto.getGrade());
        validateFormat(dto.getName(), dto.getStudentNo(), dto.getPhone(), dto.getEmail(), dto.getWechat(), dto.getQq(),
                dto.getSkills(), dto.getIntro(), dto.getRemark());
        Long deptId = dto.getDeptId();
        if (deptId == null)
        {
            throw new ServiceException("请选择部门", 400);
        }
        assertDeptAllowed(deptId);
        assertDeptExists(deptId);

        OaMember byStudentNo = memberMapper.selectByStudentNo(dto.getStudentNo());
        if (byStudentNo != null)
        {
            throw new ServiceException("学号 " + dto.getStudentNo() + " 已存在档案（" + byStudentNo.getMemberNo()
                    + "），请使用编辑或导入更新", 409);
        }
        OaMember byPhone = memberMapper.selectByPhone(dto.getPhone());
        if (byPhone != null)
        {
            throw new ServiceException("该手机号已关联学号 " + byPhone.getStudentNo() + "，请人工核对", 409);
        }

        String lockKey = OaMemberConstants.LOCK_MEMBER_CREATE + dto.getStudentNo();
        if (!lockService.tryLock(lockKey, 30))
        {
            throw new ServiceException("操作进行中，请勿重复提交", 409);
        }
        try
        {
            Date joinDate = dto.getJoinDate() == null ? new Date() : dto.getJoinDate();
            if (joinDate.after(new Date()))
            {
                throw new ServiceException("入会日期不能晚于今天", 400);
            }
            OaMember member = new OaMember();
            member.setMemberNo(memberNoGenerator.nextMemberNo(joinDate));
            member.setName(dto.getName().trim());
            member.setGender(dto.getGender());
            member.setStudentNo(dto.getStudentNo());
            member.setPhone(dto.getPhone());
            member.setCollege(defaultString(dto.getCollege()));
            member.setMajor(defaultString(dto.getMajor()));
            member.setClassName(defaultString(dto.getClassName()));
            member.setGrade(dto.getGrade());
            member.setCohort(StringUtils.isEmpty(dto.getCohort()) ? currentYear() : dto.getCohort());
            member.setDeptId(deptId);
            member.setPosition(StringUtils.isEmpty(dto.getPosition()) ? OaMemberConstants.POSITION_STAFF
                    : dto.getPosition());
            member.setStatus(OaMemberConstants.MEMBER_STATUS_ACTIVE);
            member.setSource(OaMemberConstants.SOURCE_MANUAL);
            member.setJoinDate(joinDate);
            member.setEmail(defaultString(dto.getEmail()));
            member.setWechat(defaultString(dto.getWechat()));
            member.setQq(defaultString(dto.getQq()));
            member.setSkills(defaultString(dto.getSkills()));
            member.setIntro(defaultString(dto.getIntro()));
            member.setObeyAdjust(StringUtils.isEmpty(dto.getObeyAdjust()) ? "0" : dto.getObeyAdjust());
            member.setPhotoUrl(OaFileUrlHelper.url(dto.getPhotoFileId()));
            member.setCardUrl(OaFileUrlHelper.url(dto.getCardFileId()));
            member.setRemark(dto.getRemark());
            member.setCreateBy(currentUser.username());
            try
            {
                memberMapper.insertMember(member);
            }
            catch (DuplicateKeyException e)
            {
                throw new ServiceException("学号或手机号已存在，请刷新后重试", 409);
            }
            SysUser user = accountService.ensureAccount(member);
            memberMapper.updateMemberUserId(member.getMemberId(), user.getUserId());
            member.setUserId(user.getUserId());
            roleSyncService.sync(user.getUserId(), member.getPosition());
            memberLogService.record(member, "新增", null, memberLogService.snapshot(member));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("memberId", member.getMemberId());
            result.put("memberNo", member.getMemberNo());
            result.put("userId", user.getUserId());
            return result;
        }
        finally
        {
            lockService.unlock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMember(MemberUpdateDTO dto)
    {
        if (dto.getMemberId() == null)
        {
            throw new ServiceException("成员ID不能为空", 400);
        }
        OaMember member = memberMapper.selectMemberByIdForUpdate(dto.getMemberId());
        if (member == null)
        {
            throw new ServiceException("成员不存在", 404);
        }
        assertWriteAllowed(member);
        validateRequired(dto.getName(), dto.getGender(), dto.getStudentNo(), dto.getPhone(), dto.getCollege(),
                dto.getMajor(), dto.getClassName(), dto.getGrade());
        validateFormat(dto.getName(), dto.getStudentNo(), dto.getPhone(), dto.getEmail(), dto.getWechat(), dto.getQq(),
                dto.getSkills(), dto.getIntro(), dto.getRemark());
        if (!StringUtils.equals(member.getStudentNo(), dto.getStudentNo()))
        {
            if (!isAdminOrPresident())
            {
                throw new ServiceException("学号由管理员维护，如需变更请联系管理员", 403);
            }
            OaMember exist = memberMapper.selectByStudentNo(dto.getStudentNo());
            if (exist != null && !exist.getMemberId().equals(member.getMemberId()))
            {
                throw new ServiceException("学号已存在（成员 " + exist.getMemberNo() + "），如需修改请先核对", 409);
            }
        }
        if (dto.getPhone() != null && !StringUtils.equals(member.getPhone(), dto.getPhone()))
        {
            OaMember byPhone = memberMapper.selectByPhone(dto.getPhone());
            if (byPhone != null && !byPhone.getMemberId().equals(member.getMemberId()))
            {
                throw new ServiceException("该手机号已关联学号 " + byPhone.getStudentNo() + "，请人工核对", 409);
            }
        }
        if (dto.getDeptId() != null && !dto.getDeptId().equals(member.getDeptId()))
        {
            throw new ServiceException("部门变更请使用「调部门」操作", 400);
        }
        if (StringUtils.isNotEmpty(dto.getPosition()) && !dto.getPosition().equals(member.getPosition()))
        {
            throw new ServiceException("职务变更请使用「升职」操作", 400);
        }

        Map<String, Object> before = memberLogService.snapshot(member);
        member.setName(dto.getName().trim());
        member.setGender(dto.getGender());
        member.setStudentNo(dto.getStudentNo());
        member.setPhone(dto.getPhone());
        member.setCollege(defaultString(dto.getCollege()));
        member.setMajor(defaultString(dto.getMajor()));
        member.setClassName(defaultString(dto.getClassName()));
        member.setGrade(dto.getGrade());
        if (StringUtils.isNotEmpty(dto.getCohort()))
        {
            member.setCohort(dto.getCohort());
        }
        member.setEmail(defaultString(dto.getEmail()));
        member.setWechat(defaultString(dto.getWechat()));
        member.setQq(defaultString(dto.getQq()));
        member.setSkills(defaultString(dto.getSkills()));
        member.setIntro(defaultString(dto.getIntro()));
        if (StringUtils.isNotEmpty(dto.getObeyAdjust()))
        {
            member.setObeyAdjust(dto.getObeyAdjust());
        }
        if (dto.getPhotoFileId() != null)
        {
            member.setPhotoUrl(OaFileUrlHelper.url(dto.getPhotoFileId()));
        }
        if (dto.getCardFileId() != null)
        {
            member.setCardUrl(OaFileUrlHelper.url(dto.getCardFileId()));
        }
        if (dto.getRemark() != null)
        {
            member.setRemark(dto.getRemark());
        }
        member.setUpdateBy(currentUser.username());
        memberMapper.updateMember(member);

        if (member.getUserId() != null)
        {
            if (!StringUtils.equals(before.get("phone") == null ? null : String.valueOf(before.get("phone")),
                    member.getPhone()))
            {
                accountMapper.updateUserPhone(member.getUserId(), member.getPhone());
            }
            accountMapper.updateUserName(member.getUserId(), member.getStudentNo());
        }
        memberLogService.record(member, "编辑", before, memberLogService.snapshot(member));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(MemberStatusDTO dto)
    {
        if (dto.getMemberId() == null || StringUtils.isEmpty(dto.getStatus()))
        {
            throw new ServiceException("参数不完整", 400);
        }
        if (!OaMemberConstants.MEMBER_STATUS_ACTIVE.equals(dto.getStatus())
                && !OaMemberConstants.MEMBER_STATUS_LEFT.equals(dto.getStatus())
                && !OaMemberConstants.MEMBER_STATUS_GRADUATED.equals(dto.getStatus()))
        {
            throw new ServiceException("状态值不合法", 400);
        }
        OaMember member = memberMapper.selectMemberByIdForUpdate(dto.getMemberId());
        if (member == null)
        {
            throw new ServiceException("成员不存在", 404);
        }
        assertWriteAllowed(member);
        if (dto.getStatus().equals(member.getStatus()))
        {
            throw new ServiceException("状态未变更", 400);
        }
        Date leaveDate = dto.getLeaveDate();
        if (OaMemberConstants.MEMBER_STATUS_ACTIVE.equals(dto.getStatus()))
        {
            leaveDate = null;
        }
        else if (leaveDate == null)
        {
            leaveDate = new Date();
        }
        if (leaveDate != null && member.getJoinDate() != null && leaveDate.before(member.getJoinDate()))
        {
            throw new ServiceException("离会日期不能早于入会日期", 400);
        }
        Map<String, Object> before = memberLogService.snapshot(member);
        memberMapper.updateMemberStatus(member.getMemberId(), dto.getStatus(), leaveDate);
        if (OaMemberConstants.MEMBER_STATUS_ACTIVE.equals(dto.getStatus()))
        {
            accountService.enableAccount(member.getUserId());
        }
        else
        {
            accountService.disableAccount(member.getUserId());
        }
        member.setStatus(dto.getStatus());
        member.setLeaveDate(leaveDate);
        if (StringUtils.isNotEmpty(dto.getRemark()))
        {
            member.setRemark(dto.getRemark());
        }
        String action = OaMemberConstants.MEMBER_STATUS_ACTIVE.equals(dto.getStatus()) ? "恢复" : "状态变更";
        memberLogService.record(member, action, before, memberLogService.snapshot(member));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(MemberTransferDTO dto)
    {
        if (!isAdminOrPresident())
        {
            throw new ServiceException("仅管理员/主席团可调部门或升职", 403);
        }
        if (dto.getMemberId() == null)
        {
            throw new ServiceException("成员ID不能为空", 400);
        }
        String lockKey = OaMemberConstants.LOCK_MEMBER_TRANSFER + dto.getMemberId();
        if (!lockService.tryLock(lockKey, 30))
        {
            throw new ServiceException("操作进行中，请勿重复提交", 409);
        }
        try
        {
            OaMember member = memberMapper.selectMemberByIdForUpdate(dto.getMemberId());
            if (member == null)
            {
                throw new ServiceException("成员不存在", 404);
            }
            Long newDeptId = dto.getDeptId() == null ? member.getDeptId() : dto.getDeptId();
            String newPosition = StringUtils.isEmpty(dto.getPosition()) ? member.getPosition() : dto.getPosition();
            if (newDeptId.equals(member.getDeptId()) && newPosition.equals(member.getPosition()))
            {
                throw new ServiceException("未检测到修改", 400);
            }
            if (!newDeptId.equals(member.getDeptId()))
            {
                assertDeptExists(newDeptId);
            }
            Map<String, Object> before = memberLogService.snapshot(member);
            memberMapper.updateMemberDeptPosition(member.getMemberId(), newDeptId, newPosition);
            accountService.syncDept(member.getUserId(), newDeptId);
            roleSyncService.sync(member.getUserId(), newPosition);
            boolean deptChanged = !newDeptId.equals(member.getDeptId());
            member.setDeptId(newDeptId);
            member.setPosition(newPosition);
            memberLogService.record(member, deptChanged ? "调部门" : "升职", before,
                    memberLogService.snapshot(member));
        }
        finally
        {
            lockService.unlock(lockKey);
        }
    }

    @Override
    @DataScope(deptAlias = "d")
    public MemberStatsVO selectStats(MemberQuery query)
    {
        MemberStatsVO vo = new MemberStatsVO();
        vo.setTotal(nvl(memberMapper.countActive(query)));
        vo.setPendingApply(nvl(memberMapper.countPendingApply(query)));
        vo.setMonthJoin(nvl(memberMapper.countMonthJoin(query)));
        vo.setMonthLeave(nvl(memberMapper.countMonthLeave(query)));
        return vo;
    }

    @Override
    @DataScope(deptAlias = "d")
    public List<OaMember> selectMemberListForExport(MemberQuery query)
    {
        return memberMapper.selectMemberList(query);
    }

    @Override
    public MemberDetailVO selectSelfDetail()
    {
        OaMember member = memberMapper.selectByUserId(currentUser.userId());
        if (member == null)
        {
            throw new ServiceException("当前账号未关联成员档案", 404);
        }
        List<OaMemberLog> logs = memberLogMapper.selectLogByMemberId(member.getMemberId(), 20);
        return voAssembler.toDetailVO(member, logs, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSelf(MemberSelfDTO dto)
    {
        OaMember member = memberMapper.selectByUserId(currentUser.userId());
        if (member == null)
        {
            throw new ServiceException("当前账号未关联成员档案", 404);
        }
        validateFormat(member.getName(), member.getStudentNo(), member.getPhone(), dto.getEmail(), dto.getWechat(),
                dto.getQq(), dto.getSkills(), dto.getIntro(), null);
        String photoUrl = dto.getPhotoFileId() == null ? member.getPhotoUrl()
                : OaFileUrlHelper.url(dto.getPhotoFileId());
        memberMapper.updateMemberSelf(member.getMemberId(), defaultString(dto.getWechat()), defaultString(dto.getQq()),
                defaultString(dto.getEmail()), defaultString(dto.getSkills()), defaultString(dto.getIntro()), photoUrl);
        Map<String, Object> before = memberLogService.snapshot(member);
        member.setWechat(defaultString(dto.getWechat()));
        member.setQq(defaultString(dto.getQq()));
        member.setEmail(defaultString(dto.getEmail()));
        member.setSkills(defaultString(dto.getSkills()));
        member.setIntro(defaultString(dto.getIntro()));
        member.setPhotoUrl(photoUrl);
        memberLogService.record(member, "编辑", before, memberLogService.snapshot(member));
    }

    @Override
    public OaMember selectByStudentNo(String studentNo)
    {
        if (StringUtils.isEmpty(studentNo))
        {
            return null;
        }
        return memberMapper.selectByStudentNo(studentNo);
    }

    // ------------------------------------------------------------------ 私有方法

    private boolean isAdminOrPresident()
    {
        return currentUser.isAdmin() || currentUser.hasRole(OaMemberConstants.ROLE_PRESIDENT);
    }

    private boolean isMinister()
    {
        return currentUser.hasRole(OaMemberConstants.ROLE_MINISTER);
    }

    private boolean canWrite()
    {
        return isAdminOrPresident() || isMinister();
    }

    private Long myDeptId()
    {
        SysUser user = currentUser.user();
        return user == null ? null : user.getDeptId();
    }

    private void assertReadAllowed(OaMember member)
    {
        if (isAdminOrPresident())
        {
            return;
        }
        if (currentUser.isSelf(member.getUserId()))
        {
            return;
        }
        Long deptId = myDeptId();
        if (deptId != null && deptId.equals(member.getDeptId()))
        {
            return;
        }
        throw new ServiceException("没有访问权限，请联系管理员", 403);
    }

    private void assertWriteAllowed(OaMember member)
    {
        if (isAdminOrPresident())
        {
            return;
        }
        Long deptId = myDeptId();
        if (isMinister() && deptId != null && deptId.equals(member.getDeptId()))
        {
            return;
        }
        throw new ServiceException("没有访问权限，请联系管理员", 403);
    }

    private void assertDeptAllowed(Long deptId)
    {
        if (isAdminOrPresident())
        {
            return;
        }
        Long deptId2 = myDeptId();
        if (isMinister() && deptId2 != null && deptId2.equals(deptId))
        {
            return;
        }
        throw new ServiceException("无权操作该部门", 403);
    }

    private void assertDeptExists(Long deptId)
    {
        SysDept dept = deptService.selectDeptById(deptId);
        if (dept == null || !"0".equals(dept.getStatus()) || !"0".equals(dept.getDelFlag()))
        {
            throw new ServiceException("部门不存在或已停用", 400);
        }
    }

    private void validateRequired(String name, String gender, String studentNo, String phone, String college,
            String major, String className, String grade)
    {
        if (StringUtils.isEmpty(name))
        {
            throw new ServiceException("请填写姓名", 400);
        }
        if (StringUtils.isEmpty(gender))
        {
            throw new ServiceException("请选择性别", 400);
        }
        if (StringUtils.isEmpty(studentNo))
        {
            throw new ServiceException("请填写学号", 400);
        }
        if (StringUtils.isEmpty(phone))
        {
            throw new ServiceException("请填写手机号", 400);
        }
        if (StringUtils.isEmpty(college))
        {
            throw new ServiceException("请填写学院", 400);
        }
        if (StringUtils.isEmpty(major))
        {
            throw new ServiceException("请填写专业", 400);
        }
        if (StringUtils.isEmpty(className))
        {
            throw new ServiceException("请填写班级", 400);
        }
        if (StringUtils.isEmpty(grade))
        {
            throw new ServiceException("请选择年级", 400);
        }
    }

    private void validateFormat(String name, String studentNo, String phone, String email, String wechat, String qq,
            String skills, String intro, String remark)
    {
        if (StringUtils.isNotEmpty(name) && !name.matches("^[\\u4e00-\\u9fa5A-Za-z·]{2,20}$"))
        {
            throw new ServiceException("姓名需为 2–20 个中英文字符", 400);
        }
        if (!studentNo.matches("^\\d{8,12}$"))
        {
            throw new ServiceException("学号需为 8–12 位数字", 400);
        }
        if (!phone.matches("^1[3-9]\\d{9}$"))
        {
            throw new ServiceException("请输入 11 位手机号", 400);
        }
        if (StringUtils.isNotEmpty(email) && !email.matches("^[\\w.-]+@[\\w-]+(\\.[\\w-]+)+$"))
        {
            throw new ServiceException("请输入正确的邮箱地址", 400);
        }
        if (StringUtils.isNotEmpty(wechat) && !wechat.matches("^[a-zA-Z][-_a-zA-Z0-9]{5,19}$"))
        {
            throw new ServiceException("微信号需字母开头，6–20 位字母 / 数字 / 下划线 / 减号", 400);
        }
        if (StringUtils.isNotEmpty(qq) && !qq.matches("^\\d{5,12}$"))
        {
            throw new ServiceException("QQ 号需为 5–12 位数字", 400);
        }
        if (skills != null && skills.length() > 500)
        {
            throw new ServiceException("特长最多 500 字", 400);
        }
        if (intro != null && intro.length() > 500)
        {
            throw new ServiceException("自我介绍最多 500 字", 400);
        }
        if (remark != null && remark.length() > 500)
        {
            throw new ServiceException("备注最多 500 字", 400);
        }
    }

    private String currentYear()
    {
        return new SimpleDateFormat("yyyy").format(new Date());
    }

    private String defaultString(String value)
    {
        return value == null ? "" : value;
    }

    private Long nvl(Long value)
    {
        return value == null ? 0L : value;
    }

    /**
     * 当前登录人是否具备某角色（供其他模块复用）
     */
    public boolean hasRole(String roleKey)
    {
        Set<String> roles = currentUser.roleKeys();
        return roles.contains(roleKey);
    }

    /**
     * 校验当前登录密码是否可用于敏感操作（预留）
     */
    public void checkPassword(String password)
    {
        if (StringUtils.isEmpty(password) || !SecurityUtils.matchesPassword(password,
                currentUser.user().getPassword()))
        {
            throw new ServiceException("密码错误", 400);
        }
    }
}
