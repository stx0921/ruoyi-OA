package com.ruoyi.oa.member.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
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
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.oa.member.domain.OaMember;
import com.ruoyi.oa.member.dto.MemberAddDTO;
import com.ruoyi.oa.member.dto.MemberQuery;
import com.ruoyi.oa.member.dto.MemberSelfDTO;
import com.ruoyi.oa.member.dto.MemberStatusDTO;
import com.ruoyi.oa.member.dto.MemberTransferDTO;
import com.ruoyi.oa.member.dto.MemberUpdateDTO;
import com.ruoyi.oa.member.service.IOaDeptService;
import com.ruoyi.oa.member.service.IOaMemberService;

/**
 * 成员管理 Controller（PC-01 / PC-02）
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/member")
public class OaMemberController extends BaseController
{
    @Autowired
    private IOaMemberService memberService;

    @Autowired
    private IOaDeptService deptService;

    /**
     * 部门下拉选项（成员筛选 / 详情部门下拉；部长仅本部门）
     */
    @PreAuthorize("@ss.hasPermi('oa:member:list')")
    @GetMapping("/dept-options")
    public AjaxResult deptOptions()
    {
        return success(deptService.selectDeptOptions());
    }

    /**
     * 成员列表
     */
    @PreAuthorize("@ss.hasPermi('oa:member:list')")
    @GetMapping("/list")
    public TableDataInfo list(MemberQuery query)
    {
        startPage();
        return getDataTable(memberService.selectMemberList(query));
    }

    /**
     * 成员统计卡片
     */
    @PreAuthorize("@ss.hasPermi('oa:member:list')")
    @GetMapping("/stats")
    public AjaxResult stats(MemberQuery query)
    {
        return success(memberService.selectStats(query));
    }

    /**
     * 当前登录人档案（H5-03 我的资料）
     */
    @PreAuthorize("@ss.hasPermi('oa:member:list')")
    @GetMapping("/self")
    public AjaxResult self()
    {
        return success(memberService.selectSelfDetail());
    }

    /**
     * 干事维护本人资料（白名单字段）
     */
    @PreAuthorize("@ss.hasPermi('oa:member:edit')")
    @Log(title = "我的资料", businessType = BusinessType.UPDATE)
    @PutMapping("/self")
    public AjaxResult updateSelf(@RequestBody MemberSelfDTO dto)
    {
        memberService.updateSelf(dto);
        return success();
    }

    /**
     * 成员详情
     */
    @PreAuthorize("@ss.hasPermi('oa:member:query')")
    @GetMapping("/{memberId}")
    public AjaxResult getInfo(@PathVariable("memberId") Long memberId)
    {
        return success(memberService.selectMemberDetail(memberId));
    }

    /**
     * 手动新增成员（免审）
     */
    @PreAuthorize("@ss.hasPermi('oa:member:add')")
    @Log(title = "成员管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MemberAddDTO dto)
    {
        return success(memberService.insertMember(dto));
    }

    /**
     * 编辑成员
     */
    @PreAuthorize("@ss.hasPermi('oa:member:edit')")
    @Log(title = "成员管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MemberUpdateDTO dto)
    {
        memberService.updateMember(dto);
        return success();
    }

    /**
     * 状态变更（退会 / 毕业 / 恢复在籍）
     */
    @PreAuthorize("@ss.hasPermi('oa:member:edit')")
    @Log(title = "成员状态", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult changeStatus(@RequestBody MemberStatusDTO dto)
    {
        memberService.changeStatus(dto);
        return success();
    }

    /**
     * 调部门 / 升职（免审）
     */
    @PreAuthorize("@ss.hasPermi('oa:member:edit')")
    @Log(title = "成员调部门", businessType = BusinessType.UPDATE)
    @PutMapping("/transfer")
    public AjaxResult transfer(@RequestBody MemberTransferDTO dto)
    {
        memberService.transfer(dto);
        return success();
    }

    /**
     * 软删除成员（del_flag=1，账号停用、数据保留；支持批量）
     */
    @PreAuthorize("@ss.hasPermi('oa:member:remove')")
    @Log(title = "成员管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{memberIds}")
    public AjaxResult remove(@PathVariable Long[] memberIds)
    {
        memberService.deleteMembers(memberIds);
        return success();
    }

    /**
     * 导出成员数据（POST 返回 xlsx 流）
     */
    @PreAuthorize("@ss.hasPermi('oa:member:export')")
    @Log(title = "成员管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemberQuery query)
    {
        List<OaMember> list = memberService.selectMemberListForExport(query);
        if (list != null && list.size() > 10000)
        {
            throw new ServiceException("单次最多导出 10000 条，请先缩小筛选范围", 400);
        }
        ExcelUtil<OaMember> util = new ExcelUtil<OaMember>(OaMember.class);
        util.exportExcel(response, list, "成员数据");
    }
}
