package com.ruoyi.oa.member.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.oa.member.domain.OaMember;
import com.ruoyi.oa.member.dto.MemberAddDTO;
import com.ruoyi.oa.member.dto.MemberQuery;
import com.ruoyi.oa.member.dto.MemberSelfDTO;
import com.ruoyi.oa.member.dto.MemberStatusDTO;
import com.ruoyi.oa.member.dto.MemberTransferDTO;
import com.ruoyi.oa.member.dto.MemberUpdateDTO;
import com.ruoyi.oa.member.vo.MemberDetailVO;
import com.ruoyi.oa.member.vo.MemberListVO;
import com.ruoyi.oa.member.vo.MemberStatsVO;

/**
 * 成员档案 Service
 *
 * @author ruoyi-oa
 */
public interface IOaMemberService
{
    /**
     * 查询成员列表（数据权限 + 脱敏）
     */
    List<MemberListVO> selectMemberList(MemberQuery query);

    /**
     * 查询成员详情
     */
    MemberDetailVO selectMemberDetail(Long memberId);

    /**
     * 手动新增成员（免审，直接开号）
     */
    Map<String, Object> insertMember(MemberAddDTO dto);

    /**
     * 编辑成员
     */
    void updateMember(MemberUpdateDTO dto);

    /**
     * 状态变更（退会 / 毕业 / 恢复在籍）
     */
    void changeStatus(MemberStatusDTO dto);

    /**
     * 调部门 / 升职（免审）
     */
    void transfer(MemberTransferDTO dto);

    /**
     * 软删除成员（del_flag=1，账号停用，数据保留）
     */
    void deleteMembers(Long[] memberIds);

    /**
     * 统计卡片
     */
    MemberStatsVO selectStats(MemberQuery query);

    /**
     * 导出用原始列表（不做脱敏，按角色限部门）
     */
    List<OaMember> selectMemberListForExport(MemberQuery query);

    /**
     * 当前登录人档案（H5-03）
     */
    MemberDetailVO selectSelfDetail();

    /**
     * 干事维护本人资料（白名单字段）
     */
    void updateSelf(MemberSelfDTO dto);

    /**
     * 按学号查询成员（供申请/导入模块复用）
     */
    OaMember selectByStudentNo(String studentNo);
}
