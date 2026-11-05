package com.ruoyi.oa.project.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.project.domain.OaProjectMember;

/**
 * 项目参与成员 Mapper
 *
 * <p>契约 §3.3 要求对外提供：isMember / selectByProjectId（供任务模块注入使用）。</p>
 *
 * @author ruoyi-oa
 */
public interface OaProjectMemberMapper
{
    /**
     * 项目参与成员列表（含账号名/姓名/部门，供详情展示）
     */
    List<OaProjectMember> selectByProjectId(Long projectId);

    /**
     * 某账号是否是项目参与成员
     */
    boolean isMember(@Param("projectId") Long projectId, @Param("userId") Long userId);

    /**
     * 新增参与成员
     */
    int insertMember(OaProjectMember member);

    /**
     * 批量新增参与成员（唯一键 uk_project_user 命中时 INSERT IGNORE）
     */
    int insertBatch(@Param("list") List<OaProjectMember> list);

    /**
     * 清空项目参与成员（编辑时先删后建）
     */
    int deleteByProjectId(Long projectId);

    /**
     * 查询项目参与成员账号 ID 列表
     */
    List<Long> selectUserIdsByProjectId(Long projectId);

    /**
     * 批量删除多个项目的参与成员（项目删除时级联清理）
     */
    int deleteByProjectIds(@Param("projectIds") Long[] projectIds);
}
