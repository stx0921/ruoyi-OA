package com.ruoyi.oa.recur.service;

import java.util.List;
import com.ruoyi.oa.recur.dto.RecurQuery;
import com.ruoyi.oa.recur.dto.RecurSaveDTO;
import com.ruoyi.oa.recur.dto.RecurStatusDTO;
import com.ruoyi.oa.recur.vo.RecurListVO;
import com.ruoyi.oa.recur.vo.RecurSaveResultVO;

/**
 * 周期任务规则 Service（PC-06）
 *
 * @author ruoyi-oa
 */
public interface IOaTaskRecurService
{
    /**
     * 规则列表（@DataScope 部门数据权限，含下次生成日）
     */
    List<RecurListVO> selectRecurList(RecurQuery query);

    /**
     * 新建规则（week_days 归一化、due_time 必填、start_date ≥ 今天、主责人在册）
     */
    RecurSaveResultVO insertRecur(RecurSaveDTO dto);

    /**
     * 编辑规则：applyScope=one 仅改最近生成实例（标题/描述/截止时刻/优先级/标签）；
     * applyScope=all 改规则模板，历史任务不动
     */
    void updateRecur(RecurSaveDTO dto);

    /**
     * 暂停 / 恢复 / 结束规则（已结束不可恢复，恢复时重算 next_date）
     */
    void updateStatus(RecurStatusDTO dto);

    /**
     * 批量删除周期规则（硬删除）：已生成任务保留（recur_id 置 NULL）；清理规则站内信。
     * 权限：管理员/主席团任意；部长仅本部门规则；不存在或已删除的 ID 跳过。
     */
    void deleteRecurs(Long[] recurIds);
}
