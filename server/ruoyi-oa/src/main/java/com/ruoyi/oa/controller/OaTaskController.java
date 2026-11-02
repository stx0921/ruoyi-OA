package com.ruoyi.oa.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.oa.domain.vo.OaTaskVo;
import com.ruoyi.oa.service.IOaLeaveService;

/**
 * 待办 / 已办 Controller
 *
 * <p>注意：待办/已办数据来自 Flowable 的 TaskQuery，不是 MyBatis 查询，
 * 因此不能用 {@code startPage()} + {@code getDataTable()}：
 * PageHelper 的线程变量会落到之后第一条 MyBatis 语句上，
 * 而 {@code OaLeaveMapper.selectOaLeaveByProcessInstanceId} 本身带 {@code limit 1}，
 * 两者叠加会生成 {@code limit 1 LIMIT ?} 这样的非法 SQL。
 * 这里改为在内存中切页。</p>
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
        return toPageData(oaLeaveService.selectTodoTasks());
    }

    /**
     * 我的已办
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:list')")
    @GetMapping("/done")
    public TableDataInfo done()
    {
        return toPageData(oaLeaveService.selectDoneTasks());
    }

    /**
     * 内存分页，保持与 RuoYi TableDataInfo 一致的返回结构
     */
    private TableDataInfo toPageData(List<OaTaskVo> all)
    {
        List<OaTaskVo> source = all == null ? new ArrayList<>() : all;
        PageDomain pageDomain = TableSupport.buildPageRequest();
        int pageNum = pageDomain.getPageNum() == null || pageDomain.getPageNum() < 1 ? 1 : pageDomain.getPageNum();
        int pageSize = pageDomain.getPageSize() == null || pageDomain.getPageSize() < 1 ? 10 : pageDomain.getPageSize();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, source.size());
        List<OaTaskVo> rows = from >= source.size() ? new ArrayList<>() : new ArrayList<>(source.subList(from, to));

        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(rows);
        rspData.setTotal(source.size());
        return rspData;
    }
}
