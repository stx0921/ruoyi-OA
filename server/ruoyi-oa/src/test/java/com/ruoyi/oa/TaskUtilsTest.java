package com.ruoyi.oa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import com.ruoyi.oa.recur.support.RecurDateCalculator;
import com.ruoyi.oa.task.support.OaTaskStateMachine;

/**
 * 任务协作模块纯函数单元测试（不依赖数据库、Redis、Spring 容器）
 *
 * @author ruoyi-oa
 */
class TaskUtilsTest
{
    @Test
    void weekDaysShouldNormalizeAndParse()
    {
        assertEquals("1,3,5", RecurDateCalculator.joinWeekDays(Arrays.asList(5, 1, 3, 3)));
        assertEquals("", RecurDateCalculator.joinWeekDays(Arrays.asList(0, 8, null)));
        assertEquals(Arrays.asList(1, 3, 5), RecurDateCalculator.weekDayList("5,1,3,1"));
        assertEquals(Arrays.asList("周一", "周三", "周五"), RecurDateCalculator.weekDayLabels("5,1,3"));
        assertTrue(RecurDateCalculator.hit("1,3,5", LocalDate.of(2026, 11, 2)));
        assertFalse(RecurDateCalculator.hit("1,3,5", LocalDate.of(2026, 11, 3)));
    }

    @Test
    void dueTimeShouldNormalize()
    {
        assertEquals("18:00:00", RecurDateCalculator.normalizeTime("18:00"));
        assertEquals("09:05:30", RecurDateCalculator.normalizeTime("9:05:30"));
        assertNull(RecurDateCalculator.normalizeTime("25:00"));
        assertNull(RecurDateCalculator.normalizeTime("abc"));
        assertNull(RecurDateCalculator.normalizeTime(null));
    }

    @Test
    void nextDateShouldStrictlyAfterToday()
    {
        // 2026-11-02 为周一；每周一三五 → 下一个命中日为周三 11-04（不含今天）
        assertEquals(LocalDate.of(2026, 11, 4),
                RecurDateCalculator.calcNextDate(null, "1,3,5", LocalDate.of(2026, 11, 2)));
        // 生效日晚于明天时从生效日开始算
        assertEquals(LocalDate.of(2026, 11, 11),
                RecurDateCalculator.calcNextDate(RecurDateCalculator.toDate(LocalDate.of(2026, 11, 10)), "3",
                        LocalDate.of(2026, 11, 2)));
        assertNull(RecurDateCalculator.calcNextDate(null, "", LocalDate.of(2026, 11, 2)));
    }

    @Test
    void titleTemplateShouldRenderAndValidate()
    {
        LocalDate date = LocalDate.of(2026, 11, 4);
        assertEquals("11-04 卫生检查", RecurDateCalculator.renderTitle("{MM-DD} 卫生检查", date));
        assertEquals("2026-11-04", RecurDateCalculator.renderTitle("{yyyy}-{MM}-{dd}", date));
        assertEquals("{date} 周报", RecurDateCalculator.normalizeTitleTemplate("{MM-DD} 周报"));
        assertNull(RecurDateCalculator.invalidPlaceholder("{date} 周报"));
        assertEquals("{week}", RecurDateCalculator.invalidPlaceholder("{week} 例会"));
        assertNull(RecurDateCalculator.invalidPlaceholder("无占位符"));
    }

    @Test
    void taskStateMachineShouldJudgeLegalActions()
    {
        assertTrue(OaTaskStateMachine.isFinished("3"));
        assertTrue(OaTaskStateMachine.isFinished("4"));
        assertFalse(OaTaskStateMachine.isFinished("0"));

        assertTrue(OaTaskStateMachine.isEditable("0"));
        assertTrue(OaTaskStateMachine.isEditable("1"));
        assertFalse(OaTaskStateMachine.isEditable("2"));
        assertFalse(OaTaskStateMachine.isEditable("3"));

        assertTrue(OaTaskStateMachine.isCancelable("0"));
        assertTrue(OaTaskStateMachine.isCancelable("1"));
        assertFalse(OaTaskStateMachine.isCancelable("2"));

        assertTrue(OaTaskStateMachine.isReassignable("0"));
        assertTrue(OaTaskStateMachine.isReassignable("1"));
        assertTrue(OaTaskStateMachine.isReassignable("2"));
        assertFalse(OaTaskStateMachine.isReassignable("3"));

        assertTrue(OaTaskStateMachine.isAuditable("2"));
        assertFalse(OaTaskStateMachine.isAuditable("1"));
    }
}
