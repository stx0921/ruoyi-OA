package com.ruoyi.oa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.ruoyi.oa.volunteer.support.VolunteerShowStatus;

/**
 * 志愿活动展示状态纯函数单元测试（不依赖数据库/Spring 容器）
 *
 * @author ruoyi-oa
 */
class VolunteerShowStatusTest
{
    private static Date at(String text)
    {
        try
        {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(text);
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void canceledShouldWin()
    {
        assertEquals(VolunteerShowStatus.CANCELED, VolunteerShowStatus.resolve("1", at("2026-11-10 09:00:00"),
                at("2026-11-10 12:00:00"), at("2026-11-09 18:00:00"), at("2026-11-06 10:00:00")));
    }

    @Test
    void shouldResolveByTime()
    {
        Date start = at("2026-11-10 09:00:00");
        Date end = at("2026-11-10 12:00:00");
        Date deadline = at("2026-11-09 18:00:00");

        assertEquals(VolunteerShowStatus.SIGNING_UP,
                VolunteerShowStatus.resolve("0", start, end, deadline, at("2026-11-06 10:00:00")));
        assertEquals(VolunteerShowStatus.DEADLINE_PASSED,
                VolunteerShowStatus.resolve("0", start, end, deadline, at("2026-11-09 20:00:00")));
        assertEquals(VolunteerShowStatus.ONGOING,
                VolunteerShowStatus.resolve("0", start, end, deadline, at("2026-11-10 10:00:00")));
        assertEquals(VolunteerShowStatus.FINISHED,
                VolunteerShowStatus.resolve("0", start, end, deadline, at("2026-11-10 12:00:01")));
    }

    @Test
    void canSignupShouldFollowDeadlineAndEnd()
    {
        Date start = at("2026-11-10 09:00:00");
        Date end = at("2026-11-10 12:00:00");
        Date deadline = at("2026-11-09 18:00:00");

        assertTrue(VolunteerShowStatus.canSignup("0", end, deadline, at("2026-11-06 10:00:00")));
        assertFalse(VolunteerShowStatus.canSignup("0", end, deadline, at("2026-11-09 18:00:01")));
        assertFalse(VolunteerShowStatus.canSignup("0", end, deadline, at("2026-11-10 09:00:00")));
        assertFalse(VolunteerShowStatus.canSignup("1", end, deadline, at("2026-11-06 10:00:00")));
    }

    @Test
    void closedReasonShouldBeSpecific()
    {
        Date end = at("2026-11-10 12:00:00");
        Date deadline = at("2026-11-09 18:00:00");

        assertTrue(VolunteerShowStatus.closedReason("1", end, deadline, at("2026-11-06 10:00:00")).contains("取消"));
        assertTrue(VolunteerShowStatus.closedReason("0", end, deadline, at("2026-11-10 13:00:00")).contains("结束"));
        assertTrue(VolunteerShowStatus.closedReason("0", end, deadline, at("2026-11-09 20:00:00")).contains("截止"));
        assertEquals("", VolunteerShowStatus.closedReason("0", end, deadline, at("2026-11-06 10:00:00")));
    }
}
