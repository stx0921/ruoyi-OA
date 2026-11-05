package com.ruoyi.oa.recur.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 周期规则日期/标题工具（纯静态方法，便于单元测试）
 *
 * <p>时间口径统一使用 Asia/Shanghai + {@link LocalDate#getDayOfWeek()}（ISO 1=周一…7=周日），
 * 与 week_days 存储规范（逗号分隔 1–7、去重升序）对齐，避免依赖 locale/时区。</p>
 *
 * @author ruoyi-oa
 */
public final class RecurDateCalculator
{
    /** 业务时区 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** 星期文案（下标 1–7，与 week_days 对齐） */
    private static final String[] WEEK_LABELS = { "", "周一", "周二", "周三", "周四", "周五", "周六", "周日" };

    /** next_date 计算最大搜索天数（防止脏数据死循环） */
    private static final int MAX_SEARCH_DAYS = 400;

    private RecurDateCalculator()
    {
    }

    /**
     * 当前业务日期
     */
    public static LocalDate today()
    {
        return LocalDate.now(ZONE);
    }

    /**
     * Date → LocalDate（按业务时区）
     */
    public static LocalDate toLocalDate(Date date)
    {
        if (date == null)
        {
            return null;
        }
        return Instant.ofEpochMilli(date.getTime()).atZone(ZONE).toLocalDate();
    }

    /**
     * LocalDate → Date（当日 00:00:00）
     */
    public static Date toDate(LocalDate date)
    {
        if (date == null)
        {
            return null;
        }
        return Date.from(date.atStartOfDay(ZONE).toInstant());
    }

    /**
     * 解析截止时刻（支持 H:mm、HH:mm、HH:mm:ss），非法返回 null
     */
    public static LocalTime parseTime(String dueTime)
    {
        if (dueTime == null)
        {
            return null;
        }
        String value = dueTime.trim();
        String[] parts = value.split(":");
        if (parts.length != 2 && parts.length != 3)
        {
            return null;
        }
        try
        {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            int second = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59)
            {
                return null;
            }
            return LocalTime.of(hour, minute, second);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    /**
     * 归一化截止时刻为 HH:mm:ss，非法返回 null
     */
    public static String normalizeTime(String dueTime)
    {
        LocalTime time = parseTime(dueTime);
        if (time == null)
        {
            return null;
        }
        return String.format("%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
    }

    /**
     * 日期 + 截止时刻 → Date（生成任务 due_time 用）
     */
    public static Date toDateTime(LocalDate date, String dueTime)
    {
        LocalTime time = parseTime(dueTime);
        if (date == null || time == null)
        {
            return null;
        }
        return Date.from(date.atTime(time).atZone(ZONE).toInstant());
    }

    /**
     * 解析 week_days 为有序集合（非法值忽略）
     */
    public static Set<Integer> parseWeekDays(String weekDays)
    {
        Set<Integer> result = new LinkedHashSet<Integer>();
        if (weekDays == null || weekDays.trim().isEmpty())
        {
            return result;
        }
        String[] parts = weekDays.split(",");
        for (String part : parts)
        {
            if (part == null || part.trim().isEmpty())
            {
                continue;
            }
            try
            {
                int value = Integer.parseInt(part.trim());
                if (value >= 1 && value <= 7)
                {
                    result.add(value);
                }
            }
            catch (NumberFormatException e)
            {
                // 非法值忽略，由保存校验拦截
            }
        }
        return result;
    }

    /**
     * week_days → 升序 List（列表展示用）
     */
    public static List<Integer> weekDayList(String weekDays)
    {
        List<Integer> sorted = new ArrayList<Integer>(parseWeekDays(weekDays));
        java.util.Collections.sort(sorted);
        return sorted;
    }

    /**
     * week_days → 徽标文案（周一/周三/周五，按星期升序）
     */
    public static List<String> weekDayLabels(String weekDays)
    {
        List<String> labels = new ArrayList<String>();
        for (Integer day : weekDayList(weekDays))
        {
            labels.add(WEEK_LABELS[day]);
        }
        return labels;
    }

    /**
     * 星期集合 → 存储串（去重升序、无空格），如 "1,3,5"
     */
    public static String joinWeekDays(Iterable<Integer> weekDays)
    {
        Set<Integer> set = new LinkedHashSet<Integer>();
        if (weekDays != null)
        {
            for (Integer day : weekDays)
            {
                if (day != null && day >= 1 && day <= 7)
                {
                    set.add(day);
                }
            }
        }
        List<Integer> sorted = new ArrayList<Integer>(set);
        java.util.Collections.sort(sorted);
        StringBuilder sb = new StringBuilder();
        for (Integer day : sorted)
        {
            if (sb.length() > 0)
            {
                sb.append(',');
            }
            sb.append(day);
        }
        return sb.toString();
    }

    /**
     * 某日期是否命中 week_days
     */
    public static boolean hit(String weekDays, LocalDate date)
    {
        if (date == null)
        {
            return false;
        }
        return parseWeekDays(weekDays).contains(date.getDayOfWeek().getValue());
    }

    /**
     * 计算下次生成日：取「今天之后（不含今天）」且不早于生效日的第一个命中日
     *
     * <p>与 03 文档示例口径一致：2026-11-02（周一）新建 week_days=1,3,5 → next_date=2026-11-04。</p>
     *
     * @param startDate 开始生效日（可空，空则从明天算）
     * @param weekDays  每周几存储串
     * @param today     业务今天
     * @return 下次生成日，week_days 非法时返回 null
     */
    public static LocalDate calcNextDate(Date startDate, String weekDays, LocalDate today)
    {
        Set<Integer> days = parseWeekDays(weekDays);
        if (days.isEmpty())
        {
            return null;
        }
        LocalDate base = today == null ? today() : today;
        LocalDate from = base.plusDays(1);
        if (startDate != null)
        {
            LocalDate start = toLocalDate(startDate);
            if (start != null && start.isAfter(from))
            {
                from = start;
            }
        }
        for (int i = 0; i < MAX_SEARCH_DAYS; i++)
        {
            LocalDate candidate = from.plusDays(i);
            if (days.contains(candidate.getDayOfWeek().getValue()))
            {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 规则标题占位渲染：{date}→MM-dd、{yyyy}→yyyy、{MM}→MM、{dd}→dd
     *
     * <p>兼容历史/演示数据里的 {MM-DD} 写法（等价 {date}），避免种子规则生成的任务标题残留占位符。</p>
     */
    public static String renderTitle(String template, LocalDate date)
    {
        if (template == null)
        {
            return "";
        }
        LocalDate value = date == null ? today() : date;
        String result = template;
        result = result.replace("{MM-DD}", String.format("%02d-%02d", value.getMonthValue(), value.getDayOfMonth()));
        result = result.replace("{date}", String.format("%02d-%02d", value.getMonthValue(), value.getDayOfMonth()));
        result = result.replace("{yyyy}", String.format("%04d", value.getYear()));
        result = result.replace("{MM}", String.format("%02d", value.getMonthValue()));
        result = result.replace("{dd}", String.format("%02d", value.getDayOfMonth()));
        return result;
    }

    /**
     * 标题模板归一化：把历史/演示数据的 {MM-DD} 等价写法归一为规范占位 {date}，保证库内只存规范占位
     */
    public static String normalizeTitleTemplate(String title)
    {
        if (title == null)
        {
            return null;
        }
        return title.replace("{MM-DD}", "{date}");
    }

    /**
     * 校验标题占位符：仅允许 {date}/{yyyy}/{MM}/{dd}
     *
     * @return 第一个非法占位（如 {week}）；全部合法返回 null
     */
    public static String invalidPlaceholder(String title)
    {
        if (title == null)
        {
            return null;
        }
        int from = 0;
        while (true)
        {
            int start = title.indexOf('{', from);
            if (start < 0)
            {
                return null;
            }
            int end = title.indexOf('}', start);
            if (end < 0)
            {
                return title.substring(start);
            }
            String token = title.substring(start, end + 1);
            if (!"{date}".equals(token) && !"{yyyy}".equals(token) && !"{MM}".equals(token)
                    && !"{dd}".equals(token))
            {
                return token;
            }
            from = end + 1;
        }
    }
}
