package com.ruoyi.oa.importx;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * Excel 导入解析器（POI 4.1.2 XSSFWorkbook，流式逐行读取，500 行内存无压力）
 * <p>模板版本 v1：第 1 行 12 列表头，第 2 行示例，第 3 行起数据。</p>
 *
 * @author ruoyi-oa
 */
public class MemberImportParser
{
    /** 模板列数 */
    public static final int COLUMN_COUNT = 12;

    /** 模板版本 */
    public static final String TEMPLATE_VERSION = "v1";

    /** 表头（顺序与前端模板一致） */
    public static final String[] HEADERS = { "姓名", "性别", "学号", "手机号", "学院", "专业", "班级",
            "年级", "届次", "部门", "职务", "入会日期" };

    /**
     * 解析 Excel（校验模板版本、行数上限）
     *
     * @param inputStream 文件流
     * @param maxRows     数据行上限
     * @return 数据行（已跳过全空行）
     */
    public List<MemberImportRow> parse(InputStream inputStream, int maxRows)
    {
        if (inputStream == null)
        {
            throw new ServiceException("导入文件不能为空", 400);
        }
        try (Workbook workbook = new XSSFWorkbook(inputStream))
        {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null)
            {
                throw new ServiceException("模板版本不匹配：请下载 v1 模板（12 列）", 400);
            }
            validateHeader(sheet.getRow(0));
            List<MemberImportRow> rows = new ArrayList<>();
            int lastRowNum = sheet.getLastRowNum();
            for (int i = 1; i <= lastRowNum; i++)
            {
                Row row = sheet.getRow(i);
                if (row == null)
                {
                    continue;
                }
                MemberImportRow importRow = readRow(row, i + 1);
                if (importRow.isEmptyRow())
                {
                    continue;
                }
                rows.add(importRow);
                if (rows.size() > maxRows)
                {
                    throw new ServiceException("单次最多导入 " + maxRows + " 行，当前 " + rows.size() + " 行", 400);
                }
            }
            if (rows.isEmpty())
            {
                throw new ServiceException("文件中没有可导入的数据行", 400);
            }
            return rows;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("导入文件解析失败，请确认文件为 .xlsx 且未损坏", 400);
        }
    }

    /**
     * 校验表头（12 列且顺序与 v1 模板一致）
     */
    private void validateHeader(Row header)
    {
        if (header == null || header.getLastCellNum() < COLUMN_COUNT)
        {
            throw new ServiceException("模板版本不匹配：请下载 v1 模板（12 列）", 400);
        }
        for (int i = 0; i < COLUMN_COUNT; i++)
        {
            String value = cellText(header, i);
            if (!HEADERS[i].equals(value))
            {
                throw new ServiceException("模板版本不匹配：第 " + (i + 1) + " 列应为「" + HEADERS[i]
                        + "」，请下载 v1 模板（12 列）", 400);
            }
        }
    }

    private MemberImportRow readRow(Row row, int rowNum)
    {
        MemberImportRow importRow = new MemberImportRow();
        importRow.setRowNum(rowNum);
        importRow.setName(cellText(row, 0));
        importRow.setGender(cellText(row, 1));
        importRow.setStudentNo(cellText(row, 2));
        importRow.setPhone(cellText(row, 3));
        importRow.setCollege(cellText(row, 4));
        importRow.setMajor(cellText(row, 5));
        importRow.setClassName(cellText(row, 6));
        importRow.setGrade(cellText(row, 7));
        importRow.setCohort(cellText(row, 8));
        importRow.setDeptName(cellText(row, 9));
        importRow.setPosition(cellText(row, 10));
        importRow.setJoinDateText(cellText(row, 11));
        return importRow;
    }

    /**
     * 读取单元格文本（日期单元格转 yyyy-MM-dd，数字单元格按整数展示）
     */
    private String cellText(Row row, int index)
    {
        org.apache.poi.ss.usermodel.Cell cell = row.getCell(index);
        if (cell == null)
        {
            return "";
        }
        try
        {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
            {
                return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
            }
            if (cell.getCellType() == CellType.NUMERIC)
            {
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value) && !Double.isInfinite(value))
                {
                    return String.valueOf((long) value);
                }
            }
            return StringUtils.trim(new org.apache.poi.ss.usermodel.DataFormatter().formatCellValue(cell));
        }
        catch (Exception e)
        {
            return StringUtils.trim(cell.toString());
        }
    }

}
