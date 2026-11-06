package com.ruoyi.oa.apply.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.oa.apply.dto.ApplyResubmitDTO;
import com.ruoyi.oa.apply.dto.ApplySubmitDTO;
import com.ruoyi.oa.apply.dto.ProgressDTO;
import com.ruoyi.oa.apply.dto.WithdrawDTO;
import com.ruoyi.oa.apply.service.IOaFileService;
import com.ruoyi.oa.apply.service.IOaMemberApplyService;
import com.ruoyi.oa.apply.support.OaCaptchaService;
import com.ruoyi.oa.member.service.IOaDeptService;

/**
 * H5 学生端公开接口 Controller（类级 {@code @Anonymous} 免登录）
 * <p>v1 用图形验证码替代短信，接口错误统一返回 400/429（不返回 401）。</p>
 *
 * @author ruoyi-oa
 */
@Anonymous
@RestController
@RequestMapping("/oa/apply/public")
public class OaApplyPublicController extends BaseController
{
    @Autowired
    private OaCaptchaService captchaService;

    @Autowired
    private IOaMemberApplyService applyService;

    @Autowired
    private IOaFileService fileService;

    @Autowired
    private IOaDeptService deptService;

    /**
     * P1 获取图形验证码（5 分钟有效）
     */
    @GetMapping("/captcha")
    public AjaxResult captcha()
    {
        return success(captchaService.generate());
    }

    /**
     * P2 校验图形验证码，返回 30 分钟 verifyToken（上传/提交使用）
     */
    @PostMapping("/verify-captcha")
    public AjaxResult verifyCaptcha(@RequestBody Map<String, String> body)
    {
        String uuid = body == null ? null : body.get("uuid");
        String code = body == null ? null : body.get("code");
        return success(captchaService.verify(uuid, code, IpUtils.getIpAddr()));
    }

    /**
     * P3 意向部门下拉（组织根节点下启用部门）
     */
    @GetMapping("/depts")
    public AjaxResult depts()
    {
        return success(deptService.selectAllDeptOptions());
    }

    /**
     * P4 匿名上传附件（照片/学生证，需 verifyToken）
     */
    @PostMapping("/upload")
    @Log(title = "H5附件上传", businessType = BusinessType.INSERT)
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
            @RequestParam("bizType") String bizType,
            @RequestParam(value = "verifyToken", required = false) String verifyToken)
    {
        return success(fileService.uploadPublic(file, bizType, verifyToken));
    }

    /**
     * P5 提交入会申请（R1/R2/R4/R14，同号 24h ≤5 次）
     */
    @PostMapping("/submit")
    @Log(title = "H5入会报名", businessType = BusinessType.INSERT)
    public AjaxResult submit(@RequestBody ApplySubmitDTO dto)
    {
        return success(applyService.submitApply(dto, IpUtils.getIpAddr()));
    }

    /**
     * P6 报名进度查询（手机号 + 图形验证码 → queryToken + 申请列表）
     */
    @PostMapping("/progress")
    public AjaxResult progress(@RequestBody ProgressDTO dto)
    {
        return success(applyService.progress(dto, IpUtils.getIpAddr()));
    }

    /**
     * P7 驳回后修改重提（R5，凭 queryToken 校验身份）
     */
    @PutMapping("/resubmit")
    @Log(title = "H5报名重提", businessType = BusinessType.UPDATE)
    public AjaxResult resubmit(@RequestBody ApplyResubmitDTO dto)
    {
        applyService.resubmit(dto);
        return success();
    }

    /**
     * P8 撤回申请（R6，凭 queryToken 校验身份）
     */
    @PutMapping("/withdraw")
    @Log(title = "H5报名撤回", businessType = BusinessType.UPDATE)
    public AjaxResult withdraw(@RequestBody WithdrawDTO dto)
    {
        applyService.withdraw(dto);
        return success();
    }

}
