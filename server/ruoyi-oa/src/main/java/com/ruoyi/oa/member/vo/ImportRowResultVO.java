package com.ruoyi.oa.member.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入行级结果 VO
 *
 * @author ruoyi-oa
 */
public class ImportRowResultVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** Excel 物理行号（含表头） */
    private Integer rowNum;

    /** 学号 */
    private String studentNo;

    /** 姓名 */
    private String name;

    /** 行结果：NEW / UPDATE / SKIP / ERROR */
    private String result;

    /** 行级错误 */
    private List<ImportRowErrorVO> errors = new ArrayList<>();

    public Integer getRowNum()
    {
        return rowNum;
    }

    public void setRowNum(Integer rowNum)
    {
        this.rowNum = rowNum;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    public List<ImportRowErrorVO> getErrors()
    {
        return errors;
    }

    public void setErrors(List<ImportRowErrorVO> errors)
    {
        this.errors = errors;
    }

}
