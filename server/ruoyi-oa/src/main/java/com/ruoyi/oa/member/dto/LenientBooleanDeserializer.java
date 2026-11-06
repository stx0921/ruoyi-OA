package com.ruoyi.oa.member.dto;

import java.io.IOException;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * 宽松布尔反序列化器。
 *
 * <p>Jackson 默认只接受 {@code true} / {@code false}，而本项目若干接口的
 * {@code updateSupport} 语义在文档与前端（单选框取值 {@code 1} / {@code 0}）中
 * 一直写成 1/0，导致 {@code POST /oa/member/import/confirm} 反序列化失败：
 * <pre>
 * Cannot deserialize value of type `java.lang.Boolean` from String "1"
 * </pre>
 *
 * <p>这里统一兼容：{@code true/false}、{@code 1/0}、{@code "true"/"false"}、
 * {@code "1"/"0"}、{@code y/n}、{@code yes/no}；空串与 {@code null} 归一为 {@code null}，
 * 由业务层按默认值处理。其它无法识别的取值仍然抛 400，避免把错数据静默当成 false。
 *
 * @author ruoyi-oa
 */
public class LenientBooleanDeserializer extends JsonDeserializer<Boolean>
{
    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException
    {
        String text = parser.getValueAsString();
        if (text == null)
        {
            return null;
        }
        text = text.trim();
        if (text.isEmpty())
        {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if ("1".equals(lower) || "true".equals(lower) || "y".equals(lower) || "yes".equals(lower)
                || "on".equals(lower))
        {
            return Boolean.TRUE;
        }
        if ("0".equals(lower) || "false".equals(lower) || "n".equals(lower) || "no".equals(lower)
                || "off".equals(lower))
        {
            return Boolean.FALSE;
        }
        return (Boolean) context.handleWeirdStringValue(Boolean.class, text,
                "重复处理策略 updateSupport 只支持 true/false 或 1/0");
    }
}
