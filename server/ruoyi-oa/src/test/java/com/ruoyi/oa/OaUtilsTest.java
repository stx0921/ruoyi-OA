package com.ruoyi.oa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import com.ruoyi.oa.support.MemberAccountService;
import com.ruoyi.oa.support.OaFileUrlHelper;
import com.ruoyi.oa.support.OaMemberVoAssembler;
import com.ruoyi.oa.support.OaSensitiveStrategy;
import com.ruoyi.oa.support.RoleSyncService;

/**
 * 人员录入模块纯函数单元测试（不依赖数据库与 Spring 容器）
 *
 * @author ruoyi-oa
 */
class OaUtilsTest
{
    @Test
    void fileUrlShouldRoundTrip()
    {
        assertEquals("/oa/file/901", OaFileUrlHelper.url(901L));
        assertEquals("", OaFileUrlHelper.url(null));
        assertEquals(Long.valueOf(901L), OaFileUrlHelper.parseFileId("/oa/file/901"));
        assertNull(OaFileUrlHelper.parseFileId("/profile/upload/a.jpg"));
        assertNull(OaFileUrlHelper.parseFileId(null));
    }

    @Test
    void initialPasswordShouldBeLastSixDigits()
    {
        assertEquals("005678", MemberAccountService.initialPassword("13700005678"));
        assertEquals("123456", MemberAccountService.initialPassword("123"));
        assertEquals("123456", MemberAccountService.initialPassword(null));
    }

    @Test
    void roleKeyShouldFollowPosition()
    {
        assertEquals("oa_president", RoleSyncService.roleKeyOf("1"));
        assertEquals("oa_president", RoleSyncService.roleKeyOf("2"));
        assertEquals("oa_minister", RoleSyncService.roleKeyOf("3"));
        assertEquals("oa_minister", RoleSyncService.roleKeyOf("4"));
        assertEquals("oa_staff", RoleSyncService.roleKeyOf("5"));
        assertEquals("oa_staff", RoleSyncService.roleKeyOf(null));
    }

    @Test
    void diffSummaryShouldDescribeChangedFields()
    {
        OaMemberVoAssembler assembler = new OaMemberVoAssembler();
        assertEquals("手机号 13800003333 → 13900001234",
                assembler.summary("{\"phone\":\"13800003333\"}", "{\"phone\":\"13900001234\"}"));
        assertEquals("部门 103 → 104", assembler.summary("{\"deptId\":103}", "{\"deptId\":104}"));
        assertEquals("", assembler.summary("{\"name\":\"林晓\"}", "{\"name\":\"林晓\"}"));
        assertEquals("", assembler.summary(null, null));
    }

    @Test
    void sensitiveMaskShouldFollowR12()
    {
        OaSensitiveStrategy strategy = new OaSensitiveStrategy();
        assertEquals("138****5678", strategy.phone("13812345678"));
        assertEquals("z***@x.com", strategy.email("zhang@x.com"));
        assertEquals("zh***wx", strategy.wechat("zhangsan_wx"));
        assertEquals("12***89", strategy.qq("123456789"));
        assertEquals("***", strategy.qq("1"));
        assertNull(strategy.phone(null));
    }

    @Test
    void labelMapsShouldCoverRequiredDictionaries()
    {
        assertEquals("男", OaMemberVoAssembler.GENDER_LABELS.get("0"));
        assertEquals("女", OaMemberVoAssembler.GENDER_LABELS.get("1"));
        assertEquals("大二", OaMemberVoAssembler.GRADE_LABELS.get("2"));
        assertEquals("干事", OaMemberVoAssembler.POSITION_LABELS.get("5"));
        assertEquals("在籍", OaMemberVoAssembler.STATUS_LABELS.get("0"));
        assertEquals("已退会", OaMemberVoAssembler.STATUS_LABELS.get("1"));
        assertEquals("自助申请", OaMemberVoAssembler.SOURCE_LABELS.get("1"));
    }
}
