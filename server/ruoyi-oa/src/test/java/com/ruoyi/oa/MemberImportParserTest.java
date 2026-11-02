package com.ruoyi.oa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oa.importx.MemberImportParser;
import com.ruoyi.oa.importx.MemberImportRow;

/**
 * Excel 导入解析器单元测试（POI 内存生成工作簿，不依赖数据库）
 *
 * @author ruoyi-oa
 */
class MemberImportParserTest
{
    @Test
    void parseShouldReadTwelveColumnsAndRowNum() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook())
        {
            Sheet sheet = workbook.createSheet("成员导入");
            writeHeader(sheet);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("林晓");
            row.createCell(1).setCellValue("女");
            row.createCell(2).setCellValue("2024011001");
            row.createCell(3).setCellValue("13800003333");
            row.createCell(4).setCellValue("外国语学院");
            row.createCell(5).setCellValue("英语");
            row.createCell(6).setCellValue("2024级英语1班");
            row.createCell(7).setCellValue("大二");
            row.createCell(8).setCellValue("2026");
            row.createCell(9).setCellValue("组织部");
            row.createCell(10).setCellValue("干事");
            row.createCell(11).setCellValue("2026-09-01");
            workbook.write(out);
        }
        MemberImportParser parser = new MemberImportParser();
        List<MemberImportRow> rows = parser.parse(new ByteArrayInputStream(out.toByteArray()), 500);
        assertEquals(1, rows.size());
        MemberImportRow row = rows.get(0);
        assertEquals(2, row.getRowNum());
        assertEquals("林晓", row.getName());
        assertEquals("2024011001", row.getStudentNo());
        assertEquals("13800003333", row.getPhone());
        assertEquals("组织部", row.getDeptName());
        assertEquals("2026-09-01", row.getJoinDateText());
    }

    @Test
    void parseShouldRejectWrongTemplateHeader() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook())
        {
            Sheet sheet = workbook.createSheet("sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("姓名");
            header.createCell(1).setCellValue("性别");
            workbook.write(out);
        }
        MemberImportParser parser = new MemberImportParser();
        ServiceException exception = assertThrows(ServiceException.class,
                () -> parser.parse(new ByteArrayInputStream(out.toByteArray()), 500));
        assertEquals(Integer.valueOf(400), exception.getCode());
    }

    @Test
    void parseShouldRejectTooManyRows() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook())
        {
            Sheet sheet = workbook.createSheet("成员导入");
            writeHeader(sheet);
            for (int i = 0; i < 3; i++)
            {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue("第" + (i + 1) + "人");
                row.createCell(1).setCellValue("男");
                row.createCell(2).setCellValue("20240110" + (10 + i));
                row.createCell(3).setCellValue("1380000000" + i);
                row.createCell(4).setCellValue("计算机学院");
                row.createCell(5).setCellValue("软件工程");
                row.createCell(6).setCellValue("2024级软件工程1班");
                row.createCell(7).setCellValue("大二");
                row.createCell(9).setCellValue("组织部");
            }
            workbook.write(out);
        }
        MemberImportParser parser = new MemberImportParser();
        ServiceException exception = assertThrows(ServiceException.class,
                () -> parser.parse(new ByteArrayInputStream(out.toByteArray()), 2));
        assertEquals(Integer.valueOf(400), exception.getCode());
    }

    private void writeHeader(Sheet sheet)
    {
        Row header = sheet.createRow(0);
        for (int i = 0; i < MemberImportParser.HEADERS.length; i++)
        {
            header.createCell(i).setCellValue(MemberImportParser.HEADERS[i]);
        }
    }
}
