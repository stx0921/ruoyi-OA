package com.ruoyi.oa.feedback.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.github.pagehelper.Page;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.apply.domain.OaFile;
import com.ruoyi.oa.apply.mapper.OaFileMapper;
import com.ruoyi.oa.apply.service.IOaFileService;
import com.ruoyi.oa.feedback.domain.OaFeedback;
import com.ruoyi.oa.feedback.dto.FeedbackQuery;
import com.ruoyi.oa.feedback.dto.FeedbackReplyDTO;
import com.ruoyi.oa.feedback.dto.FeedbackSubmitDTO;
import com.ruoyi.oa.feedback.mapper.OaFeedbackMapper;
import com.ruoyi.oa.feedback.service.IOaFeedbackService;
import com.ruoyi.oa.feedback.support.FeedbackFileValidator;
import com.ruoyi.oa.feedback.support.FeedbackNoGenerator;
import com.ruoyi.oa.feedback.vo.FeedbackDetailVO;
import com.ruoyi.oa.feedback.vo.FeedbackFileVO;
import com.ruoyi.oa.feedback.vo.FeedbackListVO;
import com.ruoyi.oa.feedback.vo.FeedbackModuleVO;
import com.ruoyi.oa.feedback.vo.FeedbackStatsVO;
import com.ruoyi.oa.notify.service.IOaNotifyService;

/**
 * 工单 / 意见反馈 Service 实现
 *
 * <p>关键约束：匿名脱敏、撤回权限、回复事务 + 站内信去重、附件归属校验全部在服务端完成。</p>
 *
 * @author ruoyi-oa
 */
@Service
public class OaFeedbackServiceImpl implements IOaFeedbackService
{
    private static final Logger log = LoggerFactory.getLogger(OaFeedbackServiceImpl.class);

    /** 临时附件业务类型（上传后、提交前） */
    private static final String BIZ_FEEDBACK_TEMP = "feedback_temp";

    /** 正式附件业务类型（提交后绑定） */
    private static final String BIZ_FEEDBACK = "feedback";

    /** 逻辑删除：正常 */
    private static final String DEL_NORMAL = "0";

    /** 状态：待处理 */
    private static final String STATUS_PENDING = "0";

    /** 状态：已回复 */
    private static final String STATUS_REPLIED = "1";

    /** 是否匿名：是 */
    private static final String ANONYMOUS_YES = "1";

    /** 批量回复默认文案 */
    private static final String DEFAULT_BATCH_REPLY = "已批量处理，感谢反馈";

    /** 单工单附件总数上限 */
    private static final int MAX_ATTACHMENTS = 8;

    /** 附件鉴权输出地址前缀 */
    private static final String ATTACHMENT_URL_PREFIX = "/oa/feedback/attachment/";

    /** 临时附件有效期（毫秒）：2 小时 */
    private static final long TEMP_FILE_TTL_MS = 2L * 60L * 60L * 1000L;

    /** 批量操作上限 */
    private static final int MAX_BATCH = 200;

    /** 回复内容上限 */
    private static final int REPLY_MAX_LENGTH = 500;

    /** 站内信类型：工单已回复 */
    private static final String NOTIFY_TYPE_FEEDBACK_REPLY = "feedback_reply";

    /** 类型标签（与字典 oa_feedback_type 一致） */
    private static final Map<String, String> TYPE_LABELS = new HashMap<String, String>();

    /** 状态标签（与字典 oa_feedback_status 一致） */
    private static final Map<String, String> STATUS_LABELS = new HashMap<String, String>();

    static
    {
        TYPE_LABELS.put("1", "Bug");
        TYPE_LABELS.put("2", "优化建议");
        TYPE_LABELS.put("3", "新增功能");
        TYPE_LABELS.put("4", "其他");
        STATUS_LABELS.put(STATUS_PENDING, "待处理");
        STATUS_LABELS.put(STATUS_REPLIED, "已回复");
    }

    @Autowired
    private OaFeedbackMapper feedbackMapper;

    @Autowired
    private OaFileMapper fileMapper;

    @Autowired
    private IOaFileService fileService;

    @Autowired
    private IOaNotifyService notifyService;

    @Autowired
    private FeedbackNoGenerator feedbackNoGenerator;

    @Autowired
    private FeedbackFileValidator fileValidator;

    // ============================ 查询 ============================

    @Override
    public List<FeedbackListVO> selectFeedbackList(FeedbackQuery query)
    {
        SysUser me = SecurityUtils.getLoginUser().getUser();
        boolean manage = SecurityUtils.hasPermi("oa:feedback:manage");
        FeedbackQuery condition = query == null ? new FeedbackQuery() : query;
        condition.setCurrentUserId(me.getUserId());
        normalizeTimeRange(condition);
        List<OaFeedback> list = feedbackMapper.selectFeedbackList(condition);
        List<FeedbackListVO> result = new ArrayList<>(list.size());
        for (OaFeedback feedback : list)
        {
            FeedbackListVO vo = new FeedbackListVO();
            fillListVO(vo, feedback, manage, me.getUserId());
            result.add(vo);
        }
        return toVoPage(list, result);
    }

    @Override
    public FeedbackDetailVO selectFeedbackDetail(Long feedbackId)
    {
        if (feedbackId == null)
        {
            throw new ServiceException("工单不存在或已删除", 404);
        }
        OaFeedback feedback = feedbackMapper.selectFeedbackById(feedbackId);
        if (feedback == null || !DEL_NORMAL.equals(feedback.getDelFlag()))
        {
            throw new ServiceException("工单不存在或已删除", 404);
        }
        SysUser me = SecurityUtils.getLoginUser().getUser();
        boolean manage = SecurityUtils.hasPermi("oa:feedback:manage");
        FeedbackDetailVO vo = new FeedbackDetailVO();
        fillListVO(vo, feedback, manage, me.getUserId());
        vo.setContent(feedback.getContent());
        vo.setSteps(feedback.getSteps());
        vo.setExpectResult(feedback.getExpectResult());
        vo.setReplyBy(feedback.getReplyBy());
        vo.setAttachments(loadAttachments(feedbackId));
        return vo;
    }

    @Override
    public List<FeedbackListVO> selectManageList(FeedbackQuery query)
    {
        SysUser me = SecurityUtils.getLoginUser().getUser();
        FeedbackQuery condition = query == null ? new FeedbackQuery() : query;
        normalizeTimeRange(condition);
        List<OaFeedback> list = feedbackMapper.selectManageList(condition);
        List<FeedbackListVO> result = new ArrayList<>(list.size());
        for (OaFeedback feedback : list)
        {
            FeedbackListVO vo = new FeedbackListVO();
            // 管理端返回真实提交人姓名，并附带 isAnonymous 标记
            fillListVO(vo, feedback, true, me.getUserId());
            result.add(vo);
        }
        return toVoPage(list, result);
    }

    @Override
    public List<FeedbackModuleVO> selectModuleOptions()
    {
        List<FeedbackModuleVO> rows = feedbackMapper.selectMenuOptions();
        Map<Long, FeedbackModuleVO> modules = new LinkedHashMap<>();
        if (rows != null)
        {
            for (FeedbackModuleVO row : rows)
            {
                if ("M".equals(row.getMenuType()))
                {
                    modules.put(row.getId(), row);
                }
            }
            for (FeedbackModuleVO row : rows)
            {
                if ("C".equals(row.getMenuType()))
                {
                    FeedbackModuleVO parent = modules.get(row.getParentId());
                    if (parent != null)
                    {
                        parent.getChildren().add(row);
                    }
                }
            }
        }
        return new ArrayList<>(modules.values());
    }

    @Override
    public FeedbackStatsVO selectStats()
    {
        FeedbackStatsVO stats = feedbackMapper.selectStats();
        if (stats == null)
        {
            stats = new FeedbackStatsVO();
        }
        if (stats.getPendingCount() == null)
        {
            stats.setPendingCount(0L);
        }
        if (stats.getRepliedCount() == null)
        {
            stats.setRepliedCount(0L);
        }
        if (stats.getTodayCount() == null)
        {
            stats.setTodayCount(0L);
        }
        if (stats.getWeekCount() == null)
        {
            stats.setWeekCount(0L);
        }
        return stats;
    }

    // ============================ 写操作 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitFeedback(FeedbackSubmitDTO dto)
    {
        if (dto == null)
        {
            throw new ServiceException("提交内容不能为空", 400);
        }
        SysUser me = SecurityUtils.getLoginUser().getUser();
        String type = trim(dto.getType());
        if (!TYPE_LABELS.containsKey(type))
        {
            throw new ServiceException("请选择反馈类型", 400);
        }
        String title = trim(dto.getTitle());
        if (title.length() < 2 || title.length() > 50)
        {
            throw new ServiceException("标题需为 2–50 个字符", 400);
        }
        String content = trim(dto.getContent());
        if (content.length() < 10 || content.length() > 2000)
        {
            throw new ServiceException("详细描述需为 10–2000 个字符", 400);
        }
        String steps = trim(dto.getSteps());
        if (steps.length() > 2000)
        {
            throw new ServiceException("复现步骤最多 2000 字", 400);
        }
        String expectResult = trim(dto.getExpectResult());
        if (expectResult.length() > 1000)
        {
            throw new ServiceException("期望结果最多 1000 字", 400);
        }
        String isAnonymous = normalizeAnonymous(dto.getIsAnonymous());

        // 影响模块/页面：以 sys_menu 快照为准，不信任前端回传名称
        Long moduleId = null;
        String moduleName = "";
        Long pageId = null;
        String pageName = "";
        if (dto.getModuleId() != null)
        {
            FeedbackModuleVO module = feedbackMapper.selectMenuById(dto.getModuleId());
            if (module == null || !"M".equals(module.getMenuType()))
            {
                throw new ServiceException("影响模块不存在，请刷新后重试", 400);
            }
            moduleId = module.getId();
            moduleName = StringUtils.nvl(module.getName(), "");
        }
        if (dto.getPageId() != null)
        {
            if (moduleId == null)
            {
                throw new ServiceException("请先选择影响模块", 400);
            }
            FeedbackModuleVO page = feedbackMapper.selectMenuById(dto.getPageId());
            if (page == null || !"C".equals(page.getMenuType()) || !moduleId.equals(page.getParentId()))
            {
                throw new ServiceException("影响页面不属于所选模块，请重新选择", 400);
            }
            pageId = page.getId();
            pageName = StringUtils.nvl(page.getName(), "");
        }

        // 临时附件：总数兜底 ≤8；逐个校验归属/业务类型/2 小时内，不通过忽略
        List<Long> requestFileIds = dto.getFileIds();
        if (requestFileIds != null && requestFileIds.size() > MAX_ATTACHMENTS)
        {
            throw new ServiceException("附件总数不能超过 " + MAX_ATTACHMENTS + " 个", 400);
        }
        List<Long> validFileIds = validateTempFiles(requestFileIds, me.getUserId());
        assertAttachmentTypeLimit(validFileIds);

        OaFeedback feedback = new OaFeedback();
        feedback.setType(type);
        feedback.setTitle(title);
        feedback.setModuleId(moduleId);
        feedback.setModuleName(moduleName);
        feedback.setPageId(pageId);
        feedback.setPageName(pageName);
        feedback.setContent(content);
        feedback.setSteps(steps);
        feedback.setExpectResult(expectResult);
        feedback.setIsAnonymous(isAnonymous);
        feedback.setStatus(STATUS_PENDING);
        feedback.setSubmitterId(me.getUserId());
        feedback.setSubmitterName(nickName(me));
        feedback.setSubmitTime(new Date());
        insertWithRetry(feedback);

        // 绑定正式附件（feedback_temp → feedback + feedbackId）
        for (Long fileId : validFileIds)
        {
            fileService.bindBiz(fileId, BIZ_FEEDBACK, feedback.getFeedbackId());
        }
        log.info("提交工单成功 ticketNo={} submitterId={} attachments={}", feedback.getTicketNo(),
                me.getUserId(), validFileIds.size());
        return feedback.getTicketNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawFeedback(Long feedbackId)
    {
        if (feedbackId == null)
        {
            throw new ServiceException("工单ID不能为空", 400);
        }
        Long me = SecurityUtils.getUserId();
        OaFeedback feedback = feedbackMapper.selectFeedbackById(feedbackId);
        if (feedback == null || !DEL_NORMAL.equals(feedback.getDelFlag()))
        {
            throw new ServiceException("工单不存在或已删除", 404);
        }
        if (!me.equals(feedback.getSubmitterId()))
        {
            throw new ServiceException("只能撤回自己提交的工单", 403);
        }
        if (!STATUS_PENDING.equals(feedback.getStatus()))
        {
            throw new ServiceException("工单已处理，无法撤回", 409);
        }
        int rows = feedbackMapper.withdrawFeedback(feedbackId, me);
        if (rows == 0)
        {
            throw new ServiceException("工单已处理，无法撤回", 409);
        }
        cleanupSideResources(feedbackId);
        log.info("撤回工单成功 feedbackId={} ticketNo={} submitterId={}", feedbackId, feedback.getTicketNo(), me);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyFeedback(FeedbackReplyDTO dto)
    {
        if (dto == null || dto.getFeedbackId() == null)
        {
            throw new ServiceException("工单ID不能为空", 400);
        }
        OaFeedback feedback = feedbackMapper.selectFeedbackByIdForUpdate(dto.getFeedbackId());
        if (feedback == null)
        {
            throw new ServiceException("工单不存在或已删除", 404);
        }
        int status = dto.getStatus() == null ? 1 : dto.getStatus();
        if (status != 0 && status != 1)
        {
            throw new ServiceException("状态取值非法", 400);
        }
        // 改回待处理：清空回复内容/人/时间
        if (status == 0)
        {
            feedbackMapper.reopenFeedback(feedback.getFeedbackId());
            log.info("工单状态改回待处理 feedbackId={}", feedback.getFeedbackId());
            return;
        }
        String replyContent = trim(dto.getReplyContent());
        if (StringUtils.isEmpty(replyContent))
        {
            throw new ServiceException("回复内容不能为空", 400);
        }
        if (replyContent.length() > REPLY_MAX_LENGTH)
        {
            throw new ServiceException("回复内容最多 " + REPLY_MAX_LENGTH + " 字", 400);
        }
        SysUser me = SecurityUtils.getLoginUser().getUser();
        feedback.setStatus(STATUS_REPLIED);
        feedback.setReplyContent(replyContent);
        feedback.setReplyBy(me.getUserId());
        feedback.setReplyByName(nickName(me));
        feedbackMapper.replyFeedback(feedback);
        notifyReply(feedback);
        log.info("工单回复成功 feedbackId={} ticketNo={}", feedback.getFeedbackId(), feedback.getTicketNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteFeedbackByIds(Long[] feedbackIds)
    {
        if (feedbackIds == null || feedbackIds.length == 0)
        {
            throw new ServiceException("请选择要删除的工单", 400);
        }
        if (feedbackIds.length > MAX_BATCH)
        {
            throw new ServiceException("单次最多删除 " + MAX_BATCH + " 条", 400);
        }
        int rows = feedbackMapper.deleteFeedbackByIds(feedbackIds);
        if (rows == 0)
        {
            throw new ServiceException("工单不存在或已删除", 404);
        }
        for (Long feedbackId : feedbackIds)
        {
            if (feedbackId != null)
            {
                cleanupSideResources(feedbackId);
            }
        }
        log.info("逻辑删除工单 rows={} ids={}", rows, feedbackIds.length);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchReply(Long[] feedbackIds, String replyContent)
    {
        if (feedbackIds == null || feedbackIds.length == 0)
        {
            throw new ServiceException("请选择要回复的工单", 400);
        }
        Set<Long> distinct = new LinkedHashSet<>();
        for (Long feedbackId : feedbackIds)
        {
            if (feedbackId != null)
            {
                distinct.add(feedbackId);
            }
        }
        if (distinct.isEmpty())
        {
            throw new ServiceException("请选择要回复的工单", 400);
        }
        if (distinct.size() > MAX_BATCH)
        {
            throw new ServiceException("单次最多操作 " + MAX_BATCH + " 条", 400);
        }
        String content = trim(replyContent);
        if (StringUtils.isEmpty(content))
        {
            content = DEFAULT_BATCH_REPLY;
        }
        if (content.length() > REPLY_MAX_LENGTH)
        {
            throw new ServiceException("回复内容最多 " + REPLY_MAX_LENGTH + " 字", 400);
        }

        Long[] ids = distinct.toArray(new Long[0]);
        List<OaFeedback> list = feedbackMapper.selectFeedbackByIds(ids);
        if (list == null || list.size() != ids.length)
        {
            throw new ServiceException("部分工单不存在或已删除，请刷新后重试", 400);
        }
        // 批量标记只处理待处理工单，避免覆盖已发出的正式回复
        List<OaFeedback> pending = new ArrayList<>();
        for (OaFeedback feedback : list)
        {
            if (STATUS_PENDING.equals(feedback.getStatus()))
            {
                pending.add(feedback);
            }
        }
        if (pending.isEmpty())
        {
            throw new ServiceException("所选工单均已是已回复状态，无需重复处理", 400);
        }
        Long[] pendingIds = new Long[pending.size()];
        for (int i = 0; i < pending.size(); i++)
        {
            pendingIds[i] = pending.get(i).getFeedbackId();
        }
        SysUser me = SecurityUtils.getLoginUser().getUser();
        int rows = feedbackMapper.batchReply(pendingIds, content, me.getUserId(), nickName(me));
        if (rows == 0)
        {
            throw new ServiceException("工单不存在或已删除", 404);
        }
        for (OaFeedback feedback : pending)
        {
            feedback.setReplyContent(content);
            notifyReply(feedback);
        }
        log.info("批量标记已回复 rows={} selected={} ids={}", rows, ids.length, pendingIds.length);
        return rows;
    }

    @Override
    public FeedbackFileVO uploadAttachment(MultipartFile file)
    {
        Long userId = SecurityUtils.getUserId();
        String fileType = fileValidator.validate(file);
        OaFile oaFile = fileService.upload(file, BIZ_FEEDBACK_TEMP, userId);
        FeedbackFileVO vo = new FeedbackFileVO();
        vo.setFileId(oaFile.getFileId());
        vo.setFileName(oaFile.getFileName());
        vo.setOriginalName(oaFile.getOriginalName());
        vo.setUrl(attachmentUrl(oaFile.getFileId()));
        vo.setSize(oaFile.getFileSize());
        vo.setFileType(fileType);
        return vo;
    }

    @Override
    public void writeAttachment(Long fileId, boolean attachment, HttpServletResponse response)
    {
        OaFile file = fileId == null ? null : fileMapper.selectFileById(fileId);
        if (file == null || (!BIZ_FEEDBACK.equals(file.getBizType()) && !BIZ_FEEDBACK_TEMP.equals(file.getBizType())))
        {
            throw new ServiceException("附件不存在", 404);
        }
        // 临时附件仅上传人本人可预览，避免通过 fileId 枚举他人草稿附件
        if (BIZ_FEEDBACK_TEMP.equals(file.getBizType())
                && (file.getUploaderId() == null || !file.getUploaderId().equals(SecurityUtils.getUserId())))
        {
            throw new ServiceException("无权访问该附件", 403);
        }
        fileService.writeFile(fileId, attachment, response);
    }

    private String attachmentUrl(Long fileId)
    {
        return fileId == null ? "" : ATTACHMENT_URL_PREFIX + fileId;
    }

    // ============================ 内部方法 ============================

    /**
     * 列表字段装配（含匿名脱敏与可撤回标记）
     */
    private void fillListVO(FeedbackListVO vo, OaFeedback feedback, boolean manage, Long currentUserId)
    {
        vo.setFeedbackId(feedback.getFeedbackId());
        vo.setTicketNo(feedback.getTicketNo());
        vo.setType(feedback.getType());
        vo.setTypeLabel(TYPE_LABELS.get(feedback.getType()));
        vo.setTitle(feedback.getTitle());
        vo.setModuleId(feedback.getModuleId());
        vo.setModuleName(feedback.getModuleName());
        vo.setPageId(feedback.getPageId());
        vo.setPageName(feedback.getPageName());
        vo.setIsAnonymous(feedback.getIsAnonymous());
        vo.setStatus(feedback.getStatus());
        vo.setStatusLabel(STATUS_LABELS.get(feedback.getStatus()));
        vo.setSubmitTime(feedback.getSubmitTime());
        vo.setReplyContent(feedback.getReplyContent());
        vo.setReplyByName(feedback.getReplyByName());
        vo.setReplyTime(feedback.getReplyTime());
        vo.setAttachmentCount(feedback.getAttachmentCount() == null ? 0 : feedback.getAttachmentCount());
        // 匿名脱敏：非管理端一律「匿名用户」且不下发 submitterId
        boolean anonymous = ANONYMOUS_YES.equals(feedback.getIsAnonymous());
        if (anonymous && !manage)
        {
            vo.setSubmitterName("匿名用户");
            vo.setSubmitterId(null);
        }
        else
        {
            vo.setSubmitterName(feedback.getSubmitterName());
            vo.setSubmitterId(feedback.getSubmitterId());
        }
        // 个人操作标记不受脱敏影响（本人匿名工单也要能撤回）
        vo.setCanWithdraw(currentUserId != null && currentUserId.equals(feedback.getSubmitterId())
                && STATUS_PENDING.equals(feedback.getStatus()) && DEL_NORMAL.equals(feedback.getDelFlag()));
    }

    /**
     * 查询正式附件并装配 VO
     */
    private List<FeedbackFileVO> loadAttachments(Long feedbackId)
    {
        List<OaFile> files = fileService.selectByBiz(BIZ_FEEDBACK, feedbackId);
        List<FeedbackFileVO> result = new ArrayList<>();
        if (files != null)
        {
            for (OaFile file : files)
            {
                FeedbackFileVO vo = new FeedbackFileVO();
                vo.setFileId(file.getFileId());
                vo.setFileName(file.getFileName());
                vo.setOriginalName(file.getOriginalName());
                vo.setUrl(attachmentUrl(file.getFileId()));
                vo.setSize(file.getFileSize());
                vo.setFileType(fileValidator.detectFileType(StringUtils.isNotEmpty(file.getOriginalName())
                        ? file.getOriginalName() : file.getFileName()));
                result.add(vo);
            }
        }
        return result;
    }

    /**
     * 校验临时附件：归属 + feedback_temp + 2 小时内；不通过忽略并 warn
     */
    private List<Long> validateTempFiles(List<Long> fileIds, Long userId)
    {
        List<Long> valid = new ArrayList<>();
        if (fileIds == null || fileIds.isEmpty())
        {
            return valid;
        }
        long now = System.currentTimeMillis();
        for (Long fileId : new LinkedHashSet<>(fileIds))
        {
            if (fileId == null)
            {
                continue;
            }
            OaFile file = fileMapper.selectFileById(fileId);
            boolean ok = file != null
                    && userId != null && userId.equals(file.getUploaderId())
                    && BIZ_FEEDBACK_TEMP.equals(file.getBizType())
                    && file.getCreateTime() != null
                    && now - file.getCreateTime().getTime() <= TEMP_FILE_TTL_MS;
            if (!ok)
            {
                log.warn("非法工单附件 fileId={} uploaderId={} bizType={}", fileId,
                        file == null ? null : file.getUploaderId(),
                        file == null ? null : file.getBizType());
                throw new ServiceException("附件已过期或无效（单次填写超过 2 小时），请重新上传后再提交", 400);
            }
            valid.add(fileId);
        }
        return valid;
    }

    /**
     * 附件分类型数量兜底：图片 ≤5、Word ≤3、总数 ≤8（防止绕过前端限制）
     */
    private void assertAttachmentTypeLimit(List<Long> fileIds)
    {
        if (fileIds == null || fileIds.isEmpty())
        {
            return;
        }
        int images = 0;
        int words = 0;
        for (Long fileId : fileIds)
        {
            OaFile file = fileMapper.selectFileById(fileId);
            if (file == null)
            {
                continue;
            }
            String name = StringUtils.isNotEmpty(file.getOriginalName()) ? file.getOriginalName()
                    : file.getFileName();
            String extension = extensionOf(name);
            if (Arrays.asList("jpg", "jpeg", "png", "gif", "webp").contains(extension))
            {
                images++;
            }
            else if (Arrays.asList("doc", "docx").contains(extension))
            {
                words++;
            }
        }
        if (images > 5)
        {
            throw new ServiceException("图片附件不能超过 5 张", 400);
        }
        if (words > 3)
        {
            throw new ServiceException("Word 附件不能超过 3 个", 400);
        }
    }

    private String extensionOf(String fileName)
    {
        if (StringUtils.isEmpty(fileName))
        {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        return index < 0 || index == fileName.length() - 1 ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 撤回/删除后清理附件记录与站内信（物理文件由运维/后续任务异步回收）
     */
    private void cleanupSideResources(Long feedbackId)
    {
        try
        {
            fileService.deleteByBiz(BIZ_FEEDBACK, feedbackId);
        }
        catch (Exception e)
        {
            log.warn("清理工单附件记录失败 feedbackId={}：{}", feedbackId, e.getMessage());
        }
        try
        {
            notifyService.deleteByBiz(BIZ_FEEDBACK, feedbackId);
        }
        catch (Exception e)
        {
            log.warn("清理工单站内信失败 feedbackId={}：{}", feedbackId, e.getMessage());
        }
    }

    /**
     * 新增工单；ticket_no 唯一索引冲突时重试 ≤3 次
     */
    private void insertWithRetry(OaFeedback feedback)
    {
        for (int attempt = 0; attempt < 3; attempt++)
        {
            feedback.setTicketNo(feedbackNoGenerator.next());
            try
            {
                feedbackMapper.insertFeedback(feedback);
                return;
            }
            catch (DuplicateKeyException e)
            {
                log.warn("工单编号冲突，重试第 {} 次 ticketNo={}", attempt + 1, feedback.getTicketNo());
            }
        }
        throw new ServiceException("工单编号生成冲突，请稍后重试", 409);
    }

    /**
     * 回复成功写站内信（去重键保证重复回复不重复通知；通知失败不影响主事务）
     */
    private void notifyReply(OaFeedback feedback)
    {
        if (feedback == null || feedback.getSubmitterId() == null)
        {
            return;
        }
        try
        {
            String reply = StringUtils.nvl(feedback.getReplyContent(), "");
            String brief = reply.length() > 50 ? reply.substring(0, 50) : reply;
            String title = "你的工单已有回复";
            String content = "工单 " + StringUtils.nvl(feedback.getTicketNo(), "") + " 已处理：" + brief;
            notifyService.notify(feedback.getSubmitterId(), NOTIFY_TYPE_FEEDBACK_REPLY, title, content,
                    BIZ_FEEDBACK, feedback.getFeedbackId(),
                    "feedback_reply:" + feedback.getFeedbackId() + ":" + System.currentTimeMillis());
        }
        catch (Exception e)
        {
            // 站内信为旁路能力，失败不阻断回复
            log.error("工单回复站内信写入失败 feedbackId={}", feedback.getFeedbackId(), e);
        }
    }

    /**
     * 转换 VO 列表时保留 PageHelper 分页信息（否则 getDataTable 的 total 会退化为当前页大小）
     */
    @SuppressWarnings("unchecked")
    private List<FeedbackListVO> toVoPage(List<OaFeedback> source, List<FeedbackListVO> target)
    {
        if (source instanceof Page)
        {
            Page<OaFeedback> sourcePage = (Page<OaFeedback>) source;
            Page<FeedbackListVO> voPage = new Page<>(sourcePage.getPageNum(), sourcePage.getPageSize());
            voPage.setTotal(sourcePage.getTotal());
            voPage.addAll(target);
            return voPage;
        }
        return target;
    }

    /**
     * 时间范围规范化：前端可能传 yyyy-MM-dd 或完整时间，统一截取日期部分
     */
    private void normalizeTimeRange(FeedbackQuery query)
    {
        if (query == null)
        {
            return;
        }
        query.setBeginTime(normalizeDate(query.getBeginTime()));
        query.setEndTime(normalizeDate(query.getEndTime()));
    }

    private String normalizeDate(String value)
    {
        String text = trim(value);
        return text.length() > 10 ? text.substring(0, 10) : text;
    }

    private String normalizeAnonymous(String value)
    {
        String anonymous = trim(value);
        if (StringUtils.isEmpty(anonymous))
        {
            return "0";
        }
        if ("0".equals(anonymous) || "false".equalsIgnoreCase(anonymous))
        {
            return "0";
        }
        if ("1".equals(anonymous) || "true".equalsIgnoreCase(anonymous))
        {
            return "1";
        }
        throw new ServiceException("匿名标识取值非法", 400);
    }

    private String nickName(SysUser user)
    {
        if (user == null)
        {
            return "";
        }
        return StringUtils.isNotEmpty(user.getNickName()) ? user.getNickName() : user.getUserName();
    }

    private String trim(String value)
    {
        return value == null ? "" : value.trim();
    }

}
