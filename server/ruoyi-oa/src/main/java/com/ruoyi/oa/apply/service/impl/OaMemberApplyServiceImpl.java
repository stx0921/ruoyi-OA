package com.ruoyi.oa.apply.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.github.pagehelper.Page;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.oa.apply.domain.OaMemberApply;
import com.ruoyi.oa.apply.domain.OaMemberApplyLog;
import com.ruoyi.oa.apply.dto.ApplyAuditDTO;
import com.ruoyi.oa.apply.dto.ApplyResubmitDTO;
import com.ruoyi.oa.apply.dto.ApplySubmitDTO;
import com.ruoyi.oa.apply.dto.ProgressDTO;
import com.ruoyi.oa.apply.dto.WithdrawDTO;
import com.ruoyi.oa.apply.mapper.OaMemberApplyLogMapper;
import com.ruoyi.oa.apply.mapper.OaMemberApplyMapper;
import com.ruoyi.oa.apply.service.IOaFileService;
import com.ruoyi.oa.apply.service.IOaMemberApplyService;
import com.ruoyi.oa.apply.support.ApplyNoGenerator;
import com.ruoyi.oa.apply.support.OaCaptchaService;
import com.ruoyi.oa.apply.vo.ApplyDetailVO;
import com.ruoyi.oa.apply.vo.ApplyListVO;
import com.ruoyi.oa.apply.vo.ApplyLogVO;
import com.ruoyi.oa.apply.vo.AuditItemVO;
import com.ruoyi.oa.apply.vo.AuditResultVO;
import com.ruoyi.oa.apply.vo.ProgressApplyVO;
import com.ruoyi.oa.apply.vo.ProgressResultVO;
import com.ruoyi.oa.apply.vo.SubmitResultVO;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.member.domain.OaMember;
import com.ruoyi.oa.member.mapper.OaMemberMapper;
import com.ruoyi.oa.support.DeptLeaderResolver;
import com.ruoyi.oa.support.OaCurrentUser;
import com.ruoyi.oa.support.OaDictLabelProvider;
import com.ruoyi.oa.support.OaMemberLogService;
import com.ruoyi.oa.support.MemberAccountService;
import com.ruoyi.oa.support.MemberNoGenerator;
import com.ruoyi.oa.support.OaFileUrlHelper;
import com.ruoyi.oa.support.OaLockService;
import com.ruoyi.system.service.ISysDeptService;

/**
 * 入会申请 Service 实现
 * <p>
 * 状态机：0 待审批 / 1 已通过 / 2 已驳回 / 3 已撤回 / 4 已关闭。
 * 审批通过为逐条独立事务：行锁二次校验 → 建/恢复档案 → 开账号 → 绑 oa_staff → 关闭同人其他待审 → 写日志。
 * </p>
 *
 * @author ruoyi-oa
 */
@Service
public class OaMemberApplyServiceImpl implements IOaMemberApplyService
{
    private static final Logger log = LoggerFactory.getLogger(OaMemberApplyServiceImpl.class);

    /** 提交/重提并发锁 key 前缀 */
    private static final String LOCK_APPLY_SUBMIT = "oa:lock:apply:submit:";

    /** 审批并发锁 key 前缀 */
    private static final String LOCK_APPLY_AUDIT = "oa:lock:apply:audit:";

    /** 锁 TTL（秒） */
    private static final long LOCK_TTL_SECONDS = 30L;

    /** 批量审批单次上限 */
    private static final int AUDIT_BATCH_MAX = 50;

    /** 年级标签（与字典 oa_grade 一致） */
    private static final Map<String, String> GRADE_LABELS = new HashMap<String, String>();

    /** 申请状态标签（与字典 oa_apply_status 一致） */
    private static final Map<String, String> APPLY_STATUS_LABELS = new HashMap<String, String>();

    /** 手机号正则 */
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /** 学号正则 */
    private static final String STUDENT_NO_REGEX = "^\\d{8,12}$";

    /** 姓名正则（2–20 个中英文/·） */
    private static final String NAME_REGEX = "^[\\u4e00-\\u9fa5A-Za-z·]{2,20}$";

    static
    {
        GRADE_LABELS.put("1", "大一");
        GRADE_LABELS.put("2", "大二");
        GRADE_LABELS.put("3", "大三");
        GRADE_LABELS.put("4", "大四");
        GRADE_LABELS.put("5", "研一");
        GRADE_LABELS.put("6", "研二");
        GRADE_LABELS.put("7", "研三");

        APPLY_STATUS_LABELS.put(OaMemberConstants.APPLY_STATUS_PENDING, "待审批");
        APPLY_STATUS_LABELS.put(OaMemberConstants.APPLY_STATUS_APPROVED, "已通过");
        APPLY_STATUS_LABELS.put(OaMemberConstants.APPLY_STATUS_REJECTED, "已驳回");
        APPLY_STATUS_LABELS.put(OaMemberConstants.APPLY_STATUS_WITHDRAWN, "已撤回");
        APPLY_STATUS_LABELS.put(OaMemberConstants.APPLY_STATUS_CLOSED, "已关闭");
    }

    @Autowired
    private OaMemberApplyMapper applyMapper;

    @Autowired
    private OaMemberApplyLogMapper applyLogMapper;

    @Autowired
    private OaMemberMapper memberMapper;

    @Autowired
    private OaMemberLogService memberLogService;

    @Autowired
    private OaCurrentUser currentUser;

    @Autowired
    private IOaFileService fileService;

    @Autowired
    private OaCaptchaService captchaService;

    @Autowired
    private ApplyNoGenerator applyNoGenerator;

    @Autowired
    private OaLockService lockService;

    @Autowired
    private MemberNoGenerator memberNoGenerator;

    @Autowired
    private MemberAccountService accountService;

    @Autowired
    private DeptLeaderResolver deptLeaderResolver;

    @Autowired
    private OaDictLabelProvider dictLabels;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // ============================ PC 审批 ============================

    @Override
    @DataScope(deptAlias = "a")
    public List<ApplyListVO> selectApplyList(String scope, OaMemberApply query)
    {
        // 注意：控制器 startPage() 后第一条 MyBatis 查询必须是列表 SQL；
        // 这里角色判定只能读 LoginUser（登录时已加载角色），否则角色查询会消费 PageHelper 分页变量。
        SysUser me = SecurityUtils.getLoginUser().getUser();
        boolean admin = me != null && me.isAdmin();
        boolean president = hasRole(me, OaMemberConstants.ROLE_PRESIDENT);
        boolean all = admin || president;
        OaMemberApply condition = query == null ? new OaMemberApply() : query;
        String finalScope = StringUtils.isEmpty(scope) ? "all" : scope;
        condition.getParams().put("scope", finalScope);
        condition.getParams().put("scopeUserId", me.getUserId());
        condition.getParams().put("scopeAll", all);

        List<OaMemberApply> list = applyMapper.selectApplyList(condition);
        List<ApplyListVO> result = new ArrayList<>(list.size());
        // 一次请求只读一次字典，改字典后这里立刻生效
        Map<String, String> gradeLabels = dictLabels.labels("oa_grade", GRADE_LABELS);
        Map<String, String> statusLabels = dictLabels.labels("oa_apply_status", APPLY_STATUS_LABELS);
        for (OaMemberApply apply : list)
        {
            ApplyListVO vo = new ApplyListVO();
            fillListVO(vo, apply, gradeLabels, statusLabels);
            vo.setCanAudit(canAudit(apply, me, all));
            result.add(vo);
        }
        // 保留 PageHelper 分页信息（转换成 VO 后 getDataTable 仍能取到 total）
        if (list instanceof Page)
        {
            Page<OaMemberApply> sourcePage = (Page<OaMemberApply>) list;
            Page<ApplyListVO> voPage = new Page<>(sourcePage.getPageNum(), sourcePage.getPageSize());
            voPage.setTotal(sourcePage.getTotal());
            voPage.addAll(result);
            return voPage;
        }
        return result;
    }

    @Override
    public ApplyDetailVO selectApplyDetail(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("申请不存在", 404);
        }
        OaMemberApply apply = applyMapper.selectApplyById(id);
        if (apply == null)
        {
            throw new ServiceException("申请不存在", 404);
        }
        SysUser me = currentUser.user();
        boolean admin = currentUser.isAdmin();
        boolean president = currentUser.hasRole(OaMemberConstants.ROLE_PRESIDENT);
        boolean all = admin || president;
        if (!all && !deptLeaderResolver.isLeaderOf(apply.getDeptId(), me.getUserId()))
        {
            throw new ServiceException("没有权限访问该申请", 403);
        }
        ApplyDetailVO vo = new ApplyDetailVO();
        fillListVO(vo, apply, dictLabels.labels("oa_grade", GRADE_LABELS),
                dictLabels.labels("oa_apply_status", APPLY_STATUS_LABELS));
        vo.setCollege(apply.getCollege());
        vo.setMajor(apply.getMajor());
        vo.setClassName(apply.getClassName());
        vo.setSkills(apply.getSkills());
        vo.setIntro(apply.getIntro());
        vo.setObeyAdjust(apply.getObeyAdjust());
        vo.setRetryCount(apply.getRetryCount() == null ? 0 : apply.getRetryCount());
        vo.setCohort(apply.getCohort());
        vo.setMemberId(apply.getMemberId());
        Long photoFileId = OaFileUrlHelper.parseFileId(apply.getPhotoUrl());
        Long cardFileId = OaFileUrlHelper.parseFileId(apply.getCardUrl());
        vo.setPhotoFileId(photoFileId);
        vo.setPhotoUrl(photoFileId == null ? "" : OaFileUrlHelper.url(photoFileId));
        vo.setCardFileId(cardFileId);
        vo.setCardUrl(cardFileId == null ? "" : OaFileUrlHelper.url(cardFileId));
        vo.setCanAudit(canAudit(apply, me, all));

        List<OaMemberApplyLog> logs = applyLogMapper.selectLogByApplyId(id);
        List<ApplyLogVO> logVos = new ArrayList<>(logs.size());
        for (OaMemberApplyLog item : logs)
        {
            ApplyLogVO logVo = new ApplyLogVO();
            logVo.setLogId(item.getLogId());
            logVo.setAction(item.getAction());
            logVo.setOperatorName(StringUtils.isEmpty(item.getOperatorName()) ? "系统" : item.getOperatorName());
            logVo.setRemark(item.getRemark());
            logVo.setCreateTime(item.getCreateTime());
            logVos.add(logVo);
        }
        vo.setLogs(logVos);
        return vo;
    }

    @Override
    public AuditResultVO audit(ApplyAuditDTO dto)
    {
        if (dto == null || dto.getIds() == null || dto.getIds().isEmpty())
        {
            throw new ServiceException("请选择要审批的申请", 400);
        }
        if (dto.getIds().size() > AUDIT_BATCH_MAX)
        {
            throw new ServiceException("单次最多审批 " + AUDIT_BATCH_MAX + " 条，请分批操作", 400);
        }
        if (dto.getPass() == null)
        {
            throw new ServiceException("请选择同意或驳回", 400);
        }
        String remark = StringUtils.trim(dto.getRemark());
        if (!Boolean.TRUE.equals(dto.getPass()))
        {
            if (remark == null || remark.length() < 2 || remark.length() > 200)
            {
                throw new ServiceException("驳回时必须填写审批意见（2–200 字）", 400);
            }
        }
        else if (remark != null && remark.length() > 200)
        {
            throw new ServiceException("审批意见最多 200 字", 400);
        }
        dto.setRemark(remark == null ? "" : remark);

        SysUser me = currentUser.user();
        boolean admin = currentUser.isAdmin();
        boolean president = currentUser.hasRole(OaMemberConstants.ROLE_PRESIDENT);
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        List<AuditItemVO> items = new ArrayList<>(dto.getIds().size());
        int successCount = 0;
        for (Long applyId : dto.getIds())
        {
            AuditItemVO item = new AuditItemVO();
            item.setApplyId(applyId);
            if (applyId == null)
            {
                item.setSuccess(false);
                item.setMessage("申请不存在");
                items.add(item);
                continue;
            }
            String lockKey = LOCK_APPLY_AUDIT + applyId;
            boolean locked = false;
            try
            {
                locked = lockService.tryLock(lockKey, LOCK_TTL_SECONDS);
                if (!locked)
                {
                    item.setSuccess(false);
                    item.setMessage("申请正在处理中，请稍后重试");
                    items.add(item);
                    continue;
                }
                item = template.execute(status -> doAuditOne(applyId, dto, me, admin, president));
            }
            catch (ServiceException e)
            {
                item.setSuccess(false);
                item.setMessage(e.getMessage());
                log.warn("审批申请[{}]失败：{}", applyId, e.getMessage());
            }
            catch (Exception e)
            {
                item.setSuccess(false);
                item.setMessage("审批失败，请稍后重试");
                log.error("审批申请[" + applyId + "]异常", e);
            }
            finally
            {
                if (locked)
                {
                    lockService.unlock(lockKey);
                }
            }
            if (Boolean.TRUE.equals(item.getSuccess()))
            {
                successCount++;
            }
            items.add(item);
        }
        AuditResultVO result = new AuditResultVO();
        result.setItems(items);
        result.setSuccessCount(successCount);
        result.setFailCount(items.size() - successCount);
        return result;
    }

    /**
     * 单条审批（在独立事务内执行）
     */
    private AuditItemVO doAuditOne(Long applyId, ApplyAuditDTO dto, SysUser me, boolean admin, boolean president)
    {
        AuditItemVO item = new AuditItemVO();
        item.setApplyId(applyId);
        OaMemberApply apply = applyMapper.selectApplyByIdForUpdate(applyId);
        if (apply == null)
        {
            throw new ServiceException("申请不存在", 404);
        }
        item.setApplyNo(apply.getApplyNo());
        if (!OaMemberConstants.APPLY_STATUS_PENDING.equals(apply.getStatus()))
        {
            throw new ServiceException("申请 " + apply.getApplyNo() + " 已被处理，请刷新后重试", 409);
        }
        if (!deptLeaderResolver.canAudit(apply.getDeptId(), me.getUserId(), admin, president))
        {
            throw new ServiceException("只能审批本部门申请", 403);
        }
        // 代审判定：有权限但不是该部门部长 → 记为代审
        boolean proxy = deptLeaderResolver.isProxy(apply.getDeptId(), me.getUserId(), admin, president);
        String auditUserName = StringUtils.isNotEmpty(me.getNickName()) ? me.getNickName() : me.getUserName();

        // 驳回：状态 0 → 2，不建档案不开账号
        if (!Boolean.TRUE.equals(dto.getPass()))
        {
            int rows = applyMapper.updateAuditStatus(applyId, OaMemberConstants.APPLY_STATUS_REJECTED,
                    me.getUserId(), auditUserName, dto.getRemark());
            if (rows == 0)
            {
                throw new ServiceException("申请 " + apply.getApplyNo() + " 已被处理，请刷新后重试", 409);
            }
            writeApplyLog(applyId, proxy ? "代审" : "驳回", me.getUserId(), auditUserName, dto.getRemark());
            item.setSuccess(true);
            item.setMessage("已驳回");
            return item;
        }

        // 并发兜底：同一学号已有在籍档案（可能刚被其他部门通过）→ 本条自动关闭
        OaMember exist = memberMapper.selectByStudentNoForUpdate(apply.getStudentNo());
        if (exist != null && OaMemberConstants.MEMBER_STATUS_ACTIVE.equals(exist.getStatus()))
        {
            String deptName = getDeptName(exist.getDeptId());
            String closeRemark = "已被" + deptName + "录用";
            int rows = applyMapper.updateAuditStatus(applyId, OaMemberConstants.APPLY_STATUS_CLOSED,
                    me.getUserId(), auditUserName, closeRemark);
            if (rows == 0)
            {
                throw new ServiceException("申请 " + apply.getApplyNo() + " 已被处理，请刷新后重试", 409);
            }
            writeApplyLog(applyId, proxy ? "代审" : "自动关闭", me.getUserId(), auditUserName, closeRemark);
            item.setSuccess(true);
            item.setMessage("该学号已在" + deptName + "在籍，本条申请已自动关闭");
            item.setMemberId(exist.getMemberId());
            return item;
        }

        // 建档案 / 恢复档案（同人已有退会、毕业档案则改部门并恢复）
        OaMember member;
        Map<String, Object> before = null;
        String memberAction;
        if (exist == null)
        {
            member = newMemberFromApply(apply, me);
            memberMapper.insertMember(member);
            memberAction = "新增";
        }
        else
        {
            before = memberLogService.snapshot(exist);
            member = reactivateMember(exist, apply, me);
            memberMapper.updateMember(member);
            // 恢复在籍需清空离会日期（updateMember 对 null 字段不更新，故单独清空）
            memberMapper.updateMemberStatus(member.getMemberId(), OaMemberConstants.MEMBER_STATUS_ACTIVE, null);
            memberAction = "恢复";
        }

        // 开通账号（幂等）：user_name=学号、初始密码=手机号后 6 位、绑 oa_staff
        SysUser account = accountService.ensureAccount(member);
        if (account == null)
        {
            throw new ServiceException("账号开通失败，请稍后重试", 500);
        }
        member.setUserId(account.getUserId());
        memberMapper.updateMemberUserId(member.getMemberId(), account.getUserId());
        if ("1".equals(account.getStatus()))
        {
            accountService.enableAccount(account.getUserId());
        }
        if (exist != null)
        {
            // 恢复入会可能换了部门，同步账号部门（数据权限依赖 sys_user.dept_id）
            accountService.syncDept(account.getUserId(), member.getDeptId());
        }

        // R3：关闭同人其余待审批（同事务逐条写日志）
        String joinDeptName = getDeptName(member.getDeptId());
        String closeRemark = "已被" + joinDeptName + "录用";
        List<OaMemberApply> pendingList = applyMapper.selectPendingByStudentNoForUpdate(apply.getStudentNo());
        for (OaMemberApply other : pendingList)
        {
            if (other.getId().equals(applyId))
            {
                continue;
            }
            int rows = applyMapper.updateAuditStatus(other.getId(), OaMemberConstants.APPLY_STATUS_CLOSED,
                    me.getUserId(), auditUserName, closeRemark);
            if (rows > 0)
            {
                writeApplyLog(other.getId(), "自动关闭", me.getUserId(), auditUserName, closeRemark);
            }
        }

        // 本单通过 + 回写 member_id + 写时间轴
        int rows = applyMapper.approve(applyId, member.getMemberId(), me.getUserId(), auditUserName, dto.getRemark());
        if (rows == 0)
        {
            throw new ServiceException("申请 " + apply.getApplyNo() + " 已被处理，请刷新后重试", 409);
        }
        writeApplyLog(applyId, proxy ? "代审" : "同意", me.getUserId(), auditUserName, dto.getRemark());
        memberLogService.record(member, memberAction, before, memberLogService.snapshot(member));

        item.setSuccess(true);
        item.setMessage("已同意");
        item.setMemberId(member.getMemberId());
        item.setUserId(account.getUserId());
        return item;
    }

    // ============================ H5 公开接口 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmitResultVO submitApply(ApplySubmitDTO dto, String ip)
    {
        if (dto == null)
        {
            throw new ServiceException("提交内容不能为空", 400);
        }
        // 1) 图形验证码校验令牌
        captchaService.requireVerifyToken(dto.getVerifyToken());
        // 2) 基础字段校验
        validateSubmitFields(dto);
        if (!Boolean.TRUE.equals(dto.getAgreed()))
        {
            throw new ServiceException("请先阅读并同意《入会须知》", 400);
        }
        if (dto.getApplyDeptIds() == null || dto.getApplyDeptIds().isEmpty())
        {
            throw new ServiceException("请选择意向部门", 400);
        }
        // 3) 同号 24h 提交限流（R8）
        captchaService.checkSubmitLimit(dto.getPhone());
        // 4) R4：已是在籍成员不可重复报名
        OaMember exist = memberMapper.selectByStudentNo(dto.getStudentNo());
        if (exist != null && OaMemberConstants.MEMBER_STATUS_ACTIVE.equals(exist.getStatus()))
        {
            throw new ServiceException("你已是 " + getDeptName(exist.getDeptId()) + " 成员，无需重复报名", 409);
        }
        // 5) R14：同一手机号不允许关联两个不同学号
        OaMember phoneMember = memberMapper.selectByPhone(dto.getPhone());
        if (phoneMember != null && !dto.getStudentNo().equals(phoneMember.getStudentNo()))
        {
            throw new ServiceException("该手机号已关联学号 " + phoneMember.getStudentNo() + "，请人工核对", 400);
        }
        // 6) 附件存在性校验
        String photoUrl = resolveApplyFileUrl(dto.getPhotoFileId(), "照片");
        String cardUrl = resolveApplyFileUrl(dto.getCardFileId(), "简历");

        Set<Long> deptIds = new LinkedHashSet<>(dto.getApplyDeptIds());
        List<Long> applyIds = new ArrayList<>(deptIds.size());
        List<String> applyNos = new ArrayList<>(deptIds.size());
        for (Long deptId : deptIds)
        {
            SysDept dept = deptId == null ? null : deptService.selectDeptById(deptId);
            // del_flag 为 null 时视为未删除（兼容若依 selectDeptById 历史上不返回该列）
            boolean deleted = dept != null && dept.getDelFlag() != null && !"0".equals(dept.getDelFlag());
            if (dept == null || !"0".equals(dept.getStatus()) || deleted)
            {
                throw new ServiceException("意向部门不存在或已停用", 400);
            }
            String lockKey = LOCK_APPLY_SUBMIT + dto.getStudentNo() + ":" + deptId;
            if (!lockService.tryLock(lockKey, LOCK_TTL_SECONDS))
            {
                throw new ServiceException("操作进行中，请勿重复提交", 409);
            }
            try
            {
                // R1/R2：同学号同部门仅允许 1 条待审批（行锁二次校验）
                OaMemberApply pending = applyMapper.selectPendingByStudentAndDeptForUpdate(dto.getStudentNo(), deptId);
                if (pending != null)
                {
                    throw new ServiceException("该部门已有待审批申请，请到报名进度查看", 409);
                }
                OaMemberApply apply = buildApplyFromSubmit(dto, deptId, photoUrl, cardUrl, ip);
                applyMapper.insertApply(apply);
                writeApplyLog(apply.getId(), "提交", null, dto.getName(), "");
                applyIds.add(apply.getId());
                applyNos.add(apply.getApplyNo());
            }
            finally
            {
                lockService.unlock(lockKey);
            }
        }
        captchaService.recordSubmit(dto.getPhone());

        SubmitResultVO result = new SubmitResultVO();
        result.setApplyIds(applyIds);
        result.setApplyNos(applyNos);
        result.setProgressTip("请记住申请编号，凭手机号+图形验证码可在报名进度中查询");
        log.info("H5 提交入会申请 studentNo={} depts={} applyNos={}", dto.getStudentNo(), deptIds, applyNos);
        return result;
    }

    @Override
    public ProgressResultVO progress(ProgressDTO dto, String ip)
    {
        if (dto == null || StringUtils.isEmpty(dto.getPhone()))
        {
            throw new ServiceException("请输入 11 位手机号", 400);
        }
        if (!dto.getPhone().matches(PHONE_REGEX))
        {
            throw new ServiceException("请输入 11 位手机号", 400);
        }
        // 图形验证码一次性校验，成功后才返回数据
        captchaService.verify(dto.getCaptchaUuid(), dto.getCaptchaCode(), ip);

        List<OaMemberApply> applies = applyMapper.selectApplyByPhone(dto.getPhone());
        List<ProgressApplyVO> list = new ArrayList<>(applies.size());
        Map<String, String> progressStatusLabels = dictLabels.labels("oa_apply_status", APPLY_STATUS_LABELS);
        for (OaMemberApply apply : applies)
        {
            ProgressApplyVO vo = new ProgressApplyVO();
            vo.setApplyId(apply.getId());
            vo.setApplyNo(apply.getApplyNo());
            vo.setDeptId(apply.getDeptId());
            vo.setDeptName(StringUtils.isEmpty(apply.getDeptName()) ? getDeptName(apply.getDeptId())
                    : apply.getDeptName());
            vo.setStatus(apply.getStatus());
            vo.setStatusLabel(progressStatusLabels.get(apply.getStatus()));
            vo.setCreateTime(apply.getCreateTime());
            vo.setAuditRemark(apply.getAuditRemark());
            vo.setAuditUserName(apply.getAuditUserName());
            vo.setAuditTime(apply.getAuditTime());
            vo.setMemberId(apply.getMemberId());
            // H5-02 修改重提预填：携带原申请表单快照
            vo.setName(apply.getName());
            vo.setGender(apply.getGender());
            vo.setPhone(apply.getPhone());
            vo.setCollege(apply.getCollege());
            vo.setMajor(apply.getMajor());
            vo.setClassName(apply.getClassName());
            vo.setGrade(apply.getGrade());
            vo.setCohort(apply.getCohort());
            vo.setSkills(apply.getSkills());
            vo.setIntro(apply.getIntro());
            vo.setObeyAdjust(apply.getObeyAdjust());
            vo.setPhotoFileId(OaFileUrlHelper.parseFileId(apply.getPhotoUrl()));
            vo.setCardFileId(OaFileUrlHelper.parseFileId(apply.getCardUrl()));
            if (OaMemberConstants.APPLY_STATUS_APPROVED.equals(apply.getStatus()))
            {
                vo.setAccountTip("账号：" + apply.getStudentNo() + "｜初始密码：手机号后 6 位；首次登录请修改密码");
            }
            list.add(vo);
        }
        ProgressResultVO result = new ProgressResultVO();
        result.setQueryToken(captchaService.createQueryToken(dto.getPhone()));
        result.setApplyList(list);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(ApplyResubmitDTO dto)
    {
        if (dto == null || dto.getApplyId() == null)
        {
            throw new ServiceException("申请不存在", 404);
        }
        OaMemberApply snapshot = applyMapper.selectApplyById(dto.getApplyId());
        if (snapshot == null)
        {
            throw new ServiceException("申请不存在", 404);
        }
        // 身份校验：queryToken 绑定的手机号必须与申请原手机号一致（允许重提时换手机号）
        captchaService.requireQueryToken(dto.getQueryToken(), snapshot.getPhone());
        if (!OaMemberConstants.APPLY_STATUS_REJECTED.equals(snapshot.getStatus()))
        {
            throw new ServiceException("只有已驳回的申请才能修改重提", 409);
        }
        validateResubmitFields(dto, snapshot);

        String lockKey = LOCK_APPLY_SUBMIT + snapshot.getStudentNo() + ":" + snapshot.getDeptId();
        if (!lockService.tryLock(lockKey, LOCK_TTL_SECONDS))
        {
            throw new ServiceException("操作进行中，请勿重复提交", 409);
        }
        try
        {
            OaMemberApply apply = applyMapper.selectApplyByIdForUpdate(dto.getApplyId());
            if (apply == null || !OaMemberConstants.APPLY_STATUS_REJECTED.equals(apply.getStatus()))
            {
                throw new ServiceException("该申请已处理，请刷新后重试", 409);
            }
            // R14：新手机号不得关联其他学号
            OaMember phoneMember = memberMapper.selectByPhone(dto.getPhone());
            if (phoneMember != null && !apply.getStudentNo().equals(phoneMember.getStudentNo()))
            {
                throw new ServiceException("该手机号已关联学号 " + phoneMember.getStudentNo() + "，请人工核对", 400);
            }
            // R1：该部门不允许同时存在其他待审批
            if (applyMapper.countPending(apply.getStudentNo(), apply.getDeptId(), apply.getId()) > 0)
            {
                throw new ServiceException("该部门已有待审批申请，请勿重复提交", 409);
            }
            OaMemberApply update = new OaMemberApply();
            update.setId(apply.getId());
            update.setName(dto.getName());
            update.setGender(dto.getGender());
            update.setPhone(dto.getPhone());
            update.setCollege(dto.getCollege());
            update.setMajor(dto.getMajor());
            update.setClassName(dto.getClassName());
            update.setGrade(dto.getGrade());
            update.setCohort(StringUtils.isEmpty(dto.getCohort()) ? apply.getCohort() : dto.getCohort());
            update.setSkills(dto.getSkills());
            update.setIntro(dto.getIntro());
            update.setObeyAdjust(StringUtils.isEmpty(dto.getObeyAdjust()) ? "0" : dto.getObeyAdjust());
            // 附件语义：未重新上传（fileId 为 null）时保留原附件，避免重提清空
            update.setPhotoUrl(dto.getPhotoFileId() == null ? apply.getPhotoUrl()
                    : resolveApplyFileUrl(dto.getPhotoFileId(), "照片"));
            update.setCardUrl(dto.getCardFileId() == null ? apply.getCardUrl()
                    : resolveApplyFileUrl(dto.getCardFileId(), "简历"));
            int rows = applyMapper.resubmit(update);
            if (rows == 0)
            {
                throw new ServiceException("该申请已处理，请刷新后重试", 409);
            }
            int retryCount = apply.getRetryCount() == null ? 0 : apply.getRetryCount();
            writeApplyLog(apply.getId(), "重提", null, apply.getName(), "第 " + (retryCount + 1) + " 次修改重提");
        }
        finally
        {
            lockService.unlock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(WithdrawDTO dto)
    {
        if (dto == null || dto.getApplyId() == null)
        {
            throw new ServiceException("申请不存在", 404);
        }
        OaMemberApply snapshot = applyMapper.selectApplyById(dto.getApplyId());
        if (snapshot == null)
        {
            throw new ServiceException("申请不存在", 404);
        }
        captchaService.requireQueryToken(dto.getQueryToken(), dto.getPhone());
        if (!snapshot.getPhone().equals(dto.getPhone()))
        {
            throw new ServiceException("手机号与申请不一致", 403);
        }
        if (!OaMemberConstants.APPLY_STATUS_PENDING.equals(snapshot.getStatus()))
        {
            throw new ServiceException("该申请已处理，无法撤回", 409);
        }
        String lockKey = LOCK_APPLY_SUBMIT + snapshot.getStudentNo() + ":" + snapshot.getDeptId();
        if (!lockService.tryLock(lockKey, LOCK_TTL_SECONDS))
        {
            throw new ServiceException("操作进行中，请勿重复提交", 409);
        }
        try
        {
            int rows = applyMapper.withdraw(dto.getApplyId());
            if (rows == 0)
            {
                throw new ServiceException("该申请已处理，无法撤回", 409);
            }
            writeApplyLog(snapshot.getId(), "撤回", null, snapshot.getName(), "学生主动撤回");
        }
        finally
        {
            lockService.unlock(lockKey);
        }
    }

    // ============================ 内部方法 ============================

    /**
     * 由申请构造新成员档案（来源=自助申请，职务=干事，状态=在籍）
     */
    private OaMember newMemberFromApply(OaMemberApply apply, SysUser me)
    {
        Date now = DateUtils.getNowDate();
        OaMember member = new OaMember();
        member.setMemberNo(memberNoGenerator.nextMemberNo(now));
        member.setName(apply.getName());
        member.setGender(apply.getGender());
        member.setStudentNo(apply.getStudentNo());
        member.setPhone(apply.getPhone());
        member.setCollege(StringUtils.nvl(apply.getCollege(), ""));
        member.setMajor(StringUtils.nvl(apply.getMajor(), ""));
        member.setClassName(StringUtils.nvl(apply.getClassName(), ""));
        member.setGrade(StringUtils.nvl(apply.getGrade(), ""));
        member.setCohort(resolveCohort(apply.getCohort(), now));
        member.setDeptId(apply.getDeptId());
        member.setPosition(OaMemberConstants.POSITION_STAFF);
        member.setStatus(OaMemberConstants.MEMBER_STATUS_ACTIVE);
        member.setSource(OaMemberConstants.SOURCE_APPLY);
        member.setJoinDate(now);
        member.setEmail("");
        member.setWechat("");
        member.setQq("");
        member.setSkills(StringUtils.nvl(apply.getSkills(), ""));
        member.setIntro(StringUtils.nvl(apply.getIntro(), ""));
        member.setObeyAdjust(StringUtils.isEmpty(apply.getObeyAdjust()) ? "0" : apply.getObeyAdjust());
        member.setPhotoUrl(StringUtils.nvl(apply.getPhotoUrl(), ""));
        member.setCardUrl(StringUtils.nvl(apply.getCardUrl(), ""));
        member.setApplyId(apply.getId());
        member.setRemark("自助申请入会");
        member.setCreateBy(me.getUserName());
        return member;
    }

    /**
     * 同人已有退会/毕业档案：改部门并恢复在籍（保留 member_no / apply_id / 历史）
     */
    private OaMember reactivateMember(OaMember exist, OaMemberApply apply, SysUser me)
    {
        Date now = DateUtils.getNowDate();
        exist.setName(apply.getName());
        exist.setGender(apply.getGender());
        exist.setPhone(apply.getPhone());
        exist.setCollege(StringUtils.nvl(apply.getCollege(), ""));
        exist.setMajor(StringUtils.nvl(apply.getMajor(), ""));
        exist.setClassName(StringUtils.nvl(apply.getClassName(), ""));
        exist.setGrade(StringUtils.nvl(apply.getGrade(), ""));
        exist.setCohort(resolveCohort(apply.getCohort(), now));
        exist.setDeptId(apply.getDeptId());
        exist.setPosition(OaMemberConstants.POSITION_STAFF);
        exist.setStatus(OaMemberConstants.MEMBER_STATUS_ACTIVE);
        exist.setJoinDate(now);
        exist.setLeaveDate(null);
        exist.setSkills(StringUtils.nvl(apply.getSkills(), ""));
        exist.setIntro(StringUtils.nvl(apply.getIntro(), ""));
        exist.setObeyAdjust(StringUtils.isEmpty(apply.getObeyAdjust()) ? "0" : apply.getObeyAdjust());
        exist.setPhotoUrl(StringUtils.nvl(apply.getPhotoUrl(), ""));
        exist.setCardUrl(StringUtils.nvl(apply.getCardUrl(), ""));
        exist.setUpdateBy(me.getUserName());
        return exist;
    }

    /**
     * 由提交 DTO 构造待审批申请
     */
    private OaMemberApply buildApplyFromSubmit(ApplySubmitDTO dto, Long deptId, String photoUrl, String cardUrl,
            String ip)
    {
        Date now = DateUtils.getNowDate();
        OaMemberApply apply = new OaMemberApply();
        apply.setApplyNo(applyNoGenerator.nextApplyNo(now));
        apply.setStudentNo(dto.getStudentNo());
        apply.setName(dto.getName().trim());
        apply.setGender(dto.getGender());
        apply.setPhone(dto.getPhone());
        apply.setCollege(StringUtils.nvl(dto.getCollege(), ""));
        apply.setMajor(StringUtils.nvl(dto.getMajor(), ""));
        apply.setClassName(StringUtils.nvl(dto.getClassName(), ""));
        apply.setGrade(dto.getGrade());
        apply.setCohort(resolveCohort(dto.getCohort(), now));
        apply.setDeptId(deptId);
        apply.setSkills(StringUtils.nvl(dto.getSkills(), ""));
        apply.setIntro(StringUtils.nvl(dto.getIntro(), ""));
        apply.setObeyAdjust(StringUtils.isEmpty(dto.getObeyAdjust()) ? "0" : dto.getObeyAdjust());
        apply.setPhotoUrl(photoUrl);
        apply.setCardUrl(cardUrl);
        apply.setStatus(OaMemberConstants.APPLY_STATUS_PENDING);
        apply.setRetryCount(0);
        apply.setIp(StringUtils.nvl(ip, IpUtils.getIpAddr()));
        return apply;
    }

    /**
     * 写申请时间轴
     */
    private void writeApplyLog(Long applyId, String action, Long operatorId, String operatorName, String remark)
    {
        OaMemberApplyLog applyLog = new OaMemberApplyLog();
        applyLog.setApplyId(applyId);
        applyLog.setAction(action);
        applyLog.setOperatorId(operatorId);
        applyLog.setOperatorName(StringUtils.nvl(operatorName, ""));
        applyLog.setRemark(StringUtils.nvl(remark, ""));
        applyLogMapper.insertLog(applyLog);
    }

    /**
     * 列表字段装配
     */
    private void fillListVO(ApplyListVO vo, OaMemberApply apply, Map<String, String> gradeLabels,
            Map<String, String> statusLabels)
    {
        vo.setId(apply.getId());
        vo.setApplyNo(apply.getApplyNo());
        vo.setName(apply.getName());
        vo.setStudentNo(apply.getStudentNo());
        vo.setGender(apply.getGender());
        vo.setPhone(apply.getPhone());
        vo.setDeptId(apply.getDeptId());
        vo.setDeptName(StringUtils.isEmpty(apply.getDeptName()) ? getDeptName(apply.getDeptId())
                : apply.getDeptName());
        vo.setGrade(apply.getGrade());
        vo.setGradeLabel(gradeLabels.get(apply.getGrade()));
        vo.setStatus(apply.getStatus());
        vo.setStatusLabel(statusLabels.get(apply.getStatus()));
        vo.setAuditUserName(apply.getAuditUserName());
        vo.setAuditTime(apply.getAuditTime());
        vo.setAuditRemark(apply.getAuditRemark());
        vo.setCreateTime(apply.getCreateTime());
        // 代审标记：审批人不是该部门负责人（管理员/主席团非本部门负责人审批时）
        if (apply.getAuditUserId() == null)
        {
            vo.setProxy(false);
        }
        else
        {
            vo.setProxy(!deptLeaderResolver.isLeaderOf(apply.getDeptId(), apply.getAuditUserId()));
        }
    }

    /**
     * LoginUser 中是否包含指定角色（登录时已随用户加载，不产生 DB 查询）
     */
    private boolean hasRole(SysUser user, String roleKey)
    {
        if (user == null || user.getRoles() == null)
        {
            return false;
        }
        for (SysRole role : user.getRoles())
        {
            if (roleKey.equals(role.getRoleKey()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 当前登录人是否可审批该条
     */
    private boolean canAudit(OaMemberApply apply, SysUser me, boolean all)
    {
        if (!OaMemberConstants.APPLY_STATUS_PENDING.equals(apply.getStatus()))
        {
            return false;
        }
        return all || deptLeaderResolver.isLeaderOf(apply.getDeptId(), me.getUserId());
    }

    /**
     * 部门名称（异常时返回空串，不阻断主流程）
     */
    private String getDeptName(Long deptId)
    {
        if (deptId == null)
        {
            return "";
        }
        try
        {
            SysDept dept = deptService.selectDeptById(deptId);
            return dept == null ? "" : dept.getDeptName();
        }
        catch (Exception e)
        {
            log.warn("查询部门名称失败 deptId={}", deptId, e);
            return "";
        }
    }

    /**
     * 附件逻辑地址：fileId → /oa/file/{id}；并校验附件存在
     */
    private String resolveApplyFileUrl(Long fileId, String label)
    {
        if (fileId == null)
        {
            return "";
        }
        if (fileService.selectFileById(fileId) == null)
        {
            throw new ServiceException(label + "附件不存在，请重新上传", 400);
        }
        return OaFileUrlHelper.url(fileId);
    }

    /**
     * 届次缺省取入会年份（Q2）
     */
    private String resolveCohort(String cohort, Date now)
    {
        if (StringUtils.isNotEmpty(cohort))
        {
            return cohort;
        }
        return new SimpleDateFormat("yyyy").format(now);
    }

    /**
     * H5 提交字段校验
     */
    private void validateSubmitFields(ApplySubmitDTO dto)
    {
        if (StringUtils.isEmpty(dto.getName()) || !dto.getName().trim().matches(NAME_REGEX))
        {
            throw new ServiceException("请填写姓名（2–20 个中英文字符）", 400);
        }
        if (StringUtils.isEmpty(dto.getGender()) || (!"0".equals(dto.getGender()) && !"1".equals(dto.getGender())))
        {
            throw new ServiceException("请选择性别", 400);
        }
        if (StringUtils.isEmpty(dto.getStudentNo()) || !dto.getStudentNo().matches(STUDENT_NO_REGEX))
        {
            throw new ServiceException("学号需为 8–12 位数字", 400);
        }
        if (StringUtils.isEmpty(dto.getPhone()) || !dto.getPhone().matches(PHONE_REGEX))
        {
            throw new ServiceException("请输入 11 位手机号", 400);
        }
        if (StringUtils.isEmpty(dto.getCollege()) || dto.getCollege().trim().length() > 100)
        {
            throw new ServiceException("请填写学院", 400);
        }
        if (StringUtils.isEmpty(dto.getMajor()) || dto.getMajor().trim().length() > 100)
        {
            throw new ServiceException("请填写专业", 400);
        }
        if (StringUtils.isEmpty(dto.getClassName()) || dto.getClassName().trim().length() > 50)
        {
            throw new ServiceException("请填写班级", 400);
        }
        if (StringUtils.isEmpty(dto.getGrade()))
        {
            throw new ServiceException("请选择年级", 400);
        }
        if (StringUtils.isNotEmpty(dto.getSkills()) && dto.getSkills().length() > 500)
        {
            throw new ServiceException("特长最多 500 字", 400);
        }
        if (StringUtils.isNotEmpty(dto.getIntro()) && dto.getIntro().length() > 1000)
        {
            throw new ServiceException("自我介绍最多 1000 字", 400);
        }
        if (StringUtils.isNotEmpty(dto.getObeyAdjust())
                && !"0".equals(dto.getObeyAdjust()) && !"1".equals(dto.getObeyAdjust()))
        {
            throw new ServiceException("是否服从调剂取值非法", 400);
        }
    }

    /**
     * H5 重提字段校验（驳回后修改）
     */
    private void validateResubmitFields(ApplyResubmitDTO dto, OaMemberApply apply)
    {
        if (StringUtils.isEmpty(dto.getName()) || !dto.getName().trim().matches(NAME_REGEX))
        {
            throw new ServiceException("请填写姓名（2–20 个中英文字符）", 400);
        }
        if (StringUtils.isEmpty(dto.getGender()) || (!"0".equals(dto.getGender()) && !"1".equals(dto.getGender())))
        {
            throw new ServiceException("请选择性别", 400);
        }
        if (StringUtils.isEmpty(dto.getPhone()) || !dto.getPhone().matches(PHONE_REGEX))
        {
            throw new ServiceException("请输入 11 位手机号", 400);
        }
        if (StringUtils.isEmpty(dto.getCollege()) || dto.getCollege().trim().length() > 100)
        {
            throw new ServiceException("请填写学院", 400);
        }
        if (StringUtils.isEmpty(dto.getMajor()) || dto.getMajor().trim().length() > 100)
        {
            throw new ServiceException("请填写专业", 400);
        }
        if (StringUtils.isEmpty(dto.getClassName()) || dto.getClassName().trim().length() > 50)
        {
            throw new ServiceException("请填写班级", 400);
        }
        if (StringUtils.isEmpty(dto.getGrade()))
        {
            throw new ServiceException("请选择年级", 400);
        }
        if (StringUtils.isNotEmpty(dto.getSkills()) && dto.getSkills().length() > 500)
        {
            throw new ServiceException("特长最多 500 字", 400);
        }
        if (StringUtils.isNotEmpty(dto.getIntro()) && dto.getIntro().length() > 1000)
        {
            throw new ServiceException("自我介绍最多 1000 字", 400);
        }
    }

}
