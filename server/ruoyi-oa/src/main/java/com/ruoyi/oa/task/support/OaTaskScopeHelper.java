package com.ruoyi.oa.task.support;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.ruoyi.oa.task.dto.TaskQuery;

/**
 * 任务查询参数装配（@DataScope 的 ${params.dataScope} 由切面注入，这里补 me 与时间范围）
 *
 * @author ruoyi-oa
 */
public final class OaTaskScopeHelper
{
    private OaTaskScopeHelper()
    {
    }

    /** 参数 key：当前登录人（与 03 文档 §5.5 的 XML 一致） */
    public static final String PARAM_ME = "me";

    /** 参数 key：截止时间起 */
    public static final String PARAM_BEGIN_DUE = "beginDue";

    /** 参数 key：截止时间止 */
    public static final String PARAM_END_DUE = "endDue";

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 装配当前登录人与时间范围（必须在 mapper 调用前执行）
     */
    public static void fill(TaskQuery query, Long userId)
    {
        if (query == null)
        {
            return;
        }
        query.getParams().put(PARAM_ME, userId);
        // 兼容 TaskQuery 上的强类型时间字段与前端 params[beginDue]/params[endDue] 两种传参
        if (query.getParams().get(PARAM_BEGIN_DUE) == null && query.getBeginDueTime() != null)
        {
            query.getParams().put(PARAM_BEGIN_DUE, format(query.getBeginDueTime()));
        }
        if (query.getParams().get(PARAM_END_DUE) == null && query.getEndDueTime() != null)
        {
            query.getParams().put(PARAM_END_DUE, format(query.getEndDueTime()));
        }
    }

    private static String format(Date date)
    {
        return new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }
}
