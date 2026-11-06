package com.ruoyi.oa.volunteer.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;

/**
 * 统计导出 VO（按月明细 + 合计行，ExcelUtil isStatistics 自动生成合计）
 *
 * @author ruoyi-oa
 */
public class StatExportVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 月份（yyyy-MM） */
    @Excel(name = "月份", width = 14)
    private String month;

    /** 活动数 */
    @Excel(name = "活动数", cellType = ColumnType.NUMERIC, isStatistics = true)
    private Long activityCount;

    /** 通过人次 */
    @Excel(name = "通过人次", cellType = ColumnType.NUMERIC, isStatistics = true)
    private Long passCount;

    /** 总时长（人时） */
    @Excel(name = "总时长(人时)", cellType = ColumnType.NUMERIC, isStatistics = true)
    private BigDecimal totalHours;

    public String getMonth()
    {
        return month;
    }

    public void setMonth(String month)
    {
        this.month = month;
    }

    public Long getActivityCount()
    {
        return activityCount;
    }

    public void setActivityCount(Long activityCount)
    {
        this.activityCount = activityCount;
    }

    public Long getPassCount()
    {
        return passCount;
    }

    public void setPassCount(Long passCount)
    {
        this.passCount = passCount;
    }

    public BigDecimal getTotalHours()
    {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours)
    {
        this.totalHours = totalHours;
    }

}
