package com.ruoyi.oa.member.vo;

import java.io.Serializable;

/**
 * 导入行级错误明细 VO（列名 / 原值 / 原因 / 建议）
 *
 * @author ruoyi-oa
 */
public class ImportRowErrorVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 出错列 */
    private String column;

    /** 错误原因 */
    private String msg;

    /** 原始值 */
    private String value;

    /** 修正建议 */
    private String suggestion;

    public ImportRowErrorVO()
    {
    }

    public ImportRowErrorVO(String column, String msg, String value, String suggestion)
    {
        this.column = column;
        this.msg = msg;
        this.value = value;
        this.suggestion = suggestion;
    }

    public String getColumn()
    {
        return column;
    }

    public void setColumn(String column)
    {
        this.column = column;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getSuggestion()
    {
        return suggestion;
    }

    public void setSuggestion(String suggestion)
    {
        this.suggestion = suggestion;
    }

}
