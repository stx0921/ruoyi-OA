package com.ruoyi.oa.apply.service;

import java.util.List;
import com.ruoyi.oa.apply.domain.OaMemberApply;
import com.ruoyi.oa.apply.dto.ApplyAuditDTO;
import com.ruoyi.oa.apply.dto.ApplyResubmitDTO;
import com.ruoyi.oa.apply.dto.ApplySubmitDTO;
import com.ruoyi.oa.apply.dto.ProgressDTO;
import com.ruoyi.oa.apply.dto.WithdrawDTO;
import com.ruoyi.oa.apply.vo.ApplyDetailVO;
import com.ruoyi.oa.apply.vo.ApplyListVO;
import com.ruoyi.oa.apply.vo.AuditResultVO;
import com.ruoyi.oa.apply.vo.ProgressResultVO;
import com.ruoyi.oa.apply.vo.SubmitResultVO;

/**
 * 入会申请 Service 接口（PC 审批 + H5 公开）
 *
 * @author ruoyi-oa
 */
public interface IOaMemberApplyService
{
    /**
     * 查询审批列表（scope=todo/done/all，带数据权限）
     *
     * @param scope todo=待我审批 / done=我已审批 / all=全部
     * @param query 查询条件
     * @return 列表
     */
    List<ApplyListVO> selectApplyList(String scope, OaMemberApply query);

    /**
     * 查询申请详情（完整表单 + 附件 + 时间轴）
     */
    ApplyDetailVO selectApplyDetail(Long id);

    /**
     * 批量审批（单条独立事务 + Redis 锁 + 行锁二次校验，单次 ≤50 条）
     */
    AuditResultVO audit(ApplyAuditDTO dto);

    /**
     * H5 提交入会申请（R1/R2/R4/R8/R14）
     *
     * @param dto 提交参数
     * @param ip  请求 IP
     */
    SubmitResultVO submitApply(ApplySubmitDTO dto, String ip);

    /**
     * H5 报名进度查询（手机号 + 图形验证码 → queryToken + 申请列表）
     */
    ProgressResultVO progress(ProgressDTO dto, String ip);

    /**
     * 驳回后修改重提（R5）
     */
    void resubmit(ApplyResubmitDTO dto);

    /**
     * 撤回申请（R6）
     */
    void withdraw(WithdrawDTO dto);

}
