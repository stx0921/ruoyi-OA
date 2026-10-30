package com.ruoyi.oa.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.oa.domain.vo.OaTaskVo;
import com.ruoyi.oa.service.IOaLeaveService;

/**
 * 待办 / 已办 Controller
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/task")
public class OaTaskController extends BaseController
{
    @Autowired
    private IOaLeaveService oaLeaveService;

    /**
     * 我的待办
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:list')")
    @GetMapping("/todo")
    public TableDataInfo todo()
    {
        startPage();
        List<OaTaskVo> list = oaLeaveService.selectTodoTasks();
        return getDataTable(list);
    }

    /**
     * 我的已办
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:list')")
    @GetMapping("/done")
    public TableDataInfo done()
    {
        startPage();
        List<OaTaskVo> list = oaLeaveService.selectDoneTasks();
        return getDataTable(list);
    }
}
