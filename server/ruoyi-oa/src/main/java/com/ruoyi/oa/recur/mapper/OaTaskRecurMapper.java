package com.ruoyi.oa.recur.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.recur.domain.OaTaskRecur;
import com.ruoyi.oa.recur.dto.RecurQuery;

/**
 * 周期任务规则 Mapper
 *
 * @author ruoyi-oa
 */
public interface OaTaskRecurMapper
{
    /**
     * 规则列表（需配合 @DataScope，SQL 中拼 ${params.dataScope}）
     */
    List<OaTaskRecur> selectRecurList(RecurQuery query);

    /**
     * 按 ID 查询规则（含项目名/主责人/创建人姓名）
     */
    OaTaskRecur selectById(Long recurId);

    /**
     * 按 ID 查询规则并加行锁（Job/编辑二次校验）
     */
    OaTaskRecur selectByIdForUpdate(Long recurId);

    /**
     * 新增规则
     */
    int insertRecur(OaTaskRecur recur);

    /**
     * 更新规则模板（名称/描述/项目/星期/时刻/主责人/优先级/标签/生效日/下次生成日）
     */
    int updateRecur(OaTaskRecur recur);

    /**
     * 状态变更（已结束规则不可再变更：status <> '2'）
     */
    int updateStatus(@Param("recurId") Long recurId, @Param("status") String status,
            @Param("nextDate") Date nextDate);

    /**
     * 仅更新下次生成日
     */
    int updateNextDate(@Param("recurId") Long recurId, @Param("nextDate") Date nextDate);

    /**
     * Job 扫描：启用 + 已生效 + 今日命中星期
     *
     * @param today   业务今天（yyyy-MM-dd）
     * @param weekday ISO 星期（1=周一…7=周日）
     */
    List<OaTaskRecur> selectDueRules(@Param("today") String today, @Param("weekday") int weekday);

    /**
     * 某主责人启用中的规则（退会联动暂停用）
     */
    List<OaTaskRecur> selectActiveByAssignee(Long assigneeId);

    /**
     * 某项目下启用中的规则（项目归档自动暂停用）
     */
    List<OaTaskRecur> selectActiveByProject(Long projectId);

    /**
     * 批量硬删除规则（不存在的 ID 自动跳过，幂等）
     */
    int deleteByIds(@Param("recurIds") Long[] recurIds);

    /**
     * 统计项目下规则数（项目删除前置校验）
     */
    int countByProjectIds(@Param("projectIds") Long[] projectIds);
}
