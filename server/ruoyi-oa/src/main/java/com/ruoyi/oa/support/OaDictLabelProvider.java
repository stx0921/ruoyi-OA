package com.ruoyi.oa.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.service.ISysDictTypeService;

/**
 * OA 标签映射提供者：**字典优先，硬编码兜底**。
 *
 * <p>背景：成员/申请等 VO 里的 xxxLabel 之前直接取后端写死的 Map，
 * 在「系统管理 → 字典管理」里改了字典键值后，接口返回的仍是旧文案，
 * 前端即使拿到新字典也会被 VO 里的 label 覆盖。</p>
 *
 * <p>用法：一次列表装配只调用一次 {@link #labels} 构建映射，避免逐行读 Redis。</p>
 *
 * @author ruoyi-oa
 */
@Component
public class OaDictLabelProvider
{
    private static final Logger log = LoggerFactory.getLogger(OaDictLabelProvider.class);

    @Autowired
    private ISysDictTypeService dictTypeService;

    /**
     * 合并字典标签与兜底映射
     *
     * @param dictType 字典类型（如 oa_position）
     * @param fallback 兜底映射（可为 null），字典中的同 value 会覆盖它
     * @return value → label
     */
    public Map<String, String> labels(String dictType, Map<String, String> fallback)
    {
        Map<String, String> result = new HashMap<>();
        if (fallback != null)
        {
            result.putAll(fallback);
        }
        if (dictType == null)
        {
            return result;
        }
        try
        {
            List<SysDictData> datas = dictTypeService.selectDictDataByType(dictType);
            if (datas != null)
            {
                for (SysDictData data : datas)
                {
                    if (data != null && data.getDictValue() != null)
                    {
                        result.put(data.getDictValue(), data.getDictLabel());
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("[OA字典] 读取字典 {} 失败，使用内置兜底映射：{}", dictType, e.getMessage());
        }
        return result;
    }

    /**
     * 取单个值对应的标签（字典优先，兜底映射次之）
     */
    public String label(String dictType, String value, Map<String, String> fallback)
    {
        if (value == null || value.isEmpty())
        {
            return "";
        }
        Map<String, String> merged = labels(dictType, fallback);
        return merged.getOrDefault(value, "");
    }
}
