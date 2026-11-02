package com.ruoyi.oa.apply.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.apply.domain.OaMemberApply;

/**
 * 入会申请 Mapper 接口
 *
 * @author ruoyi-oa
 */
public interface OaMemberApplyMapper
{
    /**
     * 查询审批列表（支持 scope=todo/done/all，配合 {@code @DataScope} 数据范围过滤）
     *
     * @param query 查询条件（scope/scopeUserId/scopeAll 放在 params 中）
     * @return 申请列表
     */
    List<OaMemberApply> selectApplyList(OaMemberApply query);

    /**
     * 按主键查询申请
     */
    OaMemberApply selectApplyById(@Param("id") Long id);

    /**
     * 按主键查询并加行锁（SELECT ... FOR UPDATE）
     */
    OaMemberApply selectApplyByIdForUpdate(@Param("id") Long id);

    /**
     * 锁同学号同部门待审批申请（R1 二次校验，无则返回 null）
     */
    OaMemberApply selectPendingByStudentAndDeptForUpdate(@Param("studentNo") String studentNo,
            @Param("deptId") Long deptId);

    /**
     * 锁同学号全部待审批申请（R3 关闭其余）
     */
    List<OaMemberApply> selectPendingByStudentNoForUpdate(@Param("studentNo") String studentNo);

    /**
     * 按手机号查询申请列表（H5 进度查询）
     */
    List<OaMemberApply> selectApplyByPhone(@Param("phone") String phone);

    /**
     * 新增申请
     */
    int insertApply(OaMemberApply apply);

    /**
     * 审批通过（条件更新：仅 status='0' 生效）
     */
    int approve(@Param("id") Long id, @Param("memberId") Long memberId,
            @Param("auditUserId") Long auditUserId, @Param("auditUserName") String auditUserName,
            @Param("auditRemark") String auditRemark);

    /**
     * 审批驳回/自动关闭（条件更新：仅 status='0' 生效）
     */
    int updateAuditStatus(@Param("id") Long id, @Param("status") String status,
            @Param("auditUserId") Long auditUserId, @Param("auditUserName") String auditUserName,
            @Param("auditRemark") String auditRemark);

    /**
     * 驳回后修改重提：白名单字段覆盖，status→0、retry_count+1、清审批信息（apply_no/create_time 不变）
     */
    int resubmit(OaMemberApply apply);

    /**
     * 候选人撤回（条件更新：仅 status='0' 生效）
     */
    int withdraw(@Param("id") Long id);

    /**
     * 统计同学号同部门待审批数量（excludeId 排除自身）
     */
    int countPending(@Param("studentNo") String studentNo, @Param("deptId") Long deptId,
            @Param("excludeId") Long excludeId);

    /**
     * 查询指定日期前缀下最大申请编号（单号生成器 DB 兜底）
     */
    String selectMaxApplyNo(@Param("prefix") String prefix);

}
