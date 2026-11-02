package com.ruoyi.oa.importx;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oa.member.vo.ImportRowErrorVO;
import com.ruoyi.oa.member.vo.ImportRowResultVO;

/**
 * Excel 模板 / 错误明细写出器（POI 4.1.2，禁止 EasyExcel）
 *
 * @author ruoyi-oa
 */
public class MemberTemplateWriter
{
    /** 错误明细表头 */
    private static final String[] ERROR_HEADERS = { "行号", "姓名", "学号", "列名", "原值", "错误原因", "修正建议" };

    /** 填写说明：列名 / 是否必填 / 格式与字典 / 说明 */
    private static final String[][] INSTRUCTIONS = {
            { "姓名", "是", "2–20 个中文字符或英文", "真实姓名，不含数字" },
            { "性别", "是", "男 / 女", "也接受 0（男）/ 1（女）" },
            { "学号", "是", "8–12 位数字", "全局唯一，重复学号按“更新/跳过”策略处理" },
            { "手机号", "是", "11 位数字，1[3-9] 开头", "同手机号不允许关联两个不同学号（R14）" },
            { "学院", "是", "≤100 字", "如：计算机学院" },
            { "专业", "是", "≤100 字", "如：软件工程" },
            { "班级", "是", "≤50 字", "如：2024级软件工程2班" },
            { "年级", "是", "字典 oa_grade：大一/大二/大三/大四/研一/研二/研三", "填写字典标签" },
            { "届次", "否", "字典 oa_cohort，如 2026届", "留空默认当年" },
            { "部门", "是", "组织根节点下的部门名称", "如：组织部、宣传部" },
            { "职务", "否", "字典 oa_position：会长/副会长/部长/副部长/干事", "留空默认「干事」" },
            { "入会日期", "否", "yyyy-MM-dd，且不晚于今天", "留空默认当天" } };

    /**
     * 生成导入模板（成员导入 sheet + 填写说明 sheet）
     */
    public byte[] writeTemplate()
    {
        try (XSSFWorkbook workbook = new XSSFWorkbook())
        {
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle exampleStyle = workbook.createCellStyle();
            exampleStyle.setFont(font(workbook, false, IndexedColors.GREY_50_PERCENT.getIndex()));

            Sheet sheet = workbook.createSheet("成员导入");
            Row header = sheet.createRow(0);
            for (int i = 0; i < MemberImportParser.HEADERS.length; i++)
            {
                Cell cell = header.createCell(i);
                cell.setCellValue(MemberImportParser.HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }
            Row example = sheet.createRow(1);
            String[] exampleValues = { "张三", "男", "2024011001", "13800001111", "计算机学院", "软件工程",
                    "2024级软件工程1班", "大二", "2026届", "组织部", "干事", "2026-09-01" };
            for (int i = 0; i < exampleValues.length; i++)
            {
                Cell cell = example.createCell(i);
                cell.setCellValue(exampleValues[i]);
                cell.setCellStyle(exampleStyle);
            }
            sheet.createFreezePane(0, 1);

            Sheet instructionSheet = workbook.createSheet("填写说明");
            Row instructionHeader = instructionSheet.createRow(0);
            String[] instructionTitles = { "列名", "是否必填", "格式与字典", "说明" };
            for (int i = 0; i < instructionTitles.length; i++)
            {
                Cell cell = instructionHeader.createCell(i);
                cell.setCellValue(instructionTitles[i]);
                cell.setCellStyle(headerStyle);
                instructionSheet.setColumnWidth(i, (i == 2 ? 45 : i == 3 ? 45 : 16) * 256);
            }
            for (int i = 0; i < INSTRUCTIONS.length; i++)
            {
                Row row = instructionSheet.createRow(i + 1);
                for (int j = 0; j < INSTRUCTIONS[i].length; j++)
                {
                    row.createCell(j).setCellValue(INSTRUCTIONS[i][j]);
                }
            }
            int noteIndex = INSTRUCTIONS.length + 1;
            String[] notes = {
                    "【导入规则】",
                    "1. 仅支持 .xlsx 文件，单个文件 ≤10MB，单次最多 500 行数据。",
                    "2. 第 1 行为表头，第 2 行为示例，请从第 3 行开始填写数据；示例行可直接删除。",
                    "3. 部门必须填写「组织根节点」下已启用的部门名称；部长仅可导入本部门。",
                    "4. 重复策略：更新已有档案（默认）——学号命中且手机号一致时更新院系/班级/年级/部门/职务/入会日期；跳过重复行——学号命中时跳过。",
                    "5. 学号命中但手机号不一致、或手机号已关联其他学号时，按 R14 报错，请人工核对。",
                    "6. 校验不通过的行会生成「错误明细」xlsx，修正后重新上传即可；确认导入时单行失败不会回滚其他行。" };
            for (int i = 0; i < notes.length; i++)
            {
                Row row = instructionSheet.createRow(noteIndex++);
                row.createCell(0).setCellValue(notes[i]);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
        catch (Exception e)
        {
            throw new ServiceException("导入模板生成失败，请稍后再试", 500);
        }
    }

    /**
     * 生成错误明细 xlsx（行号 / 姓名 / 学号 / 列名 / 原值 / 错误原因 / 修正建议）
     *
     * @param rows 行级校验结果
     */
    public byte[] writeErrors(List<ImportRowResultVO> rows)
    {
        try (XSSFWorkbook workbook = new XSSFWorkbook())
        {
            Sheet sheet = workbook.createSheet("错误明细");
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle redStyle = workbook.createCellStyle();
            redStyle.setFont(font(workbook, false, IndexedColors.RED.getIndex()));
            redStyle.setBorderBottom(BorderStyle.THIN);

            Row header = sheet.createRow(0);
            for (int i = 0; i < ERROR_HEADERS.length; i++)
            {
                Cell cell = header.createCell(i);
                cell.setCellValue(ERROR_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, (i == 5 || i == 6 ? 40 : 16) * 256);
            }
            int rowIndex = 1;
            if (rows != null)
            {
                for (ImportRowResultVO row : rows)
                {
                    if (row.getErrors() == null || row.getErrors().isEmpty())
                    {
                        continue;
                    }
                    for (ImportRowErrorVO error : row.getErrors())
                    {
                        Row errorRow = sheet.createRow(rowIndex++);
                        Cell rowNumCell = errorRow.createCell(0);
                        rowNumCell.setCellValue(row.getRowNum() == null ? 0 : row.getRowNum());
                        rowNumCell.setCellStyle(redStyle);
                        errorRow.createCell(1).setCellValue(nullSafe(row.getName()));
                        errorRow.createCell(2).setCellValue(nullSafe(row.getStudentNo()));
                        errorRow.createCell(3).setCellValue(nullSafe(error.getColumn()));
                        errorRow.createCell(4).setCellValue(nullSafe(error.getValue()));
                        Cell msgCell = errorRow.createCell(5);
                        msgCell.setCellValue(nullSafe(error.getMsg()));
                        msgCell.setCellStyle(redStyle);
                        errorRow.createCell(6).setCellValue(nullSafe(error.getSuggestion()));
                    }
                }
            }
            if (rowIndex == 1)
            {
                sheet.createRow(1).createCell(0).setCellValue("无错误行");
            }
            sheet.createFreezePane(0, 1);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
        catch (Exception e)
        {
            throw new ServiceException("错误明细生成失败，请稍后再试", 500);
        }
    }

    private CellStyle headerStyle(XSSFWorkbook workbook)
    {
        CellStyle style = workbook.createCellStyle();
        style.setFont(font(workbook, true, IndexedColors.BLACK.getIndex()));
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private Font font(XSSFWorkbook workbook, boolean bold, short color)
    {
        Font font = workbook.createFont();
        font.setBold(bold);
        font.setColor(color);
        font.setFontHeightInPoints((short) 11);
        return font;
    }

    private String nullSafe(String value)
    {
        return value == null ? "" : value;
    }

}
