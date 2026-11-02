package com.ruoyi.oa.member.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.member.domain.OaMember;
import com.ruoyi.oa.member.dto.MemberQuery;

/**
 * 成员档案 Mapper
 *
 * @author ruoyi-oa
 */
public interface OaMemberMapper
{
    /**
     * 查询成员列表（需配合 @DataScope，SQL 末尾拼 ${params.dataScope}）
     */
    List<OaMember> selectMemberList(MemberQuery query);

    /**
     * 按 ID 查询成员（联查部门、账号状态）
     */
    OaMember selectMemberById(Long memberId);

    /**
     * 按 ID 查询成员并加行锁
     */
    OaMember selectMemberByIdForUpdate(Long memberId);

    /**
     * 按学号查询成员
     */
    OaMember selectByStudentNo(String studentNo);

    /**
     * 按学号查询成员并加行锁
     */
    OaMember selectByStudentNoForUpdate(String studentNo);

    /**
     * 按手机号查询成员
     */
    OaMember selectByPhone(String phone);

    /**
     * 按账号 ID 查询成员（本人档案）
     */
    OaMember selectByUserId(Long userId);

    /**
     * 新增成员
     */
    int insertMember(OaMember member);

    /**
     * 更新成员（按 memberId 全量更新业务字段）
     */
    int updateMember(OaMember member);

    /**
     * 回写关联账号
     */
    int updateMemberUserId(@Param("memberId") Long memberId, @Param("userId") Long userId);

    /**
     * 状态变更
     */
    int updateMemberStatus(@Param("memberId") Long memberId, @Param("status") String status,
            @Param("leaveDate") Date leaveDate);

    /**
     * 调部门 / 改职务
     */
    int updateMemberDeptPosition(@Param("memberId") Long memberId, @Param("deptId") Long deptId,
            @Param("position") String position);

    /**
     * 干事维护本人资料（白名单字段）
     */
    int updateMemberSelf(@Param("memberId") Long memberId, @Param("wechat") String wechat,
            @Param("qq") String qq, @Param("email") String email, @Param("skills") String skills,
            @Param("intro") String intro, @Param("photoUrl") String photoUrl);

    /**
     * 查询某年最大成员编号序号（member_no like 'M{year}-%'）
     */
    Long selectMaxMemberNoSeq(@Param("year") String year);

    /**
     * 统计在籍人数（带数据权限）
     */
    Long countActive(MemberQuery query);

    /**
     * 统计待审批申请数（带数据权限）
     */
    Long countPendingApply(MemberQuery query);

    /**
     * 统计本月新增成员数（带数据权限）
     */
    Long countMonthJoin(MemberQuery query);

    /**
     * 统计本月退会/毕业成员数（带数据权限）
     */
    Long countMonthLeave(MemberQuery query);

    /**
     * 按部门统计在籍人数
     */
    List<Map<String, Object>> selectMemberCountGroupByDept();

    /**
     * 校验某账号是否是某部门在籍成员（部门负责人设置用）
     */
    int countActiveMemberByUserIdAndDept(@Param("userId") Long userId, @Param("deptId") Long deptId);
}
