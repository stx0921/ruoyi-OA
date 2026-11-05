package com.ruoyi.oa.project.controller;

import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oa.project.dto.ProjectFinishDTO;
import com.ruoyi.oa.project.dto.ProjectQuery;
import com.ruoyi.oa.project.dto.ProjectSaveDTO;
import com.ruoyi.oa.project.service.IOaProjectService;

/**
 * 项目管理 Controller（PC-05）
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/project")
public class OaProjectController extends BaseController
{
    @Autowired
    private IOaProjectService projectService;

    /**
     * 项目列表（含任务数/已完成数/参与人数）
     */
    @PreAuthorize("@ss.hasPermi('oa:project:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProjectQuery query)
    {
        startPage();
        return getDataTable(projectService.selectProjectList(query));
    }

    /**
     * 项目详情（含参与成员）
     */
    @PreAuthorize("@ss.hasPermi('oa:project:query')")
    @GetMapping("/{projectId}")
    public AjaxResult getInfo(@PathVariable("projectId") Long projectId)
    {
        return success(projectService.selectProjectDetail(projectId));
    }

    /**
     * 新建项目
     */
    @PreAuthorize("@ss.hasPermi('oa:project:add')")
    @Log(title = "项目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProjectSaveDTO dto)
    {
        return success(projectService.insertProject(dto));
    }

    /**
     * 编辑项目（已结束项目拒绝编辑）
     */
    @PreAuthorize("@ss.hasPermi('oa:project:edit')")
    @Log(title = "项目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProjectSaveDTO dto)
    {
        projectService.updateProject(dto);
        return success();
    }

    /**
     * 结束项目（归档）：存在未完成任务且未 force 时返回 409，data.unfinishedCount 供前端二次确认
     */
    @PreAuthorize("@ss.hasPermi('oa:project:edit')")
    @Log(title = "项目结束", businessType = BusinessType.UPDATE)
    @PutMapping("/finish")
    public AjaxResult finish(@RequestBody ProjectFinishDTO dto)
    {
        if (dto == null || dto.getProjectId() == null)
        {
            throw new ServiceException("项目不存在", 404);
        }
        int unfinishedCount = projectService.prepareFinish(dto.getProjectId());
        if (!Boolean.TRUE.equals(dto.getForce()) && unfinishedCount > 0)
        {
            AjaxResult result = AjaxResult.error(409,
                    "项目还有 " + unfinishedCount + " 个未完成任务，结束后项目归档只读，任务不可再流转");
            result.put(AjaxResult.DATA_TAG, Collections.singletonMap("unfinishedCount", unfinishedCount));
            return result;
        }
        int count = projectService.finishProject(dto.getProjectId());
        return success(Collections.singletonMap("unfinishedCount", count));
    }

    /**
     * 批量删除项目（硬删除）：项目下还有任务或周期规则时 409
     */
    @PreAuthorize("@ss.hasPermi('oa:project:remove')")
    @Log(title = "项目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{projectIds}")
    public AjaxResult remove(@PathVariable Long[] projectIds)
    {
        projectService.deleteProjects(projectIds);
        return success();
    }
}
