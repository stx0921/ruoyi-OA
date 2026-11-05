package com.ruoyi.oa.notify.controller;

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.oa.notify.dto.NotifyQuery;
import com.ruoyi.oa.notify.dto.NotifyReadDTO;
import com.ruoyi.oa.notify.service.IOaNotifyService;
import com.ruoyi.oa.notify.vo.NotifyTable;
import com.ruoyi.oa.notify.vo.NotifyVO;

/**
 * 站内信 Controller（登录即可，只能操作本人站内信）
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/notify")
public class OaNotifyController extends BaseController
{
    @Autowired
    private IOaNotifyService notifyService;

    /**
     * 站内信列表（含当前登录人未读数）
     */
    @GetMapping("/list")
    public NotifyTable list(NotifyQuery query)
    {
        Long userId = SecurityUtils.getUserId();
        NotifyQuery condition = query == null ? new NotifyQuery() : query;
        // 强制只能查询本人站内信，忽略前端传入的 userId
        condition.setUserId(userId);
        startPage();
        List<NotifyVO> list = notifyService.selectNotifyList(condition);
        TableDataInfo data = getDataTable(list);
        NotifyTable table = new NotifyTable();
        table.setCode(data.getCode());
        table.setMsg(data.getMsg());
        table.setRows(data.getRows());
        table.setTotal(data.getTotal());
        table.setUnreadCount(notifyService.countUnread(userId));
        return table;
    }

    /**
     * 标记已读 / 全部已读，返回剩余未读数
     */
    @PutMapping("/read")
    public AjaxResult read(@RequestBody NotifyReadDTO dto)
    {
        Long userId = SecurityUtils.getUserId();
        if (dto == null)
        {
            throw new ServiceException("请选择要标记的消息", 400);
        }
        if (Boolean.TRUE.equals(dto.getAll()))
        {
            notifyService.markAllRead(userId);
        }
        else
        {
            if (dto.getIds() == null || dto.getIds().isEmpty())
            {
                throw new ServiceException("请选择要标记的消息", 400);
            }
            notifyService.markRead(userId, dto.getIds());
        }
        return success(Collections.singletonMap("unreadCount", notifyService.countUnread(userId)));
    }
}
