package com.ruoyi.oa.volunteer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.oa.volunteer.dto.PublicSignupDTO;
import com.ruoyi.oa.volunteer.service.IVolunteerService;

/**
 * 志愿活动 H5 免登录接口（类级 {@code @Anonymous}，只暴露必要字段）
 *
 * @author ruoyi-oa
 */
@Anonymous
@RestController
@RequestMapping("/oa/volunteer/public")
public class VolunteerPublicController extends BaseController
{
    @Autowired
    private IVolunteerService volunteerService;

    /**
     * H5 活动详情（不含创建人/审核人等敏感信息）
     */
    @GetMapping("/activity/{id}")
    public AjaxResult activity(@PathVariable("id") Long id)
    {
        return success(volunteerService.selectPublicActivity(id));
    }

    /**
     * H5 报名（按 IP 限流 10 次/分钟）
     */
    @PostMapping("/signup")
    @RateLimiter(count = 10, time = 60, limitType = LimitType.IP)
    @Log(title = "志愿活动", businessType = BusinessType.INSERT)
    public AjaxResult signup(@RequestBody PublicSignupDTO dto)
    {
        return success(volunteerService.publicSignup(dto));
    }

    /**
     * H5 报名结果查询（按 IP 限流 30 次/分钟；studentNo + name 必须匹配）
     */
    @GetMapping("/result")
    @RateLimiter(count = 30, time = 60, limitType = LimitType.IP)
    public AjaxResult result(@RequestParam(value = "activityId", required = false) Long activityId,
            @RequestParam(value = "studentNo", required = false) String studentNo,
            @RequestParam(value = "name", required = false) String name)
    {
        return success(volunteerService.queryPublicResult(activityId, studentNo, name));
    }

}
